package com.astronex.photoedit.core.reshape

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Moves chosen face points by set amounts in the face's own frame: down the face (along the centre
 * line from between the eyes to the chin) and in toward that line. The move behind Chin tip, Jaw
 * angle, Smooth and Double chin, whose numbers were read off Meitu in exactly that frame.
 *
 * Both parts of a move are shares of the eyes-to-chin length, so a tilted head is reshaped along its
 * own tilt and a larger photo by the same share. "In" points from the landmark straight toward the
 * centre line; a landmark on the line (the chin) has no "in", so only its "down" part moves it.
 *
 * @author Phong-Kaster
 */
internal object FaceShift {

    /**
     * One point's move at full strength.
     *
     * @param index The face-mesh landmark ([FaceMesh]) the push sits on.
     * @param down The share of the eyes-to-chin length the point moves down the face (negative: up).
     * @param inward The share of that length it moves toward the centre line (negative: away).
     * @author Phong-Kaster
     */
    data class Shift(val index: Int, val down: Float, val inward: Float)

    /**
     * Returns the same move on a left landmark and its right mirror: the face's sides move alike,
     * each toward the centre line.
     *
     * @author Phong-Kaster
     */
    fun mirrored(left: Int, right: Int, down: Float, inward: Float): List<Shift> =
        listOf(Shift(left, down, inward), Shift(right, down, inward))

    /**
     * Returns one sliding push per [shifts], each moving its landmark by `strength ×` its shift and
     * reaching [radiusShare] of the face's width. A push can sit [offset] of the eyes-to-chin length
     * further down the face than its landmark (negative: further up), for skin the mesh does not
     * reach (below the jaw). [strength] is already clamped to -1..1; empty when it is 0 or the face is
     * too small to measure.
     *
     * @author Phong-Kaster
     */
    fun pushesFor(
        face: FaceMesh,
        shifts: List<Shift>,
        strength: Float,
        radiusShare: Float,
        offset: Float = 0f,
    ): List<WarpHandle> {
        if (strength == 0f) return emptyList()
        val top = face[FaceMeshLandmark.BETWEEN_EYES]
        val chin = face[FaceMeshLandmark.CHIN]
        val axisX = chin.x - top.x
        val axisY = chin.y - top.y
        val length = sqrt(axisX * axisX + axisY * axisY)
        val leftCheek = face[FaceMeshLandmark.LEFT_OUTLINE.first()]
        val rightCheek = face[FaceMeshLandmark.RIGHT_OUTLINE.first()]
        val faceWidth = hypot(rightCheek.x - leftCheek.x, rightCheek.y - leftCheek.y)
        if (length < MIN_SIZE || faceWidth < MIN_SIZE) return emptyList()
        // One step down the face, as a unit vector.
        val downX = axisX / length
        val downY = axisY / length
        val radius = faceWidth * radiusShare
        return shifts.map(
            transform = { shift ->
                val point = face[shift.index]
                // The point's offset from the centre line, at right angles to it.
                val relX = point.x - top.x
                val relY = point.y - top.y
                val along = relX * downX + relY * downY
                val sideX = relX - along * downX
                val sideY = relY - along * downY
                val side = hypot(sideX, sideY)
                val inX = if (side > ON_LINE) -sideX / side else 0f
                val inY = if (side > ON_LINE) -sideY / side else 0f
                val moveDown = strength * shift.down * length
                val moveIn = strength * shift.inward * length
                WarpHandle(
                    centerX = point.x + downX * offset * length,
                    centerY = point.y + downY * offset * length,
                    moveX = downX * moveDown + inX * moveIn,
                    moveY = downY * moveDown + inY * moveIn,
                    radius = radius,
                )
            },
        )
    }

    /** Faces smaller than this, in pixels, are left alone. */
    private const val MIN_SIZE = 2f

    /** A landmark closer than this to the centre line, in pixels, counts as on it: it has no "in". */
    private const val ON_LINE = 0.5f
}
