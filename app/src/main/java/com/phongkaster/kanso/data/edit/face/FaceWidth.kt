package com.phongkaster.kanso.data.edit.face

import kotlin.math.hypot

/**
 * Face width, the way Meitu's Beautify > Face > Width does it: the outline of the cheeks and jaw
 * is pulled toward the face's centre line (a narrower face, a positive amount) or pushed away from
 * it (a wider face, a negative amount), the same way round as Meitu's slider (measured on the
 * Galaxy A13: its left end widens the jaw, its right end narrows it).
 *
 * This object only decides *where* the outline should go; it returns the moves as [WarpPush]es and
 * [LocalWarp] then moves the pixels. Splitting the two keeps this part plain maths, tested on the
 * JVM with made-up landmarks.
 *
 * Everything is measured on the face itself, never in fixed pixels, so the same slider value
 * gives the same look on a 500-pixel photo and on a 2000-pixel one, and on a tilted head as on a
 * straight one:
 * - the **centre line** runs from between the eyes ([FaceMeshIndex.BETWEEN_EYES]) to the chin
 *   ([FaceMeshIndex.CHIN]), so it tilts with the head;
 * - each outline point moves along the shortest path to that line, a share of its own distance
 *   from it;
 * - the reach of each push is a share of the face's width (cheek to cheek at eye height).
 */
object FaceWidth {

    /**
     * Returns the pushes that reshape [face] by [amount], from -1 (widest) through 0 (unchanged)
     * to 1 (narrowest, a smaller face), as Meitu's Width slider. A value outside -1..1 is clamped and NaN counts as 0.
     * An amount of 0, or a face too small to measure, gives no pushes at all.
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
        // Under a pixel or two there is no face to reshape, and dividing by it would blow up.
        if (axisLengthSquared < MIN_SIZE * MIN_SIZE || faceWidth < MIN_SIZE) return emptyList()
        val radius = faceWidth * RADIUS_SHARE
        val pushes = ArrayList<WarpPush>(FaceMeshIndex.LEFT_OUTLINE.size * 2)
        listOf(FaceMeshIndex.LEFT_OUTLINE, FaceMeshIndex.RIGHT_OUTLINE).forEach(
            action = { side ->
                side.forEachIndexed(
                    action = { position, index ->
                        val point = face[index]
                        // The foot of the perpendicular from the point to the centre line: project
                        // the point onto the line's direction, then walk that far from `top`.
                        val along = ((point.x - top.x) * axisX + (point.y - top.y) * axisY) / axisLengthSquared
                        val footX = top.x + along * axisX
                        val footY = top.y + along * axisY
                        // Positive strength moves toward the line (narrower), negative away (wider).
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
     * How much each outline position moves, from the cheek at eye height (first) down to near the
     * chin (last). The cheeks and the jaw's corner move the most; the top of the cheek and the
     * chin move less, so the eyes and the chin keep their shape.
     */
    private val WEIGHTS = floatArrayOf(0.5f, 0.8f, 1f, 1f, 0.9f, 0.7f, 0.4f)

    /** At full strength, the share of its distance to the centre line an outline point moves. */
    private const val MAX_SHIFT = 0.12f

    /** Each push reaches this share of the face's width around its outline point. */
    private const val RADIUS_SHARE = 0.22f

    /** Faces smaller than this, in pixels, are left alone. */
    private const val MIN_SIZE = 2f
}
