package com.phongkaster.kanso.data.edit.edit

import android.graphics.Bitmap
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/**
 * The Edit tool's processor: renders the WHOLE of [settings] onto a photo, all in plain Kotlin on
 * the CPU, in this order: crop, quarter turns, flips, the filter, then the four colour sliders.
 *
 * Every step works on the photo's pixels as one `IntArray` (one ARGB int per pixel, row after
 * row): cropping copies a rectangle out, turning and flipping move each pixel to its new place,
 * a filter sends each pixel through a [FilterLut], and the sliders through one colour matrix.
 * Plain arrays make every step exact (a turn moves pixels, it never blurs them; a slider at its
 * middle leaves every pixel as it was) and runnable in a JVM test, where the engine's GPU code
 * cannot load.
 *
 * **Always from the original.** The tool screen's ViewModel hands this processor the ORIGINAL
 * photo every time, never a picture an earlier run produced, and [settings] holds the session's
 * complete state ([EditSettings]). So nothing is stacked or baked in: choosing a second filter
 * replaces the first, moving a slider back to its middle gives the photo back exactly, and
 * rendering the same settings twice gives the same pixels.
 *
 * **Why the filter comes before the sliders.** A filter is a fixed look designed for an ordinary
 * photo; the sliders are the person's fine tuning of whatever they see. Applying the filter first
 * and the sliders on top means "brightness +20" brightens the filtered look, the way photo apps
 * usually behave (decision A-002).
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
            // Kept so the long loops below can notice a newer render has cancelled this one.
            job = coroutineContext[Job]
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

    /** The job running [process], so a cancelled render stops early instead of running to the end. */
    @Volatile
    private var job: Job? = null

    /** Throws a CancellationException when this render was cancelled (a newer one replaced it). */
    private fun checkActive() {
        job?.ensureActive()
    }

    /** Runs every step in order on [input] and builds the result bitmap. */
    private fun render(input: Bitmap, onProgress: (Float) -> Unit): EditResult {
        // A photo above the working size is scaled down first (no copy when it already fits).
        val source = WorkingSize.fit(input, WorkingSize.EDIT)

        // 1. Crop: read only the kept rectangle's pixels out of the photo. A fixed ratio is no
        // special case here: choosing it already set settings.crop to a rectangle of that shape.
        val crop = cropBounds(source.width, source.height, settings.crop)
        var image = Pixels(IntArray(crop.width * crop.height), crop.width, crop.height)
        source.getPixels(image.data, 0, crop.width, crop.left, crop.top, crop.width, crop.height)
        onProgress(PROGRESS_CROPPED)

        // 2. and 3. Turn, then flip.
        image = turn(image, settings.quarterTurns)
        if (settings.flipHorizontal) image = flip(image, horizontal = true)
        if (settings.flipVertical) image = flip(image, horizontal = false)
        onProgress(PROGRESS_TURNED)

        // 4. The filter, mixed with the unfiltered colours by its intensity (before the sliders,
        // so the sliders fine-tune the filtered look: decision A-002).
        if (settings.hasFilter) {
            val lut = filters.lutFor(settings.filter) ?: return EditResult.Failed(EditFailure.Other)
            applyFilter(image.data, lut, settings.filterIntensity.coerceIn(0f, 1f))
        }
        onProgress(PROGRESS_FILTERED)

        // 5. The colour sliders, only when one is away from its middle, so all four at the middle
        // leave every pixel exactly as it was. The matrix comes from EditColourMatrix, the same
        // builder the live preview hands to Compose, so the preview and Done agree.
        if (settings.hasColourChange) applyMatrix(image.data, EditColourMatrix.of(settings))

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
            checkActive()
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
            checkActive()
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
            if (i and CHECK_MASK == 0) checkActive()
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
     * Sends every pixel of [data] through [lut] at [intensity], one pixel at a time through
     * [filterPixel] (the same function the live preview's [FilterPreviewRenderer] relies on).
     */
    private fun applyFilter(data: IntArray, lut: FilterLut, intensity: Float) {
        for (i in data.indices) {
            if (i and CHECK_MASK == 0) checkActive()
            data[i] = filterPixel(data[i], lut, intensity)
        }
    }

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

        /**
         * How far brightness at its maximum lifts every channel, in 0..255 units. The number lives
         * in [EditColourMatrix]; this name is kept so existing code and tests still read it here.
         */
        const val BRIGHTNESS_SHIFT = EditColourMatrix.BRIGHTNESS_SHIFT

        /**
         * How far warmth at its maximum lifts red and lowers blue, in 0..255 units (kept here for
         * the same reason as [BRIGHTNESS_SHIFT]).
         */
        const val WARMTH_SHIFT = EditColourMatrix.WARMTH_SHIFT

        /** A cancelled render is noticed every 65,536 pixels in the colour and filter loops. */
        private const val CHECK_MASK = 0xFFFF

        /** Progress reported once the crop is done. */
        private const val PROGRESS_CROPPED = 0.25f

        /** Progress reported once the turns and flips are done. */
        private const val PROGRESS_TURNED = 0.5f

        /** Progress reported once the filter is done. */
        private const val PROGRESS_FILTERED = 0.75f

        /**
         * Returns the pixels that [rect] keeps out of a [width] x [height] photo: its fractions
         * turned into pixels (rounded), with the edges put in order, kept inside the photo and at
         * least 1 x 1, so even a rectangle dragged to nothing still gives a picture.
         * [CropRect.FULL][EditSettings.CropRect.FULL] gives the whole photo.
         */
        private fun cropBounds(width: Int, height: Int, rect: EditSettings.CropRect): Bounds {
            val left = (minOf(rect.left, rect.right).coerceIn(0f, 1f) * width).roundToInt().coerceIn(0, width - 1)
            val top = (minOf(rect.top, rect.bottom).coerceIn(0f, 1f) * height).roundToInt().coerceIn(0, height - 1)
            val right = (maxOf(rect.left, rect.right).coerceIn(0f, 1f) * width).roundToInt().coerceIn(left + 1, width)
            val bottom = (maxOf(rect.top, rect.bottom).coerceIn(0f, 1f) * height).roundToInt().coerceIn(top + 1, height)
            return Bounds(left, top, right - left, bottom - top)
        }

        /**
         * The one colour matrix the four sliders of [settings] make together. The formula lives
         * in [EditColourMatrix.of], shared with the live preview; this delegate stays so existing
         * code and tests can keep asking the processor. It is `internal` (visible to the app
         * module and its tests, nowhere else).
         */
        internal fun colourMatrix(settings: EditSettings): FloatArray = EditColourMatrix.of(settings)

        /**
         * Sends one ARGB pixel [color] through the filter look-up [lut] and mixes the result with
         * the pixel's own colour: [intensity] 1 (or more) gives the filtered colour, 0 the colour
         * as it was, 0.5 halfway between, each channel rounded.
         *
         * It is a pure function (same input, same output, nothing else touched) and `internal`, so
         * Done ([EditProcessor]) and the live preview ([FilterPreviewRenderer], which runs this
         * processor with only the filter set) mix a filter in exactly the same way.
         */
        internal fun filterPixel(color: Int, lut: FilterLut, intensity: Float): Int {
            val filtered = lut.map(color)
            if (intensity >= 1f) return filtered
            return OPAQUE or
                (mix((color shr 16) and 0xFF, (filtered shr 16) and 0xFF, intensity) shl 16) or
                (mix((color shr 8) and 0xFF, (filtered shr 8) and 0xFF, intensity) shl 8) or
                mix(color and 0xFF, filtered and 0xFF, intensity)
        }

        /** The value [amount] of the way from [from] to [to], rounded. */
        private fun mix(from: Int, to: Int, amount: Float): Int = (from + (to - from) * amount).roundToInt()
    }
}
