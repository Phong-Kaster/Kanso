package com.phongkaster.kanso.tool.body

import com.phongkaster.kanso.data.edit.body.BodyLandmarks
import com.phongkaster.kanso.data.edit.body.BodyLegs
import com.phongkaster.kanso.data.edit.body.PoseIndex
import com.phongkaster.kanso.data.edit.face.WarpPush
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [BodyLegs] (and through it `BodyLimb`) on a made-up upright figure whose legs stand straight
 * down from the hips ([SampleBody]): which way each leg's edges are pushed, that a thigh is still
 * slimmed with its knee out of the photo while its calf is not, and that hidden hips leave the
 * legs alone. Plain maths, so it runs on the JVM without Robolectric.
 */
class BodyLegsTest {

    /** The figure every test reshapes: legs on the lines x = 400 and x = 600, hips at y = 600. */
    private val body = SampleBody.upright(centerX = 500f, top = 200f, length = 400f, shoulderSpread = 300f, hipSpread = 200f)

    /** Legs 0 (and NaN) gives no pushes, so the photo is not touched. */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(BodyLegs.pushesFor(body, 0f).isEmpty())
        assertTrue(BodyLegs.pushesFor(body, Float.NaN).isEmpty())
    }

    /**
     * A positive amount pulls both edges of every leg toward that leg's line (slimmer): 2 legs ×
     * (thigh + calf) × 3 levels × 2 edges, all below the hips, nothing moving along a straight leg.
     */
    @Test
    fun positive_pulls_every_leg_edge_in() {
        val pushes = BodyLegs.pushesFor(body, 1f)

        assertEquals(24, pushes.size)
        pushes.forEach(
            action = { push ->
                val line = legLineOf(push)
                val inward = if (push.centerX < line) push.moveX > 0f else push.moveX < 0f
                assertTrue("edge at ${push.centerX} of the leg at $line moved by ${push.moveX}", inward)
                assertEquals(0f, push.moveY, 0.001f)
                assertTrue("edge at y ${push.centerY} is above the hips", push.centerY > 600f)
            },
        )
    }

    /** A negative amount pushes every leg edge away from its line (thicker). */
    @Test
    fun negative_pushes_every_leg_edge_out() {
        BodyLegs.pushesFor(body, -1f).forEach(
            action = { push ->
                val line = legLineOf(push)
                val outward = if (push.centerX < line) push.moveX < 0f else push.moveX > 0f
                assertTrue("edge at ${push.centerX} of the leg at $line moved by ${push.moveX}", outward)
            },
        )
    }

    /**
     * A photo cut above the knees (knees and ankles hardly visible) still slims both thighs, but
     * no calf: 2 thighs × 3 levels × 2 edges.
     */
    @Test
    fun a_thigh_is_slimmed_with_its_knee_out_of_the_photo() {
        val cut = hide(body, PoseIndex.LEFT_KNEE, PoseIndex.RIGHT_KNEE, PoseIndex.LEFT_ANKLE, PoseIndex.RIGHT_ANKLE)

        val pushes = BodyLegs.pushesFor(cut, 1f)

        assertEquals(12, pushes.size)
        pushes.forEach(action = { push -> assertTrue("edge at y ${push.centerY} is below the knee", push.centerY < 1000f) })
    }

    /** Hidden hips leave the thighs alone; the calves still go when knees and ankles are clear. */
    @Test
    fun hidden_hips_leave_the_thighs_alone() {
        val pushes = BodyLegs.pushesFor(hide(body, PoseIndex.LEFT_HIP, PoseIndex.RIGHT_HIP), 1f)

        assertEquals(12, pushes.size)
        pushes.forEach(action = { push -> assertTrue("edge at y ${push.centerY} is above the knee", push.centerY > 1000f) })
    }

    /** A value beyond ±1 is clamped, so 5 moves exactly as far as 1. */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(BodyLegs.pushesFor(body, 1f), BodyLegs.pushesFor(body, 5f))
        assertEquals(BodyLegs.pushesFor(body, -1f), BodyLegs.pushesFor(body, -5f))
    }

    /** The same figure twice as large is pushed twice as far, with twice the reach. */
    @Test
    fun the_moves_follow_the_body_size() {
        val large = SampleBody.upright(centerX = 1000f, top = 400f, length = 800f, shoulderSpread = 600f, hipSpread = 400f)

        BodyLegs.pushesFor(body, 0.5f).zip(BodyLegs.pushesFor(large, 0.5f)).forEach(
            action = { (one, two) ->
                assertEquals(one.moveX * 2, two.moveX, 0.01f)
                assertEquals(one.radius * 2, two.radius, 0.01f)
            },
        )
    }

    /** The line (x = 400 or 600) of the leg [push] belongs to: the nearer one. */
    private fun legLineOf(push: WarpPush): Float = if (abs(push.centerX - 400f) < abs(push.centerX - 600f)) 400f else 600f

    /** [body] with the landmarks at [indices] made invisible (visibility 0). */
    private fun hide(body: BodyLandmarks, vararg indices: Int): BodyLandmarks =
        body.copy(points = body.points.mapIndexed(transform = { i, point -> if (i in indices) point.copy(visibility = 0f) else point }))
}
