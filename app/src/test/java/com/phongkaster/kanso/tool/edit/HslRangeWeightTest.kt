package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.EditHsl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the shape of the HSL tool's colour ranges: where the nine centres sit, how a range's
 * weight falls off with the distance in hue (the soft edges), and the gate that keeps greys out.
 *
 * - the nine ranges come in wheel order, 40 degrees apart, from Red at 0 to Pink at 320;
 * - a range weighs 1 at its centre, one half at the midpoint to a neighbour (so two neighbours
 *   always weigh 1 together), and 0 at the next centre and beyond;
 * - the weight never rises while moving from a centre to the next centre;
 * - the gate is about 0 for black, white, greys and near-greys, and 1 for a clear colour.
 *
 * It is a plain JVM test: the formula has no Android types, so no Robolectric is needed.
 */
class HslRangeWeightTest {

    /** The nine ranges are listed in order, with the names and centres the tool shows. */
    @Test
    fun the_nine_ranges_are_in_order_40_degrees_apart() {
        val expected = listOf(
            ColourRange.RED to 0f,
            ColourRange.ORANGE to 40f,
            ColourRange.LIGHT_GREEN to 80f,
            ColourRange.GREEN to 120f,
            ColourRange.LIGHT_BLUE to 160f,
            ColourRange.BLUE to 200f,
            ColourRange.NAVY_BLUE to 240f,
            ColourRange.PURPLE to 280f,
            ColourRange.PINK to 320f,
        )
        assertEquals("nine ranges", 9, ColourRange.entries.size)
        assertEquals(expected.map { it.first }, ColourRange.entries.toList())
        expected.forEach { (range, centre) -> assertEquals("centre of $range", centre, range.centreDegrees, 0f) }
    }

    /** A range weighs exactly 1 at its own centre. */
    @Test
    fun weight_is_one_at_each_centre() {
        ColourRange.entries.forEach { range ->
            assertEquals("$range at its centre", 1f, EditHsl.rangeWeight(range, range.centreDegrees), DELTA)
        }
    }

    /** Halfway to either neighbour (20 degrees away) a range weighs one half. */
    @Test
    fun weight_is_one_half_at_each_midpoint() {
        ColourRange.entries.forEach { range ->
            val up = (range.centreDegrees + 20f) % 360f
            val down = (range.centreDegrees - 20f + 360f) % 360f
            assertEquals("$range 20 degrees up", 0.5f, EditHsl.rangeWeight(range, up), DELTA)
            assertEquals("$range 20 degrees down", 0.5f, EditHsl.rangeWeight(range, down), DELTA)
        }
    }

    /** At every hue, in half-degree steps, the nine weights add up to 1, so a blend never loses or gains strength. */
    @Test
    fun weights_of_all_ranges_sum_to_one_for_every_hue() {
        var hue = 0f
        while (hue < 360f) {
            val sum = ColourRange.entries.sumOf { EditHsl.rangeWeight(it, hue).toDouble() }.toFloat()
            assertEquals("sum at hue $hue", 1f, sum, DELTA)
            hue += 0.5f
        }
    }

    /** From the next centre onwards, 40 degrees and further away, a range weighs nothing, also round the wheel. */
    @Test
    fun weight_is_zero_at_and_beyond_the_next_centre() {
        ColourRange.entries.forEach { range ->
            listOf(40f, 41f, 60f, 90f, 120f, 180f, 270f, 319f).forEach { distance ->
                val up = (range.centreDegrees + distance) % 360f
                val down = (range.centreDegrees - distance + 720f) % 360f
                assertEquals("$range $distance up", 0f, EditHsl.rangeWeight(range, up), DELTA)
                assertEquals("$range $distance down", 0f, EditHsl.rangeWeight(range, down), DELTA)
            }
        }
    }

    /** Red reaches back across 0 to pink: 350 degrees is 10 degrees from red's centre. */
    @Test
    fun red_reaches_across_the_wrap_to_the_pink_side() {
        val atTen = EditHsl.rangeWeight(ColourRange.RED, 10f)
        assertTrue("weight above 0.5", atTen > 0.5f)
        assertEquals("symmetric round 0", atTen, EditHsl.rangeWeight(ColourRange.RED, 350f), DELTA)
    }

    /** Moving from a centre to the next centre, in tenth-of-a-degree steps, the weight never goes up. */
    @Test
    fun weight_never_increases_from_a_centre_to_the_next() {
        ColourRange.entries.forEach { range ->
            var previous = EditHsl.rangeWeight(range, range.centreDegrees)
            for (step in 1..400) {
                val hue = (range.centreDegrees + step / 10f) % 360f
                val weight = EditHsl.rangeWeight(range, hue)
                assertTrue("$range at +${step / 10f}: $weight above $previous", weight <= previous + DELTA)
                previous = weight
            }
        }
    }

    /** Black, white and every grey from 0 to 255 get a gate of at most 0.05, and (10,10,10) and (245,245,245) too. */
    @Test
    fun the_gate_closes_for_greys() {
        for (value in 0..255) {
            val gate = EditHsl.gate(rgb(value, value, value))
            assertTrue("grey $value gate $gate", gate <= 0.05f)
        }
        assertTrue("(10,10,10)", EditHsl.gate(rgb(10, 10, 10)) <= 0.05f)
        assertTrue("(245,245,245)", EditHsl.gate(rgb(245, 245, 245)) <= 0.05f)
    }

    /** Any colour whose largest and smallest channels are 8/255 apart or less is nearly grey, so its gate is at most 0.05. */
    @Test
    fun the_gate_closes_for_near_greys() {
        for (base in 0..247 step 3) {
            for (g in 0..8) {
                for (b in 0..8) {
                    val argb = rgb(base, base + g, base + b)
                    val gate = EditHsl.gate(argb)
                    assertTrue("(${base}, ${base + g}, ${base + b}) gate $gate", gate <= 0.05f)
                }
            }
        }
    }

    /** A fully saturated colour opens the gate completely, and the gate never leaves 0..1. */
    @Test
    fun the_gate_opens_for_clear_colours() {
        assertEquals("pure red", 1f, EditHsl.gate(rgb(255, 0, 0)), DELTA)
        assertEquals("pure blue", 1f, EditHsl.gate(rgb(0, 0, 255)), DELTA)
        assertEquals("half spread", 1f, EditHsl.gate(rgb(100, 100, 228)), DELTA)
        for (spread in 0..255) {
            val gate = EditHsl.gate(rgb(0, 0, spread))
            assertTrue("spread $spread gate $gate", gate in 0f..1f)
        }
    }

    /** Builds an opaque ARGB int from three 0..255 channels. */
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private companion object {
        /** The tolerance for a weight, which is computed in single precision. */
        const val DELTA = 1e-4f
    }
}
