package com.phongkaster.kanso.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks that KansoTheme hands its content a light or dark Material 3 colour scheme according to
 * the theme mode, and that System follows the phone's night setting (Robolectric's "night" and
 * "notnight" resource qualifiers).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class KansoThemeTest {

    /** Hosts the composable under test in an empty activity. */
    @get:Rule
    val compose = createComposeRule()

    /** Light gives a light background. */
    @Test
    fun light_mode_gives_a_light_background() {
        assertLight(backgroundFor(ThemeMode.Light))
    }

    /** Dark gives a dark background. */
    @Test
    fun dark_mode_gives_a_dark_background() {
        assertDark(backgroundFor(ThemeMode.Dark))
    }

    /** Light stays light even when the phone is in night mode. */
    @Test
    @Config(qualifiers = "night")
    fun light_mode_ignores_a_night_phone() {
        assertLight(backgroundFor(ThemeMode.Light))
    }

    /** Dark stays dark even when the phone is in day mode. */
    @Test
    @Config(qualifiers = "notnight")
    fun dark_mode_ignores_a_day_phone() {
        assertDark(backgroundFor(ThemeMode.Dark))
    }

    /** System on a phone in night mode gives a dark background. */
    @Test
    @Config(qualifiers = "night")
    fun system_mode_on_a_night_phone_is_dark() {
        assertDark(backgroundFor(ThemeMode.System))
    }

    /** System on a phone in day mode gives a light background. */
    @Test
    @Config(qualifiers = "notnight")
    fun system_mode_on_a_day_phone_is_light() {
        assertLight(backgroundFor(ThemeMode.System))
    }

    /** Shows KansoTheme with [mode] and returns the background colour its content sees. */
    private fun backgroundFor(mode: ThemeMode): Color {
        var background = Color.Unspecified
        compose.setContent {
            KansoTheme(themeMode = mode) {
                background = MaterialTheme.colorScheme.background
            }
        }
        compose.waitForIdle()
        assertTrue("The themed content was never composed", background != Color.Unspecified)
        return background
    }

    /** Fails unless [color] is light (relative luminance above one half). */
    private fun assertLight(color: Color) {
        assertTrue("Expected a light background, got $color", color.luminance() > 0.5f)
    }

    /** Fails unless [color] is dark (relative luminance below one half). */
    private fun assertDark(color: Color) {
        assertTrue("Expected a dark background, got $color", color.luminance() < 0.5f)
    }
}
