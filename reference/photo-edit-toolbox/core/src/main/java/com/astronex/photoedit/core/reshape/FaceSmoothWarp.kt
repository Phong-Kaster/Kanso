package com.astronex.photoedit.core.reshape

/**
 * Smooth, the way Meitu's Beautify > Face > Face > Smooth does it: the face's outline from the cheeks
 * round to the chin made more even, the jaw's corner drawn in and the dip just above it filled out
 * (a positive amount). Skin is not touched: this is the outline's shape, not skin smoothing.
 *
 * Measured on the Galaxy A13 (2026-11-17, a front-facing portrait, landmarks from the library's own
 * face model, optical flow): Meitu's slider is one-way (0 to 100) and gentle. Its 100 moved the
 * outline only sideways, as shares of the eyes-to-chin length: the jaw's corner (172, 397) and the
 * point below it (136, 365) about 0.5% in, the cheek at eye height (234, 454, and 127, 356 above)
 * 0.2-0.3% in, the outline at the mouth's height (58, 288) 0.3% in, and the dip between (132, 361)
 * 0.3% out; the chin did not move, and 50 moved everything half as far. Meitu evens out each face's own
 * bumps (the two sides differed); a point's place on the mesh does not show those bumps (an even
 * curve fitted through the outline matched Meitu's moves poorly), so this applies the average of the
 * two sides to every face. Here the slider is two-way: +1 is Meitu's 100 and -1 the same moves
 * reversed.
 *
 * Each point gets one push ([FaceShift]), tuned on the same portrait until the warped picture matched
 * Meitu's to within 0.1%. The points are Width's too; [FaceWarps] adds the moves.
 *
 * @author Phong-Kaster
 */
internal object FaceSmoothWarp {

    /**
     * Returns the pushes that smooth [face]'s outline by [amount], from -1 (the opposite) through 0
     * (unchanged) to 1 (smoothest). A value outside -1..1 is clamped and NaN counts as 0.
     *
     * @author Phong-Kaster
     */
    fun pushesFor(face: FaceMesh, amount: Float): List<WarpHandle> {
        val strength = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        return FaceShift.pushesFor(face, SHIFTS, strength, RADIUS_SHARE)
    }

    /** Each point's sideways move at full strength, as a share of the eyes-to-chin length (+ is in). */
    private val SHIFTS =
        FaceShift.mirrored(127, 356, down = 0f, inward = 0.0030f) +
            FaceShift.mirrored(234, 454, down = 0f, inward = 0.0017f) +
            FaceShift.mirrored(132, 361, down = 0f, inward = -0.0034f) +
            FaceShift.mirrored(58, 288, down = 0f, inward = 0.0027f) +
            FaceShift.mirrored(172, 397, down = 0f, inward = 0.0051f) +
            FaceShift.mirrored(136, 365, down = 0f, inward = 0.0045f) +
            FaceShift.mirrored(150, 379, down = 0f, inward = 0.0013f)

    /** Each push reaches this share of the face's width, so each point keeps close to its own move. */
    private const val RADIUS_SHARE = 0.10f
}
