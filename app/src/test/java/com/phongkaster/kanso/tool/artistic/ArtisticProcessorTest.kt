package com.phongkaster.kanso.tool.artistic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.artistic.ArtisticProcessor
import com.phongkaster.kanso.data.edit.artistic.ArtisticStyle
import com.phongkaster.kanso.data.edit.artistic.StyleImages
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 12 and 22 for Artistic: [ArtisticProcessor] with a fake stylizer in place of the engine
 * (the style-transfer models cannot load on the JVM). The fake recolours each pixel in a fixed way
 * per style ([RecolouringStylizer.recolour]), so every expected pixel can be computed exactly.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ArtisticProcessorTest {

    /** Strength 0 gives back the input, pixel for pixel, as a new bitmap. */
    @Test
    fun strength_zero_gives_the_original() {
        val input = texture(64, 48)
        val result = run(ArtisticProcessor(RecolouringStylizer()), input, ArtisticStyle.WARM_WASH, 0f)

        assertTrue("strength 0 must still be a new bitmap", result !== input)
        assertArrayEquals(pixels(input), pixels(result))
        assertFalse("an artistic photo is opaque, so Save writes a JPEG", result.hasAlpha())
    }

    /** Full strength gives exactly the stylizer's painted picture. */
    @Test
    fun full_strength_gives_the_painted_picture() {
        val input = texture(64, 48)
        val result = run(ArtisticProcessor(RecolouringStylizer()), input, ArtisticStyle.COOL_WASH, 1f)

        val expected = pixels(input).map { RecolouringStylizer.recolour(it, ArtisticStyle.COOL_WASH) }.toIntArray()
        assertArrayEquals(expected, pixels(result))
    }

    /** Half strength is within 1 per channel of the average of the original and the painted picture. */
    @Test
    fun half_strength_is_a_mix() {
        val input = texture(64, 48)
        val result = run(ArtisticProcessor(RecolouringStylizer()), input, ArtisticStyle.MOSAIC, 0.5f)

        val original = pixels(input)
        val mixed = pixels(result)
        original.indices.forEach { index ->
            val a = original[index]
            val b = RecolouringStylizer.recolour(a, ArtisticStyle.MOSAIC)
            val c = mixed[index]
            listOf<(Int) -> Int>({ Color.red(it) }, { Color.green(it) }, { Color.blue(it) }).forEach { channel ->
                val average = (channel(a) + channel(b)) / 2.0
                assertTrue(
                    "pixel $index: ${channel(c)} is not within 1 of $average",
                    abs(channel(c) - average) <= 1.0,
                )
            }
        }
    }

    /** Choosing another style gives another picture. */
    @Test
    fun two_styles_give_different_pictures() {
        val input = texture(64, 48)
        val processor = ArtisticProcessor(RecolouringStylizer())
        val warm = run(processor, input, ArtisticStyle.WARM_WASH, 1f)
        val ink = run(processor, input, ArtisticStyle.INK, 1f)

        assertFalse("two styles gave the same picture", pixels(warm).contentEquals(pixels(ink)))
    }

    /** Moving the strength only (same input bitmap, same style) never runs the stylizer again. */
    @Test
    fun a_new_strength_reuses_the_painted_picture() {
        val input = texture(64, 48)
        val stylizer = RecolouringStylizer()
        val processor = ArtisticProcessor(stylizer)

        run(processor, input, ArtisticStyle.INK, 0.3f)
        val second = run(processor, input, ArtisticStyle.INK, 1f)
        assertEquals(1, stylizer.calls)
        val expected = pixels(input).map { RecolouringStylizer.recolour(it, ArtisticStyle.INK) }.toIntArray()
        assertArrayEquals(expected, pixels(second))

        run(processor, input, ArtisticStyle.MOSAIC, 1f)
        assertEquals("a new style must be painted", 2, stylizer.calls)
    }

    /** DoD 22, the cap test: a 6000 x 4000 photo reaches the stylizer with its long edge at most 1024. */
    @Test
    fun a_very_large_photo_is_capped_before_the_stylizer() {
        val stylizer = RecolouringStylizer()
        // RGB_565 (2 bytes a pixel) keeps the 24-megapixel input at 48 MB on the test heap.
        val huge = Bitmap.createBitmap(6000, 4000, Bitmap.Config.RGB_565)
        huge.eraseColor(Color.rgb(128, 128, 128))

        val result = run(ArtisticProcessor(stylizer), huge, ArtisticStyle.WARM_WASH, 0.5f)
        huge.recycle()

        assertEquals(1, stylizer.received.size)
        val (width, height) = stylizer.received.single()
        assertTrue("long edge $width x $height is over the cap", max(width, height) <= WorkingSize.ARTISTIC)
        assertEquals(6000f / 4000f, width.toFloat() / height, 0.01f)
        assertEquals(width, result.width)
        assertEquals(height, result.height)
    }

    /** An `OutOfMemoryError` from the stylizer becomes the out-of-memory failure. */
    @Test
    fun out_of_memory_in_the_stylizer_is_reported() {
        val result = runBlocking {
            ArtisticProcessor(ThrowingStylizer(OutOfMemoryError("fake"))).process(texture(20, 20)) {}
        }
        assertEquals(EditResult.Failed(EditFailure.OutOfMemory), result)
    }

    /** Any other engine error becomes "the model would not load". */
    @Test
    fun an_engine_error_is_reported_as_model_unavailable() {
        val result = runBlocking {
            ArtisticProcessor(ThrowingStylizer(IllegalStateException("fake"))).process(texture(20, 20)) {}
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
                returned = ArtisticProcessor(ThrowingStylizer(CancellationException("left")))
                    .process(texture(20, 20)) {}
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
        runBlocking { ArtisticProcessor(RecolouringStylizer()).process(texture(20, 20)) { seen += it } }

        assertTrue("no progress was reported", seen.isNotEmpty())
        seen.forEach { assertTrue("progress $it is outside 0..1", it in 0f..1f) }
        seen.zipWithNext().forEach { (a, b) -> assertTrue("progress went back from $a to $b", b >= a) }
        assertEquals(1f, seen.last(), 0f)
    }

    /**
     * DoD 12, the style list: at least four styles, each with a distinct id, a name and an image:
     * its asset path is `styles/<id>.webp`, and its drawn picture is 256 x 256 and not one flat
     * colour. (That the asset file exists is checked by the Iteration.)
     */
    @Test
    fun there_are_at_least_four_styles_each_with_a_name_and_an_image() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val styles = ArtisticStyle.entries
        assertTrue("only ${styles.size} styles", styles.size >= 4)
        assertEquals(styles.size, styles.map { it.id }.toSet().size)
        styles.forEach { style ->
            assertTrue("$style has no name", context.getString(style.nameRes).isNotBlank())
            assertEquals("styles/${style.id}.webp", style.assetPath)
            val image = StyleImages.draw(style)
            assertEquals(256, image.width)
            assertEquals(256, image.height)
            assertTrue("$style's image is one flat colour", pixels(image).toSet().size > 1)
        }
    }

    /** The style images are drawn the same way every time, so the exported file equals the fallback. */
    @Test
    fun style_images_are_deterministic() {
        ArtisticStyle.entries.forEach { style ->
            assertArrayEquals("$style", pixels(StyleImages.draw(style)), pixels(StyleImages.draw(style)))
        }
    }

    /** Sets [style] and [strength] on [processor], runs it on [input] and returns the edited bitmap. */
    private fun run(processor: ArtisticProcessor, input: Bitmap, style: ArtisticStyle, strength: Float): Bitmap {
        processor.style = style
        processor.strength = strength
        val result = runBlocking { processor.process(input) {} }
        return (result as EditResult.Edited).bitmap
    }

    /** Makes a [width] x [height] opaque picture whose pixels run through many colours. */
    private fun texture(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height) { index ->
            Color.rgb((index * 7) % 256, (index * 13) % 256, (index * 29) % 256)
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /** All of [bitmap]'s pixels, row by row, as packed ARGB values. */
    private fun pixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels
    }
}
