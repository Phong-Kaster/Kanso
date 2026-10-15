package com.phongkaster.kanso.tool.retouch

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.retouch.RetouchProcessor
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 10, 17 and 22 for Retouch: [RetouchProcessor] with a fake face finder ([FakeFaceFinder],
 * which "finds" the centre of the photo) and a fake smoother ([FakeSkinSmoother], a box blur) in
 * place of the engine, which cannot load on the JVM. The photo is a noisy 200 x 200 grey patch,
 * so smoothing shows up as a drop in the pixels' variance. Pixels are read straight from the
 * bitmaps.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RetouchProcessorTest {

    /** Strength 0 gives back a new bitmap equal to the input, pixel for pixel. */
    @Test
    fun strength_zero_leaves_every_pixel_as_it_was() {
        val photo = noisyPatch()
        val result = retouch(photo, strength = 0f)

        assertNotSame("the result must be a new bitmap", photo, result)
        assertArrayEquals(pixels(photo), pixels(result))
    }

    /**
     * Strength 1 brings the variance inside the face below 60% of the input's, and leaves every
     * pixel outside the face exactly as it was.
     */
    @Test
    fun strength_one_smooths_the_face_and_nothing_else() {
        val photo = noisyPatch()
        val face = FakeFaceFinder.centre(photo)
        val result = retouch(photo, strength = 1f)

        val before = variance(photo, face)
        val after = variance(result, face)
        assertTrue("variance inside the face went from $before to $after", after < 0.6 * before)
        val original = pixels(photo)
        val edited = pixels(result)
        for (y in 0 until photo.height) {
            for (x in 0 until photo.width) {
                if (face.contains(x, y)) continue
                assertEquals("pixel ($x, $y) outside the face changed", original[y * photo.width + x], edited[y * photo.width + x])
            }
        }
    }

    /** Strength 0.5 lands the variance inside the face between strength 1's and the input's. */
    @Test
    fun half_strength_smooths_half_way() {
        val photo = noisyPatch()
        val face = FakeFaceFinder.centre(photo)
        val none = variance(photo, face)
        val full = variance(retouch(photo, strength = 1f), face)
        val half = variance(retouch(photo, strength = 0.5f), face)

        assertTrue("half $half is not between full $full and none $none", half > full && half < none)
    }

    /** A photo with no face fails with "no face" and its pixels are untouched. */
    @Test
    fun no_face_is_reported_and_nothing_changes() {
        val photo = noisyPatch()
        val before = pixels(photo)
        val smoother = FakeSkinSmoother()

        val result = runBlocking { RetouchProcessor(FakeFaceFinder.none(), smoother).process(photo) {} }

        assertEquals(EditResult.Failed(EditFailure.NoFace), result)
        assertTrue("the smoother must not run without a face", smoother.received.isEmpty())
        assertArrayEquals(before, pixels(photo))
    }

    /** The input bitmap is never changed by a full-strength run, and the result is opaque. */
    @Test
    fun the_input_is_not_changed_and_the_result_is_opaque() {
        val photo = noisyPatch()
        val before = pixels(photo)
        val result = retouch(photo, strength = 1f)

        assertArrayEquals(before, pixels(photo))
        assertFalse("a retouched photo is opaque, so Save writes a JPEG", result.hasAlpha())
    }

    /**
     * DoD 22, the cap test: from a 6000 x 4000 photo, the finder and the smoother both receive a
     * picture whose long edge is at most 2000 pixels, with the photo's shape.
     */
    @Test
    fun a_very_large_photo_is_capped_before_the_finder_and_the_smoother() {
        val finder = FakeFaceFinder()
        val smoother = FakeSkinSmoother()
        // RGB_565 (2 bytes a pixel) keeps the 24-megapixel input at 48 MB on the test heap.
        val huge = Bitmap.createBitmap(6000, 4000, Bitmap.Config.RGB_565)
        huge.eraseColor(Color.rgb(128, 128, 128))

        val result = runBlocking { RetouchProcessor(finder, smoother).process(huge) {} }
        huge.recycle()

        for ((name, sizes) in listOf("finder" to finder.received, "smoother" to smoother.received)) {
            assertEquals("the $name ran ${sizes.size} times", 1, sizes.size)
            val (width, height) = sizes.single()
            assertTrue("the $name got $width x $height, over the cap", max(width, height) <= WorkingSize.RETOUCH)
            assertEquals(6000f / 4000f, width.toFloat() / height, 0.01f)
        }
        val bitmap = (result as EditResult.Edited).bitmap
        assertEquals(finder.received.single(), bitmap.width to bitmap.height)
    }

    /** DoD 17/22: an `OutOfMemoryError` from the finder becomes the out-of-memory failure. */
    @Test
    fun out_of_memory_in_the_finder_is_reported() {
        val result = runBlocking {
            RetouchProcessor(FakeFaceFinder(failure = OutOfMemoryError("fake")), FakeSkinSmoother())
                .process(noisyPatch()) {}
        }
        assertEquals(EditResult.Failed(EditFailure.OutOfMemory), result)
    }

    /** DoD 17/22: an `OutOfMemoryError` from the smoother becomes the out-of-memory failure. */
    @Test
    fun out_of_memory_in_the_smoother_is_reported() {
        val result = runBlocking {
            RetouchProcessor(FakeFaceFinder(), FakeSkinSmoother(failure = OutOfMemoryError("fake")))
                .process(noisyPatch()) {}
        }
        assertEquals(EditResult.Failed(EditFailure.OutOfMemory), result)
    }

    /** DoD 17: any other error from the finder becomes "the model would not load". */
    @Test
    fun a_finder_error_is_reported_as_model_unavailable() {
        val result = runBlocking {
            RetouchProcessor(FakeFaceFinder(failure = IllegalStateException("fake")), FakeSkinSmoother())
                .process(noisyPatch()) {}
        }
        assertEquals(EditResult.Failed(EditFailure.ModelUnavailable), result)
    }

    /** DoD 17: any other error from the smoother becomes "the model would not load". */
    @Test
    fun a_smoother_error_is_reported_as_model_unavailable() {
        val result = runBlocking {
            RetouchProcessor(FakeFaceFinder(), FakeSkinSmoother(failure = IllegalStateException("fake")))
                .process(noisyPatch()) {}
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
                returned = RetouchProcessor(FakeFaceFinder(failure = CancellationException("left")), FakeSkinSmoother())
                    .process(noisyPatch()) {}
            } catch (cancelled: CancellationException) {
                caught = cancelled
            }
        }
        assertEquals(null, returned)
        assertTrue("expected a CancellationException, got $caught", caught is CancellationException)
    }

    /** Progress only moves forward, stays in 0..1 and ends at exactly 1. */
    @Test
    fun progress_goes_monotonically_to_one() {
        val seen = mutableListOf<Float>()
        val twoFaces = FakeFaceFinder(facesFor = { listOf(Rect(0, 0, 60, 60), Rect(100, 100, 180, 180)) })

        runBlocking { RetouchProcessor(twoFaces, FakeSkinSmoother()).process(noisyPatch()) { seen += it } }

        assertTrue("no progress was reported", seen.isNotEmpty())
        seen.forEach { assertTrue("progress $it is outside 0..1", it in 0f..1f) }
        seen.zipWithNext().forEach { (a, b) -> assertTrue("progress went back from $a to $b", b >= a) }
        assertEquals(1f, seen.last(), 0f)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Runs Retouch at [strength] on [photo] with the centre-face finder and returns the result. */
    private fun retouch(photo: Bitmap, strength: Float): Bitmap {
        val processor = RetouchProcessor(FakeFaceFinder(), FakeSkinSmoother())
        processor.strength = strength
        val result = runBlocking { processor.process(photo) {} }
        return (result as EditResult.Edited).bitmap
    }

    /**
     * A 200 x 200 opaque grey patch with noise: each pixel's grey is 128 plus a random step from
     * -40 to 40, from a fixed seed so every run sees the same patch.
     */
    private fun noisyPatch(): Bitmap {
        val random = Random(SEED)
        val data = IntArray(SIZE * SIZE) {
            val grey = 128 + random.nextInt(-40, 41)
            Color.rgb(grey, grey, grey)
        }
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(data, 0, SIZE, 0, 0, SIZE, SIZE)
        return bitmap
    }

    /** Every pixel of [bitmap], row after row. */
    private fun pixels(bitmap: Bitmap): IntArray {
        val data = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return data
    }

    /** The variance of the red channel (the patch is grey, so all channels match) inside [area]. */
    private fun variance(bitmap: Bitmap, area: Rect): Double {
        val values = mutableListOf<Double>()
        for (y in area.top until area.bottom) {
            for (x in area.left until area.right) {
                values += Color.red(bitmap.getPixel(x, y)).toDouble()
            }
        }
        val mean = values.average()
        return values.sumOf { (it - mean) * (it - mean) } / values.size
    }

    /** The noisy patch's fixed numbers. */
    private companion object {
        /** The patch's width and height, in pixels. */
        const val SIZE = 200

        /** The random seed of the patch's noise. */
        const val SEED = 8
    }
}
