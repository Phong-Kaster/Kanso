package com.phongkaster.kanso.navigation

import android.os.Looper
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.homeShortcutTag
import org.robolectric.Shadows.shadowOf

// How whole-app tests leave Home for a tool's photo gallery, and how they wait for Home.
//
// Every walk that starts on Home and needs a tool's gallery goes through openToolGallery, so when
// Home's layout changes (Run 7 Phase 2 brings the carousel and the row of tool shortcuts) only
// this file is rewritten, not every walk. Home itself is recognised by its tag, never by a word
// on it: the words on Home change with its layout, and some (a tool's name) appear twice.

/** How long, in milliseconds, these helpers wait for Home before they fail. */
private const val HOME_TIMEOUT_MS = 10_000L

/**
 * Waits until Home is on screen, letting the main thread run in between (Home's loads post their
 * results there, and under Robolectric the test runs on that thread itself).
 */
fun waitForHome(compose: ComposeTestRule) {
    compose.waitUntil(timeoutMillis = HOME_TIMEOUT_MS) {
        shadowOf(Looper.getMainLooper()).idle()
        compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * From Home, opens [tool]'s photo gallery as a person would, with one tap. The caller waits for
 * the gallery.
 *
 * Home has a round shortcut for every tool, tagged `homeShortcutTag(tool)` (`home-shortcut-<TOOL>`),
 * in a row that never scrolls sideways, so no swipe of the carousel is needed for any tool. Home
 * scrolls up and down and the floating bar covers its bottom, so the shortcut is scrolled into
 * view and tapped through its click action ([tapAboveBar]), never with a touch that could land on
 * the bar.
 */
fun openToolGallery(compose: ComposeTestRule, tool: Tool) {
    waitForHome(compose)
    val tag = homeShortcutTag(tool)
    compose.waitUntil(timeoutMillis = HOME_TIMEOUT_MS) {
        shadowOf(Looper.getMainLooper()).idle()
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(tag).tapAboveBar()
}
