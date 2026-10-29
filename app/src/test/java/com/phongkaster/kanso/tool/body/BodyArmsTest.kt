package com.phongkaster.kanso.tool.body

import com.phongkaster.kanso.data.edit.body.BodyArms
import com.phongkaster.kanso.data.edit.body.BodyMask
import com.phongkaster.kanso.data.edit.face.WarpPush
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [BodyArms] (and through it `BodyLimb`) on a made-up upright figure whose arms hang straight down
 * ([SampleBody]): which way each arm's edges are pushed, where they come from, and that an arm out
 * of the photo is left alone. Plain maths, so it runs on the JVM without Robolectric.
 */
class BodyArmsTest {

    /** The figure every test reshapes: arms hanging on the lines x = 350 and x = 650. */
    private val body = SampleBody.upright(centerX = 500f, top = 200f, length = 400f, shoulderSpread = 300f, hipSpread = 200f)

    /** Arms 0 (and NaN) gives no pushes, so the photo is not touched. */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(BodyArms.pushesFor(body, 0f).isEmpty())
        assertTrue(BodyArms.pushesFor(body, Float.NaN).isEmpty())
    }

    /**
     * A positive amount pulls both edges of every arm toward that arm's line (slimmer): 2 arms ×
     * (upper arm + forearm) × 3 levels × 2 edges, and nothing moves along a vertical arm.
     */
    @Test
    fun positive_pulls_every_arm_edge_in() {
        val pushes = BodyArms.pushesFor(body, 1f)

        assertEquals(24, pushes.size)
        pushes.forEach(
            action = { push ->
                val line = armLineOf(push)
                val inward = if (push.centerX < line) push.moveX > 0f else push.moveX < 0f
                assertTrue("edge at ${push.centerX} of the arm at $line moved by ${push.moveX}", inward)
                assertEquals(0f, push.moveY, 0.001f)
            },
        )
    }

    /** A negative amount pushes every arm edge away from its line (thicker). */
    @Test
    fun negative_pushes_every_arm_edge_out() {
        BodyArms.pushesFor(body, -1f).forEach(
            action = { push ->
                val line = armLineOf(push)
                val outward = if (push.centerX < line) push.moveX < 0f else push.moveX > 0f
                assertTrue("edge at ${push.centerX} of the arm at $line moved by ${push.moveX}", outward)
            },
        )
    }

    /**
     * With a mask showing the arm on x = 650 as a band 40 px wide (630..670), that arm's pushes sit
     * on the band's edges: the first background pixel, 21 px each side of the line.
     */
    @Test
    fun edges_come_from_the_mask() {
        val values = FloatArray(1000 * 1000, init = { i -> if (i % 1000 in 630..670) 1f else 0f })
        val masked = body.copy(mask = BodyMask(1000, 1000, values, 1000, 1000))

        BodyArms.pushesFor(masked, 1f)
            .filter(predicate = { armLineOf(it) == 650f })
            .forEach(action = { push -> assertEquals("edge at ${push.centerX}", 21f, abs(push.centerX - 650f), 1.5f) })
    }

    /** Arms hardly visible (out of the photo or behind the body) get no pushes. */
    @Test
    fun hidden_arms_give_no_pushes() {
        val hidden = SampleBody.upright(
            centerX = 500f, top = 200f, length = 400f, shoulderSpread = 300f, hipSpread = 200f, visibility = 0.2f,
        )

        assertTrue(BodyArms.pushesFor(hidden, 1f).isEmpty())
    }

    /** A value beyond ±1 is clamped, so 5 moves exactly as far as 1. */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(BodyArms.pushesFor(body, 1f), BodyArms.pushesFor(body, 5f))
        assertEquals(BodyArms.pushesFor(body, -1f), BodyArms.pushesFor(body, -5f))
    }

    /** The same figure twice as large is pushed twice as far, with twice the reach. */
    @Test
    fun the_moves_follow_the_body_size() {
        val large = SampleBody.upright(centerX = 1000f, top = 400f, length = 800f, shoulderSpread = 600f, hipSpread = 400f)
        val small = BodyArms.pushesFor(body, 0.5f)
        val big = BodyArms.pushesFor(large, 0.5f)

        assertEquals(small.size, big.size)
        small.zip(big).forEach(
            action = { (one, two) ->
                assertEquals(one.moveX * 2, two.moveX, 0.01f)
                assertEquals(one.radius * 2, two.radius, 0.01f)
            },
        )
    }

    /** The line (x = 350 or 650) of the arm [push] belongs to: the nearer one. */
    private fun armLineOf(push: WarpPush): Float = if (abs(push.centerX - 350f) < abs(push.centerX - 650f)) 350f else 650f
}
