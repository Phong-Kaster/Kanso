package com.phongkaster.kanso.tool.face

import com.phongkaster.kanso.data.edit.face.FaceMeshIndex
import com.phongkaster.kanso.data.edit.face.FaceWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FaceWidth] on a made-up upright oval face ([SampleFace]): which way each outline point is
 * pushed, how far, and that the answer does not depend on the photo's size. Plain maths, so it
 * runs on the JVM without Robolectric.
 */
class FaceWidthTest {

    /** The face every test reshapes: centred at (500, 500), 200 to each cheek, 260 to the chin. */
    private val face = SampleFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /** Width 0 (and NaN) gives no pushes, so the photo is not touched. */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(FaceWidth.pushesFor(face, 0f).isEmpty())
        assertTrue(FaceWidth.pushesFor(face, Float.NaN).isEmpty())
    }

    /** A positive width pulls both sides toward the centre line: the face gets narrower. */
    @Test
    fun positive_pulls_the_outline_inward() {
        val pushes = FaceWidth.pushesFor(face, 1f)
        val outline = FaceMeshIndex.LEFT_OUTLINE.size

        assertEquals(outline * 2, pushes.size)
        pushes.take(outline).forEach(action = { assertTrue("left side moved by ${it.moveX}", it.moveX > 0f) })
        pushes.drop(outline).forEach(action = { assertTrue("right side moved by ${it.moveX}", it.moveX < 0f) })
    }

    /** A negative width pushes both sides away from the centre line: the face gets wider. */
    @Test
    fun negative_pushes_the_outline_outward() {
        val pushes = FaceWidth.pushesFor(face, -1f)
        val outline = FaceMeshIndex.LEFT_OUTLINE.size

        pushes.take(outline).forEach(action = { assertTrue("left side moved by ${it.moveX}", it.moveX < 0f) })
        pushes.drop(outline).forEach(action = { assertTrue("right side moved by ${it.moveX}", it.moveX > 0f) })
    }

    /** The two sides mirror each other, since the oval is symmetric. */
    @Test
    fun both_sides_move_the_same_amount() {
        val pushes = FaceWidth.pushesFor(face, 0.6f)
        val outline = FaceMeshIndex.LEFT_OUTLINE.size

        for (position in 0 until outline) {
            assertEquals(-pushes[position].moveX, pushes[position + outline].moveX, 0.001f)
        }
    }

    /** A value beyond ±1 is clamped, so 5 moves exactly as far as 1. */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(FaceWidth.pushesFor(face, 1f), FaceWidth.pushesFor(face, 5f))
    }

    /** The same face twice as large is pushed twice as far, with twice the reach. */
    @Test
    fun the_moves_follow_the_face_size() {
        val large = SampleFace.oval(centerX = 1000f, centerY = 1000f, halfWidth = 400f, halfHeight = 520f)
        val small = FaceWidth.pushesFor(face, 0.5f)
        val big = FaceWidth.pushesFor(large, 0.5f)

        small.zip(big).forEach(
            action = { (one, two) ->
                assertEquals(one.moveX * 2, two.moveX, 0.01f)
                assertEquals(one.radius * 2, two.radius, 0.01f)
            },
        )
    }
}
