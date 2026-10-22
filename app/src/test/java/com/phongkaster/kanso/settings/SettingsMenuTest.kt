package com.phongkaster.kanso.settings

import android.content.Context
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.openTab
import com.phongkaster.kanso.navigation.tapAboveBar
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_GROUP_MENU_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_TEXT_SIZE_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_THEME_COLOUR_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_THEME_MODE_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.TEXT_SIZE_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.settingsRowDescriptionTag
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * The Settings menu: it lists the three settings (Theme mode, Theme colour, Text size) in one
 * rounded group, each row saying what the setting is now, and a tap on a row opens that setting on
 * a screen of its own, whose Back button returns to the menu.
 *
 * It drives the real app from a fresh install, where the theme mode is System default, the colour
 * is Kanso blue and the text size is 100%.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "notnight")
class SettingsMenuTest {

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

    /** The menu holds the three rows in one group, each with the summary a fresh install shows. */
    @Test
    fun the_menu_lists_three_settings_with_their_current_values() {
        openMenu()
        compose.onNodeWithTag(SETTINGS_GROUP_MENU_TAG).assertExists()
        assertSummary(SETTINGS_MENU_THEME_MODE_TAG, context.getString(R.string.theme_mode_system))
        assertSummary(SETTINGS_MENU_THEME_COLOUR_TAG, context.getString(R.string.theme_colour_kanso_blue))
        assertSummary(SETTINGS_MENU_TEXT_SIZE_TAG, context.getString(R.string.text_size_value, 100))
    }

    /** The Theme mode row opens the Theme mode screen, and Back returns to the menu. */
    @Test
    fun theme_mode_opens_its_own_screen_and_back_returns() {
        assertOpensAndReturns(SETTINGS_MENU_THEME_MODE_TAG, THEME_MODE_SCREEN_TAG)
    }

    /** The Theme colour row opens the Theme colour screen, and Back returns to the menu. */
    @Test
    fun theme_colour_opens_its_own_screen_and_back_returns() {
        assertOpensAndReturns(SETTINGS_MENU_THEME_COLOUR_TAG, THEME_COLOUR_SCREEN_TAG)
    }

    /** The Text size row opens the Text size screen, and Back returns to the menu. */
    @Test
    fun text_size_opens_its_own_screen_and_back_returns() {
        assertOpensAndReturns(SETTINGS_MENU_TEXT_SIZE_TAG, TEXT_SIZE_SCREEN_TAG)
    }

    /**
     * Taps the menu row [rowTag], waits for the screen tagged [screenTag] (with the menu gone),
     * taps that screen's Back button, and waits for the menu to be back.
     */
    private fun assertOpensAndReturns(rowTag: String, screenTag: String) {
        openMenu()
        compose.onNodeWithTag(rowTag).tapAboveBar()
        waitUntil { count(screenTag) > 0 && count(SETTINGS_SCREEN_TAG) == 0 }
        compose.onNode(
            hasContentDescription(context.getString(R.string.back)) and hasAnyAncestor(hasTestTag(screenTag)),
        ).performClick()
        waitUntil { count(SETTINGS_SCREEN_TAG) > 0 && count(screenTag) == 0 }
    }

    /** Fails unless the row [rowTag] shows exactly the summary [expected] under its title. */
    private fun assertSummary(rowTag: String, expected: String) {
        compose.onNode(
            hasText(expected) and hasAnyAncestor(hasTestTag(rowTag)) and
                hasTestTag(settingsRowDescriptionTag(rowTag)),
            useUnmergedTree = true,
        ).assertExists("The row $rowTag must show the summary \"$expected\"")
    }

    /** Waits for Home, taps the Settings tab and waits until the menu is shown. */
    private fun openMenu() {
        waitUntil { count(HOME_SCREEN_TAG) > 0 }
        openTab(compose, KansoTab.SETTINGS)
        waitUntil { count(SETTINGS_SCREEN_TAG) > 0 }
    }

    /** Waits, up to ten seconds, until [condition] holds. */
    private fun waitUntil(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) { condition() }
        assertTrue(condition())
    }

    /** Returns how many nodes carry [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size
}
