package com.phongkaster.kanso.tool.retouch

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.retouch.RetouchProcessor
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.NoSavePhotoSaver
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.RETOUCH_PANEL_TAG
import com.phongkaster.kanso.ui.tool.RETOUCH_RUN_TAG
import com.phongkaster.kanso.ui.tool.RETOUCH_STRENGTH_TAG
import com.phongkaster.kanso.ui.tool.RetouchPanel
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 10 on the Retouch screen, on Android 15: a photo with no face says so and nothing changes.
 *
 * The test draws the tool screen's stateless part, `ToolContent`, for a real [ToolViewModel] whose
 * registry holds a real [RetouchProcessor] built on a fake finder that never finds a face, with
 * the real [RetouchPanel] in the panel slot. Tapping "Smooth skin" runs the processor through the
 * ViewModel, as the app does. Afterwards:
 *
 * - the ViewModel's failure is "no face", the shown photo is still the original (no `edited`
 *   bitmap, its pixels as decoded) and there is nothing to undo, so Save would save the original,
 *   not an edit (Q4);
 * - the message `tool_failed_no_face` is on screen inside the node tagged [NO_FACE_TAG]. The tool
 *   screen wraps the failure card in that tag for this failure (wired with the screen itself).
 *
 * Two more tests check the panel alone: Apply hands over full strength by default and the
 * slider's strength once it has been moved.
 *
 * The photo is decoded before the screen is drawn, so the loading indicator (which animates
 * forever) is never on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class RetouchToolTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** With no face found, the failure is "no face", the photo is unchanged and nothing can be undone. */
    @Test
    fun no_face_changes_nothing() {
        val photo = ToolFixtures.gradient(120, 80)
        val before = pixels(photo)
        val viewModel = show(photo)

        tapSmoothAndWait(viewModel)

        val state = viewModel.state.value
        assertEquals(EditFailure.NoFace, state.failure)
        assertNull("no edit may be shown after a failed Retouch", state.edited)
        assertFalse("a failed Retouch leaves nothing to undo", state.canUndo)
        val shown = (state.photo as ToolUiState.PhotoState.Ready).bitmap
        assertArrayEquals("the shown photo must be the original", before, pixels(shown))
    }

    /** With no face found, the screen shows the "no face" message inside [NO_FACE_TAG]. */
    @Test
    fun no_face_shows_the_message() {
        val viewModel = show(ToolFixtures.gradient(120, 80))

        tapSmoothAndWait(viewModel)
        compose.waitForIdle()

        val message = context.getString(R.string.tool_failed_no_face)
        val inTag = hasText(message) and (hasTestTag(NO_FACE_TAG) or hasAnyAncestor(hasTestTag(NO_FACE_TAG)))
        // The card may merge its text into one clickable node, so both trees are searched.
        val found = compose.onAllNodes(inTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
            compose.onAllNodes(inTag).fetchSemanticsNodes().isNotEmpty()
        assertTrue("\"$message\" is not shown inside $NO_FACE_TAG", found)
    }

    /** Apply with the slider untouched hands over full strength, 1. */
    @Test
    fun apply_hands_over_full_strength_by_default() {
        val applied = showPanel()

        compose.onNodeWithTag(RETOUCH_RUN_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(listOf(1f), applied)
    }

    /** Apply after the slider is moved to 0.25 hands over 0.25. */
    @Test
    fun apply_hands_over_the_slider_strength() {
        val applied = showPanel()

        compose.onNodeWithTag(RETOUCH_STRENGTH_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.25f) }
        compose.onNodeWithTag(RETOUCH_RUN_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(1, applied.size)
        assertEquals(0.25f, applied.single(), 0.01f)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the Retouch screen's ViewModel on [photo], with a registry holding a Retouch
     * processor whose finder finds no face, decodes the photo, and draws `ToolContent` with the
     * Retouch panel in its slot. Returns the ViewModel.
     */
    private fun show(photo: Bitmap): ToolViewModel {
        val processor = RetouchProcessor(FakeFaceFinder.none(), FakeSkinSmoother())
        val viewModel = ToolViewModel(
            Tool.RETOUCH,
            PHOTO_URI,
            FakePhotoLoader(context, photo),
            ViewModelClearedProbe(),
            NoSavePhotoSaver(),
            registry = ProcessorRegistry(mapOf(Tool.RETOUCH to processor)),
            workDispatcher = Dispatchers.Unconfined,
        )
        // The fake loader answers at once; idling the main thread lets the result land.
        viewModel.load(1000)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        compose.setContent {
            MaterialTheme {
                // collectAsState redraws the screen whenever the ViewModel's state changes.
                val state by viewModel.state.collectAsState()
                ToolContent(
                    state = state,
                    onBack = {},
                    onSave = {},
                    panel = {
                        RetouchPanel(
                            onApply = { strength ->
                                // A slider tool runs from the untouched photo with its strength set first.
                                viewModel.runRegistered(fromOriginal = true) { registered ->
                                    (registered as RetouchProcessor).strength = strength
                                }
                            },
                        )
                    },
                    onUndo = viewModel::undo,
                    onDismissFailure = viewModel::dismissFailure,
                )
            }
        }
        compose.waitForIdle()
        return viewModel
    }

    /** Taps "Smooth skin" and waits (at most 5 s of real time) until the run has ended in a failure. */
    private fun tapSmoothAndWait(viewModel: ToolViewModel) {
        compose.onNodeWithTag(RETOUCH_RUN_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(timeoutMillis = 5_000) {
            // The processor works on a real background thread; its result comes back through the
            // main looper, which a Robolectric test must let run itself.
            shadowOf(Looper.getMainLooper()).idle()
            val state = viewModel.state.value
            state.failure != null && state.processing is ToolUiState.Processing.Idle
        }
    }

    /** Draws the Retouch panel alone and returns the list every Apply's strength is added to. */
    private fun showPanel(): MutableList<Float> {
        val applied = mutableListOf<Float>()
        compose.setContent {
            MaterialTheme {
                RetouchPanel(onApply = { applied += it })
            }
        }
        compose.onNodeWithTag(RETOUCH_PANEL_TAG).assertExists()
        return applied
    }

    /** Every pixel of [bitmap], row after row. */
    private fun pixels(bitmap: Bitmap): IntArray {
        val data = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return data
    }

    /** The test's fixed names. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The tag of the node around the "no face" message on the tool screen (DoD 10). */
        const val NO_FACE_TAG = "retouch-no-face"
    }
}
