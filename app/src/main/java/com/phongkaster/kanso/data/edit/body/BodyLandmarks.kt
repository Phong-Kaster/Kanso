package com.phongkaster.kanso.data.edit.body

/**
 * One person found in a photo: the pose model's 33 landmarks, in pixels of the photo, and the
 * person's [mask] (null when the model gave none).
 *
 * The model is MediaPipe's pose landmarker. Each index always means the same place on a body
 * (11 is always the left shoulder, 23 the left hip), so code can say "point 23" and mean "the left
 * hip" on any photo. [PoseIndex] names the ones Kanso uses.
 *
 * [points] is in the model's order: `points[i]` is landmark `i`.
 */
data class BodyLandmarks(val points: List<BodyPoint>, val mask: BodyMask?) {

    /** Returns landmark [index]. Throws when the model gave fewer points than [index] needs. */
    operator fun get(index: Int): BodyPoint = points[index]
}
