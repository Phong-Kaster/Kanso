package com.astronex.photoedit.core.reshape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ChinTipWarp] on a made-up face ([MadeUpFace]): at +1 the chin's sides rise and draw in more than its
 * point, so the tip comes to a point, at -1 the reverse, as strongly; the mouth stays, and a turned or
 * larger face changes alike. Plain maths on the JVM.
 *
 * @author Phong-Kaster
 */
class ChinTipWarpTest {

    /** The face every test reshapes. */
    private val face = MadeUpFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /**
     * How far below its neighbours (148, 377) the chin's point sits, after [pushes], in pixels.
     *
     * @author Phong-Kaster
     */
    private fun tipDepth(pushes: List<WarpHandle>): Float {
        val sides = (FaceProbe.moved(face[148], pushes).y + FaceProbe.moved(face[377], pushes).y) / 2f
        return FaceProbe.moved(face[FaceMeshLandmark.CHIN], pushes).y - sides
    }

    /**
     * Chin tip 0 (and NaN) gives no pushes.
     *
     * @author Phong-Kaster
     */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(ChinTipWarp.pushesFor(face, 0f).isEmpty())
        assertTrue(ChinTipWarp.pushesFor(face, Float.NaN).isEmpty())
    }

    /**
     * At +1 the chin's point ends up further below its neighbours (more pointed), at -1 less far, by
     * about the same amount; both the point and its sides move up at +1.
     *
     * @author Phong-Kaster
     */
    @Test
    fun plus_one_points_the_chin_and_minus_one_rounds_it() {
        val before = tipDepth(emptyList())
        val pointed = tipDepth(ChinTipWarp.pushesFor(face, 1f)) - before
        val rounded = tipDepth(ChinTipWarp.pushesFor(face, -1f)) - before

        assertTrue("pointed $pointed px", pointed > 1f)
        assertEquals(-pointed, rounded, pointed * 0.1f)
        assertTrue(FaceProbe.moved(face[FaceMeshLandmark.CHIN], ChinTipWarp.pushesFor(face, 1f)).y < face[FaceMeshLandmark.CHIN].y)
    }

    /**
     * The chin's sides draw in at +1: 176 and 400 come closer together.
     *
     * @author Phong-Kaster
     */
    @Test
    fun the_sides_draw_in() {
        assertTrue(FaceProbe.change(face, 176, 400, ChinTipWarp.pushesFor(face, 1f)) < 0f)
        assertTrue(FaceProbe.change(face, 176, 400, ChinTipWarp.pushesFor(face, -1f)) > 0f)
    }

    /**
     * The mouth's corners and the jaw's corners stay where they are.
     *
     * @author Phong-Kaster
     */
    @Test
    fun the_mouth_and_the_jaw_stay() {
        val pushes = ChinTipWarp.pushesFor(face, 1f)

        listOf(61, 291, FaceMeshLandmark.LEFT_OUTLINE[4], FaceMeshLandmark.RIGHT_OUTLINE[4]).forEach(
            action = { assertEquals("$it", 0f, FaceProbe.shift(face[it], pushes), 0f) },
        )
    }

    /**
     * A value beyond ±1 is clamped.
     *
     * @author Phong-Kaster
     */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(ChinTipWarp.pushesFor(face, 1f), ChinTipWarp.pushesFor(face, 3f))
        assertEquals(ChinTipWarp.pushesFor(face, -1f), ChinTipWarp.pushesFor(face, -3f))
    }

    /**
     * The same face turned 20 degrees, or four times larger, narrows its chin by the same share.
     *
     * @author Phong-Kaster
     */
    @Test
    fun a_turned_or_larger_face_changes_alike() {
        val expected = FaceProbe.change(face, 176, 400, ChinTipWarp.pushesFor(face, 1f))

        listOf(FaceProbe.turned(face, 20f, 500f, 500f), FaceProbe.scaled(face, 4f)).forEach(
            action = { other -> assertEquals(expected, FaceProbe.change(other, 176, 400, ChinTipWarp.pushesFor(other, 1f)), 0.001f) },
        )
    }
}
