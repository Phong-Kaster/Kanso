package com.phongkaster.kanso.data.edit.body

/**
 * The pose landmarks Kanso uses, by the number MediaPipe gives them (see [BodyLandmarks]).
 *
 * "Left" and "right" are the **person's** left and right, as MediaPipe names them, not the
 * photo's: on a front-facing photo the person's left shoulder is on the photo's right. A tool that
 * treats both sides alike (Waist slims both edges) does not need to care which is which.
 */
object PoseIndex {

    /** The person's left shoulder. */
    const val LEFT_SHOULDER = 11

    /** The person's right shoulder. */
    const val RIGHT_SHOULDER = 12

    /** The person's left hip. */
    const val LEFT_HIP = 23

    /** The person's right hip. */
    const val RIGHT_HIP = 24

    /** How many landmarks the pose model gives for one person. */
    const val COUNT = 33
}
