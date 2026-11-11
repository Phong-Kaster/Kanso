package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.data.edit.edit.CurveAdjustments
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.EditCurves
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.ToneCurve
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    /** Asserts the natural spline's value at each of [xs] is the matching [expected], within 1/255. */
    private fun assertValues(curve: ToneCurve, xs: List<Float>, expected: List<Float>) {
        xs.indices.forEach { i ->
            assertEquals("at ${xs[i]}", expected[i], curve.valueAt(xs[i]), 1f / 255f)
        }
    }

    /** The natural cubic spline through (0,0), (0.5,0.75), (1,1) has these known values. */
    @Test
    fun a_natural_spline_through_three_points_has_the_known_values() {
        val curve = curveOf(CurvePoint(0.5f, 0.75f))
        assertValues(
            curve,
            listOf(0.125f, 0.25f, 0.375f, 0.625f, 0.75f, 0.875f),
            listOf(0.2168f, 0.4219f, 0.6035f, 0.8535f, 0.9219f, 0.9668f),
        )
    }

    /** The same for four points (0,0), (0.25,0.15), (0.75,0.85), (1,1). */
    @Test
    fun a_natural_spline_through_four_points_has_the_known_values() {
        val curve = curveOf(CurvePoint(0.25f, 0.15f), CurvePoint(0.75f, 0.85f))
        assertValues(curve, listOf(0.125f, 0.375f, 0.625f, 0.875f), listOf(0.0563f, 0.3063f, 0.6938f, 0.9438f))
    }

    /** A spline that overshoots white is clipped: every entry from x = 0.40 on is 255. */
    @Test
    fun an_overshoot_is_clipped_to_white() {
        val table = curveOf(CurvePoint(0.25f, 0.75f)).table()
        for (i in 102..255) assertEquals("entry $i", 255, table[i])
    }

    /** A steep S-curve overshoots, but is clipped so every entry stays inside 0..255 (not a rising check any more). */
    @Test
    fun a_steep_s_curve_stays_inside_black_and_white() {
        val table = curveOf(CurvePoint(0.4f, 0.02f), CurvePoint(0.6f, 0.98f)).table()
        assertEquals(256, table.size)
        table.forEach { assertTrue("entry $it is outside 0..255", it in 0..255) }
    }

    /** End points may sit away from 0 and 1; the curve is flat beyond them and a line between two. */
    @Test
    fun the_curve_is_flat_beyond_its_end_points() {
        val curve = ToneCurve(listOf(CurvePoint(0.2f, 0.1f), CurvePoint(0.8f, 0.9f)))
        assertEquals(0.1f, curve.valueAt(0f), 0f)
        assertEquals(0.1f, curve.valueAt(0.2f), 0f)
        assertEquals(0.5f, curve.valueAt(0.5f), 1e-5f)
        assertEquals(0.9f, curve.valueAt(1f), 0f)
        val table = curve.table()
        assertEquals(Math.round(0.1f * 255f), table[0])
        assertEquals(Math.round(0.9f * 255f), table[255])
    }

    /** A point added left of the first or right of the last becomes the new end point. */
    @Test
    fun added_beyond_the_ends_makes_a_new_end_point() {
        val inner = ToneCurve(listOf(CurvePoint(0.3f, 0.3f), CurvePoint(0.7f, 0.7f)))
        val wider = inner.added(CurvePoint(0.1f, 0.2f)).added(CurvePoint(0.9f, 0.8f))
        assertEquals(listOf(0.1f, 0.3f, 0.7f, 0.9f), wider.points.map { it.x })
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

    /** A moved point cannot pass its neighbours, and the end points slide sideways within their limits. */
    @Test
    fun moved_cannot_pass_a_neighbour_and_ends_move_sideways() {
        val curve = curveOf(CurvePoint(0.3f, 0.3f), CurvePoint(0.7f, 0.7f))
        val far = curve.moved(1, 5f, 0.4f)
        assertTrue(far.points[1].x <= far.points[2].x - ToneCurve.MIN_GAP + 1e-6f)
        val back = curve.moved(2, -5f, 0.4f)
        assertTrue(back.points[2].x >= back.points[1].x + ToneCurve.MIN_GAP - 1e-6f)

        val start = curve.moved(0, 0.2f, 0.25f)
        assertEquals(0.2f, start.points.first().x, 0f)
        assertEquals(0.25f, start.points.first().y, 0f)
        val startFar = curve.moved(0, 0.9f, 0.25f)
        assertEquals(0.3f - ToneCurve.MIN_GAP, startFar.points.first().x, 1e-6f)
        assertEquals(0f, curve.moved(0, -3f, 0f).points.first().x, 0f)

        val end = curve.moved(3, 0.85f, 2f)
        assertEquals(0.85f, end.points.last().x, 0f)
        assertEquals(1f, end.points.last().y, 0f)
        val endFar = curve.moved(3, 0.1f, 0.5f)
        assertEquals(0.7f + ToneCurve.MIN_GAP, endFar.points.last().x, 1e-6f)
        assertEquals(1f, curve.moved(3, 9f, 0.5f).points.last().x, 0f)
    }

    /**
     * Pinning a point against its neighbour, then the next point against that one, can leave the
     * outer two a float hair under two gaps apart (most often where the numbers cross 0.5, a power
     * of two, and the rounding step doubles). Moving the middle point again must then not throw
     * (it did: `coerceIn` refuses an empty range), and the curve must keep its rules.
     */
    @Test
    fun moving_a_point_squeezed_between_pinned_neighbours_never_throws() {
        var squeezed = 0
        for (k in 0..2000) {
            val base = 0.4f + k * 0.00005f
            // [0, base, 0.9, 1], then 0.9 is dragged left until it stops one gap after base.
            val pinnedOnce = curveOf(CurvePoint(base, 0.5f), CurvePoint(0.9f, 0.5f)).moved(2, 0f, 0.5f)
            // Add 0.95 and drag it left until it stops one gap after the point just pinned.
            val pinnedTwice = pinnedOnce.added(CurvePoint(0.95f, 0.5f)).moved(3, 0f, 0.5f)
            assertEquals("setup at base $base", 5, pinnedTwice.points.size)
            val xs = pinnedTwice.points.map { it.x }
            if (xs[1] + ToneCurve.MIN_GAP > xs[3] - ToneCurve.MIN_GAP) squeezed++

            for (target in listOf(0f, 0.5f, 1f)) {
                val moved = pinnedTwice.moved(2, target, 0.3f)
                val x = moved.points[2].x
                assertTrue("passed the left neighbour at base $base", x > moved.points[1].x)
                assertTrue("passed the right neighbour at base $base", x < moved.points[3].x)
                assertEquals(0.3f, moved.points[2].y, 0f)
            }
        }
        assertTrue("no case left the neighbours under two gaps apart, so nothing was tested", squeezed > 0)
    }

    /**
     * A point placed exactly on the diagonal still counts as a change (3 points), so it gets an
     * Undo step and Reset stays enabled.
     */
    @Test
    fun a_point_on_the_diagonal_counts_as_a_change() {
        val onDiagonal = ToneCurve.NEUTRAL.added(CurvePoint(0.5f, 0.5f))
        assertEquals(3, onDiagonal.points.size)
        assertNotEquals(ToneCurve.NEUTRAL, onDiagonal)
        assertFalse(onDiagonal.isNeutral)
        assertTrue(EditSettings(curves = CurveAdjustments(rgb = onDiagonal)).hasCurveChange)
    }

    /** Any point can be removed while three or more remain; a two-point curve is returned as it is. */
    @Test
    fun removed_keeps_at_least_two_points() {
        val curve = curveOf(CurvePoint(0.5f, 0.7f))
        assertTrue(curve.removed(1).isNeutral)
        val withoutFirst = curve.removed(0)
        assertEquals(listOf(0.5f, 1f), withoutFirst.points.map { it.x })
        val withoutLast = curve.removed(curve.points.lastIndex)
        assertEquals(listOf(0f, 0.5f), withoutLast.points.map { it.x })
        assertSame(withoutFirst, withoutFirst.removed(0))
        assertSame(ToneCurve.NEUTRAL, ToneCurve.NEUTRAL.removed(1))
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

    /** A Green curve changes only green and a Blue curve only blue (DoD 10); the other two bytes stay as they were. */
    @Test
    fun a_green_or_blue_curve_changes_only_that_colour() {
        val bend = curveOf(CurvePoint(0.5f, 0.8f))
        val green = intArrayOf(0xFF404040.toInt())
        EditCurves.apply(green, CurveAdjustments(green = bend))
        assertEquals(0x40, (green[0] shr 16) and 0xFF)
        assertTrue("green did not change", ((green[0] shr 8) and 0xFF) > 0x40)
        assertEquals(0x40, green[0] and 0xFF)

        val blue = intArrayOf(0xFF404040.toInt())
        EditCurves.apply(blue, CurveAdjustments(blue = bend))
        assertEquals(0x40, (blue[0] shr 16) and 0xFF)
        assertEquals(0x40, (blue[0] shr 8) and 0xFF)
        assertTrue("blue did not change", (blue[0] and 0xFF) > 0x40)
    }

    /** The master curve moves all three colours alike. */
    @Test
    fun the_rgb_curve_changes_all_three_colours_alike() {
        val pixels = intArrayOf(0xFF404040.toInt())
        EditCurves.apply(pixels, CurveAdjustments(rgb = curveOf(CurvePoint(0.25f, 0.6f))))
        val r = (pixels[0] shr 16) and 0xFF
        assertTrue("the master curve did not lift", r > 0x40)
        assertEquals(r, (pixels[0] shr 8) and 0xFF)
        assertEquals(r, pixels[0] and 0xFF)
    }

    /** The master curve runs after the single-colour one: the result is rgb(red(x)), not red(rgb(x)). */
    @Test
    fun the_rgb_curve_is_applied_after_the_single_colour_curve() {
        val red = curveOf(at(64, 200))
        val rgb = curveOf(at(128, 40))
        val pixels = intArrayOf(0xFF404040.toInt())
        EditCurves.apply(pixels, CurveAdjustments(rgb = rgb, red = red))

        val redTable = red.table()
        val rgbTable = rgb.table()
        assertEquals(rgbTable[redTable[0x40]], (pixels[0] shr 16) and 0xFF)
        assertEquals(rgbTable[0x40], (pixels[0] shr 8) and 0xFF)
        assertEquals(rgbTable[0x40], pixels[0] and 0xFF)
        assertNotEquals("the order cannot be told apart", rgbTable[redTable[0x40]], redTable[rgbTable[0x40]])
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
