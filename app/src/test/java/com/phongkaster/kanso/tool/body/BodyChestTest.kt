package com.phongkaster.kanso.tool.body

import com.phongkaster.kanso.data.edit.body.BodyChest
import com.phongkaster.kanso.data.edit.body.BodyLandmarks
import com.phongkaster.kanso.data.edit.body.PoseIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [BodyChest] on a made-up upright figure ([SampleBody]): two growing pushes below the shoulders,
 * one each side of the centre line, which way they grow, and that the chest is still found with
 * the hips (and even the nose) out of the photo. Plain maths, so it runs on the JVM without
 * Robolectric.
 */
class BodyChestTest {

    /** The figure every test reshapes: centre line x = 500, shoulders 300 apart at y = 200. */
    private val body = SampleBody.upright(centerX = 500f, top = 200f, length = 400f, shoulderSpread = 300f, hipSpread = 200f)

    /** Chest 0 (and NaN) gives no pushes, so the photo is not touched. */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(BodyChest.pushesFor(body, 0f).isEmpty())
        assertTrue(BodyChest.pushesFor(body, Float.NaN).isEmpty())
    }

    /**
     * A positive chest gives two growing pushes that do not slide: one each side of the centre
     * line, mirrored, below the shoulders and inside them.
     */
    @Test
    fun positive_grows_both_sides_below_the_shoulders() {
        val (one, two) = BodyChest.pushesFor(body, 1f)

        listOf(one, two).forEach(
            action = { push ->
                assertTrue("grow ${push.grow}", push.grow > 0f)
                assertEquals(0f, push.moveX, 0f)
                assertEquals(0f, push.moveY, 0f)
                assertTrue("centre at y ${push.centerY} is not below the shoulders", push.centerY > 200f)
                assertTrue("centre at x ${push.centerX} is not inside the shoulders", push.centerX in 350f..650f)
            },
        )
        assertEquals(500f - one.centerX, two.centerX - 500f, 0.01f)
        assertEquals(one.centerY, two.centerY, 0.01f)
    }

    /** A negative chest makes the same two pushes shrink instead. */
    @Test
    fun negative_shrinks() {
        val grown = BodyChest.pushesFor(body, 0.6f)
        val shrunk = BodyChest.pushesFor(body, -0.6f)

        grown.zip(shrunk).forEach(
            action = { (up, down) ->
                assertEquals(-up.grow, down.grow, 0.0001f)
                assertEquals(up.centerX, down.centerX, 0.0001f)
                assertEquals(up.centerY, down.centerY, 0.0001f)
            },
        )
    }

    /**
     * A portrait cut at the chest (hips out of the photo) still gets its chest below the
     * shoulders, using the nose for "up"; with the nose hidden too, "down" is down the photo.
     */
    @Test
    fun the_chest_is_found_without_the_hips_or_the_nose() {
        val noHips = hide(body, PoseIndex.LEFT_HIP, PoseIndex.RIGHT_HIP)
        val noHipsOrNose = hide(noHips, PoseIndex.NOSE)

        assertEquals(BodyChest.pushesFor(body, 1f), BodyChest.pushesFor(noHips, 1f))
        assertEquals(BodyChest.pushesFor(body, 1f), BodyChest.pushesFor(noHipsOrNose, 1f))
    }

    /** Hidden shoulders give no pushes. */
    @Test
    fun hidden_shoulders_give_no_pushes() {
        assertTrue(BodyChest.pushesFor(hide(body, PoseIndex.LEFT_SHOULDER), 1f).isEmpty())
    }

    /** A value beyond ±1 is clamped, so 5 grows exactly as much as 1. */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(BodyChest.pushesFor(body, 1f), BodyChest.pushesFor(body, 5f))
        assertEquals(BodyChest.pushesFor(body, -1f), BodyChest.pushesFor(body, -5f))
    }

    /** The same figure twice as large gets pushes twice as far out and twice as wide. */
    @Test
    fun the_pushes_follow_the_body_size() {
        val large = SampleBody.upright(centerX = 1000f, top = 400f, length = 800f, shoulderSpread = 600f, hipSpread = 400f)

        BodyChest.pushesFor(body, 0.5f).zip(BodyChest.pushesFor(large, 0.5f)).forEach(
            action = { (one, two) ->
                assertEquals(one.centerX * 2, two.centerX, 0.01f)
                assertEquals(one.centerY * 2, two.centerY, 0.01f)
                assertEquals(one.radius * 2, two.radius, 0.01f)
                assertEquals(one.grow, two.grow, 0.0001f)
            },
        )
    }

    /** [body] with the landmarks at [indices] made invisible (visibility 0). */
    private fun hide(body: BodyLandmarks, vararg indices: Int): BodyLandmarks =
        body.copy(points = body.points.mapIndexed(transform = { i, point -> if (i in indices) point.copy(visibility = 0f) else point }))
}
