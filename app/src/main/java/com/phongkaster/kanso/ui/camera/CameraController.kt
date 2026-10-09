package com.phongkaster.kanso.ui.camera

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * The camera, behind one small interface, so the camera screen never talks to the camera
 * hardware directly.
 *
 * The real version is [CameraXController]. Tests use a fake that needs no hardware: it reports
 * [CameraStatus.Ready] when bound and writes a ready-made JPEG when asked for a photo. That works
 * because the screen only does a few things with a camera: connect it to the screen ([bind]),
 * change its settings ([switchLens], [setFlashMode], [setZoomRatio]), take a photo into a file
 * ([takePicture]) and let it go ([unbind], [release]).
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

    /**
     * Swaps to the other lens (back to front, or front to back), when the phone has both.
     *
     * While the camera is connected the swap happens at once and the live picture switches;
     * otherwise the other lens is simply the one [bind] opens next. The zoom starts again at 1
     * on the new lens, and [CameraState.hasFlash] tells whether the new lens has a flash.
     */
    fun switchLens()

    /** Sets what the flash does for the next photo; the choice is kept across lens switches. */
    fun setFlashMode(mode: FlashMode)

    /**
     * Zooms the live picture and the photo to [ratio], held between the lens's
     * [CameraState.minZoomRatio] and [CameraState.maxZoomRatio]. Does nothing while the camera is
     * not connected.
     */
    fun setZoomRatio(ratio: Float)

    /** Disconnects the camera from the screen: the live picture stops and the camera is freed. */
    fun unbind()

    /** Lets go of the camera for good. Called when the camera screen is gone. */
    fun release()
}
