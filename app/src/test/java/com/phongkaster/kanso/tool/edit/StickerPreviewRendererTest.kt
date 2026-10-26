package com.phongkaster.kanso.tool.edit

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
import com.phongkaster.kanso.data.edit.edit.Sticker
import com.phongkaster.kanso.data.edit.edit.StickerPictures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks on Android 15, with real bitmaps, what [FilterPreviewRenderer] does with stickers (Run 10,
 * DoD 19): the small copy of the original that the Edit screen filters carries the stickers too.
 *
 * - The stickers are drawn BEFORE the filter, so a sticker under a filter comes out filtered, as
 *   it does in Done.
 * - A sticker alone (no filter) is enough to make the renderer answer with a picture.
 * - The sticker whose key is the touched one is left out (the screen draws it live instead).
 * - With no filter and no sticker left to draw there is nothing to render: null.
 *
 * Every sticker picture is a solid opaque red square of the width asked for, so a sticker pixel is
 * exactly red before any filter, and the expected filtered colour is the look-up's answer for red.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class StickerPreviewRendererTest {

    /** With a sticker and no filter the renderer still draws: the sticker is red at its place, the rest is the photo. */
    @Test
    fun a_sticker_alone_is_rendered_without_a_filter() {
        val photo = photo()
        val result = render(rendererOn(photo), EditSettings(stickers = listOf(sticker(1L, 0.5f, 0.5f, 0.5f))))
        assertNotNull("a sticker with no filter gave no picture", result)
        assertEquals(photo.width, result!!.width)
        assertEquals(photo.height, result.height)
        assertEquals("the sticker's middle is not its colour", RED, result.getPixel(48, 32) and RGB)
        assertEquals("a pixel far from the sticker changed", photo.getPixel(2, 2) and RGB, result.getPixel(2, 2) and RGB)
    }

    /** The sticker goes in before the filter: its red comes out as the look-up's answer for red, like the photo around it. */
    @Test
    fun a_sticker_under_a_filter_comes_out_filtered() {
        val photo = photo()
        val settings = EditSettings(
            filter = FilterPreset.KELVIN,
            filterIntensity = 1f,
            stickers = listOf(sticker(1L, 0.5f, 0.5f, 0.5f)),
        )
        val result = render(rendererOn(photo), settings)!!
        val expectedSticker = lut.map(OPAQUE_RED) and RGB
        assertEquals("the sticker was not filtered", expectedSticker, result.getPixel(48, 32) and RGB)
        assertEquals(
            "the photo around it was not filtered",
            lut.map(photo.getPixel(2, 2)) and RGB,
            result.getPixel(2, 2) and RGB,
        )
        // Guards the test itself: filtered red is not red, so an unfiltered sticker could not pass.
        assertNotEquals(RED, expectedSticker)
    }

    /** The touched sticker is not drawn; the other one is. */
    @Test
    fun the_touched_sticker_is_left_out() {
        val photo = photo()
        val left = sticker(1L, 0.25f, 0.5f, 0.25f)
        val right = sticker(2L, 0.75f, 0.5f, 0.25f)
        val settings = EditSettings(stickers = listOf(left, right))
        val renderer = rendererOn(photo)

        val oneTouched = runBlocking { renderer.render(settings, touchedStickerKey = 1L) }!!
        assertEquals("the touched sticker was drawn", photo.getPixel(24, 32) and RGB, oneTouched.getPixel(24, 32) and RGB)
        assertEquals("the other sticker was left out too", RED, oneTouched.getPixel(72, 32) and RGB)

        val noneTouched = runBlocking { renderer.render(settings, touchedStickerKey = null) }!!
        assertEquals(RED, noneTouched.getPixel(24, 32) and RGB)
        assertEquals(RED, noneTouched.getPixel(72, 32) and RGB)

        // A key that names no sticker leaves out nothing.
        val unknown = runBlocking { renderer.render(settings, touchedStickerKey = 99L) }!!
        assertEquals(RED, unknown.getPixel(24, 32) and RGB)
        assertEquals(RED, unknown.getPixel(72, 32) and RGB)
    }

    /** With no filter and no sticker, or only the touched sticker and no filter, there is nothing to render. */
    @Test
    fun nothing_to_draw_gives_null() {
        val renderer = rendererOn(photo())
        assertNull(render(renderer, EditSettings()))
        assertNull(render(renderer, EditSettings(brightness = 0.5f, quarterTurns = 1)))
        val only = EditSettings(stickers = listOf(sticker(1L, 0.5f, 0.5f, 0.5f)))
        assertNull(runBlocking { renderer.render(only, touchedStickerKey = 1L) })
    }

    /** The source copy is never modified by drawing stickers on it. */
    @Test
    fun the_source_is_not_modified_by_stickers() {
        val photo = photo()
        val before = pixels(photo)
        val renderer = rendererOn(photo)
        render(renderer, EditSettings(stickers = listOf(sticker(1L, 0.5f, 0.5f, 0.5f))))
        render(
            renderer,
            EditSettings(filter = FilterPreset.KELVIN, stickers = listOf(sticker(1L, 0.5f, 0.5f, 0.5f))),
        )
        assertArrayEquals("the source photo was changed", before, pixels(photo))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Sticker pictures that are a solid opaque red square, as wide as asked for. */
    private val pictures = StickerPictures { _, widthPx ->
        val edge = widthPx.coerceAtLeast(1)
        Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888).also { it.eraseColor(OPAQUE_RED) }
    }

    /** The generated look-up: red inverted, green doubled (clamped), blue a quarter of the way up from 128. */
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

    /** A sticker with [key], upright, at the given centre, [width] of the photo wide. */
    private fun sticker(key: Long, centreX: Float, centreY: Float, width: Float): Sticker = Sticker(
        key = key,
        pictureId = "red",
        centreX = centreX,
        centreY = centreY,
        width = width,
        degrees = 0f,
        mirrored = false,
    )

    /** The renderer the app builds, on the real Done processor, with [lut] and the red [pictures]. */
    private fun rendererOn(photo: Bitmap): FilterPreviewRenderer =
        FilterPreviewRenderer(photo) { settings -> EditProcessor(settings, FilterLut.Source { lut }, pictures) }

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

        /** The sticker colour without alpha. */
        const val RED = 0xFF0000

        /** The sticker colour as an opaque ARGB int. */
        const val OPAQUE_RED = 0xFFFF0000.toInt()
    }
}
