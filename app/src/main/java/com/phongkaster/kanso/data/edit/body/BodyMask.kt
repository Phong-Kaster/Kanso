package com.phongkaster.kanso.data.edit.body

/**
 * Which pixels of a photo belong to one person: the pose model's segmentation mask.
 *
 * **Why a mask.** Pose landmarks sit at the joints (the middle of the shoulder, the hip, the
 * elbow), not on the body's outline. A waist is slimmed by moving its *edges*, so a tool walks
 * from a joint line outward across this mask until it leaves the person ([BodyEdges]).
 *
 * The mask is [width] x [height] values from 0 (background) to 1 (person), row after row in
 * [confidence]. It may be smaller than the photo, so it also remembers the photo's size
 * ([photoWidth] x [photoHeight]) and [at] takes points in pixels of the photo.
 */
class BodyMask(
    val width: Int,
    val height: Int,
    private val confidence: FloatArray,
    private val photoWidth: Int,
    private val photoHeight: Int,
) {

    /**
     * How sure the model is (0 to 1) that the photo's pixel at ([x], [y]) shows the person. A
     * point outside the photo is background (0).
     */
    fun at(x: Float, y: Float): Float {
        if (photoWidth <= 0 || photoHeight <= 0) return 0f
        val column = (x * width / photoWidth).toInt()
        val row = (y * height / photoHeight).toInt()
        if (x < 0f || y < 0f || column !in 0 until width || row !in 0 until height) return 0f
        return confidence[row * width + column]
    }
}
