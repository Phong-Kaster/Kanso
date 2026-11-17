package com.astronex.photoedit.core.reshape

/**
 * Chin tip, the way Meitu's Beautify > Face > Face > Chin Tip does it: the sides of the chin raised
 * and drawn in more than its middle, so the chin's tip comes to a point (a positive amount), or
 * dropped and pushed out, so it rounds and flattens (a negative amount).
 *
 * Measured on the Galaxy A13 (2026-11-17, a front-facing portrait, landmarks from the library's own
 * face model, optical flow to each end): Meitu's slider is two-way and its +100 moved, as shares of
 * the eyes-to-chin length, the chin's point (152) 1.0% up, its neighbours (148, 377) 2.1% up and 0.9%
 * in, the next ones out (176, 400) 2.0% up and 1.2% in, and 150, 379 0.2% up and in; the jaw above
 * them and the mouth did not move. Its -100 did the same the other way, as strongly.
 *
 * Each of those points gets one push ([FaceShift]); neighbouring pushes blend, so the moves below were
 * tuned (on the same portrait, read the same way) until the warped picture matched Meitu's to within
 * 0.1% at both ends. The points are shared with Chin length and Chin shape; [FaceWarps] adds the moves.
 *
 * @author Phong-Kaster
 */
internal object ChinTipWarp {

    /**
     * Returns the pushes that reshape [face]'s chin by [amount], from -1 (roundest) through 0
     * (unchanged) to 1 (most pointed). A value outside -1..1 is clamped and NaN counts as 0.
     *
     * @author Phong-Kaster
     */
    fun pushesFor(face: FaceMesh, amount: Float): List<WarpHandle> {
        val strength = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        return FaceShift.pushesFor(face, SHIFTS, strength, RADIUS_SHARE)
    }

    /** Each point's move at full strength, down the face and in, as shares of the eyes-to-chin length. */
    private val SHIFTS = listOf(FaceShift.Shift(FaceMeshLandmark.CHIN, down = -0.010f, inward = 0f)) +
        FaceShift.mirrored(148, 377, down = -0.0232f, inward = 0.0098f) +
        FaceShift.mirrored(176, 400, down = -0.0196f, inward = 0.0125f) +
        FaceShift.mirrored(150, 379, down = -0.0024f, inward = 0.0026f)

    /** Each push reaches this share of the face's width: the chin, not the mouth above it. */
    private const val RADIUS_SHARE = 0.10f
}
