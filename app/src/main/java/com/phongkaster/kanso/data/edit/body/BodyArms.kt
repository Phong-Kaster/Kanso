package com.phongkaster.kanso.data.edit.body

import com.phongkaster.kanso.data.edit.face.WarpPush

/**
 * Arms, the way Meitu's Beautify > Body > Reshape > Arms does it: both arms made slimmer (a
 * positive amount) or thicker (a negative one), the same way round as Meitu's slider (seen on the
 * Galaxy A13, 2026-10-29: its right end thinned the arms).
 *
 * Each arm is two stretches, each slimmed by [BodyLimb]: the **upper arm** from shoulder to elbow
 * and the **forearm** from elbow to wrist ([PoseIndex]). The pushes stay away from the joints'
 * ends ([UPPER_LEVELS], [FORE_LEVELS]), so the shoulder, which joins the torso, and the hand are
 * left alone, and the middle of each stretch moves the most ([UPPER_WEIGHTS], [FORE_WEIGHTS]). The
 * upper arm is guessed thicker than the forearm ([UPPER_HALF], [FORE_HALF]) where the mask cannot
 * show the edge.
 *
 * Each arm, and each stretch of it, is handled on its own: an arm out of the photo or hidden behind
 * the body gets no pushes while the other one still does.
 */
object BodyArms {

    /**
     * Returns the pushes that reshape both of [body]'s arms by [amount], from -1 (thickest)
     * through 0 (unchanged) to 1 (slimmest). A value outside -1..1 is clamped and NaN counts as 0.
     */
    fun pushesFor(body: BodyLandmarks, amount: Float): List<WarpPush> {
        val strength = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        if (strength == 0f || body.points.size < PoseIndex.COUNT) return emptyList()
        val share = strength * MAX_SHIFT
        val pushes = ArrayList<WarpPush>()
        ARMS.forEach(
            action = { (shoulder, elbow, wrist) ->
                pushes += BodyLimb.pushesFor(body, shoulder, elbow, UPPER_LEVELS, UPPER_WEIGHTS, UPPER_HALF, RADIUS_SHARE, share)
                pushes += BodyLimb.pushesFor(body, elbow, wrist, FORE_LEVELS, FORE_WEIGHTS, FORE_HALF, RADIUS_SHARE, share)
            },
        )
        return pushes
    }

    /** Each arm as its shoulder, elbow and wrist landmarks: the person's left, then right. */
    private val ARMS = listOf(
        Triple(PoseIndex.LEFT_SHOULDER, PoseIndex.LEFT_ELBOW, PoseIndex.LEFT_WRIST),
        Triple(PoseIndex.RIGHT_SHOULDER, PoseIndex.RIGHT_ELBOW, PoseIndex.RIGHT_WRIST),
    )

    /** Where along the upper arm (0 at the shoulder, 1 at the elbow) its pushes sit. */
    private val UPPER_LEVELS = floatArrayOf(0.3f, 0.5f, 0.7f)

    /** How much each upper-arm level moves: most in the middle, where the arm is fullest. */
    private val UPPER_WEIGHTS = floatArrayOf(0.7f, 1f, 0.8f)

    /** Where along the forearm (0 at the elbow, 1 at the wrist) its pushes sit. */
    private val FORE_LEVELS = floatArrayOf(0.3f, 0.55f, 0.8f)

    /** How much each forearm level moves: less near the wrist, which is already narrow. */
    private val FORE_WEIGHTS = floatArrayOf(0.7f, 0.8f, 0.5f)

    /** The upper arm's guessed half-thickness, as a share of its length. */
    private const val UPPER_HALF = 0.17f

    /** The forearm's guessed half-thickness, as a share of its length. */
    private const val FORE_HALF = 0.12f

    /** Each push reaches this share of its stretch's length around its edge. */
    private const val RADIUS_SHARE = 0.3f

    /** At full strength, the share of its distance to the arm's line an edge moves. */
    private const val MAX_SHIFT = 0.15f
}
