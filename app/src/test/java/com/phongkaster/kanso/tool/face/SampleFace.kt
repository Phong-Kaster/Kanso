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
     * on the oval, from eye height down to near the chin, the same on both sides.
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
        return FaceLandmarks(points)
    }

    /** Eye height, as a share of the half height above the centre. */
    private const val EYE_LEVEL = 0.4f

    /** The lowest outline point, as a share of the half height below the centre. */
    private const val LOWEST = 0.9f
}
