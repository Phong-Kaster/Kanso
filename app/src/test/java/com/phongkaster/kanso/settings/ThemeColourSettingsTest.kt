package com.phongkaster.kanso.settings

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.ui.settings.CUSTOM_COLOUR_PICKER_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_BASELINE_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_WALLPAPER_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_DARK_TAG
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.seedColorScheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.robolectric.annotation.Config

/**
 * Drives the real app through Settings -> Appearance -> Theme colour: the wallpaper option is
 * offered only on Android 12 and later, choosing it sets the colour source to Wallpaper, the
 * Custom option shows the colour picker, and a custom colour set through the picker's callback
 * re-themes Settings and Home at once, in light and in dark. Surviving a restart is checked in
 * ThemeColourRestartTest.
 *
 * The colours are read from the scheme KansoTheme publishes on its semantics node, which is
 * exactly what `MaterialTheme.colorScheme` returns on every screen below it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "notnight")
class ThemeColourSettingsTest {

    /** Launches MainActivity before each test and lets the test find and tap its Compose nodes. */
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** The app context, used to read the same string resources the screens show. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test. Koin's state outlives the test's Application inside the same
     * JVM, and starting it twice fails.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** On Android 11 the colour section has no wallpaper option, and the baseline is in use. */
    @Test
    @Config(sdk = [30])
    fun android_11_offers_no_wallpaper_option() {
        waitForHome()
        assertEquals(BASELINE_PURPLE, appliedScheme().primary)

        openSettings()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().assertIsDisplayed()
        assertTrue(
            "The wallpaper option must not be offered below Android 12",
            compose.onAllNodesWithTag(THEME_COLOUR_WALLPAPER_TAG).fetchSemanticsNodes().isEmpty(),
        )
        compose.onNodeWithTag(THEME_COLOUR_BASELINE_TAG).performScrollTo().assertIsSelected()
    }

    /**
     * On Android 15 the wallpaper option is offered and selected by default; choosing Custom
     * shows the picker, and choosing the wallpaper again sets the source to Wallpaper and
     * applies the system's dynamic scheme.
     */
    @Test
    fun android_15_offers_the_wallpaper_option_and_choosing_it_sets_wallpaper() {
        waitForHome()
        openSettings()
        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).performScrollTo().assertIsSelected()
        assertTrue(
            "The baseline option is only for phones without wallpaper colours",
            compose.onAllNodesWithTag(THEME_COLOUR_BASELINE_TAG).fetchSemanticsNodes().isEmpty(),
        )

        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().performClick()
        waitForPicker()
        compose.onNodeWithTag(CUSTOM_COLOUR_PICKER_TAG).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsSelected()

        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).performScrollTo().performClick()
        awaitSavedThemeColour(ThemeColour.Wallpaper)
        compose.waitUntil(timeoutMillis = 10_000) {
            appliedScheme().primary == dynamicLightColorScheme(context).primary
        }
        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).assertIsSelected()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsNotSelected()
        assertTrue(
            "The picker is shown only while Custom is selected",
            compose.onAllNodesWithTag(CUSTOM_COLOUR_PICKER_TAG).fetchSemanticsNodes().isEmpty(),
        )
    }

    /**
     * In light mode, the custom colour set through the picker's callback gives Settings the
     * generated light scheme at once, and Home has it after Back, with no restart.
     */
    @Test
    fun custom_colour_applies_at_once_in_light_mode() {
        val expected = seedColorScheme(GREEN, dark = false).primary
        waitForHome()
        openSettings()

        pickCustomColour()
        compose.waitUntil(timeoutMillis = 10_000) { appliedScheme().primary == expected }
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(CUSTOM_COLOUR_PICKER_TAG).assertExists()

        Espresso.pressBack()
        waitForHomeOnly()
        assertEquals(expected, appliedScheme().primary)
    }

    /**
     * In dark mode, the custom colour set through the picker's callback gives Settings the
     * generated dark scheme at once, and Home has it after Back, with no restart.
     */
    @Test
    fun custom_colour_applies_at_once_in_dark_mode() {
        val expected = seedColorScheme(GREEN, dark = true).primary
        waitForHome()
        openSettings()
        compose.onNodeWithTag(THEME_MODE_DARK_TAG).performScrollTo().performClick()

        pickCustomColour()
        compose.waitUntil(timeoutMillis = 10_000) { appliedScheme().primary == expected }

        Espresso.pressBack()
        waitForHomeOnly()
        assertEquals(expected, appliedScheme().primary)
        assertEquals(seedColorScheme(GREEN, dark = true).background, appliedScheme().background)
    }

    /**
     * Sets the custom colour #2E7D32 through SettingsViewModel's onCustomColourChosen, the same
     * function the picker's onColorChanged calls (the picker itself is not dragged).
     */
    private fun pickCustomColour() {
        SettingsViewModel(repository()).onCustomColourChosen(GREEN)
    }

    /** Returns the colour scheme KansoTheme currently hands to every screen. */
    private fun appliedScheme(): ColorScheme =
        compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode().config[KansoColorSchemeKey]

    /** Reads the theme colour saved in the settings file right now. */
    private fun savedThemeColour(): ThemeColour = runBlocking { repository().themeColour.first() }

    /**
     * Waits, in real time, until the saved theme colour is [expected]. The write runs on a
     * background thread, which Compose's test clock does not wait for.
     */
    private fun awaitSavedThemeColour(expected: ThemeColour) {
        val deadline = System.currentTimeMillis() + 10_000
        while (savedThemeColour() != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            compose.waitForIdle()
        }
        assertEquals(expected, savedThemeColour())
    }

    /** Returns the app's one settings repository from Koin. */
    private fun repository(): SettingsRepository = KoinPlatform.getKoin().get()

    /** Waits until the colour picker is on screen. */
    private fun waitForPicker() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(CUSTOM_COLOUR_PICKER_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Taps the Settings row on Home and waits until the Settings screen is shown. */
    private fun openSettings() {
        compose.onNodeWithText(context.getString(R.string.settings))
            .performScrollTo()
            .performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home's title is on screen. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home is shown and the Settings screen has left the screen. */
    private fun waitForHomeOnly() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isEmpty()
        }
    }

    /** Colours the tests use. */
    private companion object {
        /** The custom colour of the DoD: green #2E7D32. */
        val GREEN = Color(0xFF2E7D32)

        /** The Material 3 baseline primary, purple #6750A4. */
        val BASELINE_PURPLE = Color(0xFF6750A4)
    }
}
