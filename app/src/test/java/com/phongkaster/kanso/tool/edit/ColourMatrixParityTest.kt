package com.phongkaster.kanso.tool.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.edit.EditColourMatrix
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * DoD 17, "the photo I see while editing agrees with the photo Done renders", checked pixel by
 * pixel on Android 15 with real (native) bitmap drawing.
 *
 * **Two paths, one formula.** While the person edits, the Edit screen does not recompute the
 * photo on the CPU: `LivePreviewImage` hands the 20 numbers of [EditColourMatrix.of] to Compose as
 * `ColorFilter.colorMatrix`, which on Android becomes an `android.graphics.ColorMatrixColorFilter`
 * set on the `Paint` that draws the photo, and the GPU (here Skia, Android's drawing library, run
 * natively by Robolectric) applies it while drawing. Done instead runs [EditProcessor], which
 * sends every pixel through the same numbers in plain Kotlin. Both read one builder, but they
 * apply it with different code, so the numbers being shared is not enough: this test draws the
 * preview the way the screen does (a real `Canvas`, a real `ColorMatrixColorFilter`, never a
 * hand-written multiplication, and never the builder compared with itself) and compares each
 * pixel with Done's output. They may differ only by rounding: at most [TOLERANCE] of 255 per
 * channel.
 *
 * **With a filter**, the preview is the filter-only picture ([FilterPreviewRenderer], the real
 * Done processor with only the filter set) drawn through the same colour filter; Done renders the
 * filter and then the sliders in one go. Those must agree too, at full and at half intensity.
 *
 * The photo is a fixed 64 x 64 gradient (red across, green down, blue along the diagonal), so
 * every slider meets dark, light, grey and coloured pixels. The sliders take the values -1, -0.5,
 * 0, 0.5 and 1 (shown as -100 to 100), each alone and in several mixes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ColourMatrixParityTest {

    /** Each of the four sliders alone at each of the five values agrees with Done. */
    @Test
    fun each_slider_alone_matches_done() {
        val photo = gradient()
        val settings = SLIDER_VALUES.flatMap { value ->
            listOf(
                EditSettings(brightness = value),
                EditSettings(contrast = value),
                EditSettings(saturation = value),
                EditSettings(warmth = value),
            )
        }
        settings.forEach { assertPreviewMatchesDone(photo, it, noFilters) }
    }

    /**
     * Mixes of the four sliders agree with Done: all four at the same value, and mixes where they
     * pull in different directions (the order the matrices are chained in matters there).
     */
    @Test
    fun mixed_sliders_match_done() {
        val photo = gradient()
        mixes().forEach { assertPreviewMatchesDone(photo, it, noFilters) }
    }

    /**
     * A filter at full intensity with the sliders on top: the filter-only preview drawn through
     * the colour filter agrees with Done's render of the whole settings.
     */
    @Test
    fun a_filter_at_full_intensity_with_sliders_matches_done() {
        val photo = gradient()
        val filters = curveFilters()
        (listOf(EditSettings()) + mixes()).forEach { sliders ->
            val settings = sliders.copy(filter = FilterPreset.KELVIN, filterIntensity = 1f)
            assertFilteredPreviewMatchesDone(photo, settings, filters)
        }
    }

    /** The same at half intensity, where the filtered and unfiltered colours are mixed. */
    @Test
    fun a_filter_at_half_intensity_with_sliders_matches_done() {
        val photo = gradient()
        val filters = curveFilters()
        (listOf(EditSettings()) + mixes()).forEach { sliders ->
            val settings = sliders.copy(filter = FilterPreset.KELVIN, filterIntensity = 0.5f)
            assertFilteredPreviewMatchesDone(photo, settings, filters)
        }
    }

    /**
     * Guards the test itself: drawing through `ColorMatrixColorFilter` really changes the pixels
     * here. Were the colour filter silently ignored by the test's drawing library, the parity
     * tests above would fail for every non-neutral setting; this one names the cause.
     */
    @Test
    fun the_canvas_really_applies_the_colour_filter() {
        val photo = gradient()
        val drawn = drawLikeThePreview(photo, EditSettings(brightness = EditSettings.MAX))
        val (difference, _) = largestDifference(photo, drawn)
        assertTrue(
            "ColorMatrixColorFilter changed no pixel by more than $TOLERANCE: the canvas ignored it",
            difference > TOLERANCE,
        )
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** A filter source with no look-ups, for the tests without a filter. */
    private val noFilters = FilterLut.Source { null }

    /**
     * Draws [photo] the way the preview does for [settings] (no filter part) and compares it with
     * Done's render of [settings] from the same photo.
     */
    private fun assertPreviewMatchesDone(photo: Bitmap, settings: EditSettings, filters: FilterLut.Source) {
        val preview = drawLikeThePreview(photo, settings)
        val done = renderLikeDone(photo, settings, filters)
        assertClose(settings, preview, done)
    }

    /**
     * Builds the preview for [settings] with a filter: the filter-only picture from
     * [FilterPreviewRenderer] (the real Done processor with only the filter set), drawn through
     * the colour filter; then compares it with Done's render of the whole [settings].
     */
    private fun assertFilteredPreviewMatchesDone(photo: Bitmap, settings: EditSettings, filters: FilterLut.Source) {
        val renderer = FilterPreviewRenderer(photo) { filterOnly -> EditProcessor(filterOnly, filters) }
        val filtered = runBlocking { renderer.render(settings) }
        assertNotNull("the filter preview failed for $settings", filtered)
        val preview = drawLikeThePreview(filtered!!, settings)
        val done = renderLikeDone(photo, settings, filters)
        assertClose(settings, preview, done)
    }

    /**
     * Draws [picture] onto a new bitmap of the same size with a real Android `Canvas`, through a
     * `Paint` whose colour filter is a `ColorMatrixColorFilter` of [EditColourMatrix.of]
     * ([settings]): what Compose's `ColorFilter.colorMatrix` turns into on Android. As the preview
     * does, no colour filter is set when the matrix is the identity.
     */
    private fun drawLikeThePreview(picture: Bitmap, settings: EditSettings): Bitmap {
        val matrix = EditColourMatrix.of(settings)
        val paint = Paint()
        if (!EditColourMatrix.isIdentity(matrix)) paint.colorFilter = ColorMatrixColorFilter(matrix)
        val output = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
        // Drawn at the same size and place, so no pixel is scaled or blended with another.
        Canvas(output).drawBitmap(picture, 0f, 0f, paint)
        return output
    }

    /** Runs Done's processor, [EditProcessor], on [photo] with the whole [settings]. */
    private fun renderLikeDone(photo: Bitmap, settings: EditSettings, filters: FilterLut.Source): Bitmap {
        val result = runBlocking { EditProcessor(settings, filters).process(photo) {} }
        assertTrue("Done failed for $settings: $result", result is EditResult.Edited)
        return (result as EditResult.Edited).bitmap
    }

    /**
     * Fails, naming [settings] and the worst pixel, when any channel of [preview] and [done]
     * differs by more than [TOLERANCE], or when their sizes differ.
     */
    private fun assertClose(settings: EditSettings, preview: Bitmap, done: Bitmap) {
        if (preview.width != done.width || preview.height != done.height) {
            fail("$settings: preview ${preview.width}x${preview.height}, Done ${done.width}x${done.height}")
        }
        val (difference, where) = largestDifference(preview, done)
        if (difference > TOLERANCE) {
            val x = where % preview.width
            val y = where / preview.width
            fail(
                "$settings: pixel ($x, $y) is ${describe(preview.getPixel(x, y))} in the preview " +
                    "but ${describe(done.getPixel(x, y))} after Done (off by $difference)",
            )
        }
    }

    /**
     * The largest difference, over every pixel and each of red, green and blue, between [first]
     * and [second] (the same size), with the index of the pixel where it is.
     */
    private fun largestDifference(first: Bitmap, second: Bitmap): Pair<Int, Int> {
        val a = pixels(first)
        val b = pixels(second)
        var largest = 0
        var where = 0
        for (i in a.indices) {
            val difference = max(
                abs(((a[i] shr 16) and 0xFF) - ((b[i] shr 16) and 0xFF)),
                max(abs(((a[i] shr 8) and 0xFF) - ((b[i] shr 8) and 0xFF)), abs((a[i] and 0xFF) - (b[i] and 0xFF))),
            )
            if (difference > largest) {
                largest = difference
                where = i
            }
        }
        return largest to where
    }

    /** [color] as "(r, g, b)" for a failure message. */
    private fun describe(color: Int): String =
        "(${(color shr 16) and 0xFF}, ${(color shr 8) and 0xFF}, ${color and 0xFF})"

    /** Several mixes of the four sliders: each value on all four, and opposing pulls. */
    private fun mixes(): List<EditSettings> =
        SLIDER_VALUES.map { EditSettings(brightness = it, contrast = it, saturation = it, warmth = it) } +
            listOf(
                EditSettings(brightness = 1f, contrast = -1f, saturation = 0.5f, warmth = -0.5f),
                EditSettings(brightness = -0.5f, contrast = 1f, saturation = -1f, warmth = 0.5f),
                EditSettings(brightness = 0.5f, contrast = 0.5f, saturation = -0.5f, warmth = 1f),
                EditSettings(brightness = -1f, contrast = -0.5f, saturation = 1f, warmth = -1f),
                EditSettings(brightness = 0f, contrast = 1f, saturation = 1f, warmth = 0f),
            )

    /**
     * A filter source whose every preset is one generated look-up: a 256 x 6 strip read as three
     * tone curves (rows 0-1 red, 2-3 green, 4-5 blue, so the rows the look-up samples at 1/6, 1/2
     * and 5/6 of the height fall well inside each pair). Red is lifted (a square-root curve), green
     * inverted and blue squeezed into 64..191, so the filter changes every channel differently.
     */
    private fun curveFilters(): FilterLut.Source {
        val width = 256
        val height = 6
        val data = IntArray(width * height) { index ->
            val v = index % width
            val value = when (index / width) {
                0, 1 -> (255.0 * sqrt(v / 255.0)).toInt()
                2, 3 -> 255 - v
                else -> 64 + v / 2
            }
            rgb(value, value, value)
        }
        val lut = FilterLut(bitmapOf(data, width, height))
        return FilterLut.Source { lut }
    }

    /**
     * The fixed 64 x 64 photo: red rises across, green down, blue along the anti-diagonal, so it
     * holds near-black, near-white, greys and strong colours.
     */
    private fun gradient(): Bitmap {
        val size = 64
        val data = IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            rgb(x * 4, y * 4, 252 - (x + y) * 2)
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

    /** Values shared by the tests. */
    private companion object {
        /** The slider values tried: -100, -50, 0, 50 and 100 on the screen. */
        val SLIDER_VALUES = listOf(-1f, -0.5f, 0f, 0.5f, 1f)

        /** How far, of 255, the preview and Done may differ per channel (rounding only). */
        const val TOLERANCE = 2
    }
}
