package com.phongkaster.kanso.data.edit.enhance

import android.graphics.Bitmap
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The lighting step of Enhance: brightens a dark photo toward the mid-tones and leaves a well-lit
 * one alone. It is plain Kotlin over the photo's pixels, no engine, so it runs the same in a test
 * as on the phone.
 *
 * How it works: it measures the photo's mean luminance (how bright it looks on average, 0 = black,
 * 255 = white). When that is below [DARK_MEAN] it applies a gamma curve, `out = 255 * (in / 255)^g`
 * with `g` below 1, to the red, green and blue of every pixel. Such a curve lifts the shadows a lot
 * and the highlights only a little, keeps black black and white white, and never makes any value
 * darker, so no photo can come out darker than it went in. `g` is chosen so the mean moves to
 * [TARGET_MEAN], but never below [MIN_GAMMA], so a nearly black photo is not blown out into noise.
 */
object AutoLighting {

    /** A photo whose mean luminance is below this is treated as dark and brightened. */
    const val DARK_MEAN = 110.0

    /** The mean luminance a dark photo is brightened toward: the middle of the range. */
    const val TARGET_MEAN = 128.0

    /** The strongest curve allowed; smaller would wash a very dark photo out. */
    const val MIN_GAMMA = 0.4

    /** How many halvings the search for the right curve makes; 24 is far finer than a pixel step. */
    private const val SEARCH_STEPS = 24

    /**
     * Returns [photo] brightened when it is dark, or [photo] itself (no copy) when it is already
     * well lit. A brightened result is a new ARGB_8888 bitmap; [photo] is never changed, and each
     * pixel's alpha is kept.
     */
    fun apply(photo: Bitmap): Bitmap {
        val histogram = luminanceHistogram(photo)
        val count = histogram.sum().toDouble()
        if (count == 0.0) return photo
        val mean = histogram.indices.sumOf { it.toDouble() * histogram[it] } / count
        if (mean >= DARK_MEAN) return photo

        val gamma = gammaFor(histogram, count)
        val curve = curveFor(gamma)
        val output = Bitmap.createBitmap(photo.width, photo.height, Bitmap.Config.ARGB_8888)
        // One row at a time, so only one row of pixels is held as an array, not the whole photo.
        val row = IntArray(photo.width)
        for (y in 0 until photo.height) {
            photo.getPixels(row, 0, photo.width, 0, y, photo.width, 1)
            for (x in row.indices) {
                val pixel = row[x]
                val alpha = pixel ushr 24
                val red = curve[(pixel shr 16) and 0xFF]
                val green = curve[(pixel shr 8) and 0xFF]
                val blue = curve[pixel and 0xFF]
                row[x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
            output.setPixels(row, 0, photo.width, 0, y, photo.width, 1)
        }
        return output
    }

    /** Returns the mean luminance of [photo], from 0 (black) to 255 (white). */
    fun meanLuminance(photo: Bitmap): Double {
        val histogram = luminanceHistogram(photo)
        val count = histogram.sum().toDouble()
        if (count == 0.0) return 0.0
        return histogram.indices.sumOf { it.toDouble() * histogram[it] } / count
    }

    /**
     * Counts how many pixels of [photo] have each luminance 0..255. Luminance weighs green most
     * and blue least (0.299 R + 0.587 G + 0.114 B), the way the eye judges brightness.
     */
    private fun luminanceHistogram(photo: Bitmap): IntArray {
        val histogram = IntArray(256)
        val row = IntArray(photo.width)
        for (y in 0 until photo.height) {
            photo.getPixels(row, 0, photo.width, 0, y, photo.width, 1)
            for (pixel in row) {
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                histogram[(299 * red + 587 * green + 114 * blue) / 1000]++
            }
        }
        return histogram
    }

    /**
     * Finds the gamma that brings the photo's mean luminance to [TARGET_MEAN], by halving the
     * range [MIN_GAMMA]..1 (a smaller gamma always gives a brighter result, so the search is
     * safe). When even [MIN_GAMMA] is not bright enough, [MIN_GAMMA] is used. The mean after a
     * curve is estimated from the [histogram], which is exact for grey pixels and close for others.
     */
    private fun gammaFor(histogram: IntArray, count: Double): Double {
        /** The photo's estimated mean luminance after the curve for [gamma] is applied. */
        fun meanAfter(gamma: Double): Double {
            val curve = curveFor(gamma)
            return histogram.indices.sumOf { curve[it].toDouble() * histogram[it] } / count
        }
        if (meanAfter(MIN_GAMMA) <= TARGET_MEAN) return MIN_GAMMA
        var low = MIN_GAMMA
        var high = 1.0
        repeat(SEARCH_STEPS) {
            val middle = (low + high) / 2
            // Still too bright: the curve may be gentler (a larger gamma).
            if (meanAfter(middle) > TARGET_MEAN) low = middle else high = middle
        }
        return (low + high) / 2
    }

    /** Returns the 256-entry table of the gamma curve: entry `v` is `255 * (v / 255)^gamma`. */
    private fun curveFor(gamma: Double): IntArray = IntArray(256) { value ->
        (255.0 * (value / 255.0).pow(gamma)).roundToInt().coerceIn(value, 255)
    }
}
