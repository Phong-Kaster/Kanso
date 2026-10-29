package com.phongkaster.kanso.tool.body

import com.phongkaster.kanso.data.edit.body.BodyLandmarks
import com.phongkaster.kanso.data.edit.body.BodyMask
import com.phongkaster.kanso.data.edit.body.BodyPoint
import com.phongkaster.kanso.data.edit.body.PoseIndex

/**
 * Made-up bodies for tests, standing in for MediaPipe's pose landmarker: an upright figure with
 * only the points Kanso reads placed where a real body has them, and every other point at the
 * torso's centre.
 */
object SampleBody {

    /**
     * Returns an upright figure on the vertical line x = [centerX]: shoulders [shoulderSpread]
     * apart at y = [top], hips [hipSpread] apart [length] lower, arms hanging straight down from
     * the shoulders (elbows [ELBOW_DROP], wrists [WRIST_DROP] of [length] below them), every point
     * [visibility] visible.
     * With [torsoHalf] set, it also gets a [photoWidth] x [photoHeight] mask in which the person
     * is the band [torsoHalf] to each side of the centre line, from the shoulders to the hips, so
     * the waist's edges are exactly at `centerX ± torsoHalf`.
     */
    fun upright(
        centerX: Float,
        top: Float,
        length: Float,
        shoulderSpread: Float,
        hipSpread: Float,
        visibility: Float = 1f,
        torsoHalf: Float? = null,
        photoWidth: Int = 0,
        photoHeight: Int = 0,
    ): BodyLandmarks {
        val points = MutableList(PoseIndex.COUNT, init = { BodyPoint(centerX, top + length / 2f, visibility) })
        points[PoseIndex.LEFT_SHOULDER] = BodyPoint(centerX + shoulderSpread / 2f, top, visibility)
        points[PoseIndex.RIGHT_SHOULDER] = BodyPoint(centerX - shoulderSpread / 2f, top, visibility)
        // Arms hang straight down from the shoulders: elbows half way to the hips, wrists below them.
        points[PoseIndex.LEFT_ELBOW] = BodyPoint(centerX + shoulderSpread / 2f, top + length * ELBOW_DROP, visibility)
        points[PoseIndex.RIGHT_ELBOW] = BodyPoint(centerX - shoulderSpread / 2f, top + length * ELBOW_DROP, visibility)
        points[PoseIndex.LEFT_WRIST] = BodyPoint(centerX + shoulderSpread / 2f, top + length * WRIST_DROP, visibility)
        points[PoseIndex.RIGHT_WRIST] = BodyPoint(centerX - shoulderSpread / 2f, top + length * WRIST_DROP, visibility)
        points[PoseIndex.LEFT_HIP] = BodyPoint(centerX + hipSpread / 2f, top + length, visibility)
        points[PoseIndex.RIGHT_HIP] = BodyPoint(centerX - hipSpread / 2f, top + length, visibility)
        val mask = torsoHalf?.let(block = { half -> bandMask(centerX, top, length, half, photoWidth, photoHeight) })
        return BodyLandmarks(points, mask)
    }

    /**
     * A [width] x [height] mask (the photo's own size) that is 1 inside the band [half] to each
     * side of x = [centerX], from y = [top] to [top] + [length], and 0 everywhere else.
     */
    private fun bandMask(centerX: Float, top: Float, length: Float, half: Float, width: Int, height: Int): BodyMask {
        val values = FloatArray(width * height)
        for (y in 0 until height) {
            if (y < top || y > top + length) continue
            for (x in 0 until width) {
                if (x >= centerX - half && x <= centerX + half) values[y * width + x] = 1f
            }
        }
        return BodyMask(width, height, values, width, height)
    }

    /** How far below the shoulders the elbows hang, as a share of the shoulders-to-hips length. */
    private const val ELBOW_DROP = 0.55f

    /** How far below the shoulders the wrists hang, as a share of the shoulders-to-hips length. */
    private const val WRIST_DROP = 1.1f
}
