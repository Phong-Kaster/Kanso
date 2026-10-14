package com.phongkaster.kanso.tool.retouch

import android.graphics.Bitmap
import android.graphics.Rect
import com.phongkaster.kanso.data.edit.retouch.SkinSmoother
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A stand-in for the engine's OpenCV smoother (which cannot load on the JVM): it records the size
 * of every photo it is handed, throws [failure] if one is given, and otherwise returns a copy of
 * the photo in which each pixel inside the face is the average of the 5 x 5 pixels around it
 * (a box blur, kept inside the face), and every pixel outside the face is copied unchanged.
 * A box blur cuts the variance of pixel noise to about a twenty-fifth, plenty for the tests.
 */
class FakeSkinSmoother(private val failure: Throwable? = null) : SkinSmoother {

    /** Width and height of each photo handed to [smooth], oldest first. */
    val received = CopyOnWriteArrayList<Pair<Int, Int>>()

    /** Records [photo]'s size, then throws [failure] or returns the blurred copy. */
    override fun smooth(photo: Bitmap, face: Rect): Bitmap {
        received += photo.width to photo.height
        failure?.let { throw it }
        val width = photo.width
        val height = photo.height
        val source = IntArray(width * height)
        photo.getPixels(source, 0, width, 0, 0, width, height)
        val out = source.copyOf()
        for (y in face.top until face.bottom) {
            for (x in face.left until face.right) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dy in -RADIUS..RADIUS) {
                    for (dx in -RADIUS..RADIUS) {
                        // Neighbours outside the face are skipped, so only face pixels are mixed.
                        val nx = x + dx
                        val ny = y + dy
                        if (nx < face.left || nx >= face.right || ny < face.top || ny >= face.bottom) continue
                        val pixel = source[ny * width + nx]
                        r += (pixel shr 16) and 0xFF
                        g += (pixel shr 8) and 0xFF
                        b += pixel and 0xFF
                        count++
                    }
                }
                out[y * width + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(out, 0, width, 0, 0, width, height)
        return bitmap
    }

    /** The blur's fixed size. */
    private companion object {
        /** How many pixels on each side of a pixel the blur reaches: 2 makes a 5 x 5 box. */
        const val RADIUS = 2
    }
}
