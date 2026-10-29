package com.phongkaster.kanso.data.edit.face

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Face lift, the way Meitu's Beautify > Face > Lift does it: the lower half of the face's outline
 * (the jowls and the jaw's corners) is raised toward the eyes and drawn a little in toward the
 * centre line, so the jawline looks tighter and higher, as after a face-lift (a positive amount).
 * A negative amount does the reverse: the jowls drop and spread out, a heavier, longer jaw. The
 * slider runs -1..1 through 0 (unchanged), the same way round as Meitu's Lift slider, which is
 * two-sided like its Width slider (seen on the vivo V2408A, 2026-10-29).
 *
 * Like [FaceWidth], this object only decides *where* the outline should go and returns the moves
 * as [WarpPush]es, one per outline point that moves, centred on the same points [FaceWidth] uses
 * and with the same reach, so [FaceShape] can add the two together point by point.
 *
 * Everything is measured on the face itself, never in fixed pixels, so the same slider value lifts
 * a small face and a large one alike, and a tilted head along its own tilt:
 * - **up** is the face's centre line, from the chin ([FaceMeshIndex.CHIN]) toward the point
 *   between the eyes ([FaceMeshIndex.BETWEEN_EYES]), so a tilted face is lifted along its tilt;
 * - each outline point rises a share of that line's length, and moves in a share of its own
 *   distance from the line;
 * - the reach of each push is a share of the face's width (cheek to cheek at eye height).
 *
 * The cheek at eye height does not move at all, so the eyes keep their place; the jaw's corner
 * and the jowls under it move the most, and the points near the chin less, so the chin keeps its
 * shape.
 */
object FaceLift {

    /**
     * Returns the pushes that lift [face] by [amount], from -1 (the jaw dropped the most) through 0
     * (unchanged) to 1 (lifted the most). A value outside -1..1 is clamped and NaN counts as 0. An
     * amount of 0, or a face too small to measure, gives no pushes at all.
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
        // Under a pixel or two there is no face to lift, and dividing by it would blow up.
        if (axisLengthSquared < MIN_SIZE * MIN_SIZE || faceWidth < MIN_SIZE) return emptyList()
        val axisLength = sqrt(axisLengthSquared)
        // One step "up" the face, in pixels: from the chin toward the eyes, as a unit vector.
        val upX = -axisX / axisLength
        val upY = -axisY / axisLength
        val radius = faceWidth * RADIUS_SHARE
        val pushes = ArrayList<WarpPush>(FaceMeshIndex.LEFT_OUTLINE.size * 2)
        listOf(FaceMeshIndex.LEFT_OUTLINE, FaceMeshIndex.RIGHT_OUTLINE).forEach(
            action = { side ->
                side.forEachIndexed(
                    action = { position, index ->
                        val rise = RISE_WEIGHTS[position]
                        val tighten = TIGHTEN_WEIGHTS[position]
                        // A point that does not move gets no push: an empty push would still
                        // count in LocalWarp's weights and damp its neighbours' moves.
                        if (rise == 0f && tighten == 0f) return@forEachIndexed
                        val point = face[index]
                        // The foot of the perpendicular from the point to the centre line, as in
                        // FaceWidth: the way "in" for this point.
                        val along = ((point.x - top.x) * axisX + (point.y - top.y) * axisY) / axisLengthSquared
                        val footX = top.x + along * axisX
                        val footY = top.y + along * axisY
                        // A negative strength turns both moves round: down the face and outward.
                        val up = strength * MAX_RISE * axisLength * rise
                        val inward = strength * MAX_TIGHTEN * tighten
                        pushes += WarpPush(
                            centerX = point.x,
                            centerY = point.y,
                            moveX = upX * up + (footX - point.x) * inward,
                            moveY = upY * up + (footY - point.y) * inward,
                            radius = radius,
                        )
                    },
                )
            },
        )
        return pushes
    }

    /**
     * How far each outline position rises, from the cheek at eye height (first, not at all) down
     * to near the chin (last). The jaw's corner and the jowls rise the most.
     */
    private val RISE_WEIGHTS = floatArrayOf(0f, 0.25f, 0.6f, 1f, 1f, 0.8f, 0.5f)

    /**
     * How far each outline position is drawn in toward the centre line, in the same order: a
     * little at the cheek, most at the jowls, where a lift tightens the skin.
     */
    private val TIGHTEN_WEIGHTS = floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 0.9f, 0.7f, 0.4f)

    /** At full strength, the share of the eyes-to-chin length the most lifted points rise. */
    private const val MAX_RISE = 0.05f

    /** At full strength, the share of its distance to the centre line the most tightened point moves in. */
    private const val MAX_TIGHTEN = 0.06f

    /** Each push reaches this share of the face's width around its outline point, as in [FaceWidth]. */
    private const val RADIUS_SHARE = 0.22f

    /** Faces smaller than this, in pixels, are left alone. */
    private const val MIN_SIZE = 2f
}
