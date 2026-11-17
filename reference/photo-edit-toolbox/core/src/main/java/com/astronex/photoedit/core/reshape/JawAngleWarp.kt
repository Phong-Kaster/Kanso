package com.astronex.photoedit.core.reshape

/**
 * Jaw angle, the way Meitu's Beautify > Face > Face > Jaw angle does it: the jaw's corners raised and
 * drawn in toward the centre line, a softer, higher angle (a positive amount), or dropped and pushed
 * out, a squarer jaw (a negative amount).
 *
 * Measured on the Galaxy A13 (2026-11-17, a front-facing portrait, landmarks from the library's own
 * face model, optical flow to each end): Meitu's slider is two-way. Its +100 moved, as shares of the
 * eyes-to-chin length, the jaw's corner (172, 397) 2.3% up and 1.7% in, the outline just above it (58,
 * 288) 1.7% up and 1.3% in, just below it (136, 365) 2.0% up and 1.3% in, and toward the chin (150,
 * 379) 1.3% up and 0.7% in; the chin, the cheeks and the mouth did not move. Its -100 moved the same
 * points the other way, about 0.7 times as far, so the low end here is scaled by [LOW_END].
 *
 * Each of those points gets one push ([FaceShift]), tuned on the same portrait, read the same way,
 * until the warped picture matched Meitu's to within 0.1%. The points are Width's and Jawline's too;
 * [FaceWarps] adds the moves.
 *
 * @author Phong-Kaster
 */
internal object JawAngleWarp {

    /**
     * Returns the pushes that reshape [face]'s jaw by [amount], from -1 (squarest) through 0
     * (unchanged) to 1 (highest, softest corner). A value outside -1..1 is clamped and NaN counts as 0.
     *
     * @author Phong-Kaster
     */
    fun pushesFor(face: FaceMesh, amount: Float): List<WarpHandle> {
        val clamped = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        val strength = if (clamped < 0f) clamped * LOW_END else clamped
        return FaceShift.pushesFor(face, SHIFTS, strength, RADIUS_SHARE)
    }

    /** Each point's move at full strength, down the face and in, as shares of the eyes-to-chin length. */
    private val SHIFTS =
        FaceShift.mirrored(132, 361, down = -0.0020f, inward = 0.0014f) +
            FaceShift.mirrored(58, 288, down = -0.0172f, inward = 0.0128f) +
            FaceShift.mirrored(172, 397, down = -0.0246f, inward = 0.0182f) +
            FaceShift.mirrored(136, 365, down = -0.0204f, inward = 0.0135f) +
            FaceShift.mirrored(150, 379, down = -0.0113f, inward = 0.0048f)

    /** How far the low end moves the jaw, as a share of the high end: Meitu's -100 against its +100. */
    private const val LOW_END = 0.7f

    /** Each push reaches this share of the face's width: the jaw's corner and its neighbours. */
    private const val RADIUS_SHARE = 0.13f
}
