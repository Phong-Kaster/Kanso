package com.phongkaster.kanso.data.edit.face

/**
 * Where the face to look at sits in a photo, as fractions of the photo's width and height (0 is
 * the left or top edge, 1 the right or bottom edge), so it fits the photo at any size: the shown
 * copy, the 2000-pixel working copy and the edited result all share it.
 *
 * The tool screen uses it to zoom the photo card in on the face, as Meitu does when its face tools
 * open, so a small change such as Face width is easy to see.
 */
data class FaceFocus(val left: Float, val top: Float, val right: Float, val bottom: Float) {

    /** The face's width, as a fraction of the photo's. */
    val width: Float get() = right - left

    /** The face's height, as a fraction of the photo's. */
    val height: Float get() = bottom - top

    /** The face's centre across, as a fraction of the photo's width. */
    val centerX: Float get() = (left + right) / 2f

    /** The face's centre down, as a fraction of the photo's height. */
    val centerY: Float get() = (top + bottom) / 2f
}
