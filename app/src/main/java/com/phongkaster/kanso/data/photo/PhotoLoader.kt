package com.phongkaster.kanso.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Returns the `inSampleSize` to decode a [width] x [height] image with, so that the decoded
 * image's longest edge is at most [maxEdgePx] pixels.
 *
 * Android's decoder can shrink a photo while it reads it: with `inSampleSize = 2` it keeps one
 * pixel out of every 2 in each direction (a quarter of the memory), with 4 one out of 4, and so on.
 * It works best with powers of two, so this returns the smallest power of two (1, 2, 4, 8 ...)
 * that brings the longest edge down to [maxEdgePx] or less. A photo already small enough gets 1,
 * meaning "full size". Sizes that are zero or negative also get 1.
 *
 * Example: an 8000 x 6000 photo on a phone whose longest edge is 2673 pixels, with
 * `maxEdgePx = 2 * 2673 = 5346`, gets 2 and is decoded at 4000 x 3000.
 */
fun sampleSizeFor(width: Int, height: Int, maxEdgePx: Int): Int {
    if (width <= 0 || height <= 0 || maxEdgePx <= 0) return 1
    val longest = maxOf(width, height)
    var sample = 1
    // The decoder rounds a shrunk edge up, so the check uses the rounded-up size.
    while ((longest + sample - 1) / sample > maxEdgePx) {
        sample *= 2
    }
    return sample
}

/**
 * Returns the longest edge, in pixels, a photo shown on a [screenWidthPx] x [screenHeightPx]
 * screen is decoded to: twice the screen's longest edge.
 *
 * Twice, not exactly once, keeps the photo sharp when the person later zooms in a little, while
 * still never decoding a huge camera photo at full size. Using the longest edge means the answer
 * is the same whether the phone is held upright or sideways, so rotating does not decode again.
 */
fun displayMaxEdgePx(screenWidthPx: Int, screenHeightPx: Int): Int =
    2 * maxOf(screenWidthPx, screenHeightPx)

/**
 * Decodes the photo the tool screen shows: upright, and shrunk to what the screen needs.
 *
 * A phone camera photo can be 8000 x 6000 pixels, about 190 MB once decoded, which would run the
 * app out of memory. So the loader first reads only the photo's size (no pixels), works out how
 * much to shrink it with [sampleSizeFor], and only then decodes it, already shrunk. The full-size
 * image is never held in memory.
 *
 * Cameras usually store pixels as the sensor saw them and write in the EXIF data (a small block
 * of facts inside the JPEG) how the picture must be turned to look right. The loader reads that
 * "orientation" tag and turns or mirrors the decoded pixels, so the photo always appears upright.
 *
 * The class is `open`, and so is [load], so that tests can hand the screen a ready-made photo.
 * [context] is only used for its ContentResolver, which opens the photo's Uri.
 */
open class PhotoLoader(private val context: Context) {

    /**
     * Decodes the photo at [uri] (the text form of a content or file Uri), upright, with its
     * longest edge at most about [maxEdgePx] pixels. Returns null if the photo is missing, not
     * readable by this app, or not an image; it never throws for those.
     *
     * The work runs on Dispatchers.IO, a pool of background threads meant for files, so the
     * screen keeps drawing while the photo is read.
     */
    open suspend fun load(uri: String, maxEdgePx: Int): LoadedPhoto? = withContext(Dispatchers.IO) {
        try {
            decodeUpright(Uri.parse(uri), maxEdgePx)?.let(::LoadedPhoto)
        } catch (e: CancellationException) {
            // The screen went away while decoding: let the coroutine stop as asked.
            throw e
        } catch (e: Exception) {
            // Missing file (FileNotFoundException), access taken away (SecurityException), a
            // damaged file: all mean "cannot be shown", which the screen says plainly.
            null
        } catch (e: OutOfMemoryError) {
            // Should not happen thanks to the sample size, but a crash would be worse than a
            // message.
            null
        }
    }

    /**
     * Reads the size of the photo at [uri], decodes it shrunk for [maxEdgePx], and turns it
     * upright from its EXIF orientation. Returns null when the data is not a decodable image.
     */
    private fun decodeUpright(uri: Uri, maxEdgePx: Int): Bitmap? {
        val resolver = context.contentResolver

        // Step 1: read the size only. inJustDecodeBounds makes the decoder fill outWidth and
        // outHeight from the file's header and return no pixels at all, so this is cheap.
        // (In this mode decodeStream always returns null; the answer is in `bounds`.)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Step 2: decode the pixels, already shrunk by the sample size. A stream can be read
        // only once, so the photo is opened again for each step.
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdgePx)
        }
        val pixelStream = resolver.openInputStream(uri) ?: return null
        val decoded = pixelStream.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        // Step 3: read the EXIF orientation (1 = already upright when it is missing).
        // A damaged EXIF block must not lose a photo that decoded fine: show it as it is.
        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        return turnUpright(decoded, orientation)
    }

    /**
     * Returns [bitmap] turned and mirrored as the EXIF [orientation] (1 to 8) asks, or [bitmap]
     * itself when it is already upright. The turned copy replaces the original, which is freed.
     */
    private fun turnUpright(bitmap: Bitmap, orientation: Int): Bitmap {
        // A Matrix describes a geometric change: rotate turns clockwise by the given degrees
        // (the y axis points down on screen), and a scale of -1 mirrors along that axis.
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
            else -> return bitmap
        }
        // createBitmap applies the matrix and moves the result back into view, so a 400 x 200
        // photo turned a quarter becomes 200 x 400. `true` smooths the pixels while doing it.
        val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (upright !== bitmap) bitmap.recycle()
        return upright
    }
}
