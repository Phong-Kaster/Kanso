package com.phongkaster.kanso.tool.edit

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditColourMatrix
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks the RGB tool's step in the one colour matrix ([EditColourMatrix.of]) and its place in the
 * settings (plan `docs/plans/rgb-tool.md`, decisions 3 and 5):
 *
 * - each channel alone multiplies only its own channel, by 0 at -100, by 1 at 0 and by 2 at +100,
 *   adding nothing;
 * - black stays black whatever the three channels are, so the shadows are never tinted;
 * - the channels come after warmth and before brightness: a channel at -100 removes the red that
 *   warmth added, and blacks lifted by brightness stay a neutral grey;
 * - RGB is its own tool: a channel away from 0 marks RGB as changed, not Adjust, and an Adjust
 *   slider marks Adjust, not RGB; either counts as a colour change.
 *
 * The matrix is applied to a pixel here with the same sums [EditColourMatrix] documents (each
 * output channel a weighted sum of the inputs plus an offset, clamped to 0..255), so the test reads
 * the numbers, not the picture; `ColourMatrixParityTest` checks the picture. Robolectric stands in
 * for `android.graphics.ColorMatrix`, which builds the numbers.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RgbMatrixTest {

    /**
     * Each channel alone puts its factor on the matrix's diagonal and nothing else: 0 at -100
     * (x0), 1 at 0 (x1, the identity) and 2 at +100 (x2), the other two channels untouched.
     */
    @Test
    fun each_channel_alone_scales_only_itself_by_0_1_or_2() {
        listOf(EditSettings.MIN to 0f, EditSettings.MIDDLE to 1f, EditSettings.MAX to 2f).forEach(
            action = { (value, factor) ->
                assertArrayEquals("red at $value", scale(factor, 1f, 1f), EditColourMatrix.of(EditSettings(red = value)), DELTA)
                assertArrayEquals("green at $value", scale(1f, factor, 1f), EditColourMatrix.of(EditSettings(green = value)), DELTA)
                assertArrayEquals("blue at $value", scale(1f, 1f, factor), EditColourMatrix.of(EditSettings(blue = value)), DELTA)
            },
        )
    }

    /** Values between the ends scale proportionally: +50 is x1.5, -50 is x0.5. */
    @Test
    fun a_channel_at_50_scales_by_one_and_a_half() {
        assertArrayEquals(scale(1.5f, 0.5f, 1f), EditColourMatrix.of(EditSettings(red = 0.5f, green = -0.5f)), DELTA)
    }

    /** A black pixel comes out black for every mix of the three channels at -100, 0 and +100. */
    @Test
    fun black_stays_black() {
        val values = listOf(EditSettings.MIN, EditSettings.MIDDLE, EditSettings.MAX)
        values.forEach(
            action = { red ->
                values.forEach(
                    action = { green ->
                        values.forEach(
                            action = { blue ->
                                val settings = EditSettings(red = red, green = green, blue = blue)
                                val out = apply(EditColourMatrix.of(settings), 0, 0, 0)
                                assertEquals("$settings tinted black", listOf(0, 0, 0), out)
                            },
                        )
                    },
                )
            },
        )
    }

    /** A bright channel doubled is clamped at 255, never wrapped round. */
    @Test
    fun a_doubled_channel_is_clamped_at_255() {
        assertEquals(listOf(255, 100, 100), apply(EditColourMatrix.of(EditSettings(red = EditSettings.MAX)), 200, 100, 100))
    }

    /**
     * Brightness comes after the channels: black with red at -100 and brightness at +100 becomes
     * the neutral grey brightness lifts it to. Were brightness first, red would then be scaled to
     * 0 and the grey would turn cyan.
     */
    @Test
    fun brightness_comes_after_the_channels_so_lifted_blacks_stay_grey() {
        val shift = EditColourMatrix.BRIGHTNESS_SHIFT.toInt()
        val matrix = EditColourMatrix.of(EditSettings(red = EditSettings.MIN, brightness = EditSettings.MAX))
        assertEquals(listOf(shift, shift, shift), apply(matrix, 0, 0, 0))
    }

    /**
     * Warmth comes before the channels: grey with warmth at +100 and red at -100 has no red left.
     * Were warmth after, it would add its red back on top.
     */
    @Test
    fun warmth_comes_before_the_channels() {
        val matrix = EditColourMatrix.of(EditSettings(warmth = EditSettings.MAX, red = EditSettings.MIN))
        assertEquals(0, apply(matrix, 100, 100, 100)[0])
    }

    /**
     * RGB is its own tool in the row: a channel away from 0 marks RGB, not Adjust, and an Adjust
     * slider marks Adjust, not RGB. Either makes a colour change and the settings no longer
     * neutral; all three back at 0 are neutral again.
     */
    @Test
    fun rgb_and_adjust_mark_only_their_own_tool() {
        val rgb = EditSettings(green = 0.2f)
        assertFalse(rgb.isNeutral(EditTool.RGB))
        assertTrue(rgb.isNeutral(EditTool.ADJUST))
        assertTrue(rgb.hasColourChange)
        assertFalse(rgb.isNeutral)
        assertFalse(EditColourMatrix.isIdentity(rgb))

        val adjust = EditSettings(contrast = 0.2f)
        assertTrue(adjust.isNeutral(EditTool.RGB))
        assertFalse(adjust.isNeutral(EditTool.ADJUST))

        assertTrue(EditSettings(red = 0f, green = 0f, blue = 0f).isNeutral)
    }

    /** RGB is the fourth tool of the row, right after Adjust; HSL follows RGB, Curves HSL and Filters Curves. */
    @Test
    fun rgb_sits_right_after_adjust() {
        assertEquals(EditTool.ADJUST.ordinal + 1, EditTool.RGB.ordinal)
        assertEquals(EditTool.RGB.ordinal + 1, EditTool.HSL.ordinal)
        assertEquals(EditTool.HSL.ordinal + 1, EditTool.CURVES.ordinal)
        assertEquals(EditTool.CURVES.ordinal + 1, EditTool.FILTERS.ordinal)
        assertEquals(10, EditTool.entries.size)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** The 20 numbers of a matrix that multiplies red, green and blue by [r], [g] and [b] alone. */
    private fun scale(r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    /**
     * The pixel ([r], [g], [b]), fully opaque, sent through [matrix] the way the processor sends
     * it: each channel a weighted sum plus an offset, rounded and clamped to 0..255.
     */
    private fun apply(matrix: FloatArray, r: Int, g: Int, b: Int): List<Int> = (0 until 3).map(
        transform = { row ->
            // Each row of the matrix is 5 numbers: the weights of red, green, blue, alpha, then the offset.
            val o = row * 5
            val sum = matrix[o] * r + matrix[o + 1] * g + matrix[o + 2] * b + matrix[o + 3] * 255f + matrix[o + 4]
            (sum + 0.5f).toInt().coerceIn(0, 255)
        },
    )

    /** Values shared by the tests. */
    private companion object {
        /** How far two matrix numbers may differ: float rounding of chaining matrices. */
        const val DELTA = 1e-4f
    }
}
