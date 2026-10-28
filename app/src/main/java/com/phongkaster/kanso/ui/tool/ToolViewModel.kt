package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.RenderedPhotoStore
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.edit.CatalogSticker
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.DateFormat
import com.phongkaster.kanso.data.edit.edit.EditSession
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.LayerMapping
import com.phongkaster.kanso.data.edit.edit.LayerSize
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.edit.edit.TextAlignment
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.data.edit.edit.TextLayerLayout
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.tool.model.AdjustSlider
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
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
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
 * [textLayout] measures a text layer's words, so a text keeps its letter size while the person
 * types or changes its font (the app passes the one with the bundled fonts; the default, the
 * phone's typeface for every font, is enough for a test).
 * [workDispatcher] is the background thread pool that shrinks a large photo and runs the edits,
 * so neither ever runs on the main thread; tests can pass their own.
 * [clock] tells the time a new date layer starts at (the phone's clock; a test passes a fixed one).
 */
class ToolViewModel(
    private val tool: Tool,
    private val photoUri: String,
    private val loader: PhotoLoader,
    private val probe: ViewModelClearedProbe,
    private val registry: ProcessorRegistry = ProcessorRegistry(),
    private val store: RenderedPhotoStore = RenderedPhotoStore(),
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val textLayout: TextLayerLayout = TextLayerLayout { Typeface.DEFAULT },
    private val clock: () -> LocalDateTime = { LocalDateTime.now() },
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
                (session.settings.hasFilter || session.settings.layers.isNotEmpty())
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
            // Words being typed are one action of their own (an empty new text is no action at
            // all), so they are settled first, like a drag still under way.
            settleText()
            if (!session.commit().canUndo) return
            publishSession(session.undo())
            // Undoing an "add sticker" removes that sticker: a selection pointing at it would
            // name a sticker that no longer exists, so it is cleared.
            val selected = mutableState.value.selectedLayerKey
            if (selected != null && session.settings.layers.none { it.key == selected }) {
                mutableState.update { it.copy(selectedLayerKey = null) }
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
     * again, so the row's entries work as toggles. Opening the Text sheet this way adds a new,
     * empty text in the middle of the photo and opens the keyboard for it (see [startText]);
     * opening the Date sheet sets the moment its next date gets to now ([ToolUiState.dateMoment]).
     */
    fun openTool(tool: EditTool) {
        val current = mutableState.value
        val closing = current.openTool == tool
        if (current.openTool.isLayerSheet()) {
            // Leaving a layer sheet by another door (its own tool tapped again, or another tool
            // chosen) keeps what was done, like its Confirm button; an empty text is dropped.
            settleText()
            sheetSnapshot = null
        }
        if (!closing && tool.isLayerSheet()) {
            // Opening a layer sheet: remember the whole session (settings AND undo steps) so
            // Cancel can put everything back exactly as it is now.
            publishSession(session.commit())
            sheetSnapshot = session
        }
        mutableState.update {
            it.copy(
                openTool = if (closing) null else tool,
                selectedLayerKey = if (current.openTool.isLayerSheet()) null else it.selectedLayerKey,
                touchedLayerKey = null,
                textEditing = false,
                dateMoment = if (!closing && tool == EditTool.DATE) now() else it.dateMoment,
            )
        }
        // A gesture cut short by the sheet changing never commits: let the layer rejoin the copy.
        if (current.touchedLayerKey != null) requestPreview()
        // The Text sheet opened from the row, with no text selected: a new text waits for words.
        if (!closing && tool == EditTool.TEXT) startText()
    }

    /** True for the three sheets that place layers on the photo: Stickers, Text and Date. */
    private fun EditTool?.isLayerSheet(): Boolean =
        this == EditTool.STICKERS || this == EditTool.TEXT || this == EditTool.DATE

    /** Shows the Adjust tool's [slider] (one of its four sub-tools) in place of the one shown. */
    fun chooseAdjustSlider(slider: AdjustSlider) {
        mutableState.update { it.copy(adjustSlider = slider) }
    }

    /**
     * Closes the open tool sheet, if any, leaving the photo and the row of tools. Closing a layer
     * sheet (Stickers, Text) this way keeps its changes, exactly like its Confirm button.
     */
    fun closeTool() {
        if (mutableState.value.openTool.isLayerSheet()) {
            confirmLayers()
            return
        }
        mutableState.update { it.copy(openTool = null) }
    }

    // --- The Stickers sheet --------------------------------------------------------------------------

    /**
     * The session as it was when a layer sheet (Stickers or Text) opened, or null while neither is
     * open. [cancelLayers] puts it back, so a Cancel throws away every layer added, moved, typed,
     * flipped or deleted since, along with the undo steps they made.
     */
    private var sheetSnapshot: EditSession? = null

    /** The key the next added sticker gets; it only grows, so a key is never used twice. */
    private var nextStickerKey = 1L

    /** How far a duplicated sticker sits from the first, as a fraction of the shown picture's width. */
    private val DUPLICATE_OFFSET = 0.04f

    /** The person tapped the category chip [category]: its layers fill the sheet's grid. */
    fun chooseStickerCategory(category: StickerCategory) {
        mutableState.update { it.copy(stickerCategory = category) }
    }

    /**
     * The person tapped [sticker] in the grid: it lands at the middle of the picture they SEE
     * (after the crop, turns and flips), as one action (one Undo step), and is selected.
     *
     * [processorFor] is the recipe that draws settings, with the sticker pictures in it (see
     * [editProcessorFor]); it also becomes the recipe Done renders with. At the limit nothing
     * changes (no settings, no undo step) and [ToolUiState.layerLimitShown] asks the screen for
     * its message. Nothing happens before the photo is decoded, or while comparing.
     */
    fun addSticker(sticker: CatalogSticker, processorFor: (EditSettings) -> ToolProcessor) {
        val current = mutableState.value
        if (current.comparing || current.finishing) return
        val original = (current.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        if (!session.settings.canAddLayer) {
            mutableState.update { it.copy(layerLimitShown = true) }
            return
        }
        // The mapping turns "the middle of what is shown" into fractions of the original photo,
        // which is what a Laye
        // r stores.
        val mapping = LayerMapping(original.width, original.height, session.settings)
        val key = nextStickerKey++
        this.processorFor = processorFor
        publishSession(session.addLayer(mapping.centredPicture(key, sticker.pictureId)))
        mutableState.update { it.copy(selectedLayerKey = key) }
        requestPreview()
    }

    /** True while a layer sheet (Stickers, Text) is open and nothing (Compare, Done) made it inert. */
    private fun layerEditingOpen(): Boolean {
        val current = mutableState.value
        return tool == Tool.EDIT && current.openTool.isLayerSheet() &&
            !current.comparing && !current.finishing
    }

    /**
     * True while the layers on the photo answer touches: on the Edit screen with any sheet open
     * but Crop (the cropper replaces the photo then), or none, and nothing (Compare, Done) inert.
     */
    private fun layersTouchable(): Boolean {
        val current = mutableState.value
        return tool == Tool.EDIT && current.openTool != EditTool.CROP &&
            !current.comparing && !current.finishing
    }

    /**
     * Closes the open sheet, keeping its changes like its Confirm, and opens the layer [sheet]
     * with a fresh snapshot for its Cancel. Used when a touched layer is of the other kind.
     */
    private fun switchToSheet(sheet: EditTool) {
        settleText()
        publishSession(session.commit())
        sheetSnapshot = session
        mutableState.update {
            it.copy(
                openTool = sheet,
                selectedLayerKey = null,
                textEditing = false,
                dateMoment = if (sheet == EditTool.DATE) now() else it.dateMoment,
            )
        }
    }

    /**
     * Selects the layer with [key], or none for null (a touch on empty photo), and brings it to
     * the front. Selecting alone changes nothing else; bringing a layer forward is a change of the
     * layer order, previewed here and made one undo step by the gesture's [commitSettings]. A key
     * that names no layer selects none.
     *
     * A layer can be touched with any sheet open but Crop (whose cropper hides them), or with none:
     * touching one opens the sheet of its kind (Stickers, Text or Date) if it is
     * not open yet, and the sheet that closes keeps its changes, like its Confirm (ADR-005).
     * Ignored while comparing or while Done renders.
     */
    fun selectLayer(key: Long?) {
        if (!layersTouchable()) return
        val previous = mutableState.value.selectedLayerKey
        // Leaving a text settles it: its words become one undo step, or an empty one is dropped.
        if (previous != null && previous != key) settleText()
        val chosen = session.settings.layers.firstOrNull { it.key == key }
        // A layer opens the sheet of its own kind; the sheet it leaves keeps its changes.
        if (chosen != null) {
            val sheet = when (chosen.content) {
                is LayerContent.Picture -> EditTool.STICKERS
                is LayerContent.Text -> EditTool.TEXT
                is LayerContent.Date -> EditTool.DATE
            }
            if (mutableState.value.openTool != sheet) switchToSheet(sheet)
        }
        mutableState.update { it.copy(selectedLayerKey = chosen?.key, textEditing = false) }
        val layers = session.settings.layers
        // A selected layer comes to the front (the end of the list is drawn last, on top). The
        // move is previewed, not committed: the gesture that selected it commits once its finger
        // lifts, so a tap and the drag that may follow are one undo step together.
        if (chosen != null && layers.last().key != chosen.key) {
            publishSession(session.preview(session.settings.copy(layers = layers - chosen + chosen)))
            requestPreview()
        }
    }

    /**
     * Copies the sticker [key] as one action (one Undo step): same picture, width, turn and
     * mirror, moved 4 percent of the SHOWN picture's width right and down, then selected. The
     * offset is worked out on the shown picture and carried back through [LayerMapping], so a
     * turned or flipped photo still moves the copy right and down as the person sees it. At the
     * limit nothing changes and the limit message is asked for, as in [addSticker].
     */
    fun duplicateLayer(key: Long, processorFor: (EditSettings) -> ToolProcessor) {
        if (!layerEditingOpen()) return
        val original = (mutableState.value.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        val source = session.settings.layers.firstOrNull { it.key == key } ?: return
        if (!session.settings.canAddLayer) {
            mutableState.update { it.copy(layerLimitShown = true) }
            return
        }
        val settings = session.settings
        val mapping = LayerMapping(original.width, original.height, settings)
        // The shown picture's size in pixels: the crop, with width and height swapped by a quarter turn.
        val cropW = (settings.crop.right - settings.crop.left).coerceAtLeast(0.01f) * original.width
        val cropH = (settings.crop.bottom - settings.crop.top).coerceAtLeast(0.01f) * original.height
        val turned = settings.quarterTurns % 2 != 0
        val shownW = if (turned) cropH else cropW
        val shownH = if (turned) cropW else cropH
        val shownCentre = mapping.originalToShown(LayerMapping.Point(source.centreX, source.centreY))
        val moved = mapping.shownToOriginal(
            LayerMapping.Point(
                shownCentre.x + DUPLICATE_OFFSET,
                // The same distance in pixels as sideways, as a fraction of the height.
                shownCentre.y + DUPLICATE_OFFSET * shownW / shownH,
            ),
        )
        val newKey = nextStickerKey++
        this.processorFor = processorFor
        publishSession(
            session.duplicateLayer(key, newKey, moved.x - source.centreX, moved.y - source.centreY),
        )
        mutableState.update { it.copy(selectedLayerKey = newKey) }
        requestPreview()
    }

    /** Takes the sticker [key] off the photo as one action; nothing is selected afterwards. */
    fun deleteLayer(key: Long, processorFor: (EditSettings) -> ToolProcessor) {
        if (!layerEditingOpen() || session.settings.layers.none { it.key == key }) return
        this.processorFor = processorFor
        publishSession(session.deleteLayer(key))
        mutableState.update { it.copy(selectedLayerKey = null) }
        requestPreview()
    }

    /** Mirrors the sticker [key] as one action; flipping twice returns the original settings. */
    fun flipLayer(key: Long, processorFor: (EditSettings) -> ToolProcessor) {
        if (!layerEditingOpen() || session.settings.layers.none { it.key == key }) return
        this.processorFor = processorFor
        publishSession(session.flipLayer(key))
        requestPreview()
    }

    /**
     * A move, pinch, turn or resize is under way and the layers now are [list]: the photo shows
     * it at once, but no undo entry is made until [commitSettings] (the last finger up).
     */
    fun previewLayers(list: List<Layer>, processorFor: (EditSettings) -> ToolProcessor) {
        if (!layersTouchable()) return
        // The first change of a gesture names the touched sticker: the one that differs from the
        // session's list. It is set BEFORE the preview is asked for, so that request already
        // leaves it out. Later changes of the same gesture keep it, so the request key of the
        // untouched layers never moves during a drag and nothing is re-rendered.
        if (mutableState.value.touchedLayerKey == null) {
            val before = session.settings.layers
            val changed = list.firstOrNull { it !in before }
            if (changed != null) mutableState.update { it.copy(touchedLayerKey = changed.key) }
        }
        previewSettings(session.settings.copy(layers = list), processorFor)
    }

    /** The limit message has been shown long enough: hides it again. */
    fun onLayerLimitMessageShown() {
        mutableState.update { it.copy(layerLimitShown = false) }
    }

    /**
     * The sheet's tick: keeps every change, closes the sheet and forgets the snapshot. Nothing
     * happens while Done renders or while comparing, when the sheet's controls are inert.
     */
    fun confirmLayers() {
        if (mutableState.value.comparing || mutableState.value.finishing) return
        // Words being typed become their undo step; an empty text is dropped.
        settleText()
        sheetSnapshot = null
        // Clear the touched key BEFORE asking for a preview, so the request includes that layer.
        mutableState.update {
            it.copy(openTool = null, selectedLayerKey = null, touchedLayerKey = null, textEditing = false)
        }
        requestPreview()
    }

    /**
     * The sheet's cross, and the system Back while the sheet is open: puts the session back to the
     * snapshot taken at opening (settings and undo steps, so Undo is exactly as it was), closes the
     * sheet and clears the selection. The photo is drawn from the settings, so it follows at once
     * (the Edit screen's `edited` stays null; only a filter preview is asked for again).
     * Nothing happens while Done renders (it is drawing the current layers) or while comparing.
     */
    fun cancelLayers() {
        if (mutableState.value.comparing || mutableState.value.finishing) return
        val snapshot = sheetSnapshot
        sheetSnapshot = null
        // Clear the touched key first: a gesture cancelled by this close never reaches its commit.
        mutableState.update {
            it.copy(openTool = null, selectedLayerKey = null, touchedLayerKey = null, textEditing = false)
        }
        if (snapshot != null) {
            publishSession(snapshot)
        }
        requestPreview()
    }

    // --- The Text sheet ----------------------------------------------------------------------------

    /**
     * Puts a new, empty text in the middle of the picture the person SEES (upright, at the
     * starting size, white, the phone's font, centred), selects it and opens the keyboard
     * ([ToolUiState.textEditing]). The text is only previewed, never committed: until it has
     * words it is not an action, so an empty text that is then dropped ([settleText]) leaves no
     * undo step, and the words typed into it later make ONE step together with its adding.
     *
     * At the layer limit nothing is added and the limit message is asked for. Nothing happens
     * before the photo is decoded, or while comparing or rendering.
     */
    private fun startText() {
        val current = mutableState.value
        if (current.comparing || current.finishing) return
        val original = (current.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        if (!session.settings.canAddLayer) {
            mutableState.update { it.copy(layerLimitShown = true) }
            return
        }
        val aspect = original.width.toFloat() / original.height
        val content = LayerContent.Text(
            text = "",
            colorArgb = TEXT_DEFAULT_COLOUR,
            font = TextFont.NONE,
            alignment = TextAlignment.CENTRE,
            wrapEms = LayerSize.textWrapEms(aspect),
        )
        val block = textLayout.measure(content, LayerContent.Text.PLACEHOLDER)
        val mapping = LayerMapping(original.width, original.height, session.settings)
        val layer = mapping.centredPicture(nextStickerKey++, "").copy(
            content = content,
            width = LayerSize.textWidth(LayerSize.TEXT_DEFAULT, block.widthEms, aspect),
        )
        publishSession(session.commit().preview(session.settings.copy(layers = session.settings.layers + layer)))
        mutableState.update { it.copy(selectedLayerKey = layer.key, textEditing = true) }
        requestPreview()
    }

    /**
     * The words of the selected text are now [words] (cut to [LayerContent.Text.MAX_LENGTH]
     * characters). The text keeps its letter size: its width follows its new longest line. While
     * the person types, each change is only previewed; the whole run of typing becomes one undo
     * step when it is settled (the keyboard closes, another layer is touched, the sheet closes).
     * With no text selected, typing starts a new text first. Ignored unless the Text sheet is open.
     */
    fun typeText(words: String, processorFor: (EditSettings) -> ToolProcessor) {
        if (!layerEditingOpen() || mutableState.value.openTool != EditTool.TEXT) return
        if (selectedText() == null) startText()
        val layer = selectedText() ?: return
        val content = layer.content as LayerContent.Text
        this.processorFor = processorFor
        val changed = reflowed(layer, content.copy(text = words.take(LayerContent.Text.MAX_LENGTH)))
        publishSession(session.preview(session.settings.withLayer(changed)))
        mutableState.update { it.copy(textEditing = true) }
        requestPreview()
    }

    /** The selected text gets the font [font], keeping its letter size, as one action. */
    fun chooseTextFont(font: TextFont, processorFor: (EditSettings) -> ToolProcessor) =
        editSelectedText(processorFor) { layer, content -> reflowed(layer, content.copy(font = font)) }

    /** The selected text's lines move to the next alignment (centre, left, right), as one action. */
    fun cycleTextAlignment(processorFor: (EditSettings) -> ToolProcessor) =
        editSelectedText(processorFor) { layer, content ->
            layer.copy(content = content.copy(alignment = content.alignment.next()))
        }

    /**
     * The selected text's letters take the colour [argb] (made opaque). [preview] is true while
     * the custom picker is being dragged: the photo follows without an undo step, and the drag
     * ends with [commitSettings]. A preset swatch is one action.
     */
    fun chooseTextColour(argb: Int, processorFor: (EditSettings) -> ToolProcessor, preview: Boolean = false) {
        val opaque = argb or OPAQUE_ALPHA
        if (!preview) {
            editSelectedText(processorFor) { layer, content -> layer.copy(content = content.copy(colorArgb = opaque)) }
            return
        }
        if (!layerEditingOpen()) return
        val layer = selectedText() ?: return
        val content = layer.content as LayerContent.Text
        this.processorFor = processorFor
        publishSession(session.preview(session.settings.withLayer(layer.copy(content = content.copy(colorArgb = opaque)))))
        requestPreview()
    }

    /**
     * The Text sheet's Background row: the selected text gets a box of colour [argb] behind its
     * words (its alpha kept, so a box may be see-through), or none for null, keeping its letter
     * size. A ready colour or None is one action; [preview] is true while the custom picker is
     * dragged: the photo follows without an undo step, and the drag ends with [commitSettings].
     */
    fun chooseTextBackground(argb: Int?, processorFor: (EditSettings) -> ToolProcessor, preview: Boolean = false) {
        if (!preview) {
            editSelectedText(processorFor) { layer, content -> reflowed(layer, content.copy(boxArgb = argb)) }
            return
        }
        if (!layerEditingOpen()) return
        val layer = selectedText() ?: return
        val content = layer.content as LayerContent.Text
        this.processorFor = processorFor
        publishSession(session.preview(session.settings.withLayer(reflowed(layer, content.copy(boxArgb = argb)))))
        requestPreview()
    }

    /**
     * The size slider of a layer sheet moved to [size] (1 to 100, see [LayerSize]) for the
     * selected layer: a sticker's width follows directly, a text's or a date's through its
     * measured block, so its letters take the height the number stands for. Previewed only; the slider's release
     * ([commitSettings]) makes the drag one undo step.
     */
    fun previewLayerSize(size: Float, processorFor: (EditSettings) -> ToolProcessor) {
        if (!layerEditingOpen()) return
        val original = (mutableState.value.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        val key = mutableState.value.selectedLayerKey ?: return
        val layer = session.settings.layers.firstOrNull { it.key == key } ?: return
        val width = when (val content = layer.content) {
            is LayerContent.Picture -> LayerSize.pictureWidth(size)
            is LayerContent.Text -> LayerSize.textWidth(
                size,
                textLayout.measure(content, wordsOrPlaceholder(content)).widthEms,
                original.width.toFloat() / original.height,
            )
            // A date's letters are sized like a text's (ADR-006).
            is LayerContent.Date -> LayerSize.textWidth(
                size,
                textLayout.measure(content.asText()).widthEms,
                original.width.toFloat() / original.height,
            )
        }
        this.processorFor = processorFor
        publishSession(session.preview(session.settings.withLayer(layer.copy(width = width))))
        requestPreview()
    }

    /**
     * The opacity slider of a layer sheet moved to [opacity] (0.1 to 1) for the selected layer.
     * Previewed only; the slider's release ([commitSettings]) makes the drag one undo step.
     */
    fun previewLayerOpacity(opacity: Float, processorFor: (EditSettings) -> ToolProcessor) {
        if (!layerEditingOpen()) return
        val key = mutableState.value.selectedLayerKey ?: return
        val layer = session.settings.layers.firstOrNull { it.key == key } ?: return
        this.processorFor = processorFor
        val kept = opacity.coerceIn(Layer.MIN_OPACITY, 1f)
        publishSession(session.preview(session.settings.withLayer(layer.copy(opacity = kept))))
        requestPreview()
    }

    /**
     * The size slider's number for the selected layer (see [LayerSize]), or null with none
     * selected. The sheets show it; a text's number needs its words measured, so it is worked out
     * here, with the same measure the slider's changes use.
     */
    fun selectedLayerSize(): Float? {
        val original = (mutableState.value.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return null
        val key = mutableState.value.selectedLayerKey ?: return null
        val layer = session.settings.layers.firstOrNull { it.key == key } ?: return null
        return when (val content = layer.content) {
            is LayerContent.Picture -> LayerSize.ofPicture(layer.width)
            is LayerContent.Text -> LayerSize.ofText(
                layer.width,
                textLayout.measure(content, wordsOrPlaceholder(content)).widthEms,
                original.width.toFloat() / original.height,
            )
            is LayerContent.Date -> LayerSize.ofText(
                layer.width,
                textLayout.measure(content.asText()).widthEms,
                original.width.toFloat() / original.height,
            )
        }
    }

    /**
     * The keyboard of the Text sheet opened ([on] true: the text area got the focus, or the
     * frame's edit button asks for it) or closed. Closing it settles the words typed (one undo
     * step, or nothing for an empty text, which is dropped).
     */
    fun setTextEditing(on: Boolean) {
        if (on) {
            if (!layerEditingOpen() || mutableState.value.openTool != EditTool.TEXT) return
            mutableState.update { it.copy(textEditing = true) }
            return
        }
        if (!mutableState.value.textEditing) return
        mutableState.update { it.copy(textEditing = false) }
        settleText()
    }

    /**
     * The edit button on the frame of the text [key]: selects it and opens the keyboard on its
     * words. Ignored for a layer that is not a text.
     */
    fun editText(key: Long) {
        if (session.settings.layers.firstOrNull { it.key == key }?.content !is LayerContent.Text) return
        selectLayer(key)
        setTextEditing(true)
    }

    /**
     * Ends a run of typing: an EMPTY selected text (no letters) is taken off the photo, then
     * whatever was previewed becomes one undo step (nothing at all for an empty text that was
     * only ever previewed). Safe to call at any time; with nothing to settle it changes nothing.
     */
    private fun settleText() {
        val blank = selectedText()?.takeIf { (it.content as LayerContent.Text).isBlank }
        if (blank != null) {
            publishSession(session.preview(session.settings.copy(layers = session.settings.layers - blank)))
            mutableState.update { it.copy(selectedLayerKey = null) }
        }
        val hadPending = session.pending != null
        publishSession(session.commit())
        if (blank != null || hadPending) requestPreview()
    }

    /** The selected layer when it is a text, or null. */
    private fun selectedText(): Layer? {
        val key = mutableState.value.selectedLayerKey ?: return null
        return session.settings.layers.firstOrNull { it.key == key && it.content is LayerContent.Text }
    }

    /**
     * One action on the selected text (a font, an alignment, a preset colour): [edit] makes the
     * changed layer. A text with no words yet is only previewed, so its adding and this change
     * stay one undo step (or none, if it is dropped empty); otherwise words still being typed
     * become their own step first and this change is one more.
     */
    private fun editSelectedText(
        processorFor: (EditSettings) -> ToolProcessor,
        edit: (Layer, LayerContent.Text) -> Layer,
    ) {
        if (!layerEditingOpen()) return
        val layer = selectedText() ?: return
        val content = layer.content as LayerContent.Text
        this.processorFor = processorFor
        val changed = session.settings.withLayer(edit(layer, content))
        publishSession(if (content.isBlank) session.preview(changed) else session.change(changed))
        requestPreview()
    }

    /**
     * [layer] with the text [content], its width changed so the letters keep their size: the
     * width scales with the block's width, measured (with the placeholder for an empty text)
     * before and after.
     */
    private fun reflowed(layer: Layer, content: LayerContent.Text): Layer {
        val before = layer.content as LayerContent.Text
        val old = textLayout.measure(before, wordsOrPlaceholder(before))
        val new = textLayout.measure(content, wordsOrPlaceholder(content))
        val width = if (old.widthEms > 0f) layer.width * new.widthEms / old.widthEms else layer.width
        return layer.copy(content = content, width = width)
    }

    /** A text's words, or the placeholder that gives an empty text its frame. */
    private fun wordsOrPlaceholder(content: LayerContent.Text): String =
        content.text.ifBlank { LayerContent.Text.PLACEHOLDER }

    /** These settings with [layer] in place of the layer that has the same key. */
    private fun EditSettings.withLayer(layer: Layer): EditSettings =
        copy(layers = layers.map { if (it.key == layer.key) layer else it })

    // --- The Date sheet ----------------------------------------------------------------------------

    /** Now, to the minute: the moment a new date gets until the person picks another. */
    private fun now(): LocalDateTime = clock().truncatedTo(ChronoUnit.MINUTES)

    /**
     * The person tapped the Date sheet's chip for [format]. With a date selected, that date is
     * written in this format instead, keeping its letter size, as one action. With none selected,
     * a new date is added in this format (see [addDate]). Ignored unless the Date sheet is open.
     */
    fun chooseDateFormat(format: DateFormat, processorFor: (EditSettings) -> ToolProcessor) {
        if (!layerEditingOpen() || mutableState.value.openTool != EditTool.DATE) return
        if (selectedDate() != null) {
            editSelectedDate(processorFor, edit = { it.copy(format = format) })
        } else {
            addDate(format, processorFor)
        }
    }

    /**
     * Puts a new date, written in [format], in the middle of the picture the person SEES (upright,
     * at the starting size, white, the phone's font), as one action (one Undo step), and selects
     * it. Its moment is the sheet's [ToolUiState.dateMoment] (now, unless the pickers changed it).
     * At the layer limit nothing is added and the limit message is asked for, as in [addSticker].
     * Nothing happens before the photo is decoded.
     */
    private fun addDate(format: DateFormat, processorFor: (EditSettings) -> ToolProcessor) {
        val current = mutableState.value
        val original = (current.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        if (!session.settings.canAddLayer) {
            mutableState.update { it.copy(layerLimitShown = true) }
            return
        }
        val aspect = original.width.toFloat() / original.height
        val content = LayerContent.Date(
            moment = current.dateMoment ?: now(),
            format = format,
            colorArgb = TEXT_DEFAULT_COLOUR,
            font = TextFont.NONE,
        )
        val block = textLayout.measure(content.asText())
        val mapping = LayerMapping(original.width, original.height, session.settings)
        val layer = mapping.centredPicture(nextStickerKey++, "").copy(
            content = content,
            width = LayerSize.textWidth(LayerSize.TEXT_DEFAULT, block.widthEms, aspect),
        )
        this.processorFor = processorFor
        publishSession(session.addLayer(layer))
        mutableState.update { it.copy(selectedLayerKey = layer.key) }
        requestPreview()
    }

    /**
     * The date or time picker gave [moment] (cut to the minute). With a date selected, that date
     * shows it, as one action; with none, it becomes the moment the next added date gets, and the
     * chips show it. Ignored unless the Date sheet is open.
     */
    fun chooseDateMoment(moment: LocalDateTime, processorFor: (EditSettings) -> ToolProcessor) {
        if (!layerEditingOpen() || mutableState.value.openTool != EditTool.DATE) return
        val minute = moment.truncatedTo(ChronoUnit.MINUTES)
        if (selectedDate() != null) {
            editSelectedDate(processorFor, edit = { it.copy(moment = minute) })
        } else {
            mutableState.update { it.copy(dateMoment = minute) }
        }
    }

    /** The selected date gets the font [font], keeping its letter size, as one action. */
    fun chooseDateFont(font: TextFont, processorFor: (EditSettings) -> ToolProcessor) =
        editSelectedDate(processorFor, edit = { it.copy(font = font) })

    /**
     * The selected date's letters take the colour [argb] (made opaque). [preview] is true while
     * the custom picker is being dragged: the photo follows without an undo step, and the drag
     * ends with [commitSettings]. A preset swatch is one action.
     */
    fun chooseDateColour(argb: Int, processorFor: (EditSettings) -> ToolProcessor, preview: Boolean = false) =
        editSelectedDate(processorFor, preview = preview, edit = { it.copy(colorArgb = argb or OPAQUE_ALPHA) })

    /**
     * The Date sheet's Background row: the selected date gets a box of colour [argb] behind its
     * words (its alpha kept, so a box may be see-through), or none for null. A ready colour or
     * None is one action; [preview] is true while the custom picker is dragged, ended by
     * [commitSettings]. Its letters keep their size; the layer grows or shrinks by the box's margin.
     */
    fun chooseDateBackground(argb: Int?, processorFor: (EditSettings) -> ToolProcessor, preview: Boolean = false) =
        editSelectedDate(processorFor, preview = preview, edit = { it.copy(boxArgb = argb) })

    /**
     * The person tapped the layer [key] while it was ALREADY selected (a tap that selects it is
     * [selectLayer]'s). A date then moves to the next format, the first after the last, as one
     * action, the way Instagram's date sticker does (ADR-006). A tap on any other kind of layer
     * changes nothing.
     */
    fun tapLayer(key: Long, processorFor: (EditSettings) -> ToolProcessor) {
        val date = selectedDate() ?: return
        if (date.key != key) return
        editSelectedDate(processorFor, edit = { it.copy(format = it.format.next()) })
    }

    /** The selected layer when it is a date, or null. */
    private fun selectedDate(): Layer? {
        val key = mutableState.value.selectedLayerKey ?: return null
        return session.settings.layers.firstOrNull { it.key == key && it.content is LayerContent.Date }
    }

    /**
     * One change of the selected date: [edit] makes its new content. The layer's width follows
     * the new words, so the letters keep their size (a longer format makes a wider layer). It is
     * one action, or only previewed when [preview] is true (the colour picker being dragged).
     */
    private fun editSelectedDate(
        processorFor: (EditSettings) -> ToolProcessor,
        preview: Boolean = false,
        edit: (LayerContent.Date) -> LayerContent.Date,
    ) {
        if (!layerEditingOpen()) return
        val layer = selectedDate() ?: return
        val before = layer.content as LayerContent.Date
        val after = edit(before)
        val old = textLayout.measure(before.asText())
        val new = textLayout.measure(after.asText())
        val width = if (old.widthEms > 0f) layer.width * new.widthEms / old.widthEms else layer.width
        val changed = session.settings.withLayer(layer.copy(content = after, width = width))
        this.processorFor = processorFor
        publishSession(if (preview) session.preview(changed) else session.change(changed))
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
        mutableState.update { it.copy(touchedLayerKey = null) }
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
        if (current.openTool.isLayerSheet()) {
            // Back from a layer sheet is its Cancel: the sheet's changes are thrown away.
            // While Done renders or while comparing the sheet is inert, so Back is consumed
            // (the screen stays) but cancels nothing.
            if (!current.comparing && !current.finishing) cancelLayers()
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
     * the layers (the add was undone, the sticker deleted) becomes none.
     */
    private fun publishSession(new: EditSession) {
        session = new
        mutableState.update { state ->
            val selected = state.selectedLayerKey
            val stillThere = selected != null && new.settings.layers.any { it.key == selected }
            val touched = state.touchedLayerKey
            val touchedStillThere = touched != null && new.settings.layers.any { it.key == touched }
            state.copy(
                settings = new.settings,
                canUndo = new.canUndo,
                selectedLayerKey = if (stillThere) selected else null,
                touchedLayerKey = if (touchedStillThere) touched else null,
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
     * filter) and the layers drawn into the small copy, never the one under a finger. Two
     * requests with an equal key would give the same picture, so the second is not rendered.
     */
    private data class PreviewKey(
        val filter: FilterPreset,
        val intensity: Float,
        val layers: List<Layer>,
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
        val touched = mutableState.value.touchedLayerKey
        val full = session.settings
        val settings = full.copy(layers = full.layers.filter { it.key != touched })
        if (!settings.hasFilter && settings.layers.isEmpty()) {
            lastPreviewKey = null
            mutableState.update { it.copy(filterPreview = null) }
            return
        }
        // A slider, turn or crop leaves the filter and the layers as they were: the preview is
        // still right, so no render is repeated (it would only cost memory and a redraw during a
        // drag). The key is the filter, its strength and the untouched layers.
        val key = PreviewKey(
            filter = if (settings.hasFilter) settings.filter else FilterPreset.NONE,
            intensity = if (settings.hasFilter) settings.filterIntensity else 0f,
            layers = settings.layers,
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
        // Same filter still chosen (or still none, with layers): show this frame even when the
        // intensity has moved on since, so a drag keeps changing the photo; the newer request is
        // already queued and replaces it. A failed render shows the plain photo rather than the
        // previous filter's look.
        val stillWanted = if (requested.hasFilter) {
            now.hasFilter && now.filter == requested.filter
        } else {
            val touched = mutableState.value.touchedLayerKey
            !now.hasFilter && now.layers.any { it.key != touched }
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
        val hadTouched = mutableState.value.touchedLayerKey != null
        mutableState.update { it.copy(comparing = on, touchedLayerKey = null) }
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

    /** The colours a new text or date starts with. */
    private companion object {
        /** A new text's or date's letters: white, which reads on most photos. */
        const val TEXT_DEFAULT_COLOUR = 0xFFFFFFFF.toInt()

        /** The alpha bits of an ARGB number, set to make a colour opaque. */
        const val OPAQUE_ALPHA = 0xFF000000.toInt()
    }

    /** Reports to the probe that this ViewModel has been cleared. */
    override fun onCleared() {
        probe.recordCleared(this)
        super.onCleared()
    }
}
