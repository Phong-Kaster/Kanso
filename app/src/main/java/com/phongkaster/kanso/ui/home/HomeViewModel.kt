package com.phongkaster.kanso.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the Home screen's state: one [HomeUiState] with the tools of the hero carousel (the four
 * AI tools), the tools of the row of round shortcuts (all five), each in display order, and the
 * newest photos Kanso has saved, for the "Recent edits" strip.
 *
 * A ViewModel outlives a single drawing of the screen: Android keeps it across rotation and
 * clears it only when its owner is gone for good. Here the owner is Home's navigation entry,
 * so it lives as long as Home is on the back stack (which is always). [probe] lets tests check
 * that this really holds.
 *
 * The recent photos come from [savedPhotos], the same source the My edits tab reads (in the app
 * it asks the phone's MediaStore for Kanso's own album, in tests it is a fake), and are read the
 * same way (see [refresh]). Because Home's ViewModel lives as long as the app, a photo saved
 * since the last read only appears once Home asks again, which the screen function does each
 * time Home is shown.
 */
class HomeViewModel(
    private val probe: ViewModelClearedProbe,
    private val savedPhotos: SavedPhotoSource,
) : ViewModel() {

    /**
     * The writable holder of Home's state. Only this class may put a new snapshot into it. The
     * tools never change: the four AI tools on the carousel's banners, and every tool, in the
     * order the [Tool] enum declares them, on the shortcuts. The camera is in neither: it takes a
     * photo instead of editing one, so it is the round button beside the floating tab bar. The
     * strip starts as loading with no photos: nothing has been read yet.
     */
    private val mutableState = MutableStateFlow(
        HomeUiState(
            carouselTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.BEAUTIFY, Tool.ARTISTIC),
            shortcutTools = Tool.entries.toList(),
            recentPhotos = emptyList(),
            recentLoading = true,
        ),
    )

    /**
     * What Home shows, as the one StateFlow the screen reads (see [HomeUiState] for the whole
     * pattern). asStateFlow() hands out a read-only view of [mutableState]: the screen can read
     * every snapshot, but it has no way to put one in itself.
     */
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    /** The read in progress, if any, so a newer read can cancel it. */
    private var loadJob: Job? = null

    /**
     * Reads Kanso's saved photos again for the "Recent edits" strip. The screen function calls it
     * each time Home is shown, so coming back from the Result screen lists the photo just saved
     * first.
     *
     * The read runs in viewModelScope, a coroutine scope that is cancelled when this ViewModel is
     * cleared; the source moves the slow part to a background thread, so the main thread keeps
     * drawing. Only `recentLoading` changes while it runs: the photos already shown stay in the
     * snapshot, so the strip keeps its cards instead of flashing placeholders on every return to
     * Home. The photos are put newest first by the time each was last changed, whatever order the
     * source gave (equal times keep the source's order), and only the newest [RECENT_LIMIT] are
     * kept: the strip is a glimpse, the My edits tab lists them all. A read that fails gives no
     * photos, so the strip shows its short message instead of crashing.
     */
    fun refresh() {
        loadJob?.cancel()
        mutableState.update { it.copy(recentLoading = true) }
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
            val newest = photos.sortedByDescending { it.modifiedSeconds }.take(RECENT_LIMIT)
            mutableState.update { it.copy(recentPhotos = newest, recentLoading = false) }
        }
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }

    /** Values that belong to Home's state as a whole. */
    companion object {
        /** How many photos the "Recent edits" strip shows at most: the newest ten. */
        const val RECENT_LIMIT = 10
    }
}
