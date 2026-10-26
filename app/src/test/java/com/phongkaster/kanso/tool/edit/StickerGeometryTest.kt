package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.MAX_STICKER_WIDTH_FRACTION
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.MIN_STICKER_WIDTH_FRACTION
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.SIDE_END
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.SIDE_START
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.angleBetween
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.angleChange
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.cornerX
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.cornerY
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.distanceBetween
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.handleDegrees
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.handleWidth
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.pinchCentreX
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.pinchCentreY
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.pointOnSticker
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the sticker overlay's pure maths (Run 10, DoD 8): hit testing on a turned rectangle, the
 * four corners, distances and angles, the resize handle's size and turn, and the pinch's centre.
 * Plain JUnit: the functions know nothing about Android.
 */
class StickerGeometryTest {

    /** A point inside, on the edge of and outside an unturned rectangle is a hit, a hit and a miss. */
    @Test
    fun an_unturned_rectangle_is_hit_inside_and_on_its_edge_only() {
        assertTrue(pointOnSticker(100f, 100f, 100f, 100f, 80f, 40f, 0f))
        assertTrue(pointOnSticker(139f, 119f, 100f, 100f, 80f, 40f, 0f))
        assertTrue("the edge counts", pointOnSticker(140f, 120f, 100f, 100f, 80f, 40f, 0f))
        assertFalse(pointOnSticker(141f, 100f, 100f, 100f, 80f, 40f, 0f))
        assertFalse(pointOnSticker(100f, 121f, 100f, 100f, 80f, 40f, 0f))
    }

    /** A wide rectangle turned a quarter turn covers what was its tall side, not its wide side. */
    @Test
    fun a_turned_rectangle_is_hit_where_it_lies_after_the_turn() {
        // 100 wide and 20 tall, turned 90 degrees: now 20 wide and 100 tall about (0, 0).
        assertTrue(pointOnSticker(0f, 45f, 0f, 0f, 100f, 20f, 90f))
        assertTrue(pointOnSticker(0f, -45f, 0f, 0f, 100f, 20f, 90f))
        assertFalse("the old wide side is empty now", pointOnSticker(45f, 0f, 0f, 0f, 100f, 20f, 90f))
        // Turned 45 degrees: the diagonal is covered, a point off the diagonal is not.
        assertTrue(pointOnSticker(30f, 30f, 0f, 0f, 100f, 20f, 45f))
        assertFalse(pointOnSticker(30f, -30f, 0f, 0f, 100f, 20f, 45f))
    }

    /** Unturned, the four corners are the rectangle's own corners (y grows downward). */
    @Test
    fun the_corners_of_an_unturned_rectangle_are_its_corners() {
        fun x(sx: Float, sy: Float) = cornerX(50f, 80f, 60f, 20f, 0f, sx, sy)
        fun y(sx: Float, sy: Float) = cornerY(50f, 80f, 60f, 20f, 0f, sx, sy)
        assertEquals(20f, x(SIDE_START, SIDE_START), EPS)
        assertEquals(70f, y(SIDE_START, SIDE_START), EPS)
        assertEquals(80f, x(SIDE_END, SIDE_START), EPS)
        assertEquals(70f, y(SIDE_END, SIDE_START), EPS)
        assertEquals(20f, x(SIDE_START, SIDE_END), EPS)
        assertEquals(90f, y(SIDE_START, SIDE_END), EPS)
        assertEquals(80f, x(SIDE_END, SIDE_END), EPS)
        assertEquals(90f, y(SIDE_END, SIDE_END), EPS)
    }

    /** Turned 90 degrees clockwise the top-left corner goes to the top-right of the old place. */
    @Test
    fun the_corners_follow_the_turn_clockwise() {
        // Width 60, height 20: the top-left offset (-30, -10) turned clockwise 90 is (10, -30).
        assertEquals(110f, cornerX(100f, 100f, 60f, 20f, 90f, SIDE_START, SIDE_START), EPS)
        assertEquals(70f, cornerY(100f, 100f, 60f, 20f, 90f, SIDE_START, SIDE_START), EPS)
        // A half turn swaps opposite corners.
        assertEquals(130f, cornerX(100f, 100f, 60f, 20f, 180f, SIDE_START, SIDE_START), EPS)
        assertEquals(110f, cornerY(100f, 100f, 60f, 20f, 180f, SIDE_START, SIDE_START), EPS)
    }

    /** Distance and direction: 0 is right, 90 is down, 180 is left, and a 3-4-5 triangle is 5. */
    @Test
    fun distance_and_angle_between_two_points() {
        assertEquals(5f, distanceBetween(0f, 0f, 3f, 4f), EPS)
        assertEquals(0f, angleBetween(0f, 0f, 1f, 0f), EPS)
        assertEquals(90f, angleBetween(0f, 0f, 0f, 1f), EPS)
        assertEquals(180f, Math.abs(angleBetween(0f, 0f, -1f, 0f)), EPS)
        assertEquals(-90f, angleBetween(0f, 0f, 0f, -1f), EPS)
    }

