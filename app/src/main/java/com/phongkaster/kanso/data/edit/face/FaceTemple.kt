package com.phongkaster.kanso.data.edit.face

import kotlin.math.hypot

/**
 * Temple, the way Meitu's Beautify > Face > Temple does it: the sides of the forehead, between the
 * eyebrows' ends and the hairline, made fuller (a positive amount) or hollower (a negative one),
 * the same way round as Meitu's slider (measured on the Galaxy A13, 2026-10-29: its right end moved
 * the hair's edge at eyebrow height about 25 screen pixels outward, its left end inward).
 *
 * Like [FaceWidth], this object only decides *where* the outline should go and returns the moves
 * as [WarpPush]es; [FaceShape] adds them to the other face tools' and `LocalWarp` moves the pixels.
 *
 * **Where.** The face's outline at temple height ([FaceMeshIndex.LEFT_TEMPLE],
 * [FaceMeshIndex.RIGHT_TEMPLE]: from high on the forehead down to just above the eye) is pushed
 * straight away from the face's centre line (a fuller temple) or toward it, each point by a share
 * of its own distance from the line ([MAX_SHIFT] × its weight in [WEIGHTS]): the hollow above the
 * eye's outer corner moves the most. Each push reaches [RADIUS_SHARE] of the face's width, so the
 * eyes and the cheeks below keep their shape. Everything is measured on the face, so the same
 * value looks alike on any photo size and on a tilted head.
 */
object FaceTemple {

    /**
     * Returns the pushes that reshape [face]'s temples by [amount], from -1 (hollowest) through 0
     * (unchanged) to 1 (fullest). A value outside -1..1 is clamped and NaN counts as 0. An amount of
     * 0, or a face too small to measure, gives no pushes.
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
        val pushes = ArrayList<WarpPush>(FaceMeshIndex.LEFT_TEMPLE.size * 2)
        listOf(FaceMeshIndex.LEFT_TEMPLE, FaceMeshIndex.RIGHT_TEMPLE).forEach(
            action = { side ->
                side.forEachIndexed(
                    action = { position, index ->
                        val point = face[index]
                        // The foot of the perpendicular from the point to the centre line, as in
                        // FaceWidth (the line runs on above the eyes, so `along` is negative here).
                        val along = ((point.x - top.x) * axisX + (point.y - top.y) * axisY) / axisLengthSquared
                        val footX = top.x + along * axisX
                        val footY = top.y + along * axisY
                        // Positive strength moves away from the line: a fuller temple.
                        val share = strength * MAX_SHIFT * WEIGHTS[position]
                        pushes += WarpPush(
                            centerX = point.x,
                            centerY = point.y,
                            moveX = (point.x - footX) * share,
                            moveY = (point.y - footY) * share,
                            radius = radius,
                        )
                    },
                )
            },
        )
        return pushes
    }

    /**
     * How much each temple point moves, from high on the forehead (first) down to just above the
     * eye (last): most in the middle, the temple's hollow.
     */
    private val WEIGHTS = floatArrayOf(0.6f, 1f, 0.8f)

    /**
     * At full strength, the share of its distance to the centre line a temple point moves. Set so
     * the temple's edge moves about 4% of the face's width at ±100, as Meitu's does (measured on the
     * Galaxy A13, 2026-10-29; a first 0.08 moved it about 2.5%).
     */
    private const val MAX_SHIFT = 0.13f

    /** Each push reaches this share of the face's width around its point. */
    private const val RADIUS_SHARE = 0.2f

    /** Faces smaller than this, in pixels, are left alone. */
    private const val MIN_SIZE = 2f
}
