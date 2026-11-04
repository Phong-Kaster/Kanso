package com.phongkaster.kanso.tool.edit

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
import com.phongkaster.kanso.data.edit.edit.HslAdjustments
import com.phongkaster.kanso.data.edit.edit.HslChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * DoD 16, "the live preview and the saved photo use one HSL formula and agree", checked pixel by
 * pixel on Android 15.
 *
 * The Edit screen's preview copy comes from [FilterPreviewRenderer] (the real [EditProcessor] with
 * only the filter and the HSL values set), and Done runs [EditProcessor] with the whole settings.
 * Both call the one HSL formula in `EditHsl`, so with the same HSL values every channel of the two
 * pictures may differ by at most [TOLERANCE] of 255. The source is 64 x 48, well under 640 px, so
 * the preview copy is not shrunk and the two have the same size.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class HslParityTest {

    /** HSL alone, with no filter, gives the same picture in the preview and in Done. */
    @Test
    fun hsl_without_a_filter_matches_done() {
        hslSets().forEach { assertParity(EditSettings(hsl = it)) }
    }

    /** HSL on top of a filter (at full and half intensity) gives the same picture in both. */
    @Test
    fun hsl_with_a_filter_matches_done() {
        hslSets().forEach {
            assertParity(EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 1f, hsl = it))
            assertParity(EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 0.5f, hsl = it))
        }
    }

    /** Guards the test: the HSL values really change the picture, so agreement is not trivial. */
    @Test
    fun the_hsl_values_really_change_the_photo() {
        val photo = photo()
        val hsl = HslAdjustments.NEUTRAL.with(ColourRange.BLUE, HslChannel.HUE, 1f)
            .with(ColourRange.RED, HslChannel.SATURATION, -1f)
        val done = pixels(renderLikeDone(photo, EditSettings(hsl = hsl)))
        val original = pixels(photo)
        assertTrue(original.indices.any { (original[it] and 0xFFFFFF) != (done[it] and 0xFFFFFF) })
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Several HSL sets: one value on one range, one value on every range, and a mixed one. */
    private fun hslSets(): List<HslAdjustments> {
        val sets = mutableListOf<HslAdjustments>()
        for (channel in HslChannel.entries) {
            for (value in listOf(-1f, 0.5f, 1f)) {
                sets += HslAdjustments.NEUTRAL.with(ColourRange.BLUE, channel, value)
                var all = HslAdjustments.NEUTRAL
                ColourRange.entries.forEach { all = all.with(it, channel, value) }
                sets += all
            }
        }
        sets += HslAdjustments.NEUTRAL
            .with(ColourRange.RED, HslChannel.HUE, 0.7f)
            .with(ColourRange.GREEN, HslChannel.SATURATION, -0.6f)
            .with(ColourRange.BLUE, HslChannel.LIGHTNESS, 0.4f)
        return sets
    }

    /** Fails when the preview of [settings] and Done's render differ by more than [TOLERANCE]. */
    private fun assertParity(settings: EditSettings) {
        val photo = photo()
        val renderer = FilterPreviewRenderer(photo) { filterOnly -> EditProcessor(filterOnly, filters) }
        val preview = runBlocking { renderer.render(settings) }
        assertNotNull("no preview for $settings", preview)
        val done = renderLikeDone(photo, settings)
        assertEquals(done.width, preview!!.width)
        assertEquals(done.height, preview.height)
        val a = pixels(preview)
        val b = pixels(done)
        var largest = 0
        for (i in a.indices) {
            for (shift in intArrayOf(16, 8, 0)) {
                largest = maxOf(largest, abs(((a[i] shr shift) and 0xFF) - ((b[i] shr shift) and 0xFF)))
            }
        }
        assertTrue("$settings: preview and Done differ by $largest of 255", largest <= TOLERANCE)
    }

    /** Runs Done's processor on [photo] with [settings]. */
    private fun renderLikeDone(photo: Bitmap, settings: EditSettings): Bitmap {
        val result = runBlocking { EditProcessor(settings, filters).process(photo) {} }
        assertTrue("Done failed for $settings: $result", result is EditResult.Edited)
        return (result as EditResult.Edited).bitmap
    }

    /**
     * One generated look-up for every preset: a 256 x 6 strip read as three tone curves, red lifted
     * (square-root-like), green inverted, blue squeezed into 64..191.
     */
    private val filters: FilterLut.Source = run {
        val width = 256
        val data = IntArray(width * 6) { index ->
            val v = index % width
            val value = when (index / width) {
                0, 1 -> minOf(255, v + (255 - v) / 3)
                2, 3 -> 255 - v
                else -> 64 + v / 2
            }
            (0xFF shl 24) or (value shl 16) or (value shl 8) or value
        }
        val bitmap = Bitmap.createBitmap(width, 6, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(data, 0, width, 0, 0, width, 6)
        val lut = FilterLut(bitmap)
        FilterLut.Source { lut }
    }

    /** A 64 x 48 photo whose hue sweeps the wheel across, with saturation and value varying down. */
    private fun photo(): Bitmap {
        val width = 64
        val height = 48
        val data = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            Color.HSVToColor(floatArrayOf(x * 360f / width, 0.3f + 0.7f * y / (height - 1), 0.3f + 0.7f * (x % 8) / 7f))
        }
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
        /** How far, of 255, the preview and Done may differ per channel. */
        const val TOLERANCE = 1
    }
}
