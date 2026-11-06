package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.data.edit.edit.CurveAdjustments
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.EditCurves
import com.phongkaster.kanso.data.edit.edit.ToneCurve
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the Curves tool's maths with no Android types (DoD 2, 3 and 4 of Run 12): the straight
 * curve changes nothing, the line passes through every placed point and never overshoots between
 * two points, a table never leaves 0..255, and adding, moving and removing points keep the curve's
 * rules. It runs on the plain JVM.
 */
class CurveFormulaTest {

    /** A point at the 8-bit brightness [x] going in and [y] coming out, so the table hits it exactly. */
    private fun at(x: Int, y: Int) = CurvePoint(x / 255f, y / 255f)

    /** A curve through the given points between the two end points. */
    private fun curveOf(vararg middle: CurvePoint): ToneCurve =
        middle.fold(ToneCurve.NEUTRAL) { curve, point -> curve.added(point) }

    /** The straight curve leaves every pixel byte for byte as it was, and its table is the identity. */
    @Test
    fun the_neutral_curve_changes_no_pixel() {
        val pixels = IntArray(256) { (0xFF shl 24) or (it shl 16) or ((255 - it) shl 8) or (it / 2) }
        val before = pixels.copyOf()
        EditCurves.apply(pixels, CurveAdjustments.NEUTRAL)
        assertArrayEquals(before, pixels)
        assertArrayEquals(IntArray(256) { it }, ToneCurve.NEUTRAL.table())
    }

    /** The table gives exactly the placed output at every placed input. */
    @Test
    fun the_table_passes_through_every_placed_point() {
        val points = listOf(at(51, 20), at(102, 200), at(153, 210), at(204, 240))
        val table = curveOf(*points.toTypedArray()).table()
        points.forEach { assertEquals("at ${it.x}", Math.round(it.y * 255f), table[Math.round(it.x * 255f)]) }
        assertEquals(0, table[0])
        assertEquals(255, table[255])
    }

    /** Between two points the table stays between their two outputs: no overshoot, no halo. */
    @Test
    fun the_table_never_overshoots_between_two_points() {
        val curve = curveOf(at(51, 20), at(102, 220), at(153, 225), at(204, 100))
        val table = curve.table()
        val xs = curve.points.map { Math.round(it.x * 255f) }
        val ys = curve.points.map { Math.round(it.y * 255f) }
        for (segment in 0 until xs.lastIndex) {
            val low = minOf(ys[segment], ys[segment + 1])
            val high = maxOf(ys[segment], ys[segment + 1])
            for (i in xs[segment]..xs[segment + 1]) {
                assertTrue("entry $i is ${table[i]}, outside $low..$high", table[i] in low..high)
            }
        }
    }

    /** A steep S-curve stays inside 0..255 in every entry and never goes down while the points rise. */
    @Test
    fun a_steep_s_curve_stays_inside_black_and_white_and_rises() {
        val table = curveOf(CurvePoint(0.4f, 0.02f), CurvePoint(0.6f, 0.98f)).table()
        assertEquals(256, table.size)
        table.forEach { assertTrue("entry $it is outside 0..255", it in 0..255) }
        for (i in 1 until table.size) assertTrue("the table falls at $i", table[i] >= table[i - 1])
    }

    /** A curve never holds more than ten points, and a point too close to another is not added. */
    @Test
    fun added_keeps_at_most_ten_points_and_a_gap() {
        var curve = ToneCurve.NEUTRAL
        for (i in 1..20) curve = curve.added(CurvePoint(i / 21f, 0.5f))
        assertEquals(ToneCurve.MAX_POINTS, curve.points.size)
        assertSame("an eleventh point was added", curve, curve.added(CurvePoint(0.999f * 0.5f, 0.1f)))

        val two = ToneCurve.NEUTRAL.added(CurvePoint(0.5f, 0.5f))
        assertSame(two, two.added(CurvePoint(0.5f + ToneCurve.MIN_GAP / 2f, 0.9f)))
        assertEquals(listOf(0f, 0.5f, 1f), two.points.map { it.x })
    }

    /** A moved point cannot pass its neighbours, and the end points only move up and down. */
    @Test
    fun moved_cannot_pass_a_neighbour_and_ends_keep_their_x() {
        val curve = curveOf(CurvePoint(0.3f, 0.3f), CurvePoint(0.7f, 0.7f))
        val far = curve.moved(1, 5f, 0.4f)
        assertTrue(far.points[1].x <= far.points[2].x - ToneCurve.MIN_GAP + 1e-6f)
        val back = curve.moved(2, -5f, 0.4f)
        assertTrue(back.points[2].x >= back.points[1].x + ToneCurve.MIN_GAP - 1e-6f)

        val start = curve.moved(0, 0.5f, 0.25f)
        assertEquals(0f, start.points.first().x, 0f)
        assertEquals(0.25f, start.points.first().y, 0f)
        val end = curve.moved(3, 0.1f, 2f)
        assertEquals(1f, end.points.last().x, 0f)
        assertEquals(1f, end.points.last().y, 0f)
    }

    /** Removing a middle point drops it; the two end points cannot be removed. */
    @Test
    fun removed_ignores_the_end_points() {
        val curve = curveOf(CurvePoint(0.5f, 0.7f))
        assertSame(curve, curve.removed(0))
        assertSame(curve, curve.removed(curve.points.lastIndex))
        assertTrue(curve.removed(1).isNeutral)
    }

    /** A master curve that lifts the middle brightens grey, and leaves black and white alone. */
    @Test
    fun a_master_curve_raising_the_middle_brightens_grey_only() {
        val adjustments = CurveAdjustments(rgb = curveOf(CurvePoint(0.5f, 0.75f)))
        val pixels = intArrayOf(0xFF808080.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        EditCurves.apply(pixels, adjustments)
        assertTrue("grey did not brighten", (pixels[0] and 0xFF) > 0x80)
        assertEquals(0xFF000000.toInt(), pixels[1])
        assertEquals(0xFFFFFFFF.toInt(), pixels[2])
    }

    /** A red-only curve changes the red channel and nothing else, and keeps alpha. */
    @Test
    fun a_red_curve_changes_only_red() {
        val adjustments = CurveAdjustments(red = curveOf(CurvePoint(0.5f, 0.8f)))
        val pixels = intArrayOf(0x80808080.toInt())
        EditCurves.apply(pixels, adjustments)
        assertTrue("red did not change", ((pixels[0] shr 16) and 0xFF) > 0x80)
        assertEquals(0x80, (pixels[0] shr 8) and 0xFF)
        assertEquals(0x80, pixels[0] and 0xFF)
        assertEquals(0x80, (pixels[0] shr 24) and 0xFF)
    }
}
