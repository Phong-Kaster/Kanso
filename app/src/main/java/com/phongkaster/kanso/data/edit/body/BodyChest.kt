package com.phongkaster.kanso.data.edit.body

import com.phongkaster.kanso.data.edit.face.WarpPush
import kotlin.math.hypot

/**
 * Chest, the way Meitu's Beautify > Body > Reshape > Chest does it: the bust made fuller (a
 * positive amount) or flatter (a negative one), the same way round as Meitu's slider (seen on the
 * Galaxy A13, 2026-10-29: its right end made the chest clearly bigger, its left end flatter).
 *
 * Unlike Waist and Arms, nothing slides: each side of the chest gets one **growing** [WarpPush]
 * (`grow`), which spreads the pixels out from a point like a magnifying glass, or gathers them in
 * for a negative amount, fading to nothing at its edge.
 *
 * **Where.** The pose model has no chest point, so each centre is placed from the **shoulders**
 * alone, which are in nearly every photo, even a portrait cut at the chest: [DROP] of the
 * shoulders' spread straight down the body from the shoulder line, and [INWARD] of the way from
 * the middle of the shoulders out toward each shoulder. "Down the body" is across the shoulder
 * line, on the side of the hips when they are visible, else away from the nose, else down the
 * photo. Each push reaches [RADIUS_SHARE] of the shoulders' spread.
 *
 * Shoulders that are hardly visible give no pushes.
 */
object BodyChest {

    /**
     * Returns the two pushes that reshape [body]'s chest by [amount], from -1 (flattest) through
     * 0 (unchanged) to 1 (fullest). A value outside -1..1 is clamped and NaN counts as 0.
     */
    fun pushesFor(body: BodyLandmarks, amount: Float): List<WarpPush> {
        val strength = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        if (strength == 0f || body.points.size < PoseIndex.COUNT) return emptyList()
        val left = body[PoseIndex.LEFT_SHOULDER]
        val right = body[PoseIndex.RIGHT_SHOULDER]
        if (left.visibility < MIN_VISIBILITY || right.visibility < MIN_VISIBILITY) return emptyList()
        val spread = hypot(right.x - left.x, right.y - left.y)
        if (spread < MIN_SIZE) return emptyList()
        val middleX = (left.x + right.x) / 2f
        val middleY = (left.y + right.y) / 2f
        // Across the shoulder line: the line's direction turned a quarter; then pointed down the body.
        var downX = -(right.y - left.y) / spread
        var downY = (right.x - left.x) / spread
        if (downX * towardX(body, middleX) + downY * towardY(body, middleY) < 0f) {
            downX = -downX
            downY = -downY
        }
        val radius = spread * RADIUS_SHARE
        val grow = strength * MAX_GROW
        return listOf(left, right).map(
            transform = { shoulder ->
                WarpPush(
                    centerX = middleX + (shoulder.x - middleX) * INWARD + downX * spread * DROP,
                    centerY = middleY + (shoulder.y - middleY) * INWARD + downY * spread * DROP,
                    moveX = 0f,
                    moveY = 0f,
                    radius = radius,
                    grow = grow,
                )
            },
        )
    }

    /**
     * Across the photo, which way is "down the body" from the shoulders' middle at [middleX]: toward
     * the hips when both are visible, else away from the nose when it is, else 0 (no preference).
     */
    private fun towardX(body: BodyLandmarks, middleX: Float): Float = when {
        hipsVisible(body) -> (body[PoseIndex.LEFT_HIP].x + body[PoseIndex.RIGHT_HIP].x) / 2f - middleX
        body[PoseIndex.NOSE].visibility >= MIN_VISIBILITY -> middleX - body[PoseIndex.NOSE].x
        else -> 0f
    }

    /**
     * Down the photo, which way is "down the body" from the shoulders' middle at [middleY], as
     * [towardX]; with neither hips nor nose, straight down the photo (1).
     */
    private fun towardY(body: BodyLandmarks, middleY: Float): Float = when {
        hipsVisible(body) -> (body[PoseIndex.LEFT_HIP].y + body[PoseIndex.RIGHT_HIP].y) / 2f - middleY
        body[PoseIndex.NOSE].visibility >= MIN_VISIBILITY -> middleY - body[PoseIndex.NOSE].y
        else -> 1f
    }

    /** Whether both hips are visible enough to say which way the body runs. */
    private fun hipsVisible(body: BodyLandmarks): Boolean =
        body[PoseIndex.LEFT_HIP].visibility >= MIN_VISIBILITY && body[PoseIndex.RIGHT_HIP].visibility >= MIN_VISIBILITY

    /** How far below the shoulder line each centre sits, as a share of the shoulders' spread. */
    private const val DROP = 0.45f

    /** How far out from the shoulders' middle toward each shoulder each centre sits (0 middle, 1 shoulder). */
    private const val INWARD = 0.6f

    /** Each push reaches this share of the shoulders' spread around its centre. */
    private const val RADIUS_SHARE = 0.32f

    /** At full strength, the grow of each push (see `WarpPush.grow`). */
    private const val MAX_GROW = 0.22f

    /** A shoulder less visible than this (0..1) means the chest is not in the photo. */
    private const val MIN_VISIBILITY = 0.5f

    /** Bodies smaller than this, in pixels, are left alone. */
    private const val MIN_SIZE = 2f
}
