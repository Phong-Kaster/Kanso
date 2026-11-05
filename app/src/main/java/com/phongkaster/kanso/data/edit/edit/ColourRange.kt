package com.phongkaster.kanso.data.edit.edit

/**
 * The nine colour ranges of the HSL tool, in the order they sit round the colour wheel. Each range
 * is a hue centred on [centreDegrees]; the ranges are 40 degrees apart, so together they cover the
 * whole wheel and each one fades into its two neighbours (see [EditHsl.rangeWeight]).
 *
 * The [ordinal] of a range is its index in the lists of [HslAdjustments].
 */
enum class ColourRange(
    /** The hue, in degrees from 0 (inclusive) to 360 (exclusive), at the middle of this range. */
    val centreDegrees: Float,
) {
    /** Red, at 0 degrees. */
    RED(0f),

    /** Orange, at 40 degrees. */
    ORANGE(40f),

    /** Light green (yellow-green), at 80 degrees. */
    LIGHT_GREEN(80f),

    /** Green, at 120 degrees. */
    GREEN(120f),

    /** Light blue (cyan), at 160 degrees. */
    LIGHT_BLUE(160f),

    /** Blue, at 200 degrees. */
    BLUE(200f),

    /** Navy blue (deep blue), at 240 degrees. */
    NAVY_BLUE(240f),

    /** Purple, at 280 degrees. */
    PURPLE(280f),

    /** Pink, at 320 degrees. */
    PINK(320f),
}
