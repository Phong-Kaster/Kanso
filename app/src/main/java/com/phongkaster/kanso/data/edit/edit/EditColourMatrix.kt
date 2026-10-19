package com.phongkaster.kanso.data.edit.edit

import android.graphics.ColorMatrix
import kotlin.math.abs

/**
 * The one place that turns the Edit tool's four colour sliders (brightness, contrast, saturation,
 * warmth) into numbers.
 *
 * Two parts of the app need those numbers and they must agree to the last digit:
 *
 * - [EditProcessor] sends every pixel of the photo through them when Done renders the edit;
 * - the Edit screen's live preview hands the very same 20 numbers to Compose
 *   (`ColorFilter.colorMatrix`), so the photo changes on the GPU while a slider is still being
 *   dragged, without re-rendering a single pixel on the CPU.
 *
 * Because both read [of], what the person sees while editing is what Done saves (within the
 * rounding of each channel), and there is no second formula that could drift away from the first.
 *
 * A colour matrix (Android's `ColorMatrix`, and Compose's `ColorMatrix`, which uses the same
 * layout) is a 4 x 5 grid of numbers stored row after row in 20 floats: each output channel (red,
 * green, blue, alpha) is a weighted sum of the input red, green, blue and alpha plus an offset.
 * The offsets are in 0..255 units in both Android and Compose. Chaining two matrices
 * (`postConcat`) gives one matrix that does both, so each pixel is visited once.
 */
object EditColourMatrix {

    /** How far brightness at its maximum lifts every channel, in 0..255 units. */
    const val BRIGHTNESS_SHIFT = 80f

    /** How far warmth at its maximum lifts red and lowers blue, in 0..255 units. */
    const val WARMTH_SHIFT = 40f

    /**
     * How close two matrix numbers must be to count as equal in [isIdentity]: far below anything
     * a slider can produce, yet above the float rounding of chaining matrices.
     */
    private const val IDENTITY_TOLERANCE = 1e-4f

    /**
     * Builds the one colour matrix the four sliders of [settings] make together, applied in this
     * order: contrast, saturation, warmth, brightness. The crop, turns, flips and filter of
     * [settings] play no part in it. Returns 20 floats in Android's `ColorMatrix` layout.
     *
     * - Contrast scales each channel away from (or towards) mid-grey 128: by up to 2 at the
     *   maximum, down to 0.2 at the minimum.
     * - Saturation uses `setSaturation`: 0 at the minimum (every channel becomes the pixel's
     *   luminance, so black and white), 1 in the middle, 2 at the maximum.
     * - Warmth adds up to [WARMTH_SHIFT] to red and takes it from blue (the reverse when cooler).
     * - Brightness adds up to [BRIGHTNESS_SHIFT] to every channel (or takes it away).
     *
     * With every slider at its middle this is the identity matrix (each pixel stays as it was).
     */
    fun of(settings: EditSettings): FloatArray {
        val matrix = ColorMatrix()
        val contrast = settings.contrast.coerceIn(EditSettings.MIN, EditSettings.MAX)
        if (contrast != EditSettings.MIDDLE) {
            val scale = if (contrast > 0f) 1f + contrast else 1f + contrast * 0.8f
            // Scaling around mid-grey: 128 stays 128, everything else moves away from it.
            val offset = 128f * (1f - scale)
            matrix.postConcat(ColorMatrix(scaleAndShift(scale, offset, offset, offset)))
        }
        val saturation = settings.saturation.coerceIn(EditSettings.MIN, EditSettings.MAX)
        if (saturation != EditSettings.MIDDLE) {
            val saturate = ColorMatrix()
            saturate.setSaturation(1f + saturation)
            matrix.postConcat(saturate)
        }
        val warmth = settings.warmth.coerceIn(EditSettings.MIN, EditSettings.MAX)
        if (warmth != EditSettings.MIDDLE) {
            val shift = warmth * WARMTH_SHIFT
            matrix.postConcat(ColorMatrix(scaleAndShift(1f, shift, 0f, -shift)))
        }
        val brightness = settings.brightness.coerceIn(EditSettings.MIN, EditSettings.MAX)
        if (brightness != EditSettings.MIDDLE) {
            val shift = brightness * BRIGHTNESS_SHIFT
            matrix.postConcat(ColorMatrix(scaleAndShift(1f, shift, shift, shift)))
        }
        return matrix.array
    }

    /**
     * True when [matrix] (20 floats, `ColorMatrix` layout) leaves every colour as it was: ones on
     * the diagonal, zeros everywhere else, within a tiny tolerance. The preview uses it to draw the
     * photo with no colour filter at all when no slider has moved.
     */
    fun isIdentity(matrix: FloatArray): Boolean {
        if (matrix.size != MATRIX_SIZE) return false
        for (i in matrix.indices) {
            // The diagonal of a 4 x 5 grid stored row after row sits at 0, 6, 12 and 18.
            val expected = if (i % (ROW_LENGTH + 1) == 0) 1f else 0f
            if (abs(matrix[i] - expected) > IDENTITY_TOLERANCE) return false
        }
        return true
    }

    /** True when the four sliders of [settings] make no change at all to the colours. */
    fun isIdentity(settings: EditSettings): Boolean = isIdentity(of(settings))

    /** How many numbers a colour matrix holds: 4 rows of 5. */
    private const val MATRIX_SIZE = 20

    /** How many numbers one row of a colour matrix holds (four weights and an offset). */
    private const val ROW_LENGTH = 5

    /**
     * A colour matrix's 20 numbers that multiply red, green and blue by [scale] and then add
     * [red], [green] and [blue] to them, leaving alpha alone.
     */
    private fun scaleAndShift(scale: Float, red: Float, green: Float, blue: Float): FloatArray = floatArrayOf(
        scale, 0f, 0f, 0f, red,
        0f, scale, 0f, 0f, green,
        0f, 0f, scale, 0f, blue,
        0f, 0f, 0f, 1f, 0f,
    )
}
