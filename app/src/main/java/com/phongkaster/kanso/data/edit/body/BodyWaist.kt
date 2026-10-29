package com.phongkaster.kanso.data.edit.body

import com.phongkaster.kanso.data.edit.face.WarpPush
import kotlin.math.hypot

/**
 * Waist, the way Meitu's Beautify > Body > Reshape > Waist does it: both sides of the waist are
 * pulled toward the body's centre line (slimmer, a positive amount) or pushed away from it (wider,
 * a negative amount), the same way round as Meitu's slider (seen on the Galaxy A13, 2026-10-29: its
 * right end shortened the belt, its left end lengthened it).
 *
 * Like [com.phongkaster.kanso.data.edit.face.FaceWidth], this object only decides *where* the
 * outline should go and returns the moves as [WarpPush]es; `LocalWarp` moves the pixels.
 *
 * **Where the waist is.** The torso's centre line runs from between the shoulders to between the
 * hips ([PoseIndex]). The waist is a band of that line, [LEVELS] of the way down, weighted by
 * [WEIGHTS] so the narrowest part moves the most and the shape fades out toward the ribs and the
 * hips. At each level the tool walks sideways across the person's mask ([BodyEdges]) to find
 * both edges of the waist, and pushes each edge toward the centre line by a share of its own
 * distance from it.
 *
 * **Without a mask**, or where the walk fails (an arm resting on the waist hides its edge, so the
 * walk runs on into the arm), the edge is guessed from the joints: between the shoulders' and the
 * hips' spread ([SHOULDER_HALF], [HIP_HALF]). A found edge closer or further than the guess allows
 * ([MIN_REACH], [MAX_REACH]) is not trusted either.
 *
 * Everything is measured on the body itself, so the same value slims a small figure and a large
 * one alike, and a leaning torso along its own lean. A body whose shoulders or hips are hardly
 * visible (a head-and-shoulders portrait) gets no pushes.
 */
object BodyWaist {

    /**
     * Returns the pushes that reshape [body]'s waist by [amount], from -1 (widest) through 0
     * (unchanged) to 1 (slimmest). A value outside -1..1 is clamped and NaN counts as 0. An amount
     * of 0, a torso that is not in the photo, or one too small to measure gives no pushes.
     */
    fun pushesFor(body: BodyLandmarks, amount: Float): List<WarpPush> {
        val strength = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        if (strength == 0f || body.points.size < PoseIndex.COUNT) return emptyList()
        val leftShoulder = body[PoseIndex.LEFT_SHOULDER]
        val rightShoulder = body[PoseIndex.RIGHT_SHOULDER]
        val leftHip = body[PoseIndex.LEFT_HIP]
        val rightHip = body[PoseIndex.RIGHT_HIP]
        val torso = listOf(leftShoulder, rightShoulder, leftHip, rightHip)
        if (torso.any(predicate = { it.visibility < MIN_VISIBILITY })) return emptyList()
        // The centre line: from the middle of the shoulders to the middle of the hips.
        val topX = (leftShoulder.x + rightShoulder.x) / 2f
        val topY = (leftShoulder.y + rightShoulder.y) / 2f
        val axisX = (leftHip.x + rightHip.x) / 2f - topX
        val axisY = (leftHip.y + rightHip.y) / 2f - topY
        val length = hypot(axisX, axisY)
        val shoulderSpread = hypot(rightShoulder.x - leftShoulder.x, rightShoulder.y - leftShoulder.y)
        val hipSpread = hypot(rightHip.x - leftHip.x, rightHip.y - leftHip.y)
        // Under a pixel or two there is no torso to reshape, and dividing by it would blow up.
        if (length < MIN_SIZE || shoulderSpread < MIN_SIZE) return emptyList()
        // Sideways: the centre line's direction turned a quarter, so it crosses the waist.
        val acrossX = -axisY / length
        val acrossY = axisX / length
        val radius = length * RADIUS_SHARE
        val pushes = ArrayList<WarpPush>(LEVELS.size * 2)
        LEVELS.forEachIndexed(
            action = { index, level ->
                val centreX = topX + axisX * level
                val centreY = topY + axisY * level
                // The guessed half-width here, between the shoulders' and the hips' spread.
                val guess = shoulderSpread * SHOULDER_HALF + (hipSpread * HIP_HALF - shoulderSpread * SHOULDER_HALF) * level
                val share = strength * MAX_SHIFT * WEIGHTS[index]
                SIDES.forEach(
                    action = { side ->
                        val dirX = acrossX * side
                        val dirY = acrossY * side
                        val found = body.mask?.let(
                            block = { mask -> BodyEdges.distanceToEdge(mask, centreX, centreY, dirX, dirY, guess * MAX_REACH) },
                        )
                        val half = found?.takeIf(predicate = { it >= guess * MIN_REACH }) ?: guess
                        val edgeX = centreX + dirX * half
                        val edgeY = centreY + dirY * half
                        // Positive strength moves the edge toward the centre line (slimmer).
                        pushes += WarpPush(
                            centerX = edgeX,
                            centerY = edgeY,
                            moveX = (centreX - edgeX) * share,
                            moveY = (centreY - edgeY) * share,
                            radius = radius,
                        )
                    },
                )
            },
        )
        return pushes
    }

    /**
     * Where along the centre line the waist's pushes sit, from the shoulders (0) to the hips (1):
     * the lower ribs, the natural waist, and the top of the hips.
     */
    private val LEVELS = floatArrayOf(0.45f, 0.6f, 0.75f, 0.9f)

    /** How much each level moves: most at the natural waist, less toward the ribs and the hips. */
    private val WEIGHTS = floatArrayOf(0.4f, 0.85f, 1f, 0.55f)

    /** Both sides of the body: one way across, then the other. */
    private val SIDES = intArrayOf(1, -1)

    /** At full strength, the share of its distance to the centre line an edge moves. */
    private const val MAX_SHIFT = 0.12f

    /** Each push reaches this share of the torso's length (shoulders to hips) around its edge. */
    private const val RADIUS_SHARE = 0.3f

    /** The torso's half-width just under the shoulders, as a share of the shoulders' spread. */
    private const val SHOULDER_HALF = 0.4f

    /** The torso's half-width at the hips, as a share of the hips' spread (hip joints sit inside the hips). */
    private const val HIP_HALF = 0.8f

    /** A found edge nearer than this share of the guess is not trusted. */
    private const val MIN_REACH = 0.5f

    /** The walk to an edge gives up after this share of the guess. */
    private const val MAX_REACH = 1.8f

    /** A shoulder or hip less visible than this (0..1) means the torso is not in the photo. */
    private const val MIN_VISIBILITY = 0.5f

    /** Bodies smaller than this, in pixels, are left alone. */
    private const val MIN_SIZE = 2f
}
