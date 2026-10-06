package com.phongkaster.kanso.ui.camera

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** What the camera is doing right now, as the camera screen needs to know it. */
enum class CameraStatus {
    /** Nothing is connected: the camera is not in use. */
    Idle,

    /** The camera is being opened; the live picture is not there yet. */
    Starting,

    /** The camera is open: the live picture shows and a photo can be taken. */
    Ready,

    /** The camera could not be opened (none on this phone, or another app holds it). */
    Unavailable,
}

/**
 * Everything the camera screen reads about the camera. It is a class rather than the bare
 * [status] so that later settings (which lens, the flash mode, the zoom) can be added as more
 * fields without changing how the screen reads it.
 */
data class CameraState(
    /** What the camera is doing right now. */
    val status: CameraStatus = CameraStatus.Idle,
)

/**
 * The camera, behind one small interface, so the camera screen never talks to the camera
 * hardware directly.
 *
 * The real version is [CameraXController]. Tests use a fake that needs no hardware: it reports
 * [CameraStatus.Ready] when bound and writes a ready-made JPEG when asked for a photo. That works
 * because the screen only ever does three things with a camera: connect it to the screen
 * ([bind]), take a photo into a file ([takePicture]) and let it go ([unbind], [release]).
 *
 * Every function is called on the main thread.
 */
interface CameraController {

    /** What the camera is doing, as state the screen can read at any moment and watch change. */
    val state: StateFlow<CameraState>

    /**
     * Opens the camera and sends its live picture to [surfaceProvider], for as long as
     * [lifecycleOwner] is started.
     *
     * The surface provider is the thing that hands the camera a place to draw into; the
     * screen's preview view supplies it. Binding to a lifecycle owner (the activity, or the
     * screen) means the camera opens by itself when the screen is in front and closes by itself
     * when it is not, so the app never keeps the camera busy in the background.
     *
     * Returns once the camera is connected, or once it is known that it cannot be: the outcome
     * is in [state] as [CameraStatus.Ready] or [CameraStatus.Unavailable]. If the caller's
     * coroutine is cancelled first, nothing is connected.
     */
    suspend fun bind(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider)

    /**
     * Takes one photo and writes it as a JPEG into [file], returning when the file is complete.
     *
     * The JPEG may record in its EXIF data how it must be turned to look upright, instead of
     * turning the pixels; anything that shows the file must honour that. Throws if the camera is
     * not ready or the photo could not be taken or written.
     */
    suspend fun takePicture(file: File)

    /** Disconnects the camera from the screen: the live picture stops and the camera is freed. */
    fun unbind()

    /** Lets go of the camera for good. Called when the camera screen is gone. */
    fun release()
}
