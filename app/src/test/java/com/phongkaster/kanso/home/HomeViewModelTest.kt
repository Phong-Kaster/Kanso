package com.phongkaster.kanso.home

import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.home.HomeUiState
import com.phongkaster.kanso.ui.home.HomeViewModel
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks Home's state on the plain JVM (DoD 4): the one snapshot HomeViewModel hands the screen
 * holds the tools of the hero carousel (the four AI tools) and the tools of the row of round
 * shortcuts (all five), each in the order Home draws them, and nothing for the camera, which is
 * the round button beside the floating tab bar.
 *
 * HomeViewModel needs no coroutine, file or Android service, so it is simply built and read.
 */
class HomeViewModelTest {

    /**
     * DoD 4: the carousel's banners are Remove background, Enhance, Retouch and Artistic, in that
     * order, and the shortcuts are Edit, Remove background, Enhance, Retouch and Artistic, in that
     * order, which is the order of the `Tool` enum itself.
     */
    @Test
    fun home_state_holds_the_carousel_tools_and_the_five_shortcuts_in_order() {
        val state = HomeViewModel(ViewModelClearedProbe()).state.value

        assertEquals(
            listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
            state.carouselTools,
        )
        assertEquals(
            listOf(Tool.EDIT, Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
            state.shortcutTools,
        )
        assertEquals(Tool.entries.toList(), state.shortcutTools)
    }

    /**
     * Nothing in Home's state is for the camera: the shortcuts hold each of the five editing tools
     * exactly once (so no camera entry), the carousel holds only tools that also have a shortcut,
     * and the state has no field other than the two lists (so no hidden camera flag either).
     */
    @Test
    fun home_state_has_nothing_for_the_camera() {
        val state = HomeViewModel(ViewModelClearedProbe()).state.value
        val shortcuts = state.shortcutTools

        assertEquals("A tool has two shortcuts: $shortcuts", shortcuts.toSet().size, shortcuts.size)
        assertEquals(Tool.entries.toSet(), shortcuts.toSet())
        assertEquals(5, shortcuts.size)
        assertTrue(
            "Every banner's tool must also have a shortcut: ${state.carouselTools}",
            shortcuts.containsAll(state.carouselTools),
        )

        // The fields the compiled class declares, read with Java reflection. Static fields are
        // left out: the Compose compiler adds a static `$stable` marker to classes it checks.
        val fields = HomeUiState::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()
        assertEquals(setOf("carouselTools", "shortcutTools"), fields)
        assertTrue(
            "HomeUiState has a field for the camera: $fields",
            fields.none { it.contains("camera", ignoreCase = true) },
        )
    }

    /**
     * Camera is not a tool any more (R3, assumption A-010): the `Tool` enum holds exactly the five
     * editing tools, in the order Home's shortcuts show them, and no entry is named
     * after the camera. The `Camera` navigation key is what stands for it.
     */
    @Test
    fun camera_is_not_a_tool() {
        assertEquals(5, Tool.entries.size)
        assertEquals(
            listOf(Tool.EDIT, Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
            Tool.entries,
        )
        assertTrue(
            "A tool is named after the camera: ${Tool.entries}",
            Tool.entries.none { it.name.contains("camera", ignoreCase = true) },
        )
    }
}
