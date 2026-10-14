package com.phongkaster.kanso.data.edit.cutout

import android.graphics.Bitmap
import kotlin.math.roundToInt

/**
 * Cuts the subject out of a photo with its mask: plain Kotlin over pixel arrays, no engine, so a
 * JVM test can check it pixel by pixel.
 *
 * Every pixel keeps its own red, green and blue exactly; only its alpha (how opaque it is) comes
 * from the mask. Near the subject's edge the alpha follows the mask on a short soft ramp, so the
 * outline is not jagged; well inside the subject the pixel stays fully opaque, and where the mask
 * says background it becomes fully transparent.
 */
object MaskCompositor {

    /** At or below this foreground probability a pixel is background: alpha 0. */
    const val BACKGROUND_BELOW = 0.4f

    /** At or above this foreground probability a pixel is the subject: alpha 255. */
    const val SUBJECT_ABOVE = 0.6f

    /** The probability that splits subject from background: 0.5, halfway along the ramp. */
    const val THRESHOLD = 0.5f

    /**
     * Returns a new ARGB_8888 bitmap the size of [photo] in which each pixel has [photo]'s colour
     * and the alpha its [mask] value gives (see the class description). [mask] holds one value
     * per pixel, row by row, as [Segmenter.segment] returns it. The result says it has an alpha
     * channel (`hasAlpha()` is true), so Save writes it as a transparent PNG. [photo] is not
     * changed. Throws `IllegalArgumentException` when [mask] is not one value per pixel.
     */
    fun composite(photo: Bitmap, mask: FloatArray): Bitmap {
        val width = photo.width
        val height = photo.height
        require(mask.size == width * height) {
            "The mask has ${mask.size} values for a ${width}x$height photo"
        }
        // getPixels gives each pixel as one Int, 0xAARRGGBB, not premultiplied by its alpha.
        val pixels = IntArray(width * height)
        photo.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            val alpha = alphaFor(mask[i])
            // Keep the low 24 bits (the colour) and replace the top 8 bits (the alpha).
            pixels[i] = (alpha shl 24) or (pixels[i] and 0x00FFFFFF)
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        result.setHasAlpha(true)
        return result
    }

    /**
     * True when [mask] calls at least one pixel the subject (a value at or above [THRESHOLD]);
     * false when the whole mask is background, which means the segmenter found nobody.
     */
    fun hasSubject(mask: FloatArray): Boolean = mask.any { it >= THRESHOLD }

    /**
     * The alpha, 0 to 255, for one foreground probability: 0 up to [BACKGROUND_BELOW], 255 from
     * [SUBJECT_ABOVE], and a straight ramp between them. A NaN (a broken mask value) counts as
     * background.
     */
    private fun alphaFor(probability: Float): Int {
        if (probability.isNaN() || probability <= BACKGROUND_BELOW) return 0
        if (probability >= SUBJECT_ABOVE) return 255
        val t = (probability - BACKGROUND_BELOW) / (SUBJECT_ABOVE - BACKGROUND_BELOW)
        return (t * 255f).roundToInt().coerceIn(0, 255)
    }
}
