package com.phongkaster.kanso.data.edit.face

/**
 * Chin length, the way Meitu's Beautify > Face > Chin length does it: the chin moved up the face
 * (a shorter chin, a positive amount) or down it (longer, a negative amount), the same way round as
 * Meitu's slider (measured on the Galaxy A13, 2026-10-29: its right end raised the chin's lower
 * edge about 24 screen px, its left end lowered it about 18 px, on a face about 660 px from the
 * eyes to the chin: some 3% of that length).
 *
 * The chin's point and its neighbours along the lower outline ([FaceMeshIndex.CHIN_AREA]) move
 * along the face's up-and-down line ([FaceVertical]): the chin's own point the most, its neighbours
 * less, so the jaw follows smoothly and the mouth, above the pushes' reach, stays where it is.
 */
object FaceChinLength {

    /**
     * Returns the pushes that reshape [face]'s chin by [amount], from -1 (longest) through 0
     * (unchanged) to 1 (shortest). A value outside -1..1 is clamped and NaN counts as 0.
     */
    fun pushesFor(face: FaceLandmarks, amount: Float): List<WarpPush> =
        // A shorter chin is a chin moved up: the opposite of FaceVertical's "down".
        FaceVertical.pushesFor(face, -amount, FaceMeshIndex.CHIN_AREA, WEIGHTS, MAX_SHIFT, RADIUS_SHARE)

    /** How much each chin point moves, in [FaceMeshIndex.CHIN_AREA]'s order: the chin itself most. */
    private val WEIGHTS = floatArrayOf(1f, 0.8f, 0.8f, 0.5f, 0.5f)

    /** At full strength, the share of the eyes-to-chin length the chin moves. */
    private const val MAX_SHIFT = 0.035f

    /** Each push reaches this share of the face's width around its point. */
    private const val RADIUS_SHARE = 0.25f
}
