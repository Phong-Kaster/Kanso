package com.phongkaster.kanso.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.di.ViewModelClearedProbe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the state of the Saved screen, the screen shown after a photo is saved: which photo it
 * is, and that photo decoded for display.
 *
 * [photoUri] is the text of the Uri the save returned. It comes from the navigation key (`Saved`),
 * which is kept with the back stack, so after the app is killed and restored the key comes back
 * and the photo is simply decoded again; nothing here needs saving. [loader] decodes the photo;
 * [probe] is told when this ViewModel is cleared, so tests can check that leaving the screen frees
 * it, as they do for the other screens.
 *
 * The decoded photo lives here rather than in the screen function because this ViewModel belongs
 * to the screen's navigation entry: it outlives a rotation, which draws the screen again from
 * scratch, so the photo is not decoded twice, and it is freed as soon as the screen leaves the
 * back stack.
 */
class SavedViewModel(
    private val photoUri: String,
    private val loader: PhotoLoader,
    private val probe: ViewModelClearedProbe,
) : ViewModel() {

    /** The current state behind [state]; only this ViewModel changes it. */
    private val mutableState = MutableStateFlow(SavedUiState(photoUri = photoUri))

    /**
     * What the Saved screen shows, as one snapshot the screen can read at any moment. It starts
     * with the photo still loading and is replaced by a new snapshot once the photo is decoded.
     */
    val state: StateFlow<SavedUiState> = mutableState.asStateFlow()

    /** The size the photo was last asked for, so the same request is not decoded twice. */
    private var requestedMaxEdgePx: Int? = null

    /** The decode in progress, if any; a request for a new size cancels and replaces it. */
    private var loadJob: Job? = null

    /**
     * Decodes the photo with its longest edge at most [maxEdgePx] pixels and puts the result in
     * [state]: [SavedPhoto.Ready] with the picture, or [SavedPhoto.Unreadable] when the photo
     * cannot be opened or decoded.
     *
     * The screen calls this every time it appears, including after a rotation; asking again for
     * the same size does nothing, so the photo is decoded only once. Asking for a new size cancels
     * the decode still running for the old one, whose result nobody wants any more.
     */
    fun load(maxEdgePx: Int) {
        if (requestedMaxEdgePx == maxEdgePx) return
        requestedMaxEdgePx = maxEdgePx
        loadJob?.cancel()
        // viewModelScope is a coroutine scope tied to this ViewModel: it is cancelled when the
        // ViewModel is cleared, which stops a decode no screen will show. No move to a background
        // thread is needed here: the loader already does its heavy work on Dispatchers.IO.
        loadJob = viewModelScope.launch {
            val loaded = loader.load(photoUri, maxEdgePx)
            val photo: SavedPhoto = if (loaded != null) SavedPhoto.Ready(loaded.bitmap) else SavedPhoto.Unreadable
            // update swaps in a changed copy of the snapshot; the old snapshot is never edited.
            mutableState.update { current -> current.copy(photo = photo) }
        }
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
