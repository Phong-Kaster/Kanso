package com.phongkaster.kanso.data.edit.edit

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The one place the Curves tool's per-pixel maths lives, with no Android types so it runs the same
 * on the phone and in a plain JVM test.
 *
 * Each curve is first turned into a 256-entry lookup table ([ToneCurve.table]), so a pixel costs
 * only table reads, however many points the user placed. A colour goes through its own channel's
 * table first and then through the master table: red becomes `rgbTable[redTable[red]]`. Then, when
 * the Lightness curve is bent, the pixel is turned into hue / saturation / lightness, its lightness
 * is mapped through the Lightness table and it is turned back, so hue and saturation are kept.
 */
object EditCurves {

    /**
     * Applies [adjustments] to [pixels], in place. Each pixel is an ARGB int; its alpha is kept.
     * Returns at once, leaving every pixel byte-identical, when [adjustments] change nothing.
     */
    fun apply(pixels: IntArray, adjustments: CurveAdjustments) {
        if (adjustments.isNeutral) return
        val colourBent = !(adjustments.rgb.isNeutral && adjustments.red.isNeutral &&
            adjustments.green.isNeutral && adjustments.blue.isNeutral)
        if (colourBent) {
            val master = adjustments.rgb.table()
            val red = adjustments.red.table()
            val green = adjustments.green.table()
            val blue = adjustments.blue.table()
            for (i in pixels.indices) {
                val argb = pixels[i]
                val r = master[red[(argb shr 16) and 0xFF]]
                val g = master[green[(argb shr 8) and 0xFF]]
                val b = master[blue[argb and 0xFF]]
                pixels[i] = (argb and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
            }
        }
        if (!adjustments.lightness.isNeutral) applyLightness(pixels, adjustments.lightness.table())
    }

    /**
     * Maps every pixel's HSL lightness through [table] (256 entries, 0..255), in place. Unlike the HSL
     * tool there is no grey gate (greys follow the curve too) and no 0.05..0.95 clamp (lightness may
     * reach 0 and 1). Hue and saturation are kept; a grey (no spread) stays grey.
     */
    private fun applyLightness(pixels: IntArray, table: IntArray) {
        for (i in pixels.indices) {
            val argb = pixels[i]
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val delta = maxC - minC
            val l = (maxC + minC) / 2f
            // Look the new lightness up, interpolating between the two nearest table entries.
            val pos = l.coerceIn(0f, 1f) * 255f
            val lo = min(pos.toInt(), 254)
            val l2 = ((table[lo] + (table[lo + 1] - table[lo]) * (pos - lo)) / 255f).coerceIn(0f, 1f)
            if (delta == 0f) {
                // A grey: the new lightness is the new grey level.
                val v = (l2 * 255f).roundToInt().coerceIn(0, 255)
                pixels[i] = (argb and 0xFF000000.toInt()) or (v shl 16) or (v shl 8) or v
                continue
            }
            val s = delta / (1f - abs(2f * l - 1f))
            var h = when {
                maxC == r -> 60f * (((g - b) / delta + 6f) % 6f)
                maxC == g -> 60f * ((b - r) / delta + 2f)
                else -> 60f * ((r - g) / delta + 4f)
            }
            if (h >= 360f) h -= 360f
            pixels[i] = (argb and 0xFF000000.toInt()) or hslToRgb(h, s.coerceIn(0f, 1f), l2)
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
