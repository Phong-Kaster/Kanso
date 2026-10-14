package com.phongkaster.kanso.data.edit.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException
import java.io.InputStream
import kotlin.math.cbrt
import kotlin.math.roundToInt

/**
 * A colour look-up read from the look-up image [lut]: given one pixel's colour, [map] gives the
 * colour the filter turns it into.
 *
 * "LUT" stands for look-up table. Instead of computing a filter's colours with a formula, a
 * designer bakes them into a picture once, and applying the filter is just reading that picture
 * at the right place for each pixel. That is cheap and runs on the processor (CPU) here, with no
 * GPU and no native code, so it also runs in a plain JVM test, which can pass a [lut] it drew.
 *
 * The engine ships two kinds of look-up image, told apart here by their shape:
 *
 * - A **colour cube** (`lookup_light.png`, 512 x 512): a 3D table of n x n x n colours laid out
 *   as square tiles, n = 64 in 8 x 8 tiles of 64 x 64 pixels (the layout GPUImage made common).
 *   Inside a tile, x is the red level and y the green level; the tile's number is the blue level.
 *   Any square image whose pixel count is a perfect cube with a square number of tiles is read
 *   this way (a 64 x 64 image is a 16-level cube in 4 x 4 tiles). Each channel is rounded to the
 *   nearest level; with 64 levels that is within two steps of 255, which the eye cannot see.
 * - A **curve strip** (every `filter_materials/map/...Map.png`: 256 pixels wide and a few tall):
 *   not a cube at all. The Instagram-style filters these come from read such a map as three tone
 *   curves, red from the row at 1/6 of its height, green at 1/2 and blue at 5/6, the input level
 *   picking the column. That is what is done here, for every image that is not a cube: each
 *   channel goes through its own curve, which shifts the tones and tints the photo the way the
 *   map's colours run from left (shadows) to right (highlights).
 *
 * The pixels of [lut] are copied, so it may be recycled afterwards. Use [Source] to find the
 * look-up of a [FilterPreset]; [fromAssets] builds the real one from the app's assets.
 */
class FilterLut(lut: Bitmap) {

    /** The look-up image's width in pixels. */
    private val width = lut.width

    /** Every pixel of the look-up image, row after row. */
    private val pixels = IntArray(lut.width * lut.height).also {
        lut.getPixels(it, 0, lut.width, 0, 0, lut.width, lut.height)
    }

    /** The number of levels per channel when the image is a colour cube, or null for a strip. */
    private val cubeLevels: Int? = detectCubeLevels(lut.width, lut.height)

    /** How many cube tiles sit side by side in one row of the image (unused for a strip). */
    private val tilesPerRow = cubeLevels?.let { lut.width / it } ?: 1

    /**
     * The three tone curves of a strip, red, green and blue, each with an entry for every level
     * from 0 to 255, so mapping a pixel is three array reads. Empty for a cube.
     */
    private val curves: Array<IntArray> = if (cubeLevels != null) {
        emptyArray()
    } else {
        arrayOf(
            curve(row(lut.height, 1), shift = 16),
            curve(row(lut.height, 3), shift = 8),
            curve(row(lut.height, 5), shift = 0),
        )
    }

    /** True when the image was read as a colour cube rather than as three curves. */
    val isCube: Boolean
        get() = cubeLevels != null

    /**
     * Returns the colour [color] becomes through this filter, as an opaque ARGB int (alpha 255).
     * The alpha of [color] is ignored.
     */
    fun map(color: Int): Int {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val levels = cubeLevels
        if (levels == null) {
            return OPAQUE or (curves[0][red] shl 16) or (curves[1][green] shl 8) or curves[2][blue]
        }
        // The blue level picks the tile; red and green pick the pixel inside it.
        val blueLevel = level(blue, levels)
        val x = (blueLevel % tilesPerRow) * levels + level(red, levels)
        val y = (blueLevel / tilesPerRow) * levels + level(green, levels)
        return OPAQUE or (pixels[y * width + x] and RGB)
    }

    /** Turns a channel [value] from 0..255 into the nearest of a cube's [levels]. */
    private fun level(value: Int, levels: Int): Int = (value * (levels - 1) + 127) / 255

    /** The row at [sixths]/6 of [height], kept inside the image. */
    private fun row(height: Int, sixths: Int): Int = (height * sixths / 6).coerceIn(0, height - 1)

    /**
     * Reads one curve: for each level v from 0 to 255, the channel at bit [shift] of the pixel in
     * row [y] at the column v falls on when 0..255 is spread across the image's width.
     */
    private fun curve(y: Int, shift: Int): IntArray = IntArray(CHANNEL_LEVELS) { value ->
        val x = (value * (width - 1) + 127) / 255
        (pixels[y * width + x] shr shift) and 0xFF
    }

    /**
     * Finds the look-up of a filter. This small interface is the seam between the Edit processor
     * and where look-up images come from: the app reads them from its assets ([fromAssets]), a
     * test hands over one it generated.
     */
    fun interface Source {

        /** Returns the look-up of [preset], or null when it cannot be had (a missing image). */
        fun lutFor(preset: FilterPreset): FilterLut?
    }

    /** The real look-up source, and the constants and checks the class shares. */
    companion object {

        /** The alpha bits of a fully opaque ARGB colour. */
        private const val OPAQUE = -0x1000000

        /** The red, green and blue bits of an ARGB colour. */
        private const val RGB = 0xFFFFFF

        /** One curve entry per 8-bit channel value. */
        private const val CHANNEL_LEVELS = 256

        /**
         * The real source: reads each preset's image from the app's assets through [openAsset]
         * (given a path such as `lookup/lookup_light.png`, it opens that asset; in the app,
         * `context.assets.open(path)`), the first time it is asked for, and keeps the result so
         * each image is decoded only once. A missing or undecodable image gives null.
         *
         * Taking a function instead of a Context keeps the Context out of the processor and lets
         * a test open the assets its own way.
         */
        fun fromAssets(openAsset: (String) -> InputStream): Source {
            val cache = HashMap<FilterPreset, FilterLut>()
            return Source { preset ->
                val path = preset.assetPath
                if (path == null) {
                    null
                } else {
                    // Two edits could ask at once from two threads; the lock keeps the map whole.
                    synchronized(cache) {
                        cache[preset] ?: decode(openAsset, path)?.also { cache[preset] = it }
                    }
                }
            }
        }

        /** Decodes the image at [path] through [openAsset] into a look-up, or null if it cannot. */
        private fun decode(openAsset: (String) -> InputStream, path: String): FilterLut? {
            val options = BitmapFactory.Options().apply {
                // Read the colours as they are stored: 8 bits a channel, never scaled for density.
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            val bitmap = try {
                openAsset(path).use { stream -> BitmapFactory.decodeStream(stream, null, options) }
            } catch (unreadable: IOException) {
                null
            }
            return bitmap?.let { FilterLut(it) }
        }

        /**
         * Returns the number of levels n when a [width] x [height] image is a colour cube (square,
         * n x n x n pixels, in a square grid of n x n tiles), or null when it is not.
         */
        private fun detectCubeLevels(width: Int, height: Int): Int? {
            if (width != height || width < 4) return null
            val pixelCount = width.toLong() * height
            val levels = cbrt(pixelCount.toDouble()).roundToInt()
            if (levels.toLong() * levels * levels != pixelCount) return null
            val tilesPerRow = width / levels
            return if (tilesPerRow * levels == width && tilesPerRow * tilesPerRow == levels) levels else null
        }
    }
}
