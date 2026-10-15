package com.phongkaster.kanso.tool

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.save.FakePhotoSaver
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks the tool screen's ViewModel on its own, with no screen drawn (Run 7, DoD 10): each tool
 * has a screen of its own, so the state's tool is the navigation key's tool from the first
 * snapshot to the last, through the photo's decode and a whole save. Loading and saving each
 * change only their own part of the snapshot: the decoded photo stays the very same picture
 * through a save, and it is never asked for again.
 *
 * Run 8 adds editing, driven through fake [ToolProcessor]s: an edit's result is shown and can be
 * undone back to the original, progress shows while one runs (and blocks Save), a failure keeps
 * the photo as it was, a cancellation is no failure, the photo is shrunk to the tool's working
 * size first, and Save writes the edited bitmap itself, never the original's file.
 *
 * Run 9 adds the Edit screen's sheets and Compare: `openTool` opens, closes and switches the one
 * open tool sheet, and while Compare is on no edit is applied and none is undone.
 *
 * Run 9 (T-002) makes the Edit screen a real editor: its settings are one object rendered from the
 * original every time, so the stacking `apply` path and its picture history now belong to the
 * other tools only. The Run 8 editing tests therefore run on Retouch (whose working size, 2000,
 * keeps the 600 x 400 test photo as the same object), and new tests cover the Edit session: the
 * newest settings win over a render still running, Back closes a sheet or asks to discard, and
 * Save writes the rendered settings or, with none, the original. Undo across the Edit sheets is
 * in `edit/EditUndoTest`.
 *
 * It runs under Robolectric because the [FakePhotoLoader] it is given needs an Android Context and
 * a Bitmap. Robolectric runs the test on Android's main thread, the thread the ViewModel's
 * coroutines run on, so a load or a save whose fake answers at once has finished when the call
 * returns; the main thread's queue is still run afterwards, so nothing waiting there is missed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ToolViewModelTest {

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * DoD 10: for each of the five tools, a ViewModel built for that tool starts with it and the
     * key's photo, nothing decoded and nothing saved, and keeps that tool in every snapshot it
     * hands out: before the decode, after it, while a save runs and after the save is done.
     */
    @Test
    fun the_state_keeps_the_key_s_tool_for_the_view_model_s_whole_life() {
        Tool.entries.forEach { tool ->
            // The gate holds the save open until the test completes it.
            val gate = CompletableDeferred<Unit>()
            val saver = FakePhotoSaver(gate = gate)
            val viewModel = toolViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)), saver, tool)
            assertEquals(ToolUiState(tool = tool, photoUri = PHOTO_URI), viewModel.state.value)

            viewModel.load(MAX_EDGE_PX)
            runMainThread()
            assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
            assertEquals(tool, viewModel.state.value.tool)

            viewModel.save()
            runMainThread()
            assertEquals(ToolUiState.SaveState.Saving, viewModel.state.value.save)
            assertEquals(tool, viewModel.state.value.tool)

            gate.complete(Unit)
            runMainThread()
            assertEquals(ToolUiState.SaveState.Saved(keptOriginalOrientation = false), viewModel.state.value.save)
            assertEquals(tool, viewModel.state.value.tool)
            assertEquals(PHOTO_URI, viewModel.state.value.photoUri)
            assertEquals(listOf(PHOTO_URI), saver.calls.toList())
        }
    }

    /**
     * A save, from start to end, leaves the decoded photo as the very same picture, and the
     * loader is asked for it only once.
     */
    @Test
    fun a_save_keeps_the_decoded_photo_and_does_not_load_again() {
        val bitmap = ToolFixtures.gradient(600, 400)
        val loader = FakePhotoLoader(app, bitmap)
        val viewModel = toolViewModel(loader, FakePhotoSaver())
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        val loaded = viewModel.state.value.photo
        assertTrue("The photo is not ready: $loaded", loaded is ToolUiState.PhotoState.Ready)

        viewModel.save()
        runMainThread()

        val state = viewModel.state.value
        assertEquals(ToolUiState.SaveState.Saved(keptOriginalOrientation = false), state.save)
        assertSame(loaded, state.photo)
        assertSame(bitmap, (state.photo as ToolUiState.PhotoState.Ready).bitmap)
        assertEquals(listOf(MAX_EDGE_PX), loader.requestedEdges.toList())
    }

    /** A failed save keeps the decoded photo and the tool, so the failure is shown over the photo. */
    @Test
    fun a_failed_save_keeps_the_photo_and_the_tool() {
        val viewModel = toolViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)), tool = Tool.ENHANCE)
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        // NoSavePhotoSaver fails every save.
        viewModel.save()
        runMainThread()

        assertEquals(ToolUiState.SaveState.Failed, viewModel.state.value.save)
        assertEquals(Tool.ENHANCE, viewModel.state.value.tool)
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
    }

    /**
     * Run 8: an edit that works becomes the shown photo (the original stays in `photo`), Undo
     * becomes possible, and the next edit works on the last edit rather than on the original.
     */
    @Test
    fun an_edit_sets_the_edited_photo_and_allows_undo() {
        val original = ToolFixtures.gradient(600, 400)
        val viewModel = loadedViewModel(original)
        val first = ToolFixtures.gradient(300, 200)
        val second = ToolFixtures.twoHalves(300, 200)
        val firstEdit = FakeProcessor { _, _ -> EditResult.Edited(first) }
        val secondEdit = FakeProcessor { _, _ -> EditResult.Edited(second) }

        viewModel.apply(firstEdit)
        runMainThread()
        var state = viewModel.state.value
        assertSame(first, state.edited)
        assertTrue(state.canUndo)
        assertEquals(ToolUiState.Processing.Idle, state.processing)
        assertNull(state.failure)
        assertSame(original, (state.photo as ToolUiState.PhotoState.Ready).bitmap)
        assertSame("The first edit did not work on the original", original, firstEdit.inputs.single())

        viewModel.apply(secondEdit)
        runMainThread()
        state = viewModel.state.value
        assertSame(second, state.edited)
        assertTrue(state.canUndo)
        assertSame("The second edit did not work on the first edit", first, secondEdit.inputs.single())
    }

    /**
     * Run 8: two edits, then Undo twice, returns step by step to the unedited original: the first
     * edit after one Undo, nothing edited and nothing left to undo after the second; a third Undo
     * changes nothing.
     */
    @Test
    fun undoing_every_edit_returns_to_the_original() {
        val original = ToolFixtures.gradient(600, 400)
        val viewModel = loadedViewModel(original)
        val first = ToolFixtures.gradient(300, 200)
        val second = ToolFixtures.twoHalves(300, 200)
        viewModel.apply(FakeProcessor { _, _ -> EditResult.Edited(first) })
        runMainThread()
        viewModel.apply(FakeProcessor { _, _ -> EditResult.Edited(second) })
        runMainThread()

        viewModel.undo()
        assertSame(first, viewModel.state.value.edited)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.undo()
        assertNull(viewModel.state.value.edited)
        assertFalse(viewModel.state.value.canUndo)
        assertSame(original, (viewModel.state.value.photo as ToolUiState.PhotoState.Ready).bitmap)

        viewModel.undo()
        assertNull(viewModel.state.value.edited)
        assertFalse(viewModel.state.value.canUndo)
    }

    /**
     * Run 8: the progress a running edit reports shows in the state as `Running(progress)`;
     * meanwhile Save, Undo and a second edit are all ignored (the saver is never called), and when
     * the edit ends the state is idle again with the result shown.
     */
    @Test
    fun a_running_edit_reports_its_progress_and_blocks_save() {
        val saver = FakePhotoSaver()
        val viewModel = loadedViewModel(ToolFixtures.gradient(600, 400), saver)
        val halfway = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val result = ToolFixtures.gradient(300, 200)
        val gated = FakeProcessor { _, onProgress ->
            onProgress(0.25f)
            halfway.await()
            onProgress(0.75f)
            finish.await()
            EditResult.Edited(result)
        }
        val other = FakeProcessor { input, _ -> EditResult.Edited(input) }

        viewModel.apply(gated)
        runMainThread()
        assertEquals(ToolUiState.Processing.Running(0.25f), viewModel.state.value.processing)

        viewModel.save()
        viewModel.undo()
        viewModel.apply(other)
        runMainThread()
        assertEquals(ToolUiState.SaveState.Idle, viewModel.state.value.save)
        assertTrue("Save ran during an edit", saver.calls.isEmpty() && saver.bitmapCalls.isEmpty())
        assertTrue("A second edit ran during the first", other.inputs.isEmpty())

        halfway.complete(Unit)
        runMainThread()
        assertEquals(ToolUiState.Processing.Running(0.75f), viewModel.state.value.processing)

        finish.complete(Unit)
        runMainThread()
        assertEquals(ToolUiState.Processing.Idle, viewModel.state.value.processing)
        assertSame(result, viewModel.state.value.edited)
    }

    /**
     * Run 8: an edit that fails, whether it returns a failure (no face), throws
     * `OutOfMemoryError` or throws anything else, sets the matching failure and leaves the photo
     * and the last edit exactly as they were, with Undo still possible.
     */
    @Test
    fun a_failed_edit_sets_the_failure_and_keeps_the_photo_and_the_edit() {
        val original = ToolFixtures.gradient(600, 400)
        val viewModel = loadedViewModel(original)
        val kept = ToolFixtures.gradient(300, 200)
        viewModel.apply(FakeProcessor { _, _ -> EditResult.Edited(kept) })
        runMainThread()

        val cases = listOf(
            EditFailure.NoFace to FakeProcessor { _, _ -> EditResult.Failed(EditFailure.NoFace) },
            EditFailure.OutOfMemory to FakeProcessor { _, _ -> throw OutOfMemoryError("test") },
            EditFailure.Other to FakeProcessor { _, _ -> throw IllegalStateException("test") },
        )
        cases.forEach { (failure, processor) ->
            viewModel.apply(processor)
            runMainThread()
            val state = viewModel.state.value
            assertEquals(failure, state.failure)
            assertSame("$failure replaced the shown edit", kept, state.edited)
            assertSame(original, (state.photo as ToolUiState.PhotoState.Ready).bitmap)
            assertTrue(state.canUndo)
            assertEquals(ToolUiState.Processing.Idle, state.processing)
        }

        viewModel.dismissFailure()
        assertNull(viewModel.state.value.failure)
    }

    /**
     * Run 8: a cancelled edit is not a failure. Leaving the screen (its ViewModelStore cleared)
     * while an edit waits cancels it, and no failure is recorded; a processor that throws
     * `CancellationException` itself records none either, and the photo stays as it was.
     */
    @Test
    fun a_cancelled_edit_is_not_a_failure() {
        val original = ToolFixtures.gradient(600, 400)
        val never = CompletableDeferred<Unit>()
        val stored = storedViewModel(loadedViewModel(original))
        val viewModelStore = stored.first
        val viewModel = stored.second
        viewModel.apply(FakeProcessor { _, _ -> never.await(); EditResult.Edited(original) })
        runMainThread()
        viewModelStore.clear()
        runMainThread()
        assertNull(viewModel.state.value.failure)
        assertNull(viewModel.state.value.edited)

        val other = loadedViewModel(original)
        other.apply(FakeProcessor { _, _ -> throw CancellationException("left the screen") })
        runMainThread()
        assertNull(other.state.value.failure)
        assertNull(other.state.value.edited)
        assertSame(original, (other.state.value.photo as ToolUiState.PhotoState.Ready).bitmap)
    }

    /**
     * Run 8: before an edit the photo is shrunk to the tool's working size, keeping its shape:
     * a 4000 x 3000 photo reaches Enhance at 512 x 384 and Remove background at 2000 x 1500,
     * while the decoded original itself is left at its own size.
     */
    @Test
    fun an_edit_works_on_the_photo_fitted_to_the_tool_s_working_size() {
        val expected = mapOf(
            Tool.ENHANCE to (WorkingSize.maxEdgeFor(Tool.ENHANCE) to 384),
            Tool.REMOVE_BACKGROUND to (WorkingSize.maxEdgeFor(Tool.REMOVE_BACKGROUND) to 1500),
        )
        assertEquals(512, WorkingSize.maxEdgeFor(Tool.ENHANCE))
        assertEquals(2000, WorkingSize.maxEdgeFor(Tool.REMOVE_BACKGROUND))
        expected.forEach { (tool, size) ->
            val original = Bitmap.createBitmap(4000, 3000, Bitmap.Config.RGB_565)
            val viewModel = loadedViewModel(original, tool = tool)
            val processor = FakeProcessor { input, _ -> EditResult.Edited(input) }
            viewModel.apply(processor)
            runMainThread()

            val input = processor.inputs.single()
            assertEquals("$tool: width", size.first, input.width)
            assertEquals("$tool: height", size.second, input.height)
            assertEquals(4000, original.width)
            assertEquals(3000, original.height)
        }
    }

    /**
     * Run 8: after an edit, Save hands the saver the edited bitmap itself, at its own size, and
     * never the photo's Uri, so the original file is never written; an unedited photo is still
     * saved from its Uri.
     */
    @Test
    fun save_after_an_edit_saves_the_edited_bitmap_and_not_the_original() {
        val original = ToolFixtures.gradient(600, 400)
        val edited = ToolFixtures.gradient(640, 480)

        val uneditedSaver = FakePhotoSaver()
        val unedited = loadedViewModel(original, uneditedSaver)
        unedited.save()
        runMainThread()
        assertEquals(listOf(PHOTO_URI), uneditedSaver.calls.toList())
        assertTrue(uneditedSaver.bitmapCalls.isEmpty())

        val saver = FakePhotoSaver()
        val viewModel = loadedViewModel(original, saver)
        viewModel.apply(FakeProcessor { _, _ -> EditResult.Edited(edited) })
        runMainThread()
        viewModel.save()
        runMainThread()

        val saved = saver.bitmapCalls.single()
        assertSame(edited, saved)
        assertEquals(640, saved.width)
        assertEquals(480, saved.height)
        assertNotSame(original, saved)
        assertTrue("The original's Uri was saved after an edit", saver.calls.isEmpty())
        assertEquals(ToolUiState.SaveState.Saved(keptOriginalOrientation = false), viewModel.state.value.save)
    }

    /**
     * Run 8: `runRegistered` runs the processor the registry holds for the screen's tool, and does
     * nothing on a screen whose tool has none.
     */
    @Test
    fun run_registered_runs_the_tool_s_own_processor() {
        val result = ToolFixtures.gradient(300, 200)
        val registry = ProcessorRegistry(mapOf(Tool.ENHANCE to FakeProcessor { _, _ -> EditResult.Edited(result) }))

        val enhance = loadedViewModel(ToolFixtures.gradient(600, 400), tool = Tool.ENHANCE, registry = registry)
        enhance.runRegistered()
        runMainThread()
        assertSame(result, enhance.state.value.edited)

        val edit = loadedViewModel(ToolFixtures.gradient(600, 400), tool = Tool.EDIT, registry = registry)
        edit.runRegistered()
        runMainThread()
        assertNull(edit.state.value.edited)
        assertEquals(ToolUiState.Processing.Idle, edit.state.value.processing)
    }

    /**
     * Run 9: the Edit screen's row of tools works as toggles through `openTool`: no sheet is open
     * at first; opening a tool opens its sheet; opening the same tool again closes it; opening
     * another while one is open switches to it; and `closeTool` closes whatever is open. Opening
     * and closing sheets never touches the photo or the edit.
     */
    @Test
    fun open_tool_opens_toggles_and_switches_the_sheet_and_close_tool_closes_it() {
        // Run 9: the default tool of loadedViewModel is now Retouch; the sheets are Edit's.
        val viewModel = loadedViewModel(ToolFixtures.gradient(600, 400), tool = Tool.EDIT)
        assertNull(viewModel.state.value.openTool)

        viewModel.openTool(EditTool.ADJUST)
        assertEquals(EditTool.ADJUST, viewModel.state.value.openTool)

        viewModel.openTool(EditTool.ADJUST)
        assertNull("A second tap on the open tool did not close its sheet", viewModel.state.value.openTool)

        viewModel.openTool(EditTool.CROP)
        viewModel.openTool(EditTool.FILTERS)
        assertEquals("Opening another tool did not switch to it", EditTool.FILTERS, viewModel.state.value.openTool)

        viewModel.closeTool()
        assertNull(viewModel.state.value.openTool)
        // Closing with nothing open is harmless.
        viewModel.closeTool()
        assertNull(viewModel.state.value.openTool)

        assertNull(viewModel.state.value.edited)
        assertFalse(viewModel.state.value.canUndo)
    }

    /**
     * Run 9: while Compare is on, every editing call is ignored: `apply` runs no processor and
     * leaves nothing edited, `runRegistered` runs nothing either, and `undo` takes nothing back.
     * Comparing never loses the edit. Switching Compare off makes apply and undo work again.
     */
    @Test
    fun while_comparing_apply_and_undo_change_nothing_until_compare_is_off() {
        val original = ToolFixtures.gradient(600, 400)
        val result = ToolFixtures.gradient(300, 200)
        // Run 9: `apply` is the other tools' path (the Edit screen ignores it), so this runs on
        // Retouch, the default tool of loadedViewModel, and registers Retouch's processor.
        val registry = ProcessorRegistry(mapOf(Tool.RETOUCH to FakeProcessor { _, _ -> EditResult.Edited(result) }))
        val viewModel = loadedViewModel(original, registry = registry)
        val first = ToolFixtures.twoHalves(300, 200)
        val edit = FakeProcessor { _, _ -> EditResult.Edited(first) }

        viewModel.setComparing(true)
        assertTrue(viewModel.state.value.comparing)
        viewModel.apply(edit)
        viewModel.runRegistered()
        runMainThread()
        assertTrue("A processor ran while comparing", edit.inputs.isEmpty())
        assertNull("An edit landed while comparing", viewModel.state.value.edited)
        assertFalse(viewModel.state.value.canUndo)
        assertEquals(ToolUiState.Processing.Idle, viewModel.state.value.processing)

        viewModel.setComparing(false)
        assertFalse(viewModel.state.value.comparing)
        viewModel.apply(edit)
        runMainThread()
        assertSame("Apply did not work again once Compare was off", first, viewModel.state.value.edited)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.setComparing(true)
        // Comparing keeps the edit: it only changes what the screen shows.
        assertSame(first, viewModel.state.value.edited)
        viewModel.undo()
        assertSame("Undo took an edit back while comparing", first, viewModel.state.value.edited)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.setComparing(false)
        viewModel.undo()
        assertNull("Undo did not work again once Compare was off", viewModel.state.value.edited)
        assertFalse(viewModel.state.value.canUndo)
        assertSame(original, (viewModel.state.value.photo as ToolUiState.PhotoState.Ready).bitmap)
    }

    /**
     * Run 9, DoD 18 and 6: a slider released while an earlier change is still being rendered is
     * not lost. A filter chip starts a render that hangs; the person drags a slider and lets go
     * meanwhile. The newest settings are rendered and shown, the hung render finishing late
     * changes nothing, every render starts from the original photo, and the chip and the drag are
     * one undo entry each.
     */
    @Test
    fun a_slider_released_while_a_render_runs_is_not_lost() {
        val original = ToolFixtures.gradient(600, 400)
        val viewModel = loadedViewModel(original, tool = Tool.EDIT)
        val chip = EditSettings(filter = FilterPreset.KELVIN)
        val hung = CompletableDeferred<Unit>()
        val renderer = SettingsRenderer(gates = mapOf(chip to hung))

        viewModel.changeSettings(chip, renderer.processorFor)
        runMainThread()
        assertTrue(viewModel.state.value.processing is ToolUiState.Processing.Running)
        assertNull(viewModel.state.value.edited)

        val released = chip.copy(brightness = 0.4f)
        viewModel.previewSettings(chip.copy(brightness = 0.2f), renderer.processorFor)
        viewModel.previewSettings(released, renderer.processorFor)
        viewModel.commitSettings()
        runMainThread()
        var state = viewModel.state.value
        assertEquals(released, state.settings)
        assertEquals("The released slider was not rendered", released, renderer.settingsOf(state.edited))
        assertEquals(ToolUiState.Processing.Idle, state.processing)

        // The first render finishes late: it is out of date, so it must change nothing.
        hung.complete(Unit)
        runMainThread()
        state = viewModel.state.value
        assertEquals("A late render overwrote the newest one", released, renderer.settingsOf(state.edited))
        assertEquals(ToolUiState.Processing.Idle, state.processing)
        assertNull(state.failure)
        assertTrue("A render did not start from the original", renderer.inputs.all { it === original })

        viewModel.undo()
        runMainThread()
        assertEquals(chip, viewModel.state.value.settings)
        assertEquals(chip, renderer.settingsOf(viewModel.state.value.edited))
        viewModel.undo()
        runMainThread()
        assertEquals(EditSettings(), viewModel.state.value.settings)
        assertNull(viewModel.state.value.edited)
        assertFalse(viewModel.state.value.canUndo)
    }

    /**
     * Run 9, DoD 12: Back on the Edit screen closes an open sheet first (even with changes made);
     * with changes and no sheet it asks "Discard your changes?" and stays; "Keep editing" hides the
     * question and keeps every change; "Discard" hides it too (the screen then leaves). With no
     * changes, at first or after undoing them all, Back asks nothing and says to leave.
     */
    @Test
    fun back_closes_the_sheet_then_asks_to_discard_changes_and_never_without_changes() {
        val viewModel = loadedViewModel(ToolFixtures.gradient(600, 400), tool = Tool.EDIT)
        val renderer = SettingsRenderer()
        assertFalse("Back without changes did not leave", viewModel.onBackPressed())
        assertFalse(viewModel.state.value.discardPrompt)

        viewModel.openTool(EditTool.ADJUST)
        assertTrue(viewModel.onBackPressed())
        assertNull("Back did not close the open sheet", viewModel.state.value.openTool)
        assertFalse(viewModel.state.value.discardPrompt)

        val turned = EditSettings(quarterTurns = 1)
        viewModel.changeSettings(turned, renderer.processorFor)
        runMainThread()
        viewModel.openTool(EditTool.ROTATE)
        assertTrue(viewModel.onBackPressed())
        assertNull(viewModel.state.value.openTool)
        assertFalse("Back asked to discard while a sheet was open", viewModel.state.value.discardPrompt)

        assertTrue(viewModel.onBackPressed())
        assertTrue("Back with changes did not ask to discard them", viewModel.state.value.discardPrompt)
        viewModel.dismissDiscardPrompt()
        assertFalse(viewModel.state.value.discardPrompt)
        assertEquals("Keep editing lost the changes", turned, viewModel.state.value.settings)
        assertEquals(turned, renderer.settingsOf(viewModel.state.value.edited))

        assertTrue(viewModel.onBackPressed())
        assertTrue(viewModel.state.value.discardPrompt)
        viewModel.confirmDiscard()
        assertFalse(viewModel.state.value.discardPrompt)

        viewModel.undo()
        runMainThread()
        assertFalse("Back asked about changes that were all undone", viewModel.onBackPressed())
        assertFalse(viewModel.state.value.discardPrompt)
    }

    /**
     * Run 9: on the Edit screen Save writes the rendered settings (the edited bitmap itself, never
     * the original's Uri); with the settings back at neutral (every change undone) there is no
     * edited photo, so Save copies the original from its Uri.
     */
    @Test
    fun edit_save_writes_the_rendered_settings_and_neutral_settings_save_the_original() {
        val original = ToolFixtures.gradient(600, 400)
        val renderer = SettingsRenderer()

        val saver = FakePhotoSaver()
        val viewModel = loadedViewModel(original, saver, tool = Tool.EDIT)
        val flipped = EditSettings(flipHorizontal = true, contrast = 0.3f)
        viewModel.changeSettings(flipped, renderer.processorFor)
        runMainThread()
        val edited = viewModel.state.value.edited
        assertEquals(flipped, renderer.settingsOf(edited))
        viewModel.save()
        runMainThread()
        assertSame(edited, saver.bitmapCalls.single())
        assertTrue("The original's Uri was saved after an edit", saver.calls.isEmpty())

        val neutralSaver = FakePhotoSaver()
        val neutral = loadedViewModel(original, neutralSaver, tool = Tool.EDIT)
        neutral.changeSettings(flipped, renderer.processorFor)
        runMainThread()
        neutral.undo()
        runMainThread()
        assertNull(neutral.state.value.edited)
        neutral.save()
        runMainThread()
        assertEquals(listOf(PHOTO_URI), neutralSaver.calls.toList())
        assertTrue(neutralSaver.bitmapCalls.isEmpty())
    }

    /**
     * Run 9: the Edit screen ignores the stacking `apply` path: a processor handed to it never
     * runs and nothing is edited, so no change can be baked into the photo behind the settings.
     */
    @Test
    fun the_edit_screen_ignores_apply() {
        val viewModel = loadedViewModel(ToolFixtures.gradient(600, 400), tool = Tool.EDIT)
        val processor = FakeProcessor { input, _ -> EditResult.Edited(input) }
        viewModel.apply(processor)
        runMainThread()
        assertTrue(processor.inputs.isEmpty())
        assertNull(viewModel.state.value.edited)
        assertFalse(viewModel.state.value.canUndo)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Stands in for the Edit panel's recipe `{ settings -> EditProcessor(settings, filters) }`,
     * with no engine: each processor it builds records the settings and the input it was given
     * and answers with a new small bitmap, remembered with its settings so a test can ask which
     * settings the shown picture was rendered from. A render whose settings are in [gates] waits
     * for its gate first, and ignores being cancelled while it waits, as a real processor that
     * does not check for cancellation would.
     */
    private class SettingsRenderer(private val gates: Map<EditSettings, CompletableDeferred<Unit>> = emptyMap()) {

        /** Every input bitmap a render was given, oldest first. */
        val inputs = CopyOnWriteArrayList<Bitmap>()

        /** Each picture rendered, with the settings it was rendered from. */
        private val results = CopyOnWriteArrayList<Pair<Bitmap, EditSettings>>()

        /** Builds the processor for one set of settings, as the ViewModel asks for each render. */
        val processorFor: (EditSettings) -> ToolProcessor = { settings ->
            FakeProcessor { input, _ ->
                inputs += input
                gates[settings]?.let { gate -> withContext(NonCancellable) { gate.await() } }
                val picture = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                results += picture to settings
                EditResult.Edited(picture)
            }
        }

        /** The settings [bitmap] was rendered from, or null for a picture this did not render. */
        fun settingsOf(bitmap: Bitmap?): EditSettings? = results.firstOrNull { it.first === bitmap }?.second
    }

    /**
     * A [ToolProcessor] for tests: records every input it is given and answers with [block],
     * which may report progress, wait, return a result or throw.
     */
    private class FakeProcessor(
        private val block: suspend (input: Bitmap, onProgress: (Float) -> Unit) -> EditResult,
    ) : ToolProcessor {

        /** Every input bitmap this processor was given, oldest first. */
        val inputs = CopyOnWriteArrayList<Bitmap>()

        /** Records [input] and runs [block] on it. */
        override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult {
            inputs += input
            return block(input, onProgress)
        }
    }

    /**
     * Builds the ViewModel on [tool] for [photo] through a [FakePhotoLoader], loads the photo and
     * checks it is shown, so an edit can run on it at once.
     *
     * Run 9: the default tool is Retouch, not Edit. The Run 8 tests that use the default drive the
     * stacking `apply` path, which the Edit screen no longer takes (it renders its settings from
     * the original instead); Retouch still does, at a working size that keeps the test photo whole.
     */
    private fun loadedViewModel(
        photo: Bitmap,
        saver: PhotoSaver = NoSavePhotoSaver(),
        tool: Tool = Tool.RETOUCH,
        registry: ProcessorRegistry = ProcessorRegistry(),
    ): ToolViewModel {
        val viewModel = toolViewModel(FakePhotoLoader(app, photo), saver, tool, registry)
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        return viewModel
    }

    /**
     * Puts [viewModel] in a new ViewModelStore, the way a navigation entry keeps it, and returns
     * the store (clearing it is what leaving the screen does) with the stored ViewModel.
     */
    private fun storedViewModel(viewModel: ToolViewModel): Pair<ViewModelStore, ToolViewModel> {
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            /** Hands out the ViewModel given. */
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            }
        }
        return store to ViewModelProvider(store, factory)[ToolViewModel::class.java]
    }

    /**
     * Builds the ViewModel on [tool] (Edit by default) for [PHOTO_URI] with [loader], the way the
     * app's Koin recipe does but by hand, with a probe nobody reads, [saver] (by default one
     * that writes nothing and fails every save) and [registry]. Shrinking before an edit runs on
     * `Dispatchers.Unconfined`, in place on the test's thread, so the test needs no other thread.
     */
    private fun toolViewModel(
        loader: FakePhotoLoader,
        saver: PhotoSaver = NoSavePhotoSaver(),
        tool: Tool = Tool.EDIT,
        registry: ProcessorRegistry = ProcessorRegistry(),
    ): ToolViewModel =
        ToolViewModel(
            tool = tool,
            photoUri = PHOTO_URI,
            loader = loader,
            probe = ViewModelClearedProbe(),
            saver = saver,
            registry = registry,
            workDispatcher = Dispatchers.Unconfined,
        )

    /** Runs everything waiting in the main thread's queue, where the ViewModel's jobs end. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Values shared by the tests. */
    private companion object {
        /** The photo the ViewModel works on; the fake loader reads nothing from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** The size the screen asks for on the test phone: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3
    }
}
