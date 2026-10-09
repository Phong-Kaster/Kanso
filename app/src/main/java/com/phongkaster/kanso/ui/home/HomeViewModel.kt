package com.phongkaster.kanso.ui.home

import androidx.lifecycle.ViewModel
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the Home screen's state: one [HomeUiState] with the two groups of tool cards, the AI
 * tools and the Edit tool, each in display order.
 *
 * A ViewModel outlives a single drawing of the screen: Android keeps it across rotation and
 * clears it only when its owner is gone for good. Here the owner is Home's navigation entry,
 * so it lives as long as Home is on the back stack (which is always). [probe] lets tests check
 * that this really holds.
 */
class HomeViewModel(private val probe: ViewModelClearedProbe) : ViewModel() {

    /**
     * The writable holder of Home's state. Only this class may put a new snapshot into it, and
     * Home never needs one, so it keeps its first snapshot for good: the four AI tools under
     * "AI tools", then Edit alone under "Edit". The camera is in neither group: Home shows it as
     * a shortcut of its own, because it takes a photo instead of editing one.
     */
    private val mutableState = MutableStateFlow(
        HomeUiState(
            aiTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
            editTools = listOf(Tool.EDIT),
        ),
    )

    /**
     * What Home shows, as the one StateFlow the screen reads (see [HomeUiState] for the whole
     * pattern). asStateFlow() hands out a read-only view of [mutableState]: the screen can read
     * every snapshot, but it has no way to put one in itself.
     */
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
