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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.ui.tool.EDIT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.EDIT_SHEET_TAG
import com.phongkaster.kanso.ui.tool.EDIT_TOOL_ROW_TAG
import com.phongkaster.kanso.ui.tool.TOOL_BACK_TAG
import com.phongkaster.kanso.ui.tool.TOOL_CARD_TAG
import com.phongkaster.kanso.ui.tool.TOOL_REPLACE_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolPanel
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks on Android 15 the Edit screen's layout (DoD 1, 3 and R4):
 *
 * - the photo card takes 60% of the body, the space between the top bar and the Save bar, and
 *   keeps the same place and size with no sheet open and with each of the four sheets open;
 * - the open tool's sheet fills the rest of the body under the row of tools, down to the Save bar;
 * - nothing dims the photo while a sheet is open: the card's pixels are the same as with every
 *   sheet closed, and no dialog or popup (which a modal bottom sheet would bring) is on screen;
 * - the Edit screen has no "Replace" chip, while the other four tool screens keep theirs.
 *
 * The test draws the tool screen's stateless part, `ToolContent`, for a real [ToolViewModel] on a
 * fake 120 x 80 photo, with the Edit sheet slot filled as `ToolScreen` fills it (`ToolPanel`). A
 * sheet is opened and closed through the ViewModel's own `openTool` and `closeTool`, the functions
 * the row of tools calls. Positions are read with `positionInRoot` plus the node's size, in the
 * root's pixels (3 per dp on this xxhdpi screen).
 *
 * The photo is decoded before the screen is drawn, so the loading indicator (which animates
 * forever) is never on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EditLayoutTest {

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
     * The photo card is 60% (within 1 dp) of the body below the top bar and above the Save bar,
     * and its top and height are the same with no sheet and with each of the four sheets open.
     *
     * The body runs from the card's top (right under the top bar) to the sheet area's bottom
     * (right above the Save bar, whose button sits 12 dp inside it); the row of tools and the
     * sheet area together are the other 40%.
     */
    @Test
    fun the_photo_takes_sixty_percent_of_the_body_in_all_five_states() {
        val viewModel = showEdit()
        val onePx = dp(1f)
        val cards = statesToVisit().map { open ->
            moveTo(viewModel, open)
            val card = bounds(TOOL_CARD_TAG)
            val row = bounds(EDIT_TOOL_ROW_TAG)
            val sheet = bounds(EDIT_SHEET_TAG)
            val back = bounds(TOOL_BACK_TAG)

            // The body starts under the top bar: below the Back button, within the bar's margin.
            assertTrue("card top ${card.top} above the Back button ${back.bottom}", card.top >= back.bottom - 0.5f)
            assertTrue("card top ${card.top} far below the top bar ${back.bottom}", card.top - back.bottom <= dp(20f))
            // Run 9: the bottom Save bar is gone (Done is at the top right), so the body now simply
            // ends at the sheet area's bottom and nothing below it is measured.

            val body = sheet.bottom - card.top
            assertEquals("photo share of the body with $open open", 0.6f * body, card.height, onePx)
            assertEquals("controls share of the body with $open open", 0.4f * body, sheet.bottom - row.top, onePx)
            card
        }
        val first = cards.first()
        cards.forEachIndexed { index, card ->
            assertEquals("card top in state $index", first.top, card.top, 0.5f)
            assertEquals("card height in state $index", first.height, card.height, 0.5f)
            assertEquals("card width in state $index", first.width, card.width, 0.5f)
        }
    }

    /**
     * Each tool's sheet fills the rest of the body under the row of tools: its area starts at the
     * row's bottom, ends at the Save bar's top, is as wide as the photo card, and holds the open
     * tool's controls ([EDIT_PANEL_TAG]).
     */
    @Test
    fun each_sheet_fills_the_rest_below_the_row_of_tools() {
        val viewModel = showEdit()
        val onePx = dp(1f)
        EditTool.entries.forEach { tool ->
            moveTo(viewModel, tool)
            val card = bounds(TOOL_CARD_TAG)
            val row = bounds(EDIT_TOOL_ROW_TAG)
            val sheet = bounds(EDIT_SHEET_TAG)

            assertEquals("row under the photo, $tool", card.bottom, row.top, onePx)
            assertEquals("sheet under the row, $tool", row.bottom, sheet.top, onePx)
            assertEquals("sheet left edge, $tool", card.left, sheet.left, onePx)
            assertEquals("sheet right edge, $tool", card.right, sheet.right, onePx)
            assertTrue("sheet of $tool only ${sheet.height} px tall", sheet.height >= dp(100f))
            compose.onAllNodes(hasTestTag(EDIT_PANEL_TAG) and hasAnyAncestor(hasTestTag(EDIT_SHEET_TAG)))
                .assertCountEquals(1)
        }
    }

    /**
     * Nothing dims the photo while a sheet is open: the photo card's pixels, drawn from the
     * activity's content view, are exactly the same with each sheet open as with every sheet
     * closed, and no dialog or popup window is on screen at any time.
     */
    @Test
    fun an_open_sheet_leaves_the_photo_undimmed() {
        val viewModel = showEdit()
        moveTo(viewModel, null)
        val card = bounds(TOOL_CARD_TAG)
        val closed = pixelsIn(card)
        // The card is drawn: some of its pixels are opaque.
        assertTrue("the photo card drew nothing", closed.any { (it ushr 24) == 0xFF })
        EditTool.entries.forEach { tool ->
            moveTo(viewModel, tool)
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onAllNodes(isPopup()).assertCountEquals(0)
            val open = pixelsIn(card)
            val changed = closed.indices.count { closed[it] != open[it] }
            assertEquals("pixels of the photo that changed with $tool open", 0, changed)
        }
    }

    /**
     * The Edit screen has no "Replace" chip, and each of the other four tool screens has one, all
     * drawn with the photo ready.
     */
    @Test
    fun only_the_edit_screen_has_no_replace_chip() {
        val photo = photo()
        var shown by mutableStateOf(readyState(Tool.EDIT, photo))
        compose.setContent {
            MaterialTheme {
                ToolContent(state = shown, onBack = {}, onDone = {})
            }
        }
        Tool.entries.forEach { tool ->
            shown = readyState(tool, photo)
            compose.waitForIdle()
            val replaceChips = compose.onAllNodesWithTag(TOOL_REPLACE_TAG)
            if (tool == Tool.EDIT) replaceChips.assertCountEquals(0) else replaceChips.assertCountEquals(1)
        }
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

    /** The five states the layout is checked in: no sheet open, then each tool's sheet. */
    private fun statesToVisit(): List<EditTool?> = listOf<EditTool?>(null) + EditTool.entries

    /**
     * Puts the screen in the state where [open]'s sheet is open (none for null), through the
     * ViewModel's own functions, and waits until the sheet's animation has settled.
     */
    private fun moveTo(viewModel: ToolViewModel, open: EditTool?) {
        if (open == null) {
            viewModel.closeTool()
        } else if (viewModel.state.value.openTool != open) {
            // openTool closes a sheet that is already open, so it is called only to change it.
            viewModel.openTool(open)
        }
        compose.waitForIdle()
        assertEquals(open, viewModel.state.value.openTool)
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

    /** [value] dp in this screen's pixels. */
    private fun dp(value: Float): Float = with(compose.density) { value.dp.toPx() }

    /**
     * Draws the activity's content view into a fresh bitmap and returns the pixels inside [area]
     * (in the root's pixels, rounded inward), row after row.
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
            val left = kotlin.math.ceil(area.left).toInt().coerceIn(0, bitmap.width - 1)
            val top = kotlin.math.ceil(area.top).toInt().coerceIn(0, bitmap.height - 1)
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

    /** A snapshot of [tool]'s screen with [photo] decoded and shown, nothing edited or saved. */
    private fun readyState(tool: Tool, photo: Bitmap): ToolUiState =
        ToolUiState(tool = tool, photoUri = PHOTO_URI, photo = ToolUiState.PhotoState.Ready(photo))

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
