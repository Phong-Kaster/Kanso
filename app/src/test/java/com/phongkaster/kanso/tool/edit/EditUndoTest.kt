package com.phongkaster.kanso.tool.edit

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks Undo on the Edit screen through its ViewModel (Run 9, DoD 7 and 6): a slider release, a
 * filter chip, a crop ratio and a quarter turn are one undo entry each, whichever sheet they came
 * from; Undo takes them back one at a time in reverse order, and after every step what the screen
 * shows agrees with the settings of that moment. A drag that is not released makes no entry of its
 * own.
 *
 * Run 9 (T-003): the Edit screen no longer renders the full photo while editing. What it shows is
 * the live preview: Compose draws the crop, turns, flips and colour sliders straight from
 * `state.settings` (`LivePreviewImage`), on top of `state.filterPreview`, the small copy of the
 * original with only the filter applied (null when there is no filter). `state.edited` stays null
 * until Done. So "the picture shown is the current settings" now means: the settings are the
 * expected ones, nothing was rendered in full, and the filter preview is the original filtered
 * with exactly the expected filter and intensity (or absent when there is none).
 *
 * The filter previews are made by a fake processor recipe ([RecordingRenderer]), never the real
 * engine. The preview renderer always works on `Dispatchers.Default`, whatever dispatcher the
 * ViewModel is given, so its result comes back through the main looper a moment later: the tests
 * poll for it in real time, with a time limit ([waitFor]).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class EditUndoTest {

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * Four actions from four sheets (a released slider, a filter chip, the Square ratio, a quarter
     * turn) make four undo entries. Undo walks them back in reverse order, the shown picture
     * following the settings each time, until the neutral settings show the original (nothing
     * edited); one more Undo changes nothing. Every render started from the original photo.
     *
     * Run 9 (T-003): "the shown picture" is now the live preview ([assertShows]), not a fully
     * rendered `edited` bitmap, which stays null until Done; the renders checked at the end are
     * the filter previews, which must also start from the original (600 x 400 is within the
     * preview's 640 px, so it is not even shrunk).
     */
    @Test
    fun undo_takes_back_one_action_at_a_time_in_reverse_order_across_sheets() {
        val original = ToolFixtures.gradient(600, 400)
        val viewModel = loadedEditViewModel(original)
        val renderer = RecordingRenderer()

        // A slider drag of several steps, then the release: one action.
        viewModel.previewSettings(EditSettings(brightness = 0.1f), renderer.processorFor)
        viewModel.previewSettings(EditSettings(brightness = 0.3f), renderer.processorFor)
        viewModel.commitSettings()
        val afterSlider = viewModel.state.value.settings
        assertEquals(EditSettings(brightness = 0.3f), afterSlider)

        viewModel.changeSettings(afterSlider.copy(filter = FilterPreset.HUDSON), renderer.processorFor)
        val afterFilter = viewModel.state.value.settings

        viewModel.chooseCropRatio(CropRatio.SQUARE, renderer.processorFor)
        val afterCrop = viewModel.state.value.settings
        assertEquals(CropRatio.SQUARE, afterCrop.cropRatio)
        // A square inside a 600 x 400 photo keeps the full height and the middle two thirds.
        assertEquals(1f / 6f, afterCrop.crop.left, 1e-4f)
        assertEquals(5f / 6f, afterCrop.crop.right, 1e-4f)
        assertEquals(0f, afterCrop.crop.top, 1e-4f)
        assertEquals(1f, afterCrop.crop.bottom, 1e-4f)

        viewModel.changeSettings(afterCrop.copy(quarterTurns = 1), renderer.processorFor)
        runMainThread()
        val afterTurn = viewModel.state.value.settings
        assertShows(viewModel, renderer, afterTurn)
        assertTrue(viewModel.state.value.canUndo)

        listOf(afterCrop, afterFilter, afterSlider).forEach { expected ->
            viewModel.undo()
            runMainThread()
            assertEquals(expected, viewModel.state.value.settings)
            assertShows(viewModel, renderer, expected)
            assertTrue(viewModel.state.value.canUndo)
        }

        viewModel.undo()
        runMainThread()
        assertEquals(EditSettings(), viewModel.state.value.settings)
        assertNull("Neutral settings still show an edited picture", viewModel.state.value.edited)
        assertNull("Neutral settings still show a filter", viewModel.state.value.filterPreview)
        assertFalse(viewModel.state.value.canUndo)

        viewModel.undo()
        runMainThread()
        assertEquals(EditSettings(), viewModel.state.value.settings)
        assertFalse(viewModel.state.value.canUndo)

        assertTrue("A render did not start from the original", renderer.inputs.all { it === original })
    }

    /**
     * A slider being dragged moves the picture but makes no undo entry until it is released; a
     * drag released where it began makes none at all; tapping the chip that is already chosen
     * makes none either. Undo during an unreleased drag takes back the drag itself.
     *
     * Run 9 (T-003): the picture is the live preview now ([assertShows]); a colour slider makes no
     * render at all, and a filter removed by Undo leaves no filter preview.
     */
    @Test
    fun a_drag_without_release_and_a_repeated_chip_add_no_undo_entry() {
        val viewModel = loadedEditViewModel(ToolFixtures.gradient(600, 400))
        val renderer = RecordingRenderer()

        val dragged = EditSettings(saturation = -0.5f)
        viewModel.previewSettings(EditSettings(saturation = -0.2f), renderer.processorFor)
        viewModel.previewSettings(dragged, renderer.processorFor)
        runMainThread()
        assertFalse("A drag made an undo entry before its release", viewModel.state.value.canUndo)
        assertEquals(dragged, viewModel.state.value.settings)
        assertShows(viewModel, renderer, dragged)

        // Back to where it began, then released: nothing changed, so nothing to undo.
        viewModel.previewSettings(EditSettings(), renderer.processorFor)
        viewModel.commitSettings()
        runMainThread()
        assertFalse(viewModel.state.value.canUndo)
        assertNull(viewModel.state.value.edited)

        val chip = EditSettings(filter = FilterPreset.WALDEN)
        viewModel.changeSettings(chip, renderer.processorFor)
        viewModel.changeSettings(chip, renderer.processorFor)
        viewModel.undo()
        runMainThread()
        assertEquals("A repeated chip made a second entry", EditSettings(), viewModel.state.value.settings)
        assertFalse(viewModel.state.value.canUndo)
        assertShows(viewModel, renderer, EditSettings())

        viewModel.previewSettings(EditSettings(warmth = 0.6f), renderer.processorFor)
        viewModel.undo()
        runMainThread()
        assertEquals("Undo did not take back the drag under way", EditSettings(), viewModel.state.value.settings)
        assertNull(viewModel.state.value.edited)
    }

    /**
     * While Compare is on, the Edit controls change nothing: a chip, a ratio and Undo are all
     * ignored, and the edit is kept for when Compare is switched off.
     *
     * Run 9 (T-003): what is kept is checked through the live preview ([assertShows]): the turn
     * stays in the settings, no full render is made and the refused filter left no preview.
     */
    @Test
    fun while_comparing_settings_and_undo_change_nothing() {
        val viewModel = loadedEditViewModel(ToolFixtures.gradient(600, 400))
        val renderer = RecordingRenderer()
        val turned = EditSettings(quarterTurns = 3)
        viewModel.changeSettings(turned, renderer.processorFor)
        runMainThread()

        viewModel.setComparing(true)
        viewModel.changeSettings(turned.copy(filter = FilterPreset.AMARO), renderer.processorFor)
        viewModel.chooseCropRatio(CropRatio.SIXTEEN_NINE, renderer.processorFor)
        viewModel.undo()
        runMainThread()
        assertEquals(turned, viewModel.state.value.settings)
        assertShows(viewModel, renderer, turned)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.setComparing(false)
        viewModel.undo()
        runMainThread()
        assertEquals(EditSettings(), viewModel.state.value.settings)
        assertNull(viewModel.state.value.edited)
    }

    /**
     * Redo on the Edit screen: after two Undos, Redo brings the actions back in order, the shown
     * picture following the settings each time (the filter preview included), and `canRedo` says
     * when there is something to bring back. A new action after an Undo empties the redo list, and
     * Redo is ignored while comparing.
     */
    @Test
    fun redo_brings_back_undone_actions_and_a_new_action_clears_it() {
        val viewModel = loadedEditViewModel(ToolFixtures.gradient(600, 400))
        val renderer = RecordingRenderer()
        val filtered = EditSettings(filter = FilterPreset.HUDSON)
        val turned = filtered.copy(quarterTurns = 1)
        viewModel.changeSettings(filtered, renderer.processorFor)
        viewModel.changeSettings(turned, renderer.processorFor)
        runMainThread()
        assertFalse(viewModel.state.value.canRedo)

        viewModel.undo()
        viewModel.undo()
        runMainThread()
        assertEquals(EditSettings(), viewModel.state.value.settings)
        assertTrue(viewModel.state.value.canRedo)

        viewModel.setComparing(true)
        viewModel.redo()
        runMainThread()
        assertEquals("Redo worked while comparing", EditSettings(), viewModel.state.value.settings)
        viewModel.setComparing(false)

        viewModel.redo()
        runMainThread()
        assertShows(viewModel, renderer, filtered)
        assertTrue(viewModel.state.value.canRedo)

        viewModel.redo()
        runMainThread()
        assertShows(viewModel, renderer, turned)
        assertFalse(viewModel.state.value.canRedo)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.undo()
        runMainThread()
        assertTrue(viewModel.state.value.canRedo)
        val changed = filtered.copy(brightness = 0.2f)
        viewModel.changeSettings(changed, renderer.processorFor)
        runMainThread()
        assertFalse("A new action left Redo usable", viewModel.state.value.canRedo)
        viewModel.redo()
        runMainThread()
        assertEquals(changed, viewModel.state.value.settings)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Stands in for the Edit panel's recipe that turns settings into a processor, with no engine:
     * each processor records its input and answers with a new small bitmap, remembered with the
     * settings it was made for, so a test can tell which settings the shown picture came from.
     */
    private class RecordingRenderer {

        /** Every input bitmap a render was given, oldest first. */
        val inputs = CopyOnWriteArrayList<Bitmap>()

        /** Each picture rendered, with the settings it was rendered from. */
        private val results = CopyOnWriteArrayList<Pair<Bitmap, EditSettings>>()

        /** Builds the processor for one set of settings, as the ViewModel asks for each render. */
        val processorFor: (EditSettings) -> ToolProcessor = { settings ->
            object : ToolProcessor {
                /**
                 * Records [input] and answers with a fresh picture tied to the settings.
                 *
                 * Run 9 (T-003): the picture is the input's size, because the filter preview
                 * renderer scales any other size into a new bitmap, which [settingsOf] could no
                 * longer recognise.
                 */
                override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult {
                    inputs += input
                    val picture = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
                    results += picture to settings
                    return EditResult.Edited(picture)
                }
            }
        }

        /** The settings [bitmap] was rendered from, or null for a picture this did not render. */
        fun settingsOf(bitmap: Bitmap?): EditSettings? = results.firstOrNull { it.first === bitmap }?.second
    }

    /**
     * Checks that what the Edit screen shows is [expected], and that nothing is marked as running.
     *
     * Run 9 (T-003): it used to compare a fully rendered `edited` bitmap with a render of the
     * settings; the screen now draws the live preview instead, so this checks the two things that
     * preview is drawn from. The settings, from which Compose takes the colour matrix, the crop,
     * the turns and the flips, must be [expected]. The filter layer (`filterPreview`) must be the
     * original with exactly [expected]'s filter and intensity and nothing else, waited for in real
     * time since it is made on a background thread; with no filter it must be null. And no full
     * render may have happened (`edited` null): that is Done's job.
     */
    private fun assertShows(viewModel: ToolViewModel, renderer: RecordingRenderer, expected: EditSettings) {
        val filterOnly = EditSettings(filter = expected.filter, filterIntensity = expected.filterIntensity)
        if (expected.hasFilter) {
            waitFor("the filter preview of $filterOnly") {
                renderer.settingsOf(viewModel.state.value.filterPreview) == filterOnly
            }
        }
        val state = viewModel.state.value
        assertEquals("The settings drawn are not the current settings", expected, state.settings)
        assertNull("Edit rendered the full photo before Done", state.edited)
        if (expected.hasFilter) {
            assertEquals("The filter shown is not the current one", filterOnly, renderer.settingsOf(state.filterPreview))
        } else {
            assertNull("A filter is shown with no filter chosen", state.filterPreview)
        }
        assertEquals(ToolUiState.Processing.Idle, state.processing)
    }

    /**
     * Runs the main thread's queue until [condition] holds, polling in real time for at most
     * [WAIT_TIMEOUT_MS]; fails, naming [what], if it never does. The filter preview is made on
     * `Dispatchers.Default` and hands its result back through the main looper, so a test that
     * only idled the looper once could look before it arrived.
     */
    private fun waitFor(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS
        while (true) {
            runMainThread()
            if (condition()) return
            assertTrue("Timed out waiting for $what", System.currentTimeMillis() < deadline)
            Thread.sleep(POLL_INTERVAL_MS)
        }
    }

    /** Builds an Edit screen ViewModel for [photo] by hand, loads the photo and checks it is shown. */
    private fun loadedEditViewModel(photo: Bitmap): ToolViewModel {
        val viewModel = ToolViewModel(
            tool = Tool.EDIT,
            photoUri = PHOTO_URI,
            loader = FakePhotoLoader(app, photo),
            probe = ViewModelClearedProbe(),
            workDispatcher = Dispatchers.Unconfined,
        )
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        return viewModel
    }

    /** Runs everything waiting in the main thread's queue, where the ViewModel's jobs end. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Values shared by the tests. */
    private companion object {
        /** The photo the ViewModel works on; the fake loader reads nothing from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** The size the screen asks for: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3

        /** The longest a test waits, in real time, for a filter preview to arrive. */
        const val WAIT_TIMEOUT_MS = 5_000L

        /** How long [waitFor] sleeps between two looks. */
        const val POLL_INTERVAL_MS = 10L
    }
}
