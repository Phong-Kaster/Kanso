package com.phongkaster.kanso.data.edit.retouch

import android.graphics.Bitmap
import android.graphics.Rect
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.core.Rect as CvRect

/**
 * The real [SkinSmoother]: OpenCV's bilateral filter, run on the face's rectangle only.
 *
 * A bilateral filter is a blur that respects edges: each pixel becomes an average of its
 * neighbours, but a neighbour counts less the more its colour differs. Small differences (pores,
 * blemishes, noise on skin) are averaged away while large ones (the edges of the eyes, lips and
 * hair) stay sharp, which is what skin smoothing wants.
 *
 * Why not the engine's GL beauty shader (`CompositeBeautyProgram` in `core/imageprocessor`): it
 * draws through OpenGL and so must run on a thread that owns a GL context, which a background
 * coroutine does not; OpenCV runs on any thread. OpenCV's native library is packed in the app
 * (`opencv/`), so nothing is downloaded. This file and [EngineFaceFinder] are the only Retouch
 * files that name engine classes; they never run in a JVM test, where fakes stand in.
 */
class EngineSkinSmoother : SkinSmoother {

    /**
     * Returns a copy of [photo] whose pixels inside [face] went through the bilateral filter.
     * Throws `IllegalStateException` when OpenCV's native library will not load, which
     * [RetouchProcessor] reports as the model being unavailable.
     *
     * OpenCV keeps a picture in a `Mat` (a matrix of pixels) whose memory lives in native code,
     * outside Java's garbage collector, so every Mat made here is released in `finally`.
     */
    override fun smooth(photo: Bitmap, face: Rect): Bitmap {
        // initLocal loads the OpenCV library packed inside the app; false means it could not.
        check(OpenCVLoader.initLocal()) { "OpenCV's native library did not load" }
        // A mutable ARGB copy: OpenCV writes the result back into it, and photo stays untouched.
        val out = photo.copy(Bitmap.Config.ARGB_8888, true)
        val left = face.left.coerceIn(0, photo.width)
        val top = face.top.coerceIn(0, photo.height)
        val right = face.right.coerceIn(0, photo.width)
        val bottom = face.bottom.coerceIn(0, photo.height)
        if (right <= left || bottom <= top) return out
        val whole = Mat()
        val rgb = Mat()
        val filtered = Mat()
        val rgba = Mat()
        var region: Mat? = null
        try {
            // bitmapToMat gives four channels (red, green, blue, alpha).
            Utils.bitmapToMat(out, whole)
            // submat is a window onto the face's pixels in `whole`, not a copy: writing into it
            // writes into `whole`.
            region = whole.submat(CvRect(left, top, right - left, bottom - top))
            // The bilateral filter takes one or three channels, so alpha is dropped and put back.
            Imgproc.cvtColor(region, rgb, Imgproc.COLOR_RGBA2RGB)
            Imgproc.bilateralFilter(rgb, filtered, diameterFor(right - left, bottom - top), SIGMA_COLOR, SIGMA_SPACE)
            Imgproc.cvtColor(filtered, rgba, Imgproc.COLOR_RGB2RGBA)
            rgba.copyTo(region)
            Utils.matToBitmap(whole, out)
        } finally {
            region?.release()
            rgba.release()
            filtered.release()
            rgb.release()
            whole.release()
        }
        return out
    }

    /**
     * The filter's neighbourhood, in pixels across, for a face [width] x [height] pixels: about a
     * fiftieth of the face, between [MIN_DIAMETER] and [MAX_DIAMETER], so a large face is smoothed
     * as much as a small one looks smoothed, without the filter getting very slow.
     */
    private fun diameterFor(width: Int, height: Int): Int =
        (minOf(width, height) / FACE_FRACTION).coerceIn(MIN_DIAMETER, MAX_DIAMETER)

    /** The filter's fixed settings. */
    private companion object {
        /** How different two colours (0..255) may be and still be averaged together. */
        const val SIGMA_COLOR = 40.0

        /** How far, in pixels, neighbours still count. */
        const val SIGMA_SPACE = 8.0

        /** The face's shorter side divided by this gives the neighbourhood's size. */
        const val FACE_FRACTION = 50

        /** The smallest neighbourhood, in pixels across. */
        const val MIN_DIAMETER = 5

        /** The largest neighbourhood, in pixels across; larger is much slower. */
        const val MAX_DIAMETER = 15
    }
}
