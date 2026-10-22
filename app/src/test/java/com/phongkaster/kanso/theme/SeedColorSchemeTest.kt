package com.phongkaster.kanso.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.ui.theme.seedColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks the seed -> colour scheme generator with the custom colour of DoD 15 (green #2E7D32):
 * every Material 3 role is set, the four accent roles differ, every "on" colour is readable on
 * the colour it belongs to, and the result is not the baseline purple.
 *
 * The generator is plain calculation; Robolectric only stands in for the Android classes the
 * Compose colour code may touch on the JVM.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SeedColorSchemeTest {

    /** The light scheme sets every role, and its accents differ from each other. */
    @Test
    fun light_scheme_sets_every_role_with_distinct_accents() {
        val scheme = seedColorScheme(GREEN, dark = false)
        assertEveryRoleSet(scheme)
        assertDistinctAccents(scheme)
    }

    /** A picked colour is the primary exactly, in light and in dark, with white text on Coca-Cola red. */
    @Test
    fun exact_primary_is_the_picked_colour() {
        val cocaColaRed = Color(0xFFF40009)
        for (dark in listOf(false, true)) {
            val scheme = seedColorScheme(cocaColaRed, dark = dark, exactPrimary = true)
            assertEquals(cocaColaRed, scheme.primary)
            assertEquals(Color.White, scheme.onPrimary)
        }
    }

    /** A very light pick gets dark text, so the label on a button stays readable. */
    @Test
    fun exact_primary_gets_dark_text_on_a_light_colour() {
        val scheme = seedColorScheme(Color(0xFFFFEB3B), dark = false, exactPrimary = true)
        assertEquals(Color(0xFFFFEB3B), scheme.primary)
        assertTrue(scheme.onPrimary.luminance() < 0.1f)
    }

    /** The dark scheme sets every role, and its accents differ from each other. */
    @Test
    fun dark_scheme_sets_every_role_with_distinct_accents() {
        val scheme = seedColorScheme(GREEN, dark = true)
        assertEveryRoleSet(scheme)
        assertDistinctAccents(scheme)
    }

    /** In the light scheme every "on" colour has at least 3:1 contrast with its pair. */
    @Test
    fun light_scheme_on_colours_are_readable() {
        assertOnColoursReadable(seedColorScheme(GREEN, dark = false))
    }

    /** In the dark scheme every "on" colour has at least 3:1 contrast with its pair. */
    @Test
    fun dark_scheme_on_colours_are_readable() {
        assertOnColoursReadable(seedColorScheme(GREEN, dark = true))
    }

    /** The green seed gives a light primary that is not the baseline purple, and is green-ish. */
    @Test
    fun light_primary_is_not_the_baseline_purple() {
        val primary = seedColorScheme(GREEN, dark = false).primary
        assertNotEquals(BASELINE_PURPLE, primary)
        assertTrue("Expected a green primary, got $primary", primary.green > primary.red)
        assertTrue("Expected a green primary, got $primary", primary.green > primary.blue)
    }

    /** The light and dark schemes differ, and the same seed always gives the same scheme. */
    @Test
    fun schemes_are_pure_and_differ_between_light_and_dark() {
        val light = seedColorScheme(GREEN, dark = false)
        val dark = seedColorScheme(GREEN, dark = true)
        assertNotEquals(light.background, dark.background)
        assertTrue("Light background should be light", light.background.luminance() > 0.5f)
        assertTrue("Dark background should be dark", dark.background.luminance() < 0.5f)
        assertEquals(light.primary, seedColorScheme(GREEN, dark = false).primary)
        assertEquals(dark.primary, seedColorScheme(GREEN, dark = true).primary)
    }

    /** A see-through seed gives the same scheme as the same colour fully opaque. */
    @Test
    fun seed_transparency_is_ignored() {
        assertEquals(
            seedColorScheme(GREEN, dark = false).primary,
            seedColorScheme(GREEN.copy(alpha = 0.3f), dark = false).primary,
        )
    }

    /** Fails unless every Material 3 colour role of [scheme] holds an opaque colour. */
    private fun assertEveryRoleSet(scheme: ColorScheme) {
        rolesOf(scheme).forEach { (name, color) ->
            assertTrue("Role $name is not set", color != Color.Unspecified)
            assertTrue("Role $name is not opaque: $color", color.alpha > 0.99f)
        }
    }

    /** Fails if any two of primary, secondary, tertiary and error of [scheme] are equal. */
    private fun assertDistinctAccents(scheme: ColorScheme) {
        val accents = listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.error)
        assertEquals("Accent roles must all differ: $accents", 4, accents.toSet().size)
    }

    /** Fails unless each "on" role of [scheme] has a contrast ratio of at least 3:1 with its pair. */
    private fun assertOnColoursReadable(scheme: ColorScheme) {
        onPairsOf(scheme).forEach { (name, pair) ->
            val ratio = contrast(pair.first, pair.second)
            assertTrue("$name contrast is only $ratio:1", ratio >= 3.0)
        }
    }

    /** Returns every colour role of [scheme] with its name. */
    private fun rolesOf(scheme: ColorScheme): List<Pair<String, Color>> = listOf(
        "primary" to scheme.primary,
        "onPrimary" to scheme.onPrimary,
        "primaryContainer" to scheme.primaryContainer,
        "onPrimaryContainer" to scheme.onPrimaryContainer,
        "inversePrimary" to scheme.inversePrimary,
        "secondary" to scheme.secondary,
        "onSecondary" to scheme.onSecondary,
        "secondaryContainer" to scheme.secondaryContainer,
        "onSecondaryContainer" to scheme.onSecondaryContainer,
        "tertiary" to scheme.tertiary,
        "onTertiary" to scheme.onTertiary,
        "tertiaryContainer" to scheme.tertiaryContainer,
        "onTertiaryContainer" to scheme.onTertiaryContainer,
        "background" to scheme.background,
        "onBackground" to scheme.onBackground,
        "surface" to scheme.surface,
        "onSurface" to scheme.onSurface,
        "surfaceVariant" to scheme.surfaceVariant,
        "onSurfaceVariant" to scheme.onSurfaceVariant,
        "surfaceTint" to scheme.surfaceTint,
        "inverseSurface" to scheme.inverseSurface,
        "inverseOnSurface" to scheme.inverseOnSurface,
        "error" to scheme.error,
        "onError" to scheme.onError,
        "errorContainer" to scheme.errorContainer,
        "onErrorContainer" to scheme.onErrorContainer,
        "outline" to scheme.outline,
        "outlineVariant" to scheme.outlineVariant,
        "scrim" to scheme.scrim,
        "surfaceBright" to scheme.surfaceBright,
        "surfaceDim" to scheme.surfaceDim,
        "surfaceContainer" to scheme.surfaceContainer,
        "surfaceContainerHigh" to scheme.surfaceContainerHigh,
        "surfaceContainerHighest" to scheme.surfaceContainerHighest,
        "surfaceContainerLow" to scheme.surfaceContainerLow,
        "surfaceContainerLowest" to scheme.surfaceContainerLowest,
    )

    /** Returns each "on" role of [scheme] paired with the colour it is drawn on. */
    private fun onPairsOf(scheme: ColorScheme): List<Pair<String, Pair<Color, Color>>> = listOf(
        "onPrimary" to (scheme.onPrimary to scheme.primary),
        "onPrimaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
        "onSecondary" to (scheme.onSecondary to scheme.secondary),
        "onSecondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
        "onTertiary" to (scheme.onTertiary to scheme.tertiary),
        "onTertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
        "onError" to (scheme.onError to scheme.error),
        "onErrorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
        "onBackground" to (scheme.onBackground to scheme.background),
        "onSurface" to (scheme.onSurface to scheme.surface),
        "onSurfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
        "inverseOnSurface" to (scheme.inverseOnSurface to scheme.inverseSurface),
    )

    /** Returns the WCAG contrast ratio of colours [a] and [b], from 1 (same) to 21. */
    private fun contrast(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance()).toDouble()
        val darker = minOf(a.luminance(), b.luminance()).toDouble()
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Colours the tests use. */
    private companion object {
        /** The custom colour of the DoD: green #2E7D32. */
        val GREEN = Color(0xFF2E7D32)

        /** The Material 3 baseline primary, purple #6750A4. */
        val BASELINE_PURPLE = Color(0xFF6750A4)
    }
}
