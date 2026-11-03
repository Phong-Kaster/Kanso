package com.phongkaster.kanso.tool.edit

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.EditColourMatrix
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.model.RgbChannel
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
 * Checks the RGB tool through the Edit screen's ViewModel (plan `docs/plans/rgb-tool.md`,
 * decision 8, "the same rules as Adjust"):
 *
 * 1. the first time the sheet opens, the Red chip is chosen;
 * 2. reopened, it shows the chip chosen last, and choosing a chip never changes the photo;
 * 3. each drag-and-let-go is one Undo step, and Redo brings it back;
 * 4. the RGB entry's "changed" dot shows while a channel is away from 0, and only RGB's;
 * 5. Compare keeps the RGB change and never renders it; the live preview draws it through the
 *    shared colour matrix; Done is the processor, checked in `EditProcessorTest`;
 * 6. Back with only an RGB change asks "Discard your changes?".
 *
 * The ViewModel is built by hand, as in `EditUndoTest`, on a generated photo; no screen is drawn.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RgbToolTest {

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The real Edit processor with no filters: the RGB tests choose none. */
    private val processorFor: (EditSettings) -> ToolProcessor = { settings ->
        EditProcessor(settings, FilterLut.Source({ null }))
    }

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * Rules 1 and 2: Red is chosen at first; a chosen Blue is still chosen after the sheet is
     * closed and opened again; choosing chips changes neither the settings nor the Undo list.
     */
    @Test
    fun red_first_and_the_last_chip_is_remembered() {
        val viewModel = loadedEditViewModel()
        assertEquals(RgbChannel.RED, viewModel.state.value.rgbChannel)

        viewModel.openTool(EditTool.RGB)
        viewModel.chooseRgbChannel(RgbChannel.BLUE)
        viewModel.openTool(EditTool.RGB) // tapping the open tool again closes its sheet
        assertNull(viewModel.state.value.openTool)
        viewModel.openTool(EditTool.RGB)

        assertEquals(EditTool.RGB, viewModel.state.value.openTool)
        assertEquals(RgbChannel.BLUE, viewModel.state.value.rgbChannel)
        assertEquals("a chip changed the photo", EditSettings(), viewModel.state.value.settings)
        assertFalse("a chip made an Undo step", viewModel.state.value.canUndo)
    }

    /**
     * Rule 3: a drag of several steps, let go, is one Undo step; a second drag on another channel
     * is another. Undo takes them back one at a time; Redo brings them back in order.
     */
    @Test
    fun each_drag_is_one_undo_step_and_redo_brings_it_back() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.RGB)

        drag(viewModel, EditSettings(red = 0.1f), EditSettings(red = 0.3f))
        val afterRed = viewModel.state.value.settings
        assertEquals(EditSettings(red = 0.3f), afterRed)
        drag(viewModel, afterRed.copy(blue = -0.2f), afterRed.copy(blue = -0.5f))
        val afterBlue = viewModel.state.value.settings

        viewModel.undo()
        runMainThread()
        assertEquals(afterRed, viewModel.state.value.settings)
        viewModel.undo()
        runMainThread()
        assertEquals(EditSettings(), viewModel.state.value.settings)
        assertFalse(viewModel.state.value.canUndo)

        viewModel.redo()
        runMainThread()
        assertEquals(afterRed, viewModel.state.value.settings)
        viewModel.redo()
        runMainThread()
        assertEquals(afterBlue, viewModel.state.value.settings)
    }

    /**
     * Rule 4: the RGB entry carries the dot while a channel is away from 0 and Adjust does not;
     * dragging the channel back to 0 takes the dot away again.
     */
    @Test
    fun the_dot_follows_the_channels() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.RGB)
        assertTrue(viewModel.state.value.settings.isNeutral(EditTool.RGB))

        drag(viewModel, EditSettings(green = 0.4f))
        assertFalse("no dot on RGB", viewModel.state.value.settings.isNeutral(EditTool.RGB))
        assertTrue("a dot on Adjust", viewModel.state.value.settings.isNeutral(EditTool.ADJUST))

        drag(viewModel, EditSettings(green = 0f))
        assertTrue("the dot stayed at 0", viewModel.state.value.settings.isNeutral(EditTool.RGB))
    }

    /**
     * Rule 5: while comparing, the RGB change is kept and nothing is rendered in full; with Compare
     * off again the live preview's colour matrix is the one built from the settings, which holds
     * the channel (it is not the identity).
     */
    @Test
    fun compare_keeps_the_rgb_change_and_the_preview_draws_it() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.RGB)
        drag(viewModel, EditSettings(red = -0.6f))
        val edited = viewModel.state.value.settings

        viewModel.setComparing(true)
        runMainThread()
        assertTrue(viewModel.state.value.comparing)
        assertEquals("comparing lost the RGB change", edited, viewModel.state.value.settings)
        assertNull("comparing rendered the edit", viewModel.state.value.edited)

        viewModel.setComparing(false)
        runMainThread()
        assertEquals(edited, viewModel.state.value.settings)
        assertFalse(
            "the preview's matrix ignores the RGB change",
            EditColourMatrix.isIdentity(EditColourMatrix.of(viewModel.state.value.settings)),
        )
    }

    /**
     * Rule 6: Back first closes the RGB sheet; Back again, with only an RGB change made, asks
     * "Discard your changes?" instead of leaving.
     */
    @Test
    fun back_with_an_rgb_change_asks_to_discard() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.RGB)
        drag(viewModel, EditSettings(blue = 0.5f))

        assertTrue(viewModel.onBackPressed())
        assertNull(viewModel.state.value.openTool)
        assertTrue("Back left the screen with an RGB change", viewModel.onBackPressed())
        assertTrue(viewModel.state.value.discardPrompt)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * One slider drag: each of [steps] handed over while the finger moves, then the finger let go,
     * as the RGB sheet's slider does.
     */
    private fun drag(viewModel: ToolViewModel, vararg steps: EditSettings) {
        steps.forEach(action = { step -> viewModel.previewSettings(step, processorFor) })
        viewModel.commitSettings()
        runMainThread()
    }

    /** Builds an Edit screen ViewModel for a generated photo by hand and loads it. */
    private fun loadedEditViewModel(): ToolViewModel {
        val photo: Bitmap = ToolFixtures.gradient(600, 400)
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
    }
}
