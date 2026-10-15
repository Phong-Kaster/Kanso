package com.phongkaster.kanso.data.edit.edit

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The Edit tool's processor: applies [settings] (crop, quarter turns, flips, the four colour
 * sliders and a filter, in that order) to a photo, all in plain Kotlin on the CPU.
 *
 * Every step works on the photo's pixels as one `IntArray` (one ARGB int per pixel, row after
 * row): cropping copies a rectangle out, turning and flipping move each pixel to its new place,
 * the sliders send each pixel through one colour matrix, and a filter through a [FilterLut]. Plain
 * arrays make every step exact (a turn moves pixels, it never blurs them; a slider at its middle
 * leaves every pixel as it was) and runnable in a JVM test, where the engine's GPU code cannot load.
 *
 * **One processor, one change.** The tool screen's ViewModel runs each processor on the photo as
 * it is shown now, which already holds every earlier edit; so [settings] must hold only the change
 * still to be made, never the whole history, or each run would apply the earlier changes again.
 * The Edit panel builds such a one-change value for each action: a turn, a flip or a crop is
 * applied as soon as it is tapped, and the sliders and a filter when the person lets go (see
 * `EditPanel`). Each is then one step that Undo can take back.
 *
 * [filters] finds the look-up of the chosen filter: the app passes [FilterLut.fromAssets], a test
 * a source of look-ups it generated.
 *
 * The result is always a new opaque bitmap (`setHasAlpha(false)`, so Save writes a JPEG). A photo
 * larger than [WorkingSize.EDIT] on its longest edge is first scaled down to it, so even a photo
 * handed over at full size cannot run the phone out of memory; running out anyway gives
 * [EditFailure.OutOfMemory], and a filter whose image cannot be read gives [EditFailure.Other].
 */
