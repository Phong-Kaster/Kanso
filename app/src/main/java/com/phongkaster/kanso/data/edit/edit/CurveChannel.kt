package com.phongkaster.kanso.data.edit.edit

/**
 * The channels the Curves tool can bend. [RGB] is the master curve that moves all three colours the
 * same way; the next three bend one colour on its own, to fix or add a colour cast; [LIGHTNESS] bends
 * only how light each pixel is (the L of HSL) and leaves hue and saturation alone.
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

    /** The lightness (L of HSL), applied last, after the four colour curves. */
    LIGHTNESS,
}
