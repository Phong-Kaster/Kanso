package com.phongkaster.kanso.ui.myedits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.di.ViewModelClearedProbe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the My edits tab's state, as one [MyEditsUiState]: the photos Kanso has saved, newest
 * first, and whether they are still being read.
 *
 * The photos come from [savedPhotos]; in the app it asks the phone's MediaStore for Kanso's own
 * album, in tests it is a fake. The ViewModel belongs to the tab's navigation entry. Leaving the
 * tab removes that entry, so the ViewModel is cleared (it reports that to [probe], so a test can
 * check it) and a fresh one reads the photos again next time; the screen function also asks for a
 * new read each time it is shown (see [refresh]), so a photo saved a moment ago is always listed.
 */
class MyEditsViewModel(
    private val savedPhotos: SavedPhotoSource,
    private val probe: ViewModelClearedProbe,
) : ViewModel() {

    /** The writable state; only this class changes it. It starts as loading with no photos. */
    private val mutableState = MutableStateFlow(MyEditsUiState())

    /**
     * What My edits shows, as the one StateFlow the screen reads (see HomeUiState for the
     * pattern). asStateFlow() hands out a read-only view of [mutableState].
     */
    val state: StateFlow<MyEditsUiState> = mutableState.asStateFlow()

    /** The read in progress, if any, so a newer read can cancel it. */
    private var loadJob: Job? = null

    /**
     * Reads Kanso's saved photos again. The screen function calls it each time the tab is shown.
     *
     * The read runs in viewModelScope, a coroutine scope that is cancelled when this ViewModel is
     * cleared; the source itself moves the slow part to a background thread, so the main thread
     * keeps drawing. The photos already shown stay on screen until the new list arrives. The
     * list is put newest first by the time each photo was last changed, whatever order the source
     * gave (photos changed at the same second keep the source's order). A read that fails gives
     * the empty list, so the screen shows its "no edits yet" message instead of crashing.
     */
    fun refresh() {
        loadJob?.cancel()
        mutableState.update { it.copy(loading = true) }
        loadJob = viewModelScope.launch {
            val photos = try {
                savedPhotos.savedPhotos()
            } catch (cancelled: CancellationException) {
                // A newer read replaced this one; cancellation must travel on, not be swallowed.
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            // sortedByDescending is stable: equal times keep the order the source gave.
            mutableState.value = MyEditsUiState(
                photos = photos.sortedByDescending { it.modifiedSeconds },
                loading = false,
            )
        }
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
