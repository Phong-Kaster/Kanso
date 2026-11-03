package com.phongkaster.kanso.a11y

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.seedColorScheme
import com.phongkaster.kanso.ui.tool.model.RgbChannel
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks that the RGB tool's three tints (plan `docs/plans/rgb-tool.md`, decision 7) read clearly
 * on the Edit sheet in the light and in the dark theme: each channel's slider track, thumb and
 * chip dot, in the tint [RgbChannel.tintOn] picks for the theme, reaches WCAG's 3:1 for graphics
 * against the sheet (`surfaceContainerHigh`) and against the screen's `surface`.
 *
 * Kanso blue is checked, and four custom seed colours from Settings' colour picker (a red, a
 * green, a yellow and a purple), since a custom seed tints every surface. The wallpaper scheme
 * comes from the phone and cannot be checked here.
 *
 * The WCAG contrast ratio is (L1 + 0.05) / (L2 + 0.05), L1 the lighter colour's relative
 * luminance, worked out here from the 8-bit sRGB channels as in `KansoBlueContrastTest`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RgbTintContrastTest {

    /** In every light scheme, each tint stands out 3:1 from the sheet and the surface. */
    @Test
    fun light_tints_reach_3_to_1_on_the_sheet() {
        assertTintsReach(dark = false)
    }

    /** In every dark scheme, each tint stands out 3:1 from the sheet and the surface. */
    @Test
    fun dark_tints_reach_3_to_1_on_the_sheet() {
        assertTintsReach(dark = true)
    }

    /** A light scheme picks each channel's light tint, a dark scheme its dark tint. */
    @Test
    fun the_tint_follows_the_theme() {
        RgbChannel.entries.forEach(
            action = { channel ->
                assertEquals(channel.lightTint, channel.tintOn(seedColorScheme(KANSO_BLUE_SEED, dark = false).surface))
                assertEquals(channel.darkTint, channel.tintOn(seedColorScheme(KANSO_BLUE_SEED, dark = true).surface))
            },
        )
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Fails unless, in every checked scheme of the [dark] or light look, each channel's tint
     * reaches [GRAPHICS_RATIO] against the sheet and the surface; lists every pair that falls short.
     */
    private fun assertTintsReach(dark: Boolean) {
        val failures = mutableListOf<String>()
        SEEDS.forEach(
            action = { (seedName, seed) ->
                val scheme = seedColorScheme(seed, dark)
                RgbChannel.entries.forEach(
                    action = { channel ->
                        val tint = channel.tintOn(scheme.surface)
                        backgrounds(scheme).forEach(
                            action = { (backgroundName, background) ->
                                val ratio = contrastRatio(tint, background)
                                if (ratio < GRAPHICS_RATIO) {
                                    failures += "$seedName ${if (dark) "dark" else "light"}: $channel on " +
                                        "$backgroundName %.2f:1, needs %.1f:1".format(ratio, GRAPHICS_RATIO)
                                }
                            },
                        )
                    },
                )
            },
        )
        assertTrue("RGB tints below 3:1:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** The colours an RGB tint is drawn on in [scheme]: the sheet and the screen's surface. */
    private fun backgrounds(scheme: ColorScheme): List<Pair<String, Color>> = listOf(
        "the sheet (surfaceContainerHigh)" to scheme.surfaceContainerHigh,
        "surface" to scheme.surface,
    )

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
