package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.data.edit.edit.LayerSize
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Proves the size slider's number for each kind of layer (ADR-005): a sticker's is its width in
 * percent of the photo, a text's is its letter height from 1 to 50 percent of the photo's height,
 * and every conversion comes back to where it started. Plain JVM, no Android.
 */
class LayerSizeTest {

    /** A sticker 30 percent of the photo wide shows 30, and 30 gives that width back. */
    @Test
    fun a_sticker_size_is_its_width_in_percent() {
        assertEquals(30f, LayerSize.ofPicture(0.3f), EPSILON)
        assertEquals(0.3f, LayerSize.pictureWidth(30f), EPSILON)
    }

    /** A sticker wider than the photo, or tinier than 1 percent, shows the slider's end. */
    @Test
    fun a_sticker_size_stays_within_1_and_100() {
        assertEquals(LayerSize.MAX, LayerSize.ofPicture(1.7f), EPSILON)
        assertEquals(LayerSize.MIN, LayerSize.ofPicture(0.001f), EPSILON)
        assertEquals(1f, LayerSize.pictureWidth(250f), EPSILON)
    }

    /** Size 1 makes a letter 1 percent of the photo's height and size 100 makes it 50 percent. */
    @Test
    fun a_text_size_runs_from_1_to_50_percent_of_the_photo_height() {
        assertEquals(0.01f, LayerSize.textLetterHeight(1f), EPSILON)
        assertEquals(0.5f, LayerSize.textLetterHeight(100f), EPSILON)
        assertEquals(0.255f, LayerSize.textLetterHeight(50.5f), EPSILON)
    }

    /**
     * A text 5 letter heights wide on a 3:2 photo, at size 20: its letters are about 10.4 percent
     * of the photo's height, so it is 5 x 0.104 / 1.5 of the photo's width; reading that width
     * back gives 20 again.
     */
    @Test
    fun a_text_width_and_its_size_convert_both_ways() {
        val width = LayerSize.textWidth(20f, blockWidthEms = 5f, photoAspect = 1.5f)
        assertEquals(5f * LayerSize.textLetterHeight(20f) / 1.5f, width, EPSILON)
        assertEquals(20f, LayerSize.ofText(width, blockWidthEms = 5f, photoAspect = 1.5f), 1e-3f)
    }

    /** A block with no width (nothing measured yet) reads as the starting size, not a division by zero. */
    @Test
    fun a_text_with_no_measured_width_reads_as_the_default_size() {
        assertEquals(LayerSize.TEXT_DEFAULT, LayerSize.ofText(0.3f, blockWidthEms = 0f, photoAspect = 1f), EPSILON)
    }

    /** Values shared by the tests. */
    private companion object {
        /** Float noise allowed in a comparison. */
        const val EPSILON = 1e-5f
    }
}
