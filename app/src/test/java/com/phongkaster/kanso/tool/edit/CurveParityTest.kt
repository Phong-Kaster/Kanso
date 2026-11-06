package com.phongkaster.kanso.tool.edit

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.edit.CurveAdjustments
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
import com.phongkaster.kanso.data.edit.edit.HslAdjustments
import com.phongkaster.kanso.data.edit.edit.HslChannel
import com.phongkaster.kanso.data.edit.edit.ToneCurve
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
 * DoD 7 and 8, "what is shown matches the saved photo" and "curves are applied after the Filter
 * and HSL, and before Adjust and RGB", checked pixel by pixel on Android 15.
 *
 * The Edit screen's picture is [FilterPreviewRenderer]'s copy (filter, HSL, curves on the CPU) with
 * the colour matrix drawn on top by Compose; Done runs [EditProcessor] with the whole settings. The
 * test stands in for Compose by running a colour-only [EditProcessor] over the preview copy, then
 * compares with Done within [TOLERANCE] of 255 per channel (the same as `HslParityTest`).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CurveParityTest {

    /** A curve alone, with no filter and no HSL, gives the same picture in the preview and Done. */
    @Test
    fun a_curve_alone_matches_done() {
        assertParity(EditSettings(curves = master(bend(0.5f, 0.2f))))
        assertParity(EditSettings(curves = master(bend(0.3f, 0.6f).added(CurvePoint(0.7f, 0.4f)))))
    }

    /** Per-channel curves (which the render already accepts) match Done too. */
    @Test
    fun channel_curves_match_done() {
        val curves = CurveAdjustments.NEUTRAL
            .with(CurveChannel.RED, bend(0.5f, 0.8f))
            .with(CurveChannel.BLUE, bend(0.4f, 0.1f))
            .with(CurveChannel.RGB, bend(0.6f, 0.5f))
        assertParity(EditSettings(curves = curves))
    }

    /** A Lightness curve (alone and with the colour curves) gives the same picture in the preview and Done. */
    @Test
    fun a_lightness_curve_matches_done() {
        assertParity(EditSettings(curves = CurveAdjustments.NEUTRAL.with(CurveChannel.LIGHTNESS, bend(0.5f, 0.2f))))
        val curves = CurveAdjustments.NEUTRAL
            .with(CurveChannel.RGB, bend(0.6f, 0.5f))
            .with(CurveChannel.LIGHTNESS, bend(0.3f, 0.6f).added(CurvePoint(0.7f, 0.4f)))
        assertParity(EditSettings(curves = curves, brightness = 0.2f))
    }

    /**
     * Filter, HSL, curves and the colour sliders together (a combination whose steps do not
     * commute) give the same picture in the preview and in Done.
     */
    @Test
    fun filter_hsl_curves_and_colour_match_done() {
        assertParity(fullSettings())
    }

    /**
     * Guards the order: curves come after filter and HSL and before the colour matrix. Applying the
     * colour sliders first and the curves second gives a clearly different picture, so a swapped
     * order in either path would fail the parity test above.
     */
    @Test
    fun the_order_is_filter_hsl_curves_then_colour() {
        val settings = fullSettings()
        val photo = photo()
        val done = pixels(renderLikeDone(photo, settings))
        val colourOnly = EditSettings(
            brightness = settings.brightness,
            saturation = settings.saturation,
            warmth = settings.warmth,
        )
        val cpuOnly = settings.copy(brightness = 0f, saturation = 0f, warmth = 0f)
        // The wrong order: the colour sliders first, then filter, HSL and curves.
        val swapped = pixels(renderLikeDone(renderLikeDone(photo, colourOnly), cpuOnly))
        assertTrue("the order did not matter, so the test proves nothing", largestDifference(done, swapped) > TOLERANCE)
    }

    /** Guards the test: the curve really changes the picture, so agreement is not trivial. */
    @Test
    fun the_curve_really_changes_the_photo() {
        val photo = photo()
        val done = pixels(renderLikeDone(photo, EditSettings(curves = master(bend(0.5f, 0.2f)))))
        assertTrue(largestDifference(pixels(photo), done) > 10)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Filter, HSL, a master curve and brightness, saturation and warmth, all away from neutral. */
    private fun fullSettings() = EditSettings(
        filter = FilterPreset.KELVIN,
        filterIntensity = 0.8f,
        hsl = HslAdjustments.NEUTRAL
            .with(ColourRange.BLUE, HslChannel.HUE, 0.8f)
            .with(ColourRange.RED, HslChannel.SATURATION, -0.7f),
        curves = master(bend(0.5f, 0.15f)),
        brightness = 0.3f,
        saturation = -0.4f,
        warmth = 0.5f,
    )

    /** A master curve bent through one middle point. */
    private fun bend(x: Float, y: Float): ToneCurve = ToneCurve.NEUTRAL.added(CurvePoint(x, y))

    /** Curves with only the master curve set to [curve]. */
    private fun master(curve: ToneCurve): CurveAdjustments =
        CurveAdjustments.NEUTRAL.with(CurveChannel.RGB, curve)

    /**
     * Fails when the preview of [settings] (with the colour matrix put on top) and Done's render
     * differ by more than [TOLERANCE].
     */
    private fun assertParity(settings: EditSettings) {
        val photo = photo()
        val renderer = FilterPreviewRenderer(photo) { filterOnly -> EditProcessor(filterOnly, filters) }
        val copy = runBlocking { renderer.render(settings) }
        assertNotNull("no preview for $settings", copy)
        // Compose draws the colour sliders over the copy; a colour-only Done run stands in for it.
        val colourOnly = EditSettings(
            brightness = settings.brightness,
            contrast = settings.contrast,
            saturation = settings.saturation,
            warmth = settings.warmth,
        )
        val shown = renderLikeDone(copy!!, colourOnly)
        val done = renderLikeDone(photo, settings)
        assertEquals(done.width, shown.width)
        assertEquals(done.height, shown.height)
        val largest = largestDifference(pixels(shown), pixels(done))
        assertTrue("$settings: preview and Done differ by $largest of 255", largest <= TOLERANCE)
    }

    /** The largest per-channel difference, of 255, between two equally long pixel arrays. */
    private fun largestDifference(a: IntArray, b: IntArray): Int {
        var largest = 0
        for (i in a.indices) {
            for (shift in intArrayOf(16, 8, 0)) {
                largest = maxOf(largest, abs(((a[i] shr shift) and 0xFF) - ((b[i] shr shift) and 0xFF)))
            }
        }
        return largest
    }

    /** Runs Done's processor on [photo] with [settings]. */
    private fun renderLikeDone(photo: Bitmap, settings: EditSettings): Bitmap {
        val result = runBlocking { EditProcessor(settings, filters).process(photo) {} }
        assertTrue("Done failed for $settings: $result", result is EditResult.Edited)
        return (result as EditResult.Edited).bitmap
    }

    /** One generated look-up for every preset, as in `HslParityTest`. */
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
