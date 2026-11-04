package com.phongkaster.kanso.data.edit.edit

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The one place the HSL tool's per-pixel maths lives, with no Android types so it runs the same on
 * the phone and in a plain JVM test.
 *
 * For each pixel it converts RGB to hue / saturation / lightness, blends the amounts of the colour
 * ranges the pixel's hue falls between, nudges the three values and converts back. Two details keep
 * the result smooth:
 *
 * - **Soft ranges.** A range's weight falls from 1 at its centre to 0 at the next centre as a
 *   cosine squared, so two neighbours always weigh 1 together and no hue jumps from one range to
 *   the next (no banding).
 * - **A gate for greys.** Hue means nothing for a grey or a near-grey, so the blended change is
 *   multiplied by [gate], which is 0 for greys and 1 for clear colours. Without it, noise in a
 *   dark or washed-out pixel would flip its hue at random.
 */
object EditHsl {

    /** The widest turn of hue, in degrees, that an amount of +1 or -1 makes. */
    private const val MAX_HUE_TURN_DEGREES = 20f

    /** The distance between two neighbouring range centres, in degrees. */
    private const val RANGE_SPACING_DEGREES = 40f

    /** Table entries per degree of hue (0.1 degree bins). */
    private const val BINS_PER_DEGREE = 10f

    /** The number of hue bins in the tables: 3600 for the full wheel. */
    private const val TABLE_SIZE = 3600

    /** Lightness that +1 may reach at most (never pure white). */
    private const val LIGHTNESS_CEILING = 0.95f

    /** Lightness that -1 may reach at least (never pure black). */
    private const val LIGHTNESS_FLOOR = 0.05f

    /**
     * Applies [adjustments] to [pixels], in place. Each pixel is an ARGB int; its alpha is kept.
     * Returns at once when [adjustments] change nothing.
     */
    fun apply(pixels: IntArray, adjustments: HslAdjustments) {
        if (adjustments.isNeutral) return
        val tableH = FloatArray(TABLE_SIZE)
        val tableS = FloatArray(TABLE_SIZE)
        val tableL = FloatArray(TABLE_SIZE)
        buildTables(adjustments, tableH, tableS, tableL)
        for (i in pixels.indices) {
            val argb = pixels[i]
            val gate = gate(argb)
            if (gate <= 0f) continue // a pure grey: nothing to change, and hue is undefined
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val delta = maxC - minC
            val l = (maxC + minC) / 2f
            val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
            var h = when {
                delta == 0f -> 0f
                maxC == r -> 60f * (((g - b) / delta + 6f) % 6f)
                maxC == g -> 60f * ((b - r) / delta + 2f)
                else -> 60f * ((r - g) / delta + 4f)
            }
            if (h >= 360f) h -= 360f

            // Look the blended change up in the tables, interpolating between the two nearest bins.
            val pos = h * BINS_PER_DEGREE
            val bin = min(pos.toInt(), TABLE_SIZE - 1)
            val frac = pos - bin
            val next = if (bin + 1 == TABLE_SIZE) 0 else bin + 1
            val dH = gate * (tableH[bin] + (tableH[next] - tableH[bin]) * frac)
            val dS = gate * (tableS[bin] + (tableS[next] - tableS[bin]) * frac)
            val dL = gate * (tableL[bin] + (tableL[next] - tableL[bin]) * frac)

            var h2 = (h + MAX_HUE_TURN_DEGREES * dH) % 360f
            if (h2 < 0f) h2 += 360f
            val s2 = (s * (1f + dS)).coerceIn(0f, 1f)
            val l2 = if (dL >= 0f) {
                l + max(0f, LIGHTNESS_CEILING - l) * dL
            } else {
                l - max(0f, l - LIGHTNESS_FLOOR) * -dL
            }
            pixels[i] = (argb and 0xFF000000.toInt()) or hslToRgb(h2, s2, l2.coerceIn(0f, 1f))
        }
    }

    /**
     * How much [range] counts at a pixel of [hueDegrees]: 1 at its centre, falling as a cosine
     * squared to 0 at the next range's centre and staying 0 beyond. The distance is measured round
     * the wheel, so red also reaches back to pink.
     */
    internal fun rangeWeight(range: ColourRange, hueDegrees: Float): Float {
        val raw = abs(hueDegrees - range.centreDegrees) % 360f
        val d = min(raw, 360f - raw)
        if (d >= RANGE_SPACING_DEGREES) return 0f
        val c = cos(PI * d / (2.0 * RANGE_SPACING_DEGREES)).toFloat()
        return c * c
    }

    /**
     * How much the pixel [argb] is a real colour rather than a grey, from 0 to 1: a smoothstep of
     * its spread (largest minus smallest channel, 0..1) between 0.03 and 0.25. Black, white and
     * greys, and anything almost grey, come out at or near 0.
     */
    internal fun gate(argb: Int): Float {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val spread = (max(r, max(g, b)) - min(r, min(g, b))) / 255f
        val x = ((spread - 0.03f) / (0.25f - 0.03f)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    /**
     * Fills the three tables, one entry per 0.1 degree of hue, with the blended hue, saturation and
     * lightness amounts at that hue: the sum over the nine ranges of weight times amount. Done once
     * per [apply], so the per-pixel path needs no trigonometry. The gate is not in the tables.
     */
    private fun buildTables(
        adjustments: HslAdjustments,
        tableH: FloatArray,
        tableS: FloatArray,
        tableL: FloatArray,
    ) {
        val ranges = ColourRange.entries
        for (bin in 0 until TABLE_SIZE) {
            val hue = bin / BINS_PER_DEGREE
            var dH = 0f
            var dS = 0f
            var dL = 0f
            for (range in ranges) {
                val w = rangeWeight(range, hue)
                if (w <= 0f) continue
                dH += w * adjustments.hue[range.ordinal]
                dS += w * adjustments.saturation[range.ordinal]
                dL += w * adjustments.lightness[range.ordinal]
            }
            tableH[bin] = dH
            tableS[bin] = dS
            tableL[bin] = dL
        }
    }

    /** Packs the three channels (each 0..1 before [m] is added) into the RGB bits of an ARGB int. */
    private fun pack(r1: Float, g1: Float, b1: Float, m: Float): Int {
        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return (r shl 16) or (g shl 8) or b
    }

    /** Converts hue (degrees), saturation and lightness (0..1) to the RGB bits of an ARGB int (alpha 0). */
    private fun hslToRgb(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2f * l - 1f)) * s
        val hp = h / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val m = l - c / 2f
        return when {
            hp < 1f -> pack(c, x, 0f, m)
            hp < 2f -> pack(x, c, 0f, m)
            hp < 3f -> pack(0f, c, x, m)
            hp < 4f -> pack(0f, x, c, m)
            hp < 5f -> pack(x, 0f, c, m)
            else -> pack(c, 0f, x, m)
        }
    }
}
