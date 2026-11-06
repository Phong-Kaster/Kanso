package com.phongkaster.kanso.data.edit.edit

/**
 * The Curves tool's five curves: the master [rgb] curve, one each for [red], [green] and [blue], and
 * a [lightness] curve. Each starts as the straight diagonal ([ToneCurve.NEUTRAL]), which changes
 * nothing.
 *
 * The value is immutable: [with] makes a new one, which is what [EditSettings] wants for state.
 */
data class CurveAdjustments(
    /** The master curve, applied to all three colours. */
    val rgb: ToneCurve = ToneCurve.NEUTRAL,
    /** The red channel's own curve. */
    val red: ToneCurve = ToneCurve.NEUTRAL,
    /** The green channel's own curve. */
    val green: ToneCurve = ToneCurve.NEUTRAL,
    /** The blue channel's own curve. */
    val blue: ToneCurve = ToneCurve.NEUTRAL,
    /** The lightness (L of HSL) curve, applied after the four colour curves. */
    val lightness: ToneCurve = ToneCurve.NEUTRAL,
) {

    /** The curve stored for [channel]. */
    fun curve(channel: CurveChannel): ToneCurve = when (channel) {
        CurveChannel.RGB -> rgb
        CurveChannel.RED -> red
        CurveChannel.GREEN -> green
        CurveChannel.BLUE -> blue
        CurveChannel.LIGHTNESS -> lightness
    }

    /** A copy with [channel]'s curve replaced by [curve]; the other four are kept. */
    fun with(channel: CurveChannel, curve: ToneCurve): CurveAdjustments = when (channel) {
        CurveChannel.RGB -> copy(rgb = curve)
        CurveChannel.RED -> copy(red = curve)
        CurveChannel.GREEN -> copy(green = curve)
        CurveChannel.BLUE -> copy(blue = curve)
        CurveChannel.LIGHTNESS -> copy(lightness = curve)
    }

    /** True when all five curves are the straight diagonal, so the tool changes nothing. */
    val isNeutral: Boolean
        get() = rgb.isNeutral && red.isNeutral && green.isNeutral && blue.isNeutral && lightness.isNeutral

    /** The shared neutral value. */
    companion object {
        /** All five curves straight: nothing changed. */
        val NEUTRAL = CurveAdjustments()
    }
}
