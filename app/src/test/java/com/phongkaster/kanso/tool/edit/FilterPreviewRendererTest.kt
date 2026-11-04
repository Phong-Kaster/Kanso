package com.phongkaster.kanso.tool.edit

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.HslAdjustments
import com.phongkaster.kanso.data.edit.edit.HslChannel
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.max
import kotlin.math.min

/**
 * Checks on Android 15, with real bitmaps, the part of the Edit screen's live preview that the CPU
 * computes: [FilterPreviewRenderer], the small original with only the filter applied.
 *
 * - With no filter there is nothing to compute: it answers null (the screen draws the original).
 * - At intensity 1 every pixel is the original's pixel sent through the look-up, and the picture
 *   is the source's size; at 0.5 every channel lies between the original and the filtered one.
 * - Only the filter is applied: crop, turns, flips and sliders in the settings are left for
 *   Compose to draw, so they must not change the picture here.
 * - The source, the original photo's small copy, is never modified, however often it is used.
 * - A filter whose look-up cannot be found gives null, not a crash.
 *
 * The renderer is built as the app builds it, on the real [EditProcessor], with a generated
 * look-up (a 256 x 6 strip of three tone curves) instead of the engine's images.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class FilterPreviewRendererTest {

    /** Without a filter, even with every other setting changed, there is nothing to render. */
    @Test
    fun no_filter_gives_null() {
        val renderer = rendererOn(photo())
        assertNull(render(renderer, EditSettings()))
        assertNull(
            render(
                renderer,
                EditSettings(brightness = 0.5f, quarterTurns = 1, crop = EditSettings.CropRect(0.1f, 0.1f, 0.9f, 0.9f)),
            ),
        )
        assertNull(render(renderer, EditSettings(filter = FilterPreset.NONE, filterIntensity = 1f)))
    }

    /** At intensity 1 each pixel is the original's pixel through the look-up, at the source's size. */
    @Test
    fun full_intensity_is_the_original_through_the_look_up() {
        val photo = photo()
        val result = render(rendererOn(photo), EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 1f))
        assertNotNull(result)
        assertEquals(photo.width, result!!.width)
        assertEquals(photo.height, result.height)
        val expected = pixels(photo).map { lut.map(it) and RGB }.toIntArray()
        val actual = pixels(result).map { it and RGB }.toIntArray()
        assertArrayEquals(expected, actual)
    }

    /**
     * At intensity 0.5 each channel lies between the original's and the fully filtered one, and
     * the picture differs from both (it is a real mix, not one of the two).
     */
    @Test
    fun half_intensity_lies_between_the_original_and_the_filtered() {
        val photo = photo()
        val renderer = rendererOn(photo)
        val full = pixels(render(renderer, EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 1f))!!)
        val half = render(renderer, EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 0.5f))
        assertNotNull(half)
        assertEquals(photo.width, half!!.width)
        assertEquals(photo.height, half.height)
        val original = pixels(photo)
        val mixed = pixels(half)
        for (i in original.indices) {
            for (shift in intArrayOf(16, 8, 0)) {
                val from = (original[i] shr shift) and 0xFF
                val to = (full[i] shr shift) and 0xFF
                val value = (mixed[i] shr shift) and 0xFF
                assertTrue(
                    "pixel $i, channel shift $shift: $value is not between $from and $to",
                    value in min(from, to)..max(from, to),
                )
            }
        }
        assertTrue(
            "half intensity gave the original",
            original.indices.any { (mixed[it] and RGB) != (original[it] and RGB) },
        )
        assertTrue(
            "half intensity gave the full filter",
            original.indices.any { (mixed[it] and RGB) != (full[it] and RGB) },
        )
    }

    /**
     * Only the filter is applied: settings that also crop, turn, flip and move every slider give
     * exactly the filter-only picture, the source's size, because Compose draws the rest on top.
     */
    @Test
    fun only_the_filter_is_applied() {
        val photo = photo()
        val renderer = rendererOn(photo)
        val filterOnly = render(renderer, EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 0.7f))!!
        val everything = render(
            renderer,
            EditSettings(
                filter = FilterPreset.KELVIN,
                filterIntensity = 0.7f,
                crop = EditSettings.CropRect(0.2f, 0.1f, 0.8f, 0.7f),
                quarterTurns = 1,
                flipHorizontal = true,
                flipVertical = true,
                brightness = 0.6f,
                contrast = -0.4f,
                saturation = 0.8f,
                warmth = -1f,
            ),
        )!!
        assertEquals(photo.width, everything.width)
        assertEquals(photo.height, everything.height)
        assertArrayEquals(pixels(filterOnly), pixels(everything))
    }

    /** The source is never modified: after renders at several intensities its pixels are the same. */
    @Test
    fun the_source_is_never_modified() {
        val photo = photo()
        val before = pixels(photo)
        val renderer = rendererOn(photo)
        listOf(1f, 0.5f, 0.2f, 1f).forEach { intensity ->
            assertNotNull(render(renderer, EditSettings(filter = FilterPreset.KELVIN, filterIntensity = intensity)))
        }
        assertArrayEquals("the source photo was changed", before, pixels(photo))
    }

    /** A filter whose look-up cannot be found gives null rather than a picture or a crash. */
    @Test
    fun a_missing_look_up_gives_null() {
        val renderer = FilterPreviewRenderer(photo()) { settings -> EditProcessor(settings, FilterLut.Source { null }) }
        assertNull(render(renderer, EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 1f)))
    }

    /**
     * DoD 17: with no filter and no sticker, an HSL value that exists only in the preview still
     * produces a picture (not null), and it differs from the unfiltered photo, so dragging an HSL
     * slider updates the photo on screen even when no filter is chosen.
     */
    @Test
    fun an_hsl_change_alone_gives_a_changed_preview() {
        val photo = photo()
        var hsl = HslAdjustments.NEUTRAL
        ColourRange.entries.forEach { hsl = hsl.with(it, HslChannel.SATURATION, -1f) }
        val result = render(rendererOn(photo), EditSettings(hsl = hsl))
        assertNotNull("an HSL change alone gave no preview", result)
        assertEquals(photo.width, result!!.width)
        assertEquals(photo.height, result.height)
        val original = pixels(photo)
        val changed = pixels(result)
        assertTrue(
            "the HSL change left the photo as it was",
            original.indices.any { (original[it] and RGB) != (changed[it] and RGB) },
        )
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * The generated look-up: a 256 x 6 strip read as three tone curves (rows 0-1 red, 2-3 green,
     * 4-5 blue): red inverted, green doubled (clamped), blue a quarter of the way up from 128.
     */
    private val lut: FilterLut = run {
        val width = 256
        val data = IntArray(width * 6) { index ->
            val v = index % width
            val value = when (index / width) {
                0, 1 -> 255 - v
                2, 3 -> minOf(255, v * 2)
                else -> 128 + (v - 128) / 4
            }
            rgb(value, value, value)
        }
        FilterLut(bitmapOf(data, width, 6))
    }

    /** The renderer the app builds, on the real Done processor, with [lut] for every preset. */
    private fun rendererOn(photo: Bitmap): FilterPreviewRenderer =
        FilterPreviewRenderer(photo) { settings -> EditProcessor(settings, FilterLut.Source { lut }) }

    /** Runs [renderer] for [settings], waiting for its background work. */
    private fun render(renderer: FilterPreviewRenderer, settings: EditSettings): Bitmap? =
        runBlocking { renderer.render(settings) }

    /** A 96 x 64 photo: red rising across, green down, blue falling along the diagonal. */
    private fun photo(): Bitmap {
        val width = 96
        val height = 64
        val data = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            rgb(x * 255 / (width - 1), y * 255 / (height - 1), 255 - (x + y) * 255 / (width + height - 2))
        }
        return bitmapOf(data, width, height)
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

    /** Values shared by the tests. */
    private companion object {
        /** The red, green and blue bits of an ARGB colour (alpha left out). */
        const val RGB = 0xFFFFFF
    }
}
