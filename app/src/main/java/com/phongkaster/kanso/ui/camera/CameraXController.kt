package com.phongkaster.kanso.ui.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
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
 * The camera's settings:
 *
 * - Lens. A [CameraSelector] names which physical camera to open (back or front). A camera that
 *   is already connected cannot be told to become another one, so switching lens means
 *   "rebinding": unbinding the use cases and binding fresh ones to the other selector, with the
 *   same lifecycle owner and preview surface. That is why [bind] remembers both.
 * - Flash. The flash belongs to the still photo, so it is a setting of [ImageCapture]
 *   (`flashMode`), which can be changed at any time and applies to the next photo. Whether the
 *   lens has a flash at all is read from the camera's `CameraInfo.hasFlashUnit()`.
 * - Zoom. `bindToLifecycle` returns a [Camera], with two halves: `cameraControl` to change
 *   things (`setZoomRatio`) and `cameraInfo` to read them. `cameraInfo.zoomState` is a LiveData
 *   (a value that tells its observers each time it changes) holding the current zoom and the
 *   smallest and largest the lens allows; this controller copies it into [state].
 *
 * Only [context]'s application context is kept, so the controller never holds on to an activity.
 */
class CameraXController(context: Context) : CameraController {

    /** The application context: lives as long as the app, so keeping it leaks nothing. */
    private val appContext: Context = context.applicationContext

