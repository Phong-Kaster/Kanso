package com.phongkaster.kanso.data.edit.face

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Moves a group of face points along the face's own up-and-down line, for the tools that make a
 * part of the face longer or shorter: Chin length (the chin up or down) and Hairline (the hairline
 * down or up).
 *
 * "Down" is the face's centre line, from between the eyes ([FaceMeshIndex.BETWEEN_EYES]) to the
 * chin ([FaceMeshIndex.CHIN]), so a tilted head moves along its own tilt. Each point in `points`
 * moves `amount × maxShift × weight` of that line's length (the eyes-to-chin length) down the face
 * (a negative `amount` moves it up), and each push reaches `radiusShare` of the face's width. A
 * push can sit `offset` of that length further down the face than its landmark (negative: further
 * up), for a feature the landmarks do not reach: the face mesh stops on the forehead's skin, below
 * the real hairline. A face too small to measure, or an amount of 0, gives no pushes.
 */
object FaceVertical {

    /**
     * Returns the pushes that move [points] of [face] (landmark numbers, with [weights] in the same
     * order) down the face by [amount] (-1..1, clamped; NaN counts as 0), each by
     * `amount × maxShift × weight` of the eyes-to-chin length, reaching [radiusShare] of the face's
     * width, each centred [offset] of that length down the face from its landmark (0: on it).
     */
    fun pushesFor(
        face: FaceLandmarks,
        amount: Float,
        points: List<Int>,
        weights: FloatArray,
        maxShift: Float,
        radiusShare: Float,
        offset: Float = 0f,
    ): List<WarpPush> {
        val strength = if (amount.isNaN()) 0f else amount.coerceIn(-1f, 1f)
        if (strength == 0f) return emptyList()
        val top = face[FaceMeshIndex.BETWEEN_EYES]
        val chin = face[FaceMeshIndex.CHIN]
        val axisX = chin.x - top.x
        val axisY = chin.y - top.y
        val length = sqrt(axisX * axisX + axisY * axisY)
        val leftCheek = face[FaceMeshIndex.LEFT_OUTLINE.first()]
        val rightCheek = face[FaceMeshIndex.RIGHT_OUTLINE.first()]
        val faceWidth = hypot(rightCheek.x - leftCheek.x, rightCheek.y - leftCheek.y)
        if (length < MIN_SIZE || faceWidth < MIN_SIZE) return emptyList()
        // One step down the face, as a unit vector.
        val downX = axisX / length
        val downY = axisY / length
        val radius = faceWidth * radiusShare
        return points.mapIndexed(
            transform = { position, index ->
                val point = face[index]
                val move = strength * maxShift * length * weights[position]
                WarpPush(
                    centerX = point.x + downX * offset * length,
                    centerY = point.y + downY * offset * length,
                    moveX = downX * move,
                    moveY = downY * move,
                    radius = radius,
                )
            },
        )
    }

    /** Faces smaller than this, in pixels, are left alone. */
    private const val MIN_SIZE = 2f
}
