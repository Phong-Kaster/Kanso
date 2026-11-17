package com.astronex.photoedit.core.reshape

/**
 * Double chin, the way Meitu's Beautify > Face > Face > Double chin does it: the lower edge of the
 * jaw, where it meets the neck, lifted toward the face, so a fold under the jaw shows less (a
 * positive amount).
 *
 * Measured on the Galaxy A13 (2026-11-17, a front-facing portrait, landmarks from the library's own
 * face model, optical flow): Meitu's slider is one-way (0 to 100) and gentle, and it found its fold on
 * one side of that face only. Its 100 lifted the lower edge there, as shares of the eyes-to-chin
 * length: the jaw's corner (172) 0.2%, the points toward the chin (136, 150, 149, 176) 0.4%, 0.5%,
 * 0.5% and 0.3%, beside the chin (148) 0.1%, and the neck just below them a little less; the chin's
 * point did not move. The library finds no fold, so it lifts both sides alike by those amounts, which
 * gives about half of Meitu's moves on that side (Meitu also follows the fold's own shape). Here the
 * slider is two-way: +1 is Meitu's 100 and -1 lowers the edge as much.
 *
 * Each point gets one push ([FaceShift]), tuned on the same portrait until the warped landmarks
 * matched Meitu's to within 0.1%. The points are shared with Width, Chin length, Chin shape and Chin
 * tip; [FaceWarps] adds the moves.
 *
 * @author Phong-Kaster
 */
internal object DoubleChinWarp {

    /**
     * Returns the pushes that reshape [face]'s lower jaw by [amount], from -1 (lowered) through 0
     * (unchanged) to 1 (lifted most). A value outside -1..1 is clamped and NaN counts as 0.
     *
     * @author Phong-Kaster
     */
    fun pushesFor(face: FaceMesh, amount: Float): List<WarpHandle> {
        val strength = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        return FaceShift.pushesFor(face, SHIFTS, strength, RADIUS_SHARE)
    }

    /** Each point's move at full strength, down the face and in, as shares of the eyes-to-chin length. */
    private val SHIFTS =
        FaceShift.mirrored(172, 397, down = -0.0019f, inward = 0.001f) +
            FaceShift.mirrored(136, 365, down = -0.0034f, inward = 0.002f) +
            FaceShift.mirrored(150, 379, down = -0.0050f, inward = 0.002f) +
            FaceShift.mirrored(149, 378, down = -0.0054f, inward = 0.002f) +
            FaceShift.mirrored(176, 400, down = -0.0026f, inward = 0.001f) +
            FaceShift.mirrored(148, 377, down = -0.0008f, inward = 0f)

    /** Each push reaches this share of the face's width: the jaw's lower edge and the neck below it. */
    private const val RADIUS_SHARE = 0.12f
}
