package com.phongkaster.kanso.tool.body

import com.phongkaster.kanso.data.edit.body.BodyWaist
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [BodyWaist] on a made-up upright figure ([SampleBody]): which way each waist edge is pushed,
 * where the edges are taken from (the mask, or a guess without one), and that a torso out of the
 * photo is left alone. Plain maths, so it runs on the JVM without Robolectric.
 */
class BodyWaistTest {

    /** The figure every test reshapes: centre line x = 500, shoulders at y = 200, hips at y = 600. */
    private val body = SampleBody.upright(centerX = 500f, top = 200f, length = 400f, shoulderSpread = 300f, hipSpread = 200f)

    /** Waist 0 (and NaN) gives no pushes, so the photo is not touched. */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(BodyWaist.pushesFor(body, 0f).isEmpty())
        assertTrue(BodyWaist.pushesFor(body, Float.NaN).isEmpty())
    }

    /**
     * A positive waist pulls both edges toward the centre line (slimmer): an edge left of it moves
     * right, one right of it moves left, and nothing moves up or down on an upright torso.
     */
    @Test
    fun positive_pulls_both_edges_in() {
        val pushes = BodyWaist.pushesFor(body, 1f)

        assertEquals(8, pushes.size)
        pushes.forEach(
            action = { push ->
                val inward = if (push.centerX < 500f) push.moveX > 0f else push.moveX < 0f
                assertTrue("edge at ${push.centerX} moved by ${push.moveX}", inward)
                assertEquals(0f, push.moveY, 0.001f)
            },
        )
    }

    /** A negative waist pushes both edges away from the centre line (wider). */
    @Test
    fun negative_pushes_both_edges_out() {
        val pushes = BodyWaist.pushesFor(body, -1f)

        pushes.forEach(
            action = { push ->
                val outward = if (push.centerX < 500f) push.moveX < 0f else push.moveX > 0f
                assertTrue("edge at ${push.centerX} moved by ${push.moveX}", outward)
            },
        )
    }

    /** With a mask, the pushes sit on the mask's edges: the first background pixel, 101 px each side of the centre line. */
    @Test
    fun edges_come_from_the_mask() {
        val masked = SampleBody.upright(
            centerX = 500f, top = 200f, length = 400f, shoulderSpread = 300f, hipSpread = 200f,
            torsoHalf = 100f, photoWidth = 1000, photoHeight = 1000,
        )

        BodyWaist.pushesFor(masked, 1f).forEach(
            action = { push -> assertEquals("edge at ${push.centerX}", 101f, abs(push.centerX - 500f), 1.5f) },
        )
    }

    /** Without a mask the edges are guessed from the joints, the same distance on both sides. */
    @Test
    fun without_a_mask_the_edges_are_guessed_evenly() {
        val pushes = BodyWaist.pushesFor(body, 1f)
        val left = pushes.filter(predicate = { it.centerX < 500f }).map(transform = { 500f - it.centerX })
        val right = pushes.filter(predicate = { it.centerX > 500f }).map(transform = { it.centerX - 500f })

        assertEquals(left.size, right.size)
        left.zip(right).forEach(action = { (one, two) -> assertEquals(one, two, 0.01f) })
    }

    /** A torso hardly visible (a portrait cut above the hips) gets no pushes. */
    @Test
    fun a_hidden_torso_gives_no_pushes() {
        val hidden = SampleBody.upright(
            centerX = 500f, top = 200f, length = 400f, shoulderSpread = 300f, hipSpread = 200f, visibility = 0.2f,
        )

        assertTrue(BodyWaist.pushesFor(hidden, 1f).isEmpty())
    }

    /** A value beyond ±1 is clamped, so 5 moves exactly as far as 1. */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(BodyWaist.pushesFor(body, 1f), BodyWaist.pushesFor(body, 5f))
        assertEquals(BodyWaist.pushesFor(body, -1f), BodyWaist.pushesFor(body, -5f))
    }

    /** The same figure twice as large is pushed twice as far, with twice the reach. */
    @Test
    fun the_moves_follow_the_body_size() {
        val large = SampleBody.upright(centerX = 1000f, top = 400f, length = 800f, shoulderSpread = 600f, hipSpread = 400f)
        val small = BodyWaist.pushesFor(body, 0.5f)
        val big = BodyWaist.pushesFor(large, 0.5f)

        small.zip(big).forEach(
            action = { (one, two) ->
                assertEquals(one.moveX * 2, two.moveX, 0.01f)
                assertEquals(one.radius * 2, two.radius, 0.01f)
            },
        )
    }
}
