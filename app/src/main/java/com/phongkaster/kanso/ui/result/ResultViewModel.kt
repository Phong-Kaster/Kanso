package com.phongkaster.kanso.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.edit.RenderedPhotoStore
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import com.phongkaster.kanso.di.ViewModelClearedProbe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the state of the Result screen, in its two uses (see [ResultUiState]).
 *
 * Right after Done ([viewer] false, [photoUri] null) the picture is whatever Done left in [store],
 * the hand-off store: a navigation key is saved as text and cannot carry a bitmap, so Done puts the
 * picture there and this ViewModel reads it once, when it is built. An empty store (the app was
 * killed and restored) gives [ResultPhoto.Empty], "Nothing to show". Nothing is in the gallery yet:
 * [save] is the one place the picture is written there, through [saver].
 *
 * As the photo viewer ([viewer] true, [photoUri] the text of a photo opened from My edits) the
 * photo is decoded by [loader] when the screen asks ([load]), and nothing is ever saved.
 *
 * [probe] is told when this ViewModel is cleared, so tests can check that leaving the screen frees
 * it, as they do for the other screens. The picture lives here rather than in the screen function
 * because this ViewModel belongs to the screen's navigation entry: it outlives a rotation, and it is
 * freed as soon as the screen leaves the back stack.
 */
class ResultViewModel(
    private val photoUri: String?,
    viewer: Boolean,
    private val store: RenderedPhotoStore,
    private val loader: PhotoLoader,
    private val saver: PhotoSaver,
    private val probe: ViewModelClearedProbe,
) : ViewModel() {

    /** True for the My edits viewer; it needs a photo Uri to show anything. */
    private val isViewer = viewer && photoUri != null

    /** The photo's own Uri when Done handed over an untouched photo; Save then copies that file exactly. */
    private val untouchedUri: String? = if (isViewer) null else store.originalUri()

    /** The current state behind [state]; only this ViewModel changes it. */
    private val mutableState = MutableStateFlow(
        ResultUiState(
            photoUri = photoUri,
            viewer = isViewer,
            photo = if (isViewer) {
                ResultPhoto.Loading
            } else {
                // Read the hand-off once: what Done rendered, or nothing.
                store.peek()?.let { ResultPhoto.Ready(it) } ?: ResultPhoto.Empty
            },
        ),
    )

    /**
     * What the Result screen shows, as one snapshot the screen can read at any moment: the picture
     * and where saving it stands.
     */
    val state: StateFlow<ResultUiState> = mutableState.asStateFlow()

    /** The size the viewer's photo was last asked for, so the same request is not decoded twice. */
    private var requestedMaxEdgePx: Int? = null

    /** The decode in progress, if any; a request for a new size cancels and replaces it. */
    private var loadJob: Job? = null

    /**
     * In the viewer, decodes the photo with its longest edge at most [maxEdgePx] pixels and puts
     * the result in [state]: [ResultPhoto.Ready] with the picture, or [ResultPhoto.Unreadable] when
     * it cannot be opened or decoded. Right after Done the picture is already in memory, so this
     * does nothing.
     *
     * The screen calls this every time it appears, including after a rotation; asking again for
     * the same size does nothing. Asking for a new size cancels the decode still running for the
     * old one, whose result nobody wants any more.
     */
    fun load(maxEdgePx: Int) {
        val uri = photoUri
        if (!isViewer || uri == null) return
        if (requestedMaxEdgePx == maxEdgePx) return
        requestedMaxEdgePx = maxEdgePx
        loadJob?.cancel()
        // viewModelScope is a coroutine scope tied to this ViewModel: it is cancelled when the
        // ViewModel is cleared, which stops a decode no screen will show. The loader already does
        // its heavy work on Dispatchers.IO.
        loadJob = viewModelScope.launch {
            val loaded = loader.load(uri, maxEdgePx)
            val photo: ResultPhoto = if (loaded != null) ResultPhoto.Ready(loaded.bitmap) else ResultPhoto.Unreadable
            // update swaps in a changed copy of the snapshot; the old snapshot is never edited.
            mutableState.update { current -> current.copy(photo = photo) }
        }
    }

    /**
     * Tells whether Save must first ask for the storage-write permission (Android 8.1 and 9 only,
     * and only while it is not granted). The screen asks for it before calling [save].
     */
    fun needsWritePermission(): Boolean = saver.needsWritePermission()

    /**
     * Saves the rendered picture into `Pictures/Kanso` as a new file, in the background: a PNG
     * when it has see-through pixels, else a JPEG at quality 95 (see `PhotoSaver.save`). This is
     * the only place the Result screen writes to the gallery.
     *
     * Nothing happens in the viewer, without a picture, while a save is running (so one tap never
     * makes two files) or once the picture is saved (so it cannot be saved twice).
     */
    fun save() {
        val current = mutableState.value
        if (current.viewer) return
        val bitmap = (current.photo as? ResultPhoto.Ready)?.bitmap ?: return
        if (current.save == ResultUiState.SaveState.Saving || current.save is ResultUiState.SaveState.Saved) return
        mutableState.update { it.copy(save = ResultUiState.SaveState.Saving) }
        viewModelScope.launch {
            val outcome = when (val result = untouchedUri?.let { saver.save(it) } ?: saver.save(bitmap)) {
                is SaveResult.Saved -> ResultUiState.SaveState.Saved(result.keptOriginalOrientation)
                SaveResult.Failed -> ResultUiState.SaveState.Failed
            }
            mutableState.update { it.copy(save = outcome) }
        }
    }

    /**
     * Records that the person refused the storage-write permission, so the screen says why. A
     * save already running or done is left as it is.
     */
    fun onWritePermissionDenied() {
        mutableState.update { current ->
            if (current.save == ResultUiState.SaveState.Saving || current.save is ResultUiState.SaveState.Saved) {
                current
            } else {
                current.copy(save = ResultUiState.SaveState.PermissionDenied)
            }
        }
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        // The picture was only waiting here for this screen; a full-size bitmap must not stay in memory
        // for the rest of the process (the viewer never used the store).
        if (!isViewer) store.clear()
        probe.recordCleared(this)
        super.onCleared()
    }
}
