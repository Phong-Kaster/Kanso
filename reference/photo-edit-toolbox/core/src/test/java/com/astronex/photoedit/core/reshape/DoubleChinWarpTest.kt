package com.astronex.photoedit.core.reshape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DoubleChinWarp] on a made-up face ([MadeUpFace]): at +1 the jaw's lower edge between its corner and
 * the chin rises toward the face, both sides alike, at -1 it drops as far; the chin's point and the
 * mouth stay. Plain maths on the JVM.
 *
 * @author Phong-Kaster
 */
class DoubleChinWarpTest {

    /** The face every test reshapes. */
    private val face = MadeUpFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /**
     * Double chin 0 (and NaN) gives no pushes.
     *
     * @author Phong-Kaster
     */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(DoubleChinWarp.pushesFor(face, 0f).isEmpty())
        assertTrue(DoubleChinWarp.pushesFor(face, Float.NaN).isEmpty())
    }

    /**
     * At +1 the lower edge (150, 379) rises, the two sides by the same amount; at -1 it drops as far.
     *
     * @author Phong-Kaster
     */
    @Test
    fun plus_one_lifts_the_lower_edge_on_both_sides() {
        val lifted = DoubleChinWarp.pushesFor(face, 1f)
        val lowered = DoubleChinWarp.pushesFor(face, -1f)
        val left = face[150].y - FaceProbe.moved(face[150], lifted).y
        val right = face[379].y - FaceProbe.moved(face[379], lifted).y

        assertTrue("lifted $left px", left > 0.5f)
        assertEquals(left, right, 0.001f)
        assertEquals(-left, face[150].y - FaceProbe.moved(face[150], lowered).y, 0.001f)
    }

    /**
     * The chin's point and the mouth's corners stay where they are.
     *
     * @author Phong-Kaster
     */
    @Test
    fun the_chin_point_and_the_mouth_stay() {
        val pushes = DoubleChinWarp.pushesFor(face, 1f)

        listOf(FaceMeshLandmark.CHIN, 61, 291).forEach(
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
        assertEquals(DoubleChinWarp.pushesFor(face, 1f), DoubleChinWarp.pushesFor(face, 10f))
    }
}
