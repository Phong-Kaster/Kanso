package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the back-stack moves of BackStackActions.kt on plain lists, the way the navigation host
 * applies them to the app's saved back stack: `isTabKey`, `selectTab`, `replaceWithGallery` and
 * `closeToHome`. No screen, no Android: each move is a function of a list, so the test builds a
 * list (Home at the bottom), applies the move and compares the list that is left.
 */
class BackStackActionsTest {

    /** Home, My edits and Settings are tab keys; a gallery, a tool screen, the camera and Saved are not. */
    @Test
    fun only_the_three_tab_keys_are_tab_keys() {
        assertTrue(isTabKey(Home))
        assertTrue(isTabKey(MyEdits))
        assertTrue(isTabKey(Settings))
        assertFalse(isTabKey(Gallery(Tool.EDIT)))
        assertFalse(isTabKey(ToolPhoto(Tool.RETOUCH, PHOTO)))
        assertFalse(isTabKey(Camera(Tool.EDIT)))
        assertFalse(isTabKey(Saved(Tool.EDIT, PHOTO)))
    }

    /** Each tab names its own key, in the bar's order Home, My edits, Settings. */
    @Test
    fun the_tabs_hold_their_keys_in_bar_order() {
        assertEquals(listOf(KansoTab.HOME, KansoTab.MY_EDITS, KansoTab.SETTINGS), KansoTab.entries.toList())
        assertEquals(listOf<NavKey>(Home, MyEdits, Settings), KansoTab.entries.map { it.key })
    }

    /** From Home, choosing My edits or Settings puts that tab on top of Home, and Home clears it. */
    @Test
    fun select_tab_from_home_puts_the_tab_above_home() {
        val stack = mutableListOf<NavKey>(Home)
        stack.selectTab(KansoTab.MY_EDITS)
        assertEquals(listOf<NavKey>(Home, MyEdits), stack)

        stack.selectTab(KansoTab.HOME)
        assertEquals(listOf<NavKey>(Home), stack)

        stack.selectTab(KansoTab.SETTINGS)
        assertEquals(listOf<NavKey>(Home, Settings), stack)
    }

    /** Moving between tabs never piles them up: My edits, Settings, My edits leaves one tab above Home. */
    @Test
    fun select_tab_never_stacks_tabs() {
        val stack = mutableListOf<NavKey>(Home)
        stack.selectTab(KansoTab.MY_EDITS)
        stack.selectTab(KansoTab.SETTINGS)
        assertEquals(listOf<NavKey>(Home, Settings), stack)
        stack.selectTab(KansoTab.MY_EDITS)
        assertEquals(listOf<NavKey>(Home, MyEdits), stack)
    }

    /** Choosing the tab already shown leaves the list exactly as it was. */
    @Test
    fun select_tab_on_the_shown_tab_changes_nothing() {
        val home = mutableListOf<NavKey>(Home)
        home.selectTab(KansoTab.HOME)
        assertEquals(listOf<NavKey>(Home), home)

        val settings = mutableListOf<NavKey>(Home, Settings)
        settings.selectTab(KansoTab.SETTINGS)
        assertEquals(listOf<NavKey>(Home, Settings), settings)
    }

    /** From deeper screens, choosing a tab drops everything above Home first. */
    @Test
    fun select_tab_from_a_deep_stack_clears_down_to_home_first() {
        val stack = mutableListOf<NavKey>(Home, Gallery(Tool.EDIT), ToolPhoto(Tool.EDIT, PHOTO))
        stack.selectTab(KansoTab.SETTINGS)
        assertEquals(listOf<NavKey>(Home, Settings), stack)

        val deep = mutableListOf<NavKey>(Home, MyEdits, Gallery(Tool.ENHANCE))
        deep.selectTab(KansoTab.HOME)
        assertEquals(listOf<NavKey>(Home), deep)
    }

    /** Replace on a tool screen opened from its gallery pops back to that same gallery. */
    @Test
    fun replace_pops_back_to_the_gallery_just_below() {
        val stack = mutableListOf<NavKey>(Home, Gallery(Tool.RETOUCH), ToolPhoto(Tool.RETOUCH, PHOTO))
        stack.replaceWithGallery(Tool.RETOUCH)
        assertEquals(listOf<NavKey>(Home, Gallery(Tool.RETOUCH)), stack)
    }

    /**
     * Replace on a tool screen not opened from its own gallery (from the camera, or from another
     * tool's gallery) replaces the tool screen with that tool's gallery.
     */
    @Test
    fun replace_puts_the_gallery_in_place_of_the_tool_screen_otherwise() {
        val fromCamera = mutableListOf<NavKey>(Home, ToolPhoto(Tool.EDIT, PHOTO))
        fromCamera.replaceWithGallery(Tool.EDIT)
        assertEquals(listOf<NavKey>(Home, Gallery(Tool.EDIT)), fromCamera)

        val fromTab = mutableListOf<NavKey>(Home, MyEdits, ToolPhoto(Tool.EDIT, PHOTO))
        fromTab.replaceWithGallery(Tool.EDIT)
        assertEquals(listOf<NavKey>(Home, MyEdits, Gallery(Tool.EDIT)), fromTab)

        val otherGallery = mutableListOf<NavKey>(Home, Gallery(Tool.ENHANCE), ToolPhoto(Tool.ARTISTIC, PHOTO))
        otherGallery.replaceWithGallery(Tool.ARTISTIC)
        assertEquals(listOf<NavKey>(Home, Gallery(Tool.ENHANCE), Gallery(Tool.ARTISTIC)), otherGallery)
    }

    /** Replace never removes the bottom key: on Home alone the gallery is added above it. */
    @Test
    fun replace_never_removes_home() {
        val stack = mutableListOf<NavKey>(Home)
        stack.replaceWithGallery(Tool.EDIT)
        assertEquals(listOf<NavKey>(Home, Gallery(Tool.EDIT)), stack)
    }

    /** Close goes Home from any depth and leaves Home alone on the stack. */
    @Test
    fun close_to_home_leaves_only_home() {
        val stack = mutableListOf<NavKey>(Home, MyEdits, ToolPhoto(Tool.EDIT, PHOTO), Saved(Tool.EDIT, PHOTO))
        stack.closeToHome()
        assertEquals(listOf<NavKey>(Home), stack)

        val home = mutableListOf<NavKey>(Home)
        home.closeToHome()
        assertEquals(listOf<NavKey>(Home), home)
    }

    /** An empty list, or one with something else at the bottom, is reset to Home alone. */
    @Test
    fun close_to_home_restores_a_missing_home() {
        val empty = mutableListOf<NavKey>()
        empty.closeToHome()
        assertEquals(listOf<NavKey>(Home), empty)

        val noHome = mutableListOf<NavKey>(Settings, Gallery(Tool.EDIT))
        noHome.closeToHome()
        assertEquals(listOf<NavKey>(Home), noHome)
    }

    /** Values the tests share. */
    private companion object {
        /** A photo's Uri as text, as the tool and Saved keys carry it. */
        const val PHOTO = "content://media/external/images/media/42"
    }
}
