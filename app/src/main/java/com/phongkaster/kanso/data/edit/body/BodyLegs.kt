package com.phongkaster.kanso.data.edit.body

import com.phongkaster.kanso.data.edit.face.WarpPush

/**
 * Legs, the way Meitu's Beautify > Body > Reshape > Legs does it: both legs made slimmer (a
 * positive amount) or thicker (a negative one), the same way round as Meitu's other slimming
 * sliders (Waist, Arms; seen on the Galaxy A13, 2026-10-29).
 *
 * Each leg is two stretches, each slimmed by [BodyLimb] as the arms are: the **thigh** from hip to
 * knee and the **calf** from knee to ankle ([PoseIndex]). The pushes stay off the joints' ends
 * ([THIGH_LEVELS], [CALF_LEVELS]), so the hip, where the legs meet the body, and the ankle are left
 * alone. The thigh is guessed thicker than the calf ([THIGH_HALF], [CALF_HALF]) where the mask
 * cannot show the edge, as between two legs pressed together.
 *
 * **A photo cut above the knee.** Many photos end at the thighs. A thigh needs only its hip to be
 * clearly visible: the knee may lie outside the photo ([KNEE_OUT_OF_PHOTO]), where the pose model
 * still places it along the leg. A calf needs both its knee and its ankle clearly visible.
 */
object BodyLegs {

    /**
     * Returns the pushes that reshape both of [body]'s legs by [amount], from -1 (thickest)
     * through 0 (unchanged) to 1 (slimmest). A value outside -1..1 is clamped and NaN counts as 0.
     */
    fun pushesFor(body: BodyLandmarks, amount: Float): List<WarpPush> {
        val strength = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        if (strength == 0f || body.points.size < PoseIndex.COUNT) return emptyList()
        val share = strength * MAX_SHIFT
        val pushes = ArrayList<WarpPush>()
        LEGS.forEach(
            action = { (hip, knee, ankle) ->
                pushes += BodyLimb.pushesFor(
                    body, hip, knee, THIGH_LEVELS, THIGH_WEIGHTS, THIGH_HALF, RADIUS_SHARE, share,
                    endVisibility = KNEE_OUT_OF_PHOTO,
                )
                pushes += BodyLimb.pushesFor(body, knee, ankle, CALF_LEVELS, CALF_WEIGHTS, CALF_HALF, RADIUS_SHARE, share)
            },
        )
        return pushes
    }

    /** Each leg as its hip, knee and ankle landmarks: the person's left, then right. */
    private val LEGS = listOf(
        Triple(PoseIndex.LEFT_HIP, PoseIndex.LEFT_KNEE, PoseIndex.LEFT_ANKLE),
        Triple(PoseIndex.RIGHT_HIP, PoseIndex.RIGHT_KNEE, PoseIndex.RIGHT_ANKLE),
    )

    /** Where along the thigh (0 at the hip, 1 at the knee) its pushes sit. */
    private val THIGH_LEVELS = floatArrayOf(0.3f, 0.55f, 0.8f)

    /** How much each thigh level moves: most at mid-thigh, where the leg is fullest. */
    private val THIGH_WEIGHTS = floatArrayOf(0.7f, 1f, 0.8f)

    /** Where along the calf (0 at the knee, 1 at the ankle) its pushes sit. */
    private val CALF_LEVELS = floatArrayOf(0.25f, 0.5f, 0.75f)

    /** How much each calf level moves: most at the calf's belly, little near the ankle. */
    private val CALF_WEIGHTS = floatArrayOf(0.8f, 1f, 0.6f)

    /** The thigh's guessed half-thickness, as a share of its length. */
    private const val THIGH_HALF = 0.2f

    /** The calf's guessed half-thickness, as a share of its length. */
    private const val CALF_HALF = 0.13f

    /** Each push reaches this share of its stretch's length around its edge. */
    private const val RADIUS_SHARE = 0.3f

    /** At full strength, the share of its distance to the leg's line an edge moves. */
    private const val MAX_SHIFT = 0.12f

    /** How visible a knee must be for its thigh: any, since the knee may be below the photo. */
    private const val KNEE_OUT_OF_PHOTO = 0f
}
