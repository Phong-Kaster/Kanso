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

/** Which of the phone's cameras is in use. */
enum class CameraLens {
    /** The camera on the back of the phone, facing away from the person. */
    Back,

    /** The camera on the front of the phone, facing the person (the "selfie" camera). */
    Front;

    /** Returns the other lens: [Front] for [Back] and the other way round. */
    fun other(): CameraLens = if (this == Back) Front else Back
}

/**
 * What the flash does when a photo is taken.
 *
 * The flash only fires for the still photo; the live picture never lights it.
 */
enum class FlashMode {
    /** The camera decides: it fires the flash only when the scene is too dark. */
    Auto,

    /** The flash fires for every photo. */
    On,

    /** The flash never fires. */
    Off;

    /** Returns the mode the flash button moves to next: auto, then on, then off, then auto. */
    fun next(): FlashMode = when (this) {
        Auto -> On
        On -> Off
        Off -> Auto
    }
}

/**
 * Everything the camera screen reads about the camera: what it is doing, which lens is in use,
 * the flash and the zoom. Each field has a sensible value even before the camera is open, so the
 * screen can always draw its controls.
 */
data class CameraState(
    /** What the camera is doing right now. */
    val status: CameraStatus = CameraStatus.Idle,
    /** The lens in use, or the one that will be opened next. */
    val lens: CameraLens = CameraLens.Back,
    /** True when the phone has both a back and a front camera, so the lens can be switched. */
    val canSwitchLens: Boolean = false,
    /** What the flash does for the next photo. */
    val flashMode: FlashMode = FlashMode.Auto,
    /**
     * True when the lens in use has a flash. Front cameras usually have none, and then the flash
     * button is hidden.
     */
    val hasFlash: Boolean = false,
    /** The current zoom: 1 is no zoom, 2 shows things twice as large, and so on. */
    val zoomRatio: Float = 1f,
    /** The smallest zoom the lens in use allows (below 1 on phones with a wide-angle lens). */
    val minZoomRatio: Float = 1f,
    /** The largest zoom the lens in use allows; 1 when it cannot zoom at all. */
    val maxZoomRatio: Float = 1f,
)

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
