package com.phongkaster.kanso.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks which launcher icon each kind of theme colour gets. */
class LauncherIconTest {

    /** Every icon's own background colour picks that same icon. */
    @Test
    fun each_icon_colour_matches_its_own_icon() {
        LauncherIcon.entries.forEach { icon ->
            assertEquals(icon, LauncherIcon.closestTo(icon.argb))
        }
    }

    /** Colours from the custom picker land on the icon of the same colour family. */
    @Test
    fun picked_colours_match_their_colour_family() {
        assertEquals(LauncherIcon.Pink, LauncherIcon.closestTo(0xFFE12299.toInt()))
        assertEquals(LauncherIcon.Green, LauncherIcon.closestTo(0xFF2E7D32.toInt()))
        assertEquals(LauncherIcon.Green, LauncherIcon.closestTo(0xFF8BC34A.toInt()))
        assertEquals(LauncherIcon.Amber, LauncherIcon.closestTo(0xFFFFEB3B.toInt()))
        assertEquals(LauncherIcon.Red, LauncherIcon.closestTo(0xFFFF0000.toInt()))
        assertEquals(LauncherIcon.KansoBlue, LauncherIcon.closestTo(0xFF2196F3.toInt()))
        assertEquals(LauncherIcon.Teal, LauncherIcon.closestTo(0xFF00BCD4.toInt()))
        assertEquals(LauncherIcon.Purple, LauncherIcon.closestTo(0xFF9C27B0.toInt()))
    }

    /** Greys, black and white have no hue to match, so they get the near-black Ink icon. */
    @Test
    fun colours_without_a_hue_get_ink() {
        assertEquals(LauncherIcon.Ink, LauncherIcon.closestTo(0xFF000000.toInt()))
        assertEquals(LauncherIcon.Ink, LauncherIcon.closestTo(0xFFFFFFFF.toInt()))
        assertEquals(LauncherIcon.Ink, LauncherIcon.closestTo(0xFF808080.toInt()))
        assertEquals(LauncherIcon.Ink, LauncherIcon.closestTo(0xFF5A5F66.toInt()))
    }
}
