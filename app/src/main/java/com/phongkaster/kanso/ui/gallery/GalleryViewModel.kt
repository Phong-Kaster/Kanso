package com.phongkaster.kanso.ui.gallery

import androidx.lifecycle.ViewModel
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool

/**
 * Holds the state of a tool's photo gallery: which [tool] the chosen photo will be opened in.
 *
 * It belongs to the gallery's navigation entry, so it survives rotation and is cleared as soon
 * as the gallery is popped off the back stack. It reports that to [probe], so a test can check it.
 */
class GalleryViewModel(
    val tool: Tool,
    private val probe: ViewModelClearedProbe,
) : ViewModel() {

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
