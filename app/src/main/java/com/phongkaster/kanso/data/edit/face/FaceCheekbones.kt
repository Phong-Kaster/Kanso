package com.phongkaster.kanso.data.edit.face

import kotlin.math.hypot

/**
 * Cheekbones, the way Meitu's Beautify > Face > Cheekbones does it: the widest part of the face,
 * just below the eyes, pulled in toward the face's centre line (a slimmer face across the
 * cheekbones, a positive amount) or pushed out (wider, a negative amount), the same way round as
 * Meitu's slider (measured on the Galaxy A13, 2026-10-29: its right end moved the face's edge
 * across the cheekbones about 7 screen px inward on each side, its left end about 5 px outward).
 *
 * Unlike [FaceWidth], which moves the whole outline from the eyes down to the chin, Cheekbones
 * moves only its top three points ([FaceMeshIndex.LEFT_OUTLINE] and [FaceMeshIndex.RIGHT_OUTLINE],
 * positions 0 to 2): the jaw and the chin stay as they are. Each point moves along the shortest
 * path to the centre line by `amount ×` [MAX_SHIFT] `×` its weight of its distance to it, and
 * each push reaches [RADIUS_SHARE] of the face's width. The points are the same as Width's, so
 * [FaceShape] adds the two together point by point.
 */
object FaceCheekbones {

    /**
     * Returns the pushes that reshape [face]'s cheekbones by [amount], from -1 (widest) through 0
     * (unchanged) to 1 (slimmest). A value outside -1..1 is clamped and NaN counts as 0. An amount
     * of 0, or a face too small to measure, gives no pushes.
     */
    fun pushesFor(face: FaceLandmarks, amount: Float): List<WarpPush> {
        val strength = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        if (strength == 0f) return emptyList()
        val top = face[FaceMeshIndex.BETWEEN_EYES]
        val chin = face[FaceMeshIndex.CHIN]
        val axisX = chin.x - top.x
        val axisY = chin.y - top.y
        val axisLengthSquared = axisX * axisX + axisY * axisY
        val leftCheek = face[FaceMeshIndex.LEFT_OUTLINE.first()]
        val rightCheek = face[FaceMeshIndex.RIGHT_OUTLINE.first()]
        val faceWidth = hypot(rightCheek.x - leftCheek.x, rightCheek.y - leftCheek.y)
        if (axisLengthSquared < MIN_SIZE * MIN_SIZE || faceWidth < MIN_SIZE) return emptyList()
        val radius = faceWidth * RADIUS_SHARE
        val pushes = ArrayList<WarpPush>(WEIGHTS.size * 2)
        listOf(FaceMeshIndex.LEFT_OUTLINE, FaceMeshIndex.RIGHT_OUTLINE).forEach(
            action = { side ->
                WEIGHTS.indices.forEach(
                    action = { position ->
                        val point = face[side[position]]
                        // The foot of the perpendicular from the point to the centre line.
                        val along = ((point.x - top.x) * axisX + (point.y - top.y) * axisY) / axisLengthSquared
                        val footX = top.x + along * axisX
                        val footY = top.y + along * axisY
                        // Positive strength moves toward the line: slimmer cheekbones.
                        val share = strength * MAX_SHIFT * WEIGHTS[position]
                        pushes += WarpPush(
                            centerX = point.x,
                            centerY = point.y,
                            moveX = (footX - point.x) * share,
                            moveY = (footY - point.y) * share,
                            radius = radius,
                        )
                    },
                )
            },
        )
        return pushes
    }

    /**
     * How much each of the outline's top points moves, from the cheek at eye height (first) down
     * to the cheekbone's lower edge: most at the cheekbone itself.
     */
    private val WEIGHTS = floatArrayOf(0.7f, 1f, 0.6f)

    /** At full strength, the share of its distance to the centre line a cheekbone point moves. */
    private const val MAX_SHIFT = 0.07f

    /** Each push reaches this share of the face's width around its point. */
    private const val RADIUS_SHARE = 0.2f

    /** Faces smaller than this, in pixels, are left alone. */
    private const val MIN_SIZE = 2f
}
