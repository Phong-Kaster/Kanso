package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.NoSavePhotoSaver
import com.phongkaster.kanso.ui.tool.EDIT_BRIGHTNESS_TAG
import com.phongkaster.kanso.ui.tool.EDIT_FLIP_HORIZONTAL_TAG
import com.phongkaster.kanso.ui.tool.EDIT_FLIP_VERTICAL_TAG
import com.phongkaster.kanso.ui.tool.EDIT_INTENSITY_TAG
import com.phongkaster.kanso.ui.tool.EDIT_ROTATE_LEFT_TAG
import com.phongkaster.kanso.ui.tool.EDIT_ROTATE_RIGHT_TAG
import com.phongkaster.kanso.ui.tool.EditPanel
import com.phongkaster.kanso.ui.tool.EditPanelContent
import com.phongkaster.kanso.ui.tool.EditToolRow
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.editFilterTag
import com.phongkaster.kanso.ui.tool.editRatioTag
import com.phongkaster.kanso.ui.tool.editSliderValueTag
import com.phongkaster.kanso.ui.tool.editToolDotTag
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Checks on Android 15 the Edit tool's controls and what they do to the photo (DoD 2, 5, 14, 16).
 *
 * Most tests draw the real Edit panel (`EditPanel`) for a real [ToolViewModel] on a fake 120 x 80
 * photo: the panel shows the sheet of the tool opened with the ViewModel's `openTool`, draws the
 * session's settings from the ViewModel's state, and hands each change with the real Edit
 * processor to the ViewModel (`changeSettings`, `previewSettings`, `commitSettings`,
 * `chooseCropRatio`), as the screen does. The photo the screen shows is the state's `edited`
 * bitmap once an edit is made, so that is the bitmap read here. Within 5 s of real time:
 *
 * - brightness pushed to its maximum and let go makes the shown photo brighter on average;
 * - the 1:1 chip makes the shown photo square;
 * - a filter chip changes the shown photo's colours, and dragging its intensity changes them
 *   again while the finger is still down. The filter's look-up comes from a generated inverting
 *   strip handed to the panel, so this test checks the panel's wiring, not the assets
 *   (`EditFilterTest` checks those);
 * - brightness set to 70 is still 70 when Adjust is opened again after another sheet.
 *
 * Two tests draw stateless parts with the settings held by the test: the Rotate sheet's four
 * buttons (one action each) and the row of tools' "changed" dots.
 *
 * The photo is decoded before the screen is drawn, so the loading indicator (which animates
 * forever) is never on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EditToolTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** The settings the stateless tests draw, held by the test as the ViewModel holds them. */
    private var heldSettings by mutableStateOf(EditSettings())

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * Brightness at its maximum, let go, makes the shown photo brighter on average.
     *
     * Run 9: brightness lives in the Adjust sheet now, so the test opens Adjust first; the slider
     * now hands whole settings to the ViewModel's `previewSettings`/`commitSettings` instead of a
     * one-off change to the removed `apply`, and the photo is rendered from the original.
     */
    @Test
    fun brightness_brightens_the_shown_photo() {
        val viewModel = show(EditTool.ADJUST)
        val before = meanLuminance(shown(viewModel))
        compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(EditSettings.MAX) }
        val after = waitForEdit(viewModel, previous = null)
        assertTrue("brightness: $before -> ${meanLuminance(after)}", meanLuminance(after) > before)
    }

    /**
     * The 1:1 chip makes the shown 120 x 80 photo square.
     *
     * Run 9: the crop chips live in the Crop sheet now, so the test opens Crop first; a chip now
     * goes to the ViewModel's `chooseCropRatio`, which sets the centred square crop rectangle.
     */
    @Test
    fun a_crop_ratio_changes_the_shape_of_the_shown_photo() {
        val viewModel = show(EditTool.CROP)
        val before = shown(viewModel)
        assertNotEquals(before.width, before.height)
        compose.onNodeWithTag(editRatioTag(CropRatio.SQUARE)).performSemanticsAction(SemanticsActions.OnClick)
        val after = waitForEdit(viewModel, previous = null)
        assertEquals(after.height, after.width)
        assertEquals(before.height, after.height)
    }

    /**
     * A filter chip changes the colours of the shown photo.
     *
     * Run 9: the filter chips live in the Filters sheet now, so the test opens Filters first; a
     * chip is now one action through the ViewModel's `changeSettings`.
     */
    @Test
    fun a_filter_changes_the_colours_of_the_shown_photo() {
        val viewModel = show(EditTool.FILTERS)
        val before = channelMeans(shown(viewModel))
        compose.onNodeWithTag(editFilterTag(FilterPreset.KELVIN)).performSemanticsAction(SemanticsActions.OnClick)
        val after = channelMeans(waitForEdit(viewModel, previous = null))
        val change = (0..2).maxOf { abs(after[it] - before[it]) }
        assertTrue("the filter barely changed the colours ($change)", change > 3.0)
    }

    /**
     * DoD 16: dragging the filter's intensity changes the shown photo while the finger is still
     * down; lifting it keeps the result and leaves an Undo step.
     */
    @Test
    fun the_filter_intensity_changes_the_photo_while_dragged() {
        val viewModel = show(EditTool.FILTERS)
        compose.onNodeWithTag(editFilterTag(FilterPreset.KELVIN)).performSemanticsAction(SemanticsActions.OnClick)
        val full = waitForEdit(viewModel, previous = null)
        val fullMeans = channelMeans(full)

        val slider = compose.onNodeWithTag(EDIT_INTENSITY_TAG)
        slider.performTouchInput {
            // Intensity 100 has the thumb at the right end; starting in the middle and moving
            // left lowers it well below 100.
            down(center)
            repeat(DRAG_STEPS) { moveBy(Offset(-width * DRAG_STEP_FRACTION, 0f)) }
        }
        val dragged = waitForEdit(viewModel, previous = full)
        assertTrue(
            "the intensity did not fall while dragged",
            viewModel.state.value.settings.filterIntensity < 0.9f,
        )
        val draggedMeans = channelMeans(dragged)
        val change = (0..2).maxOf { abs(draggedMeans[it] - fullMeans[it]) }
        assertTrue("the drag barely changed the photo ($change)", change > 3.0)

        slider.performTouchInput { up() }
        compose.waitForIdle()
        assertTrue(viewModel.state.value.canUndo)
        assertEquals(FilterPreset.KELVIN, viewModel.state.value.settings.filter)
    }

    /**
     * DoD 5 through the real ViewModel: brightness set to 70 is still 70, on the slider and in
     * its number, after Filters is opened and then Adjust again (each opening draws a new sheet).
     */
    @Test
    fun brightness_70_is_still_70_when_adjust_is_opened_again() {
        val viewModel = show(EditTool.ADJUST)
        compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.7f) }
        compose.waitForIdle()
        compose.onNodeWithTag(editSliderValueTag(EDIT_BRIGHTNESS_TAG)).assertTextEquals("70")

        compose.runOnIdle { viewModel.openTool(EditTool.FILTERS) }
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG).assertDoesNotExist()
        compose.runOnIdle { viewModel.openTool(EditTool.ADJUST) }
        compose.waitForIdle()

        compose.onNodeWithTag(editSliderValueTag(EDIT_BRIGHTNESS_TAG)).assertTextEquals("70")
        val slider = compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG).fetchSemanticsNode()
        assertEquals(0.7f, slider.config[SemanticsProperties.ProgressBarRangeInfo].current, 0.0001f)
        assertEquals(0.7f, viewModel.state.value.settings.brightness, 0.0001f)
    }

    /**
     * DoD 14: the Rotate sheet offers Turn left, Turn right, Flip across and Flip upside down,
     * and each tap is exactly one action (`onChange`, never `onPreview`) with the expected
     * settings: a quarter turn each way, then each flip on.
     */
    @Test
    fun rotate_offers_four_actions_each_calling_on_change_once() {
        val changes = mutableListOf<EditSettings>()
        var previews = 0
        compose.setContent {
            MaterialTheme {
                EditPanelContent(
                    openTool = EditTool.ROTATE,
                    settings = heldSettings,
                    onChange = { new ->
                        changes += new
                        heldSettings = new
                    },
                    onPreview = { previews++ },
                    onCommit = {},
                    onRatio = {},
                )
            }
        }
        compose.waitForIdle()
        val buttons = listOf(
            EDIT_ROTATE_LEFT_TAG to "Turn left",
            EDIT_ROTATE_RIGHT_TAG to "Turn right",
            EDIT_FLIP_HORIZONTAL_TAG to "Flip across",
            EDIT_FLIP_VERTICAL_TAG to "Flip upside down",
        )
        buttons.forEachIndexed { index, (tag, name) ->
            compose.onNodeWithTag(tag).assert(hasText(name))
            // The row scrolls sideways, so the tap goes through the semantics action.
            compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
            assertEquals("$name made ${changes.size - index} actions", index + 1, changes.size)
        }
        // Run 9 review: turns wrap into 0..3, so one turn left is 3 (the same as -1) and the turn
        // right after it comes back to 0.
        assertEquals(3, changes[0].quarterTurns)
        assertEquals(0, changes[1].quarterTurns)
        assertTrue(changes[2].flipHorizontal)
        assertFalse(changes[2].flipVertical)
        assertTrue(changes[3].flipHorizontal && changes[3].flipVertical)
        assertEquals("a button is never a slider drag", 0, previews)
    }

    /**
     * DoD 2: the row of tools shows a small dot ("Changed") only on a tool whose settings are not
     * neutral: none for the starting settings, Adjust for a brightness change, Rotate for a
     * quarter turn but not for a full turn, Filters for a filter, Crop for a cropped rectangle.
     */
    @Test
    fun a_dot_marks_only_the_tools_whose_settings_are_not_neutral() {
        compose.setContent {
            MaterialTheme {
                EditToolRow(openTool = null, enabled = true, onOpen = {}, settings = heldSettings)
            }
        }
        compose.waitForIdle()
        assertDots(emptySet())

        setHeld(EditSettings(brightness = 0.4f))
        assertDots(setOf(EditTool.ADJUST))
        compose.onNodeWithTag(editToolDotTag(EditTool.ADJUST), useUnmergedTree = true)
            .assert(hasContentDescription("Changed"))

        setHeld(EditSettings(brightness = 0.4f, quarterTurns = 4))
        assertDots(setOf(EditTool.ADJUST))
        setHeld(EditSettings(brightness = 0.4f, quarterTurns = 1))
        assertDots(setOf(EditTool.ADJUST, EditTool.ROTATE))
        setHeld(EditSettings(filter = FilterPreset.KELVIN))
        assertDots(setOf(EditTool.FILTERS))
        setHeld(EditSettings(crop = EditSettings.CropRect(0.1f, 0f, 1f, 1f)))
        assertDots(setOf(EditTool.CROP))
        setHeld(EditSettings())
        assertDots(emptySet())
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the Edit screen's ViewModel on the fake photo, decodes the photo, opens [open]'s
     * sheet, and draws the Edit panel for whichever tool the ViewModel has open. Returns the
     * ViewModel.
     *
     * Run 9: the host no longer draws `ToolContent` with `EditPanel(onApply = viewModel::apply)`:
     * that API is gone (the panel now hands whole settings to the session calls), and the tool
     * screen's sheet slot is being rewired outside this test. The test draws the panel itself, as
     * the screen's sheet area does: from the ViewModel's settings, drawn afresh (`key`) for each
     * tool opened. The slider, chip, processor, ViewModel and shown photo are still all real.
     */
    private fun show(open: EditTool): ToolViewModel {
        val viewModel = ToolViewModel(
            Tool.EDIT,
            PHOTO_URI,
            FakePhotoLoader(context, photo()),
            ViewModelClearedProbe(),
            NoSavePhotoSaver(),
            workDispatcher = Dispatchers.Unconfined,
        )
        // The fake loader answers at once; idling the main thread lets the result land.
        viewModel.load(1000)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        viewModel.openTool(open)
        assertEquals(open, viewModel.state.value.openTool)
        val filters = invertingSource()
        compose.setContent {
            MaterialTheme {
                // collectAsState redraws the panel whenever the ViewModel's state changes.
                val state by viewModel.state.collectAsState()
                val tool = state.openTool
                if (tool != null) {
                    key(tool) {
                        EditPanel(
                            openTool = tool,
                            settings = state.settings,
                            onChange = viewModel::changeSettings,
                            onPreview = viewModel::previewSettings,
                            onCommit = viewModel::commitSettings,
                            onRatio = viewModel::chooseCropRatio,
                            enabled = !state.comparing,
                            filterSource = filters,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        return viewModel
    }

    /** Hands [settings] to the stateless tests' host and waits for the redraw. */
    private fun setHeld(settings: EditSettings) {
        compose.runOnIdle { heldSettings = settings }
        compose.waitForIdle()
    }

    /** Asserts that the row of tools shows a dot on exactly the tools in [expected]. */
    private fun assertDots(expected: Set<EditTool>) {
        EditTool.entries.forEach { tool ->
            val dot = compose.onNodeWithTag(editToolDotTag(tool), useUnmergedTree = true)
            if (tool in expected) dot.assertExists("$tool should carry a dot") else dot.assertDoesNotExist()
        }
    }

    /**
     * Waits (at most 5 s of real time) until an edit has landed and none is running, and returns
     * the photo now shown. [previous] is the `edited` bitmap before the action (null for none).
     */
    private fun waitForEdit(viewModel: ToolViewModel, previous: Bitmap?): Bitmap {
        compose.waitUntil(timeoutMillis = 5_000) {
            // The processor works on a real background thread; its result comes back through the
            // main looper, which a Robolectric test must let run itself.
            shadowOf(Looper.getMainLooper()).idle()
            val state = viewModel.state.value
            state.edited != null && state.edited !== previous &&
                state.processing is ToolUiState.Processing.Idle
        }
        compose.waitForIdle()
        return checkNotNull(viewModel.state.value.edited)
    }

    /** The photo the screen shows now: the edited one, or the original before any edit. */
    private fun shown(viewModel: ToolViewModel): Bitmap {
        val state = viewModel.state.value
        return state.edited ?: (state.photo as ToolUiState.PhotoState.Ready).bitmap
    }

    /** A filter source whose every filter inverts the colours, from a generated 256 x 3 strip. */
    private fun invertingSource(): FilterLut.Source {
        val data = IntArray(256 * 3) { index -> (255 - index % 256).let { rgb(it, it, it) } }
        val strip = Bitmap.createBitmap(256, 3, Bitmap.Config.ARGB_8888)
        strip.setPixels(data, 0, 256, 0, 0, 256, 3)
        val lut = FilterLut(strip)
        return FilterLut.Source { lut }
    }

    /** The fake 120 x 80 photo: mid-grey on the left, a moderate orange on the right. */
    private fun photo(): Bitmap {
        val width = PHOTO_WIDTH
        val height = PHOTO_HEIGHT
        val data = IntArray(width * height) { index ->
            if (index % width < width / 2) rgb(128, 128, 128) else rgb(180, 120, 80)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(data, 0, width, 0, 0, width, height)
        return bitmap
    }

    /** An opaque ARGB colour from its three channels. */
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    /** Every pixel of [bitmap], row after row. */
    private fun pixels(bitmap: Bitmap): IntArray {
        val data = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return data
    }

    /** The mean luminance of [bitmap] (Rec. 601 weights), 0 to 255. */
    private fun meanLuminance(bitmap: Bitmap): Double = pixels(bitmap).map { color ->
        0.299 * ((color shr 16) and 0xFF) + 0.587 * ((color shr 8) and 0xFF) + 0.114 * (color and 0xFF)
    }.average()

    /** The mean red, green and blue of [bitmap], in that order. */
    private fun channelMeans(bitmap: Bitmap): DoubleArray {
        val data = pixels(bitmap)
        return doubleArrayOf(
            data.map { (it shr 16) and 0xFF }.average(),
            data.map { (it shr 8) and 0xFF }.average(),
            data.map { it and 0xFF }.average(),
        )
    }

    /** The made-up photo and how the finger moves in the drag test. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The fake photo's width in pixels. */
        const val PHOTO_WIDTH = 120

        /** The fake photo's height in pixels. */
        const val PHOTO_HEIGHT = 80

        /** The number of moves in one drag. */
        const val DRAG_STEPS = 5

        /** Each move's length as a fraction of the slider's width. */
        const val DRAG_STEP_FRACTION = 0.07f
    }
}
