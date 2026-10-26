package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.edit.CatalogSticker
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.Sticker
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.edit.edit.StickerMapping
import com.phongkaster.kanso.data.edit.edit.EditSession
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
import com.phongkaster.kanso.data.edit.RenderedPhotoStore
import com.phongkaster.kanso.data.photo.PhotoLoader
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
 * that leaving the screen frees it; [store] is the hand-off to the Result screen: when the person
 * taps Done ([done]) the finished picture is put there and the screen opens Result, which reads it
 * back. This ViewModel never writes a file: saving and sharing belong to the Result screen.
 *
 * Editing works on a copy held in memory, never on the photo's file, in one of two ways.
 *
 * - **The Edit screen** keeps one [EditSettings] object for the whole visit, inside an
 *   [EditSession] (the settings plus the earlier settings Undo can go back to). Every change goes
 *   through [changeSettings] (one action: a filter chip, a turn, a flip), [commitCrop] (a crop
 *   gesture finished, which goes on through [changeSettings]), [previewSettings] (a slider being
 *   dragged), [commitSettings] (the slider let go) or [chooseCropRatio]; [undo] just
 *   goes back to earlier settings. None of them renders the full photo: the screen draws the
 *   crop, turns, flips and colour sliders live on top of the original as it draws, and only a
 *   filter is computed here, on a small copy of the original ([ToolUiState.filterPreview]). The
 *   full render from the ORIGINAL photo, with the WHOLE current settings, runs once, when the
 *   edit is finished ([done], [renderFinal]), so nothing is ever stacked on an earlier result.
 * - **The other tools** (Remove background, Enhance, Retouch, Artistic) use [apply]: it runs one
 *   edit (a [ToolProcessor]) on the photo as it is now shown, the result becomes the new shown
 *   photo, and every earlier version is kept so [undo] can step back one edit at a time.
 *
 * [registry] finds the processor built for this screen's tool, for [runRegistered].
 * [workDispatcher] is the background thread pool that shrinks a large photo and runs the edits,
 * so neither ever runs on the main thread; tests can pass their own.
 */
