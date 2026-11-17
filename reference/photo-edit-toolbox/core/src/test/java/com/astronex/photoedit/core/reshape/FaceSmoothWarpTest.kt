package com.astronex.photoedit.core.reshape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FaceSmoothWarp] on a made-up face ([MadeUpFace]): the outline moves only sideways, the jaw's corner
 * in and the dip above it (outline position 2) out at +1, the chin not at all; -1 is +1 reversed and
 * a half value moves half as far. Plain maths on the JVM.
 *
 * @author Phong-Kaster
 */
class FaceSmoothWarpTest {

    /** The face every test reshapes, upright, so "sideways" is along x. */
    private val face = MadeUpFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /**
     * Smooth 0 (and NaN) gives no pushes.
     *
     * @author Phong-Kaster
     */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(FaceSmoothWarp.pushesFor(face, 0f).isEmpty())
        assertTrue(FaceSmoothWarp.pushesFor(face, Float.NaN).isEmpty())
    }

    /**
     * Every push slides sideways only, never up or down the face.
     *
     * @author Phong-Kaster
     */
    @Test
    fun the_outline_moves_only_sideways() {
        assertTrue(FaceSmoothWarp.pushesFor(face, 1f).all(predicate = { it.moveY == 0f && it.grow == 0f }))
    }

    /**
     * At +1 the jaw's corners come closer together and the dip above them (132, 361) moves apart.
     *
     * @author Phong-Kaster
     */
    @Test
    fun the_corner_moves_in_and_the_dip_out() {
        val pushes = FaceSmoothWarp.pushesFor(face, 1f)

        assertTrue(FaceProbe.change(face, 172, 397, pushes) < 0f)
        assertTrue(FaceProbe.change(face, 132, 361, pushes) > 0f)
    }

    /**
     * The chin stays where it is.
     *
     * @author Phong-Kaster
     */
    @Test
    fun the_chin_stays() {
        assertEquals(0f, FaceProbe.shift(face[FaceMeshLandmark.CHIN], FaceSmoothWarp.pushesFor(face, 1f)), 0f)
    }

    /**
     * -1 gives the same pushes as +1 with every move reversed, and 0.5 moves each push half as far.
     *
     * @author Phong-Kaster
     */
    @Test
    fun minus_one_reverses_and_half_halves() {
        val full = FaceSmoothWarp.pushesFor(face, 1f)
        val reversed = FaceSmoothWarp.pushesFor(face, -1f)
        val half = FaceSmoothWarp.pushesFor(face, 0.5f)

        full.indices.forEach(
            action = { i ->
                assertEquals(-full[i].moveX, reversed[i].moveX, 0.0001f)
                assertEquals(full[i].moveX / 2f, half[i].moveX, 0.0001f)
            },
        )
    }

    /**
     * A value beyond ±1 is clamped.
     *
     * @author Phong-Kaster
     */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(FaceSmoothWarp.pushesFor(face, 1f), FaceSmoothWarp.pushesFor(face, 2f))
    }
}
