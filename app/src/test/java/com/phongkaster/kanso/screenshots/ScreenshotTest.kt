package com.phongkaster.kanso.screenshots

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.placeholder.PLACEHOLDER_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.CUSTOM_COLOUR_PICKER_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_DARK_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_LIGHT_TAG
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.baselineColorScheme
import com.phongkaster.kanso.ui.theme.seedColorScheme
import com.github.takahirom.roborazzi.captureRoboImage
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
import org.robolectric.annotation.GraphicsMode

/**
 * Takes the nine reference pictures of the app: Home, Settings and the Edit placeholder, each in
 * light, in dark, and in the custom colour #2E7D32.
 *
 * Roborazzi is a library that draws a screen on the computer, with no phone or emulator, and
 * saves it as a PNG. Each saved PNG is a baseline: the agreed look of that screen. Later runs
 * draw the screen again and compare it with the baseline, so an unplanned change to how a screen
 * looks fails the build. The baselines live in `app/src/test/screenshots/`.
 *
 * Robolectric runs Android on the plain Java machine of the computer. By default it does not
 * really draw anything; `GraphicsMode.Mode.NATIVE` switches on Android's real drawing code, so
 * the pictures show actual pixels.
 *
 * Every test drives the real app: MainActivity, its navigation, its ViewModels and Koin, nothing
 * faked. Light and dark are chosen the way a person would, through Settings -> Theme mode. The
 * light and dark pictures run as Android 11 (no wallpaper colours there), so the colours are the
 * built-in baseline purple. The custom colour pictures run as Android 15 in light mode. All of
 * them use the screen size of a typical phone (411 x 891 dp at xxhdpi density).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = PHONE)
class ScreenshotTest {

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

    /** Picture 1: Home in light mode with the baseline colours. */
    @Test
    fun home_light() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Light)
        backToHome()
        capture("1_home_light.png")
    }

    /** Picture 2: Home in dark mode with the baseline colours. */
    @Test
    fun home_dark() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Dark)
        backToHome()
        capture("2_home_dark.png")
    }

    /** Picture 3: Home in light mode with the custom colour #2E7D32. */
    @Test
    @Config(sdk = [35], qualifiers = PHONE)
    fun home_custom_colour() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Light)
        chooseCustomColour()
        backToHome()
        capture("3_home_custom_colour.png")
    }

    /** Picture 4: Settings in light mode with the baseline colours. */
    @Test
    fun settings_light() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Light)
        compose.onNodeWithTag(THEME_MODE_LIGHT_TAG).assertIsSelected()
        capture("4_settings_light.png")
    }

    /** Picture 5: Settings in dark mode with the baseline colours. */
    @Test
    fun settings_dark() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Dark)
        compose.onNodeWithTag(THEME_MODE_DARK_TAG).assertIsSelected()
        capture("5_settings_dark.png")
    }

    /**
     * Picture 6: Settings in light mode with the custom colour #2E7D32 applied. The Custom row is
     * selected, so the colour picker is shown below it; that is how the screen really looks.
     */
    @Test
    @Config(sdk = [35], qualifiers = PHONE)
    fun settings_custom_colour() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Light)
        chooseCustomColour()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().assertIsSelected()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(CUSTOM_COLOUR_PICKER_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        capture("6_settings_custom_colour.png")
    }

    /** Picture 7: the Edit placeholder in light mode with the baseline colours. */
    @Test
    fun placeholder_light() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Light)
        backToHome()
        openEditPlaceholder()
        capture("7_placeholder_light.png")
    }

    /** Picture 8: the Edit placeholder in dark mode with the baseline colours. */
    @Test
    fun placeholder_dark() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Dark)
        backToHome()
        openEditPlaceholder()
        capture("8_placeholder_dark.png")
    }

    /** Picture 9: the Edit placeholder in light mode with the custom colour #2E7D32. */
    @Test
    @Config(sdk = [35], qualifiers = PHONE)
    fun placeholder_custom_colour() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Light)
        chooseCustomColour()
        backToHome()
        openEditPlaceholder()
        capture("9_placeholder_custom_colour.png")
    }

    /**
     * Saves a picture of the whole window as `src/test/screenshots/[name]`. When baselines are
     * checked, Roborazzi compares the drawing with that file instead of saving it.
     */
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name")
    }

    /**
     * Taps the Light or Dark row of Settings -> Theme mode (only those two are used), waits in
     * real time until the choice is saved, then waits until the theme hands out the baseline
     * scheme of that mode. On Android 11 the baseline is the colour source, so its background is
     * exactly what the app must show; on Android 15 the wallpaper scheme is in use, so only the
     * saved mode is waited for there and the custom colour check follows.
     */
    private fun chooseThemeMode(mode: ThemeMode) {
        val tag = if (mode == ThemeMode.Dark) THEME_MODE_DARK_TAG else THEME_MODE_LIGHT_TAG
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        awaitSaved("theme mode $mode") { savedThemeMode() == mode }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val expected = baselineColorScheme(dark = mode == ThemeMode.Dark)
            compose.waitUntil(timeoutMillis = 10_000) {
                appliedScheme().background == expected.background &&
                    appliedScheme().primary == expected.primary
            }
        }
    }

    /**
     * Sets the custom colour #2E7D32 through SettingsViewModel's onCustomColourChosen, the same
     * function the picker's onColorChanged calls (the picker itself is not dragged), then waits
     * until it is saved and the light scheme generated from it is applied.
     */
    private fun chooseCustomColour() {
        val expected = seedColorScheme(GREEN, dark = false)
        SettingsViewModel(repository()).onCustomColourChosen(GREEN)
        awaitSaved("a custom theme colour") { savedThemeColour() is ThemeColour.Custom }
        compose.waitUntil(timeoutMillis = 10_000) {
            appliedScheme().primary == expected.primary &&
                appliedScheme().background == expected.background
        }
    }

    /**
     * Waits, in real time, until [isSaved] holds, then fails naming [what] if it never did. The
     * settings file is written on a background thread, which Compose's test clock does not wait
     * for, so the file is read again every 50 ms instead.
     */
    private fun awaitSaved(what: String, isSaved: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!isSaved() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            compose.waitForIdle()
        }
        assertTrue("Expected $what to be saved", isSaved())
    }

    /** Returns the colour scheme KansoTheme currently hands to every screen. */
    private fun appliedScheme(): ColorScheme =
        compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode().config[KansoColorSchemeKey]

    /** Reads the theme mode saved in the settings file right now. */
    private fun savedThemeMode(): ThemeMode = runBlocking { repository().themeMode.first() }

    /** Reads the theme colour saved in the settings file right now. */
    private fun savedThemeColour(): ThemeColour = runBlocking { repository().themeColour.first() }

    /** Returns the app's one settings repository from Koin. */
    private fun repository(): SettingsRepository = KoinPlatform.getKoin().get()

    /** Taps the Settings row on Home and waits until the Settings screen is shown. */
    private fun openSettings() {
        compose.onNodeWithText(context.getString(R.string.settings))
            .performScrollTo()
            .performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Presses Back on Settings, waits until Home alone is shown, and keeps the applied scheme. */
    private fun backToHome() {
        val scheme = appliedScheme()
        Espresso.pressBack()
        waitForHomeOnly()
        assertEquals(scheme.primary, appliedScheme().primary)
    }

    /** Taps the Edit tool on Home and waits until its placeholder shows the tool's name. */
    private fun openEditPlaceholder() {
        val name = context.getString(Tool.EDIT.labelRes)
        compose.onNodeWithText(name).performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(
                hasText(name) and hasAnyAncestor(hasTestTag(PLACEHOLDER_SCREEN_TAG)),
            ).fetchSemanticsNodes().isNotEmpty()
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
    }
}

/**
 * The Robolectric screen every picture is drawn on: 411 dp wide, 891 dp tall, at xxhdpi
 * (about three pixels per dp), the size of a typical modern phone.
 */
private const val PHONE = "w411dp-h891dp-xxhdpi"
