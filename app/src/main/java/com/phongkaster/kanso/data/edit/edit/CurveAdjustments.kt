package com.phongkaster.kanso.data.edit.edit

/**
 * The Curves tool's four curves: the master [rgb] curve and one each for [red], [green] and [blue].
 * Each starts as the straight diagonal ([ToneCurve.NEUTRAL]), which changes nothing.
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
) {

    /** The curve stored for [channel]. */
    fun curve(channel: CurveChannel): ToneCurve = when (channel) {
        CurveChannel.RGB -> rgb
        CurveChannel.RED -> red
        CurveChannel.GREEN -> green
        CurveChannel.BLUE -> blue
    }

    /** A copy with [channel]'s curve replaced by [curve]; the other three are kept. */
    fun with(channel: CurveChannel, curve: ToneCurve): CurveAdjustments = when (channel) {
        CurveChannel.RGB -> copy(rgb = curve)
        CurveChannel.RED -> copy(red = curve)
        CurveChannel.GREEN -> copy(green = curve)
        CurveChannel.BLUE -> copy(blue = curve)
    }

    /** True when all four curves are the straight diagonal, so the tool changes nothing. */
    val isNeutral: Boolean
        get() = rgb.isNeutral && red.isNeutral && green.isNeutral && blue.isNeutral

    /** The shared neutral value. */
    companion object {
        /** All four curves straight: nothing changed. */
        val NEUTRAL = CurveAdjustments()
    }
}
