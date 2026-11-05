package com.phongkaster.kanso.a11y

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.seedColorScheme
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow.markColours
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.displayColour
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks DoD 22 of Run 11: the mark on the chosen HSL circle stands out against all nine colours,
 * in the light and in the dark theme. The mark has two parts, picked by `markColours`: the ring
 * drawn round the disc and the check drawn on it. Each reaches WCAG's 3:1 for graphics against the
 * circle's fill (`ColourRange.displayColour`), for every range.
 *
 * Kanso blue is checked, and four custom seed colours from Settings' colour picker (a red, a
 * green, a yellow and a purple), since a custom seed tints the theme's `surface` and `onSurface`.
 * The wallpaper scheme comes from the phone and cannot be checked here. The check and the ring are
 * separate tests, so a failure says which part of the mark is too faint.
 *
 * The WCAG contrast ratio is (L1 + 0.05) / (L2 + 0.05), L1 the lighter colour's relative
 * luminance, worked out here from the 8-bit sRGB channels exactly as `RgbTintContrastTest` does.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ColourRangeMarkContrastTest {

    /** In every light scheme the check mark stands out 3:1 from each of the nine fills. */
    @Test
    fun light_check_mark_reaches_3_to_1_on_all_nine_colours() {
        assertMarkReaches(dark = false, part = MarkPart.CHECK)
    }

    /** In every dark scheme the check mark stands out 3:1 from each of the nine fills. */
    @Test
    fun dark_check_mark_reaches_3_to_1_on_all_nine_colours() {
        assertMarkReaches(dark = true, part = MarkPart.CHECK)
    }

    /** In every light scheme the ring stands out 3:1 from each of the nine fills. */
    @Test
    fun light_ring_reaches_3_to_1_on_all_nine_colours() {
        assertMarkReaches(dark = false, part = MarkPart.RING)
    }

    /** In every dark scheme the ring stands out 3:1 from each of the nine fills. */
    @Test
    fun dark_ring_reaches_3_to_1_on_all_nine_colours() {
        assertMarkReaches(dark = true, part = MarkPart.RING)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** The two parts of the mark on a chosen circle. */
    private enum class MarkPart { RING, CHECK }

    /**
     * Fails unless, in every checked scheme of the [dark] or light look, the [part] of the mark
     * reaches [GRAPHICS_RATIO] against each range's fill; lists every pair that falls short.
     */
    private fun assertMarkReaches(dark: Boolean, part: MarkPart) {
        val failures = mutableListOf<String>()
        SEEDS.forEach(
            action = { (seedName, seed) ->
                val scheme = seedColorScheme(seed, dark)
                ColourRange.entries.forEach(
                    action = { range ->
                        val fill = range.displayColour()
                        val (ring, check) = markColours(scheme, fill)
                        val mark = if (part == MarkPart.RING) ring else check
                        val ratio = contrastRatio(mark, fill)
                        if (ratio < GRAPHICS_RATIO) {
                            failures += "$seedName ${if (dark) "dark" else "light"}: the $part on $range " +
                                "%.2f:1, needs %.1f:1".format(ratio, GRAPHICS_RATIO)
                        }
                    },
                )
            },
        )
        assertTrue("HSL $part below 3:1:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** Returns the WCAG contrast ratio of [a] and [b]: the lighter's luminance over the darker's. */
    private fun contrastRatio(a: Color, b: Color): Double {
        val first = relativeLuminance(a)
        val second = relativeLuminance(b)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }

    /**
     * Returns the WCAG relative luminance of [colour], from 0 (black) to 1 (white): each 8-bit
     * sRGB channel is turned back into linear light, then weighted by how bright the eye finds it.
     */
    private fun relativeLuminance(colour: Color): Double {
        val argb = colour.toArgb()
        val red = linearChannel((argb shr 16) and 0xFF)
        val green = linearChannel((argb shr 8) and 0xFF)
        val blue = linearChannel(argb and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    /** Undoes the sRGB gamma curve on one 8-bit [channel], giving its linear light from 0 to 1. */
    private fun linearChannel(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    /** The schemes checked and the contrast minimum. */
    private companion object {
        /** WCAG AA for icons, outlines and other graphics. */
        const val GRAPHICS_RATIO = 3.0

        /** Kanso blue and four custom seeds, each with a name for the failure message. */
        val SEEDS = listOf(
            "Kanso blue" to KANSO_BLUE_SEED,
            "red seed" to Color(0xFFE53935),
            "green seed" to Color(0xFF43A047),
            "yellow seed" to Color(0xFFFDD835),
            "purple seed" to Color(0xFF8E24AA),
        )
    }
}
