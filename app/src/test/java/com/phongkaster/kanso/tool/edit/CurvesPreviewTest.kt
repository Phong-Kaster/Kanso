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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 7, "the photo follows the finger while a point is dragged, even with no filter chosen",
 * checked through the Edit screen's ViewModel.
 *
 * A session with no filter, no HSL change and no sticker, whose only change is a bent master curve,
 * must still get a new filter preview (the small CPU copy the screen draws), and straightening the
 * curve again must take the copy away so the screen shows the original.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CurvesPreviewTest {

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The real Edit processor with no filters: this test chooses none. */
    private val processorFor: (EditSettings) -> ToolProcessor = { settings ->
        EditProcessor(settings, FilterLut.Source({ null }))
    }

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** A curve-only change produces a new, non-null filter preview; a straight curve removes it. */
    @Test
    fun a_curve_only_change_produces_a_filter_preview() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.CURVES)
        assertNull(viewModel.state.value.filterPreview)

        val bent = EditSettings(
            curves = CurveAdjustments.NEUTRAL.with(
                CurveChannel.RGB,
                ToneCurve.NEUTRAL.added(CurvePoint(0.5f, 0.2f)),
            ),
        )
        viewModel.previewSettings(bent, processorFor)
        waitUntil("the curve's preview") { viewModel.state.value.filterPreview != null }
        assertNotNull(viewModel.state.value.filterPreview)

        viewModel.previewSettings(EditSettings(), processorFor)
        runMainThread()
        assertNull("a straight curve kept a preview", viewModel.state.value.filterPreview)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Polls, running the main thread's queue, until [done] is true or five seconds pass. */
    private fun waitUntil(what: String, done: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!done() && System.currentTimeMillis() < deadline) {
            runMainThread()
            Thread.sleep(20)
        }
        runMainThread()
        assertTrue("timed out waiting for $what", done())
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

        /** How long to wait for the background render. */
        const val TIMEOUT_MS = 5_000L
    }
}
