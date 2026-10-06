package com.phongkaster.kanso.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Everything the gallery shows at one moment.
 *
 * [albums] feed the album switcher; [selectedAlbumId] is the chosen album, or null for
 * "All photos"; [photos] are that album's photos, newest first; [loading] is true while the
 * photo library is being read.
 */
data class GalleryUiState(
    val albums: List<Album> = emptyList(),
    val selectedAlbumId: Long? = null,
    val photos: List<Photo> = emptyList(),
    val loading: Boolean = true,
)

/**
 * Holds the state of a tool's photo gallery: which [tool] the chosen photo will be opened in,
 * the albums, the chosen album and its photos.
 *
 * The photos come from [photoSource]; in the app it reads the phone's library, in tests it is a
 * fake. It belongs to the gallery's navigation entry, so it survives rotation and is cleared as
 * soon as the gallery is popped off the back stack. It reports that to [probe], so a test can
 * check it.
 */
class GalleryViewModel(
    val tool: Tool,
    private val probe: ViewModelClearedProbe,
    private val photoSource: PhotoSource,
) : ViewModel() {

    /** The writable state; only this class changes it. */
    private val mutableState = MutableStateFlow(GalleryUiState())

    /**
     * What the gallery shows. A StateFlow always holds a current value and tells its readers
     * when it changes; the screen reads it with collectAsState and redraws on every change.
     */
    val state: StateFlow<GalleryUiState> = mutableState.asStateFlow()

    /** The read in progress, if any, so a newer read can cancel it. */
    private var loadJob: Job? = null

    /**
     * Reads the albums and the chosen album's photos again. The screen calls it when it appears
     * and every time it comes back to the front, so photos shared or taken in the meantime (or
     * access granted in the system settings) show up.
     *
     * The read runs in viewModelScope, a coroutine scope cancelled when this ViewModel is cleared,
     * and moves to `Dispatchers.IO` so the main thread, which draws the screen, never waits for
     * the photo library. The photos already shown stay on screen until the new list arrives. If
     * the read fails (for example access was taken away), the gallery shows its empty state
     * instead of crashing.
     */
    fun refresh() {
        loadJob?.cancel()
        val wantedAlbumId = mutableState.value.selectedAlbumId
        mutableState.update { it.copy(loading = true) }
        loadJob = viewModelScope.launch {
            try {
                val (albums, albumId, photos) = withContext(Dispatchers.IO) {
                    val albums = photoSource.albums()
                    // If the chosen album no longer exists (its last photo was deleted), fall
                    // back to "All photos".
                    val albumId = wantedAlbumId?.takeIf { id -> albums.any { it.id == id } }
                    Triple(albums, albumId, photoSource.photos(albumId))
                }
                mutableState.value = GalleryUiState(
                    albums = albums,
                    selectedAlbumId = albumId,
                    photos = photos,
                    loading = false,
                )
            } catch (cancelled: CancellationException) {
                // A newer read replaced this one; cancellation must travel on, not be swallowed.
                throw cancelled
            } catch (failure: Exception) {
                mutableState.update { it.copy(photos = emptyList(), loading = false) }
            }
        }
    }

    /**
     * Shows the photos of album [albumId], or every photo when it is null ("All photos"), and
     * reads them from the library.
     */
    fun selectAlbum(albumId: Long?) {
        if (albumId == mutableState.value.selectedAlbumId) return
        // The old album's photos are dropped at once so they are never shown under the new
        // album's name; the grid stays away until the new photos arrive.
        mutableState.update { it.copy(selectedAlbumId = albumId, photos = emptyList()) }
        refresh()
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
