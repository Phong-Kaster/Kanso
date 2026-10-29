package com.phongkaster.kanso.tool.face

import com.phongkaster.kanso.data.edit.face.FaceLandmarks
import com.phongkaster.kanso.data.edit.face.FaceMeshIndex
import com.phongkaster.kanso.data.edit.face.FacePoint
import kotlin.math.sqrt

/**
 * Made-up landmarks for tests, standing in for MediaPipe's face mesh: an upright oval face with
 * only the points Kanso reads placed where a real face has them, and every other point at the
 * face's centre.
 */
object SampleFace {

    /** How many points the face mesh gives, with the iris points. */
    const val POINT_COUNT = 478

    /**
     * Returns an upright oval face centred on ([centerX], [centerY]), [halfWidth] across from the
     * centre to each cheek and [halfHeight] from the centre to the chin. The centre line runs from
     * between the eyes (40% of [halfHeight] above the centre) to the chin; the outline points sit
     * on the oval, from eye height down to near the chin, and the temples on the oval above eye
     * height, the same on both sides.
     */
    fun oval(centerX: Float, centerY: Float, halfWidth: Float, halfHeight: Float): FaceLandmarks {
        val points = MutableList(POINT_COUNT, init = { FacePoint(centerX, centerY) })
        points[FaceMeshIndex.FOREHEAD] = FacePoint(centerX, centerY - halfHeight)
        points[FaceMeshIndex.BETWEEN_EYES] = FacePoint(centerX, centerY - EYE_LEVEL * halfHeight)
        points[FaceMeshIndex.CHIN] = FacePoint(centerX, centerY + halfHeight)
        val steps = FaceMeshIndex.LEFT_OUTLINE.size - 1
        FaceMeshIndex.LEFT_OUTLINE.indices.forEach(
            action = { position ->
                // From eye height down to just above the chin, evenly.
                val down = -EYE_LEVEL + (LOWEST + EYE_LEVEL) * position / steps
                val across = halfWidth * sqrt(1f - down * down)
                val y = centerY + down * halfHeight
                points[FaceMeshIndex.LEFT_OUTLINE[position]] = FacePoint(centerX - across, y)
                points[FaceMeshIndex.RIGHT_OUTLINE[position]] = FacePoint(centerX + across, y)
            },
        )
        // The temples, on the oval above eye height: high on the forehead down to above the eye.
        TEMPLE_HEIGHTS.forEachIndexed(
            action = { position, down ->
                val across = halfWidth * sqrt(1f - down * down)
                val y = centerY + down * halfHeight
                points[FaceMeshIndex.LEFT_TEMPLE[position]] = FacePoint(centerX - across, y)
                points[FaceMeshIndex.RIGHT_TEMPLE[position]] = FacePoint(centerX + across, y)
            },
        )
        // The chin's neighbours and the hairline's, on the oval, each pair mirrored left and right:
        // across is a share of the half width, the height follows from the oval.
        placeOnOval(points, FaceMeshIndex.CHIN_AREA, centerX, centerY, halfWidth, halfHeight, below = true)
        placeOnOval(points, FaceMeshIndex.HAIRLINE, centerX, centerY, halfWidth, halfHeight, below = false)
        return FaceLandmarks(points)
    }

    /**
     * Puts [indices] (a middle point, then pairs left and right) on the oval: the middle at the
     * bottom ([below]) or the top, each pair [ACROSS] of the half width out, at the oval's height.
     */
    private fun placeOnOval(
        points: MutableList<FacePoint>,
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
                points[index] = FacePoint(centerX + side * across * halfWidth, centerY + down * halfHeight)
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
