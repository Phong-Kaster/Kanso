package com.phongkaster.kanso.tool.edit

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.Sticker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Checks how [EditProcessor] draws stickers, on Android 15 with real bitmap drawing (Run 10,
 * DoD 15, 16 and 18): a crop cuts a sticker; a turn and a flip carry it with the photo; a filter
 * and the colour sliders colour it; the output size does not depend on stickers; a one-pixel
 * checkerboard drawn 1:1 appears pixel for pixel; no stickers gives the old pixels.
 *
 * The photo is a flat grey, so any pixel that is not grey is a sticker pixel. The fake
 * [SolidStickerPictures] makes square pictures, so a sticker of width 0.2 on a 100 x 100 photo is a
 * 20 x 20 block.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class EditProcessorStickerTest {

    /** The photo's flat grey. */
    private val grey = rgb(100, 100, 100)

    /** A filter source for the tests that do not filter. */
    private val noFilters = FilterLut.Source { null }

    /** One solid sticker at [centreX], [centreY] of the given [width] (all fractions of the photo). */
    private fun sticker(centreX: Float, centreY: Float, width: Float, id: String = "solid") = Sticker(
        key = 1, pictureId = id, centreX = centreX, centreY = centreY, width = width, degrees = 0f, mirrored = false,
    )

    /** A 40 px (0.4 wide) four-quadrant sticker in the middle of a 100 x 100 photo (covers 30..69). */
    private fun quad(degrees: Float = 0f, mirrored: Boolean = false) = Sticker(
        key = 1, pictureId = SolidStickerPictures.QUAD, centreX = 0.5f, centreY = 0.5f, width = 0.4f,
        degrees = degrees, mirrored = mirrored,
    )

    /** Checks the colour at the middle of each quarter of the 40 x 40 square at 30..69. */
    private fun assertQuadrants(edited: Bitmap, topLeft: Int, topRight: Int, bottomLeft: Int, bottomRight: Int) {
        assertEquals("top left", topLeft, edited.getPixel(40, 40))
        assertEquals("top right", topRight, edited.getPixel(60, 40))
        assertEquals("bottom left", bottomLeft, edited.getPixel(40, 60))
        assertEquals("bottom right", bottomRight, edited.getPixel(60, 60))
    }

    /** A crop through the middle of the sticker keeps only the kept part of it. */
    @Test
    fun a_crop_through_a_sticker_keeps_only_the_kept_part() {
        val settings = EditSettings(
            crop = EditSettings.CropRect(0.5f, 0f, 1f, 1f),
            stickers = listOf(sticker(0.5f, 0.5f, 0.4f)), // covers x 30..69, y 30..69
        )
        val edited = run(photo(100, 100), settings)
        assertEquals(50, edited.width)
        assertEquals(100, edited.height)
        assertEquals(SolidStickerPictures.SOLID, edited.getPixel(0, 50))
        assertEquals(SolidStickerPictures.SOLID, edited.getPixel(19, 50))
        assertEquals(grey, edited.getPixel(20, 50))
        assertEquals(grey, edited.getPixel(10, 10))
        assertEquals(grey, edited.getPixel(10, 75))
    }

    /** A crop that leaves the sticker out leaves no sticker pixel. */
    @Test
    fun a_crop_that_excludes_a_sticker_shows_none_of_it() {
        val settings = EditSettings(
            crop = EditSettings.CropRect(0.75f, 0f, 1f, 1f),
            stickers = listOf(sticker(0.4f, 0.5f, 0.2f)), // covers x 30..49
        )
        val edited = run(photo(100, 100), settings)
        assertTrue(pixels(edited).all { it == grey })
    }

    /** One quarter turn right then a horizontal flip put the sticker at the turned, flipped place. */
    @Test
    fun a_turn_and_a_flip_carry_the_sticker_with_the_photo() {
        val settings = EditSettings(
            quarterTurns = 1,
            flipHorizontal = true,
            stickers = listOf(sticker(0.25f, 0.75f, 0.2f)), // x 15..34, y 65..84 before the turn
        )
        val edited = run(photo(100, 100), settings)
        // Turn right: (x, y) -> (99 - y, x), so x 15..34, y 65..84 -> x 15..34, y 15..34.
        // Flip across: x -> 99 - x, so x 65..84, y 15..34.
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                val inside = x in 65..84 && y in 15..34
                assertEquals("pixel ($x, $y)", if (inside) SolidStickerPictures.SOLID else grey, edited.getPixel(x, y))
            }
        }
    }

    /** A filter at full strength colours the sticker like the photo: sticker pixel = filtered colour. */
    @Test
    fun a_filter_at_full_strength_colours_the_sticker() {
        val filters = invertFilter()
        val lut = filters.lutFor(FilterPreset.HUDSON)!!
        val settings = EditSettings(
            filter = FilterPreset.HUDSON,
            filterIntensity = 1f,
            stickers = listOf(sticker(0.5f, 0.5f, 0.2f)),
        )
        val edited = run(photo(100, 100), settings, filters)
        assertEquals(lut.map(SolidStickerPictures.SOLID) or OPAQUE, edited.getPixel(50, 50))
        assertEquals(lut.map(grey) or OPAQUE, edited.getPixel(5, 5))
    }

    /** Raised brightness moves the sticker's colour through the same colour matrix as the photo. */
    @Test
    fun a_colour_slider_colours_the_sticker() {
        val settings = EditSettings(brightness = 0.5f, stickers = listOf(sticker(0.5f, 0.5f, 0.2f)))
        val matrix = EditProcessor.colourMatrix(settings)
        val edited = run(photo(100, 100), settings)
        assertClose(through(matrix, SolidStickerPictures.SOLID), edited.getPixel(50, 50), 1)
        assertClose(through(matrix, grey), edited.getPixel(5, 5), 1)
        assertTrue(edited.getPixel(50, 50) != SolidStickerPictures.SOLID)
    }

    /** Stickers never change the output's size, with or without a crop and a turn. */
    @Test
    fun stickers_do_not_change_the_output_size() {
        val base = EditSettings(crop = EditSettings.CropRect(0.1f, 0.2f, 0.9f, 0.7f), quarterTurns = 1)
        val without = run(photo(100, 100), base)
        val with = run(photo(100, 100), base.copy(stickers = listOf(sticker(0.5f, 0.5f, 0.6f), sticker(0.0f, 0.0f, 0.5f))))
        assertEquals(without.width, with.width)
        assertEquals(without.height, with.height)
    }

    /** A one-pixel checkerboard sticker placed 1:1 with no turn appears pixel for pixel. */
    @Test
    fun a_checkerboard_sticker_at_scale_one_appears_pixel_for_pixel() {
        // 32 x 32 photo, sticker 0.25 wide = 8 px, centred: covers x 12..19, y 12..19.
        val settings = EditSettings(stickers = listOf(sticker(0.5f, 0.5f, 0.25f, SolidStickerPictures.CHECKER)))
        val edited = run(photo(32, 32), settings)
        assertEquals(32, edited.width)
        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val expected = if (x in 12..19 && y in 12..19) {
                    if (((x - 12) + (y - 12)) % 2 == 0) SolidStickerPictures.CHECKER_A else SolidStickerPictures.CHECKER_B
                } else {
                    grey
                }
                assertClose(expected, edited.getPixel(x, y), 1)
            }
        }
    }

    /**
     * A checkerboard whose left edge falls a quarter of a pixel into a pixel is still copied pixel
     * for pixel: smoothing would blend neighbours there (a quarter of the next colour), so any
     * pixel that is not exactly one of the two colours would fail this.
     */
    @Test
    fun a_checkerboard_at_a_quarter_pixel_offset_is_not_smoothed() {
        // 32 x 32 photo, 8 px wide, centre x = 16.25 -> the picture starts at x = 12.25.
        val settings = EditSettings(stickers = listOf(sticker(0.5078125f, 0.5f, 0.25f, SolidStickerPictures.CHECKER)))
        val edited = run(photo(32, 32), settings)
        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val expected = if (x in 12..19 && y in 12..19) {
                    if (((x - 12) + (y - 12)) % 2 == 0) SolidStickerPictures.CHECKER_A else SolidStickerPictures.CHECKER_B
                } else {
                    grey
                }
                assertEquals("pixel ($x, $y)", expected, edited.getPixel(x, y))
            }
        }
    }

    /** A sticker turned 90 degrees clockwise: each quadrant moves to the next corner clockwise. */
    @Test
    fun a_sticker_turned_ninety_degrees_goes_clockwise_about_its_centre() {
        val edited = run(photo(100, 100), EditSettings(stickers = listOf(quad(degrees = 90f))))
        // Top-left red -> top-right, top-right green -> bottom-right, bottom-right yellow ->
        // bottom-left, bottom-left blue -> top-left.
        assertQuadrants(
            edited,
            topLeft = SolidStickerPictures.QUAD_BOTTOM_LEFT,
            topRight = SolidStickerPictures.QUAD_TOP_LEFT,
            bottomLeft = SolidStickerPictures.QUAD_BOTTOM_RIGHT,
            bottomRight = SolidStickerPictures.QUAD_TOP_RIGHT,
        )
        // Still the same 40 x 40 square about the centre, nothing outside it.
        assertEquals(grey, edited.getPixel(25, 25))
        assertEquals(grey, edited.getPixel(74, 74))
    }

    /** At 45 degrees clockwise the quadrants' centres move to the top, right, bottom and left. */
    @Test
    fun a_sticker_turned_forty_five_degrees_lands_on_the_diagonal_positions() {
        val edited = run(photo(100, 100), EditSettings(stickers = listOf(quad(degrees = 45f))))
        // A quadrant centre is 10 px right and 10 px down/up of the middle (14.14 px away); turned
        // clockwise by 45 degrees the top-left one (-10, -10) lands straight above the middle.
        assertEquals(SolidStickerPictures.QUAD_TOP_LEFT, edited.getPixel(50, 36))
        assertEquals(SolidStickerPictures.QUAD_TOP_RIGHT, edited.getPixel(64, 50))
        assertEquals(SolidStickerPictures.QUAD_BOTTOM_RIGHT, edited.getPixel(50, 64))
        assertEquals(SolidStickerPictures.QUAD_BOTTOM_LEFT, edited.getPixel(36, 50))
        // The square's old corner is empty after the turn: |dx| + |dy| = 38 is beyond 28.3.
        assertEquals(grey, edited.getPixel(31, 31))
        assertEquals(grey, edited.getPixel(68, 68))
    }

    /** A mirrored sticker swaps left and right. */
    @Test
    fun a_mirrored_sticker_swaps_left_and_right() {
        val edited = run(photo(100, 100), EditSettings(stickers = listOf(quad(mirrored = true))))
        assertQuadrants(
            edited,
            topLeft = SolidStickerPictures.QUAD_TOP_RIGHT,
            topRight = SolidStickerPictures.QUAD_TOP_LEFT,
            bottomLeft = SolidStickerPictures.QUAD_BOTTOM_RIGHT,
            bottomRight = SolidStickerPictures.QUAD_BOTTOM_LEFT,
        )
    }

    /** The mirror is applied before the turn: mirror first, then 90 degrees clockwise. */
    @Test
    fun a_mirrored_and_turned_sticker_mirrors_first_then_turns() {
        val edited = run(photo(100, 100), EditSettings(stickers = listOf(quad(degrees = 90f, mirrored = true))))
        // Mirrored: TL green, TR red, BL yellow, BR blue. Then clockwise 90: TL -> TR, TR -> BR,
        // BR -> BL, BL -> TL. (Turning first and mirroring after would give red, blue, green, yellow.)
        assertQuadrants(
            edited,
            topLeft = SolidStickerPictures.QUAD_BOTTOM_RIGHT,
            topRight = SolidStickerPictures.QUAD_TOP_RIGHT,
            bottomLeft = SolidStickerPictures.QUAD_BOTTOM_LEFT,
            bottomRight = SolidStickerPictures.QUAD_TOP_LEFT,
        )
    }

    /**
     * On a 200 x 100 photo cropped from (50, 50) the sticker's width is a fraction of the photo's
     * width (20 px, not 10), its centre uses the width and the height separately, and the crop's
     * left AND top both shift it.
     */
    @Test
    fun a_sticker_on_a_wide_cropped_photo_lands_by_width_height_and_crop_corner() {
        val settings = EditSettings(
            crop = EditSettings.CropRect(0.25f, 0.5f, 0.75f, 1f), // x 50..149, y 50..99
            stickers = listOf(sticker(0.5f, 0.75f, 0.1f)), // centre (100, 75), 20 px: x 90..109, y 65..84
        )
        val edited = run(photo(200, 100), settings)
        assertEquals(100, edited.width)
        assertEquals(50, edited.height)
        // In the crop the sticker covers x 40..59, y 15..34.
        for (y in 0 until 50) {
            for (x in 0 until 100) {
                val inside = x in 40..59 && y in 15..34
                assertEquals("pixel ($x, $y)", if (inside) SolidStickerPictures.SOLID else grey, edited.getPixel(x, y))
            }
        }
    }

    /** A half see-through sticker mixes with the photo: s * a + d * (255 - a), rounded, per channel. */
    @Test
    fun a_half_see_through_sticker_blends_with_the_photo() {
        val edited = run(photo(100, 100), EditSettings(stickers = listOf(sticker(0.5f, 0.5f, 0.2f, SolidStickerPictures.HALF))))
        val alpha = 128
        val expected = rgb(
            (255 * alpha + 100 * (255 - alpha) + 127) / 255,
            (0 * alpha + 100 * (255 - alpha) + 127) / 255,
            (0 * alpha + 100 * (255 - alpha) + 127) / 255,
        )
        assertClose(expected, edited.getPixel(50, 50), 1)
        assertClose(expected, edited.getPixel(41, 41), 1)
        // It really is a mix, neither the sticker's red nor the photo's grey.
        assertTrue(edited.getPixel(50, 50) != grey)
        assertTrue(((edited.getPixel(50, 50) shr 16) and 0xFF) < 255)
        assertEquals(grey, edited.getPixel(5, 5))
    }

    /** An over-sized sticker asks for a picture no wider than the photo's long side, and still covers it. */
    @Test
    fun an_oversized_sticker_decodes_no_wider_than_the_photo() {
        val pictures = SolidStickerPictures()
        val result = runBlocking {
            EditProcessor(EditSettings(stickers = listOf(sticker(0.5f, 0.5f, 50f))), noFilters, pictures)
                .process(photo(100, 60)) {}
        }
        assertTrue("the edit failed: $result", result is EditResult.Edited)
        assertEquals(listOf(100), pictures.requestedWidths)
        val edited = (result as EditResult.Edited).bitmap
        assertTrue(pixels(edited).all { it == SolidStickerPictures.SOLID })
    }

    /** A sticker with a NaN or infinite number draws nothing and does not fail the edit. */
    @Test
    fun a_sticker_with_a_non_finite_number_is_skipped() {
        val good = sticker(0.5f, 0.5f, 0.2f)
        val bad = listOf(
            good.copy(key = 2, width = Float.NaN),
            good.copy(key = 3, width = Float.POSITIVE_INFINITY),
            good.copy(key = 4, centreX = Float.NaN),
            good.copy(key = 5, centreY = Float.POSITIVE_INFINITY),
            good.copy(key = 6, degrees = Float.NaN),
        )
        val edited = run(photo(100, 100), EditSettings(stickers = bad))
        assertTrue(pixels(edited).all { it == grey })
    }

    /** With no stickers the pixels are the old ones, even when a picture source is given. */
    @Test
    fun no_stickers_gives_the_old_pixels() {
        val input = photo(100, 100)
        val edited = run(input, EditSettings())
        assertArrayEquals(pixels(input), pixels(edited))
        val cropped = run(input, EditSettings(crop = EditSettings.CropRect(0.25f, 0.25f, 0.75f, 0.75f)))
        assertEquals(50, cropped.width)
        assertTrue(pixels(cropped).all { it == grey })
    }

    /** Renders [settings] over [input] with the fake sticker pictures and returns the bitmap. */
    private fun run(input: Bitmap, settings: EditSettings, filters: FilterLut.Source = noFilters): Bitmap {
        val result = runBlocking { EditProcessor(settings, filters, SolidStickerPictures()).process(input) {} }
        assertTrue("the edit failed: $result", result is EditResult.Edited)
        return (result as EditResult.Edited).bitmap
    }

    /** A [width] x [height] photo of flat grey. */
    private fun photo(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(IntArray(width * height) { grey }, 0, width, 0, 0, width, height)
        return bitmap
    }

    /** Every pixel of [bitmap], row after row. */
    private fun pixels(bitmap: Bitmap): IntArray {
        val data = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return data
    }

    /** An opaque colour from its three channels. */
    private fun rgb(r: Int, g: Int, b: Int): Int = OPAQUE or (r shl 16) or (g shl 8) or b

    /** A filter source whose HUDSON look-up inverts every channel (v becomes 255 - v). */
    private fun invertFilter(): FilterLut.Source {
        val strip = Bitmap.createBitmap(256, 1, Bitmap.Config.ARGB_8888)
        strip.setPixels(IntArray(256) { v -> rgb(255 - v, 255 - v, 255 - v) }, 0, 256, 0, 0, 256, 1)
        val lut = FilterLut(strip)
        return FilterLut.Source { preset -> if (preset == FilterPreset.HUDSON) lut else null }
    }

    /** Sends [color] through the 4 x 5 colour [matrix] the way the processor does, rounded and clamped. */
    private fun through(matrix: FloatArray, color: Int): Int {
        val r = ((color shr 16) and 0xFF).toFloat()
        val g = ((color shr 8) and 0xFF).toFloat()
        val b = (color and 0xFF).toFloat()
        fun channel(row: Int): Int {
            val v = matrix[row] * r + matrix[row + 1] * g + matrix[row + 2] * b + matrix[row + 3] * 255f + matrix[row + 4]
            return (if (v <= 0f) 0f else if (v >= 255f) 255f else v + 0.5f).toInt().coerceIn(0, 255)
        }
        return rgb(channel(0), channel(5), channel(10))
    }

    /** Fails unless each channel of [actual] is within [tolerance] of [expected]. */
    private fun assertClose(expected: Int, actual: Int, tolerance: Int) {
        for (shift in intArrayOf(16, 8, 0)) {
            val difference = abs(((expected shr shift) and 0xFF) - ((actual shr shift) and 0xFF))
            assertTrue(
                "expected ${Integer.toHexString(expected)} but was ${Integer.toHexString(actual)}",
                difference <= tolerance,
            )
        }
    }

    private companion object {
        /** The alpha bits of a fully opaque ARGB colour. */
        const val OPAQUE = -0x1000000
    }
}
