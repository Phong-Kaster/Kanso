package com.phongkaster.kanso.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.seedColorScheme
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks that text, icons and outlines are readable on Kanso blue, in the light and in the dark
 * scheme (DoD 17), with the WCAG contrast ratio: (L1 + 0.05) / (L2 + 0.05), where L1 and L2
 * are the relative luminances of the lighter and the darker colour. It runs from 1 (the same
 * colour) to 21 (black on white).
 *
 * - At least 4.5 (WCAG's level for normal text) for every "on" colour on the colour it belongs
 *   to, and for `onSurfaceVariant` on the surface and on every surface container.
 * - At least 3.0 (WCAG's level for icons, outlines and large text) for the icon and outline
 *   pairs the screens use: `primary`, `onSurfaceVariant` and `outline` on the surface, and
 *   `onSecondaryContainer` on `secondaryContainer`.
 * - The floating tab bar's pairs (DoD 22): its selected pill on the bar at 3.0, and the selected
 *   icon on the pill and every label on the bar at 4.5.
 *
 * The luminance is worked out here from the 8-bit sRGB channels, so the test does not rest on
 * any library's own rounding. Each failure names the pair, the look and the ratio found.
 * Robolectric only stands in for the Android classes the Compose colour code may touch.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class KansoBlueContrastTest {

    /** In the light scheme, text pairs reach 4.5:1. */
    @Test
    fun light_text_pairs_reach_4_5_to_1() {
        assertPairsReach(textPairs(scheme(dark = false)), TEXT_RATIO, look = "light")
    }

    /** In the dark scheme, text pairs reach 4.5:1. */
    @Test
    fun dark_text_pairs_reach_4_5_to_1() {
        assertPairsReach(textPairs(scheme(dark = true)), TEXT_RATIO, look = "dark")
    }

    /** In the light scheme, icon and outline pairs reach 3:1. */
    @Test
    fun light_icon_and_outline_pairs_reach_3_to_1() {
        assertPairsReach(iconPairs(scheme(dark = false)), ICON_RATIO, look = "light")
    }

    /** In the dark scheme, icon and outline pairs reach 3:1. */
    @Test
    fun dark_icon_and_outline_pairs_reach_3_to_1() {
        assertPairsReach(iconPairs(scheme(dark = true)), ICON_RATIO, look = "dark")
    }

    /**
     * DoD 22, in the light scheme: on the floating tab bar, the selected tab's pill stands out on
     * the bar (3:1), and the selected icon on the pill and every label on the bar reach 4.5:1.
     */
    @Test
    fun light_tab_bar_pairs_reach_their_minimums() {
        val scheme = scheme(dark = false)
        assertPairsReach(tabBarTextPairs(scheme), TEXT_RATIO, look = "light")
        assertPairsReach(tabBarGraphicPairs(scheme), ICON_RATIO, look = "light")
    }

    /** DoD 22, in the dark scheme: the same tab bar pairs as in light. */
    @Test
    fun dark_tab_bar_pairs_reach_their_minimums() {
        val scheme = scheme(dark = true)
        assertPairsReach(tabBarTextPairs(scheme), TEXT_RATIO, look = "dark")
        assertPairsReach(tabBarGraphicPairs(scheme), ICON_RATIO, look = "dark")
    }

    /**
     * Returns the pairs of the floating tab bar (`KansoTabBar`) that must reach the text level:
     * the selected icon (`inverseSurface`) on the selected pill (`inversePrimary`), the selected
     * label (`inversePrimary`) on the bar (`inverseSurface`), and the other tabs' icons and labels
     * (`inverseOnSurface`) on the bar.
     */
    private fun tabBarTextPairs(scheme: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("tab bar: selected icon (inverseSurface) on the pill (inversePrimary)", scheme.inverseSurface, scheme.inversePrimary),
        Triple("tab bar: selected label (inversePrimary) on the bar (inverseSurface)", scheme.inversePrimary, scheme.inverseSurface),
        Triple(
            "tab bar: unselected icons and labels (inverseOnSurface) on the bar (inverseSurface)",
            scheme.inverseOnSurface,
            scheme.inverseSurface,
        ),
    )

    /**
     * Returns the pair of the floating tab bar that must reach the graphics level: the selected
     * tab's pill (`inversePrimary`) on the bar (`inverseSurface`).
     */
    private fun tabBarGraphicPairs(scheme: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("tab bar: the selected pill (inversePrimary) on the bar (inverseSurface)", scheme.inversePrimary, scheme.inverseSurface),
    )

    /** Returns Kanso blue in the [dark] or light look, as KansoTheme hands it to the screens. */
    private fun scheme(dark: Boolean): ColorScheme = seedColorScheme(KANSO_BLUE_SEED, dark)

    /**
     * Returns the pairs that carry text in [scheme]: each as its name, the foreground colour
     * and the colour it is drawn on.
     */
    private fun textPairs(scheme: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("onPrimary on primary", scheme.onPrimary, scheme.primary),
        Triple("onPrimaryContainer on primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer),
        Triple("onSecondary on secondary", scheme.onSecondary, scheme.secondary),
        Triple(
            "onSecondaryContainer on secondaryContainer",
            scheme.onSecondaryContainer,
            scheme.secondaryContainer,
        ),
        Triple("onTertiary on tertiary", scheme.onTertiary, scheme.tertiary),
        Triple(
            "onTertiaryContainer on tertiaryContainer",
            scheme.onTertiaryContainer,
            scheme.tertiaryContainer,
        ),
        Triple("onError on error", scheme.onError, scheme.error),
        Triple("onErrorContainer on errorContainer", scheme.onErrorContainer, scheme.errorContainer),
        Triple("onSurface on surface", scheme.onSurface, scheme.surface),
        Triple("inverseOnSurface on inverseSurface", scheme.inverseOnSurface, scheme.inverseSurface),
        Triple("onSurfaceVariant on surface", scheme.onSurfaceVariant, scheme.surface),
        Triple(
            "onSurfaceVariant on surfaceContainerLowest",
            scheme.onSurfaceVariant,
            scheme.surfaceContainerLowest,
        ),
        Triple("onSurfaceVariant on surfaceContainerLow", scheme.onSurfaceVariant, scheme.surfaceContainerLow),
        Triple("onSurfaceVariant on surfaceContainer", scheme.onSurfaceVariant, scheme.surfaceContainer),
        Triple("onSurfaceVariant on surfaceContainerHigh", scheme.onSurfaceVariant, scheme.surfaceContainerHigh),
        Triple(
            "onSurfaceVariant on surfaceContainerHighest",
            scheme.onSurfaceVariant,
            scheme.surfaceContainerHighest,
        ),
    )

    /** Returns the icon and outline pairs of [scheme], in the same form as [textPairs]. */
    private fun iconPairs(scheme: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("primary on surface", scheme.primary, scheme.surface),
        Triple("onSurfaceVariant on surface", scheme.onSurfaceVariant, scheme.surface),
        Triple(
            "onSecondaryContainer on secondaryContainer",
            scheme.onSecondaryContainer,
            scheme.secondaryContainer,
        ),
        Triple("outline on surface", scheme.outline, scheme.surface),
    )

    /**
     * Fails unless every pair in [pairs] has a contrast ratio of at least [minimum], listing
     * every pair that falls short, with its ratio, in the [look] named.
     */
    private fun assertPairsReach(pairs: List<Triple<String, Color, Color>>, minimum: Double, look: String) {
        val failures = pairs.mapNotNull { (name, foreground, background) ->
            val ratio = contrastRatio(foreground, background)
            if (ratio >= minimum) {
                null
            } else {
                "$name ($look): %.2f:1, needs %.1f:1".format(ratio, minimum)
            }
        }
        assertTrue(
            "Kanso blue pairs below the contrast minimum:\n" + failures.joinToString("\n"),
            failures.isEmpty(),
        )
    }

    /** Returns the WCAG contrast ratio of [a] and [b]: the lighter's luminance over the darker's. */
    private fun contrastRatio(a: Color, b: Color): Double {
        val first = relativeLuminance(a)
        val second = relativeLuminance(b)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }

    /**
     * Returns the WCAG relative luminance of [colour], from 0 (black) to 1 (white): each 8-bit
     * sRGB channel is turned back into linear light, then the three are weighted by how bright
     * the eye finds red, green and blue.
     */
    private fun relativeLuminance(colour: Color): Double {
        val argb = colour.toArgb()
        val red = linearChannel((argb shr 16) and 0xFF)
        val green = linearChannel((argb shr 8) and 0xFF)
        val blue = linearChannel(argb and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    /**
     * Undoes the sRGB gamma curve on one 8-bit [channel] (0 to 255), giving its linear light
     * from 0 to 1: a straight line for the darkest values, a 2.4 power curve above.
     */
    private fun linearChannel(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    /** The contrast minimums the tests use. */
    private companion object {
        /** WCAG AA for normal text. */
        const val TEXT_RATIO = 4.5

        /** WCAG AA for icons, outlines and other graphics, and for large text. */
        const val ICON_RATIO = 3.0
    }
}
