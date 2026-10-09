package com.phongkaster.kanso.ui.camera

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
