package com.phongkaster.kanso.ui.home

import androidx.lifecycle.ViewModel
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool

/**
 * Holds the Home screen's state: for now, simply the tools to list, in display order.
 *
 * A ViewModel outlives a single drawing of the screen: Android keeps it across rotation and
 * clears it only when its owner is gone for good. Here the owner is Home's navigation entry,
 * so it lives as long as Home is on the back stack (which is always). [probe] lets tests check
 * that this really holds.
 */
class HomeViewModel(private val probe: ViewModelClearedProbe) : ViewModel() {

    /** The tools Home lists, one row each. */
    val tools: List<Tool> = Tool.entries

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
