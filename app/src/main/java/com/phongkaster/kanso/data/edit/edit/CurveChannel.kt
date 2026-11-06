package com.phongkaster.kanso.data.edit.edit

/**
 * The channels the Curves tool can bend. [RGB] is the master curve that moves all three colours the
 * same way; the other three bend one colour on its own, to fix or add a colour cast.
 */
enum class CurveChannel {

    /** The master curve, applied to red, green and blue alike after their own curves. */
    RGB,

    /** The red channel only. */
    RED,

    /** The green channel only. */
    GREEN,

    /** The blue channel only. */
    BLUE,
}
