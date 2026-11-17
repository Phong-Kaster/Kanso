package com.astronex.photoedit.core.reshape

import kotlin.math.sqrt

/**
 * Made-up landmarks for tests, standing in for MediaPipe's face mesh: an upright oval face with
 * only the points the reshapes read placed where a real face has them, and every other point at the
 * face's centre.
 */
internal object MadeUpFace {

    /** How many points the face mesh gives, with the iris points. */
    const val POINT_COUNT = 478

    /**
     * Returns an upright oval face centred on ([centerX], [centerY]), [halfWidth] across from the
     * centre to each cheek and [halfHeight] from the centre to the chin. The centre line runs from
     * between the eyes (40% of [halfHeight] above the centre) to the chin; the outline points sit
     * on the oval, from eye height down to near the chin, and the temples on the oval above eye
     * height, the same on both sides.
     */
    fun oval(centerX: Float, centerY: Float, halfWidth: Float, halfHeight: Float): FaceMesh {
        val points = MutableList(POINT_COUNT, init = { MeshPoint(centerX, centerY) })
        points[FaceMeshLandmark.FOREHEAD] = MeshPoint(centerX, centerY - halfHeight)
        points[FaceMeshLandmark.BETWEEN_EYES] = MeshPoint(centerX, centerY - EYE_LEVEL * halfHeight)
        points[FaceMeshLandmark.CHIN] = MeshPoint(centerX, centerY + halfHeight)
        val steps = FaceMeshLandmark.LEFT_OUTLINE.size - 1
        FaceMeshLandmark.LEFT_OUTLINE.indices.forEach(
            action = { position ->
                // From eye height down to just above the chin, evenly.
                val down = -EYE_LEVEL + (LOWEST + EYE_LEVEL) * position / steps
                val across = halfWidth * sqrt(1f - down * down)
                val y = centerY + down * halfHeight
                points[FaceMeshLandmark.LEFT_OUTLINE[position]] = MeshPoint(centerX - across, y)
                points[FaceMeshLandmark.RIGHT_OUTLINE[position]] = MeshPoint(centerX + across, y)
            },
        )
        // The temples, on the oval above eye height: high on the forehead down to above the eye.
        TEMPLE_HEIGHTS.forEachIndexed(
            action = { position, down ->
                val across = halfWidth * sqrt(1f - down * down)
                val y = centerY + down * halfHeight
                points[FaceMeshLandmark.LEFT_TEMPLE[position]] = MeshPoint(centerX - across, y)
                points[FaceMeshLandmark.RIGHT_TEMPLE[position]] = MeshPoint(centerX + across, y)
            },
        )
        // The chin's neighbours and the hairline's, on the oval, each pair mirrored left and right:
        // across is a share of the half width, the height follows from the oval.
        placeOnOval(points, FaceMeshLandmark.CHIN_AREA, centerX, centerY, halfWidth, halfHeight, below = true)
        placeOnOval(points, FaceMeshLandmark.HAIRLINE, centerX, centerY, halfWidth, halfHeight, below = false)
        // Two more outline pairs Smooth and Double chin read: above the cheek at eye height (127,
        // 356), and between the outline's last point and the chin's outer neighbours (149, 378).
        listOf(Triple(127, 356, -0.55f), Triple(149, 378, 0.88f)).forEach(
            action = { (left, right, down) ->
                val across = halfWidth * sqrt(1f - down * down)
                val y = centerY + down * halfHeight
                points[left] = MeshPoint(centerX - across, y)
                points[right] = MeshPoint(centerX + across, y)
            },
        )
        placeFeatures(points, centerX, centerY, halfWidth, halfHeight)
        return FaceMesh(points)
    }

