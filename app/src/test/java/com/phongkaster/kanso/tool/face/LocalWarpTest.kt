package com.phongkaster.kanso.tool.face

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.face.LocalWarp
import com.phongkaster.kanso.data.edit.face.WarpPush
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
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
