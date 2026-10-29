package com.phongkaster.kanso.tool.face

import com.phongkaster.kanso.data.edit.face.FaceLift
import com.phongkaster.kanso.data.edit.face.FaceMeshIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FaceLift] on a made-up upright oval face ([SampleFace]): which way each outline point is
 * pushed, which points stay, and that the answer follows the face's size. Plain maths, so it runs
 * on the JVM without Robolectric.
 */
class FaceLiftTest {

    /** The face every test lifts: centred at (500, 500), 200 to each cheek, 260 to the chin. */
    private val face = SampleFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /** Lift 0 (and NaN) gives no pushes, so the photo is not touched. */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(FaceLift.pushesFor(face, 0f).isEmpty())
        assertTrue(FaceLift.pushesFor(face, Float.NaN).isEmpty())
    }

    /**
     * A negative lift is the same pushes turned round: the jaw drops (pushed down the photo) and
     * spreads (pushed away from the centre line), as Meitu's Lift slider does left of 0.
     */
    @Test
    fun negative_drops_the_jaw() {
        val lifted = FaceLift.pushesFor(face, 0.7f)
        val dropped = FaceLift.pushesFor(face, -0.7f)
        val moving = FaceMeshIndex.LEFT_OUTLINE.size - 1

        assertEquals(lifted.size, dropped.size)
        dropped.forEach(action = { assertTrue("moved by ${it.moveY} down", it.moveY > 0f) })
        dropped.take(moving).forEach(action = { assertTrue("left side moved by ${it.moveX}", it.moveX < 0f) })
        lifted.zip(dropped).forEach(
            action = { (up, down) ->
                assertEquals(-up.moveX, down.moveX, 0.001f)
                assertEquals(-up.moveY, down.moveY, 0.001f)
            },
        )
    }

    /**
     * The cheek at eye height gets no push (the eyes stay put); every other outline point, on both
     * sides, is pushed up the face (toward the eyes, so up the photo on an upright face).
     */
    @Test
    fun the_lower_outline_rises_and_the_cheek_top_stays() {
        val pushes = FaceLift.pushesFor(face, 1f)
        val moving = FaceMeshIndex.LEFT_OUTLINE.size - 1
        val centres = pushes.map(transform = { it.centerX to it.centerY })
        val leftTop = face[FaceMeshIndex.LEFT_OUTLINE.first()]
        val rightTop = face[FaceMeshIndex.RIGHT_OUTLINE.first()]

        assertEquals(moving * 2, pushes.size)
        assertTrue((leftTop.x to leftTop.y) !in centres)
        assertTrue((rightTop.x to rightTop.y) !in centres)
        pushes.forEach(action = { assertTrue("moved by ${it.moveY} down", it.moveY < 0f) })
    }

    /** Both sides are drawn in toward the centre line: the left side moves right, the right side left. */
    @Test
    fun the_jaw_is_drawn_inward() {
        val pushes = FaceLift.pushesFor(face, 1f)
        val moving = FaceMeshIndex.LEFT_OUTLINE.size - 1

        pushes.take(moving).forEach(action = { assertTrue("left side moved by ${it.moveX}", it.moveX > 0f) })
        pushes.drop(moving).forEach(action = { assertTrue("right side moved by ${it.moveX}", it.moveX < 0f) })
    }

    /** The two sides mirror each other, since the oval is symmetric. */
    @Test
    fun both_sides_move_the_same_amount() {
        val pushes = FaceLift.pushesFor(face, 0.6f)
        val moving = FaceMeshIndex.LEFT_OUTLINE.size - 1

        for (position in 0 until moving) {
            assertEquals(-pushes[position].moveX, pushes[position + moving].moveX, 0.001f)
            assertEquals(pushes[position].moveY, pushes[position + moving].moveY, 0.001f)
        }
    }

    /** A value beyond ±1 is clamped, so 5 lifts exactly as far as 1, and -5 drops as far as -1. */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(FaceLift.pushesFor(face, 1f), FaceLift.pushesFor(face, 5f))
        assertEquals(FaceLift.pushesFor(face, -1f), FaceLift.pushesFor(face, -5f))
    }

    /** The same face twice as large is lifted twice as far, with twice the reach. */
    @Test
    fun the_moves_follow_the_face_size() {
        val large = SampleFace.oval(centerX = 1000f, centerY = 1000f, halfWidth = 400f, halfHeight = 520f)
        val small = FaceLift.pushesFor(face, 0.5f)
        val big = FaceLift.pushesFor(large, 0.5f)

        small.zip(big).forEach(
            action = { (one, two) ->
                assertEquals(one.moveX * 2, two.moveX, 0.01f)
                assertEquals(one.moveY * 2, two.moveY, 0.01f)
                assertEquals(one.radius * 2, two.radius, 0.01f)
            },
        )
    }
}
