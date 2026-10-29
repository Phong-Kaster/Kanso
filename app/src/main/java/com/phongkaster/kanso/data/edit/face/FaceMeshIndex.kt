package com.phongkaster.kanso.data.edit.face

/**
 * The face-mesh landmarks Kanso uses, by the number MediaPipe gives them (see [FaceLandmarks]).
 *
 * "Left" and "right" are the photo's left and right, not the person's: on a front-facing photo the
 * photo's left side is the person's right cheek.
 *
 * The outline lists run from the cheek at eye height down to just above the chin, so position 0
 * is the top of the cheek and the last position is near the chin. The two lists mirror each other
 * position by position.
 */
object FaceMeshIndex {

    /** The top of the forehead, on the face's centre line. */
    const val FOREHEAD = 10

    /** The point between the eyes, on the face's centre line. */
    const val BETWEEN_EYES = 168

    /** The bottom of the chin, on the face's centre line. */
    const val CHIN = 152

    /** The outline of the face's left side in the photo, from the cheek at eye height down. */
    val LEFT_OUTLINE = listOf(234, 93, 132, 58, 172, 136, 150)

    /** The outline of the face's right side in the photo, mirroring [LEFT_OUTLINE] point by point. */
    val RIGHT_OUTLINE = listOf(454, 323, 361, 288, 397, 365, 379)

    /**
     * The outline of the face's left temple in the photo, from high on the forehead down to just
     * above the eye's outer corner (Temple).
     */
    val LEFT_TEMPLE = listOf(54, 21, 162)

    /** The right temple in the photo, mirroring [LEFT_TEMPLE] point by point. */
    val RIGHT_TEMPLE = listOf(284, 251, 389)

    /**
     * The chin and its neighbours along the lower outline (Chin length): the chin itself (152), then
     * the points beside it on the left and right in the photo (148, 377), then the next ones out
     * (176, 400).
     */
    val CHIN_AREA = listOf(152, 148, 377, 176, 400)

    /**
     * The top of the forehead and its neighbours along the hairline (Hairline): the middle (10),
     * then the points beside it on the left and right in the photo (109, 338), then the next ones
     * out (67, 297).
     */
    val HAIRLINE = listOf(10, 109, 338, 67, 297)
}