    /** Runs CameraX's callbacks on the main thread, where the rest of this class runs. */
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)

    /** The current state behind [state]; only this controller changes it. */
    private val mutableState = MutableStateFlow(CameraState())

    /** What the camera is doing and how it is set, for the camera screen. */
    override val state: StateFlow<CameraState> = mutableState.asStateFlow()

    /** The app's camera owner, once it has arrived. */
    private var provider: ProcessCameraProvider? = null

    /** The live-picture use case currently bound, if any. */
    private var preview: Preview? = null

    /** The still-photo use case currently bound, if any; [takePicture] uses it. */
    private var imageCapture: ImageCapture? = null

    /** The camera currently connected, if any; used to zoom it. */
    private var camera: Camera? = null

    /** The lifecycle owner of the current connection, kept so a lens switch can rebind. */
    private var boundOwner: LifecycleOwner? = null

    /** The preview surface of the current connection, kept so a lens switch can rebind. */
    private var boundSurfaceProvider: Preview.SurfaceProvider? = null

    /** The zoom LiveData being watched, if any, so the watching can be stopped. */
    private var watchedZoom: LiveData<ZoomState>? = null

    /** Copies each new zoom value of the connected camera into [state]. */
    private val zoomObserver = Observer<ZoomState> { zoom ->
        mutableState.update {
            it.copy(
                zoomRatio = zoom.zoomRatio,
                minZoomRatio = zoom.minZoomRatio,
                maxZoomRatio = zoom.maxZoomRatio,
            )
        }
    }

    /**
     * Opens the chosen lens (the back camera at first; the other one when the chosen one is
     * missing) with a live picture and still photos, tied to [lifecycleOwner], and sends the
     * picture to [surfaceProvider]. Sets [CameraStatus.Ready] on success and
     * [CameraStatus.Unavailable] when there is no camera or it cannot be opened.
     */
    override suspend fun bind(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        // CameraX must be told about use cases on the main thread.
        withContext(Dispatchers.Main.immediate) {
            mutableState.update { it.copy(status = CameraStatus.Starting) }
            val cameraProvider = try {
                provider ?: awaitProvider().also { provider = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No camera service on this phone: the screen says the camera could not start.
                mutableState.update { it.copy(status = CameraStatus.Unavailable) }
                return@withContext
            }
            boundOwner = lifecycleOwner
            boundSurfaceProvider = surfaceProvider
            bindUseCases(cameraProvider, lifecycleOwner, surfaceProvider)
        }
    }

    /**
     * Connects the camera of the lens in [state] to fresh use cases. Used both for the first
     * connection and for rebinding after a lens switch. Runs on the main thread.
     */
    private fun bindUseCases(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
    ) {
        try {
            val hasBack = hasCamera(cameraProvider, CameraLens.Back)
            val hasFront = hasCamera(cameraProvider, CameraLens.Front)
            val wanted = mutableState.value.lens
            // The chosen lens if the phone has it, else the other one, else none at all.
            val lens = when {
                hasCamera(cameraProvider, wanted) -> wanted
                hasBack || hasFront -> wanted.other()
                else -> null
            }
            if (lens == null) {
                unbindUseCases()
                mutableState.update { it.copy(status = CameraStatus.Unavailable, canSwitchLens = false) }
                return
            }

            // The live picture: the preview use case draws into whatever surface the provider
            // hands it, here the PreviewView on the camera screen.
            val newPreview = Preview.Builder().build()
            newPreview.surfaceProvider = surfaceProvider

            // The still photo. MINIMIZE_LATENCY favours a quick shutter over squeezing out the
            // last bit of quality, which suits a "take it, then keep or retake" screen. The
            // photo's rotation is taken from how the phone is held and written into the JPEG's
            // EXIF data rather than applied to the pixels. The flash mode chosen so far is kept.
            val newCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(mutableState.value.flashMode.toCameraX())
                .build()

            // Drop whatever this controller bound before (the other lens, or the connection
            // from before a rotation), then bind both use cases together so they share the one
            // open camera.
            unbindUseCases()
            val newCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                lens.selector(),
                newPreview,
                newCapture,
            )
            preview = newPreview
            imageCapture = newCapture
            camera = newCamera
            watchZoom(newCamera)
            mutableState.update {
                it.copy(
                    status = CameraStatus.Ready,
                    lens = lens,
                    canSwitchLens = hasBack && hasFront,
                    hasFlash = newCamera.cameraInfo.hasFlashUnit(),
                )
            }
        } catch (e: Exception) {
            // The camera is held by another app, or the phone refused the combination of use
            // cases: the screen says the camera could not be started.
            unbindUseCases()
            mutableState.update { it.copy(status = CameraStatus.Unavailable) }
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

    /**
     * Swaps to the other lens. While connected, this rebinds at once: the old lens's use cases
     * are unbound and new ones are bound to the other lens, with the same lifecycle owner and
     * preview surface.
     */
    override fun switchLens() {
        val current = mutableState.value
        if (!current.canSwitchLens) return
        mutableState.update { it.copy(lens = it.lens.other()) }
        val cameraProvider = provider
        val owner = boundOwner
        val surfaceProvider = boundSurfaceProvider
        if (cameraProvider != null && owner != null && surfaceProvider != null) {
            mutableState.update { it.copy(status = CameraStatus.Starting) }
            bindUseCases(cameraProvider, owner, surfaceProvider)
        }
    }

    /** Sets the flash for the next photo, on the bound still-photo use case if there is one. */
    override fun setFlashMode(mode: FlashMode) {
        imageCapture?.flashMode = mode.toCameraX()
        mutableState.update { it.copy(flashMode = mode) }
    }

    /**
     * Asks the connected camera to zoom to [ratio], held within the lens's limits.
     *
     * The new ratio goes into [state] straight away, so a pinch that sends many small steps in a
     * row builds on the latest one; the camera's own zoom report then follows. The camera does
     * the zooming in the background; a step overtaken by the next one is simply dropped by
     * CameraX, which is fine.
     */
    override fun setZoomRatio(ratio: Float) {
        val connected = camera ?: return
        val current = mutableState.value
        val clamped = ratio.coerceIn(current.minZoomRatio, current.maxZoomRatio)
        connected.cameraControl.setZoomRatio(clamped)
        mutableState.update { it.copy(zoomRatio = clamped) }
    }

    /** Disconnects this controller's use cases from the camera and reports [CameraStatus.Idle]. */
    override fun unbind() {
        unbindUseCases()
        boundOwner = null
        boundSurfaceProvider = null
        mutableState.update { it.copy(status = CameraStatus.Idle) }
    }

    /** Frees the camera for good; the same as [unbind], since the provider is shared app-wide. */
    override fun release() {
        unbind()
    }

    /**
     * Starts copying [newCamera]'s zoom into [state]. `observeForever` watches without a
     * lifecycle, so [unbindUseCases] stops the watching by hand.
     */
    private fun watchZoom(newCamera: Camera) {
        val zoom = newCamera.cameraInfo.zoomState
        zoom.observeForever(zoomObserver)
        watchedZoom = zoom
    }

    /**
     * Unbinds only the use cases this controller bound (never another screen's), stops watching
     * the zoom, and forgets them.
     */
    private fun unbindUseCases() {
        watchedZoom?.removeObserver(zoomObserver)
        watchedZoom = null
        val bound = listOfNotNull<UseCase>(preview, imageCapture)
        if (bound.isNotEmpty()) provider?.unbind(*bound.toTypedArray())
        preview = null
        imageCapture = null
        camera = null
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

    /** Tells whether the phone has a camera for [lens]. */
    private fun hasCamera(cameraProvider: ProcessCameraProvider, lens: CameraLens): Boolean =
        // hasCamera throws when the camera's details cannot be read; that counts as "no".
        runCatching { cameraProvider.hasCamera(lens.selector()) }.getOrDefault(false)

    /** Returns CameraX's ready-made selector for this lens. */
    private fun CameraLens.selector(): CameraSelector = when (this) {
        CameraLens.Back -> CameraSelector.DEFAULT_BACK_CAMERA
        CameraLens.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
    }

    /** Returns CameraX's number for this flash mode. */
    private fun FlashMode.toCameraX(): Int = when (this) {
        FlashMode.Auto -> ImageCapture.FLASH_MODE_AUTO
        FlashMode.On -> ImageCapture.FLASH_MODE_ON
        FlashMode.Off -> ImageCapture.FLASH_MODE_OFF
    }
}
