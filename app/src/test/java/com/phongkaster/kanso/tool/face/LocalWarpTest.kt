package com.phongkaster.kanso.tool.face

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.face.LocalWarp
import com.phongkaster.kanso.data.edit.face.WarpPush
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [LocalWarp] on a 100 x 100 picture whose left half is black and right half white, so a push
 * shows up as the edge between them moving. Pixels are read straight from the bitmaps.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class LocalWarpTest {

    /** No pushes gives a new bitmap equal to the input, pixel for pixel. */
    @Test
    fun no_pushes_gives_an_exact_copy() {
        val photo = halves()
        val result = LocalWarp.warp(photo, emptyList())

        assertNotSame(photo, result)
        assertArrayEquals(pixels(photo), pixels(result))
    }

    /** A push to the right drags the black half over the edge, at the push's centre. */
    @Test
    fun a_push_moves_the_edge() {
        val photo = halves()
        val result = LocalWarp.warp(photo, listOf(WarpPush(centerX = 50f, centerY = 50f, moveX = 10f, moveY = 0f, radius = 30f)))

        // White before; at the centre the picture moves about 8 pixels, so black reaches x = 55.
        assertEquals(Color.WHITE, photo.getPixel(55, 50))
        assertEquals(Color.BLACK, result.getPixel(55, 50))
    }

    /** Every pixel outside the push's circle keeps its colour, and the input is not changed. */
    @Test
    fun outside_the_circle_nothing_moves() {
        val photo = halves()
        val before = pixels(photo)
        val result = LocalWarp.warp(photo, listOf(WarpPush(centerX = 50f, centerY = 50f, moveX = 10f, moveY = 0f, radius = 30f)))
        val after = pixels(result)

        assertArrayEquals(before, pixels(photo))
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val dx = x - 50
                val dy = y - 50
                if (dx * dx + dy * dy < 30 * 30) continue
                assertEquals("pixel ($x, $y) outside the push changed", before[y * SIZE + x], after[y * SIZE + x])
            }
        }
    }

    /**
     * A growing push (Chest) makes a black dot at its centre bigger, and a negative one smaller,
     * while every pixel outside its circle keeps its colour.
     */
    @Test
    fun a_growing_push_makes_the_middle_bigger_or_smaller() {
        val photo = dot()
        val before = blackCount(photo)
        val bigger = LocalWarp.warp(photo, listOf(WarpPush(centerX = 50f, centerY = 50f, moveX = 0f, moveY = 0f, radius = 30f, grow = 0.3f)))
        val smaller = LocalWarp.warp(photo, listOf(WarpPush(centerX = 50f, centerY = 50f, moveX = 0f, moveY = 0f, radius = 30f, grow = -0.3f)))

        assertTrue("black went from $before to ${blackCount(bigger)}", blackCount(bigger) > before)
        assertTrue("black went from $before to ${blackCount(smaller)}", blackCount(smaller) < before)
        assertEquals(photo.getPixel(5, 5), bigger.getPixel(5, 5))
        assertEquals(photo.getPixel(95, 95), smaller.getPixel(95, 95))
    }

    /**
     * Under a growing push the centre itself does not move, a point to its right moves right
     * (outward) for a positive grow, and nothing moves at the circle's edge.
     */
    @Test
    fun a_growing_push_spreads_out_from_its_centre() {
        val push = listOf(WarpPush(centerX = 50f, centerY = 50f, moveX = 0f, moveY = 0f, radius = 30f, grow = 0.3f))
        val move = FloatArray(2)

        LocalWarp.moveAt(50f, 50f, push, move)
        assertEquals(0f, move[0], 0.0001f)
        LocalWarp.moveAt(60f, 50f, push, move)
        assertTrue("moved by ${move[0]}", move[0] > 0f)
        assertEquals(0f, move[1], 0.0001f)
        assertFalse(LocalWarp.moveAt(80f, 50f, push, move))
    }

    /** A 100 x 100 opaque white picture with a black dot of radius 10 at its centre. */
    private fun dot(): Bitmap {
        val data = IntArray(SIZE * SIZE, init = { i ->
            val dx = i % SIZE - 50
            val dy = i / SIZE - 50
            if (dx * dx + dy * dy <= 100) Color.BLACK else Color.WHITE
        })
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(data, 0, SIZE, 0, 0, SIZE, SIZE)
        return bitmap
    }

    /** How many pixels of [bitmap] are darker than mid grey. */
    private fun blackCount(bitmap: Bitmap): Int = pixels(bitmap).count(predicate = { (it and 0xFF) < 128 })

    /**
     * A push whose move is small next to its circle (every Kanso reshape: Temple moves about 24 px
     * in a 120 px circle) never folds the picture over itself. Walking out from the centre along
     * the move, the point each pixel reads from (`x − move(x)`) must keep moving outward, never
     * back, and the push's own point moves exactly by its move. Gustafsson's falloff, used before,
     * dropped from about 0.5 to 0 in the circle's last 2.4 px, folded there and left a seam.
     */
    @Test
    fun a_small_move_in_a_large_circle_never_folds() {
        val push = listOf(WarpPush(centerX = 200f, centerY = 200f, moveX = 24f, moveY = 0f, radius = 120f))
        val move = FloatArray(2)

        LocalWarp.moveAt(200f, 200f, push, move)
        assertEquals(24f, move[0], 0.0001f)
        var previous = Float.NEGATIVE_INFINITY
        var x = 80f
        while (x <= 320f) {
            LocalWarp.moveAt(x, 200f, push, move)
            val readsFrom = x - move[0]
            assertTrue("at x $x the picture folds: reads from $readsFrom after $previous", readsFrom > previous)
            previous = readsFrom
            x += 0.25f
        }
    }

    /** A 100 x 100 opaque picture, black on the left half (x < 50) and white on the right. */
    private fun halves(): Bitmap {
        val data = IntArray(SIZE * SIZE, init = { if (it % SIZE < SIZE / 2) Color.BLACK else Color.WHITE })
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(data, 0, SIZE, 0, 0, SIZE, SIZE)
        return bitmap
    }

    /** Every pixel of [bitmap], row after row. */
    private fun pixels(bitmap: Bitmap): IntArray {
        val data = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return data
    }

    /** The picture's fixed numbers. */
    private companion object {
        /** The picture's width and height, in pixels. */
        const val SIZE = 100
    }
}
