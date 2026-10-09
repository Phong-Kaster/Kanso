package com.phongkaster.kanso.ui.camera

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
