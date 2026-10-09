package com.phongkaster.kanso.settings

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.SETTINGS_SCOPE
import com.phongkaster.kanso.di.appModule
import com.phongkaster.kanso.ui.settings.CUSTOM_COLOUR_PICKER_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_KANSO_BLUE_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_WALLPAPER_TAG
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.seedColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.robolectric.annotation.Config

/**
 * Checks that both colour choices that are not the default survive a restart, on Android 15:
 *
 * - a custom colour: #2E7D32 is picked, and the new activity opens Home with the `primary`
 *   generated from it and Settings with Custom selected;
 * - the wallpaper choice: "From wallpaper" is tapped in Settings, and the new activity opens Home
 *   with the system's wallpaper `primary` and Settings with the wallpaper row selected and the
 *   Kanso blue row not selected.
 *
 * In each, between the two activities the first activity and the settings DataStore are shut
 * down and Koin is started afresh, as a new process would. Kanso blue, the default, is never
 * what either test expects, so a choice that was lost on the way shows up as Kanso blue and the
 * test fails.
 *
 * It launches activities itself (ActivityScenario) because it needs two of them in one test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "notnight")
class ThemeColourRestartTest {

    /** Lets the test find and tap Compose nodes in whatever activity the test launches. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The app context, used to read the same string resources the screens show. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after the test. Koin's state outlives the test's Application inside the same
     * JVM, and starting it twice fails.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** The custom colour is picked, the app is "restarted", and Home opens in its colours. */
    @Test
    fun custom_colour_survives_a_restart() {
        val expected = seedColorScheme(GREEN, dark = false).primary

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            openSettings()
            // The same function the colour picker's onColorChanged calls.
            SettingsViewModel(KoinPlatform.getKoin().get()).onCustomColourChosen(GREEN)
            // The theme only changes once the DataStore has written the file and re-read it.
            compose.waitUntil(timeoutMillis = 10_000) { appliedScheme().primary == expected }
        }

        // Release the settings file, then rebuild every singleton as a new process would.
        restartLikeANewProcess()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            // MainActivity reads the saved colour before its first frame, so this holds at once;
            // waiting a little only keeps a slow file read from failing the test.
            compose.waitUntil(timeoutMillis = 10_000) { appliedScheme().primary == expected }
            assertEquals(expected, appliedScheme().primary)

            openSettings()
            compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().assertIsSelected()
            compose.onNodeWithTag(CUSTOM_COLOUR_PICKER_TAG).assertExists()
        }
    }

    /**
     * "From wallpaper" is chosen in Settings, the app is "restarted", and Home opens in the
     * wallpaper's colours, with the wallpaper row still selected in Settings.
     *
     * The wallpaper scheme is the one the system builds, `dynamicLightColorScheme`, read here
     * exactly as ThemeColourSettingsTest reads it (the class runs in light mode, "notnight").
     */
    @Test
    fun wallpaper_choice_survives_a_restart() {
        val wallpaper = dynamicLightColorScheme(context).primary
        val kansoBlue = seedColorScheme(KANSO_BLUE_SEED, dark = false).primary
        // The app starts in Kanso blue. If the wallpaper's primary were the same colour, a lost
        // wallpaper choice would look kept and the checks below would prove nothing, so the
        // test fails here instead of passing for the wrong reason.
        assertNotEquals(
            "The wallpaper's primary must differ from Kanso blue's for this test to mean anything",
            kansoBlue,
            wallpaper,
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            openSettings()
            // Tapped like a person would; the row calls SettingsViewModel.onWallpaperChosen.
            compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).performScrollTo().performClick()
            // The theme only changes once the DataStore has written the file and re-read it, so
            // when the app's scheme turns to the wallpaper's (and so away from Kanso blue,
            // checked above) the choice is on disk.
            compose.waitUntil(timeoutMillis = 10_000) { appliedScheme().primary == wallpaper }
        }

        // Release the settings file, then rebuild every singleton as a new process would.
        restartLikeANewProcess()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            // MainActivity reads the saved colour before its first frame, so this holds at once;
            // waiting a little only keeps a slow file read from failing the test.
            compose.waitUntil(timeoutMillis = 10_000) { appliedScheme().primary == wallpaper }
            assertEquals(wallpaper, appliedScheme().primary)

            openSettings()
            compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).performScrollTo().assertIsSelected()
            compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG)
                .performScrollTo()
                .assertIsNotSelected()
        }
    }

    /**
     * Does what a restart of the app's process does to its settings: cancels the settings
     * DataStore's scope and waits for it to finish, which releases the file (only one DataStore
     * may have it open), then stops Koin and starts it again with the app's module, so every
     * singleton, the DataStore included, is built anew and reads the file from disk.
     */
    private fun restartLikeANewProcess() {
        runBlocking {
            KoinPlatform.getKoin().get<CoroutineScope>(SETTINGS_SCOPE)
                .coroutineContext[Job]!!
                .cancelAndJoin()
        }
        stopKoin()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(appModule)
        }
    }

    /** Returns the colour scheme KansoTheme currently hands to every screen. */
    private fun appliedScheme(): ColorScheme =
        compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode().config[KansoColorSchemeKey]

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

    /** Colours the tests use. */
    private companion object {
        /** The custom colour of the DoD: green #2E7D32. */
        val GREEN = Color(0xFF2E7D32)
    }
}
