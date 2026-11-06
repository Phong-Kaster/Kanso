package com.phongkaster.kanso.tool.edit

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.CurveAdjustments
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.ToneCurve
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
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
 * Checks the Curves tool through the Edit screen's ViewModel, with the same rules as HSL (DoD 5, 6
 * and 7 of Run 12): bending the master curve lights the Curves dot and nothing else's, each tap,
 * drag, removal or Reset is exactly one Undo step and Redo brings it back, Reset is one step that
 * returns the curve to straight, Compare keeps the change and renders nothing, and Back asks to
 * discard. The ViewModel is built by hand on a generated photo; no screen is drawn.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CurvesToolTest {

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The real Edit processor with no filters. */
    private val processorFor: (EditSettings) -> ToolProcessor = { settings ->
        EditProcessor(settings, FilterLut.Source({ null }))
    }

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** A bent master curve commits, lights the Curves dot, and no other tool's dot. */
    @Test
    fun a_committed_bend_lights_only_the_curves_dot() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.CURVES)
        assertEquals(EditTool.CURVES, viewModel.state.value.openTool)
        assertFalse(viewModel.state.value.settings.hasCurveChange)

        commit(viewModel, bent(0.5f, 0.7f))
        val settings = viewModel.state.value.settings
        assertTrue(settings.hasCurveChange)
        assertFalse(settings.isNeutral(EditTool.CURVES))
        EditTool.entries.filter { it != EditTool.CURVES }.forEach {
            assertTrue("Curves lit the dot of $it", settings.isNeutral(it))
        }
    }

    /** One drag (several previews, one commit) is one Undo step, and Redo brings the final curve back. */
    @Test
    fun one_drag_is_one_undo_step_and_redo_brings_it_back() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.CURVES)

        commit(viewModel, bent(0.5f, 0.6f), bent(0.5f, 0.7f), bent(0.5f, 0.8f))
        val afterDrag = viewModel.state.value.settings
        assertEquals(0.8f, afterDrag.curves.rgb.points[1].y, 0f)

        viewModel.undo()
        runMainThread()
        assertEquals("one Undo took back only part of the drag", EditSettings(), viewModel.state.value.settings)
        assertFalse("the drag was more than one step", viewModel.state.value.canUndo)

        viewModel.redo()
        runMainThread()
        assertEquals(afterDrag, viewModel.state.value.settings)
    }

    /** Two separate bends are two Undo steps, undone one at a time. */
    @Test
    fun two_commits_are_two_undo_steps() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.CURVES)
        commit(viewModel, bent(0.5f, 0.7f))
        val first = viewModel.state.value.settings
        commit(viewModel, bent(0.5f, 0.7f).let { curveSettings(it.curves.rgb.added(CurvePoint(0.25f, 0.2f))) })

        viewModel.undo()
        runMainThread()
        assertEquals(first, viewModel.state.value.settings)
        assertTrue(viewModel.state.value.canUndo)
        viewModel.undo()
        runMainThread()
        assertEquals(EditSettings(), viewModel.state.value.settings)
    }

    /** Reset is one commit of the straight curve: one Undo step that brings the bend back, and the dot goes out. */
    @Test
    fun reset_is_one_undo_step_and_returns_to_neutral() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.CURVES)
        commit(viewModel, bent(0.5f, 0.7f))
        val bentSettings = viewModel.state.value.settings

        commit(viewModel, curveSettings(ToneCurve.NEUTRAL))
        assertTrue(viewModel.state.value.settings.curves.isNeutral)
        assertFalse(viewModel.state.value.settings.hasCurveChange)
        assertTrue(viewModel.state.value.settings.isNeutral(EditTool.CURVES))

        viewModel.undo()
        runMainThread()
        assertEquals("Undo of Reset did not bring the bend back", bentSettings, viewModel.state.value.settings)
        viewModel.redo()
        runMainThread()
        assertTrue(viewModel.state.value.settings.curves.isNeutral)
    }

    /** Compare keeps the curve change and renders nothing in full while on. */
    @Test
    fun compare_shows_the_photo_without_the_curve_change() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.CURVES)
        commit(viewModel, bent(0.4f, 0.6f))
        val edited = viewModel.state.value.settings

        viewModel.setComparing(true)
        runMainThread()
        assertTrue(viewModel.state.value.comparing)
        assertEquals("comparing lost the curve change", edited, viewModel.state.value.settings)
        assertNull("comparing rendered the edit", viewModel.state.value.edited)

        viewModel.setComparing(false)
        runMainThread()
        assertFalse(viewModel.state.value.comparing)
        assertEquals(edited, viewModel.state.value.settings)
    }

    /** Back first closes the Curves sheet; Back again, with a curve change made, asks to discard. */
    @Test
    fun back_with_a_curve_change_asks_to_discard() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.CURVES)
        commit(viewModel, bent(0.5f, 0.3f))

        assertTrue(viewModel.onBackPressed())
        assertNull(viewModel.state.value.openTool)
        assertTrue("Back left the screen with a curve change", viewModel.onBackPressed())
        assertTrue(viewModel.state.value.discardPrompt)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Settings whose master curve is [curve]. */
    private fun curveSettings(curve: ToneCurve): EditSettings =
        EditSettings(curves = CurveAdjustments.NEUTRAL.with(CurveChannel.RGB, curve))

    /** Settings whose master curve has one middle point at ([x], [y]). */
    private fun bent(x: Float, y: Float): EditSettings =
        curveSettings(ToneCurve.NEUTRAL.added(CurvePoint(x, y)))

    /** One gesture: each of [steps] previewed while the finger moves, then one commit. */
    private fun commit(viewModel: ToolViewModel, vararg steps: EditSettings) {
        steps.forEach { step -> viewModel.previewSettings(step, processorFor) }
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
