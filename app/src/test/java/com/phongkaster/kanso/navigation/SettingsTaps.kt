package com.phongkaster.kanso.navigation

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_THEME_COLOUR_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_THEME_MODE_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.TEXT_SIZE_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_SCREEN_TAG

// Shared steps for the whole-app tests that use Settings. Settings is a menu: each setting is a
// row that opens the setting on a screen of its own, with a Back button that returns to the menu.

/** How long, in milliseconds, a step waits for a screen to be shown before it fails. */
private const val SCREEN_TIMEOUT_MS = 10_000L

/**
 * Opens the Settings tab, taps the menu row tagged [menuTag] and waits until the setting's own
 * screen, tagged [screenTag], is shown.
 */
fun openSetting(compose: ComposeTestRule, menuTag: String, screenTag: String) {
    openTab(compose, KansoTab.SETTINGS)
    compose.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MS) { shown(compose, SETTINGS_SCREEN_TAG) }
    compose.onNodeWithTag(menuTag).tapAboveBar()
    compose.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MS) { shown(compose, screenTag) }
}

/** Opens the Theme mode screen from the Settings tab. */
fun openThemeModeScreen(compose: ComposeTestRule) =
    openSetting(compose, SETTINGS_MENU_THEME_MODE_TAG, THEME_MODE_SCREEN_TAG)

/** Opens the Theme colour screen from the Settings tab. */
fun openThemeColourScreen(compose: ComposeTestRule) =
    openSetting(compose, SETTINGS_MENU_THEME_COLOUR_TAG, THEME_COLOUR_SCREEN_TAG)

/** Tells whether one of the three setting screens is shown. */
fun onSettingScreen(compose: ComposeTestRule): Boolean =
    shown(compose, THEME_MODE_SCREEN_TAG) || shown(compose, THEME_COLOUR_SCREEN_TAG) ||
        shown(compose, TEXT_SIZE_SCREEN_TAG)

/** Presses Back on a setting's own screen and waits until the Settings menu is shown again. */
fun backToSettingsMenu(compose: ComposeTestRule) {
    Espresso.pressBack()
    compose.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MS) {
        shown(compose, SETTINGS_SCREEN_TAG) && !onSettingScreen(compose)
    }
}

/**
 * Presses Back until Home alone is shown: from a setting's own screen that is twice (to the
 * Settings menu, then Home); from the menu once.
 */
fun backToHomeFromSettings(compose: ComposeTestRule) {
    if (onSettingScreen(compose)) backToSettingsMenu(compose)
    Espresso.pressBack()
    compose.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MS) {
        shown(compose, HOME_SCREEN_TAG) && !shown(compose, SETTINGS_SCREEN_TAG) && !onSettingScreen(compose)
    }
}

/** Tells whether a node tagged [tag] is in the tree. */
private fun shown(compose: ComposeTestRule, tag: String): Boolean =
    compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
