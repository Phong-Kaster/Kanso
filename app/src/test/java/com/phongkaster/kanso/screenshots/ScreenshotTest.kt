package com.phongkaster.kanso.screenshots

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.navigation.openTab
import com.phongkaster.kanso.navigation.tapAboveBar
import com.phongkaster.kanso.ui.home.HOME_CAROUSEL_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.HOME_SHORTCUTS_TAG
import com.phongkaster.kanso.ui.home.homeCarouselDotTag
import com.phongkaster.kanso.ui.home.homeShortcutTag
import com.phongkaster.kanso.ui.settings.CUSTOM_COLOUR_PICKER_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_THEME_COLOUR_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_THEME_MODE_TAG
import com.phongkaster.kanso.ui.settings.TEXT_SIZE_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_SWATCH_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_KANSO_BLUE_SWATCH_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_KANSO_BLUE_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_WALLPAPER_SWATCH_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_WALLPAPER_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_DARK_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_LIGHT_TAG
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
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
 * Takes eight reference pictures of the app: Home and Settings, each in light, in dark, and in
 * the custom colour #2E7D32, and Settings once more in light and in dark on Android 15, where the
 * colour list shows all three rows with their swatches.
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
 * faked. Light and dark are chosen the way a person would, through Settings -> Theme mode, with
 * Settings opened from the floating tab bar's Settings tab (`TabTaps.openTab`). The
 * light and dark pictures 1, 2, 4 and 5 run as Android 11 (no wallpaper colours there), in Kanso
 * blue, the colours of a fresh install. The custom colour pictures run as Android 15 in light
 * mode. The swatch pictures 24 and 25 run as Android 15 in Kanso blue, so the wallpaper row is
 * offered too. All of them use the screen size of a typical phone (411 x 891 dp at xxhdpi
 * density).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = PHONE)
class ScreenshotTest {