class EditProcessor(
    private val settings: EditSettings,
    private val filters: FilterLut.Source,
) : ToolProcessor {

    /**
     * Applies [settings] to [input] on a background thread and returns the new picture, reporting
     * progress after each step. [input] itself is never changed.
     */
    override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult =
        withContext(Dispatchers.Default) {
            try {
                onProgress(0f)
                val result = render(input, onProgress)
                onProgress(1f)
                result
            } catch (cancelled: CancellationException) {
                // Leaving the screen cancels the edit: that must stop the coroutine, not fail it.
                throw cancelled
            } catch (outOfMemory: OutOfMemoryError) {
                EditResult.Failed(EditFailure.OutOfMemory)
            } catch (other: Exception) {
                EditResult.Failed(EditFailure.Other)
            }
        }

    /** Runs every step in order on [input] and builds the result bitmap. */
    private fun render(input: Bitmap, onProgress: (Float) -> Unit): EditResult {
        // A photo above the working size is scaled down first (no copy when it already fits).
        val source = WorkingSize.fit(input, WorkingSize.EDIT)

        // 1. Crop: read only the kept rectangle's pixels out of the photo.
        val crop = cropBounds(source.width, source.height, settings)
        var image = Pixels(IntArray(crop.width * crop.height), crop.width, crop.height)
        source.getPixels(image.data, 0, crop.width, crop.left, crop.top, crop.width, crop.height)
        onProgress(PROGRESS_CROPPED)

        // 2. and 3. Turn, then flip.
        image = turn(image, settings.quarterTurns)
        if (settings.flipHorizontal) image = flip(image, horizontal = true)
        if (settings.flipVertical) image = flip(image, horizontal = false)
        onProgress(PROGRESS_TURNED)

        // 4. The colour sliders, only when one is away from its middle, so all four at the middle
        // leave every pixel exactly as it was.
        if (settings.hasColourChange) applyMatrix(image.data, colourMatrix(settings))
        onProgress(PROGRESS_COLOURED)

        // 5. The filter, mixed with the unfiltered colours by its intensity.
        if (settings.hasFilter) {
            val lut = filters.lutFor(settings.filter) ?: return EditResult.Failed(EditFailure.Other)
            applyFilter(image.data, lut, settings.filterIntensity.coerceIn(0f, 1f))
        }

        // An edited photo is opaque: every pixel's alpha is set to 255 and the bitmap says so.
        val data = image.data
        for (i in data.indices) data[i] = data[i] or OPAQUE
        val output = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        output.setPixels(data, 0, image.width, 0, 0, image.width, image.height)
        output.setHasAlpha(false)
        return EditResult.Edited(output)
    }

    /**
     * A picture as a plain array of ARGB pixels, [width] x [height], row after row: the form every
     * step of this processor works on.
     */
    private class Pixels(val data: IntArray, val width: Int, val height: Int)

    /**
     * The rectangle a crop keeps, in pixels: its top-left corner ([left], [top]) and its size
     * ([width] x [height]). Internal rather than private only so the companion's crop formula
     * can return it.
     */
    internal class Bounds(val left: Int, val top: Int, val width: Int, val height: Int)

    /**
     * Returns [image] turned by [quarterTurns] quarter turns (positive clockwise). Each pixel is
     * moved, none is blended, so four turns give back exactly the same pixels.
     */
    private fun turn(image: Pixels, quarterTurns: Int): Pixels {
        // Any number of turns comes down to 0, 1, 2 or 3 clockwise turns: -1 is the same as 3.
        val turns = ((quarterTurns % 4) + 4) % 4
        if (turns == 0) return image
        val w = image.width
        val h = image.height
        val input = image.data
        val output = IntArray(input.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = input[y * w + x]
                when (turns) {
                    // Clockwise: the old row y becomes the new column h-1-y (the image is h wide).
                    1 -> output[x * h + (h - 1 - y)] = pixel
                    // Half a turn: upside down, same size.
                    2 -> output[(h - 1 - y) * w + (w - 1 - x)] = pixel
                    // Anticlockwise: the old column x becomes the new row w-1-x.
                    else -> output[(w - 1 - x) * h + y] = pixel
                }
            }
        }
        return if (turns == 2) Pixels(output, w, h) else Pixels(output, h, w)
    }

    /** Returns [image] mirrored left to right when [horizontal], top to bottom otherwise. */
    private fun flip(image: Pixels, horizontal: Boolean): Pixels {
        val w = image.width
        val h = image.height
        val input = image.data
        val output = IntArray(input.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val target = if (horizontal) y * w + (w - 1 - x) else (h - 1 - y) * w + x
                output[target] = input[y * w + x]
            }
        }
        return Pixels(output, w, h)
    }

    /**
     * Sends every pixel of [data] through the 4 x 5 colour matrix [matrix] (the layout of
     * Android's `ColorMatrix`: each output channel is a weighted sum of the input channels plus
     * an offset, in 0..255 units), rounding and clamping each channel to 0..255.
     */
    private fun applyMatrix(data: IntArray, matrix: FloatArray) {
        for (i in data.indices) {
            val color = data[i]
            val r = ((color shr 16) and 0xFF).toFloat()
            val g = ((color shr 8) and 0xFF).toFloat()
            val b = (color and 0xFF).toFloat()
            val newR = channel(matrix[0] * r + matrix[1] * g + matrix[2] * b + matrix[3] * 255f + matrix[4])
            val newG = channel(matrix[5] * r + matrix[6] * g + matrix[7] * b + matrix[8] * 255f + matrix[9])
            val newB = channel(matrix[10] * r + matrix[11] * g + matrix[12] * b + matrix[13] * 255f + matrix[14])
            data[i] = OPAQUE or (newR shl 16) or (newG shl 8) or newB
        }
    }

    /**
     * Sends every pixel of [data] through [lut] and mixes the result with the pixel's own colour:
     * [intensity] 1 gives the filtered colour, 0 the colour as it was, 0.5 halfway between.
     */
    private fun applyFilter(data: IntArray, lut: FilterLut, intensity: Float) {
        for (i in data.indices) {
            val color = data[i]
            val filtered = lut.map(color)
            data[i] = if (intensity >= 1f) {
                filtered
            } else {
                OPAQUE or
                    (mix((color shr 16) and 0xFF, (filtered shr 16) and 0xFF, intensity) shl 16) or
                    (mix((color shr 8) and 0xFF, (filtered shr 8) and 0xFF, intensity) shl 8) or
                    mix(color and 0xFF, filtered and 0xFF, intensity)
            }
        }
    }

    /** The value [amount] of the way from [from] to [to], rounded. */
    private fun mix(from: Int, to: Int, amount: Float): Int = (from + (to - from) * amount).roundToInt()

    /** Rounds [value] to a channel value, clamped to 0..255. */
    private fun channel(value: Float): Int = when {
        value <= 0f -> 0
        value >= 255f -> 255
        else -> (value + 0.5f).toInt()
    }

    /** The crop and colour formulas, kept with the constants they use. */
    companion object {

        /** The alpha bits of a fully opaque ARGB colour. */
        private const val OPAQUE = -0x1000000

        /** How far brightness at its maximum lifts every channel, in 0..255 units. */
        const val BRIGHTNESS_SHIFT = 80f

        /** How far warmth at its maximum lifts red and lowers blue, in 0..255 units. */
        const val WARMTH_SHIFT = 40f

        /** Progress reported once the crop is done. */
        private const val PROGRESS_CROPPED = 0.25f

        /** Progress reported once the turns and flips are done. */
        private const val PROGRESS_TURNED = 0.5f

        /** Progress reported once the colour sliders are done. */
        private const val PROGRESS_COLOURED = 0.75f

        /**
         * Returns the rectangle that [settings]' crop keeps out of a [width] x [height] photo.
         *
         * - No crop: the whole photo.
         * - A fixed ratio: the largest rectangle of that shape, centred. When the leftover is an
         *   odd number of pixels, the extra one goes to the left (or top) margin, so the photo's
         *   middle pixel stays the crop's middle pixel.
         * - Free: [EditSettings.freeCrop]'s fractions turned into pixels, at least 1 x 1.
         */
        private fun cropBounds(width: Int, height: Int, settings: EditSettings): Bounds {
            val ratio = settings.cropRatio ?: return Bounds(0, 0, width, height)
            if (ratio == CropRatio.FREE) {
                val rect = settings.freeCrop
                val left = (minOf(rect.left, rect.right).coerceIn(0f, 1f) * width).roundToInt().coerceIn(0, width - 1)
                val top = (minOf(rect.top, rect.bottom).coerceIn(0f, 1f) * height).roundToInt().coerceIn(0, height - 1)
                val right = (maxOf(rect.left, rect.right).coerceIn(0f, 1f) * width).roundToInt().coerceIn(left + 1, width)
                val bottom = (maxOf(rect.top, rect.bottom).coerceIn(0f, 1f) * height).roundToInt().coerceIn(top + 1, height)
                return Bounds(left, top, right - left, bottom - top)
            }
            val cropWidth: Int
            val cropHeight: Int
            // Compare width/height with the ratio by cross-multiplying, so no rounding sneaks in.
            if (width.toLong() * ratio.heightPart > height.toLong() * ratio.widthPart) {
                // The photo is wider than the shape: keep the full height, trim the sides.
                cropHeight = height
                cropWidth = (height.toFloat() * ratio.widthPart / ratio.heightPart).roundToInt().coerceIn(1, width)
            } else {
                // The photo is taller than (or exactly) the shape: keep the full width.
                cropWidth = width
                cropHeight = (width.toFloat() * ratio.heightPart / ratio.widthPart).roundToInt().coerceIn(1, height)
            }
            return Bounds((width - cropWidth + 1) / 2, (height - cropHeight + 1) / 2, cropWidth, cropHeight)
        }

        /**
         * Builds the one colour matrix the four sliders of [settings] make together, applied in
         * this order: contrast, saturation, warmth, brightness.
         *
         * A colour matrix (Android's `ColorMatrix`) is a 4 x 5 grid of numbers: each output channel
         * is a weighted sum of the input red, green, blue and alpha plus an offset. Chaining two
         * matrices (`postConcat`) gives one matrix that does both, so the pixels are visited once.
         *
         * - Contrast scales each channel away from (or towards) mid-grey 128: by up to 2 at the
         *   maximum, down to 0.2 at the minimum.
         * - Saturation uses `setSaturation`: 0 at the minimum (every channel becomes the pixel's
         *   luminance, so black and white), 1 in the middle, 2 at the maximum.
         * - Warmth adds up to [WARMTH_SHIFT] to red and takes it from blue (the reverse when cooler).
         * - Brightness adds up to [BRIGHTNESS_SHIFT] to every channel (or takes it away).
         */
        private fun colourMatrix(settings: EditSettings): FloatArray {
            val matrix = ColorMatrix()
            val contrast = settings.contrast.coerceIn(EditSettings.MIN, EditSettings.MAX)
            if (contrast != EditSettings.MIDDLE) {
                val scale = if (contrast > 0f) 1f + contrast else 1f + contrast * 0.8f
                val offset = 128f * (1f - scale)
                matrix.postConcat(ColorMatrix(scaleAndShift(scale, offset, offset, offset)))
            }
            val saturation = settings.saturation.coerceIn(EditSettings.MIN, EditSettings.MAX)
            if (saturation != EditSettings.MIDDLE) {
                val saturate = ColorMatrix()
                saturate.setSaturation(1f + saturation)
                matrix.postConcat(saturate)
            }
            val warmth = settings.warmth.coerceIn(EditSettings.MIN, EditSettings.MAX)
            if (warmth != EditSettings.MIDDLE) {
                val shift = warmth * WARMTH_SHIFT
                matrix.postConcat(ColorMatrix(scaleAndShift(1f, shift, 0f, -shift)))
            }
            val brightness = settings.brightness.coerceIn(EditSettings.MIN, EditSettings.MAX)
            if (brightness != EditSettings.MIDDLE) {
                val shift = brightness * BRIGHTNESS_SHIFT
                matrix.postConcat(ColorMatrix(scaleAndShift(1f, shift, shift, shift)))
            }
            return matrix.array
        }

        /**
         * A colour matrix's 20 numbers that multiply red, green and blue by [scale] and then add
         * [red], [green] and [blue] to them, leaving alpha alone.
         */
        private fun scaleAndShift(scale: Float, red: Float, green: Float, blue: Float): FloatArray = floatArrayOf(
            scale, 0f, 0f, 0f, red,
            0f, scale, 0f, 0f, green,
            0f, 0f, scale, 0f, blue,
            0f, 0f, 0f, 1f, 0f,
        )
    }
}
