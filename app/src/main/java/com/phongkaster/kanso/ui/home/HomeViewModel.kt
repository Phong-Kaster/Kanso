package com.phongkaster.kanso.ui.home

import androidx.lifecycle.ViewModel
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the Home screen's state: one [HomeUiState] with the tools of the hero carousel (the four
 * AI tools) and the tools of the row of round shortcuts (all five), each in display order.
 *
 * A ViewModel outlives a single drawing of the screen: Android keeps it across rotation and
 * clears it only when its owner is gone for good. Here the owner is Home's navigation entry,
 * so it lives as long as Home is on the back stack (which is always). [probe] lets tests check
 * that this really holds.
 */
class HomeViewModel(private val probe: ViewModelClearedProbe) : ViewModel() {

    /**
     * The writable holder of Home's state. Only this class may put a new snapshot into it, and
     * Home never needs one, so it keeps its first snapshot for good: the four AI tools on the
     * carousel's banners, and every tool, in the order the [Tool] enum declares them, on the
     * shortcuts. The camera is in neither: it takes a photo instead of editing one, so it is the
     * round button beside the floating tab bar.
     */
    private val mutableState = MutableStateFlow(
        HomeUiState(
            carouselTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
            shortcutTools = Tool.entries.toList(),
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
