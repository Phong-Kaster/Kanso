package com.phongkaster.kanso.tool.enhance

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.enhance.AutoLighting
import com.phongkaster.kanso.data.edit.enhance.EnhanceProcessor
import com.phongkaster.kanso.data.edit.enhance.Upscaler
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 8, 17 and 22 for Enhance: [EnhanceProcessor] with a fake upscaler in place of the engine
 * (Real-ESRGAN cannot load on the JVM). The fake doubles the photo's size and records what it was
 * given, so the tests can check the size cap, the size of the result, the lighting step, the
 * failures and the progress. Pixels are read straight from the bitmaps.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class EnhanceProcessorTest {

    /**
     * A stand-in for the engine: returns [photo] scaled to twice its width and height (no
     * smoothing, so a flat colour stays exactly that colour), reports [progressSteps] on the way,
     * and remembers the size of every photo it received.
     */
    private class DoublingUpscaler(
        private val progressSteps: List<Float> = listOf(0f, 0.5f, 1f),
    ) : Upscaler {

        /** Width and height of each photo handed to [upscale], in order. */
        val received = mutableListOf<Pair<Int, Int>>()

        /** Records [photo]'s size, reports the steps, then doubles it. */
        override suspend fun upscale(photo: Bitmap, onProgress: (Float) -> Unit): Bitmap {
            received += photo.width to photo.height
            progressSteps.forEach(onProgress)
            return Bitmap.createScaledBitmap(photo, photo.width * 2, photo.height * 2, false)
        }
    }

    /** A stand-in for an engine that fails: it throws [failure] as soon as it is called. */
    private class ThrowingUpscaler(private val failure: Throwable) : Upscaler {

        /** Throws [failure]. */
        override suspend fun upscale(photo: Bitmap, onProgress: (Float) -> Unit): Bitmap = throw failure
    }

    /** A photo under the cap comes back exactly twice as wide and twice as tall. */
    @Test
    fun output_is_twice_the_input_on_each_side() {
        val upscaler = DoublingUpscaler()
        val result = runBlocking { EnhanceProcessor(upscaler).process(flat(300, 200, 128)) {} }

        val bitmap = (result as EditResult.Edited).bitmap
        assertEquals(listOf(300 to 200), upscaler.received)
        assertEquals(600, bitmap.width)
        assertEquals(400, bitmap.height)
        assertFalse("an enhanced photo is opaque, so Save writes a JPEG", bitmap.hasAlpha())
    }

    /**
     * DoD 22, the cap test: a 6000 x 4000 photo reaches the upscaler with its long edge at most
     * 512 pixels, shape kept, and the result is exactly twice what the upscaler received.
     */
    @Test
    fun a_very_large_photo_is_capped_before_the_upscaler() {
        val upscaler = DoublingUpscaler()
        // RGB_565 (2 bytes a pixel) keeps the 24-megapixel input at 48 MB on the test heap.
        val huge = Bitmap.createBitmap(6000, 4000, Bitmap.Config.RGB_565)
        huge.eraseColor(Color.rgb(128, 128, 128))

        val result = runBlocking { EnhanceProcessor(upscaler).process(huge) {} }
        huge.recycle()

        assertEquals(1, upscaler.received.size)
        val (width, height) = upscaler.received.single()
        assertTrue("long edge $width x $height is over the cap", max(width, height) <= WorkingSize.ENHANCE)
        assertEquals(WorkingSize.ENHANCE, width)
        assertEquals(6000f / 4000f, width.toFloat() / height, 0.01f)
        val bitmap = (result as EditResult.Edited).bitmap
        assertEquals(width * 2, bitmap.width)
        assertEquals(height * 2, bitmap.height)
    }

    /** A dark photo (mean luminance 40) comes out of Enhance with its mean in 100..160. */
    @Test
    fun a_dark_photo_is_brightened_toward_the_mid_tones() {
        val dark = darkTexture(64, 48)
        assertEquals(40.0, meanLuminance(dark), 1.5)

        val result = runBlocking { EnhanceProcessor(DoublingUpscaler()).process(dark) {} }

        val mean = meanLuminance((result as EditResult.Edited).bitmap)
        assertTrue("mean luminance $mean is not in 100..160", mean in 100.0..160.0)
    }

    /** The lighting step alone lifts a flat patch of luminance 40 into 100..160 too. */
    @Test
    fun the_lighting_step_lifts_a_flat_dark_patch() {
        val mean = meanLuminance(AutoLighting.apply(flat(40, 40, 40)))
        assertTrue("mean luminance $mean is not in 100..160", mean in 100.0..160.0)
    }

    /** A well-lit photo (mean luminance 128) keeps its mean within 10. */
    @Test
    fun a_well_lit_photo_is_left_alone() {
        val lit = litTexture(64, 48)
        val before = meanLuminance(lit)
        assertEquals(128.0, before, 1.5)

        val result = runBlocking { EnhanceProcessor(DoublingUpscaler()).process(lit) {} }

        val after = meanLuminance((result as EditResult.Edited).bitmap)
        assertTrue("mean luminance moved from $before to $after", abs(after - before) <= 10.0)
    }

    /** A flat photo, whatever its grey, is never made darker by more than 2. */
    @Test
    fun a_flat_photo_is_never_darkened() {
        listOf(0, 10, 40, 90, 109, 110, 128, 200, 255).forEach { grey ->
            val result = runBlocking { EnhanceProcessor(DoublingUpscaler()).process(flat(20, 20, grey)) {} }
            val after = meanLuminance((result as EditResult.Edited).bitmap)
            assertTrue("grey $grey came out at $after", after >= grey - 2.0)
        }
    }

    /** DoD 17/22: an `OutOfMemoryError` from the engine step becomes the out-of-memory failure. */
    @Test
    fun out_of_memory_in_the_upscaler_is_reported() {
        val result = runBlocking {
            EnhanceProcessor(ThrowingUpscaler(OutOfMemoryError("fake"))).process(flat(20, 20, 40)) {}
        }
        assertEquals(EditResult.Failed(EditFailure.OutOfMemory), result)
    }

    /** DoD 17: any other engine error becomes "the model would not load". */
    @Test
    fun an_engine_error_is_reported_as_model_unavailable() {
        val result = runBlocking {
            EnhanceProcessor(ThrowingUpscaler(IllegalStateException("fake"))).process(flat(20, 20, 40)) {}
        }
        assertEquals(EditResult.Failed(EditFailure.ModelUnavailable), result)
    }

    /** A cancellation is passed on, never turned into a failure. */
    @Test
    fun a_cancellation_propagates() {
        var caught: Throwable? = null
        var returned: EditResult? = null
        runBlocking {
            try {
                returned = EnhanceProcessor(ThrowingUpscaler(CancellationException("left")))
                    .process(flat(20, 20, 40)) {}
            } catch (cancelled: CancellationException) {
                caught = cancelled
            }
        }
        assertEquals(null, returned)
        assertTrue("expected a CancellationException, got $caught", caught is CancellationException)
    }

    /** Progress only moves forward, stays in 0..1 and ends at exactly 1, even if the engine jumps back. */
    @Test
    fun progress_goes_monotonically_to_one() {
        val upscaler = DoublingUpscaler(progressSteps = listOf(0f, 0.4f, 0.2f, 0.8f, 1f))
        val seen = mutableListOf<Float>()

        runBlocking { EnhanceProcessor(upscaler).process(flat(20, 20, 40)) { seen += it } }

        assertTrue("no progress was reported", seen.isNotEmpty())
        seen.forEach { assertTrue("progress $it is outside 0..1", it in 0f..1f) }
        seen.zipWithNext().forEach { (a, b) -> assertTrue("progress went back from $a to $b", b >= a) }
        assertEquals(1f, seen.last(), 0f)
    }

    /** Makes a [width] x [height] opaque photo of one grey, `rgb(grey, grey, grey)`. */
    private fun flat(width: Int, height: Int, grey: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(grey, grey, grey))
        return bitmap
    }

    /** Makes a dark grey texture whose values run 30..50 (mean about 40). */
    private fun darkTexture(width: Int, height: Int): Bitmap = texture(width, height, base = 30)

    /** Makes a mid grey texture whose values run 118..138 (mean about 128). */
    private fun litTexture(width: Int, height: Int): Bitmap = texture(width, height, base = 118)

    /** Makes a grey texture whose values run from [base] to [base] + 20, evenly spread. */
    private fun texture(width: Int, height: Int, base: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height) { index ->
            val grey = base + (index % 21)
            Color.rgb(grey, grey, grey)
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /** The mean luminance (0.299 R + 0.587 G + 0.114 B) of [bitmap], read pixel by pixel. */
    private fun meanLuminance(bitmap: Bitmap): Double {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.sumOf { pixel ->
            0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
        } / pixels.size
    }
}
