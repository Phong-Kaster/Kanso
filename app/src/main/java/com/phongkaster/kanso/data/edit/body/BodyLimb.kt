package com.phongkaster.kanso.data.edit.body

import com.phongkaster.kanso.data.edit.face.WarpPush
import kotlin.math.hypot

/**
 * Slims (or thickens) one stretch of a limb between two joints: the upper arm from shoulder to
 * elbow, the forearm from elbow to wrist, and later the thigh and the calf. Every limb tool
 * ([BodyArms], then Legs) is a few calls to [pushesFor].
 *
 * **How.** The line from one joint to the other runs down the middle of the limb. At a few points
 * along it ([pushesFor]'s `levels`), the tool walks sideways across the person's mask
 * ([BodyEdges]) to the limb's two edges and pushes each edge toward the line, as Waist does for
 * the torso. A limb is round, so its thickness is guessed from its own length (`halfShare`) when
 * the mask cannot tell: no mask, or an arm resting against the body, where the walk runs on into
 * the torso. A found edge nearer than [MIN_REACH] or further than [MAX_REACH] times the guess is
 * not trusted either.
 *
 * **A joint out of the photo.** The pose model still places a joint it cannot see (a knee below
 * the photo's edge), with a low visibility. The near joint must always be clearly visible
 * ([SURE]); the far one only as visible as the caller asks (`endVisibility`), so a thigh can be
 * slimmed in a photo cut above the knee, along the direction the model expects the leg to run.
 *
 * Everything is measured on the limb itself, so the same value slims a near arm and a far one
 * alike, whatever its angle. A stretch whose joints are hardly visible gets no pushes.
 */
object BodyLimb {

    /**
     * Returns the pushes for [body]'s stretch from landmark [start] to landmark [end]:
     * - at each of [levels] (0 at [start], 1 at [end]), both edges are pushed toward the line by
     *   [share] × that level's weight in [weights] of their distance to it ([share] already holds
     *   the slider's strength, so a negative one pushes outward);
     * - the limb's half-thickness is guessed as [halfShare] of the stretch's length;
     * - each push reaches [radiusShare] of the stretch's length.
     *
     * No pushes when [start] is less than [SURE] visible, [end] less than [endVisibility], or the
     * stretch is shorter than [MIN_SIZE] pixels.
     */
    fun pushesFor(
        body: BodyLandmarks,
        start: Int,
        end: Int,
        levels: FloatArray,
        weights: FloatArray,
        halfShare: Float,
        radiusShare: Float,
        share: Float,
        endVisibility: Float = SURE,
    ): List<WarpPush> {
        val from = body[start]
        val to = body[end]
        if (from.visibility < SURE || to.visibility < endVisibility) return emptyList()
        val axisX = to.x - from.x
        val axisY = to.y - from.y
        val length = hypot(axisX, axisY)
        if (length < MIN_SIZE) return emptyList()
        // Sideways: the limb's direction turned a quarter, so it crosses the limb.
        val acrossX = -axisY / length
        val acrossY = axisX / length
        val guess = length * halfShare
        val radius = length * radiusShare
        val pushes = ArrayList<WarpPush>(levels.size * 2)
        levels.forEachIndexed(
            action = { index, level ->
                val centreX = from.x + axisX * level
                val centreY = from.y + axisY * level
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
                        val move = share * weights[index]
                        pushes += WarpPush(
                            centerX = edgeX,
                            centerY = edgeY,
                            moveX = (centreX - edgeX) * move,
                            moveY = (centreY - edgeY) * move,
                            radius = radius,
                        )
                    },
                )
            },
        )
        return pushes
    }

    /** Both sides of the limb: one way across, then the other. */
    private val SIDES = intArrayOf(1, -1)

    /** A found edge nearer than this share of the guess is not trusted. */
    private const val MIN_REACH = 0.5f

    /** The walk to an edge gives up after this share of the guess. */
    private const val MAX_REACH = 1.8f

    /** How visible (0..1) a joint must be to be trusted as in the photo. */
    const val SURE = 0.5f

    /** Stretches shorter than this, in pixels, are left alone. */
    private const val MIN_SIZE = 2f
}
