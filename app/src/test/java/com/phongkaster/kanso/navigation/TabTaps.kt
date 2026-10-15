package com.phongkaster.kanso.navigation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp

// How whole-app tests reach the tabs and the camera, and how they tap a row on a tab screen.
//
// Since Run 7 Settings and the camera are reached only through the floating tab bar: its tabs are
// found by their tags (`nav-tab-<TAB>`, from kansoTabTag) and the Camera button by
// `nav-camera`, never by their text, because "Settings" is on screen twice (the title and the
// tab). The bar floats over the bottom of every tab screen, which is why tapAboveBar exists.

/** How long, in milliseconds, a tap waits for the bar to be on screen before it fails. */
private const val BAR_TIMEOUT_MS = 10_000L

/**
 * Waits until [tab]'s item is on the floating bar, then taps it, as a person would. The caller
 * waits for the screen the tab opens.
 */
fun openTab(compose: ComposeTestRule, tab: KansoTab) {
    compose.waitUntil(timeoutMillis = BAR_TIMEOUT_MS) {
        compose.onAllNodesWithTag(kansoTabTag(tab)).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(kansoTabTag(tab)).performClick()
}

/**
 * Waits until the round Camera button beside the bar is on screen, then taps it. The caller waits
 * for the camera screen.
 */
fun tapCameraButton(compose: ComposeTestRule) {
    compose.waitUntil(timeoutMillis = BAR_TIMEOUT_MS) {
        compose.onAllNodesWithTag(KANSO_CAMERA_BUTTON_TAG).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG).performClick()
}

/**
 * Scrolls this node into view, then clicks it through its semantics click action, and returns it.
 *
 * Why not `performClick()`: on the 320 x 470 dp screen whole-app tests run on, `performScrollTo()`
 * may leave the node at the very bottom of the screen, under the floating bar. `performClick()` is
 * a real touch at the node's centre, which would land on the bar instead. The semantics action
 * calls the node's own click handler, the one a screen reader's double tap calls, so it always
 * reaches the node that was asked for.
 */
fun SemanticsNodeInteraction.tapAboveBar(): SemanticsNodeInteraction {
    performScrollTo()
    performSemanticsAction(SemanticsActions.OnClick)
    return this
}

/**
 * Scrolls this node into view and, if it then ends under the floating bar or the Camera button,
 * scrolls its scrolling container on until the node rests [ABOVE_BAR_MARGIN] above them, and
 * returns it. For tests that read a node's drawn pixels on a tab screen: `performScrollTo()` can
 * leave the node under the bar, and its pixels would then be the bar's. A tab screen leaves room
 * after its last item for exactly this (its `contentPadding`). With no bar on screen, it is
 * `performScrollTo()` alone.
 */
fun SemanticsNodeInteraction.scrollAboveBar(compose: ComposeTestRule): SemanticsNodeInteraction {
    performScrollTo()
    val barTop = listOf(KANSO_TAB_BAR_TAG, KANSO_CAMERA_BUTTON_TAG)
        .flatMap { tag -> compose.onAllNodesWithTag(tag).fetchSemanticsNodes() }
        .minOfOrNull { it.positionInRoot.y } ?: return this
    val node = fetchSemanticsNode()
    val margin = with(compose.density) { ABOVE_BAR_MARGIN.toPx() }
    val overlap = node.positionInRoot.y + node.size.height + margin - barTop
    if (overlap <= 0f) return this
    // The nearest ancestor that can scroll: the tab screen's scrolling column.
    var scroller = node.parent
    while (scroller != null && SemanticsActions.ScrollBy !in scroller.config) scroller = scroller.parent
    val scrollBy = checkNotNull(scroller?.config?.get(SemanticsActions.ScrollBy)?.action) {
        "The node lies under the floating bar and has no scrolling ancestor to move it above"
    }
    compose.runOnUiThread { scrollBy(0f, overlap) }
    compose.waitForIdle()
    return this
}

/** How far above the floating bar [scrollAboveBar] leaves a node. */
private val ABOVE_BAR_MARGIN = 8.dp
