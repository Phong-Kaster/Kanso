package com.phongkaster.kanso.data.edit.edit

/**
 * The one place the Curves tool's per-pixel maths lives, with no Android types so it runs the same
 * on the phone and in a plain JVM test.
 *
 * Each curve is first turned into a 256-entry lookup table ([ToneCurve.table]), so a pixel costs
 * only table reads, however many points the user placed. A colour goes through its own channel's
 * table first and then through the master table: red becomes `rgbTable[redTable[red]]`.
 */
object EditCurves {

    /**
     * Applies [adjustments] to [pixels], in place. Each pixel is an ARGB int; its alpha is kept.
     * Returns at once, leaving every pixel byte-identical, when [adjustments] change nothing.
     */
    fun apply(pixels: IntArray, adjustments: CurveAdjustments) {
        if (adjustments.isNeutral) return
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
}
