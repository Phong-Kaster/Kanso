package com.phongkaster.kanso.data.edit.face

/**
 * One push of a local warp, the building block of every reshape (and of Photoshop's Liquify). A
 * push does one of two things around ([centerX], [centerY]), fading out until nothing changes at
 * [radius] pixels away:
 * - **slide**: the pixels move by ([moveX], [moveY]), fully at the centre and less and less
 *   further out (Face width, Lift, Waist, Arms);
 * - **grow**: with [grow] not 0, the pixels spread out from the centre (positive, like a
 *   magnifying glass: what is there looks bigger) or gather toward it (negative: smaller), most
 *   half way out and not at all at the centre or the edge (Chest). A growing push leaves
 *   [moveX] and [moveY] at 0.
 *
 * All values are in pixels of the photo, except [grow], a share: 0.2 makes the middle of the
 * circle about a quarter bigger.
 */
data class WarpPush(
    val centerX: Float,
    val centerY: Float,
    val moveX: Float,
    val moveY: Float,
    val radius: Float,
    val grow: Float = 0f,
)
