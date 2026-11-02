package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.max

/**
 * Checks the Edit tool's filters on Android 15 with real bitmap drawing (DoD 4), on the same
 * 100 x 100 patch of mid-grey and colours as `EditProcessorTest`:
 *
 * - None gives the patch back pixel for pixel; so does any filter at intensity 0;
 * - there are at least six named filters, and each one, read from the engine's own look-up image
 *   in the app's merged assets (the real [FilterLut.fromAssets]), changes the patch's mean colour;
 * - Autumn turns a grey and a pale blue warm;
 * - at intensity 0.5 each channel's mean lies between the patch's and the full filter's;
 * - the two kinds of look-up image are read the right way: a generated identity colour cube
 *   keeps every colour (within the cube's rounding), a generated inverting curve strip inverts;
 * - a filter whose image cannot be found fails with a plain "something went wrong".
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class EditFilterTest {

    /** The application under test, whose assets hold the engine's look-up images. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** The real source, reading the engine's images from the app's assets. */
    private val bundled: FilterLut.Source by lazy {
        FilterLut.fromAssets { path -> context.assets.open(path) }
    }

    /** None gives the patch back exactly. */
    @Test
    fun none_gives_the_photo_back() {
        val patch = patch()
        val edited = run(patch, EditSettings(filter = FilterPreset.NONE), bundled)
        assertArrayEquals(pixels(patch), pixels(edited))
    }

    /** Intensity 0 gives the patch back exactly, whatever the filter. */
    @Test
    fun intensity_zero_gives_the_photo_back() {
        val patch = patch()
        val edited = run(patch, EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 0f), bundled)
        assertArrayEquals(pixels(patch), pixels(edited))
    }

    /** There are at least six named filters besides None, each with its own image. */
    @Test
    fun there_are_at_least_six_filters() {
        val named = FilterPreset.entries.filter { it != FilterPreset.NONE }
        assertTrue("only ${named.size} filters", named.size >= 6)
        assertEquals(named.size, named.mapNotNull { it.assetPath }.toSet().size)
    }

    /** Each filter, from the engine's bundled image, changes the patch's mean colour. */
    @Test
    fun each_bundled_filter_changes_the_colours() {
        val patch = patch()
        val before = channelMeans(patch)
        FilterPreset.entries.filter { it != FilterPreset.NONE }.forEach { preset ->
            assertNotNull("$preset's image ${preset.assetPath} could not be read", bundled.lutFor(preset))
            val after = channelMeans(run(patch, EditSettings(filter = preset), bundled))
            val change = (0..2).maxOf { abs(after[it] - before[it]) }
            assertTrue("$preset barely changes the colours (largest mean change $change)", change > 3.0)
        }
    }

    /** At half intensity each channel's mean lies between the patch's and the full filter's. */
    @Test
    fun half_intensity_lies_between_the_photo_and_the_filter() {
        val patch = patch()
        val before = channelMeans(patch)
        FilterPreset.entries.filter { it != FilterPreset.NONE }.forEach { preset ->
            val full = channelMeans(run(patch, EditSettings(filter = preset, filterIntensity = 1f), bundled))
            val half = channelMeans(run(patch, EditSettings(filter = preset, filterIntensity = 0.5f), bundled))
            for (channel in 0..2) {
                val low = minOf(before[channel], full[channel]) - 1.0
                val high = max(before[channel], full[channel]) + 1.0
                assertTrue(
                    "$preset channel $channel: half ${half[channel]} is not between " +
                        "${before[channel]} and ${full[channel]}",
                    half[channel] in low..high,
                )
            }
        }
    }

    /**
     * Autumn, Kanso's own image, is read as a colour cube and does what its name says: a neutral
     * grey turns brown (red above green above blue) and darker, a pale denim blue turns warm (red
     * above blue).
     */
    @Test
    fun autumn_turns_grey_and_pale_blue_warm() {
        val lut = bundled.lutFor(FilterPreset.AUTUMN)!!
        assertTrue(lut.isCube)
        val grey = lut.map(rgb(128, 128, 128))
        val denim = lut.map(rgb(0xB9, 0xC6, 0xCC))
        assertTrue(((grey shr 16) and 0xFF) > ((grey shr 8) and 0xFF))
        assertTrue(((grey shr 8) and 0xFF) > (grey and 0xFF))
        assertTrue(((grey shr 16) and 0xFF) < 128)
        assertTrue(((denim shr 16) and 0xFF) > (denim and 0xFF))
    }

    /** A generated 512 x 512 identity cube is read as a cube and keeps each colour within 3. */
    @Test
    fun an_identity_cube_keeps_the_colours() {
        val lut = FilterLut(identityCube())
        assertTrue(lut.isCube)
        pixels(patch()).forEach { color ->
            val mapped = lut.map(color)
            for (shift in listOf(16, 8, 0)) {
                val difference = abs(((mapped shr shift) and 0xFF) - ((color shr shift) and 0xFF))
                assertTrue("channel at bit $shift moved by $difference", difference <= 3)
            }
        }
    }

    /** A generated 256 x 3 strip running from white to black is read as curves that invert. */
    @Test
    fun an_inverting_strip_inverts_the_colours() {
        val lut = FilterLut(invertingStrip())
        assertFalse(lut.isCube)
        assertEquals(rgb(245, 155, 55), lut.map(rgb(10, 100, 200)))
    }

    /** A filter whose image is missing fails plainly instead of giving a wrong picture. */
    @Test
    fun a_missing_filter_image_fails_plainly() {
        val result = runBlocking {
            EditProcessor(EditSettings(filter = FilterPreset.KELVIN), FilterLut.Source { null }).process(patch()) {}
        }
        assertEquals(EditResult.Failed(EditFailure.Other), result)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Runs the real processor with [settings] and [filters] on [input], returning the result. */
    private fun run(input: Bitmap, settings: EditSettings, filters: FilterLut.Source): Bitmap {
        val result = runBlocking { EditProcessor(settings, filters).process(input) {} }
        assertTrue("the edit failed: $result", result is EditResult.Edited)
        return (result as EditResult.Edited).bitmap
    }

    /** The 100 x 100 patch: the left half mid-grey, the right half four bands of moderate colours. */
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

    /**
     * A standard 512 x 512 identity colour cube: 64 tiles of 64 x 64 in an 8 x 8 grid, the tile
     * number the blue level, x in a tile the red level, y the green level.
     */
    private fun identityCube(): Bitmap {
        val size = 512
        val data = IntArray(size * size)
        for (blue in 0 until 64) {
            val tileX = (blue % 8) * 64
            val tileY = (blue / 8) * 64
            for (green in 0 until 64) {
                for (red in 0 until 64) {
                    data[(tileY + green) * size + tileX + red] = rgb(level(red), level(green), level(blue))
                }
            }
        }
        return bitmapOf(data, size, size)
    }

    /** A 256 x 3 strip whose column x is the grey 255 - x in every row. */
    private fun invertingStrip(): Bitmap {
        val data = IntArray(256 * 3) { index -> (255 - index % 256).let { rgb(it, it, it) } }
        return bitmapOf(data, 256, 3)
    }

    /** Turns a cube level (0 to 63) into its channel value (0 to 255), rounded. */
    private fun level(value: Int): Int = (value * 255 + 31) / 63

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

    /** The mean red, green and blue of [bitmap], in that order. */
    private fun channelMeans(bitmap: Bitmap): DoubleArray {
        val data = pixels(bitmap)
        return doubleArrayOf(
            data.map { (it shr 16) and 0xFF }.average(),
            data.map { (it shr 8) and 0xFF }.average(),
            data.map { it and 0xFF }.average(),
        )
    }
}
