package com.phongkaster.kanso.data.edit.face

import android.graphics.Bitmap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Moves pixels by a list of [WarpPush]es: the engine behind every reshape (face width now; slimmer
 * waist or longer legs later, with points from a body model).
 *
 * **Backward mapping.** For each pixel of the *result*, it works out which pixel of the *input*
 * should land there, and copies it. Going this way round never leaves holes: every result pixel
 * gets a colour. Going forward (moving each input pixel to its new place) would leave gaps where
 * pixels spread apart.
 *
 * **One push.** Inside a push's circle, a result pixel at distance `dist` from its centre takes
 * the input pixel at `result − f² × move`, where
 * `f = (radius² − dist²) / (radius² − dist² + |move|²)` (Gustafsson, "Interactive Image Warping",
 * 1993). `f` is near 1 at the centre and falls smoothly to 0 at the circle's edge, so the warp has
 * no visible seam and nothing outside the circle moves.
 *
 * **Several pushes.** Neighbouring pushes overlap, and adding their moves together would move a
 * pixel between them further than either asks. So where the weights `f²` add up to more than 1,
 * the summed move is divided by that total: the pixel moves like a weighted average of its
 * neighbours' pushes.
 *
 * **Sampling.** The input position is usually between pixels, so the colour is mixed from the four
 * pixels around it (bilinear sampling), which keeps edges smooth.
 *
 * It is plain Kotlin on an `IntArray` of pixels, with no native code, so it runs on any thread and
 * in a JVM test. Only the box around the pushes is visited, which keeps it quick on a phone.
 */
object LocalWarp {

    /**
     * Returns a new bitmap: [photo] with every push in [pushes] applied at once. [photo] is not
     * changed. With no pushes the result is an exact copy.
     */
    fun warp(photo: Bitmap, pushes: List<WarpPush>): Bitmap {
        val out = photo.copy(Bitmap.Config.ARGB_8888, true)
        if (pushes.isEmpty()) return out
        val width = photo.width
        val height = photo.height
        val source = IntArray(width * height)
        out.getPixels(source, 0, width, 0, 0, width, height)
        val result = source.copyOf()
        // The smallest box holding every push's circle, cut to the photo.
        val left = max(0, floor(pushes.minOf(selector = { it.centerX - it.radius })).toInt())
        val top = max(0, floor(pushes.minOf(selector = { it.centerY - it.radius })).toInt())
        val right = min(width, ceil(pushes.maxOf(selector = { it.centerX + it.radius })).toInt() + 1)
        val bottom = min(height, ceil(pushes.maxOf(selector = { it.centerY + it.radius })).toInt() + 1)
        val move = FloatArray(2)
        for (y in top until bottom) {
            for (x in left until right) {
                // No push reaches this pixel: it keeps its own colour.
                if (!moveAt(x.toFloat(), y.toFloat(), pushes, move)) continue
                result[y * width + x] = sample(source, width, height, x - move[0], y - move[1])
            }
        }
        out.setPixels(result, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * How far the point ([x], [y]) moves under [pushes], written into [out] (`out[0]` across,
     * `out[1]` down): every push's move weighted by `f²`, divided by the total weight when it is
     * above 1 (see the class comment). Returns false, with [out] at 0, when no push reaches the
     * point. [warp] uses it for every pixel and [WarpMesh] for every point of its grid, so the
     * live picture and the saved one move by the same rule.
     */
    fun moveAt(x: Float, y: Float, pushes: List<WarpPush>, out: FloatArray): Boolean {
        var moveX = 0f
        var moveY = 0f
        var weight = 0f
        for (push in pushes) {
            val offsetX = x - push.centerX
            val offsetY = y - push.centerY
            val radiusSquared = push.radius * push.radius
            val distanceSquared = offsetX * offsetX + offsetY * offsetY
            if (distanceSquared >= radiusSquared) continue
            val inside = radiusSquared - distanceSquared
            val f = inside / (inside + push.moveX * push.moveX + push.moveY * push.moveY)
            val k = f * f
            moveX += k * push.moveX
            moveY += k * push.moveY
            weight += k
        }
        if (weight > 1f) {
            moveX /= weight
            moveY /= weight
        }
        out[0] = moveX
        out[1] = moveY
        return weight > 0f
    }

    /**
     * Returns the colour at ([x], [y]) in the [width] x [height] [pixels], mixed from the four
     * pixels around that point (bilinear sampling). A point outside the picture takes the nearest
     * edge pixel. Each channel (alpha, red, green, blue) is mixed on its own.
     */
    private fun sample(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
        val px = x.coerceIn(0f, (width - 1).toFloat())
        val py = y.coerceIn(0f, (height - 1).toFloat())
        val x0 = px.toInt()
        val y0 = py.toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)
        // How far the point sits between the left and right pixels, and the top and bottom ones.
        val ax = px - x0
        val ay = py - y0
        val topLeft = pixels[y0 * width + x0]
        val topRight = pixels[y0 * width + x1]
        val bottomLeft = pixels[y1 * width + x0]
        val bottomRight = pixels[y1 * width + x1]
        var colour = 0
        for (shift in CHANNEL_SHIFTS) {
            val upper = channel(topLeft, shift) + ax * (channel(topRight, shift) - channel(topLeft, shift))
            val lower = channel(bottomLeft, shift) + ax * (channel(bottomRight, shift) - channel(bottomLeft, shift))
            val mixed = (upper + ay * (lower - upper) + ROUND_HALF).toInt().coerceIn(0, CHANNEL_MAX)
            colour = colour or (mixed shl shift)
        }
        return colour
    }

    /** One channel (0..255) of [colour], the one that starts [shift] bits up. */
    private fun channel(colour: Int, shift: Int): Float = ((colour ushr shift) and CHANNEL_MAX).toFloat()

    /** Where alpha, red, green and blue sit in an ARGB pixel, in bits from the bottom. */
    private val CHANNEL_SHIFTS = intArrayOf(24, 16, 8, 0)

    /** The largest value of one channel, also the mask that reads one from a pixel. */
    private const val CHANNEL_MAX = 0xFF

    /** Added before cutting off the fraction, so a mixed channel rounds to the nearest value. */
    private const val ROUND_HALF = 0.5f
}
