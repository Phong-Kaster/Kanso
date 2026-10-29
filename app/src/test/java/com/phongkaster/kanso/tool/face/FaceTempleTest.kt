package com.phongkaster.kanso.tool.face

import com.phongkaster.kanso.data.edit.face.FaceMeshIndex
import com.phongkaster.kanso.data.edit.face.FaceTemple
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FaceTemple] on a made-up upright oval face ([SampleFace]): which way each temple point is pushed,
 * that both sides mirror, and that the answer follows the face's size. Plain maths, so it runs on
 * the JVM without Robolectric.
 */
class FaceTempleTest {

    /** The face every test reshapes: centred at (500, 500), 200 to each cheek, 260 to the chin. */
    private val face = SampleFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /** Temple 0 (and NaN) gives no pushes, so the photo is not touched. */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(FaceTemple.pushesFor(face, 0f).isEmpty())
        assertTrue(FaceTemple.pushesFor(face, Float.NaN).isEmpty())
    }

    /**
     * A positive temple pushes each temple point away from the centre line (fuller): the left side
     * moves left, the right side right, one push per temple point, above the eyes.
     */
    @Test
    fun positive_fills_the_temples_outward() {
        val pushes = FaceTemple.pushesFor(face, 1f)
        val side = FaceMeshIndex.LEFT_TEMPLE.size

        assertEquals(side * 2, pushes.size)
        pushes.take(side).forEach(action = { assertTrue("left temple moved by ${it.moveX}", it.moveX < 0f) })
        pushes.drop(side).forEach(action = { assertTrue("right temple moved by ${it.moveX}", it.moveX > 0f) })
        pushes.forEach(action = { assertTrue("temple at y ${it.centerY} is not above the eyes", it.centerY < 500f - 0.4f * 260f) })
    }

    /** A negative temple pulls the temples toward the centre line (hollower). */
    @Test
    fun negative_hollows_the_temples_inward() {
        val pushes = FaceTemple.pushesFor(face, -1f)
        val side = FaceMeshIndex.LEFT_TEMPLE.size

        pushes.take(side).forEach(action = { assertTrue("left temple moved by ${it.moveX}", it.moveX > 0f) })
        pushes.drop(side).forEach(action = { assertTrue("right temple moved by ${it.moveX}", it.moveX < 0f) })
    }

    /** The two sides mirror each other, since the oval is symmetric. */
    @Test
    fun both_sides_move_the_same_amount() {
        val pushes = FaceTemple.pushesFor(face, 0.6f)
        val side = FaceMeshIndex.LEFT_TEMPLE.size

        for (position in 0 until side) {
            assertEquals(-pushes[position].moveX, pushes[position + side].moveX, 0.001f)
        }
    }

    /** A value beyond ±1 is clamped, so 5 moves exactly as far as 1. */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(FaceTemple.pushesFor(face, 1f), FaceTemple.pushesFor(face, 5f))
        assertEquals(FaceTemple.pushesFor(face, -1f), FaceTemple.pushesFor(face, -5f))
    }

    /** The same face twice as large is pushed twice as far, with twice the reach. */
    @Test
    fun the_moves_follow_the_face_size() {
        val large = SampleFace.oval(centerX = 1000f, centerY = 1000f, halfWidth = 400f, halfHeight = 520f)

        FaceTemple.pushesFor(face, 0.5f).zip(FaceTemple.pushesFor(large, 0.5f)).forEach(
            action = { (one, two) ->
                assertEquals(one.moveX * 2, two.moveX, 0.01f)
                assertEquals(one.radius * 2, two.radius, 0.01f)
            },
        )
    }
}
