package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.ui.tool.components.EDIT_CROPPER_TAG
import com.phongkaster.kanso.ui.tool.components.EditCropper
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EditPanel
import com.phongkaster.kanso.ui.tool.components.photocard.LIVE_PREVIEW_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.editRatioTag
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

/**
 * Checks on Android 15 the Edit screen's cropper (DoD 13, Run 9 T-004): while the Crop sheet is
 * open, the photo card is the cropper on the original photo, with handles and a rectangle that
 * can be dragged, instead of the plain live preview.
 *
 * Most tests draw the tool screen's stateless part, `ToolContent`, for a real [ToolViewModel] on a
 * fake 120 x 80 photo, wired as `ToolScreen` wires it: the Edit panel in the sheet slot and the
 * real [EditCropper] in the cropper slot, whose finished gesture goes to the ViewModel's
 * `commitCrop`. The tests check that:
 *
 * - the cropper replaces the live preview only while Crop is open and Compare is off;
 * - a ratio chip sets a crop rectangle of that very shape, not only a lock;
 * - `commitCrop` is one undo action, and is ignored for an equal rectangle or while comparing;
 * - the cropper follows Undo (a tap on it after Undo reports nothing new);
 * - a drag of the rectangle is at most one undo action, and a switched-off cropper reports none.
 *
 * The cropper's own rectangle (`CropState.region`) is private to [EditCropper], so where a test
 * needs to know what the cropper shows it taps the cropper: a lifted finger reports the shown
 * rectangle whenever it differs from the session's crop, so "nothing reported" means "the cropper
 * shows the session's crop".
 *
 * The photo is decoded before the screen is drawn, so the loading indicator (which animates
 * forever) is never on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EditCropTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** A filter source whose every filter inverts the colours, so nothing reads the app's assets. */
    private val filters: FilterLut.Source = invertingSource()

    /** Builds the processor drawing a set of settings, as the screen's cropper slot does. */
    private val processorFor: (EditSettings) -> ToolProcessor = { wanted -> EditProcessor(wanted, filters) }

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * DoD 13 (a): with Crop open the photo card is the cropper and the plain live preview is
     * gone; with another tool open, or with Compare on, the cropper is gone again.
     */
    @Test
    fun the_cropper_replaces_the_photo_only_while_crop_is_open_and_compare_is_off() {
        val viewModel = show(EditTool.CROP)
        compose.onNodeWithTag(EDIT_CROPPER_TAG, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).assertDoesNotExist()

        compose.runOnIdle { viewModel.openTool(EditTool.ADJUST) }
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_CROPPER_TAG, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).assertExists()

        compose.runOnIdle { viewModel.openTool(EditTool.CROP) }
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_CROPPER_TAG, useUnmergedTree = true).assertExists()

        compose.runOnIdle { viewModel.setComparing(true) }
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_CROPPER_TAG, useUnmergedTree = true).assertDoesNotExist()

        compose.runOnIdle { viewModel.setComparing(false) }
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_CROPPER_TAG, useUnmergedTree = true).assertExists()
    }

    /**
     * DoD 13 (b): the 1:1 chip on the 3:2 photo chooses the square shape AND sets a square crop
     * rectangle (in the photo's pixels), the largest one: the photo's full height.
     */
    @Test
    fun the_square_chip_sets_a_square_crop_rectangle() {
        val viewModel = show(EditTool.CROP)
        compose.onNodeWithTag(editRatioTag(CropRatio.SQUARE)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        val settings = viewModel.state.value.settings
        assertEquals(CropRatio.SQUARE, settings.cropRatio)
        assertEquals("the crop is not square: ${settings.crop}", 1f, pixelAspect(settings.crop), ASPECT_TOLERANCE)
        assertEquals("the square is not the largest one", 1f, settings.crop.bottom - settings.crop.top, EDGE_TOLERANCE)
    }

    /**
     * DoD 13 (b): the 16:9 chip on the 3:2 photo chooses 16:9 AND sets a 16:9 crop rectangle (in
     * the photo's pixels), the largest one: the photo's full width.
     */
    @Test
    fun the_sixteen_nine_chip_sets_a_sixteen_nine_crop_rectangle() {
        val viewModel = show(EditTool.CROP)
        compose.onNodeWithTag(editRatioTag(CropRatio.SIXTEEN_NINE)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        val settings = viewModel.state.value.settings
        assertEquals(CropRatio.SIXTEEN_NINE, settings.cropRatio)
        assertEquals("the crop is not 16:9: ${settings.crop}", 16f / 9f, pixelAspect(settings.crop), ASPECT_TOLERANCE)
        assertEquals("the rectangle is not the largest one", 1f, settings.crop.right - settings.crop.left, EDGE_TOLERANCE)
    }

    /**
     * DoD 13 (c): a finished crop (`commitCrop`) is exactly one undo action, and Undo restores the
     * crop before it exactly.
     */
    @Test
    fun commit_crop_is_one_undo_action_and_undo_restores_the_previous_crop() {
        val viewModel = show(EditTool.CROP)
        val before = viewModel.state.value.settings.crop
        assertFalse(viewModel.state.value.canUndo)

        compose.runOnIdle { viewModel.commitCrop(SOME_CROP, processorFor) }
        compose.waitForIdle()
        assertEquals(SOME_CROP, viewModel.state.value.settings.crop)
        assertTrue("a finished crop left no undo step", viewModel.state.value.canUndo)

        compose.runOnIdle { viewModel.undo() }
        compose.waitForIdle()
        assertEquals("Undo did not restore the crop exactly", before, viewModel.state.value.settings.crop)
        assertFalse("one crop made more than one undo step", viewModel.state.value.canUndo)
    }

    /**
     * DoD 13 (c): handing `commitCrop` the crop the session already holds is no action: it adds
     * no undo entry.
     */
    @Test
    fun commit_crop_of_an_equal_rectangle_adds_no_undo_entry() {
        val viewModel = show(EditTool.CROP)
        compose.runOnIdle {
            viewModel.commitCrop(SOME_CROP, processorFor)
            viewModel.commitCrop(SOME_CROP, processorFor)
        }
        compose.waitForIdle()
        assertEquals("the same rectangle twice made a second undo step", 1, undoEntries(viewModel))
    }

    /** DoD 13 (c): `commitCrop` while Compare is on changes nothing and adds no undo entry. */
    @Test
    fun commit_crop_while_comparing_does_nothing() {
        val viewModel = show(EditTool.CROP)
        compose.runOnIdle {
            viewModel.setComparing(true)
            viewModel.commitCrop(SOME_CROP, processorFor)
        }
        compose.waitForIdle()
        assertEquals(EditSettings.CropRect.FULL, viewModel.state.value.settings.crop)
        assertFalse(viewModel.state.value.canUndo)
        compose.runOnIdle { viewModel.setComparing(false) }
        compose.waitForIdle()
        assertFalse("a crop made while comparing appeared afterwards", viewModel.state.value.canUndo)
    }

    /**
     * DoD 13 (d): the cropper follows the session. After the 1:1 chip, a finished crop and an
     * Undo, the cropper must show the square again: a tap on it (a finger put down and lifted
     * without moving) then reports nothing, so the crop stays the square and only the chip's undo
     * entry is left. A cropper still showing the undone rectangle (or the whole photo) would
     * report it as a new crop.
     */
    @Test
    fun the_cropper_follows_undo() {
        val viewModel = show(EditTool.CROP)
        compose.onNodeWithTag(editRatioTag(CropRatio.SQUARE)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val square = viewModel.state.value.settings.crop

        // A tap right after the chip: the cropper already shows the square, so nothing is reported.
        tapCropper()
        assertEquals("the cropper did not show the chip's square", square, viewModel.state.value.settings.crop)

        compose.runOnIdle { viewModel.commitCrop(SOME_CROP, processorFor) }
        compose.waitForIdle()
        compose.runOnIdle { viewModel.undo() }
        compose.waitForIdle()
        assertEquals(square, viewModel.state.value.settings.crop)

        tapCropper()
        assertEquals("the cropper did not follow Undo", square, viewModel.state.value.settings.crop)
        assertEquals("a tap on the cropper made an undo step", 1, undoEntries(viewModel))
    }

    /**
     * DoD 13 (e): dragging the rectangle on the screen's cropper moves the crop, and the whole
     * drag is exactly one undo entry: one Undo brings back the square the chip set, and the
     * chip's own entry is the only one left.
     */
    @Test
    fun a_drag_on_the_cropper_is_one_undo_action() {
        val viewModel = show(EditTool.CROP)
        compose.onNodeWithTag(editRatioTag(CropRatio.SQUARE)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val square = viewModel.state.value.settings.crop

        dragCropperLeft()
        val dragged = viewModel.state.value.settings.crop
        assertNotEquals("the drag did not move the crop", square, dragged)
        assertEquals("the drag changed the crop's shape", pixelAspect(square), pixelAspect(dragged), ASPECT_TOLERANCE)
        assertTrue("the drag did not move the crop to the left: $dragged", dragged.left < square.left)

        compose.runOnIdle { viewModel.undo() }
        compose.waitForIdle()
        assertEquals("one Undo did not take the whole drag back", square, viewModel.state.value.settings.crop)
        assertEquals("the drag made more than one undo step", 1, undoEntries(viewModel))
    }

    /**
     * DoD 13 (e), the cropper alone: one drag inside the rectangle reports exactly one finished
     * crop, moved to the left and of the same size; with the cropper switched off (Compare on) the
     * same drag reports none.
     */
    @Test
    fun the_cropper_reports_one_crop_per_drag_and_none_while_switched_off() {
        val reported = showCropperAlone(enabled = true)
        dragCropperLeft()
        assertEquals("one drag reported ${reported.size} crops", 1, reported.size)
        val moved = reported.single()
        assertTrue("the drag did not move the rectangle left: $moved", moved.left < SQUARE_CROP.left)
        assertEquals(SQUARE_CROP.right - SQUARE_CROP.left, moved.right - moved.left, EDGE_TOLERANCE)
        assertEquals(SQUARE_CROP.bottom - SQUARE_CROP.top, moved.bottom - moved.top, EDGE_TOLERANCE)
    }

    /** DoD 13 (e): a switched-off cropper (Compare on) reports nothing for a drag. */
    @Test
    fun a_switched_off_cropper_reports_no_crop() {
        val reported = showCropperAlone(enabled = false)
        dragCropperLeft()
        assertTrue("a switched-off cropper reported $reported", reported.isEmpty())
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the Edit screen's ViewModel on the fake photo, decodes the photo, opens [open]'s
     * sheet, and draws the screen's stateless part wired as `ToolScreen` wires it: the Edit panel
     * in the sheet slot and the real cropper in the cropper slot. Returns the ViewModel.
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
                    editCropper = {
                        (state.photo as? ToolUiState.PhotoState.Ready)?.let { ready ->
                            EditCropper(
                                bitmap = ready.bitmap,
                                settings = state.settings,
                                enabled = !state.comparing,
                                onCropFinished = { rect -> viewModel.commitCrop(rect, processorFor) },
                            )
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        return viewModel
    }

    /**
     * Draws the cropper alone, filling the screen, on the fake photo with the centred square crop
     * chosen, [enabled] or not, and returns the list every reported crop is added to.
     */
    private fun showCropperAlone(enabled: Boolean): MutableList<EditSettings.CropRect> {
        val reported = mutableListOf<EditSettings.CropRect>()
        val bitmap = photo()
        val settings = EditSettings(cropRatio = CropRatio.SQUARE, crop = SQUARE_CROP)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    EditCropper(
                        bitmap = bitmap,
                        settings = settings,
                        enabled = enabled,
                        onCropFinished = { rect -> reported += rect },
                    )
                }
            }
        }
        compose.waitForIdle()
        return reported
    }

    /** Puts a finger down in the middle of the cropper and lifts it without moving, then settles. */
    private fun tapCropper() {
        compose.onNodeWithTag(EDIT_CROPPER_TAG).performTouchInput {
            down(center)
            up()
        }
        compose.waitForIdle()
    }

    /**
     * Drags from the middle of the cropper (inside the crop rectangle, which the cropper keeps
     * centred in view) an eighth of its width to the left, then settles.
     */
    private fun dragCropperLeft() {
        compose.onNodeWithTag(EDIT_CROPPER_TAG).performTouchInput {
            swipe(start = center, end = center - Offset(width / 8f, 0f), durationMillis = DRAG_MILLIS)
        }
        compose.waitForIdle()
    }

    /**
     * Counts the undo entries the session holds by taking them all back, one at a time (so call
     * it last). Stops after a hundred, so a broken Undo fails the count instead of looping.
     */
    private fun undoEntries(viewModel: ToolViewModel): Int {
        var count = 0
        while (viewModel.state.value.canUndo && count < 100) {
            compose.runOnIdle { viewModel.undo() }
            compose.waitForIdle()
            count++
        }
        return count
    }

    /** The shape (width over height) of [crop] in the fake photo's pixels. */
    private fun pixelAspect(crop: EditSettings.CropRect): Float =
        ((crop.right - crop.left) * PHOTO_WIDTH) / ((crop.bottom - crop.top) * PHOTO_HEIGHT)

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
        val data = IntArray(PHOTO_WIDTH * PHOTO_HEIGHT) { index ->
            if (index % PHOTO_WIDTH < PHOTO_WIDTH / 2) rgb(128, 128, 128) else rgb(180, 120, 80)
        }
        val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(data, 0, PHOTO_WIDTH, 0, 0, PHOTO_WIDTH, PHOTO_HEIGHT)
        return bitmap
    }

    /** An opaque ARGB colour from its three channels. */
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    /** The made-up photo, the crops the tests use, the drag and the tolerances. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The fake photo's width in pixels. */
        const val PHOTO_WIDTH = 120

        /** The fake photo's height in pixels. */
        const val PHOTO_HEIGHT = 80

        /** How far a crop's pixel shape may be from the wanted one (rounding to whole pixels). */
        const val ASPECT_TOLERANCE = 0.02f

        /** How far a crop edge or size, as a fraction of the photo, may be from the wanted one. */
        const val EDGE_TOLERANCE = 0.01f

        /** How long the test's drag lasts, in milliseconds: long enough for several moves. */
        const val DRAG_MILLIS = 400L

        /** A free crop rectangle the tests hand to `commitCrop`. */
        val SOME_CROP = EditSettings.CropRect(0.1f, 0.2f, 0.8f, 0.9f)

        /** The largest centred square of the 120 x 80 photo: 80 x 80 from x = 20. */
        val SQUARE_CROP = EditSettings.CropRect(20f / 120f, 0f, 100f / 120f, 1f)
    }
}
