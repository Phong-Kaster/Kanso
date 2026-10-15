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
import com.phongkaster.kanso.tool.NoSavePhotoSaver
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
 * from; Undo takes them back one at a time in reverse order, and after every step the photo shown
 * is the original rendered with the settings of that moment. A drag that is not released makes no
 * entry of its own.
 *
 * The renders are done by a fake processor recipe ([RecordingRenderer]), never the real engine,
 * and they run on `Dispatchers.Unconfined` on Robolectric's main thread, so each one has finished
 * when the call returns.
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

        viewModel.previewSettings(EditSettings(warmth = 0.6f), renderer.processorFor)
        viewModel.undo()
        runMainThread()
        assertEquals("Undo did not take back the drag under way", EditSettings(), viewModel.state.value.settings)
        assertNull(viewModel.state.value.edited)
    }

    /**
     * While Compare is on, the Edit controls change nothing: a chip, a ratio and Undo are all
     * ignored, and the edit is kept for when Compare is switched off.
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
                /** Records [input] and answers with a fresh picture tied to the settings. */
                override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult {
                    inputs += input
                    val picture = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                    results += picture to settings
                    return EditResult.Edited(picture)
                }
            }
        }

        /** The settings [bitmap] was rendered from, or null for a picture this did not render. */
        fun settingsOf(bitmap: Bitmap?): EditSettings? = results.firstOrNull { it.first === bitmap }?.second
    }

    /** Checks that the picture shown is [expected] rendered, and that no render is still running. */
    private fun assertShows(viewModel: ToolViewModel, renderer: RecordingRenderer, expected: EditSettings) {
        val state = viewModel.state.value
        assertEquals("The picture shown is not the current settings", expected, renderer.settingsOf(state.edited))
        assertEquals(ToolUiState.Processing.Idle, state.processing)
    }

    /** Builds an Edit screen ViewModel for [photo] by hand, loads the photo and checks it is shown. */
    private fun loadedEditViewModel(photo: Bitmap): ToolViewModel {
        val viewModel = ToolViewModel(
            tool = Tool.EDIT,
            photoUri = PHOTO_URI,
            loader = FakePhotoLoader(app, photo),
            probe = ViewModelClearedProbe(),
            saver = NoSavePhotoSaver(),
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
    }
}
