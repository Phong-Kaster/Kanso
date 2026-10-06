package com.phongkaster.kanso.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The real camera, driven by CameraX, the Jetpack library that hides the differences between
 * phone cameras behind one simple API.
 *
 * How CameraX is used here:
 *
 * - CameraX thinks in "use cases": each one is a job the camera does at the same time as the
 *   others. This screen needs two. The [Preview] use case streams the live picture into a
 *   surface on screen (supplied by a `PreviewView`). The [ImageCapture] use case takes a full
 *   quality still photo when asked and saves it to a file.
 * - The [ProcessCameraProvider] is the one object per app that owns the cameras. Getting it
 *   takes a moment the first time (it asks the system which cameras exist), so it is handed out
 *   as a "future", a value that arrives later; [awaitProvider] waits for it without blocking.
 * - `bindToLifecycle` connects the chosen camera to the use cases and ties them to a lifecycle
 *   owner. From then on CameraX opens the camera when that owner is started and closes it when
 *   it stops or is destroyed, so the app cannot forget to free the camera when the person
 *   leaves. [unbind] disconnects earlier, when the screen stops showing the live picture.
 *
 * Only [context]'s application context is kept, so the controller never holds on to an activity.
 * Lens switching, flash and zoom belong in this class later; [state] is where they will show.
 */
class CameraXController(context: Context) : CameraController {

    /** The application context: lives as long as the app, so keeping it leaks nothing. */
    private val appContext: Context = context.applicationContext

    /** Runs CameraX's callbacks on the main thread, where the rest of this class runs. */
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)

    /** The current state behind [state]; only this controller changes it. */
    private val mutableState = MutableStateFlow(CameraState())

    /** What the camera is doing, for the camera screen. */
    override val state: StateFlow<CameraState> = mutableState.asStateFlow()

    /** The app's camera owner, once it has arrived. */
    private var provider: ProcessCameraProvider? = null

    /** The live-picture use case currently bound, if any. */
    private var preview: Preview? = null

    /** The still-photo use case currently bound, if any; [takePicture] uses it. */
    private var imageCapture: ImageCapture? = null

    /**
     * Opens the back camera (or the front one on a phone with no back camera) with a live
     * picture and still photos, tied to [lifecycleOwner], and sends the picture to
     * [surfaceProvider]. Sets [CameraStatus.Ready] on success and [CameraStatus.Unavailable]
     * when there is no camera or it cannot be opened.
     */
    override suspend fun bind(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        // CameraX must be told about use cases on the main thread.
        withContext(Dispatchers.Main.immediate) {
            mutableState.update { it.copy(status = CameraStatus.Starting) }
            try {
                val cameraProvider = provider ?: awaitProvider().also { provider = it }
                val selector = chooseCamera(cameraProvider)
                if (selector == null) {
                    mutableState.update { it.copy(status = CameraStatus.Unavailable) }
                    return@withContext
                }

                // The live picture: the preview use case draws into whatever surface the
                // provider hands it, here the PreviewView on the camera screen.
                val newPreview = Preview.Builder().build()
                newPreview.surfaceProvider = surfaceProvider

                // The still photo. MINIMIZE_LATENCY favours a quick shutter over squeezing out
                // the last bit of quality, which suits a "take it, then keep or retake" screen.
                // The photo's rotation is taken from how the phone is held and written into the
                // JPEG's EXIF data rather than applied to the pixels.
                val newCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Drop whatever this controller bound before (after a rotation, say), then bind
                // both use cases together so they share the one open camera.
                unbindUseCases()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, newPreview, newCapture)
                preview = newPreview
                imageCapture = newCapture
                mutableState.update { it.copy(status = CameraStatus.Ready) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No camera service, the camera is held by another app, or the phone refused the
                // combination of use cases: the screen says the camera could not be started.
                unbindUseCases()
                mutableState.update { it.copy(status = CameraStatus.Unavailable) }
            }
        }
    }

    /**
     * Takes one photo with the still-photo use case and saves it into [file].
     *
     * ImageCapture.OutputFileOptions tells CameraX where to write; CameraX does the writing on
     * its own background thread and calls back when the file is complete or when it failed.
     * Throws [IllegalStateException] if the camera is not ready, or the [ImageCaptureException]
     * CameraX reports.
     */
    override suspend fun takePicture(file: File) {
        val capture = withContext(Dispatchers.Main.immediate) { imageCapture }
            ?: throw IllegalStateException("The camera is not ready")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        // suspendCancellableCoroutine turns CameraX's callback into a plain suspending call: the
        // coroutine waits here until one of the two callbacks resumes it.
        suspendCancellableCoroutine<Unit> { continuation ->
            capture.takePicture(
                options,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        continuation.resume(Unit)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
        }
    }

    /** Disconnects this controller's use cases from the camera and reports [CameraStatus.Idle]. */
    override fun unbind() {
        unbindUseCases()
        mutableState.update { it.copy(status = CameraStatus.Idle) }
    }

    /** Frees the camera for good; the same as [unbind], since the provider is shared app-wide. */
    override fun release() {
        unbind()
    }

    /**
     * Unbinds only the use cases this controller bound (never another screen's), and forgets
     * them.
     */
    private fun unbindUseCases() {
        val bound = listOfNotNull<UseCase>(preview, imageCapture)
        if (bound.isNotEmpty()) provider?.unbind(*bound.toTypedArray())
        preview = null
        imageCapture = null
    }

    /**
     * Waits, without blocking the thread, for the app's [ProcessCameraProvider].
     *
     * `getInstance` hands back a future; addListener runs the given code once the value is
     * there, and the coroutine resumes with it (or with the error that stopped it).
     */
    private suspend fun awaitProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(appContext)
            future.addListener(
                {
                    try {
                        continuation.resume(future.get())
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                },
                mainExecutor,
            )
        }

    /**
     * Returns the camera to open: the back camera if the phone has one, else the front one, or
     * null when it has neither.
     */
    private fun chooseCamera(cameraProvider: ProcessCameraProvider): CameraSelector? =
        listOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)
            .firstOrNull { selector ->
                // hasCamera throws when the camera's details cannot be read; that counts as "no".
                runCatching { cameraProvider.hasCamera(selector) }.getOrDefault(false)
            }
}
