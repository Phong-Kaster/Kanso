package com.phongkaster.kanso.data.edit.face

/**
 * One push of a local warp, the building block of every reshape (and of Photoshop's Liquify):
 * the pixels around ([centerX], [centerY]) slide by ([moveX], [moveY]), fully at the centre and
 * less and less further out, until nothing moves at [radius] pixels away. All values are in
 * pixels of the photo.
 */
data class WarpPush(
    val centerX: Float,
    val centerY: Float,
    val moveX: Float,
    val moveY: Float,
    val radius: Float,
)
