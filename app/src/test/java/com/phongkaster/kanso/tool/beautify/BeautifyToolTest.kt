package com.phongkaster.kanso.tool.beautify

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
import com.phongkaster.kanso.data.edit.beautify.BeautifyProcessor
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.component.toolpanel.BEAUTIFY_PANEL_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BEAUTIFY_STRENGTH_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BEAUTIFY_WIDTH_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BEAUTIFY_LIFT_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BEAUTIFY_WAIST_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BEAUTIFY_ARMS_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BEAUTIFY_CHEST_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BEAUTIFY_LEGS_TAG
import com.phongkaster.kanso.ui.tool.model.BeautifyAmounts
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BeautifyPanel
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BEAUTIFY_BACK_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.beautifyGroupTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.beautifyToolTag
import com.phongkaster.kanso.ui.tool.model.BeautifyGroup
import com.phongkaster.kanso.ui.tool.model.BeautifyTool
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
 * DoD 10 on the Beautify screen, on Android 15: a photo with no face says so and nothing changes.
 *
 * The test draws the tool screen's stateless part, `ToolContent`, for a real [ToolViewModel] whose
 * registry holds a real [BeautifyProcessor] built on a fake finder that never finds a face, with
 * the real [BeautifyPanel] in the panel slot. Opening Smooth and letting go of its slider runs the
 * processor through the ViewModel, as the app does. Afterwards:
 *
 * - the ViewModel's failure is "no face", the shown photo is still the original (no `edited`
 *   bitmap, its pixels as decoded) and there is nothing to undo, so Save would save the original,
 *   not an edit (Q4);
 * - the message `tool_failed_no_face` is on screen inside the node tagged [NO_FACE_TAG]. The tool
 *   screen wraps the failure card in that tag for this failure (wired with the screen itself).
 *
 * Three more tests check the panel alone: tapping a tool opens its slider, and letting go of
 * either slider hands over its value with the other one at its default.
 *
 * The photo is decoded before the screen is drawn, so the loading indicator (which animates
 * forever) is never on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class BeautifyToolTest {

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
        assertNull("no edit may be shown after a failed Beautify", state.edited)
        assertFalse("a failed Beautify leaves nothing to undo", state.canUndo)
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

    /** No slider shows until a tool is tapped; tapping Smooth shows the smoothing slider only. */
    @Test
    fun tapping_a_tool_opens_its_slider() {
        showPanel()

        compose.onNodeWithTag(BEAUTIFY_STRENGTH_TAG).assertDoesNotExist()
        compose.onNodeWithTag(BEAUTIFY_WIDTH_TAG).assertDoesNotExist()
        open(BeautifyTool.SMOOTH)

        compose.onNodeWithTag(BEAUTIFY_STRENGTH_TAG).assertExists()
        compose.onNodeWithTag(BEAUTIFY_WIDTH_TAG).assertDoesNotExist()
    }

    /**
     * The top row shows Face and Smooth; tapping Face swaps it for Face's row (Width, Back) and
     * opens Width's slider at once.
     */
    @Test
    fun face_opens_its_own_row_with_width_ready() {
        showPanel()

        compose.onNodeWithTag(beautifyGroupTag(BeautifyGroup.FACE)).assertExists()
        compose.onNodeWithTag(beautifyToolTag(BeautifyTool.SMOOTH)).assertExists()
        compose.onNodeWithTag(beautifyToolTag(BeautifyTool.FACE_WIDTH)).assertDoesNotExist()
        tap(beautifyGroupTag(BeautifyGroup.FACE))

        compose.onNodeWithTag(beautifyToolTag(BeautifyTool.FACE_WIDTH)).assertExists()
        compose.onNodeWithTag(BEAUTIFY_BACK_TAG).assertExists()
        compose.onNodeWithTag(beautifyToolTag(BeautifyTool.SMOOTH)).assertDoesNotExist()
        compose.onNodeWithTag(BEAUTIFY_WIDTH_TAG).assertExists()
    }

    /** Back returns to the top row and closes the slider; the width set inside Face is kept. */
    @Test
    fun back_returns_to_the_top_row_keeping_the_values() {
        val applied = showPanel()
        open(BeautifyTool.FACE_WIDTH)
        compose.onNodeWithTag(BEAUTIFY_WIDTH_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.4f) }
        compose.waitForIdle()

        tap(BEAUTIFY_BACK_TAG)
        compose.onNodeWithTag(beautifyGroupTag(BeautifyGroup.FACE)).assertExists()
        compose.onNodeWithTag(BEAUTIFY_WIDTH_TAG).assertDoesNotExist()
        open(BeautifyTool.SMOOTH)
        compose.onNodeWithTag(BEAUTIFY_STRENGTH_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.3f) }
        compose.waitForIdle()

        val (strength, width, lift) = applied.last()
        assertEquals(0.3f, strength, 0.01f)
        assertEquals(0.4f, width, 0.01f)
        assertEquals(0f, lift, 0f)
    }

    /** Letting go of the smoothing slider at 0.25 applies 0.25 with the face width still 0. */
    @Test
    fun releasing_the_smoothing_slider_applies_it() {
        val applied = showPanel()

        open(BeautifyTool.SMOOTH)
        compose.onNodeWithTag(BEAUTIFY_STRENGTH_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.25f) }
        compose.waitForIdle()

        val (strength, width, lift) = applied.single()
        assertEquals(0.25f, strength, 0.01f)
        assertEquals(0f, width, 0f)
        assertEquals(0f, lift, 0f)
    }

    /** Letting go of the face width slider at -0.5 applies -0.5 with no smoothing. */
    @Test
    fun releasing_the_width_slider_applies_it() {
        val applied = showPanel()

        open(BeautifyTool.FACE_WIDTH)
        compose.onNodeWithTag(BEAUTIFY_WIDTH_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(-0.5f) }
        compose.waitForIdle()

        val (strength, width, lift) = applied.single()
        assertEquals(0f, strength, 0f)
        assertEquals(-0.5f, width, 0.01f)
        assertEquals(0f, lift, 0f)
    }

    /** Face's row shows Lift beside Width; tapping it swaps Width's slider for Lift's. */
    @Test
    fun lift_sits_in_the_face_row_and_opens_its_slider() {
        showPanel()

        tap(beautifyGroupTag(BeautifyGroup.FACE))
        compose.onNodeWithTag(beautifyToolTag(BeautifyTool.FACE_LIFT)).assertExists()
        tap(beautifyToolTag(BeautifyTool.FACE_LIFT))

        compose.onNodeWithTag(BEAUTIFY_LIFT_TAG).assertExists()
        compose.onNodeWithTag(BEAUTIFY_WIDTH_TAG).assertDoesNotExist()
    }

    /**
     * Letting go of the face lift slider at -0.6 (left of 0: it is two-sided, as in Meitu) applies
     * -0.6, keeping the width set before it: the two face tools add up, neither resets the other.
     */
    @Test
    fun releasing_the_lift_slider_applies_it_with_the_width() {
        val applied = showPanel()
        open(BeautifyTool.FACE_WIDTH)
        compose.onNodeWithTag(BEAUTIFY_WIDTH_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.3f) }
        compose.waitForIdle()

        tap(beautifyToolTag(BeautifyTool.FACE_LIFT))
        compose.onNodeWithTag(BEAUTIFY_LIFT_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(-0.6f) }
        compose.waitForIdle()

        val (strength, width, lift) = applied.last()
        assertEquals(0f, strength, 0f)
        assertEquals(0.3f, width, 0.01f)
        assertEquals(-0.6f, lift, 0.01f)
    }

    /** The top row shows Body; tapping it swaps the row for Body's (Waist, Back) with Waist's slider open. */
    @Test
    fun body_opens_its_own_row_with_waist_ready() {
        showPanel()

        compose.onNodeWithTag(beautifyGroupTag(BeautifyGroup.BODY)).assertExists()
        tap(beautifyGroupTag(BeautifyGroup.BODY))

        compose.onNodeWithTag(beautifyToolTag(BeautifyTool.WAIST)).assertExists()
        compose.onNodeWithTag(BEAUTIFY_BACK_TAG).assertExists()
        compose.onNodeWithTag(BEAUTIFY_WAIST_TAG).assertExists()
    }

    /** Letting go of the waist slider at 0.7 applies 0.7, with the face and smoothing untouched. */
    @Test
    fun releasing_the_waist_slider_applies_it() {
        val applied = showPanel()

        open(BeautifyTool.WAIST)
        compose.onNodeWithTag(BEAUTIFY_WAIST_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.7f) }
        compose.waitForIdle()

        val amounts = applied.single()
        assertEquals(0.7f, amounts.waist, 0.01f)
        assertEquals(BeautifyAmounts(), amounts.copy(waist = 0f))
    }

    /** Body's row shows Arms beside Waist; letting go of its slider at -0.4 applies -0.4 and nothing else. */
    @Test
    fun releasing_the_arms_slider_applies_it() {
        val applied = showPanel()

        tap(beautifyGroupTag(BeautifyGroup.BODY))
        compose.onNodeWithTag(beautifyToolTag(BeautifyTool.ARMS)).assertExists()
        tap(beautifyToolTag(BeautifyTool.ARMS))
        compose.onNodeWithTag(BEAUTIFY_ARMS_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(-0.4f) }
        compose.waitForIdle()

        val amounts = applied.single()
        assertEquals(-0.4f, amounts.arms, 0.01f)
        assertEquals(BeautifyAmounts(), amounts.copy(arms = 0f))
    }

    /** Body's row shows Chest; letting go of its slider at 0.5 applies 0.5 and nothing else. */
    @Test
    fun releasing_the_chest_slider_applies_it() {
        val applied = showPanel()

        tap(beautifyGroupTag(BeautifyGroup.BODY))
        tap(beautifyToolTag(BeautifyTool.CHEST))
        compose.onNodeWithTag(BEAUTIFY_CHEST_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.5f) }
        compose.waitForIdle()

        val amounts = applied.single()
        assertEquals(0.5f, amounts.chest, 0.01f)
        assertEquals(BeautifyAmounts(), amounts.copy(chest = 0f))
    }

    /** Body's row shows Legs; letting go of its slider at 0.8 applies 0.8 and nothing else. */
    @Test
    fun releasing_the_legs_slider_applies_it() {
        val applied = showPanel()

        tap(beautifyGroupTag(BeautifyGroup.BODY))
        tap(beautifyToolTag(BeautifyTool.LEGS))
        compose.onNodeWithTag(BEAUTIFY_LEGS_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.8f) }
        compose.waitForIdle()

        val amounts = applied.single()
        assertEquals(0.8f, amounts.legs, 0.01f)
        assertEquals(BeautifyAmounts(), amounts.copy(legs = 0f))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the Beautify screen's ViewModel on [photo], with a registry holding a Beautify
     * processor whose finder finds no face, decodes the photo, and draws `ToolContent` with the
     * Beautify panel in its slot. Returns the ViewModel.
     */
    private fun show(photo: Bitmap): ToolViewModel {
        val processor = BeautifyProcessor(FakeFaceFinder.none(), FakeLandmarkFinder(), FakeSkinSmoother(), FakeBodyFinder())
        val viewModel = ToolViewModel(
            Tool.BEAUTIFY,
            PHOTO_URI,
            FakePhotoLoader(context, photo),
            ViewModelClearedProbe(),
            registry = ProcessorRegistry(mapOf(Tool.BEAUTIFY to processor)),
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
                    onDone = {},
                    panel = {
                        BeautifyPanel(
                            onApply = { amounts ->
                                // A slider let go renders quietly from the untouched photo, as the app does.
                                viewModel.renderRegistered(
                                    configure = { registered ->
                                        (registered as BeautifyProcessor).strength = amounts.smoothing
                                        registered.width = amounts.faceWidth
                                        registered.lift = amounts.faceLift
                                        registered.waist = amounts.waist
                                        registered.arms = amounts.arms
                                        registered.chest = amounts.chest
                                        registered.legs = amounts.legs
                                    },
                                )
                            },
                            // While dragging, a live frame with the same values, as the app does.
                            onPreview = { amounts ->
                                viewModel.previewRegistered(
                                    configure = { registered ->
                                        (registered as BeautifyProcessor).strength = amounts.smoothing
                                        registered.width = amounts.faceWidth
                                        registered.lift = amounts.faceLift
                                        registered.waist = amounts.waist
                                        registered.arms = amounts.arms
                                        registered.chest = amounts.chest
                                        registered.legs = amounts.legs
                                    },
                                )
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

    /**
     * Opens Smooth, lets go of its slider at half strength (which runs Beautify) and waits (at most
     * 5 s of real time) until the run has ended in a failure.
     */
    private fun tapSmoothAndWait(viewModel: ToolViewModel) {
        open(BeautifyTool.SMOOTH)
        compose.onNodeWithTag(BEAUTIFY_STRENGTH_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.5f) }
        compose.waitUntil(timeoutMillis = 5_000) {
            // The processor works on a real background thread; its result comes back through the
            // main looper, which a Robolectric test must let run itself.
            shadowOf(Looper.getMainLooper()).idle()
            val state = viewModel.state.value
            state.failure != null && !state.rendering
        }
    }

    /**
     * Draws the Beautify panel alone and returns the list every apply's values are added to. A
     * [BeautifyAmounts] reads back in order as (smoothing, face width, face lift, waist).
     */
    private fun showPanel(): MutableList<BeautifyAmounts> {
        val applied = mutableListOf<BeautifyAmounts>()
        compose.setContent {
            MaterialTheme {
                BeautifyPanel(onApply = { amounts -> applied += amounts }, onPreview = {})
            }
        }
        compose.onNodeWithTag(BEAUTIFY_PANEL_TAG).assertExists()
        return applied
    }

    /**
     * Opens [tool]'s slider as a person would: a tool in a group is reached by tapping the group in
     * the top row (which opens the group's first tool at once), any other tool by tapping it.
     */
    private fun open(tool: BeautifyTool) {
        val group = tool.group
        if (group != null) {
            tap(beautifyGroupTag(group))
            if (group.tools.first() == tool) return
        }
        tap(beautifyToolTag(tool))
    }

    /** Taps the node tagged [tag] and lets the screen settle. */
    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
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
        const val NO_FACE_TAG = "beautify-no-face"
    }
}
