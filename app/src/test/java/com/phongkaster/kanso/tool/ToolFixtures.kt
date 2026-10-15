package com.phongkaster.kanso.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.robolectric.Shadows.shadowOf

/**
 * Test helpers for the tool screen: photo fixtures made at test time. (The fake loader that goes
 * with them is [FakePhotoLoader], in its own file.)
 *
 * The fixtures are real JPEG files written into a temporary folder. They are served to the app
 * under a `content://` Uri through Robolectric's ContentResolver, the way MediaStore serves real
 * photos, so the loader reads them exactly as it reads a phone's photos. (A `file://` Uri would
 * do too on a phone, but Windows paths do not fit in one cleanly.)
 */
object ToolFixtures {

    /** The authority of the made-up content Uris the fixtures are served under. */
    private const val AUTHORITY = "com.phongkaster.kanso.test.fixtures"

    /**
     * Draws a [width] x [height] picture whose left half is red and right half is blue, so a
     * test can tell after decoding which way it was turned.
     */
    fun twoHalves(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.RED
        canvas.drawRect(0f, 0f, width / 2f, height.toFloat(), paint)
        paint.color = Color.BLUE
        canvas.drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    /** Draws a [width] x [height] diagonal gradient, a stand-in for a real photo in pictures. */
    fun gradient(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        paint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(Color.rgb(255, 183, 77), Color.rgb(229, 57, 53), Color.rgb(57, 73, 171)),
            null,
            Shader.TileMode.CLAMP,
        )
        Canvas(bitmap).drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    /** The width, in pixels, of each stripe of [narrowStripes]. */
    const val STRIPE_PX = 8

    /**
     * Draws the narrow striped photo of the backdrop's checks (DoD 27): 300 x 800 pixels, its
     * upper half vertical red-and-white stripes [STRIPE_PX] pixels wide, its lower half
     * blue-and-white stripes. Red stripes start at x = 0, so they cover x = 0..7, 16..23 and so on.
     */
    fun narrowStripes(): Bitmap {
        val width = 300
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint()
        for (left in 0 until width step 2 * STRIPE_PX) {
            paint.color = Color.RED
            canvas.drawRect(left.toFloat(), 0f, (left + STRIPE_PX).toFloat(), height / 2f, paint)
            paint.color = Color.BLUE
            canvas.drawRect(left.toFloat(), height / 2f, (left + STRIPE_PX).toFloat(), height.toFloat(), paint)
        }
        return bitmap
    }

    /**
     * Writes [bitmap] as a JPEG into [file] and, if [orientation] is given, stores that EXIF
     * orientation tag (1 to 8) in it, as a camera would.
     */
    fun writeJpeg(file: File, bitmap: Bitmap, orientation: Int? = null): File {
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (orientation != null) {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.saveAttributes()
        }
        return file
    }

    /**
     * Serves [file] through the app's ContentResolver under a content Uri named [name], and
     * returns that Uri as text. Every open gets a fresh stream, as a real provider gives.
     */
    fun serve(context: Context, file: File, name: String): String {
        val uri = Uri.parse("content://$AUTHORITY/$name")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { FileInputStream(file) }
        return uri.toString()
    }

    /** A content Uri nothing is served under: opening it fails like a deleted photo. */
    const val MISSING_URI = "content://$AUTHORITY/missing.jpg"
}
