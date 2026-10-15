package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 *
 * Editing works on a copy held in memory, never on the photo's file: [apply] runs one edit (a
 * [ToolProcessor]) on the photo as it is now shown, and the result becomes the new shown photo.
 * Every earlier version is kept, so [undo] can step back one edit at a time all the way to the
 * original. [registry] finds the processor built for this screen's tool, for [runRegistered].
 * [workDispatcher] is the background thread pool that shrinks a large photo before an edit, so
 * even that never runs on the main thread; tests can pass their own.
 */
class ToolViewModel(
    private val tool: Tool,
    private val photoUri: String,
    private val loader: PhotoLoader,
    private val probe: ViewModelClearedProbe,
    private val saver: PhotoSaver,
    private val registry: ProcessorRegistry = ProcessorRegistry(),
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
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
     * The versions of the photo before each edit, oldest first, so [undo] can step back.
     *
     * An entry is the `edited` value that was shown before an edit: null stands for "the unedited
     * original", which is not copied here because the state's `photo` always keeps it. The
     * bitmaps live only in memory and only as long as this ViewModel: they are not part of the
     * state, because the screen never needs to draw them.
     */
    private val history = ArrayDeque<Bitmap?>()

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
     * Runs one edit, [processor], on the photo as it is shown now, in the background.
     *
     * Nothing happens while another edit is running (one at a time), or while there is no photo
     * yet. The photo is first shrunk, if it is larger, to the size this tool can handle on a
     * phone ([WorkingSize.maxEdgeFor]), so a very large photo never runs the phone out of
     * memory. While the edit runs, the state says how far along it is. When it works, the result
     * becomes the shown photo and the one before it is kept for [undo]; when it fails, the shown
     * photo stays exactly as it was and the state says why, in [ToolUiState.failure].
     * Nothing happens either while the person is comparing with the original
     * ([ToolUiState.comparing]): every editing control is disabled then.
     */
    fun apply(processor: ToolProcessor, fromOriginal: Boolean = false) {
        val current = mutableState.value
        if (current.comparing) return
        if (current.processing is ToolUiState.Processing.Running) return
        val original = (current.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        // What the person sees right now: the last edit, or the original if there is none.
        val previousEdit = current.edited
        // A slider tool (Retouch, Artistic) re-runs from the untouched photo, so moving its
        // strength replaces the last result instead of stacking a second smoothing on top.
        val shown = if (fromOriginal) original else previousEdit ?: original
        mutableState.update {
            it.copy(processing = ToolUiState.Processing.Running(0f), failure = null)
        }
        viewModelScope.launch {
            val result = try {
                // Shrinking is real work on a large photo, so it happens on a background thread.
                val input = withContext(workDispatcher) {
                    if (fromOriginal) fittedOriginal(shown) else WorkingSize.fit(shown, WorkingSize.maxEdgeFor(tool))
                }
                // The processor moves its own heavy work off the main thread (its contract), and
                // running it on the work dispatcher makes that hold even if one forgets.
                withContext(workDispatcher) {
                    processor.process(input) { progress -> reportProgress(progress) }
                }
            } catch (cancelled: CancellationException) {
                // Leaving the screen cancels the edit: that is not a failure, so it must go on
                // up and stop this coroutine rather than be reported. A cancellation that did
                // not come from leaving (a timeout inside the processor) still ends the run, so
                // the screen must not stay locked as if it were running.
                mutableState.update { it.copy(processing = ToolUiState.Processing.Idle) }
                throw cancelled
            } catch (outOfMemory: OutOfMemoryError) {
                EditResult.Failed(EditFailure.OutOfMemory)
            } catch (other: Throwable) {
                EditResult.Failed(EditFailure.Other)
            }
            when (result) {
                is EditResult.Edited -> {
                    // A slider re-run replaces the slider's last result, so it adds no Undo step
                    // (and no 12 MB picture) of its own: Undo then goes back past the whole slider
                    // session, to what was there before it.
                    if (!(fromOriginal && lastEditWasFromOriginal)) history.addLast(previousEdit)
                    lastEditWasFromOriginal = fromOriginal
                    mutableState.update {
                        it.copy(
                            edited = result.bitmap,
                            canUndo = true,
                            failure = null,
                            processing = ToolUiState.Processing.Idle,
                        )
                    }
                }
                is EditResult.Failed -> mutableState.update {
                    it.copy(failure = result.failure, processing = ToolUiState.Processing.Idle)
                }
            }
        }
    }

    /** True while the shown edit came from a run on the original photo (a slider tool's result). */
    private var lastEditWasFromOriginal = false

    /**
     * The original photo and its working-size copy, remembered by [fittedOriginal].
     */
    private var fittedOriginalCache: Pair<Bitmap, Bitmap>? = null

    /**
     * The working-size copy of the original photo, made once and handed back on every later call,
     * so a processor that caches by bitmap (Artistic keeps its styled picture) sees the same
     * object each time a strength slider moves and does not run its model again.
     */
    private fun fittedOriginal(original: Bitmap): Bitmap {
        fittedOriginalCache?.let { (source, fitted) -> if (source === original) return fitted }
        val fitted = WorkingSize.fit(original, WorkingSize.maxEdgeFor(tool))
        fittedOriginalCache = original to fitted
        return fitted
    }

    /**
     * Runs the edit built for this screen's tool, if one has been built yet; with none, nothing
     * happens. A tool's panel that has a single "do it" action calls this. A tool with settings
     * (a strength, a style) passes [configure], which sets them on the registered processor just
     * before it runs, and [fromOriginal] true to run on the untouched photo (see [apply]).
     */
    fun runRegistered(fromOriginal: Boolean = false, configure: (ToolProcessor) -> Unit = {}) {
        val processor = registry.processorFor(tool) ?: return
        // A tap during a run, or while comparing, is ignored by apply, so its settings must not
        // reach the processor.
        if (mutableState.value.processing is ToolUiState.Processing.Running) return
        if (mutableState.value.comparing) return
        configure(processor)
        apply(processor, fromOriginal)
    }

    /**
     * Records how far the running edit is, from 0 to 1. A report that arrives after the edit has
     * ended (a processor may call from its own thread a moment late) is ignored, so it can never
     * make a finished edit look as if it were running again.
     */
    private fun reportProgress(progress: Float) {
        if (progress.isNaN()) return
        val clamped = progress.coerceIn(0f, 1f)
        mutableState.update {
            if (it.processing is ToolUiState.Processing.Running) {
                it.copy(processing = ToolUiState.Processing.Running(clamped))
            } else {
                it
            }
        }
    }

    /**
     * Takes back the last edit: the photo shown before it is shown again. Undoing every edit
     * returns to the unedited original. Nothing happens while an edit runs, while the person is
     * comparing with the original, or when there is nothing to take back.
     */
    fun undo() {
        if (mutableState.value.comparing) return
        if (mutableState.value.processing is ToolUiState.Processing.Running) return
        if (history.isEmpty()) return
        val before = history.removeLast()
        lastEditWasFromOriginal = false
        mutableState.update {
            it.copy(edited = before, canUndo = history.isNotEmpty(), failure = null)
        }
    }

    /**
     * Opens the sheet of the Edit screen's [tool] under the photo, closing any other one, since
     * only one sheet is open at a time. Tapping the tool whose sheet is already open closes it
     * again, so the row's entries work as toggles.
     */
    fun openTool(tool: EditTool) {
        mutableState.update { it.copy(openTool = if (it.openTool == tool) null else tool) }
    }

    /** Closes the open tool sheet, if any, leaving the photo and the row of tools. */
    fun closeTool() {
        mutableState.update { it.copy(openTool = null) }
    }

    /**
     * Switches Compare [on] or off. While it is on, the screen shows the untouched original and
     * every editing control is disabled ([apply] and [undo] do nothing); switching it off shows
     * the edit again. The edit itself is never touched, so nothing is lost by comparing.
     */
    fun setComparing(on: Boolean) {
        mutableState.update { it.copy(comparing = on) }
    }

    /** Hides the message about the last failed edit, once the person has read it. */
    fun dismissFailure() {
        mutableState.update { it.copy(failure = null) }
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
     * running is ignored, so one tap never makes two files; so is a tap while an edit is running,
     * so a half-made edit is never saved.
     *
     * When the person has edited the photo, the edited photo shown on screen is saved, exactly
     * at the size they saw it, as a new file; otherwise the original photo is copied as it is.
     * Either way the original photo's file is never changed.
     */
    fun save() {
        val current = mutableState.value
        if (current.save == ToolUiState.SaveState.Saving) return
        if (current.processing is ToolUiState.Processing.Running) return
        // Read once, now, so an edit that lands while the save runs cannot change what is saved.
        val edited = current.edited
        mutableState.update { it.copy(save = ToolUiState.SaveState.Saving) }
        viewModelScope.launch {
            val result = if (edited != null) saver.save(edited) else saver.save(photoUri)
            when (result) {
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
