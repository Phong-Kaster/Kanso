package com.phongkaster.kanso.a11y

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.seedColorScheme
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.curveLineColorFor
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks that the Curves graph's line reads clearly in the light and in the dark theme: in each of
 * the five seeds `RgbTintContrastTest` uses, every channel's line colour reaches WCAG's 3:1 for
 * graphics against the graph's own background, `surfaceContainerLowest`.
 *
 * The line colour comes from `curveLineColorFor` in `CurvesPanel.kt`, the same plain function the
 * composable `curveLineColor` calls, so the test cannot drift from the app. If a seed cannot reach 3:1
 * the test fails and names it; the threshold is never lowered.
 *
 * The contrast ratio is (L1 + 0.05) / (L2 + 0.05), as in `RgbTintContrastTest`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CurveLineContrastTest {

    /** In every light scheme, each channel's line stands out 3:1 from the graph's background. */
    @Test
    fun light_curve_lines_reach_3_to_1_on_the_graph() {
        assertLinesReach(dark = false)
    }

    /** In every dark scheme, each channel's line stands out 3:1 from the graph's background. */
    @Test
    fun dark_curve_lines_reach_3_to_1_on_the_graph() {
        assertLinesReach(dark = true)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Fails unless every seed's every channel line reaches [GRAPHICS_RATIO]; lists each shortfall. */
    private fun assertLinesReach(dark: Boolean) {
        val failures = mutableListOf<String>()
        SEEDS.forEach(
            action = { (seedName, seed) ->
                val scheme = seedColorScheme(seed, dark)
                CurveChannel.entries.forEach(
                    action = { channel ->
                        val ratio = contrastRatio(curveLineColorFor(scheme, channel), scheme.surfaceContainerLowest)
                        if (ratio < GRAPHICS_RATIO) {
                            failures += "$seedName ${if (dark) "dark" else "light"}: $channel line on " +
                                "surfaceContainerLowest %.2f:1, needs %.1f:1".format(ratio, GRAPHICS_RATIO)
                        }
                    },
                )
            },
        )
        assertTrue("Curve lines below 3:1:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** Returns the WCAG contrast ratio of [a] and [b]: the lighter's luminance over the darker's. */
    private fun contrastRatio(a: Color, b: Color): Double {
        val first = relativeLuminance(a)
        val second = relativeLuminance(b)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }

    /** Returns the WCAG relative luminance of [colour], from 0 (black) to 1 (white). */
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

        /** Kanso blue and four custom seeds, the same five as `RgbTintContrastTest`. */
        val SEEDS = listOf(
            "Kanso blue" to KANSO_BLUE_SEED,
            "red seed" to Color(0xFFE53935),
            "green seed" to Color(0xFF43A047),
            "yellow seed" to Color(0xFFFDD835),
            "purple seed" to Color(0xFF8E24AA),
        )
    }
}
