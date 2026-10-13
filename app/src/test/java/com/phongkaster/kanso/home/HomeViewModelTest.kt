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
 * Checks Home's state on the plain JVM (DoD 5): the one snapshot HomeViewModel hands the screen
 * holds the two groups of tool cards, the AI tools and the Edit tool, each in the order Home
 * draws them, and nothing for the camera, which Home shows as a shortcut of its own.
 *
 * HomeViewModel needs no coroutine, file or Android service, so it is simply built and read.
 */
class HomeViewModelTest {

    /**
     * The "AI tools" group is Remove background, Enhance, Retouch and Artistic, in that order, and
     * the "Edit" group is Edit alone (assumption A-001).
     */
    @Test
    fun home_state_holds_the_ai_tools_and_the_edit_group_in_order() {
        val state = HomeViewModel(ViewModelClearedProbe()).state.value

        assertEquals(
            listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
            state.aiTools,
        )
        assertEquals(listOf(Tool.EDIT), state.editTools)
    }

    /**
     * Nothing in Home's state is for the camera: the two groups together hold each of the five
     * editing tools exactly once (so no camera entry), and the state has no field other than the
     * two groups (so no hidden camera flag either).
     */
    @Test
    fun home_state_has_nothing_for_the_camera() {
        val state = HomeViewModel(ViewModelClearedProbe()).state.value
        val listed = state.aiTools + state.editTools

        assertEquals("A tool is listed twice: $listed", listed.toSet().size, listed.size)
        assertEquals(Tool.entries.toSet(), listed.toSet())
        assertEquals(5, listed.size)

        // The fields the compiled class declares, read with Java reflection. Static fields are
        // left out: the Compose compiler adds a static `$stable` marker to classes it checks.
        val fields = HomeUiState::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()
        assertEquals(setOf("aiTools", "editTools"), fields)
        assertTrue(
            "HomeUiState has a field for the camera: $fields",
            fields.none { it.contains("camera", ignoreCase = true) },
        )
    }

    /**
     * Camera is not a tool any more (R3, assumption A-010): the `Tool` enum holds exactly the five
     * editing tools, in the order Home shows them, and no entry is named
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