    /** A turn across the 180 line is the short way round, not a jump of nearly 360. */
    @Test
    fun an_angle_change_takes_the_short_way_round() {
        assertEquals(20f, angleChange(170f, -170f), EPS)
        assertEquals(-20f, angleChange(-170f, 170f), EPS)
        assertEquals(90f, angleChange(10f, 100f), EPS)
        assertEquals(0f, angleChange(45f, 45f), EPS)
    }

    /**
     * DoD 8: a finger moved from distance d at angle a to distance 2d at angle a + 90 doubles the
     * width and adds 90 degrees (within half a degree), for several starting angles including ones
     * that cross the 180 line.
     */
    @Test
    fun a_finger_at_twice_the_distance_and_ninety_degrees_on_doubles_and_turns() {
        val startWidth = 80f
        for (angle in listOf(-170f, -45f, 0f, 20f, 100f, 170f)) {
            val d = 70f
            val fromX = d * cos(Math.toRadians(angle.toDouble())).toFloat()
            val fromY = d * sin(Math.toRadians(angle.toDouble())).toFloat()
            val toX = 2f * d * cos(Math.toRadians(angle + 90.0)).toFloat()
            val toY = 2f * d * sin(Math.toRadians(angle + 90.0)).toFloat()
            val width = handleWidth(
                startWidth,
                distanceBetween(0f, 0f, fromX, fromY),
                distanceBetween(0f, 0f, toX, toY),
                1f,
                1000f,
            )
            val turn = handleDegrees(
                15f,
                angleBetween(0f, 0f, fromX, fromY),
                angleBetween(0f, 0f, toX, toY),
            )
            assertEquals("width at $angle", 160f, width, 0.5f)
            assertEquals("turn at $angle", 105f, turn, 0.5f)
        }
    }

    /** The handle keeps the width between the limits, and a finger on the centre changes nothing. */
    @Test
    fun the_handle_keeps_the_width_within_its_limits() {
        assertEquals(10f, handleWidth(80f, 100f, 1f, 10f, 200f), EPS)
        assertEquals(200f, handleWidth(80f, 10f, 1000f, 10f, 200f), EPS)
        assertEquals(80f, handleWidth(80f, 0f, 50f, 10f, 200f), EPS)
        assertTrue(MIN_STICKER_WIDTH_FRACTION > 0f)
        assertTrue(MAX_STICKER_WIDTH_FRACTION <= 1f)
    }

    /** One finger, no scale, no turn: the centre moves by exactly the finger's move. */
    @Test
    fun a_pinch_with_one_finger_moves_the_centre_by_the_fingers_move() {
        val x = pinchCentreX(100f, 50f, 90f, 60f, 130f, 85f, 1f, 0f)
        val y = pinchCentreY(100f, 50f, 90f, 60f, 130f, 85f, 1f, 0f)
        assertEquals(140f, x, EPS)
        assertEquals(75f, y, EPS)
    }

    /** A pinch about the sticker's own centre scales and turns it in place. */
    @Test
    fun a_pinch_about_the_centre_leaves_the_centre_where_it_is() {
        assertEquals(100f, pinchCentreX(100f, 50f, 100f, 50f, 100f, 50f, 1.5f, 45f), EPS)
        assertEquals(50f, pinchCentreY(100f, 50f, 100f, 50f, 100f, 50f, 1.5f, 45f), EPS)
    }

    /** A pinch about another point swings the centre round it: scaled and turned clockwise. */
    @Test
    fun a_pinch_about_another_point_scales_and_turns_the_offset() {
        // Offset (10, 0) from the pivot (0, 0), turned 90 clockwise and doubled: (0, 20).
        assertEquals(0f, pinchCentreX(10f, 0f, 0f, 0f, 0f, 0f, 2f, 90f), EPS)
        assertEquals(20f, pinchCentreY(10f, 0f, 0f, 0f, 0f, 0f, 2f, 90f), EPS)
        // The new pivot is where the offset is laid from.
        assertEquals(100f, pinchCentreX(10f, 0f, 0f, 0f, 100f, 50f, 1f, 0f) - 10f, EPS)
        assertEquals(50f, pinchCentreY(10f, 0f, 0f, 0f, 100f, 50f, 1f, 0f), EPS)
    }

    /** Values shared by the tests. */
    private companion object {
        /** The slack for float arithmetic. */
        const val EPS = 1e-3f
    }
}
