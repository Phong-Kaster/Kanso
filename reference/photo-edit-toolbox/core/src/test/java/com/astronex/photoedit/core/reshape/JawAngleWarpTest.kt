package com.astronex.photoedit.core.reshape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [JawAngleWarp] on a made-up face ([MadeUpFace]): at +1 the jaw's corners rise and draw in, at -1 they
 * drop and spread about 0.7 times as far (Meitu's two ends); the chin and the mouth stay, Width's moves
 * add up with it, and a turned or larger face changes alike. Plain maths on the JVM.
 *
 * @author Phong-Kaster
 */
class JawAngleWarpTest {

    /** The face every test reshapes. */
    private val face = MadeUpFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /** The jaw's corners, left then right in the photo (outline position 4: 172, 397). */
    private val corners = FaceMeshLandmark.LEFT_OUTLINE[4] to FaceMeshLandmark.RIGHT_OUTLINE[4]

    /**
     * Jaw angle 0 (and NaN) gives no pushes.
     *
     * @author Phong-Kaster
     */
    @Test
    fun zero_gives_no_pushes() {
        assertTrue(JawAngleWarp.pushesFor(face, 0f).isEmpty())
        assertTrue(JawAngleWarp.pushesFor(face, Float.NaN).isEmpty())
    }

    /**
     * At +1 the jaw's corners rise and come closer together; at -1 they drop and move apart.
     *
     * @author Phong-Kaster
     */
    @Test
    fun plus_one_raises_and_draws_in_the_corners() {
        val corner = face[corners.first]
        val raised = FaceProbe.moved(corner, JawAngleWarp.pushesFor(face, 1f))
        val dropped = FaceProbe.moved(corner, JawAngleWarp.pushesFor(face, -1f))

        assertTrue(raised.y < corner.y && raised.x > corner.x)
        assertTrue(dropped.y > corner.y && dropped.x < corner.x)
        assertTrue(FaceProbe.change(face, corners.first, corners.second, JawAngleWarp.pushesFor(face, 1f)) < 0f)
    }

    /**
     * The low end moves the corners about 0.7 times as far as the high end.
     *
     * @author Phong-Kaster
     */
    @Test
    fun the_low_end_is_weaker() {
        val high = FaceProbe.shift(face[corners.first], JawAngleWarp.pushesFor(face, 1f))
        val low = FaceProbe.shift(face[corners.first], JawAngleWarp.pushesFor(face, -1f))

        assertEquals(0.7f, low / high, 0.03f)
    }

    /**
     * The chin and the mouth's corners stay where they are.
     *
     * @author Phong-Kaster
     */
    @Test
    fun the_chin_and_the_mouth_stay() {
        val pushes = JawAngleWarp.pushesFor(face, 1f)

        listOf(FaceMeshLandmark.CHIN, 61, 291).forEach(
            action = { assertEquals("$it", 0f, FaceProbe.shift(face[it], pushes), 0f) },
        )
    }

    /**
     * With Width also at +1, the jaw's corner gets one push whose move is the sum of both.
     *
     * @author Phong-Kaster
     */
    @Test
    fun width_and_jaw_angle_add_up() {
        val corner = face[corners.first]
        val both = FaceWarps.pushesFor(face, FaceWarpAmounts(width = 1f, jawAngle = 1f))
        val widthAlone = FaceWidthWarp.pushesFor(face, 1f).first(predicate = { it.centerX == corner.x && it.centerY == corner.y })
        val angleAlone = JawAngleWarp.pushesFor(face, 1f).first(predicate = { it.centerX == corner.x && it.centerY == corner.y })

        val merged = both.single(predicate = { it.centerX == corner.x && it.centerY == corner.y })
        assertEquals(widthAlone.moveX + angleAlone.moveX, merged.moveX, 0.0001f)
        assertEquals(widthAlone.moveY + angleAlone.moveY, merged.moveY, 0.0001f)
    }

    /**
     * A value beyond ±1 is clamped.
     *
     * @author Phong-Kaster
     */
    @Test
    fun beyond_one_is_clamped() {
        assertEquals(JawAngleWarp.pushesFor(face, 1f), JawAngleWarp.pushesFor(face, 4f))
        assertEquals(JawAngleWarp.pushesFor(face, -1f), JawAngleWarp.pushesFor(face, -4f))
    }

    /**
     * The same face turned 20 degrees, or four times larger, narrows its jaw by the same share.
     *
     * @author Phong-Kaster
     */
    @Test
    fun a_turned_or_larger_face_changes_alike() {
        val expected = FaceProbe.change(face, corners.first, corners.second, JawAngleWarp.pushesFor(face, 1f))

        listOf(FaceProbe.turned(face, 20f, 500f, 500f), FaceProbe.scaled(face, 4f)).forEach(
            action = { other ->
                val pushes = JawAngleWarp.pushesFor(other, 1f)
                assertEquals(expected, FaceProbe.change(other, corners.first, corners.second, pushes), 0.001f)
            },
        )
    }
}
