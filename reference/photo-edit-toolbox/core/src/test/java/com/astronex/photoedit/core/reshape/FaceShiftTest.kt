package com.astronex.photoedit.core.reshape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * [FaceShift] on a made-up face ([MadeUpFace]): a shift moves its landmark down the face and in toward
 * the centre line by shares of the eyes-to-chin length, a point on the line only down, a turned face
 * along its own tilt, a larger face by the same share. Plain maths on the JVM.
 *
 * @author Phong-Kaster
 */
class FaceShiftTest {

    /** The face every test moves: between the eyes at y 396, the chin at y 760, so 364 px apart. */
    private val face = MadeUpFace.oval(centerX = 500f, centerY = 500f, halfWidth = 200f, halfHeight = 260f)

    /** The eyes-to-chin length of [face], in pixels. */
    private val length = 0.4f * 260f + 260f

    /**
     * Strength 0 gives no pushes.
     *
     * @author Phong-Kaster
     */
    @Test
    fun zero_strength_gives_no_pushes() {
        assertTrue(FaceShift.pushesFor(face, listOf(FaceShift.Shift(172, 0.1f, 0.1f)), 0f, 0.1f).isEmpty())
    }

    /**
     * On an upright face a point on the photo's left moves right when it moves in and down when it
     * moves down, each by its share of the eyes-to-chin length times the strength; its mirror on the
     * right moves left.
     *
     * @author Phong-Kaster
     */
    @Test
    fun a_shift_moves_down_and_in_by_shares_of_the_length() {
        val pushes = FaceShift.pushesFor(face, FaceShift.mirrored(172, 397, down = 0.02f, inward = 0.01f), 0.5f, 0.1f)

        assertEquals(0.01f * length, pushes[0].moveY, 0.001f)
        assertEquals(0.005f * length, pushes[0].moveX, 0.001f)
        assertEquals(0.01f * length, pushes[1].moveY, 0.001f)
        assertEquals(-0.005f * length, pushes[1].moveX, 0.001f)
    }

    /**
     * The chin sits on the centre line, so its "in" share moves it nowhere: only its "down" share does.
     *
     * @author Phong-Kaster
     */
    @Test
    fun a_point_on_the_centre_line_moves_only_down() {
        val push = FaceShift.pushesFor(face, listOf(FaceShift.Shift(FaceMeshLandmark.CHIN, -0.01f, 0.05f)), 1f, 0.1f).single()

        assertEquals(0f, push.moveX, 0.0001f)
        assertEquals(-0.01f * length, push.moveY, 0.001f)
    }

    /**
     * Each push reaches its share of the face's width, and an offset puts it that share of the length
     * further down the face than its landmark.
     *
     * @author Phong-Kaster
     */
    @Test
    fun radius_and_offset_follow_the_face() {
        val faceWidth = hypot(face[454].x - face[234].x, face[454].y - face[234].y)
        val push = FaceShift.pushesFor(face, listOf(FaceShift.Shift(150, 0f, 0.01f)), 1f, 0.12f, offset = 0.05f).single()

        assertEquals(faceWidth * 0.12f, push.radius, 0.001f)
        assertEquals(face[150].x, push.centerX, 0.001f)
        assertEquals(face[150].y + 0.05f * length, push.centerY, 0.001f)
    }

    /**
     * The same face turned 30 degrees moves each point the same distance, turned with it; four times
     * larger, four times as far.
     *
     * @author Phong-Kaster
     */
    @Test
    fun a_turned_or_larger_face_moves_alike() {
        val shifts = listOf(FaceShift.Shift(172, -0.02f, 0.015f))
        val upright = FaceShift.pushesFor(face, shifts, 1f, 0.1f).single()
        val turned = FaceShift.pushesFor(FaceProbe.turned(face, 30f, 500f, 500f), shifts, 1f, 0.1f).single()
        val larger = FaceShift.pushesFor(FaceProbe.scaled(face, 4f), shifts, 1f, 0.1f).single()

        val angle = Math.toRadians(30.0)
        val expectedX = (upright.moveX * cos(angle) - upright.moveY * sin(angle)).toFloat()
        val expectedY = (upright.moveX * sin(angle) + upright.moveY * cos(angle)).toFloat()
        assertEquals(expectedX, turned.moveX, 0.01f)
        assertEquals(expectedY, turned.moveY, 0.01f)
        assertEquals(upright.moveX * 4f, larger.moveX, 0.01f)
        assertEquals(upright.moveY * 4f, larger.moveY, 0.01f)
    }
}