    /**
     * Puts the eyes, brows, nose and mouth where a front-facing face has them, as shares of the
     * half width (across) and half height (down): each eye 0.4 of the half width from corner to
     * corner, centred 0.45 out at eye height, its brow 0.24 above; the nose from the bridge
     * (0.3 above the centre) to the tip (0.15 below), its wings 0.56 apart; the mouth 0.9 wide,
     * 0.42 below the centre. Mirrored left and right.
     */
    private fun placeFeatures(points: MutableList<MeshPoint>, centerX: Float, centerY: Float, halfWidth: Float, halfHeight: Float) {
        val eyeY = centerY - EYE_LEVEL * halfHeight
        // Each side: sign -1 is the photo's left, +1 its right.
        listOf(-1f to EyeSide.LEFT, 1f to EyeSide.RIGHT).forEach(
            action = { (sign, side) ->
                val eyeX = centerX + sign * 0.45f * halfWidth
                points[side.iris] = MeshPoint(eyeX, eyeY)
                points[side.outer] = MeshPoint(centerX + sign * 0.65f * halfWidth, eyeY)
                points[side.inner] = MeshPoint(centerX + sign * 0.25f * halfWidth, eyeY)
                points[side.upperLid] = MeshPoint(eyeX, eyeY - 0.08f * halfWidth)
                points[side.lowerLid] = MeshPoint(eyeX, eyeY + 0.08f * halfWidth)
                points[side.brow] = MeshPoint(eyeX, eyeY - 0.24f * halfWidth)
                points[side.noseWing] = MeshPoint(centerX + sign * 0.28f * halfWidth, centerY + 0.12f * halfHeight)
                points[side.noseAlar] = MeshPoint(centerX + sign * 0.3f * halfWidth, centerY + 0.18f * halfHeight)
                points[side.mouthCorner] = MeshPoint(centerX + sign * 0.45f * halfWidth, centerY + MOUTH_LEVEL * halfHeight)
            },
        )
        points[NOSE_BRIDGE] = MeshPoint(centerX, centerY - 0.3f * halfHeight)
        points[NOSE_TIP] = MeshPoint(centerX, centerY + 0.15f * halfHeight)
        val mouthY = centerY + MOUTH_LEVEL * halfHeight
        points[UPPER_LIP_TOP] = MeshPoint(centerX, mouthY - 0.1f * halfHeight)
        points[UPPER_LIP_INNER] = MeshPoint(centerX, mouthY - 0.01f * halfHeight)
        points[LOWER_LIP_INNER] = MeshPoint(centerX, mouthY + 0.01f * halfHeight)
        points[LOWER_LIP_BOTTOM] = MeshPoint(centerX, mouthY + 0.13f * halfHeight)
    }

    /**
     * The face-mesh numbers of one side's eye, brow, nose wing and mouth corner, by the photo's
     * side: the iris centre, the outer and inner corners, the lids, the brow's middle, the nose's
     * wing and alar base, and the mouth's corner.
     */
    enum class EyeSide(
        val iris: Int,
        val outer: Int,
        val inner: Int,
        val upperLid: Int,
        val lowerLid: Int,
        val brow: Int,
        val noseWing: Int,
        val noseAlar: Int,
        val mouthCorner: Int,
    ) {
        /** The photo's left. */
        LEFT(iris = 468, outer = 33, inner = 133, upperLid = 159, lowerLid = 145, brow = 105, noseWing = 129, noseAlar = 64, mouthCorner = 61),

        /** The photo's right. */
        RIGHT(iris = 473, outer = 263, inner = 362, upperLid = 386, lowerLid = 374, brow = 334, noseWing = 358, noseAlar = 294, mouthCorner = 291),
    }

    /** The nose's bridge, between the eyes and the tip. */
    const val NOSE_BRIDGE = 6

    /** The nose's tip. */
    const val NOSE_TIP = 1

    /** The top of the upper lip, on the centre line. */
    const val UPPER_LIP_TOP = 0

    /** The upper lip's inner edge, on the centre line. */
    const val UPPER_LIP_INNER = 13

    /** The lower lip's inner edge, on the centre line. */
    const val LOWER_LIP_INNER = 14

    /** The bottom of the lower lip, on the centre line. */
    const val LOWER_LIP_BOTTOM = 17

    /** The mouth's height, as a share of the half height below the centre. */
    private const val MOUTH_LEVEL = 0.42f

    /**
     * Puts [indices] (a middle point, then pairs left and right) on the oval: the middle at the
     * bottom ([below]) or the top, each pair [ACROSS] of the half width out, at the oval's height.
     */
    private fun placeOnOval(
        points: MutableList<MeshPoint>,
        indices: List<Int>,
        centerX: Float,
        centerY: Float,
        halfWidth: Float,
        halfHeight: Float,
        below: Boolean,
    ) {
        val sign = if (below) 1f else -1f
        indices.forEachIndexed(
            action = { position, index ->
                val across = ACROSS[position]
                val down = sign * sqrt(1f - across * across)
                val side = if (position % 2 == 1) -1f else 1f
                points[index] = MeshPoint(centerX + side * across * halfWidth, centerY + down * halfHeight)
            },
        )
    }

    /** How far out each chin and hairline point sits, as a share of the half width, in list order. */
    private val ACROSS = floatArrayOf(0f, 0.25f, 0.25f, 0.5f, 0.5f)

    /** Where the temple points sit, as a share of the half height (negative: above the centre). */
    private val TEMPLE_HEIGHTS = floatArrayOf(-0.82f, -0.68f, -0.52f)

    /** Eye height, as a share of the half height above the centre. */
    private const val EYE_LEVEL = 0.4f

    /** The lowest outline point, as a share of the half height below the centre. */
    private const val LOWEST = 0.9f
}
