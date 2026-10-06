package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the tool screen shows in place of the photo: still loading, the photo, or a message.
 */
sealed interface ToolUiState {

    /** The photo is being decoded; nothing is shown yet. */
    data object Loading : ToolUiState

    /** The photo is decoded: [bitmap] is upright and sized for the screen. */
    data class Ready(val bitmap: Bitmap) : ToolUiState

    /** The photo is missing, not readable by Kanso, or not an image. */
    data object Unreadable : ToolUiState
}

/**
 * Holds the state of the screen where [tool] works on the photo named by [photoUri].
 *
 * The tool and the photo come from the navigation key (`ToolPhoto`), which is saved with the
 * back stack; this ViewModel only keeps what can be worked out again from them, the decoded
 * photo. That is why nothing here needs saving: after the app is killed, the key comes back and
 * the photo is simply decoded again.
 *
 * [loader] decodes the photo; [probe] is told when this ViewModel is cleared, so tests can check
 * that leaving the screen frees it.
 */
class ToolViewModel(
    val tool: Tool,
    val photoUri: String,
    private val loader: PhotoLoader,
    private val probe: ViewModelClearedProbe,
) : ViewModel() {

    /** The current state behind [state]; only this ViewModel changes it. */
    private val mutableState = MutableStateFlow<ToolUiState>(ToolUiState.Loading)

    /** What the screen shows, as state it can read at any moment. */
    val state: StateFlow<ToolUiState> = mutableState.asStateFlow()

    /** The size the photo was last asked for, so the same request is not decoded twice. */
    private var requestedMaxEdgePx: Int? = null

    /** The decode in progress, if any; a request for a new size replaces it. */
    private var loadJob: Job? = null

    /**
     * Decodes the photo with its longest edge at most [maxEdgePx] pixels, in the background.
     *
     * The screen calls this every time it appears, including after rotation; asking again for
     * the same size does nothing, so the photo is decoded only once.
     */
    fun load(maxEdgePx: Int) {
        if (requestedMaxEdgePx == maxEdgePx) return
        requestedMaxEdgePx = maxEdgePx
        loadJob?.cancel()
        // viewModelScope is cancelled when this ViewModel is cleared, which stops a decode that
        // is no longer wanted. The loader itself moves the heavy work off the main thread.
        loadJob = viewModelScope.launch {
            val photo = loader.load(photoUri, maxEdgePx)
            mutableState.value = if (photo != null) {
                ToolUiState.Ready(photo.bitmap)
            } else {
                ToolUiState.Unreadable
            }
        }
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
