package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.EditHsl
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.HslAdjustments
import com.phongkaster.kanso.data.edit.edit.HslChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Checks the HSL tool's storage and its per-pixel formula ([EditHsl.apply]):
 *
 * - a new [EditSettings] holds 27 values at 0, each can be set on its own, and a value past the
 *   ends is clamped to -1 and +1 (the slider's -100 and +100);
 * - Hue at +-100 turns a colour at a range's centre by 20 degrees, and +50 by about 10;
 * - Saturation at -100 gives a grey, and at +100 doubles the saturation but never passes full colour;
 * - Lightness at -100 darkens strongly and at +100 lightens strongly, never to pure black or white;
 * - a ramp through every hue shows no jump (no banding) when one range is pushed to its ends;
 * - Blue's values leave a skin tone, pure red and greys alone.
 *
 * A pixel at a range's centre is used wherever an exact answer is needed: there every other range
 * weighs 0, so the blend is exactly that range's own value. The test builds pixels from hue,
 * saturation and lightness with its own conversion, so it does not trust the one it checks. It is
 * a plain JVM test; the formula has no Android types.
 */
class HslFormulaTest {

    /** A colour read back as hue (degrees, 0 up to 360), saturation and lightness (0..1). */
    private data class Hsl(val hue: Float, val saturation: Float, val lightness: Float)

    /** A new settings object holds 27 HSL values, all 0, and reports no HSL change. */
    @Test
    fun new_settings_hold_27_values_all_at_zero() {
        val settings = EditSettings()
        var count = 0
        ColourRange.entries.forEach { range ->
            HslChannel.entries.forEach { channel ->
                assertEquals("$range $channel", 0f, settings.hsl[range, channel], 0f)
                count++
            }
        }
        assertEquals(27, count)
        assertEquals(9, settings.hsl.hue.size)
        assertEquals(9, settings.hsl.saturation.size)
        assertEquals(9, settings.hsl.lightness.size)
        assertTrue(settings.hsl.isNeutral)
        assertFalse(settings.hasHslChange)
    }

    /** Setting one of the 27 values changes that one only, and marks HSL as changed. */
    @Test
    fun each_value_can_be_set_independently() {
        ColourRange.entries.forEach { target ->
            HslChannel.entries.forEach { channel ->
                val changed = HslAdjustments.NEUTRAL.with(target, channel, 0.5f)
                ColourRange.entries.forEach { range ->
                    HslChannel.entries.forEach { other ->
                        val expected = if (range == target && other == channel) 0.5f else 0f
                        assertEquals("$range $other after setting $target $channel", expected, changed[range, other], 0f)
                    }
                }
                assertFalse(changed.isNeutral)
                assertTrue(EditSettings(hsl = changed).hasHslChange)
            }
        }
    }

    /** A value beyond the ends is clamped to -1 and +1, which the slider shows as -100 and +100. */
    @Test
    fun values_past_the_ends_are_clamped() {
        ColourRange.entries.forEach { range ->
            HslChannel.entries.forEach { channel ->
                val high = HslAdjustments.NEUTRAL.with(range, channel, 7f)[range, channel]
                val low = HslAdjustments.NEUTRAL.with(range, channel, -7f)[range, channel]
                assertEquals("$range $channel high", 1f, high, 0f)
                assertEquals("$range $channel low", -1f, low, 0f)
                assertEquals(100, (high * 100f).roundToInt())
                assertEquals(-100, (low * 100f).roundToInt())
            }
        }
    }

    /** Hue +100 turns a fully saturated centre colour 20 degrees up, in every one of the nine ranges. */
    @Test
    fun hue_plus_100_turns_20_degrees_up() {
        ColourRange.entries.forEach { range ->
            val before = pixel(range.centreDegrees, 1f, 0.5f)
            val after = applyTo(before, HslAdjustments.NEUTRAL.with(range, HslChannel.HUE, 1f))
            assertEquals("$range", 20f, hueTurn(before, after), 0.5f)
        }
    }

    /** Hue -100 turns a fully saturated centre colour 20 degrees down, in every one of the nine ranges. */
    @Test
    fun hue_minus_100_turns_20_degrees_down() {
        ColourRange.entries.forEach { range ->
            val before = pixel(range.centreDegrees, 1f, 0.5f)
            val after = applyTo(before, HslAdjustments.NEUTRAL.with(range, HslChannel.HUE, -1f))
            assertEquals("$range", -20f, hueTurn(before, after), 0.5f)
        }
    }

    /** Hue +50 turns about 10 degrees: the turn is proportional to the amount. */
    @Test
    fun hue_plus_50_turns_about_10_degrees() {
        ColourRange.entries.forEach { range ->
            val before = pixel(range.centreDegrees, 1f, 0.5f)
            val after = applyTo(before, HslAdjustments.NEUTRAL.with(range, HslChannel.HUE, 0.5f))
            assertEquals("$range", 10f, hueTurn(before, after), 0.5f)
        }
    }

    /** Saturation -100 turns a half-saturated centre colour into a grey: red, green and blue are equal (within 1). */
    @Test
    fun saturation_minus_100_gives_grey() {
        ColourRange.entries.forEach { range ->
            val before = pixel(range.centreDegrees, 0.5f, 0.5f)
            val after = applyTo(before, HslAdjustments.NEUTRAL.with(range, HslChannel.SATURATION, -1f))
            val spread = max(red(after), max(green(after), blue(after))) - min(red(after), min(green(after), blue(after)))
            assertTrue("$range spread $spread", spread <= 1)
        }
    }

    /** Saturation +100 doubles a half-saturated colour's saturation, and a three-quarters one stops at full colour. */
    @Test
    fun saturation_plus_100_doubles_but_never_passes_full() {
        ColourRange.entries.forEach { range ->
            listOf(0.25f, 0.5f, 0.75f).forEach { s ->
                val before = pixel(range.centreDegrees, s, 0.5f)
                val after = applyTo(before, HslAdjustments.NEUTRAL.with(range, HslChannel.SATURATION, 1f))
                val expected = min(2f * toHsl(before).saturation, 1f)
                assertEquals("$range s=$s", expected, toHsl(after).saturation, 2f / 255f)
                assertTrue("channels in range", listOf(red(after), green(after), blue(after)).all { it in 0..255 })
            }
        }
    }

    /** Lightness -100 takes at least half the lightness away from a saturated centre colour and never makes it black. */
    @Test
    fun lightness_minus_100_darkens_strongly_but_not_to_black() {
        ColourRange.entries.forEach { range ->
            val before = pixel(range.centreDegrees, 1f, 0.5f)
            val after = applyTo(before, HslAdjustments.NEUTRAL.with(range, HslChannel.LIGHTNESS, -1f))
            val lBefore = toHsl(before).lightness
            val lAfter = toHsl(after).lightness
            assertTrue("$range lightness $lAfter from $lBefore", lAfter <= lBefore / 2f + 1f / 255f)
            assertNotEquals("$range is not black", 0, after and 0xFFFFFF)
        }
    }

    /** Lightness +100 covers at least half the distance to white from a saturated centre colour and never makes it white. */
    @Test
    fun lightness_plus_100_lightens_strongly_but_not_to_white() {
        ColourRange.entries.forEach { range ->
            val before = pixel(range.centreDegrees, 1f, 0.5f)
            val after = applyTo(before, HslAdjustments.NEUTRAL.with(range, HslChannel.LIGHTNESS, 1f))
            val lBefore = toHsl(before).lightness
            val lAfter = toHsl(after).lightness
            assertTrue("$range lightness $lAfter from $lBefore", lAfter >= lBefore + (1f - lBefore) / 2f - 1f / 255f)
            assertNotEquals("$range is not white", 0xFFFFFF, after and 0xFFFFFF)
        }
    }

    /** The alpha channel of a pixel is kept by the formula. */
    @Test
    fun alpha_is_kept() {
        val before = (0x80 shl 24) or (pixel(200f, 1f, 0.5f) and 0xFFFFFF)
        val after = applyTo(before, HslAdjustments.NEUTRAL.with(ColourRange.BLUE, HslChannel.HUE, 1f))
        assertEquals(0x80, (after shr 24) and 0xFF)
    }

    /** A neutral set of values changes no pixel. */
    @Test
    fun neutral_values_change_nothing() {
        val pixels = IntArray(361) { pixel(it.toFloat(), 1f, 0.5f) }
        val copy = pixels.copyOf()
        EditHsl.apply(pixels, HslAdjustments.NEUTRAL)
        assertTrue(copy.contentEquals(pixels))
    }

    /** Blue's Hue at +100 never makes neighbouring pixels of a full-saturation hue ramp differ by more than 12/255. */
    @Test
    fun blue_hue_plus_100_leaves_no_band() {
        assertRampIsSmooth(HslAdjustments.NEUTRAL.with(ColourRange.BLUE, HslChannel.HUE, 1f))
    }

    /** Blue's Saturation at +100 never makes neighbouring pixels of the ramp differ by more than 12/255. */
    @Test
    fun blue_saturation_plus_100_leaves_no_band() {
        assertRampIsSmooth(HslAdjustments.NEUTRAL.with(ColourRange.BLUE, HslChannel.SATURATION, 1f))
    }

    /** Blue's Lightness at -100 never makes neighbouring pixels of the ramp differ by more than 12/255. */
    @Test
    fun blue_lightness_minus_100_leaves_no_band() {
        assertRampIsSmooth(HslAdjustments.NEUTRAL.with(ColourRange.BLUE, HslChannel.LIGHTNESS, -1f))
    }

    /** Blue's Hue at +100 leaves a skin tone, pure red and five greys alone (within 1/255 a channel). */
    @Test
    fun blue_hue_plus_100_spares_skin_reds_and_greys() {
        assertBlueSpares(HslChannel.HUE, 1f)
    }

    /** Blue's Hue at -100 leaves a skin tone, pure red and five greys alone. */
    @Test
    fun blue_hue_minus_100_spares_skin_reds_and_greys() {
        assertBlueSpares(HslChannel.HUE, -1f)
    }

    /** Blue's Saturation at +100 leaves a skin tone, pure red and five greys alone. */
    @Test
    fun blue_saturation_plus_100_spares_skin_reds_and_greys() {
        assertBlueSpares(HslChannel.SATURATION, 1f)
    }

    /** Blue's Saturation at -100 leaves a skin tone, pure red and five greys alone. */
    @Test
    fun blue_saturation_minus_100_spares_skin_reds_and_greys() {
        assertBlueSpares(HslChannel.SATURATION, -1f)
    }

    /** Blue's Lightness at +100 leaves a skin tone, pure red and five greys alone. */
    @Test
    fun blue_lightness_plus_100_spares_skin_reds_and_greys() {
        assertBlueSpares(HslChannel.LIGHTNESS, 1f)
    }

    /** Blue's Lightness at -100 leaves a skin tone, pure red and five greys alone. */
    @Test
    fun blue_lightness_minus_100_spares_skin_reds_and_greys() {
        assertBlueSpares(HslChannel.LIGHTNESS, -1f)
    }

    /**
     * Runs [adjustments] over a 361-pixel ramp (hue 0 to 360 in one-degree steps, full saturation,
     * half lightness) and asserts that neighbouring output pixels never differ by more than 12/255
     * in any channel.
     */
    private fun assertRampIsSmooth(adjustments: HslAdjustments) {
        val pixels = IntArray(361) { pixel(it.toFloat(), 1f, 0.5f) }
        EditHsl.apply(pixels, adjustments)
        for (i in 1 until pixels.size) {
            val a = pixels[i - 1]
            val b = pixels[i]
            val jump = max(abs(red(a) - red(b)), max(abs(green(a) - green(b)), abs(blue(a) - blue(b))))
            assertTrue("jump of $jump between hue ${i - 1} and $i", jump <= 12)
        }
    }

    /**
     * Sets Blue's [channel] to [value] and asserts that a skin tone (hue about 25 degrees), pure red
     * and five greys each change by at most 1 in every channel.
     */
    private fun assertBlueSpares(channel: HslChannel, value: Float) {
        val adjustments = HslAdjustments.NEUTRAL.with(ColourRange.BLUE, channel, value)
        val spared = listOf(
            pixel(25f, 0.45f, 0.65f),
            pixel(25f, 0.6f, 0.5f),
            rgb(255, 0, 0),
            rgb(0, 0, 0),
            rgb(60, 60, 60),
            rgb(128, 128, 128),
            rgb(200, 200, 200),
            rgb(255, 255, 255),
        )
        spared.forEach { before ->
            val after = applyTo(before, adjustments)
            assertTrue("red of ${before.toUInt().toString(16)}", abs(red(after) - red(before)) <= 1)
            assertTrue("green of ${before.toUInt().toString(16)}", abs(green(after) - green(before)) <= 1)
            assertTrue("blue of ${before.toUInt().toString(16)}", abs(blue(after) - blue(before)) <= 1)
        }
    }

    /** Runs the formula over a single pixel and returns the result. */
    private fun applyTo(argb: Int, adjustments: HslAdjustments): Int {
        val pixels = intArrayOf(argb)
        EditHsl.apply(pixels, adjustments)
        return pixels[0]
    }

    /** The signed turn of hue, in degrees, from [before] to [after], taking the short way round the wheel. */
    private fun hueTurn(before: Int, after: Int): Float {
        var diff = toHsl(after).hue - toHsl(before).hue
        if (diff > 180f) diff -= 360f
        if (diff <= -180f) diff += 360f
        return diff
    }

    /** Builds an opaque ARGB pixel from hue (degrees), saturation and lightness (0..1), rounded to 8 bits. */
    private fun pixel(hue: Float, saturation: Float, lightness: Float): Int {
        val c = (1f - abs(2f * lightness - 1f)) * saturation
        val hp = (hue % 360f) / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val (r1, g1, b1) = when {
            hp < 1f -> Triple(c, x, 0f)
            hp < 2f -> Triple(x, c, 0f)
            hp < 3f -> Triple(0f, c, x)
            hp < 4f -> Triple(0f, x, c)
            hp < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = lightness - c / 2f
        return rgb(((r1 + m) * 255f).roundToInt(), ((g1 + m) * 255f).roundToInt(), ((b1 + m) * 255f).roundToInt())
    }

    /** Reads hue (degrees, 0 up to 360), saturation and lightness (0..1) back from an ARGB pixel. */
    private fun toHsl(argb: Int): Hsl {
        val r = red(argb) / 255f
        val g = green(argb) / 255f
        val b = blue(argb) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        val l = (maxC + minC) / 2f
        if (delta == 0f) return Hsl(0f, 0f, l)
        val s = delta / (1f - abs(2f * l - 1f))
        var h = when (maxC) {
            r -> 60f * (((g - b) / delta + 6f) % 6f)
            g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
        if (h >= 360f) h -= 360f
        return Hsl(h, s, l)
    }

    /** Builds an opaque ARGB int from three 0..255 channels. */
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    /** The red channel, 0..255. */
    private fun red(argb: Int): Int = (argb shr 16) and 0xFF

    /** The green channel, 0..255. */
    private fun green(argb: Int): Int = (argb shr 8) and 0xFF

    /** The blue channel, 0..255. */
    private fun blue(argb: Int): Int = argb and 0xFF
}
