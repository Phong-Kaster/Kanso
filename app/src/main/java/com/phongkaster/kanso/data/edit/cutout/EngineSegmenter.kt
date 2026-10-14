package com.phongkaster.kanso.data.edit.cutout

import android.graphics.Bitmap
import android.graphics.Color
import com.photo.editor.localml.removebackground.SelfieSegmentationUC
import org.opencv.android.OpenCVLoader

/**
 * The real [Segmenter]: ML Kit's selfie segmentation, wrapped by the engine's
 * `SelfieSegmentationUC` (in `libs/localml.aar`, reached through `core/localml`).
 *
 * Why this model: it is bundled inside the app, so it works offline with no download, as Kanso
 * promises. The engine's other cut-out paths (`SubjectSegmentationUC`, and `RemoveBackgroundUC`,
 * which falls back to it) use a Google Play services model that downloads on first use, so they
 * are never called. The selfie model is trained on people: a photo with no person in it usually
 * gives an empty mask, which [CutoutProcessor] reports as "no subject".
 *
 * This is the only file in the app that names engine classes. [engine] builds the engine object
 * when the first photo is segmented (not when the app starts), and a different one can be passed
 * in. The engine is native code, so this class is never run in a JVM test; tests use a fake
 * [Segmenter] instead.
 *
 * What the engine returns is not documented (the AAR ships no source; see
 * `.harness/knowledge/reference/engine-cutout.md`). Kanso calls `invoke(photo, true)`: the private
 * methods `segmentationToMask` and `segmentationToImageMask` suggest the flag picks the "image
 * mask", the photo with its background made transparent. To be safe either way, [toMask] reads the
 * returned bitmap's alpha channel as the probability when it has any see-through pixel, and its
 * brightness (a black-and-white mask) when it is fully opaque. Which of the two the engine really
 * gives must be checked on a phone (DoD 7).
 */
class EngineSegmenter(
    private val engine: () -> SelfieSegmentationUC = { SelfieSegmentationUC() },
) : Segmenter {

    /** The engine object, built on first use and then kept, because building it loads the model. */
    private var segmentation: SelfieSegmentationUC? = null

    /**
     * Segments [photo] with the engine and returns one foreground probability per pixel of
     * [photo], or null when the engine returns no picture. Throws `IllegalStateException` when
     * OpenCV's native library will not load, which [CutoutProcessor] reports as the model being
     * unavailable.
     */
    override suspend fun segment(photo: Bitmap): FloatArray? {
        // The engine converts pictures through OpenCV, whose native library must be loaded
        // first; initLocal loads the copy packed inside the app and returns false if it cannot.
        check(OpenCVLoader.initLocal()) { "OpenCV's native library did not load" }
        val uc = segmentation ?: engine().also { segmentation = it }
        // The engine's result type comes from Java bytecode, so it is read as possibly null.
        val result: Bitmap = uc.invoke(photo, true) ?: return null
        return toMask(result, photo.width, photo.height)
    }

    /**
     * Turns the engine's [result] into a mask of [width] x [height] values from 0 to 1, scaling
     * the result to the photo's size first if the engine returned another size. See the class
     * description for how a value is read from a pixel.
     */
    private fun toMask(result: Bitmap, width: Int, height: Int): FloatArray {
        val sized = if (result.width == width && result.height == height) {
            result
        } else {
            Bitmap.createScaledBitmap(result, width, height, true)
        }
        val pixels = IntArray(width * height)
        sized.getPixels(pixels, 0, width, 0, 0, width, height)
        val seeThrough = pixels.any { Color.alpha(it) < OPAQUE }
        return FloatArray(pixels.size) { i ->
            val pixel = pixels[i]
            if (seeThrough) {
                Color.alpha(pixel) / OPAQUE.toFloat()
            } else {
                // A fully opaque result is read as a black (background) and white (subject) mask.
                maxOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel)) / OPAQUE.toFloat()
            }
        }
    }

    /** Values the mask conversion shares. */
    private companion object {
        /** The alpha (and the channel value) of a fully opaque, fully bright pixel. */
        const val OPAQUE = 255
    }
}
