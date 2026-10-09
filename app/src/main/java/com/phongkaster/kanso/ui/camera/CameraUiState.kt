package com.phongkaster.kanso.ui.camera

import java.io.File

/**
 * Everything the camera screen shows at one moment, as one snapshot. What a UiState is, and why
 * every screen has exactly one, is explained on `HomeUiState`; see there.
 *
 * The camera's snapshot holds two kinds of facts. [camera] is what the camera itself reports
 * (opening, ready or unavailable, the lens, the flash and the zoom): `CameraViewModel` copies it
 * from the camera controller every time it changes. The other fields are about the photo just
 * taken, which waits on screen until the person keeps it or retakes it.
 */
data class CameraUiState(
    /** What the camera is doing, with its lens, flash and zoom, as the controller last said. */
    val camera: CameraState = CameraState(),
    /** The photo just taken, waiting for Keep or Retake; null while the live picture shows. */
    val shot: File? = null,
    /** True while a photo is being taken, so the shutter cannot be pressed twice. */
    val taking: Boolean = false,
    /** True when the last attempt to take a photo failed; cleared by the next attempt. */
    val shotFailed: Boolean = false,
)