    /** Launches MainActivity before each test and lets the test find and tap its Compose nodes. */
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

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
        assertNewHomeAtRest()
        capture("1_home_light.png")
    }

    /** Picture 2: Home in dark mode with the baseline colours. */
    @Test
    fun home_dark() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Dark)
        backToHome()
        assertNewHomeAtRest()
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
        assertNewHomeAtRest()
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
        backToSettingsMenu()
        openSetting(SETTINGS_MENU_THEME_COLOUR_TAG, THEME_COLOUR_SCREEN_TAG)
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().assertIsSelected()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(CUSTOM_COLOUR_PICKER_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        capture("6_settings_custom_colour.png")
    }

    /**
     * Picture 24: Settings on Android 15 in light mode with Kanso blue, a fresh install's colour:
     * the colour group shows Kanso blue (selected), From wallpaper and Custom colour, each with its
     * swatch, all on screen without scrolling.
     */
    @Test
    @Config(sdk = [35], qualifiers = PHONE)
    fun settings_swatches_light() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Light)
        backToSettingsMenu()
        openSetting(SETTINGS_MENU_THEME_COLOUR_TAG, THEME_COLOUR_SCREEN_TAG)
        assertThreeSwatchRowsShown()
        capture("24_settings_swatches_light.png")
    }

    /**
     * Picture 25: Settings on Android 15 in dark mode with Kanso blue: the same three colour rows
     * and swatches as picture 24, on the dark theme. Each swatch shows the light version of its
     * colour in both pictures.
     */
    @Test
    @Config(sdk = [35], qualifiers = PHONE)
    fun settings_swatches_dark() {
        waitForHome()
        openSettings()
        chooseThemeMode(ThemeMode.Dark)
        backToSettingsMenu()
        openSetting(SETTINGS_MENU_THEME_COLOUR_TAG, THEME_COLOUR_SCREEN_TAG)
        assertThreeSwatchRowsShown()
        capture("25_settings_swatches_dark.png")
    }

    /**
     * Fails unless the three colour rows' swatches are all on screen as the picture will show it
     * (the list is not scrolled for them), with Kanso blue selected and the other two not.
     * The swatches are found in the unmerged tree, because each row merges its swatch for screen
     * readers.
     */
    private fun assertThreeSwatchRowsShown() {
        listOf(
            THEME_COLOUR_KANSO_BLUE_SWATCH_TAG,
            THEME_COLOUR_WALLPAPER_SWATCH_TAG,
            THEME_COLOUR_CUSTOM_SWATCH_TAG,
        ).forEach { tag ->
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
        }
        compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG).assertIsSelected()
        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).assertIsNotSelected()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsNotSelected()
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
     * real time until the choice is saved, then waits until the theme hands out Kanso blue in
     * that mode (`seedColorScheme(KANSO_BLUE_SEED, dark)`). Every test calls this on a fresh
     * install, before any custom colour is picked, and a fresh install is in Kanso blue on every
     * Android version, so its background is exactly what the app must show on Android 11 and 15
     * alike; the custom colour pictures then wait for their own colour in chooseCustomColour.
     */
    private fun chooseThemeMode(mode: ThemeMode) {
        val tag = if (mode == ThemeMode.Dark) THEME_MODE_DARK_TAG else THEME_MODE_LIGHT_TAG
        openSetting(SETTINGS_MENU_THEME_MODE_TAG, THEME_MODE_SCREEN_TAG)
        compose.onNodeWithTag(tag).tapAboveBar()
        awaitSaved("theme mode $mode") { savedThemeMode() == mode }
        val expected = seedColorScheme(KANSO_BLUE_SEED, dark = mode == ThemeMode.Dark)
        compose.waitUntil(timeoutMillis = 10_000) {
            appliedScheme().background == expected.background &&
                appliedScheme().primary == expected.primary
        }
    }

    /**
     * Sets the custom colour #2E7D32 through SettingsViewModel's onCustomColourChosen, the same
     * function the picker's onColorChanged calls (the picker itself is not dragged), then waits
     * until it is saved and the light scheme generated from it is applied.
     */
    private fun chooseCustomColour() {
        val expected = seedColorScheme(GREEN, dark = false, exactPrimary = true)
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

    /** Taps the Settings tab on the floating bar and waits until the Settings screen is shown. */
    private fun openSettings() {
        openTab(compose, KansoTab.SETTINGS)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Taps the Settings menu row tagged [menuTag] and waits until the setting's own screen, tagged
     * [screenTag], is shown.
     */
    private fun openSetting(menuTag: String, screenTag: String) {
        compose.onNodeWithTag(menuTag).tapAboveBar()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(screenTag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Presses Back on a setting's own screen and waits until the Settings menu is shown again. */
    private fun backToSettingsMenu() {
        Espresso.pressBack()
        compose.waitUntil(timeoutMillis = 10_000) { isShown(SETTINGS_SCREEN_TAG) && !onSettingDetail() }
    }

    /** Tells whether one of the three setting screens is shown. */
    private fun onSettingDetail(): Boolean =
        isShown(THEME_MODE_SCREEN_TAG) || isShown(THEME_COLOUR_SCREEN_TAG) || isShown(TEXT_SIZE_SCREEN_TAG)

    /** Tells whether a node with [tag] is in the tree. */
    private fun isShown(tag: String): Boolean = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /**
     * Presses Back until Home alone is shown (from a setting's own screen that is twice: to the
     * Settings menu, then Home), and keeps the applied scheme.
     */
    private fun backToHome() {
        val scheme = appliedScheme()
        if (onSettingDetail()) backToSettingsMenu()
        Espresso.pressBack()
        waitForHomeOnly()
        assertEquals(scheme.primary, appliedScheme().primary)
    }

    /** Waits until Home is on screen, found by its own tag. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Fails unless Home is the redesigned one (Run 7) and at rest before its picture is taken: the
     * hero carousel and the row of five tool shortcuts are there, and the carousel shows its first
     * banner (the first page dot is the selected one), so pictures 1, 2 and 3 always show the same
     * banner in front.
     */
    private fun assertNewHomeAtRest() {
        compose.waitForIdle()
        compose.onNodeWithTag(HOME_CAROUSEL_TAG).assertExists()
        compose.onNodeWithTag(HOME_SHORTCUTS_TAG).assertExists()
        Tool.entries.forEach { tool -> compose.onNodeWithTag(homeShortcutTag(tool)).assertExists() }
        compose.onNodeWithTag(homeCarouselDotTag(0))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
    }

    /** Waits until Home is shown and the Settings screen has left the screen. */
    private fun waitForHomeOnly() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty() &&
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
