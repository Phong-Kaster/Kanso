package com.phongkaster.kanso.ui.tool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the state of a tool screen: [tool] working on the photo named by [photoUri].
 *
 * Each tool has a screen of its own, opened from its navigation key (`ToolPhoto(tool, photoUri)`):
 * one ViewModel class serves all five, told by the key which tool it is for. The tool never
 * changes for this ViewModel's whole life, so the state's `tool` is always the key's tool; to work
 * with another tool the person goes back and opens that tool's own screen.
 *
 * Both facts come from the key, which is saved with the back stack. The decoded photo is not
 * saved anywhere: after the app is killed, the key comes back and the photo is simply decoded
 * again.
 *
 * [loader] decodes the photo; [probe] is told when this ViewModel is cleared, so tests can check
 * that leaving the screen frees it; [saver] writes the photo into the phone's `Pictures/Kanso`
 * album when the person taps Save.
 */
class ToolViewModel(
    private val tool: Tool,
    private val photoUri: String,
    private val loader: PhotoLoader,
    private val probe: ViewModelClearedProbe,
    private val saver: PhotoSaver,
) : ViewModel() {

    /**
     * The current state behind [state]; only this ViewModel changes it. It starts with the tool
     * and the photo's Uri from the key, nothing decoded yet and nothing saved.
     */
    private val mutableState = MutableStateFlow(ToolUiState(tool, photoUri))

    /**
     * What the screen shows (its tool, the photo, and where saving stands), as one snapshot it
     * can read at any moment.
     */
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
            val shown = if (photo != null) {
                ToolUiState.PhotoState.Ready(photo.bitmap)
            } else {
                ToolUiState.PhotoState.Unreadable
            }
            // update { } changes only the photo part of the snapshot as it is right now, so a
            // decode that finishes while a save runs leaves the save part alone.
            mutableState.update { it.copy(photo = shown) }
        }
    }

    /**
     * Carries the Uri (as text) of each newly saved photo to the screen, once.
     *
     * A Channel is a queue between coroutines: each item is received by exactly one reader, one
     * time. That makes it right for "the photo was saved, move on" — unlike state, it is not
     * delivered again when the screen is drawn again after a rotation.
     */
    private val savedChannel = Channel<String>(Channel.BUFFERED)

    /** The saved photos' Uris, each delivered once; the screen calls its `onSaved` with them. */
    val savedEvents: Flow<String> = savedChannel.receiveAsFlow()

    /**
     * Tells whether Save must first ask for the storage-write permission (Android 8.1 and 9
     * only, and only while it is not granted). The screen asks for it before calling [save].
     */
    fun needsWritePermission(): Boolean = saver.needsWritePermission()

    /**
     * Saves the photo into `Pictures/Kanso`, in the background. A tap while a save is already
     * running is ignored, so one tap never makes two files.
     */
    fun save() {
        if (mutableState.value.save == ToolUiState.SaveState.Saving) return
        mutableState.update { it.copy(save = ToolUiState.SaveState.Saving) }
        viewModelScope.launch {
            when (val result = saver.save(photoUri)) {
                is SaveResult.Saved -> {
                    mutableState.update {
                        it.copy(save = ToolUiState.SaveState.Saved(result.keptOriginalOrientation))
                    }
                    savedChannel.send(result.uri)
                }
                SaveResult.Failed -> mutableState.update {
                    it.copy(save = ToolUiState.SaveState.Failed)
                }
            }
        }
    }

    /** Records that the person refused the storage-write permission, so the screen says why. */
    fun onWritePermissionDenied() {
        mutableState.update { it.copy(save = ToolUiState.SaveState.PermissionDenied) }
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
