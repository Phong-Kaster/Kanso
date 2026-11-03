package com.phongkaster.kanso.data.edit.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
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
 * The Edit tool's processor: renders the WHOLE of [settings] onto a photo, in this order: the
 * layers, crop, quarter turns, flips, the filter, then the colour sliders and channels. All of it is
 * plain Kotlin on the CPU, apart from each sticker, which is drawn with Android's `Canvas`.
 *
 * Every step works on the photo's pixels as one `IntArray` (one ARGB int per pixel, row after
 * row): cropping copies a rectangle out, turning and flipping move each pixel to its new place,
 * a filter sends each pixel through a [FilterLut], and the sliders (Adjust's four, RGB's three)
 * through one colour matrix.
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
    private val layerPictures: LayerPictures = LayerPictures { _, _ -> null },
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
        // Stickers are part of the photo, so they go in before the turns, flips, filter and
        // sliders, which then act on them too. Their place is a fraction of the ORIGINAL photo,
        // so a crop keeps only the part of a sticker inside the rectangle.
        drawLayers(image, source.width, source.height, crop)
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

        // 5. The colour sliders and the RGB channels, only when one is away from its middle, so all
        // of them at the middle leave every pixel exactly as it was. The matrix comes from EditColourMatrix, the same
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
     * Draws every sticker of [settings], oldest first, into the cropped [image]. [photoWidth] and
     * [photoHeight] are the size of the whole (working-size) photo the layers' fractions refer
     * to, and [crop] is where the kept rectangle sits in it, so a sticker is shifted by the
     * crop's corner.
     *
     * Each sticker is drawn on its own small bitmap, only as big as the part of it that lands
     * inside the image, then its pixels are alpha-blended over the image's one row at a time
     * and that bitmap is recycled. So the extra memory is one bitmap the size of the visible part
     * of the sticker (at most the kept area, when a sticker covers all of it) plus one row of
     * ints. The picture asked of [layerPictures] is never wider than the photo's longest edge,
     * however big the sticker is: the `Matrix` stretches the rest, so an over-sized sticker cannot
     * run the phone out of memory. The picture itself belongs to [layerPictures] and is never
     * recycled here. The blend happens before the image is made opaque, so a soft edge mixes with
     * the photo.
     *
     * A sticker whose width, centre or turn is not a finite number (NaN or infinity) draws nothing.
     */
    private fun drawLayers(image: Pixels, photoWidth: Int, photoHeight: Int, crop: Bounds) {
        for (sticker in settings.layers) {
            checkActive()
            if (!sticker.width.isFinite() || !sticker.centreX.isFinite() ||
                !sticker.centreY.isFinite() || !sticker.degrees.isFinite()
            ) {
                continue
            }
            val targetWidth = sticker.width * photoWidth
            if (!targetWidth.isFinite() || targetWidth < 1f) continue
            // Ask for no more pixels than the photo's long side: bigger would only cost memory.
            val decodeWidth = minOf(targetWidth, maxOf(photoWidth, photoHeight).toFloat())
                .roundToInt().coerceAtLeast(1)
            val picture = layerPictures.picture(sticker.content, decodeWidth) ?: continue
            if (picture.width <= 0 || picture.height <= 0) continue

            // One uniform scale from the decoded picture to its size in working pixels.
            val scale = targetWidth / picture.width
            val targetHeight = scale * picture.height
            val radians = Math.toRadians(sticker.degrees.toDouble())
            val cos = Math.abs(Math.cos(radians))
            val sin = Math.abs(Math.sin(radians))
            val halfBoxWidth = (targetWidth * cos + targetHeight * sin) / 2.0
            val halfBoxHeight = (targetWidth * sin + targetHeight * cos) / 2.0
            // The sticker's centre in the cropped image's pixels.
            val centreX = sticker.centreX * photoWidth - crop.left
            val centreY = sticker.centreY * photoHeight - crop.top

            // The bounding box of the turned sticker, cut down to the part inside the image. A
            // tiny margin keeps float noise from adding a row of nothing at an exact edge.
            val left = Math.floor(centreX - halfBoxWidth + BOX_EPSILON).toInt().coerceAtLeast(0)
            val top = Math.floor(centreY - halfBoxHeight + BOX_EPSILON).toInt().coerceAtLeast(0)
            val right = Math.ceil(centreX + halfBoxWidth - BOX_EPSILON).toInt().coerceAtMost(image.width)
            val bottom = Math.ceil(centreY + halfBoxHeight - BOX_EPSILON).toInt().coerceAtMost(image.height)
            if (right <= left || bottom <= top) continue
            val boxWidth = right - left
            val boxHeight = bottom - top

            val box = Bitmap.createBitmap(boxWidth, boxHeight, Bitmap.Config.ARGB_8888)
            try {
                // The picture's centre goes to the origin, is scaled (and mirrored), turned
                // clockwise, then moved to the sticker's centre within the box.
                val matrix = Matrix()
                matrix.postTranslate(-picture.width / 2f, -picture.height / 2f)
                matrix.postScale(if (sticker.mirrored) -scale else scale, scale)
                matrix.postRotate(sticker.degrees)
                matrix.postTranslate((centreX - left).toFloat(), (centreY - top).toFloat())
                // A sticker drawn 1:1 with no turn is copied pixel for pixel: smoothing would blur it.
                val exact = Math.abs(scale - 1f) < SCALE_EPSILON && sticker.degrees % 360f == 0f
                val paint = Paint().apply {
                    isFilterBitmap = !exact
                    isAntiAlias = !exact
                    // The layer's opacity fades every pixel it lays down; the blend below then
                    // mixes it with the photo like any other soft edge.
                    alpha = opacityAlpha(sticker.opacity)
                }
                Canvas(box).drawBitmap(picture, matrix, paint)

                // Read the box one row at a time, so no int array of the whole box is built.
                val row = IntArray(boxWidth)
                for (y in 0 until boxHeight) {
                    checkActive()
                    box.getPixels(row, 0, boxWidth, 0, y, boxWidth, 1)
                    blendRow(image, row, left, top + y, boxWidth)
                }
            } finally {
                box.recycle()
            }
        }
    }

    /**
     * A layer's [opacity] (0..1) as a paint alpha (0..255), kept within [Layer.MIN_OPACITY]..1 so
     * a value out of range (or not a number) never hides a layer or overflows the paint.
     */
    private fun opacityAlpha(opacity: Float): Int {
        val kept = if (opacity.isNaN()) 1f else opacity.coerceIn(Layer.MIN_OPACITY, 1f)
        return (kept * OPAQUE_ALPHA).roundToInt()
    }

    /**
     * Lays one row of sticker pixels [row] ([rowWidth] non-premultiplied ARGB ints, as
     * `Bitmap.getPixels` gives them) over [image], the row's first pixel landing at ([left], [y]):
     * each channel is the sticker's colour weighted by its alpha plus the photo's weighted by what
     * is left, rounded. A fully clear pixel leaves the photo alone and a fully opaque one replaces
     * it exactly.
     */
    private fun blendRow(image: Pixels, row: IntArray, left: Int, y: Int, rowWidth: Int) {
        for (x in 0 until rowWidth) {
            val s = row[x]
            val alpha = s ushr 24
            if (alpha == 0) continue
            val index = y * image.width + left + x
            if (alpha == 255) {
                image.data[index] = s
                continue
            }
            val d = image.data[index]
            val inverse = 255 - alpha
            val r = (((s shr 16) and 0xFF) * alpha + ((d shr 16) and 0xFF) * inverse + 127) / 255
            val g = (((s shr 8) and 0xFF) * alpha + ((d shr 8) and 0xFF) * inverse + 127) / 255
            val b = ((s and 0xFF) * alpha + (d and 0xFF) * inverse + 127) / 255
            image.data[index] = OPAQUE or (r shl 16) or (g shl 8) or b
        }
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

        /** Float noise tolerated when sizing a sticker's bounding box, in pixels. */
        private const val BOX_EPSILON = 1e-3

        /** A fully solid paint's alpha. */
        private const val OPAQUE_ALPHA = 255f

        /** How close to 1 a sticker's scale must be to count as drawn 1:1. */
        private const val SCALE_EPSILON = 1e-4f

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
         * The one colour matrix the sliders and channels of [settings] make together. The formula lives
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
