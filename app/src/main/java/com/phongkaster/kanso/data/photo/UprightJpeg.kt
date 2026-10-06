package com.phongkaster.kanso.data.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

/**
 * Decodes a whole photo at full size, every pixel of it, from a stream.
 *
 * It is a small interface so that a test can hand the saver a decoder that runs out of memory,
 * the way a huge photo would on a real phone, and check that the saver still saves something.
 */
fun interface FullSizeDecoder {

    /** Decodes [input] at full size, or returns null when the bytes are not a picture. */
    fun decode(input: InputStream): Bitmap?
}

/** The decoder used outside tests: Android's own BitmapFactory, with no shrinking at all. */
val DefaultFullSizeDecoder: FullSizeDecoder = FullSizeDecoder { input -> BitmapFactory.decodeStream(input) }

/** The JPEG quality a turned photo is written at: 95 out of 100, close to the camera's own. */
const val UPRIGHT_JPEG_QUALITY = 95

/**
 * Reads the EXIF orientation tag (1 to 8) from the JPEG bytes in [input].
 *
 * EXIF is a small block of facts a camera writes inside the JPEG; its "orientation" tells how the
 * stored pixels must be turned to look right. A photo without the tag, or with a damaged EXIF
 * block, counts as already upright (1), so it is copied as it is rather than lost.
 */
fun readExifOrientation(input: InputStream): Int = try {
    ExifInterface(input).getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL,
    )
} catch (_: Exception) {
    // A damaged EXIF block (IOException) or an odd value the reader trips over: keep the photo.
    ExifInterface.ORIENTATION_NORMAL
}

/**
 * Returns the Matrix that turns and mirrors the stored pixels of a photo with EXIF [orientation]
 * upright, or null when the photo is already upright (orientation 1, 0 for "unknown", or any
 * value outside 1 to 8) and its bytes can be copied untouched.
 *
 * A Matrix describes a geometric change: rotate turns clockwise by the given degrees (the y axis
 * points down on screen), and a scale of -1 mirrors along that axis.
 */
fun uprightMatrix(orientation: Int): Matrix? {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return null
    }
    return matrix
}

/**
 * Returns a copy of [bitmap] changed by [matrix] (from [uprightMatrix]); [bitmap] itself is
 * freed once the copy exists. A 4000 x 3000 photo turned a quarter becomes 3000 x 4000.
 *
 * It needs memory for both pictures at once for a moment, so it may throw OutOfMemoryError on
 * a very large photo; the caller catches that.
 */
fun applyUpright(bitmap: Bitmap, matrix: Matrix): Bitmap {
    // createBitmap applies the matrix and moves the result back into view. `true` smooths the
    // pixels while turning, which matters only for the mirror-and-turn cases.
    val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (upright !== bitmap) bitmap.recycle()
    return upright
}
