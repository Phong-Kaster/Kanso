package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey

// The moves the app makes on its back stack, as plain functions on a list of keys.
//
// The back stack is a list of NavKeys with Home at the bottom; the last key is the screen shown
// (see Home.kt). Navigating is changing that list. Keeping each move here, out of the navigation
// host, means a plain unit test can check it on an ordinary list, with no screen at all
// (BackStackActionsTest), and the host's click handlers are one call each. The functions are
// extensions of MutableList<NavKey>, so they work on the app's saved back stack and on any list.

/**
 * Tells whether [key] is the key of one of the three tabs ([KansoTab]): Home, My edits or
 * Settings. The navigation host shows the floating tab bar only while the key on top is a tab.
 */
fun isTabKey(key: NavKey): Boolean = KansoTab.entries.any { tab -> tab.key == key }

/**
 * Shows [tab]: clears the back stack down to `[Home]`, then, for My edits or Settings, puts that
 * tab's key on top, so the stack is `[Home]`, `[Home, MyEdits]` or `[Home, Settings]`.
 *
 * Why tabs never stack: moving from My edits to Settings replaces My edits instead of adding
 * Settings above it, so the back stack never piles tabs up, and Back from any tab other than
 * Home always lands on Home in one step. Choosing the tab that is already shown, with only Home
 * below it, changes nothing, so a second tap neither reloads the screen nor plays a transition.
 */
fun MutableList<NavKey>.selectTab(tab: KansoTab) {
    val wanted = if (tab == KansoTab.HOME) listOf<NavKey>(Home) else listOf(Home, tab.key)
    // Already there: leave the list untouched, so nothing on screen moves.
    if (toList() == wanted) return
    closeToHome()
    if (tab != KansoTab.HOME) add(tab.key)
}

/**
 * The move behind a tool screen's "Replace" button: leaves the gallery of [tool] on top, so the
 * person picks another photo for the same tool.
 *
 * If the key just below the top is already `Gallery(tool)` (the tool screen was opened from that
 * gallery), the top key is popped and the gallery comes back as it was. Otherwise (the tool
 * screen was opened from the camera, say) the top key is replaced by `Gallery(tool)`, so Back
 * from the gallery still goes where Back from the tool screen went. The bottom key is never
 * removed: on a stack holding one key, the gallery is added above it.
 */
fun MutableList<NavKey>.replaceWithGallery(tool: Tool) {
    val gallery = Gallery(tool)
    if (size >= 2 && this[size - 2] == gallery) {
        removeLastOrNull()
        return
    }
    if (size > 1) removeLastOrNull()
    add(gallery)
}

/**
 * Clears the back stack down to `[Home]`: every key above Home is removed. If the list is empty,
 * or something other than Home is at the bottom, it is reset to Home alone, because Home is the
 * screen the app always returns to.
 */
fun MutableList<NavKey>.closeToHome() {
    while (size > 1) removeLastOrNull()
    if (firstOrNull() != Home) {
        clear()
        add(Home)
    }
}
