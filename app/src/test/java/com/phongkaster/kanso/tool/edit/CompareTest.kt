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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditColourMatrix
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.ui.tool.AdjustSlider
import com.phongkaster.kanso.ui.tool.EDIT_BRIGHTNESS_TAG
import com.phongkaster.kanso.ui.tool.editAdjustChipTag
import com.phongkaster.kanso.ui.tool.EDIT_COMPARE_TAG
import com.phongkaster.kanso.ui.tool.EDIT_INTENSITY_TAG
import com.phongkaster.kanso.ui.tool.EDIT_PILL_TAG
import com.phongkaster.kanso.ui.tool.EDIT_ROTATE_LEFT_TAG
import com.phongkaster.kanso.ui.tool.LIVE_PREVIEW_TAG
import com.phongkaster.kanso.ui.tool.LivePreviewMatrix
import com.phongkaster.kanso.ui.tool.TOOL_CARD_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_UNDO_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolPanel
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.editFilterTag
import com.phongkaster.kanso.ui.tool.editRatioTag
import com.phongkaster.kanso.ui.tool.editToolTag
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
import kotlin.math.abs

/**
 * Checks on Android 15 the Edit screen's Compare switch and the pill that holds it (DoD 8, 21):
 *
 * - with an edit made, switching Compare on shows the untouched original photo, and switching it
 *   off shows the same edit again (the same settings, drawn live again, never re-rendered);
 * - while Compare is on every editing control is disabled at once: the four tool entries, Undo,
 *   and the controls of each sheet (the four colour sliders, the crop shapes, the turn buttons,
 *   the filters and their intensity), while Compare itself stays usable; the ViewModel also
 *   refuses an edit and an undo; switching Compare off enables them all again;
 * - the pill holds Undo and Compare, floats in the bottom-right quarter of the photo card, and
 *   both stay shown and reachable while a sheet is open.
 *
 * The test draws the tool screen's stateless part, `ToolContent`, for a real [ToolViewModel] on a
 * fake 120 x 80 photo, with the Edit sheet slot filled as `ToolScreen` fills it (`ToolPanel`,
 * disabled while Compare is on). The edit is real: the brightness slider pushed to its maximum and
 * let go changes the ViewModel's settings session, and the photo card draws it live
 * (`LivePreviewImage`, Run 9). What the photo shows is read from the live preview's semantics (its
 * colour matrix) and from pixels, drawing the activity's content view into a bitmap.
 *
 * The photo is decoded before the screen is drawn, so the loading indicator (which animates
 * forever) is never on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class CompareTest {

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

    /**
     * With a brightening edit made, Compare on draws the photo as it was before the edit, and
     * Compare off draws exactly the edited pixels again, from the same settings. The middle of
     * the image node is compared, clear of the pill at the card's bottom right.
     *
     * Run 9 (T-003): the edit is no longer a rendered `edited` bitmap (that stays null until Done);
     * the screen draws it live (`LivePreviewImage`, a colour matrix on the original). So the test
     * checks what is really drawn: before the edit the live preview's matrix is the identity, after
     * it the matrix is the settings' one and the pixels are brighter; while comparing there is no
     * live preview at all (the plain original is drawn) and the pixels match the unedited ones;
     * off again, the live preview returns with the same matrix and the same pixels. The unedited
     * pixels and the compared ones come from two drawing paths (the live preview with no colour
     * filter, then the plain image), so they are compared by their colours on average and at the
     * centre of each half of the photo, not bit for bit, which a half-pixel shift of the scaled
     * edge between grey and orange would break.
     */
    @Test
    fun compare_shows_the_original_and_switching_it_off_shows_the_edit_again() {
        val viewModel = showEdit(EditTool.ADJUST)
        val image = middleOf(bounds(TOOL_IMAGE_TAG))
        assertEquals("nothing edited yet, yet colours are changed", IDENTITY_MATRIX, liveMatrix())
        val originalPixels = pixelsIn(image)

        val edited = makeEdit(viewModel)
        val editedPixels = pixelsIn(image)
        assertFalse("the edit did not change the shown photo", originalPixels.contentEquals(editedPixels))

        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertTrue(viewModel.state.value.comparing)
        compose.onNodeWithTag(EDIT_COMPARE_TAG).assertIsOn()
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).assertDoesNotExist()
        assertLooksLike("Compare on must show the original", originalPixels, pixelsIn(image), image)
        assertEquals("comparing must keep the edit", edited, viewModel.state.value.settings)
        assertNull("comparing must not render the edit", viewModel.state.value.edited)

        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertFalse(viewModel.state.value.comparing)
        compose.onNodeWithTag(EDIT_COMPARE_TAG).assertIsOff()
        assertEquals("Compare off must bring back the same edit", edited, viewModel.state.value.settings)
        assertEquals("Compare off must draw the edit live again", EditColourMatrix.of(edited).toList(), liveMatrix())
        assertArrayEquals("Compare off must show the edit again", editedPixels, pixelsIn(image))
    }

    /**
     * While Compare is on, every editing control is disabled at once and Compare stays enabled:
     * Adjust's chips and its slider, the four tool entries (a tap on one opens nothing) and Undo; the
     * controls of the other three sheets too (each opened through the ViewModel, since the row is
     * disabled). The ViewModel refuses an edit and an undo meanwhile. Switching Compare off
     * enables the sliders, the entries and Undo again.
     *
     * Run 9 (T-003): the edit is the session's settings drawn live, not a rendered `edited`
     * bitmap (null until Done), so "the edit is kept" is checked on the settings, and the photo
     * through the live preview: absent while comparing (the plain original is drawn), back with
     * the edit's colour matrix once Compare is off.
     */
    @Test
    fun compare_disables_every_editing_control_and_switching_it_off_enables_them() {
        val viewModel = showEdit(EditTool.ADJUST)
        val edited = makeEdit(viewModel)
        assertEditingEnabled(true)

        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertTrue(viewModel.state.value.comparing)
        assertEditingEnabled(false)
        compose.onNodeWithTag(EDIT_COMPARE_TAG).assertIsEnabled()
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).assertDoesNotExist()

        // A tap on a disabled entry changes nothing.
        compose.onNodeWithTag(editToolTag(EditTool.CROP)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.ADJUST, viewModel.state.value.openTool)

        // Every other sheet's controls are disabled too.
        mapOf(
            EditTool.CROP to listOf(editRatioTag(CropRatio.SQUARE)),
            EditTool.ROTATE to listOf(EDIT_ROTATE_LEFT_TAG),
            EditTool.FILTERS to listOf(editFilterTag(FilterPreset.KELVIN), EDIT_INTENSITY_TAG),
        ).forEach { (tool, tags) ->
            viewModel.openTool(tool)
            compose.waitForIdle()
            assertEquals(tool, viewModel.state.value.openTool)
            tags.forEach { tag -> compose.onNodeWithTag(tag).assertIsNotEnabled() }
        }
        viewModel.openTool(EditTool.ADJUST)
        compose.waitForIdle()

        // The ViewModel refuses an edit and an undo while comparing.
        // Run 9: Edit's changes go through the settings session; Compare must refuse them.
        viewModel.changeSettings(EditSettings(brightness = EditSettings.MIN)) { EditProcessor(it, FilterLut.Source { null }) }
        viewModel.undo()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        val during = viewModel.state.value
        assertEquals("an edit or an undo went through while comparing", edited, during.settings)
        assertNull("comparing rendered the photo", during.edited)
        assertTrue(during.canUndo)
        assertTrue(during.processing is ToolUiState.Processing.Idle)
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).assertDoesNotExist()

        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertFalse(viewModel.state.value.comparing)
        assertEditingEnabled(true)
        assertEquals(edited, viewModel.state.value.settings)
        assertEquals("the live preview did not come back", EditColourMatrix.of(edited).toList(), liveMatrix())
    }

    /**
     * The pill holds both Undo and Compare and floats in the bottom-right quarter of the photo
     * card, near its corner, with no sheet open and with each sheet open; Undo is the screen's
     * only Undo. With a sheet open, a real tap on Compare reaches it and switches it on.
     */
    @Test
    fun the_pill_holds_undo_and_compare_at_the_photos_bottom_right() {
        val viewModel = showEdit(null)
        (listOf<EditTool?>(null) + EditTool.entries).forEach { open ->
            if (open == null) viewModel.closeTool() else if (viewModel.state.value.openTool != open) viewModel.openTool(open)
            compose.waitForIdle()
            assertEquals(open, viewModel.state.value.openTool)

            compose.onAllNodesWithTag(TOOL_UNDO_TAG).assertCountEquals(1)
            compose.onNode(hasTestTag(TOOL_UNDO_TAG) and hasAnyAncestor(hasTestTag(EDIT_PILL_TAG))).assertIsDisplayed()
            compose.onNode(hasTestTag(EDIT_COMPARE_TAG) and hasAnyAncestor(hasTestTag(EDIT_PILL_TAG))).assertIsDisplayed()

            val card = bounds(TOOL_CARD_TAG)
            val pill = bounds(EDIT_PILL_TAG)
            assertTrue("pill $pill not in the right half of $card with $open open", pill.left >= card.center.x)
            assertTrue("pill $pill not in the lower half of $card with $open open", pill.top >= card.center.y)
            assertTrue("pill $pill sticks out of $card with $open open", pill.right <= card.right && pill.bottom <= card.bottom)
            assertTrue("pill $pill far from the right edge with $open open", card.right - pill.right <= dp(24f))
            assertTrue("pill $pill far from the bottom edge with $open open", card.bottom - pill.bottom <= dp(24f))
        }

        viewModel.openTool(EditTool.ADJUST)
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertTrue("a tap on Compare did not reach it with a sheet open", viewModel.state.value.comparing)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the Edit screen's ViewModel on the fake photo, decodes the photo, opens [open]'s
     * sheet (none for null), and draws `ToolContent` with the Edit sheet slot filled as
     * `ToolScreen` fills it: the panel is disabled while Compare is on or an edit runs. Returns
     * the ViewModel.
     */
    private fun showEdit(open: EditTool?): ToolViewModel {
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
        if (open != null) viewModel.openTool(open)
        assertEquals(open, viewModel.state.value.openTool)
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
                        ToolPanel(
                            tool = Tool.EDIT,
                            state = state,
                            openTool = tool,
                            // Run 9: wired as ToolScreen wires it - every change goes to the settings session.
                            enabled = !state.comparing,
                            onEditChange = viewModel::changeSettings,
                            onEditPreview = viewModel::previewSettings,
                            onEditCommit = viewModel::commitSettings,
                            onEditRatio = viewModel::chooseCropRatio,
                        )
                    },
                )
            }
        }
        compose.waitForIdle()
        return viewModel
    }

    /**
     * Makes one real edit through the Adjust sheet (brightness pushed to its maximum and let go)
     * and returns the settings it left.
     *
     * Run 9 (T-003): it used to wait for a rendered `edited` bitmap, which the Edit screen no
     * longer makes before Done. A colour slider is drawn live, at once, so this checks instead
     * that the settings hold the edit as one Undo step, that nothing was rendered, and that the
     * live preview draws the settings' colour matrix.
     */
    private fun makeEdit(viewModel: ToolViewModel): EditSettings {
        compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(EditSettings.MAX) }
        // Run 9: the semantics action only moves the slider (a preview); letting go is the commit
        // that makes it one Undo step.
        viewModel.commitSettings()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        val state = viewModel.state.value
        assertEquals(EditSettings.MAX, state.settings.brightness, 0.0001f)
        assertTrue(state.canUndo)
        assertNull("a colour slider rendered the photo", state.edited)
        assertTrue(state.processing is ToolUiState.Processing.Idle)
        assertEquals(EditColourMatrix.of(state.settings).toList(), liveMatrix())
        return state.settings
    }

    /** The colour matrix the live preview now draws with ([LivePreviewMatrix]), as 20 numbers. */
    private fun liveMatrix(): List<Float> =
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).fetchSemanticsNode().config[LivePreviewMatrix]

    /**
     * Fails with [message] unless [actual] looks like [expected], both the pixels of [area] row
     * after row: their mean red, green and blue each within [MEAN_TOLERANCE], and the pixels at
     * the centre of the area's left and right quarters (well inside the grey and the orange half)
     * each channel within [POINT_TOLERANCE].
     */
    private fun assertLooksLike(message: String, expected: IntArray, actual: IntArray, area: Rect) {
        assertEquals(message, expected.size, actual.size)
        val expectedMeans = channelMeans(expected)
        val actualMeans = channelMeans(actual)
        (0..2).forEach { channel ->
            assertEquals("$message (mean of channel $channel)", expectedMeans[channel], actualMeans[channel], MEAN_TOLERANCE)
        }
        val width = (area.right.toInt() - area.left.toInt()).coerceAtLeast(1)
        val row = actual.size / width / 2
        listOf(width / 4, width * 3 / 4).forEach { column ->
            val index = row * width + column
            listOf(16, 8, 0).forEach { shift ->
                val want = (expected[index] shr shift) and 0xFF
                val got = (actual[index] shr shift) and 0xFF
                assertTrue("$message (pixel $column,$row: $want vs $got)", abs(want - got) <= POINT_TOLERANCE)
            }
        }
    }

    /** The mean red, green and blue of [pixels], in that order. */
    private fun channelMeans(pixels: IntArray): DoubleArray = doubleArrayOf(
        pixels.map { (it shr 16) and 0xFF }.average(),
        pixels.map { (it shr 8) and 0xFF }.average(),
        pixels.map { it and 0xFF }.average(),
    )

    /**
     * Fails unless the Adjust sheet's four sub-tool chips and its one slider (Brightness, the
     * chip chosen at the start), the four tool entries and Undo are all enabled ([enabled] true)
     * or all disabled ([enabled] false).
     */
    private fun assertEditingEnabled(enabled: Boolean) {
        val tags = listOf(EDIT_BRIGHTNESS_TAG, TOOL_UNDO_TAG) +
            AdjustSlider.entries.map(::editAdjustChipTag) +
            EditTool.entries.map(::editToolTag)
        tags.forEach { tag ->
            val node = compose.onNodeWithTag(tag)
            if (enabled) node.assertIsEnabled() else node.assertIsNotEnabled()
        }
    }

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

    /** [value] dp in this screen's pixels. */
    private fun dp(value: Float): Float = with(compose.density) { value.dp.toPx() }

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

    /** The fake 120 x 80 photo: mid-grey on the left, a moderate orange on the right. */
    private fun photo(): Bitmap {
        val width = 120
        val height = 80
        val data = IntArray(width * height) { index ->
            if (index % width < width / 2) rgb(128, 128, 128) else rgb(180, 120, 80)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(data, 0, width, 0, 0, width, height)
        return bitmap
    }

    /** An opaque ARGB colour from its three channels. */
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    /** The made-up Uri of the fake photo, and the numbers the pixel and matrix checks use. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /**
         * The identity colour matrix's 20 numbers (ones at 0, 6, 12 and 18): what the live
         * preview reports while no colour slider has moved.
         */
        val IDENTITY_MATRIX = List(20) { index -> if (index % 6 == 0) 1f else 0f }

        /** How far apart two means of a channel (0 to 255) may be and still look alike. */
        const val MEAN_TOLERANCE = 1.5

        /** How far apart one channel (0 to 255) of two sampled pixels may be and still look alike. */
        const val POINT_TOLERANCE = 3
    }
}
