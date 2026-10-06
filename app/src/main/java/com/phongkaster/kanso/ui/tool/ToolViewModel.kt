package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
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
 * Where saving the photo stands, for the Save action and the message under the photo.
 */
sealed interface SaveUiState {

    /** Nothing saved yet on this visit, or the last message has been replaced. */
    data object Idle : SaveUiState

    /** A save is running; Save cannot be tapped again until it ends. */
    data object Saving : SaveUiState

    /**
     * The photo was saved. [keptOriginalOrientation] is true when it was too large to turn
     * upright and was copied as it is; the screen then says some apps may show it sideways.
     */
    data class Saved(val keptOriginalOrientation: Boolean) : SaveUiState

    /** The photo could not be saved; nothing was written. */
    data object Failed : SaveUiState

    /** The person refused the storage-write permission (Android 8.1 and 9); nothing was written. */
    data object PermissionDenied : SaveUiState
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
 * that leaving the screen frees it; [saver] writes the photo into the phone's `Pictures/Kanso`
 * album when the person taps Save.
 */
class ToolViewModel(
    val tool: Tool,
    val photoUri: String,
    private val loader: PhotoLoader,
    private val probe: ViewModelClearedProbe,
    private val saver: PhotoSaver,
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

    /** The current state behind [saveState]; only this ViewModel changes it. */
    private val mutableSaveState = MutableStateFlow<SaveUiState>(SaveUiState.Idle)

    /** Where saving stands, as state the screen can read at any moment. */
    val saveState: StateFlow<SaveUiState> = mutableSaveState.asStateFlow()

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
        if (mutableSaveState.value == SaveUiState.Saving) return
        mutableSaveState.value = SaveUiState.Saving
        viewModelScope.launch {
            when (val result = saver.save(photoUri)) {
                is SaveResult.Saved -> {
                    mutableSaveState.value = SaveUiState.Saved(result.keptOriginalOrientation)
                    savedChannel.send(result.uri)
                }
                SaveResult.Failed -> mutableSaveState.value = SaveUiState.Failed
            }
        }
    }

    /** Records that the person refused the storage-write permission, so the screen says why. */
    fun onWritePermissionDenied() {
        mutableSaveState.value = SaveUiState.PermissionDenied
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
