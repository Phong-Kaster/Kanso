package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditColourMatrix
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_BRIGHTNESS_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_FLIP_HORIZONTAL_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_FLIP_VERTICAL_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_INTENSITY_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_ROTATE_LEFT_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_ROTATE_RIGHT_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EditPanel
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EditPanelContent
import com.phongkaster.kanso.ui.tool.components.editbottombar.edittoolrow.EditToolRow
import com.phongkaster.kanso.ui.tool.components.photocard.LIVE_PREVIEW_TAG
import com.phongkaster.kanso.ui.tool.components.photocard.LivePreviewGeometry
import com.phongkaster.kanso.ui.tool.components.photocard.LivePreviewMatrix
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.editFilterTag
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.editRatioTag
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.editSliderValueTag
import com.phongkaster.kanso.ui.tool.components.editbottombar.edittoolrow.editToolDotTag
import kotlinx.coroutines.Dispatchers
import org.junit.After
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
import kotlin.math.abs

/**
 * Checks on Android 15 the Edit tool's controls and what they do to the photo (DoD 2, 5, 14, 16).
 *
 * Most tests draw the tool screen's stateless part, `ToolContent`, for a real [ToolViewModel] on a
 * fake 120 x 80 photo, with the Edit sheet slot holding the real Edit panel (`EditPanel`): the
 * panel shows the sheet of the tool opened with the ViewModel's `openTool`, draws the session's
 * settings from the ViewModel's state, and hands each change with the real Edit processor to the
 * ViewModel (`changeSettings`, `previewSettings`, `commitSettings`, `chooseCropRatio`), as the
 * screen does.
 *
 * Run 9 (T-003): the photo the Edit screen shows is no longer a rendered `edited` bitmap (that
 * stays null until Done) but the live preview drawn by the photo card (`LivePreviewImage`): the
 * colour sliders, crop, turns and flips are applied by Compose as it draws, on the original or on
 * `filterPreview`, the small copy with only the filter applied. So the tests read what is really
 * on screen: the live preview's semantics (its colour matrix and geometry), the image node's
 * bounds, and its pixels, drawn from the activity's content view into a bitmap. Within 5 s of
 * real time:
 *
 * - brightness pushed to its maximum makes the shown photo brighter on average;
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
     * Brightness at its maximum makes the shown photo brighter on average.
     *
     * Run 9: brightness lives in the Adjust sheet now, so the test opens Adjust first; the slider
     * now hands whole settings to the ViewModel's `previewSettings`/`commitSettings` instead of a
     * one-off change to the removed `apply`, and the photo is rendered from the original.
     *
     * Run 9 (T-003): no render is waited for any more (a colour slider renders nothing; it used to
     * time out waiting for `edited`). The live preview must draw the settings' colour matrix, whose
     * offsets lift red, green and blue, and the pixels drawn in the photo must be brighter.
     */
    @Test
    fun brightness_brightens_the_shown_photo() {
        val viewModel = show(EditTool.ADJUST)
        val image = middleOf(bounds(TOOL_IMAGE_TAG))
        val before = meanLuminance(pixelsIn(image))
        compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(EditSettings.MAX) }
        compose.waitForIdle()

        val state = viewModel.state.value
        assertEquals(EditSettings.MAX, state.settings.brightness, 0.0001f)
        val matrix = liveMatrix()
        assertEquals("the live preview does not draw the settings", EditColourMatrix.of(state.settings).toList(), matrix)
        // The offsets of the red, green and blue rows sit at places 4, 9 and 14.
        assertTrue("brightness does not lift every channel: $matrix", matrix[4] > 0f && matrix[9] > 0f && matrix[14] > 0f)
        val after = meanLuminance(pixelsIn(image))
        assertTrue("brightness: $before -> $after", after > before)
        assertNull("a colour slider rendered the photo", state.edited)
    }

    /**
     * The 1:1 chip makes the shown 120 x 80 photo square.
     *
     * Run 9: the crop chips live in the Crop sheet now, so the test opens Crop first; a chip now
     * goes to the ViewModel's `chooseCropRatio`, which sets the centred square crop rectangle.
     *
     * Run 9 (T-003): no render is waited for any more (it timed out waiting for `edited`). The
     * shown photo is the image node, sized to the cropped shape: it goes from 3:2 to square. The
     * crop keeps the photo's full height (as the rendered photo kept its 80 px), and the live
     * preview's geometry draws exactly the settings' crop.
     */
    @Test
    fun a_crop_ratio_changes_the_shape_of_the_shown_photo() {
        val viewModel = show(EditTool.CROP)
        val before = bounds(TOOL_IMAGE_TAG)
        assertEquals(PHOTO_WIDTH.toFloat() / PHOTO_HEIGHT, before.width / before.height, ASPECT_TOLERANCE)
        compose.onNodeWithTag(editRatioTag(CropRatio.SQUARE)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        val after = bounds(TOOL_IMAGE_TAG)
        assertEquals("the shown photo is not square: $after", 1f, after.width / after.height, ASPECT_TOLERANCE)
        val state = viewModel.state.value
        val crop = state.settings.crop
        assertEquals(0f, crop.top, 0.0001f)
        assertEquals(1f, crop.bottom, 0.0001f)
        assertEquals(
            "the live preview does not draw the settings' crop",
            "turns=0;flipH=false;flipV=false;crop=${crop.left},${crop.top},${crop.right},${crop.bottom}",
            liveGeometry(),
        )
        assertNull("a crop rendered the photo", state.edited)
    }

    /**
     * A filter chip changes the colours of the shown photo.
     *
     * Run 9: the filter chips live in the Filters sheet now, so the test opens Filters first; a
     * chip is now one action through the ViewModel's `changeSettings`.
     *
     * Run 9 (T-003): a filter is the one part still computed, on the small preview copy
     * (`filterPreview`), not as a full `edited` render (it timed out waiting for one). The test
     * waits for that preview and reads the pixels drawn in the photo.
     */
    @Test
    fun a_filter_changes_the_colours_of_the_shown_photo() {
        val viewModel = show(EditTool.FILTERS)
        val image = middleOf(bounds(TOOL_IMAGE_TAG))
        val before = channelMeans(pixelsIn(image))
        compose.onNodeWithTag(editFilterTag(FilterPreset.KELVIN)).performSemanticsAction(SemanticsActions.OnClick)
        waitForFilterPreview(viewModel, previous = null)
        val after = channelMeans(pixelsIn(image))
        val change = (0..2).maxOf { abs(after[it] - before[it]) }
        assertTrue("the filter barely changed the colours ($change)", change > 3.0)
        assertNull("a filter rendered the full photo", viewModel.state.value.edited)
    }

    /**
     * DoD 16: dragging the filter's intensity changes the shown photo while the finger is still
     * down; lifting it keeps the result and leaves an Undo step.
     *
     * Run 9 (T-003): each intensity step now asks for a new filter preview (`filterPreview`), not
     * a full `edited` render (the test timed out waiting for one); the test waits for a new
     * preview while the finger is still down and reads the pixels drawn in the photo. The slider
     * is scrolled into view first, since the sheet under the photo scrolls.
     */
    @Test
    fun the_filter_intensity_changes_the_photo_while_dragged() {
        val viewModel = show(EditTool.FILTERS)
        val image = middleOf(bounds(TOOL_IMAGE_TAG))
        compose.onNodeWithTag(editFilterTag(FilterPreset.KELVIN)).performSemanticsAction(SemanticsActions.OnClick)
        val full = waitForFilterPreview(viewModel, previous = null)
        val fullMeans = channelMeans(pixelsIn(image))

        val slider = compose.onNodeWithTag(EDIT_INTENSITY_TAG)
        slider.performScrollTo()
        slider.performTouchInput {
            // Intensity 100 has the thumb at the right end; starting in the middle and moving
            // left lowers it well below 100.
            down(center)
            repeat(DRAG_STEPS) { moveBy(Offset(-width * DRAG_STEP_FRACTION, 0f)) }
        }
        waitForFilterPreview(viewModel, previous = full)
        assertTrue(
            "the intensity did not fall while dragged",
            viewModel.state.value.settings.filterIntensity < 0.9f,
        )
        val draggedMeans = channelMeans(pixelsIn(image))
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
     * sheet, and draws the screen's stateless part with the Edit panel in its sheet slot. Returns
     * the ViewModel.
     *
     * Run 9: the panel hands whole settings to the session calls, drawn from the ViewModel's
     * settings, afresh (`key`) for each tool opened. The slider, chip, processor and ViewModel
     * are all real.
     *
     * Run 9 (T-003): the host now draws `ToolContent` (the photo card with its live preview, the
     * row of tools and the sheet), not the panel alone, so the tests can read the photo as shown;
     * the panel in the sheet slot is wired as `ToolScreen` wires it, except for the generated
     * filter source.
     */
    private fun show(open: EditTool): ToolViewModel {
        val viewModel = ToolViewModel(
            Tool.EDIT,
            PHOTO_URI,
            FakePhotoLoader(context, photo()),
            ViewModelClearedProbe(),
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
                // collectAsState redraws the screen whenever the ViewModel's state changes.
                val state by viewModel.state.collectAsState()
                ToolContent(
                    state = state,
                    onBack = {},
                    onDone = {},
                    onUndo = viewModel::undo,
                    onOpenTool = viewModel::openTool,
                    onCompare = viewModel::setComparing,
                    editSheet = { tool ->
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
                    },
                )
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
     * Waits (at most 5 s of real time) until a filter preview other than [previous] (null for
     * none) is in the state, lets the screen draw it, and returns it.
     *
     * Run 9 (T-003): replaces the wait for a full `edited` render. The preview renderer works on
     * `Dispatchers.Default` whatever dispatcher the ViewModel is given, and its result comes back
     * through the main looper, which a Robolectric test must let run itself.
     */
    private fun waitForFilterPreview(viewModel: ToolViewModel, previous: Bitmap?): Bitmap {
        compose.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            val preview = viewModel.state.value.filterPreview
            preview != null && preview !== previous
        }
        compose.waitForIdle()
        return checkNotNull(viewModel.state.value.filterPreview)
    }

    /** The colour matrix the live preview now draws with ([LivePreviewMatrix]), as 20 numbers. */
    private fun liveMatrix(): List<Float> =
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).fetchSemanticsNode().config[LivePreviewMatrix]

    /** The geometry the live preview now draws with ([LivePreviewGeometry]), as its text. */
    private fun liveGeometry(): String =
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).fetchSemanticsNode().config[LivePreviewGeometry]

    /**
     * The bounds of the node tagged [tag] in the root's pixels, from its `positionInRoot` and its
     * size (never clipped to what is visible).
     */
    private fun bounds(tag: String): Rect {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        val position = node.positionInRoot
        return Rect(position.x, position.y, position.x + node.size.width, position.y + node.size.height)
    }

    /** The middle half of [area] each way: a quarter of its width and height in from each side. */
    private fun middleOf(area: Rect): Rect = Rect(
        area.left + area.width / 4,
        area.top + area.height / 4,
        area.right - area.width / 4,
        area.bottom - area.height / 4,
    )

    /**
     * Draws the activity's content view into a fresh bitmap and returns the pixels inside [area]
     * (in the root's pixels), row after row.
     *
     * It draws the content view itself instead of using Compose's captureToImage, whose wait for
     * the window to redraw times out under Robolectric here (see `settings/ScreenPixels.kt`). The
     * Compose root fills the content view from its top-left corner, so the two share coordinates.
     */
    private fun pixelsIn(area: Rect): IntArray {
        compose.waitForIdle()
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = checkNotNull(content.getChildAt(0)) { "The activity has no content view yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return try {
            val left = area.left.toInt().coerceIn(0, bitmap.width - 1)
            val top = area.top.toInt().coerceIn(0, bitmap.height - 1)
            val right = area.right.toInt().coerceIn(left + 1, bitmap.width)
            val bottom = area.bottom.toInt().coerceIn(top + 1, bitmap.height)
            val width = right - left
            val height = bottom - top
            val data = IntArray(width * height)
            bitmap.getPixels(data, 0, width, left, top, width, height)
            data
        } finally {
            bitmap.recycle()
        }
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

    /** The mean luminance of [pixels] (Rec. 601 weights), 0 to 255. */
    private fun meanLuminance(pixels: IntArray): Double = pixels.map { color ->
        0.299 * ((color shr 16) and 0xFF) + 0.587 * ((color shr 8) and 0xFF) + 0.114 * (color and 0xFF)
    }.average()

    /** The mean red, green and blue of [pixels], in that order. */
    private fun channelMeans(pixels: IntArray): DoubleArray = doubleArrayOf(
        pixels.map { (it shr 16) and 0xFF }.average(),
        pixels.map { (it shr 8) and 0xFF }.average(),
        pixels.map { it and 0xFF }.average(),
    )

    /** The made-up photo, how the finger moves in the drag test, and the shape tolerance. */
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

        /** How far the image node's width-to-height ratio may be from the expected one (pixel rounding). */
        const val ASPECT_TOLERANCE = 0.02f
    }
}
