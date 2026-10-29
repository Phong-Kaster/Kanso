package com.phongkaster.kanso.data.edit.face

/**
 * Hairline, the way Meitu's Beautify > Face > Hairline does it: the hairline moved down the face
 * (a lower hairline and a smaller forehead, a positive amount) or up it (a higher hairline, a
 * negative amount), the same way round as Meitu's slider (measured on the Galaxy A13, 2026-10-29:
 * its right end lowered the hairline at the parting about 17 screen px, its left end raised it
 * about 17 px, some 2.5% of the eyes-to-chin length).
 *
 * The top of the forehead and its neighbours along the hairline ([FaceMeshIndex.HAIRLINE]) move
 * along the face's up-and-down line ([FaceVertical]): the middle the most, the sides less, so the
 * hairline keeps its curve.
 *
 * **Above the landmarks.** The face mesh stops on the forehead's skin, below the real hairline: on
 * the Galaxy A13 (2026-10-29) a first version centred on the landmarks moved the middle of the
 * forehead and the hairline itself only about 1 screen px. So each push sits [OFFSET] of the
 * eyes-to-chin length above its landmark, on the hairline, and reaches [RADIUS_SHARE] of the face's
 * width: far enough to carry the hairline, not down to the eyebrows.
 */
object FaceHairline {

    /**
     * Returns the pushes that reshape [face]'s hairline by [amount], from -1 (highest) through 0
     * (unchanged) to 1 (lowest). A value outside -1..1 is clamped and NaN counts as 0.
     */
    fun pushesFor(face: FaceLandmarks, amount: Float): List<WarpPush> =
        FaceVertical.pushesFor(face, amount, FaceMeshIndex.HAIRLINE, WEIGHTS, MAX_SHIFT, RADIUS_SHARE, OFFSET)

    /** How much each hairline point moves, in [FaceMeshIndex.HAIRLINE]'s order: the middle most. */
    private val WEIGHTS = floatArrayOf(1f, 0.85f, 0.85f, 0.55f, 0.55f)

    /** At full strength, the share of the eyes-to-chin length the hairline moves. */
    private const val MAX_SHIFT = 0.035f

    /** Each push reaches this share of the face's width around its point. */
    private const val RADIUS_SHARE = 0.3f

    /** How far above its landmark each push sits, as a share of the eyes-to-chin length (up is negative). */
    private const val OFFSET = -0.2f
}
