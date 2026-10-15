package com.phongkaster.kanso.tool.edit

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSession
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
 *   that shape within 1 pixel, as large as fits and centred (within 1 pixel of the photo's
 *   middle); a crop rectangle of (0.25, 0.25, 0.75, 0.75) gives 200 x 150 starting at the
 *   photo's pixel (100, 75); the crop comes from the settings' rectangle, never from the ratio;
 *   no crop gives the same pixels back;
 * - turn and flip: a clockwise quarter turn gives 300 x 400 with the top-left pixel at the top
 *   right, anticlockwise at the bottom left, four turns give the same pixels back, and a flip
 *   across moves the top-left pixel to the top right and keeps the size;
 * - colour sliders, each alone on a 100 x 100 patch of mid-grey and colours: all four at the
 *   middle change nothing; brightness moves the mean luminance, contrast its spread, saturation
 *   the colourfulness (black and white at the minimum) and warmth red against blue; the colour
 *   matrix itself is the identity in the middle;
 * - Run 9, whole settings from the original: the filter runs before the sliders, rendering twice
 *   from the same original gives the same pixels and leaves the original alone, and a second
 *   filter replaces the first rather than stacking on it;
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

    /** A filter source with no look-ups, for the tests that use no filter. */
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

    /**
     * Each fixed ratio gives its shape within 1 pixel, as large as fits, centred on the middle.
     *
     * Run 9: a ratio is no longer a branch of the processor; choosing it sets a centred rectangle
     * in the settings ([EditSession.cropRatioChosen]) and the processor crops that rectangle. The
     * rectangle is made of fractions rounded to pixels, so the crop's middle pixel may now land
     * up to 1 pixel from the photo's middle pixel (it had to be exact when the processor computed
     * the rectangle in pixels itself).
     */
    @Test
    fun each_ratio_crops_to_its_shape_around_the_middle() {
        val photo = quarters()
        val centreX = photo.width / 2
        val centreY = photo.height / 2
        CropRatio.entries.filter { it != CropRatio.FREE }.forEach { ratio ->
            val settings = EditSession().cropRatioChosen(ratio, photo.width.toFloat() / photo.height).settings
            val edited = run(photo, settings)
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
            val middle = edited.getPixel(edited.width / 2, edited.height / 2)
            val nearCentre = (-1..1).any { dx ->
                (-1..1).any { dy -> photo.getPixel(centreX + dx, centreY + dy) == middle }
            }
            assertTrue("$ratio's middle pixel is not within 1 pixel of the photo's middle", nearCentre)
        }
    }

    /**
     * A crop keeps exactly the rectangle asked for, in pixels.
     *
     * Run 9: there is one crop rectangle for every ratio (`crop`, which replaced `freeCrop`), and
     * it applies without any ratio set; `cropRatio` only locks the handles' shape.
     */
    @Test
    fun a_free_crop_keeps_its_rectangle() {
        val photo = quarters()
        val edited = run(photo, EditSettings(crop = EditSettings.CropRect(0.25f, 0.25f, 0.75f, 0.75f)))
        assertEquals(200, edited.width)
        assertEquals(150, edited.height)
        assertEquals(photo.getPixel(100, 75), edited.getPixel(0, 0))
        assertEquals(photo.getPixel(299, 224), edited.getPixel(199, 149))
    }

    /**
     * The crop comes from `settings.crop` alone: a locked ratio with the whole-photo rectangle
     * crops nothing, because the ratio only constrains the handles (Run 9, DoD 25).
     */
    @Test
    fun the_crop_comes_from_the_settings_rectangle_not_the_ratio() {
        val photo = quarters()
        val locked = run(photo, EditSettings(cropRatio = CropRatio.SQUARE, crop = EditSettings.CropRect.FULL))
        assertEquals(photo.width, locked.width)
        assertEquals(photo.height, locked.height)
        assertArrayEquals(pixels(photo), pixels(locked))

        val square = EditSettings(cropRatio = CropRatio.SQUARE, crop = EditSettings.CropRect(0.125f, 0f, 0.875f, 1f))
        val cropped = run(photo, square)
        assertEquals(300, cropped.width)
        assertEquals(300, cropped.height)
        assertEquals(photo.getPixel(50, 0), cropped.getPixel(0, 0))
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

    /**
     * Four quarter turns, either way, give the same pixels back.
     *
     * Run 9: the processor now renders the session's whole settings from the original, so four
     * turns are one value (`quarterTurns = 4`) rendered once, not four renders chained on each
     * other's output as the old one-change-at-a-time panel made them.
     */
    @Test
    fun four_turns_give_the_photo_back() {
        val photo = quarters()
        listOf(4, -4).forEach { turns ->
            val edited = run(photo, EditSettings(quarterTurns = turns))
            assertEquals(photo.width, edited.width)
            assertEquals(photo.height, edited.height)
            assertArrayEquals("$turns turns", pixels(photo), pixels(edited))
        }
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

    /**
     * The filter runs before the colour sliders (Run 9, A-002). The filter here is a hard
     * threshold (below 128 becomes black, the rest white) and brightness at its maximum adds 80:
     * grey 100 filtered first becomes 0, then 80. The other order would give 100 + 80 = 180,
     * which the threshold turns white.
     */
    @Test
    fun the_filter_is_applied_before_the_sliders() {
        val grey = bitmapOf(IntArray(10 * 10) { rgb(100, 100, 100) }, 10, 10)
        val settings = EditSettings(filter = FilterPreset.KELVIN, brightness = EditSettings.MAX)
        val edited = run(grey, settings, testFilters())
        pixels(edited).forEach { assertEquals(rgb(80, 80, 80), it) }
    }

    /**
     * The processor renders whole settings from the original and changes nothing it is given, so
     * rendering the same settings twice from the same original gives the same pixels: nothing
     * stacks or is baked in (Run 9, DoD 6, 25).
     */
    @Test
    fun rendering_twice_from_the_original_gives_the_same_pixels() {
        val photo = quarters()
        val before = pixels(photo)
        val settings = EditSettings(
            crop = EditSettings.CropRect(0.1f, 0.2f, 0.9f, 0.8f),
            quarterTurns = 1,
            flipHorizontal = true,
            filter = FilterPreset.HUDSON,
            filterIntensity = 0.6f,
            brightness = 0.3f,
            warmth = -0.4f,
        )
        val first = run(photo, settings, testFilters())
        val second = run(photo, settings, testFilters())
        assertArrayEquals("the original was changed", before, pixels(photo))
        assertEquals(first.width, second.width)
        assertEquals(first.height, second.height)
        assertArrayEquals(pixels(first), pixels(second))
    }

    /**
     * Choosing a second filter replaces the first (Run 9, DoD R1): rendering the inverting filter
     * from the original gives grey 100 -> 155, whatever filter was shown before. Had it run on the
     * thresholded picture (100 -> 0) it would give 255.
     */
    @Test
    fun a_second_filter_replaces_the_first() {
        val grey = bitmapOf(IntArray(10 * 10) { rgb(100, 100, 100) }, 10, 10)
        val thresholded = run(grey, EditSettings(filter = FilterPreset.KELVIN), testFilters())
        pixels(thresholded).forEach { assertEquals(rgb(0, 0, 0), it) }
        val inverted = run(grey, EditSettings(filter = FilterPreset.HUDSON), testFilters())
        pixels(inverted).forEach { assertEquals(rgb(155, 155, 155), it) }
    }

    /**
     * The colour matrix is reachable from tests (`internal`) and is the identity with every
     * slider in the middle; brightness at its maximum adds [EditProcessor.BRIGHTNESS_SHIFT] to
     * each of red, green and blue and leaves the rest alone (Run 9).
     */
    @Test
    fun the_colour_matrix_is_the_identity_in_the_middle() {
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        assertArrayEquals(identity, EditProcessor.colourMatrix(EditSettings()), 0.0001f)
        val brighter = EditProcessor.colourMatrix(EditSettings(brightness = EditSettings.MAX))
        val expected = identity.copyOf().also {
            it[4] = EditProcessor.BRIGHTNESS_SHIFT
            it[9] = EditProcessor.BRIGHTNESS_SHIFT
            it[14] = EditProcessor.BRIGHTNESS_SHIFT
        }
        assertArrayEquals(expected, brighter, 0.0001f)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Runs the real processor with [settings] and [filters] on [input] and returns the edited bitmap. */
    private fun run(input: Bitmap, settings: EditSettings, filters: FilterLut.Source = noFilters): Bitmap {
        val result = runBlocking { EditProcessor(settings, filters).process(input) {} }
        assertTrue("the edit failed: $result", result is EditResult.Edited)
        return (result as EditResult.Edited).bitmap
    }

    /**
     * Two generated filters, each a 256 x 1 grey strip read as three equal tone curves: KELVIN
     * stands for a hard threshold (below 128 black, the rest white) and HUDSON for an inversion
     * (v becomes 255 - v). Every other preset has no look-up.
     */
    private fun testFilters(): FilterLut.Source {
        val threshold = FilterLut(strip { v -> if (v < 128) 0 else 255 })
        val invert = FilterLut(strip { v -> 255 - v })
        return FilterLut.Source { preset ->
            when (preset) {
                FilterPreset.KELVIN -> threshold
                FilterPreset.HUDSON -> invert
                else -> null
            }
        }
    }

    /** A 256 x 1 strip whose column v is the grey [curve] of v: one tone curve for all three channels. */
    private fun strip(curve: (Int) -> Int): Bitmap =
        bitmapOf(IntArray(256) { v -> curve(v).let { rgb(it, it, it) } }, 256, 1)

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
