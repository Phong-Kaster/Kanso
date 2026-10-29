package com.phongkaster.kanso.data.edit.face

/**
 * Every landmark of one face, in pixels of the photo, as the face-landmark model numbers them.
 *
 * The model is MediaPipe's face mesh: 468 points that cover the whole face (outline, brows, eyes,
 * nose, lips), plus 10 iris points (468 to 477) when the model refines the eyes. Each index always
 * means the same place on a face, so code can say "point 152" and mean "the chin" on any photo.
 * [FaceMeshIndex] names the ones Kanso uses.
 *
 * [points] is in the model's order: `points[i]` is landmark `i`.
 */
data class FaceLandmarks(val points: List<FacePoint>) {

    /** Returns landmark [index]. Throws when the model gave fewer points than [index] needs. */
    operator fun get(index: Int): FacePoint = points[index]
}