class ToolViewModel(
    private val tool: Tool,
    private val photoUri: String,
    private val loader: PhotoLoader,
    private val probe: ViewModelClearedProbe,
    private val registry: ProcessorRegistry = ProcessorRegistry(),
    private val store: RenderedPhotoStore = RenderedPhotoStore(),
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    /**
     * The current state behind [state]; only this ViewModel changes it. It starts with the tool
     * and the photo's Uri from the key and nothing decoded yet.
     */
    private val mutableState = MutableStateFlow(ToolUiState(tool, photoUri))

    /**
     * What the screen shows (its tool, the photo, the edit and whether Done is working), as one
     * snapshot it can read at any moment.
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
            // update { } changes only the photo part of the snapshot as it is right now, so the
            // rest of it (an edit, Done's state) is left alone.
            mutableState.update { it.copy(photo = shown) }
            // A filter chosen before the photo arrived (or kept from a smaller decode) is applied
            // to the newly decoded original now; everything else the screen draws by itself.
            if (tool == Tool.EDIT && photo != null &&
                (session.settings.hasFilter || session.settings.stickers.isNotEmpty())
            ) {
                requestPreview()
            }
        }
    }

    /**
     * Runs one edit, [processor], on the photo as it is shown now, in the background.
     *
     * Nothing happens while another edit is running (one at a time), or while there is no photo
     * yet. The photo is first shrunk, if it is larger, to the size this tool can handle on a
     * phone ([WorkingSize.maxEdgeFor]), so a very large photo never runs the phone out of
     * memory. While the edit runs, the state says how far along it is. When it works, the result
     * becomes the shown photo and the one before it is kept for [undo] (except on a repeat run
     * [fromOriginal], which replaces the last such result without an undo step of its own; see
     * the comment where the result lands); when it fails, the shown
     * photo stays exactly as it was and the state says why, in [ToolUiState.failure].
     * Nothing happens either while the person is comparing with the original
     * ([ToolUiState.comparing]): every editing control is disabled then.
     *
     * The Edit screen never uses this (Run 9): it would stack one change on the last result.
     * There, a call does nothing; its changes go through [changeSettings] and the calls beside it.
     */
    fun apply(processor: ToolProcessor, fromOriginal: Boolean = false) {
        if (tool == Tool.EDIT) return
        val current = mutableState.value
        if (current.comparing || current.finishing) return
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
    @Volatile
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
        if (mutableState.value.comparing || mutableState.value.finishing) return
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
     * Takes back the last edit. Nothing happens while the person is comparing with the original,
     * or when there is nothing to take back.
     *
     * On the Edit screen this goes back one action of the session (a slider release, a chip, a
     * crop, a turn, a sticker change), whichever sheet it came from, and a selected sticker that
     * the undo removed is no longer selected: the screen draws the earlier settings at once,
     * and if they hold a filter its preview is computed again from the original (the newest
     * request always wins). On the other tools' screens the photo shown before the last edit is shown
     * again (undoing every edit returns to the original), and nothing happens while an edit runs.
     */
    fun undo() {
        if (mutableState.value.comparing || mutableState.value.finishing) return
        if (tool == Tool.EDIT) {
            // A drag still under way counts as an action of its own, so it is settled first.
            if (!session.commit().canUndo) return
            publishSession(session.undo())
            // Undoing an "add sticker" removes that sticker: a selection pointing at it would
            // name a sticker that no longer exists, so it is cleared.
            val selected = mutableState.value.selectedStickerKey
            if (selected != null && session.settings.stickers.none { it.key == selected }) {
                mutableState.update { it.copy(selectedStickerKey = null) }
            }
            requestPreview()
            return
        }
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
        val current = mutableState.value
        val closing = current.openTool == tool
        if (current.openTool == EditTool.STICKERS) {
            // Leaving the Stickers sheet by another door (its own tool tapped again, or another
            // tool chosen) keeps what was done, like its Confirm button.
            stickersSnapshot = null
        } else if (tool == EditTool.STICKERS) {
            // Opening Stickers: remember the whole session (settings AND undo steps) so Cancel
            // can put everything back exactly as it is now.
            stickersSnapshot = session.commit()
        }
        mutableState.update {
            it.copy(
                openTool = if (closing) null else tool,
                selectedStickerKey = if (current.openTool == EditTool.STICKERS) null else it.selectedStickerKey,
                touchedStickerKey = null,
            )
        }
        // A gesture cut short by the sheet changing never commits: let the sticker rejoin the copy.
        if (current.touchedStickerKey != null) requestPreview()
    }

    /** Shows the Adjust tool's [slider] (one of its four sub-tools) in place of the one shown. */
    fun chooseAdjustSlider(slider: AdjustSlider) {
        mutableState.update { it.copy(adjustSlider = slider) }
    }

    /**
     * Closes the open tool sheet, if any, leaving the photo and the row of tools. Closing the
     * Stickers sheet this way keeps its changes, exactly like its Confirm button.
     */
    fun closeTool() {
        if (mutableState.value.openTool == EditTool.STICKERS) {
            confirmStickers()
            return
        }
        mutableState.update { it.copy(openTool = null) }
    }

    // --- The Stickers sheet --------------------------------------------------------------------------

    /**
     * The session as it was when the Stickers sheet opened, or null while that sheet is closed.
     * [cancelStickers] puts it back, so a Cancel throws away every sticker added, moved, flipped
     * or deleted since, along with the undo steps they made.
     */
    private var stickersSnapshot: EditSession? = null

    /** The key the next added sticker gets; it only grows, so a key is never used twice. */
    private var nextStickerKey = 1L

    /** How far a duplicated sticker sits from the first, as a fraction of the shown picture's width. */
    private val DUPLICATE_OFFSET = 0.04f

    /** The person tapped the category chip [category]: its stickers fill the sheet's grid. */
    fun chooseStickerCategory(category: StickerCategory) {
        mutableState.update { it.copy(stickerCategory = category) }
    }

    /**
     * The person tapped [sticker] in the grid: it lands at the middle of the picture they SEE
     * (after the crop, turns and flips), as one action (one Undo step), and is selected.
     *
     * [processorFor] is the recipe that draws settings, with the sticker pictures in it (see
     * [editProcessorFor]); it also becomes the recipe Done renders with. At the limit nothing
     * changes (no settings, no undo step) and [ToolUiState.stickerLimitShown] asks the screen for
     * its message. Nothing happens before the photo is decoded, or while comparing.
     */
    fun addSticker(sticker: CatalogSticker, processorFor: (EditSettings) -> ToolProcessor) {
        val current = mutableState.value
        if (current.comparing || current.finishing) return
        val original = (current.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        if (!session.settings.canAddSticker) {
            mutableState.update { it.copy(stickerLimitShown = true) }
            return
        }
        // The mapping turns "the middle of what is shown" into fractions of the original photo,
        // which is what a Sticker stores.
        val mapping = StickerMapping(original.width, original.height, session.settings)
        val key = nextStickerKey++
        this.processorFor = processorFor
        publishSession(session.addSticker(mapping.centredSticker(key, sticker.pictureId)))
        mutableState.update { it.copy(selectedStickerKey = key) }
        requestPreview()
    }

    /** True while the Stickers sheet is open and nothing (Compare, Done) has made it inert. */
    private fun stickerEditingOpen(): Boolean {
        val current = mutableState.value
        return tool == Tool.EDIT && current.openTool == EditTool.STICKERS &&
            !current.comparing && !current.finishing
    }

    /**
     * Selects the sticker with [key], or none for null (a touch on empty photo). Selecting is not
     * an action: the settings and the undo history stay as they are. A key that names no sticker
     * selects none. Ignored unless the Stickers sheet is open and not inert.
     */
    fun selectSticker(key: Long?) {
        if (!stickerEditingOpen()) return
        val known = key != null && session.settings.stickers.any { it.key == key }
        mutableState.update { it.copy(selectedStickerKey = if (known) key else null) }
    }

    /**
     * Copies the sticker [key] as one action (one Undo step): same picture, width, turn and
     * mirror, moved 4 percent of the SHOWN picture's width right and down, then selected. The
     * offset is worked out on the shown picture and carried back through [StickerMapping], so a
     * turned or flipped photo still moves the copy right and down as the person sees it. At the
     * limit nothing changes and the limit message is asked for, as in [addSticker].
     */
    fun duplicateSticker(key: Long, processorFor: (EditSettings) -> ToolProcessor) {
        if (!stickerEditingOpen()) return
        val original = (mutableState.value.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        val source = session.settings.stickers.firstOrNull { it.key == key } ?: return
        if (!session.settings.canAddSticker) {
            mutableState.update { it.copy(stickerLimitShown = true) }
            return
        }
        val settings = session.settings
        val mapping = StickerMapping(original.width, original.height, settings)
        // The shown picture's size in pixels: the crop, with width and height swapped by a quarter turn.
        val cropW = (settings.crop.right - settings.crop.left).coerceAtLeast(0.01f) * original.width
        val cropH = (settings.crop.bottom - settings.crop.top).coerceAtLeast(0.01f) * original.height
        val turned = settings.quarterTurns % 2 != 0
        val shownW = if (turned) cropH else cropW
        val shownH = if (turned) cropW else cropH
        val shownCentre = mapping.originalToShown(StickerMapping.Point(source.centreX, source.centreY))
        val moved = mapping.shownToOriginal(
            StickerMapping.Point(
                shownCentre.x + DUPLICATE_OFFSET,
                // The same distance in pixels as sideways, as a fraction of the height.
                shownCentre.y + DUPLICATE_OFFSET * shownW / shownH,
            ),
        )
        val newKey = nextStickerKey++
        this.processorFor = processorFor
        publishSession(
            session.duplicateSticker(key, newKey, moved.x - source.centreX, moved.y - source.centreY),
        )
        mutableState.update { it.copy(selectedStickerKey = newKey) }
        requestPreview()
    }

    /** Takes the sticker [key] off the photo as one action; nothing is selected afterwards. */
    fun deleteSticker(key: Long, processorFor: (EditSettings) -> ToolProcessor) {
        if (!stickerEditingOpen() || session.settings.stickers.none { it.key == key }) return
        this.processorFor = processorFor
        publishSession(session.deleteSticker(key))
        mutableState.update { it.copy(selectedStickerKey = null) }
        requestPreview()
    }

    /** Mirrors the sticker [key] as one action; flipping twice returns the original settings. */
    fun flipSticker(key: Long, processorFor: (EditSettings) -> ToolProcessor) {
        if (!stickerEditingOpen() || session.settings.stickers.none { it.key == key }) return
        this.processorFor = processorFor
        publishSession(session.flipSticker(key))
        requestPreview()
    }

    /**
     * A move, pinch, turn or resize is under way and the stickers now are [list]: the photo shows
     * it at once, but no undo entry is made until [commitSettings] (the last finger up).
     */
    fun previewStickers(list: List<Sticker>, processorFor: (EditSettings) -> ToolProcessor) {
        if (!stickerEditingOpen()) return
        // The first change of a gesture names the touched sticker: the one that differs from the
        // session's list. It is set BEFORE the preview is asked for, so that request already
        // leaves it out. Later changes of the same gesture keep it, so the request key of the
        // untouched stickers never moves during a drag and nothing is re-rendered.
        if (mutableState.value.touchedStickerKey == null) {
            val before = session.settings.stickers
            val changed = list.firstOrNull { it !in before }
            if (changed != null) mutableState.update { it.copy(touchedStickerKey = changed.key) }
        }
        previewSettings(session.settings.copy(stickers = list), processorFor)
    }

    /** The limit message has been shown long enough: hides it again. */
    fun onStickerLimitMessageShown() {
        mutableState.update { it.copy(stickerLimitShown = false) }
    }

    /**
     * The sheet's tick: keeps every change, closes the sheet and forgets the snapshot. Nothing
     * happens while Done renders or while comparing, when the sheet's controls are inert.
     */
    fun confirmStickers() {
        if (mutableState.value.comparing || mutableState.value.finishing) return
        stickersSnapshot = null
        // Clear the touched key BEFORE asking for a preview, so the request includes that sticker.
        mutableState.update { it.copy(openTool = null, selectedStickerKey = null, touchedStickerKey = null) }
        requestPreview()
    }

    /**
     * The sheet's cross, and the system Back while the sheet is open: puts the session back to the
     * snapshot taken at opening (settings and undo steps, so Undo is exactly as it was), closes the
     * sheet and clears the selection. The photo is drawn from the settings, so it follows at once
     * (the Edit screen's `edited` stays null; only a filter preview is asked for again).
     * Nothing happens while Done renders (it is drawing the current stickers) or while comparing.
     */
    fun cancelStickers() {
        if (mutableState.value.comparing || mutableState.value.finishing) return
        val snapshot = stickersSnapshot
        stickersSnapshot = null
        // Clear the touched key first: a gesture cancelled by this close never reaches its commit.
        mutableState.update { it.copy(openTool = null, selectedStickerKey = null, touchedStickerKey = null) }
        if (snapshot != null) {
            publishSession(snapshot)
        }
        requestPreview()
    }

    // --- The Edit screen's session ---------------------------------------------------------------

    /**
     * The Edit screen's settings and its undo entries, for the whole visit. It is immutable: each
     * change replaces it with a new session, and [publishSession] copies what the screen needs
     * into the state. Other tools never change it.
     */
    private var session = EditSession()

    /**
     * The last recipe the Edit panel gave for turning settings into a processor, kept so that
     * [undo] (which the panel does not call), the filter preview and the final render can draw
     * settings the same way.
     */
    private var processorFor: ((EditSettings) -> ToolProcessor)? = null

    /**
     * One action on the Edit screen (a filter chip, a quarter turn, a flip, a finished crop) made
     * the settings [new]: it becomes one undo entry. The screen draws the new settings at once;
     * only a filter needs work here, so the filter preview is asked for (see [requestPreview]).
     * [processorFor] builds the processor that draws a set of settings. Nothing happens while
     * comparing, when every control is disabled.
     */
    fun changeSettings(new: EditSettings, processorFor: (EditSettings) -> ToolProcessor) {
        if (mutableState.value.comparing || mutableState.value.finishing) return
        this.processorFor = processorFor
        publishSession(session.change(new))
        requestPreview()
    }

    /**
     * The person finished one gesture on the cropper (a handle dragged, or the rectangle moved),
     * leaving the crop rectangle [rect], given as fractions of the original photo. It becomes one
     * undo entry, keeping every other setting (the chosen crop shape included). A rectangle equal
     * to the current crop is not an action, so it is ignored: the cropper reports what it shows,
     * and after an undo it shows exactly the crop the session already holds. Nothing happens while
     * comparing, when the cropper is switched off.
     */
    fun commitCrop(rect: EditSettings.CropRect, processorFor: (EditSettings) -> ToolProcessor) {
        val current = mutableState.value
        if (current.comparing || current.finishing) return
        if (current.settings.crop == rect) return
        // Keep the recipe the Edit panel gave, so the filter cache stays the one panel and render share.
        changeSettings(current.settings.copy(crop = rect), this.processorFor ?: processorFor)
    }

    /**
     * A slider on the Edit screen is being dragged and stands at [new]: the photo follows it at
     * once (the screen applies a colour slider as it draws; a filter's intensity is computed on the
     * small preview copy), but no undo entry is made until [commitSettings]. Nothing happens while
     * comparing.
     */
    fun previewSettings(new: EditSettings, processorFor: (EditSettings) -> ToolProcessor) {
        if (mutableState.value.comparing || mutableState.value.finishing) return
        this.processorFor = processorFor
        publishSession(session.preview(new))
        requestPreview()
    }

    /**
     * The finger let go of a slider: the whole drag becomes one undo entry. The settings do not
     * change, so nothing new is asked for; the preview request the last drag step sent is still
     * the newest, so it is the one shown. This works even while comparing, so a drag is never left
     * half recorded.
     */
    fun commitSettings() {
        // The last finger is up: no sticker is touched any more, so the next preview request
        // includes the one that was (it joins the filtered copy).
        mutableState.update { it.copy(touchedStickerKey = null) }
        publishSession(session.commit())
        if (processorFor != null) requestPreview()
    }

    /**
     * The person chose the crop shape [ratio] (null or Free unlocks it), as one action: a fixed
     * ratio sets the largest centred rectangle of that shape inside the photo. The photo's shape
     * is read from the decoded original, before any turn, which is what [EditSession.cropRatioChosen]
     * expects. Nothing happens before the photo is decoded, or while comparing.
     */
    fun chooseCropRatio(ratio: CropRatio?, processorFor: (EditSettings) -> ToolProcessor) {
        val current = mutableState.value
        if (current.comparing || current.finishing) return
        val original = (current.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        val photoAspect = original.width.toFloat() / original.height
        this.processorFor = processorFor
        publishSession(session.cropRatioChosen(ratio, photoAspect))
        requestPreview()
    }

    /**
     * The system Back on the Edit screen, answered in steps: an open sheet is closed first; then,
     * with changes made, the "Discard your changes?" question is asked; with no changes the answer
     * is false, which tells the caller to leave the screen. True means Back was handled here.
     */
    fun onBackPressed(): Boolean {
        val current = mutableState.value
        if (current.openTool == EditTool.STICKERS) {
            // Back from the Stickers sheet is its Cancel: the sticker changes are thrown away.
            // While Done renders or while comparing the sheet is inert, so Back is consumed
            // (the screen stays) but cancels nothing.
            if (!current.comparing && !current.finishing) cancelStickers()
            return true
        }
        if (current.openTool != null) {
            closeTool()
            return true
        }
        if (!session.settings.isNeutral) {
            mutableState.update { it.copy(discardPrompt = true) }
            return true
        }
        return false
    }

    /** "Keep editing": hides the discard question and leaves every change as it was. */
    fun dismissDiscardPrompt() {
        mutableState.update { it.copy(discardPrompt = false) }
    }

    /**
     * "Discard": hides the question; the caller then leaves the screen, and this ViewModel, with
     * its changes, is cleared with it. The photo's file was never touched, so nothing is undone.
     */
    fun confirmDiscard() {
        mutableState.update { it.copy(discardPrompt = false) }
    }

    /**
     * Makes [new] the current session and shows its settings and whether Undo can be used. The
     * selection is pruned here, on every settings change: a selected key that is no longer one of
     * the stickers (the add was undone, the sticker deleted) becomes none.
     */
    private fun publishSession(new: EditSession) {
        session = new
        mutableState.update { state ->
            val selected = state.selectedStickerKey
            val stillThere = selected != null && new.settings.stickers.any { it.key == selected }
            val touched = state.touchedStickerKey
            val touchedStillThere = touched != null && new.settings.stickers.any { it.key == touched }
            state.copy(
                settings = new.settings,
                canUndo = new.canUndo,
                selectedStickerKey = if (stillThere) selected else null,
                touchedStickerKey = if (touchedStillThere) touched else null,
            )
        }
    }

    // --- The Edit screen's live filter preview -----------------------------------------------------

    /**
     * The filter settings waiting to be drawn on the small preview copy.
     *
     * A Channel is a queue between coroutines (see [doneChannel]). This one is CONFLATED: it
     * holds at most one item, and sending a new one replaces an item nobody has taken yet. So
     * while one preview render runs, every intensity step a finger sends overwrites the one
     * before it, and when the render ends the collector takes only the LATEST settings: the
     * steps overtaken in between are dropped instead of being rendered one by one behind the
     * finger. And because a send is never refused, the last setting (a slider let go mid-render)
     * is never lost: it waits in the channel until the running render is done.
     */
    private val previewRequests = Channel<EditSettings>(Channel.CONFLATED)

    /**
     * The coroutine that takes requests out of [previewRequests] one at a time and renders them;
     * started on the first request and stopped with the ViewModel ([viewModelScope]).
     */
    private var previewCollector: Job? = null

    /** The original photo [previewSource] was made from, to know when it must be made again. */
    private var previewSourceOf: Bitmap? = null

    /**
     * The original photo shrunk to [FilterPreviewRenderer.PREVIEW_EDGE] pixels, made once per
     * decoded original, so a filter preview never works on the full-size photo.
     */
    private var previewSource: Bitmap? = null

    /**
     * What a preview request is made of: the filter and its strength (none and 0 without a
     * filter) and the stickers drawn into the small copy, never the one under a finger. Two
     * requests with an equal key would give the same picture, so the second is not rendered.
     */
    private data class PreviewKey(
        val filter: FilterPreset,
        val intensity: Float,
        val stickers: List<Sticker>,
    )

    /** The key last asked for, so an unchanged request is not rendered again. */
    private var lastPreviewKey: PreviewKey? = null

    /** The renderer in use, built on [previewSource] with the processor recipe it was given. */
    private var previewRenderer: FilterPreviewRenderer? = null

    /** The processor recipe [previewRenderer] was built with, to know when to build a new one. */
    private var previewRendererRecipe: ((EditSettings) -> ToolProcessor)? = null

    /**
     * Asks for the session's current filter to be drawn on the small preview copy.
     *
     * Without a filter there is nothing to compute: [ToolUiState.filterPreview] becomes null at
     * once and the screen draws the original. With one, the settings go into [previewRequests],
     * where a newer request replaces an older one still waiting. Nothing here marks the screen as
     * busy (no `Processing.Running`): the preview is quick, and the controls must not flicker or
     * lock while a finger drags a slider.
     */
    private fun requestPreview() {
        // The sticker under a finger is not part of the request: the screen draws it live, and
        // leaving it out is what keeps the request the same for the whole of a drag.
        val touched = mutableState.value.touchedStickerKey
        val full = session.settings
        val settings = full.copy(stickers = full.stickers.filter { it.key != touched })
        if (!settings.hasFilter && settings.stickers.isEmpty()) {
            lastPreviewKey = null
            mutableState.update { it.copy(filterPreview = null) }
            return
        }
        // A slider, turn or crop leaves the filter and the stickers as they were: the preview is
        // still right, so no render is repeated (it would only cost memory and a redraw during a
        // drag). The key is the filter, its strength and the untouched stickers.
        val key = PreviewKey(
            filter = if (settings.hasFilter) settings.filter else FilterPreset.NONE,
            intensity = if (settings.hasFilter) settings.filterIntensity else 0f,
            stickers = settings.stickers,
        )
        if (key == lastPreviewKey) return
        lastPreviewKey = key
        if (previewCollector == null) {
            previewCollector = viewModelScope.launch {
                // The loop waits for each request in turn and ends only when the scope is
                // cancelled (this ViewModel cleared).
                for (requested in previewRequests) renderPreview(requested)
            }
        }
        // A conflated channel always accepts, so trySend never fails here.
        previewRequests.trySend(settings)
    }

    /**
     * Renders the filter of [requested] on the small preview copy of the original, in the
     * background, and publishes it unless a newer filter choice has replaced it meanwhile (the
     * newer one is then already waiting in [previewRequests]), or the filter was removed (the
     * preview then stays null). A preview that fails shows the plain photo: the full render at
     * the end reports a real failure.
     */
    private suspend fun renderPreview(requested: EditSettings) {
        val original = (mutableState.value.photo as? ToolUiState.PhotoState.Ready)?.bitmap
        val build = processorFor
        if (original == null || build == null) {
            // Nothing to render yet: forget the request so a later one is not skipped as a repeat.
            lastPreviewKey = null
            return
        }
        val rendered = try {
            val renderer = previewRendererFor(original, build)
            // Off the main thread, like every edit here.
            withContext(workDispatcher) { renderer.render(requested) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failed: Throwable) {
            // Out of memory while shrinking, or anything else: no preview for this filter.
            null
        }
        val now = session.settings
        // Same filter still chosen (or still none, with stickers): show this frame even when the
        // intensity has moved on since, so a drag keeps changing the photo; the newer request is
        // already queued and replaces it. A failed render shows the plain photo rather than the
        // previous filter's look.
        val stillWanted = if (requested.hasFilter) {
            now.hasFilter && now.filter == requested.filter
        } else {
            val touched = mutableState.value.touchedStickerKey
            !now.hasFilter && now.stickers.any { it.key != touched }
        }
        if (stillWanted) {
            mutableState.update { it.copy(filterPreview = rendered) }
            if (rendered == null) lastPreviewKey = null
        }
    }

    /**
     * The renderer for [original] and the recipe [build]: the one already made when both are the
     * same objects as last time, otherwise a new one. Shrinking the original is real work, so it
     * runs on [workDispatcher] and happens once per decoded original.
     */
    private suspend fun previewRendererFor(
        original: Bitmap,
        build: (EditSettings) -> ToolProcessor,
    ): FilterPreviewRenderer {
        val current = previewRenderer
        if (current != null && previewSourceOf === original && previewRendererRecipe === build) {
            return current
        }
        val source = previewSource.takeIf { previewSourceOf === original }
            ?: withContext(workDispatcher) { WorkingSize.fit(original, FilterPreviewRenderer.PREVIEW_EDGE) }
        previewSourceOf = original
        previewSource = source
        previewRendererRecipe = build
        return FilterPreviewRenderer(source, build).also { previewRenderer = it }
    }

    // --- The Edit screen's full render -------------------------------------------------------------

    /**
     * Renders the Edit session's CURRENT settings from the decoded photo (the screen-sized decode,
     * shrunk further to at most [WorkingSize.EDIT] pixels on its longest edge) and returns the
     * result, or null when there is nothing to render yet (no decoded photo, or no processor
     * recipe because no setting has been changed yet on this screen; once one has, the recipe
     * stays set even if every change is later undone). It changes no state: the caller (the Done
     * button) decides what to show while it runs and what to do with the picture.
     *
     * This is the only render of the whole picture on the Edit screen, run once when the edit is
     * finished: while the person edits, the screen draws the settings live instead.
     */
    suspend fun renderFinal(): EditResult? {
        val original = (mutableState.value.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return null
        val build = processorFor ?: return null
        return renderFull(original, build, session.settings)
    }

    /**
     * Runs the processor [build] makes for [settings] on [original], shrunk once to this tool's
     * working size ([WorkingSize.maxEdgeFor]), on [workDispatcher], and returns the result; it
     * never writes the state. Always the original, never an earlier result, so nothing is stacked.
     * [onProgress] hears how far along it is, from 0 to 1.
     *
     * A failure comes back as [EditResult.Failed] (out of memory, or anything else). A
     * cancellation (the screen was left) is passed on up, as coroutines expect, not reported as a
     * failure.
     */
    private suspend fun renderFull(
        original: Bitmap,
        build: (EditSettings) -> ToolProcessor,
        settings: EditSettings,
        onProgress: (Float) -> Unit = {},
    ): EditResult = try {
        withContext(workDispatcher) {
            build(settings).process(fittedOriginal(original), onProgress)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (outOfMemory: OutOfMemoryError) {
        EditResult.Failed(EditFailure.OutOfMemory)
    } catch (other: Throwable) {
        EditResult.Failed(EditFailure.Other)
    }

    /**
     * Switches Compare [on] or off. While it is on, the screen shows the untouched original and
     * every editing control is disabled ([apply] and [undo] do nothing); switching it off shows
     * the edit again. The edit itself is never touched, so nothing is lost by comparing.
     */
    fun setComparing(on: Boolean) {
        val hadTouched = mutableState.value.touchedStickerKey != null
        mutableState.update { it.copy(comparing = on, touchedStickerKey = null) }
        // A gesture cancelled by Compare never commits, so the sticker rejoins the filtered copy.
        if (hadTouched) requestPreview()
    }

    /** Hides the message about the last failed edit, once the person has read it. */
    fun dismissFailure() {
        mutableState.update { it.copy(failure = null) }
    }

    // --- Done: hand the finished picture to the Result screen ----------------------------------

    /**
     * Tells the screen, once per finished Done, that the picture is in the [store] and the Result
     * screen can be opened.
     *
     * A Channel is a queue between coroutines: each item is received by exactly one reader, one
     * time. That makes it right for "the picture is ready, move on": unlike state, it is not
     * delivered again when the screen is drawn again after a rotation, or when the person comes
     * back from Result to this screen.
     */
    private val doneChannel = Channel<Unit>(Channel.BUFFERED)

    /** One item per finished Done; the screen calls its `onDone` for each. */
    val doneEvents: Flow<Unit> = doneChannel.receiveAsFlow()

    /**
     * The person tapped Done: the finished picture goes into the hand-off [store], then
     * [doneEvents] tells the screen to open Result. Nothing is written to the phone's storage
     * here; the Result screen saves or shares the picture if the person asks it to.
     *
     * Nothing happens before the photo is shown, while an edit runs (a half-made edit is never
     * handed over) or while an earlier Done is still rendering, so a second tap is ignored.
     *
     * - **The other four tools** hand over the picture shown now: the last edit, or, with no edit
     *   yet, the decoded original photo. That is instant, so it needs no waiting state.
     * - **The Edit screen** has no finished picture while the person edits (the screen draws the
     *   settings live), so Done renders the CURRENT settings from the decoded photo, shrunk to at
     *   most [WorkingSize.EDIT] pixels on its longest edge ([renderFinal]'s path), off the main
     *   thread. Meanwhile [ToolUiState.finishing] is true: the Done button shows a small spinner
     *   and ignores taps. A failed render keeps the person on this screen and stores why in
     *   [ToolUiState.failure], which stays until [dismissFailure] or the next Done; nothing is
     *   handed over. Only when no setting has been changed yet on this screen (no processor
     *   recipe) is there nothing to render, and the decoded original is handed over as it is; once
     *   a setting has been changed, Done renders even if every change was later undone.
     *
     * Done does not end the editing session: the settings, the undo steps and the edits stay here,
     * so Back from Result returns to this screen exactly as it was left.
     */
    fun done() {
        val current = mutableState.value
        if (current.finishing) return
        if (current.processing is ToolUiState.Processing.Running) return
        val original = (current.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        val build = processorFor
        if (tool != Tool.EDIT || build == null) {
            // Instant: the picture shown now (on Edit before any setting was ever changed, so with
            // no processor recipe, the decoded original).
            val edited = if (tool == Tool.EDIT) null else current.edited
            handOver(edited ?: original, originalUri = if (edited == null) photoUri else null)
            return
        }
        // Read once, now: the picture handed over is the settings as they were when Done was tapped.
        val settings = session.settings
        mutableState.update { it.copy(finishing = true, failure = null) }
        viewModelScope.launch {
            val result = try {
                renderFull(original, build, settings)
            } catch (cancelled: CancellationException) {
                // The screen was left: the render is over, so Done must not stay locked.
                mutableState.update { it.copy(finishing = false) }
                throw cancelled
            }
            when (result) {
                is EditResult.Edited -> {
                    mutableState.update { it.copy(finishing = false) }
                    handOver(result.bitmap)
                }
                // The same failure message an edit shows; the person stays here to try again.
                is EditResult.Failed -> mutableState.update {
                    it.copy(failure = result.failure, finishing = false)
                }
            }
        }
    }

    /**
     * Puts [picture] into the hand-off [store] (replacing any earlier one; [originalUri] is the photo's own Uri when [picture] is that photo untouched) and tells the screen,
     * through [doneEvents], to open Result. A buffered channel accepts at once, so trySend never
     * fails here.
     */
    private fun handOver(picture: Bitmap, originalUri: String? = null) {
        store.put(picture, originalUri)
        doneChannel.trySend(Unit)
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
