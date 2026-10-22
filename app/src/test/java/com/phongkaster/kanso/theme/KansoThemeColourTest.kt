package com.phongkaster.kanso.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.theme.seedColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks that KansoTheme maps each theme colour to the right scheme: the custom colour to the
 * generated scheme, Kanso blue to the scheme generated from KANSO_BLUE_SEED, and the wallpaper
 * to the system's dynamic schemes on Android 12 and later (and to Kanso blue below), in light
 * and in dark.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class KansoThemeColourTest {

    /** Hosts the composable under test in an empty activity. */
    @get:Rule
    val compose = createComposeRule()

    /** The app context, which the dynamic (wallpaper) schemes are read from. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** A custom colour gives its generated light scheme in light mode. */
    @Test
    fun custom_colour_gives_the_generated_light_scheme() {
        val primary = primaryFor(ThemeMode.Light, ThemeColour.Custom(GREEN_ARGB))
        assertEquals(seedColorScheme(Color(GREEN_ARGB), dark = false, exactPrimary = true).primary, primary)
        assertTrue(
            "Expected a primary other than the default Kanso blue",
            primary != seedColorScheme(KANSO_BLUE_SEED, dark = false).primary,
        )
    }

    /** A custom colour gives its generated dark scheme in dark mode. */
    @Test
    fun custom_colour_gives_the_generated_dark_scheme() {
        val primary = primaryFor(ThemeMode.Dark, ThemeColour.Custom(GREEN_ARGB))
        assertEquals(seedColorScheme(Color(GREEN_ARGB), dark = true, exactPrimary = true).primary, primary)
    }

    /** The Kanso blue choice gives the light scheme generated from Kanso blue's seed. */
    @Test
    fun kanso_blue_gives_the_generated_light_scheme() {
        assertEquals(
            seedColorScheme(KANSO_BLUE_SEED, dark = false).primary,
            primaryFor(ThemeMode.Light, ThemeColour.KansoBlue),
        )
    }

    /** The Kanso blue choice gives the dark scheme generated from Kanso blue's seed in dark mode. */
    @Test
    fun kanso_blue_gives_the_generated_dark_scheme() {
        assertEquals(
            seedColorScheme(KANSO_BLUE_SEED, dark = true).primary,
            primaryFor(ThemeMode.Dark, ThemeColour.KansoBlue),
        )
    }

    /** On Android 15 the wallpaper choice gives the system's dynamic schemes. */
    @Test
    fun wallpaper_gives_the_dynamic_schemes_on_android_12_and_later() {
        assertEquals(
            dynamicLightColorScheme(context).primary,
            primaryFor(ThemeMode.Light, ThemeColour.Wallpaper),
        )
    }

    /** On Android 15 in dark mode the wallpaper choice gives the dynamic dark scheme. */
    @Test
    fun wallpaper_gives_the_dynamic_dark_scheme_in_dark_mode() {
        assertEquals(
            dynamicDarkColorScheme(context).primary,
            primaryFor(ThemeMode.Dark, ThemeColour.Wallpaper),
        )
    }

    /** On Android 11, which has no wallpaper colours, the wallpaper choice falls back to Kanso blue. */
    @Test
    @Config(sdk = [30])
    fun wallpaper_falls_back_to_kanso_blue_below_android_12() {
        assertEquals(
            seedColorScheme(KANSO_BLUE_SEED, dark = false).primary,
            primaryFor(ThemeMode.Light, ThemeColour.Wallpaper),
        )
    }

    /** The scheme published on the theme's semantics node is the one its content reads. */
    @Test
    fun published_scheme_matches_what_the_content_reads() {
        var seen: ColorScheme? = null
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light, themeColour = ThemeColour.Custom(GREEN_ARGB)) {
                seen = MaterialTheme.colorScheme
            }
        }
        compose.waitForIdle()
        val published = compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode()
            .config[KansoColorSchemeKey]
        assertEquals(seen!!.primary, published.primary)
        assertEquals(seen!!.surfaceContainer, published.surfaceContainer)
    }

    /** Shows KansoTheme with [mode] and [colour] and returns the primary colour its content sees. */
    private fun primaryFor(mode: ThemeMode, colour: ThemeColour): Color {
        var primary = Color.Unspecified
        compose.setContent {
            KansoTheme(themeMode = mode, themeColour = colour) {
                primary = MaterialTheme.colorScheme.primary
            }
        }
        compose.waitForIdle()
        assertTrue("The themed content was never composed", primary != Color.Unspecified)
        return primary
    }

    /** Colours the tests use. */
    private companion object {
        /** The custom colour of the DoD, green #2E7D32, as ARGB. */
        val GREEN_ARGB = 0xFF2E7D32.toInt()
    }
}
