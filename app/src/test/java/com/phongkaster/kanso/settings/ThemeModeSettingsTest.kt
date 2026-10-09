package com.phongkaster.kanso.settings

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_DARK_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_LIGHT_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_SYSTEM_TAG
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the real app through Settings -> Appearance -> Theme mode and reads the result from the
 * drawn pixels: picking a mode re-themes Settings and Home at once, and System follows the
 * phone's night setting. Surviving a restart is checked in ThemeModeRestartTest.
 *
 * Every check looks at two things in one drawing: the background, and the text of a known label
 * (the "Dark" row on Settings, the "Settings" row on Home), which must have the opposite
 * brightness. So a screen that only painted its own background would not pass.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ThemeModeSettingsTest {

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

    /**
     * Picking Dark turns Settings dark at once, Home is dark after Back, and picking Light turns
     * both light again, all without a restart.
     */
    @Test
    @Config(qualifiers = "notnight")
    fun picking_dark_then_light_rethemes_settings_and_home_at_once() {
        waitForHome()
        assertDrawnLight(sampleHome())

        openSettings()
        assertDrawnLight(sampleSettings())
        compose.onNodeWithTag(THEME_MODE_SYSTEM_TAG).assertIsSelected()
        pick(THEME_MODE_DARK_TAG)
        waitUntilDark()
        assertDrawnDark(sampleSettings())
        compose.onNodeWithTag(THEME_MODE_DARK_TAG).assertIsSelected()
        compose.onNodeWithTag(THEME_MODE_SYSTEM_TAG).assertIsNotSelected()

        Espresso.pressBack()
        waitForHomeOnly()
        assertDrawnDark(sampleHome())

        openSettings()
        pick(THEME_MODE_LIGHT_TAG)
        waitUntilLight()
        assertDrawnLight(sampleSettings())
        compose.onNodeWithTag(THEME_MODE_LIGHT_TAG).assertIsSelected()

        Espresso.pressBack()
        waitForHomeOnly()
        assertDrawnLight(sampleHome())
    }

    /** On a phone in night mode, System (the default) is dark, Light overrides it, System returns. */
    @Test
    @Config(qualifiers = "night")
    fun system_mode_follows_a_night_phone() {
        waitForHome()
        assertDrawnDark(sampleHome())

        openSettings()
        assertDrawnDark(sampleSettings())
        compose.onNodeWithTag(THEME_MODE_SYSTEM_TAG).assertIsSelected()
        pick(THEME_MODE_LIGHT_TAG)
        waitUntilLight()
        assertDrawnLight(sampleSettings())
        pick(THEME_MODE_SYSTEM_TAG)
        waitUntilDark()
        assertDrawnDark(sampleSettings())

        Espresso.pressBack()
        waitForHomeOnly()
        assertDrawnDark(sampleHome())
    }

    /** On a phone in day mode, System (the default) is light, Dark overrides it, System returns. */
    @Test
    @Config(qualifiers = "notnight")
    fun system_mode_follows_a_day_phone() {
        waitForHome()
        assertDrawnLight(sampleHome())

        openSettings()
        assertDrawnLight(sampleSettings())
        pick(THEME_MODE_DARK_TAG)
        waitUntilDark()
        assertDrawnDark(sampleSettings())
        pick(THEME_MODE_SYSTEM_TAG)
        waitUntilLight()
        assertDrawnLight(sampleSettings())

        Espresso.pressBack()
        waitForHomeOnly()
        assertDrawnLight(sampleHome())
    }

    /**
     * Draws Home and samples its background and the text of its "Settings" row. Home is taller
     * than this test's screen (Robolectric's default 320 x 470 dp), so the row is first scrolled
     * into view: the text must be drawn on screen to be sampled.
     */
    private fun sampleHome(): ScreenSample {
        compose.onNodeWithText(context.getString(R.string.settings)).performScrollTo()
        return sampleAround(hasText(context.getString(R.string.settings)))
    }

    /** Draws Settings and samples its background and the text of its "Dark" row. */
    private fun sampleSettings(): ScreenSample = sampleAround(
        hasText(context.getString(R.string.theme_mode_dark)) and
            hasAnyAncestor(hasTestTag(SETTINGS_SCREEN_TAG)),
    )

    /**
     * Lets Compose settle, finds the one text node matching [text] and samples the drawn screen
     * around it. The unmerged tree is searched so the bounds are the text's own, not its row's.
     */
    private fun sampleAround(text: SemanticsMatcher): ScreenSample {
        compose.waitForIdle()
        val bounds = compose.onNode(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        return compose.activity.sampleScreen(bounds)
    }

    /** Taps the theme mode row tagged [tag]. */
    private fun pick(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    /** Waits until the screen's background is drawn dark, which needs the DataStore write. */
    private fun waitUntilDark() {
        compose.waitUntil(timeoutMillis = 10_000) {
            isDark(compose.activity.drawnColorNearBottomLeft())
        }
    }

    /** Waits until the screen's background is drawn light, which needs the DataStore write. */
    private fun waitUntilLight() {
        compose.waitUntil(timeoutMillis = 10_000) {
            isLight(compose.activity.drawnColorNearBottomLeft())
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
        compose.onNodeWithText(context.getString(R.string.theme_mode)).assertIsDisplayed()
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
}
