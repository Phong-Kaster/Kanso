package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditSession
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
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
 * Editing works on a copy held in memory, never on the photo's file, in one of two ways.
 *
 * - **The Edit screen** keeps one [EditSettings] object for the whole visit, inside an
 *   [EditSession] (the settings plus the earlier settings Undo can go back to). Every change goes
 *   through [changeSettings] (one action: a chip, a turn, a crop), [previewSettings] (a slider
 *   being dragged) and [commitSettings] (the slider let go), or [chooseCropRatio]; [undo] just
 *   goes back to earlier settings. None of them renders the full photo: the screen draws the
 *   crop, turns, flips and colour sliders live on top of the original as it draws, and only a
 *   filter is computed here, on a small copy of the original ([ToolUiState.filterPreview]). The
 *   full render from the ORIGINAL photo, with the WHOLE current settings, runs once, when the
 *   edit is finished ([save], [renderFinal]), so nothing is ever stacked on an earlier result.
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
            // A filter chosen before the photo arrived (or kept from a smaller decode) is applied
            // to the newly decoded original now; everything else the screen draws by itself.
            if (tool == Tool.EDIT && photo != null && session.settings.hasFilter) requestPreview()
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
     *
     * The Edit screen never uses this (Run 9): it would stack one change on the last result.
     * There, a call does nothing; its changes go through [changeSettings] and the calls beside it.
     */
    fun apply(processor: ToolProcessor, fromOriginal: Boolean = false) {
        if (tool == Tool.EDIT) return
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
     * Takes back the last edit. Nothing happens while the person is comparing with the original,
     * or when there is nothing to take back.
     *
     * On the Edit screen this goes back one action of the session (a slider release, a chip, a
     * crop, a turn), whichever sheet it came from: the screen draws the earlier settings at once,
     * and if they hold a filter its preview is computed again from the original (the newest
     * request always wins). On the other tools' screens the photo shown before the last edit is shown
     * again (undoing every edit returns to the original), and nothing happens while an edit runs.
     */
    fun undo() {
        if (mutableState.value.comparing) return
        if (tool == Tool.EDIT) {
            // A drag still under way counts as an action of its own, so it is settled first.
            if (!session.commit().canUndo) return
            publishSession(session.undo())
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
        mutableState.update { it.copy(openTool = if (it.openTool == tool) null else tool) }
    }

    /** Closes the open tool sheet, if any, leaving the photo and the row of tools. */
    fun closeTool() {
        mutableState.update { it.copy(openTool = null) }
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
        if (mutableState.value.comparing) return
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
        if (current.comparing) return
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
        if (mutableState.value.comparing) return
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
        publishSession(session.commit())
    }

    /**
     * The person chose the crop shape [ratio] (null or Free unlocks it), as one action: a fixed
     * ratio sets the largest centred rectangle of that shape inside the photo. The photo's shape
     * is read from the decoded original, before any turn, which is what [EditSession.cropRatioChosen]
     * expects. Nothing happens before the photo is decoded, or while comparing.
     */
    fun chooseCropRatio(ratio: CropRatio?, processorFor: (EditSettings) -> ToolProcessor) {
        val current = mutableState.value
        if (current.comparing) return
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

    /** Makes [new] the current session and shows its settings and whether Undo can be used. */
    private fun publishSession(new: EditSession) {
        session = new
        mutableState.update { it.copy(settings = new.settings, canUndo = new.canUndo) }
    }

    // --- The Edit screen's live filter preview -----------------------------------------------------

    /**
     * The filter settings waiting to be drawn on the small preview copy.
     *
     * A Channel is a queue between coroutines (see [savedChannel]). This one is CONFLATED: it
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

    /** The filter and intensity last asked for, so an unchanged filter is not rendered again. */
    private var lastPreviewKey: Pair<FilterPreset, Float>? = null

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
        val settings = session.settings
        if (!settings.hasFilter) {
            lastPreviewKey = null
            mutableState.update { it.copy(filterPreview = null) }
            return
        }
        // A slider, turn or crop leaves the filter as it was: its preview is still right, so no
        // render is repeated (it would only cost memory and a redraw during a drag).
        val key = settings.filter to settings.filterIntensity
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
        // Same filter still chosen: show this frame even when the intensity has moved on since,
        // so a drag keeps changing the photo; the newer request is already queued and replaces it.
        // A failed render shows the plain photo rather than the previous filter's look.
        if (now.hasFilter && now.filter == requested.filter) {
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
     * Renders the Edit session's CURRENT settings at full working size from the original photo and
     * returns the result, or null when there is nothing to render yet (no decoded photo, or no
     * processor recipe because nothing was changed). It changes no state: the caller (the Done
     * button) decides what to show while it runs and what to do with the picture.
     *
     * This is the only full-size render on the Edit screen, run once when the edit is finished:
     * while the person edits, the screen draws the settings live instead.
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
     *
     * The Edit screen has no rendered photo while the person edits (the screen draws the settings
     * live), so with changes made Save first renders the CURRENT settings in full from the
     * original, showing the work as running (taps are ignored meanwhile), then saves that picture;
     * with no changes it copies the original file. This keeps Save working until the Done button
     * takes over the final render.
     */
    fun save() {
        val current = mutableState.value
        if (current.save == ToolUiState.SaveState.Saving) return
        if (current.processing is ToolUiState.Processing.Running) return
        if (tool == Tool.EDIT && !session.settings.isNeutral) {
            saveRenderedSettings()
            return
        }
        // Read once, now, so an edit that lands while the save runs cannot change what is saved.
        val edited = current.edited
        mutableState.update { it.copy(save = ToolUiState.SaveState.Saving) }
        viewModelScope.launch { writeAndAnnounce(edited) }
    }

    /**
     * The Edit screen's Save with changes made: renders the session's current settings in full
     * from the original ([renderFull]), then writes the result. While the render runs the state
     * says so ([ToolUiState.Processing.Running]), which also makes [save] ignore further taps. A
     * failed render says why and saves nothing. Nothing happens before the photo is decoded.
     */
    private fun saveRenderedSettings() {
        val original = (mutableState.value.photo as? ToolUiState.PhotoState.Ready)?.bitmap ?: return
        val build = processorFor ?: return
        // Read once, now: the picture saved is the settings as they were when Save was tapped.
        val settings = session.settings
        mutableState.update {
            it.copy(processing = ToolUiState.Processing.Running(0f), failure = null)
        }
        viewModelScope.launch {
            val result = try {
                renderFull(original, build, settings) { progress -> reportProgress(progress) }
            } catch (cancelled: CancellationException) {
                // The screen was left: the run is over, so the screen must not stay locked.
                mutableState.update { it.copy(processing = ToolUiState.Processing.Idle) }
                throw cancelled
            }
            when (result) {
                is EditResult.Edited -> {
                    mutableState.update {
                        it.copy(processing = ToolUiState.Processing.Idle, save = ToolUiState.SaveState.Saving)
                    }
                    writeAndAnnounce(result.bitmap)
                }
                is EditResult.Failed -> mutableState.update {
                    it.copy(failure = result.failure, processing = ToolUiState.Processing.Idle)
                }
            }
        }
    }

    /**
     * Writes [edited] into `Pictures/Kanso` as a new file, or copies the original photo's file
     * when it is null, then records the outcome and, on success, sends the new photo's Uri to
     * the screen through [savedEvents]. The caller has already set the state to Saving.
     */
    private suspend fun writeAndAnnounce(edited: Bitmap?) {
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
