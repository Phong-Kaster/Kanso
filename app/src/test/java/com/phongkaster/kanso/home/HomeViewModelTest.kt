package com.phongkaster.kanso.home

import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Checks Home's state on the plain JVM (DoD 5): the one snapshot HomeViewModel hands the screen
 * lists every tool, in the order the `Tool` enum declares them, which is the order Home draws its
 * rows in.
 *
 * HomeViewModel needs no coroutine, file or Android service, so it is simply built and read.
 */
class HomeViewModelTest {

    /**
     * The state's tool list equals `Tool.entries`, in the same order, and that order is the six
     * tools Home has always shown, Edit first and Camera last.
     */
    @Test
    fun home_state_lists_every_tool_in_order() {
        val viewModel = HomeViewModel(ViewModelClearedProbe())

        val tools = viewModel.state.value.tools

        assertEquals(Tool.entries.toList(), tools)
        assertEquals(
            listOf(
                Tool.EDIT,
                Tool.REMOVE_BACKGROUND,
                Tool.ENHANCE,
                Tool.RETOUCH,
                Tool.ARTISTIC,
                Tool.CAMERA,
            ),
            tools,
        )
    }
}
