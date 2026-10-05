package com.phongkaster.kanso.ui.placeholder

import androidx.lifecycle.ViewModel
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool

/**
 * Holds the state of a tool's stand-in screen: which [tool] it stands for.
 *
 * It belongs to that screen's navigation entry, so it survives rotation and is cleared as soon
 * as the screen is popped off the back stack. It reports that to [probe], so a test can check it.
 */
class PlaceholderViewModel(
    val tool: Tool,
    private val probe: ViewModelClearedProbe,
) : ViewModel() {

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
