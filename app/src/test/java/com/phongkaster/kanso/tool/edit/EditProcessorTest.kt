package com.phongkaster.kanso.tool.edit

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Checks the Edit tool's processor on Android 15 with real bitmap drawing (DoD 1, 2, 3, 22):
 *
 * - crop: each fixed ratio on a 400 x 300 photo with a different colour in each quarter gives
 *   that shape within 1 pixel, as large as fits and centred (the crop's middle pixel is the
 *   photo's middle pixel); a free crop of (0.25, 0.25, 0.75, 0.75) gives 200 x 150 starting at
 *   the photo's pixel (100, 75); no crop gives the same pixels back;
 * - turn and flip: a clockwise quarter turn gives 300 x 400 with the top-left pixel at the top
 *   right, anticlockwise at the bottom left, four turns give the same pixels back, and a flip
 *   across moves the top-left pixel to the top right and keeps the size;
 * - colour sliders, each alone on a 100 x 100 patch of mid-grey and colours: all four at the
 *   middle change nothing; brightness moves the mean luminance, contrast its spread, saturation
 *   the colourfulness (black and white at the minimum) and warmth red against blue;
 * - a very large photo is worked on at most [WorkingSize.EDIT] pixels long, the result is opaque,
 *   and running out of memory is reported as such.
 *
 * Every pixel of the 400 x 300 photo also carries its position (modulo 16) in its red and green,
 * so a pixel landing one place off is caught, not only one landing in the wrong quarter.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class EditProcessorTest {

    /** A filter source with no look-ups: none of these tests uses a filter. */
    private val noFilters = FilterLut.Source { null }

    /** No crop, no turn, all sliders in the middle: the pixels come back exactly as they were. */
    @Test
    fun no_change_gives_the_same_pixels() {
        val photo = quarters()
        val edited = run(photo, EditSettings())
        assertEquals(photo.width, edited.width)
        assertEquals(photo.height, edited.height)
        assertArrayEquals(pixels(photo), pixels(edited))
    }

    /** Each fixed ratio gives its shape within 1 pixel, as large as fits, centred on the middle. */
    @Test
    fun each_ratio_crops_to_its_shape_around_the_middle() {
        val photo = quarters()
        val middle = photo.getPixel(photo.width / 2, photo.height / 2)
        CropRatio.entries.filter { it != CropRatio.FREE }.forEach { ratio ->
            val edited = run(photo, EditSettings(cropRatio = ratio))
            val expectedWidth = edited.height * ratio.widthPart / ratio.heightPart.toFloat()
            val expectedHeight = edited.width * ratio.heightPart / ratio.widthPart.toFloat()
            assertTrue(
                "$ratio gave ${edited.width} x ${edited.height}",
                abs(edited.width - expectedWidth) <= 1f || abs(edited.height - expectedHeight) <= 1f,
            )
            assertTrue(
                "$ratio is not as large as fits: ${edited.width} x ${edited.height}",
                edited.width == photo.width || edited.height == photo.height,
            )
            assertEquals(
                "$ratio's middle pixel is not the photo's middle pixel",
                middle,
                edited.getPixel(edited.width / 2, edited.height / 2),
            )
        }
    }

    /** A free crop keeps exactly the rectangle asked for, in pixels. */
    @Test
    fun a_free_crop_keeps_its_rectangle() {
        val photo = quarters()
        val edited = run(
            photo,
            EditSettings(cropRatio = CropRatio.FREE, freeCrop = EditSettings.CropRect(0.25f, 0.25f, 0.75f, 0.75f)),
        )
        assertEquals(200, edited.width)
        assertEquals(150, edited.height)
        assertEquals(photo.getPixel(100, 75), edited.getPixel(0, 0))
        assertEquals(photo.getPixel(299, 224), edited.getPixel(199, 149))
    }

    /** A clockwise quarter turn stands the photo up and moves its top-left pixel to the top right. */
    @Test
    fun a_turn_right_moves_the_top_left_to_the_top_right() {
        val photo = quarters()
        val edited = run(photo, EditSettings(quarterTurns = 1))
        assertEquals(300, edited.width)
        assertEquals(400, edited.height)
        assertEquals(photo.getPixel(0, 0), edited.getPixel(299, 0))
        assertEquals(photo.getPixel(399, 299), edited.getPixel(0, 399))
    }

    /** An anticlockwise quarter turn moves the top-left pixel to the bottom left. */
    @Test
    fun a_turn_left_moves_the_top_left_to_the_bottom_left() {
        val photo = quarters()
        val edited = run(photo, EditSettings(quarterTurns = -1))
        assertEquals(300, edited.width)
        assertEquals(400, edited.height)
        assertEquals(photo.getPixel(0, 0), edited.getPixel(0, 399))
    }

    /** Four quarter turns, one edit at a time as the panel makes them, give the same pixels back. */
    @Test
    fun four_turns_give_the_photo_back() {
        val photo = quarters()
        var edited = photo
        repeat(4) { edited = run(edited, EditSettings(quarterTurns = 1)) }
        assertEquals(photo.width, edited.width)
        assertEquals(photo.height, edited.height)
        assertArrayEquals(pixels(photo), pixels(edited))
    }

    /** A flip across moves the top-left pixel to the top right and keeps the size. */
    @Test
    fun a_flip_across_mirrors_left_and_right() {
        val photo = quarters()
        val edited = run(photo, EditSettings(flipHorizontal = true))
        assertEquals(400, edited.width)
        assertEquals(300, edited.height)
        assertEquals(photo.getPixel(0, 0), edited.getPixel(399, 0))
        assertEquals(photo.getPixel(0, 299), edited.getPixel(399, 299))
    }

    /** A flip upside down moves the top-left pixel to the bottom left. */
    @Test
    fun a_flip_down_mirrors_top_and_bottom() {
        val photo = quarters()
        val edited = run(photo, EditSettings(flipVertical = true))
        assertEquals(photo.getPixel(0, 0), edited.getPixel(0, 299))
    }

    /** All four sliders at their middle give the patch back pixel for pixel. */
    @Test
    fun sliders_in_the_middle_change_nothing() {
        val patch = patch()
        val edited = run(
            patch,
            EditSettings(
                brightness = EditSettings.MIDDLE,
                contrast = EditSettings.MIDDLE,
                saturation = EditSettings.MIDDLE,
                warmth = EditSettings.MIDDLE,
            ),
        )
        assertArrayEquals(pixels(patch), pixels(edited))
    }

    /** Brightness up raises the mean luminance; down lowers it. */
    @Test
    fun brightness_moves_the_mean_luminance() {
        val patch = patch()
        val before = meanLuminance(patch)
        val up = meanLuminance(run(patch, EditSettings(brightness = EditSettings.MAX)))
        val down = meanLuminance(run(patch, EditSettings(brightness = EditSettings.MIN)))
        assertTrue("up: $before -> $up", up > before)
        assertTrue("down: $before -> $down", down < before)
    }

    /** Contrast up spreads the luminance (its standard deviation rises); down flattens it. */
    @Test
    fun contrast_moves_the_luminance_spread() {
        val patch = patch()
        val before = luminanceSpread(patch)
        val up = luminanceSpread(run(patch, EditSettings(contrast = EditSettings.MAX)))
        val down = luminanceSpread(run(patch, EditSettings(contrast = EditSettings.MIN)))
        assertTrue("up: $before -> $up", up > before)
        assertTrue("down: $before -> $down", down < before)
    }

    /** Saturation at its minimum makes every pixel grey; up makes the colours stronger. */
    @Test
    fun saturation_moves_the_colourfulness() {
        val patch = patch()
        val grey = run(patch, EditSettings(saturation = EditSettings.MIN))
        pixels(grey).forEach { color ->
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            assertTrue("pixel ($r, $g, $b) is not grey", max(r, max(g, b)) - minOf(r, g, b) <= 2)
        }
        val before = meanChroma(patch)
        val up = meanChroma(run(patch, EditSettings(saturation = EditSettings.MAX)))
        assertTrue("up: $before -> $up", up > before)
    }

    /** Warmth up raises red against blue; down lowers it. */
    @Test
    fun warmth_moves_red_against_blue() {
        val patch = patch()
        val before = redMinusBlue(patch)
        val up = redMinusBlue(run(patch, EditSettings(warmth = EditSettings.MAX)))
        val down = redMinusBlue(run(patch, EditSettings(warmth = EditSettings.MIN)))
        assertTrue("up: $before -> $up", up > before)
        assertTrue("down: $before -> $down", down < before)
    }

    /** A 6000 x 4000 photo is worked on, and returned, at most 2000 pixels on its long edge. */
    @Test
    fun a_very_large_photo_is_worked_on_at_the_cap() {
        val huge = Bitmap.createBitmap(6000, 4000, Bitmap.Config.ARGB_8888)
        val edited = run(huge, EditSettings(brightness = 0.5f))
        assertTrue(
            "long edge ${max(edited.width, edited.height)} is above the cap",
            max(edited.width, edited.height) <= WorkingSize.EDIT,
        )
        huge.recycle()
    }

    /** The result is opaque, so Save writes it as a JPEG. */
    @Test
    fun the_result_is_opaque() {
        assertFalse(run(patch(), EditSettings(contrast = 0.5f)).hasAlpha())
    }

    /** Running out of memory during the edit becomes the out-of-memory failure, not a crash. */
    @Test
    fun running_out_of_memory_is_reported() {
        val starved = FilterLut.Source { throw OutOfMemoryError("test") }
        val result = runBlocking {
            EditProcessor(EditSettings(filter = FilterPreset.KELVIN), starved).process(patch()) {}
        }
        assertEquals(EditResult.Failed(EditFailure.OutOfMemory), result)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Runs the real processor with [settings] on [input] and returns the edited bitmap. */
    private fun run(input: Bitmap, settings: EditSettings): Bitmap {
        val result = runBlocking { EditProcessor(settings, noFilters).process(input) {} }
        assertTrue("the edit failed: $result", result is EditResult.Edited)
        return (result as EditResult.Edited).bitmap
    }

    /**
     * The 400 x 300 photo: red, green, blue and yellow quarters, each pixel's red and green nudged
     * by its x and y (modulo 16), and the top-left pixel white.
     */
    private fun quarters(): Bitmap {
        val width = 400
        val height = 300
        val data = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val right = x >= width / 2
                val bottom = y >= height / 2
                val (r, g, b) = when {
                    !right && !bottom -> Triple(200, 40, 40)
                    right && !bottom -> Triple(40, 200, 40)
                    !right -> Triple(40, 40, 200)
                    else -> Triple(200, 200, 40)
                }
                data[y * width + x] = rgb(r + x % 16, g + y % 16, b)
            }
        }
        data[0] = rgb(255, 255, 255)
        return bitmapOf(data, width, height)
    }

    /**
     * The 100 x 100 test patch: the left half mid-grey, the right half four bands of moderate
     * colours (none at full strength, so a stronger setting still has room to move them).
     */
    private fun patch(): Bitmap {
        val size = 100
        val bands = listOf(rgb(180, 100, 80), rgb(90, 140, 200), rgb(120, 170, 90), rgb(200, 160, 120))
        val data = IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            if (x < size / 2) rgb(128, 128, 128) else bands[y * bands.size / size]
        }
        return bitmapOf(data, size, size)
    }

    /** An opaque ARGB colour from its three channels. */
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    /** A bitmap of [width] x [height] holding [data], row after row. */
    private fun bitmapOf(data: IntArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(data, 0, width, 0, 0, width, height)
        return bitmap
    }

    /** Every pixel of [bitmap], row after row. */
    private fun pixels(bitmap: Bitmap): IntArray {
        val data = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return data
    }

    /** The luminance of each pixel of [bitmap] (Rec. 601 weights), 0 to 255. */
    private fun luminances(bitmap: Bitmap): List<Double> = pixels(bitmap).map { color ->
        0.299 * ((color shr 16) and 0xFF) + 0.587 * ((color shr 8) and 0xFF) + 0.114 * (color and 0xFF)
    }

    /** The mean luminance of [bitmap]. */
    private fun meanLuminance(bitmap: Bitmap): Double = luminances(bitmap).average()

    /** The standard deviation of [bitmap]'s luminance. */
    private fun luminanceSpread(bitmap: Bitmap): Double {
        val values = luminances(bitmap)
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    /** The mean, over [bitmap]'s pixels, of the largest channel minus the smallest. */
    private fun meanChroma(bitmap: Bitmap): Double = pixels(bitmap).map { color ->
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        (max(r, max(g, b)) - minOf(r, g, b)).toDouble()
    }.average()

    /** The mean red minus the mean blue of [bitmap]. */
    private fun redMinusBlue(bitmap: Bitmap): Double = pixels(bitmap).map { color ->
        (((color shr 16) and 0xFF) - (color and 0xFF)).toDouble()
    }.average()
}
