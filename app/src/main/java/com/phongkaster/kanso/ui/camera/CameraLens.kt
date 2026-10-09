package com.phongkaster.kanso.ui.camera

/** Which of the phone's cameras is in use. */
enum class CameraLens {
    /** The camera on the back of the phone, facing away from the person. */
    Back,

    /** The camera on the front of the phone, facing the person (the "selfie" camera). */
    Front;

    /** Returns the other lens: [Front] for [Back] and the other way round. */
    fun other(): CameraLens = if (this == Back) Front else Back
}
