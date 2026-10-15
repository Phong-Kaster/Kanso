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
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.NoSavePhotoSaver
import com.phongkaster.kanso.ui.tool.EDIT_BRIGHTNESS_TAG
import com.phongkaster.kanso.ui.tool.EDIT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.EDIT_ROTATE_LEFT_TAG
import com.phongkaster.kanso.ui.tool.EDIT_SHEET_TAG
import com.phongkaster.kanso.ui.tool.EDIT_TOOL_ROW_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolPanel
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.editFilterTag
import com.phongkaster.kanso.ui.tool.editRatioTag
import com.phongkaster.kanso.ui.tool.editToolTag
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
 * Checks on Android 15 the Edit screen's row of tools under the photo (DoD 2):
 *
 * - it holds exactly four entries, Crop, Rotate, Adjust and Filters, left to right, each an icon
 *   drawn above its name;
 * - none is open at first and no sheet is shown;
 * - tapping an entry opens that tool's sheet (the Edit panel inside the sheet area, with the
 *   tool's own controls), marks the entry selected and highlights it; tapping it again closes the
 *   sheet; tapping another entry switches to that tool's sheet.
 *
 * The test draws the tool screen's stateless part, `ToolContent`, for a real [ToolViewModel] on a
 * fake 120 x 80 photo, with the Edit sheet slot filled as `ToolScreen` fills it (`ToolPanel`), so
 * a tap goes through the real row, the real ViewModel and the real panel. Positions are read with
 * `positionInRoot`; pixels by drawing the activity's content view into a bitmap.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EditToolRowTest {

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
     * The row holds four entries, Crop, Rotate, Adjust and Filters, in that order from left to
     * right, each named by its text and with its icon drawn above the name: the name starts at
     * least an icon's height (24 dp) below the entry's top, and the band above the name holds
     * drawn ink.
     */
    @Test
    fun the_row_shows_four_tools_in_order_each_an_icon_over_its_name() {
        showEdit()
        compose.onAllNodes(isRowEntry()).assertCountEquals(EditTool.entries.size)
        assertEquals(listOf(EditTool.CROP, EditTool.ROTATE, EditTool.ADJUST, EditTool.FILTERS), EditTool.entries)

        val lefts = EditTool.entries.map { tool ->
            val name = context.getString(nameOf(tool))
            compose.onNodeWithTag(editToolTag(tool)).assert(hasText(name))
            val entry = bounds(compose.onNodeWithTag(editToolTag(tool)).fetchSemanticsNode())
            val text = bounds(
                compose.onNode(hasText(name) and hasAnyAncestor(hasTestTag(editToolTag(tool))), useUnmergedTree = true)
                    .fetchSemanticsNode(),
            )
            // The name sits in the entry, under the 24 dp icon.
            assertTrue("$tool's name is not inside its entry", text.top >= entry.top && text.bottom <= entry.bottom + 0.5f)
            assertTrue("$tool's name starts ${text.top - entry.top} px below the entry's top", text.top - entry.top >= dp(24f))
            // The icon is drawn in the band above the name.
            val band = Rect(entry.left, entry.top + dp(4f), entry.right, text.top - dp(1f))
            val reference = pixelAt(entry.center.x, entry.top + dp(2f))
            val ink = pixelsIn(band).count { colourDistance(it, reference) > 60 }
            assertTrue("no icon drawn above $tool's name ($ink pixels)", ink > 40)
            entry.left
        }
        assertEquals("entries left to right", lefts.sorted(), lefts)
        assertEquals("two entries share a place", lefts.size, lefts.toSet().size)
    }

    /** At first no entry is selected and no sheet is shown under the row. */
    @Test
    fun no_tool_is_open_at_first() {
        val viewModel = showEdit()
        assertNull(viewModel.state.value.openTool)
        EditTool.entries.forEach { tool -> compose.onNodeWithTag(editToolTag(tool)).assertIsNotSelected() }
        compose.onAllNodesWithTag(EDIT_PANEL_TAG).assertCountEquals(0)
    }

    /**
     * Tapping each entry opens its sheet, with the Edit panel inside the sheet area and that
     * tool's own control (a crop shape for Crop, turn left for Rotate, brightness for Adjust, a
     * filter for Filters); the entry is selected and highlighted, the others are not. Tapping it
     * again closes the sheet and clears the selection and the highlight.
     */
    @Test
    fun tapping_a_tool_opens_its_sheet_and_tapping_it_again_closes_it() {
        val viewModel = showEdit()
        EditTool.entries.forEach { tool ->
            val entry = bounds(compose.onNodeWithTag(editToolTag(tool)).fetchSemanticsNode())
            // A point in the entry's background, above its icon.
            val highlightX = entry.center.x
            val highlightY = entry.top + dp(2f)
            val closedColour = pixelAt(highlightX, highlightY)

            compose.onNodeWithTag(editToolTag(tool)).performClick()
            compose.waitForIdle()
            assertEquals(tool, viewModel.state.value.openTool)
            compose.onAllNodes(hasTestTag(EDIT_PANEL_TAG) and hasAnyAncestor(hasTestTag(EDIT_SHEET_TAG)))
                .assertCountEquals(1)
            compose.onAllNodes(hasTestTag(controlTagOf(tool)) and hasAnyAncestor(hasTestTag(EDIT_SHEET_TAG)))
                .assertCountEquals(1)
            EditTool.entries.forEach { other ->
                val node = compose.onNodeWithTag(editToolTag(other))
                if (other == tool) node.assertIsSelected() else node.assertIsNotSelected()
            }
            assertNotEquals("$tool's entry is not highlighted", closedColour, pixelAt(highlightX, highlightY))

            compose.onNodeWithTag(editToolTag(tool)).performClick()
            compose.waitForIdle()
            assertNull(viewModel.state.value.openTool)
            compose.onAllNodesWithTag(EDIT_PANEL_TAG).assertCountEquals(0)
            compose.onNodeWithTag(editToolTag(tool)).assertIsNotSelected()
            assertEquals("$tool's highlight stayed after closing", closedColour, pixelAt(highlightX, highlightY))
        }
    }

    /**
     * With Crop open, tapping Adjust switches the sheet to Adjust's: Adjust is selected, Crop is
     * not, the brightness slider is shown and Crop's shape chips are gone.
     */
    @Test
    fun tapping_another_tool_switches_the_sheet() {
        val viewModel = showEdit()
        compose.onNodeWithTag(editToolTag(EditTool.CROP)).performClick()
        compose.waitForIdle()
        compose.onAllNodesWithTag(controlTagOf(EditTool.CROP)).assertCountEquals(1)

        compose.onNodeWithTag(editToolTag(EditTool.ADJUST)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.ADJUST, viewModel.state.value.openTool)
        compose.onNodeWithTag(editToolTag(EditTool.ADJUST)).assertIsSelected()
        compose.onNodeWithTag(editToolTag(EditTool.CROP)).assertIsNotSelected()
        compose.onAllNodesWithTag(EDIT_BRIGHTNESS_TAG).assertCountEquals(1)
        compose.onAllNodesWithTag(controlTagOf(EditTool.CROP)).assertCountEquals(0)
        compose.onAllNodesWithTag(EDIT_PANEL_TAG).assertCountEquals(1)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the Edit screen's ViewModel on the fake photo, decodes the photo, and draws
     * `ToolContent` with the Edit sheet slot filled as `ToolScreen` fills it. Returns the
     * ViewModel; no sheet is open yet.
     */
    private fun showEdit(): ToolViewModel {
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
        compose.setContent {
            MaterialTheme {
                // collectAsState redraws the screen whenever the ViewModel's state changes.
                val state by viewModel.state.collectAsState()
                ToolContent(
                    state = state,
                    onBack = {},
                    onSave = {},
                    onUndo = viewModel::undo,
                    onOpenTool = viewModel::openTool,
                    onCompare = viewModel::setComparing,
                    editSheet = { tool ->
                        ToolPanel(
                            tool = Tool.EDIT,
                            state = state,
                            openTool = tool,
                            enabled = !state.comparing && state.processing is ToolUiState.Processing.Idle,
                            // Run 9: Edit's controls no longer take onApply (settings session instead).
                        )
                    },
                )
            }
        }
        compose.waitForIdle()
        return viewModel
    }

    /** Matches the row's entries: a node whose test tag is an entry's, not the row's own. */
    private fun isRowEntry(): SemanticsMatcher = SemanticsMatcher("an entry of the row of tools") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag != null && tag != EDIT_TOOL_ROW_TAG && EditTool.entries.any { editToolTag(it) == tag }
    }

    /** The string resource of [tool]'s name under its icon. */
    private fun nameOf(tool: EditTool): Int = when (tool) {
        EditTool.CROP -> R.string.edit_layout_crop
        EditTool.ROTATE -> R.string.edit_layout_rotate
        EditTool.ADJUST -> R.string.edit_layout_adjust
        EditTool.FILTERS -> R.string.edit_layout_filters
    }

    /** The test tag of one control only [tool]'s sheet shows. */
    private fun controlTagOf(tool: EditTool): String = when (tool) {
        EditTool.CROP -> editRatioTag(CropRatio.SQUARE)
        EditTool.ROTATE -> EDIT_ROTATE_LEFT_TAG
        EditTool.ADJUST -> EDIT_BRIGHTNESS_TAG
        EditTool.FILTERS -> editFilterTag(FilterPreset.KELVIN)
    }

    /** The bounds of [node] in the root's pixels, from its `positionInRoot` and its size. */
    private fun bounds(node: SemanticsNode): Rect {
        val position = node.positionInRoot
        return Rect(position.x, position.y, position.x + node.size.width, position.y + node.size.height)
    }

    /** [value] dp in this screen's pixels. */
    private fun dp(value: Float): Float = with(compose.density) { value.dp.toPx() }

    /**
     * Draws the activity's content view into a fresh bitmap and hands it to [read], then frees it.
     *
     * It draws the content view itself instead of using Compose's captureToImage, whose wait for
     * the window to redraw times out under Robolectric here (see `settings/ScreenPixels.kt`). The
     * Compose root fills the content view from its top-left corner, so the two share coordinates.
     */
    private fun <T> drawn(read: (Bitmap) -> T): T {
        compose.waitForIdle()
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = checkNotNull(content.getChildAt(0)) { "The activity has no content view yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return try {
            read(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /** The colour drawn at ([x], [y]) in the root's pixels. */
    private fun pixelAt(x: Float, y: Float): Int = drawn { bitmap ->
        bitmap.getPixel(x.toInt().coerceIn(0, bitmap.width - 1), y.toInt().coerceIn(0, bitmap.height - 1))
    }

    /** The pixels drawn inside [area] (in the root's pixels), row after row. */
    private fun pixelsIn(area: Rect): IntArray = drawn { bitmap ->
        val left = area.left.toInt().coerceIn(0, bitmap.width - 1)
        val top = area.top.toInt().coerceIn(0, bitmap.height - 1)
        val right = area.right.toInt().coerceIn(left + 1, bitmap.width)
        val bottom = area.bottom.toInt().coerceIn(top + 1, bitmap.height)
        val width = right - left
        val height = bottom - top
        val data = IntArray(width * height)
        bitmap.getPixels(data, 0, width, left, top, width, height)
        data
    }

    /** The sum of the red, green and blue differences between two colours, 0 to 765. */
    private fun colourDistance(a: Int, b: Int): Int =
        abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) +
            abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) +
            abs((a and 0xFF) - (b and 0xFF))

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

    /** The made-up Uri of the fake photo. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"
    }
}
