package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.ui.tool.component.toolpanel.EDIT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.EDIT_SHEET_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EDIT_TOOL_ROW_TAG
import com.phongkaster.kanso.ui.tool.TOOL_BACK_TAG
import com.phongkaster.kanso.ui.tool.TOOL_CARD_TAG
import com.phongkaster.kanso.ui.tool.TOOL_REPLACE_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.component.toolpanel.ToolPanel
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EditPanel
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LAYER_SHEET_CANCEL_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LAYER_SHEET_CONFIRM_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.STICKERS_PANEL_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.R
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
 * - the row of tools is the lowest thing on screen (the `Scaffold`'s bottom bar), and the photo
 *   card fills the space between the top bar and the bottom bar, in all five states;
 * - a tapped tool's panel appears just above the row of tools, as wide as the photo, in an area of
 *   one fixed height, so the photo never changes size; with no tool open there is no panel;
 * - an open panel brings no dialog or popup (which a modal bottom sheet would), so nothing dims
 *   the photo or swallows taps on it;
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
     * In all five states (no tool open, then each tool open) the row of tools is the lowest thing
     * on screen, a small margin above the window's bottom edge, and the photo card fills the space
     * from right under the top bar down to a small gap above the bottom bar (the open panel, or
     * the row when no tool is open). The card keeps its top and width throughout.
     */
    @Test
    fun the_row_of_tools_is_at_the_bottom_and_the_photo_fills_the_rest() {
        val viewModel = showEdit()
        val rootHeight = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
        val cards = statesToVisit().map { open ->
            moveTo(viewModel, open)
            val card = bounds(TOOL_CARD_TAG)
            // With Stickers open its sheet takes the row's place as the lowest thing on screen.
            val row = if (open.isLayerSheet()) bounds(EDIT_SHEET_TAG) else bounds(EDIT_TOOL_ROW_TAG)
            val back = bounds(TOOL_BACK_TAG)

            // The row is the bottom bar: near the window's bottom edge, under everything else.
            assertTrue("row bottom ${row.bottom} far above the screen's bottom $rootHeight", rootHeight - row.bottom <= dp(24f))
            // The body starts under the top bar: below the Back button, within the bar's margin.
            assertTrue("card top ${card.top} above the Back button ${back.bottom}", card.top >= back.bottom - 0.5f)
            assertTrue("card top ${card.top} far below the top bar ${back.bottom}", card.top - back.bottom <= dp(20f))
            // The card ends a small gap above the bottom bar's top.
            // With no tool open the panel area is empty but keeps its height, so the card ends
            // above it either way: within a small gap of the open panel, and above the row.
            assertTrue("card bottom ${card.bottom} below the row ${row.top}, $open", card.bottom <= row.top + 0.5f)
            if (open != null) {
                val sheetTop = bounds(EDIT_SHEET_TAG).top
                assertTrue("card bottom ${card.bottom} below the panel $sheetTop, $open", card.bottom <= sheetTop + 0.5f)
                assertTrue("card bottom ${card.bottom} far above the panel $sheetTop, $open", sheetTop - card.bottom <= dp(24f))
            }
            open to card
        }
        // The photo keeps its top, width and height whichever tool is open, layer sheets included:
        // they fit the same height as the row and the panel area (owner's decision, 2026-10-28).
        val first = cards.first().second
        cards.forEachIndexed { index, (open, card) ->
            assertEquals("card top in state $index ($open)", first.top, card.top, 0.5f)
            assertEquals("card width in state $index ($open)", first.width, card.width, 0.5f)
            assertEquals("card height in state $index ($open)", first.height, card.height, 0.5f)
        }
    }

    /**
     * With no tool open there is no panel. Each tool's panel appears just above the row of tools,
     * as wide as the photo card, holding the open tool's controls ([EDIT_PANEL_TAG]), and the
     * photo card keeps exactly the height it has with no tool open.
     */
    @Test
    fun each_panel_opens_just_above_the_row_of_tools() {
        val viewModel = showEdit()
        val onePx = dp(1f)
        moveTo(viewModel, null)
        compose.onAllNodesWithTag(EDIT_SHEET_TAG).assertCountEquals(0)
        val closedCard = bounds(TOOL_CARD_TAG)
        EditTool.entries.forEach { tool ->
            moveTo(viewModel, tool)
            val card = bounds(TOOL_CARD_TAG)
            val sheet = bounds(EDIT_SHEET_TAG)

            if (tool.isLayerSheet()) {
                // A layer sheet replaces the row, so there is no row to sit above; it is the
                // bottom bar itself, near the window's bottom edge.
                compose.onAllNodesWithTag(EDIT_TOOL_ROW_TAG).assertCountEquals(0)
                val rootHeight = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
                assertTrue("Stickers sheet far above the bottom", rootHeight - sheet.bottom <= dp(24f))
            } else {
                val row = bounds(EDIT_TOOL_ROW_TAG)
                assertTrue("panel of $tool below the row's top", sheet.bottom <= row.top + 0.5f)
                assertTrue("panel of $tool far above the row", row.top - sheet.bottom <= dp(12f))
            }
            assertEquals("panel left edge, $tool", card.left, sheet.left, onePx)
            assertEquals("panel right edge, $tool", card.right, sheet.right, onePx)
            assertTrue("panel of $tool only ${sheet.height} px tall", sheet.height >= dp(48f))
            // Every sheet, a layer sheet included, leaves the photo exactly as tall as with none open.
            assertEquals("photo height with $tool open", closedCard.height, card.height, 0.5f)
            compose.onAllNodes(hasTestTag(EDIT_PANEL_TAG) and hasAnyAncestor(hasTestTag(EDIT_SHEET_TAG)))
                .assertCountEquals(1)
        }
    }

    /**
     * DoD 41, amended by ADR-005 and on 2026-10-28: tapping the Stickers entry opens a sheet that
     * replaces the row of tools in the same height, so the photo keeps its left, top, width and
     * height. While it is open none of
     * the five tool entries is on screen; the cross and the tick are displayed in the sheet's top
     * row, the cross left of the tick, each a 48 x 48 dp target with a content description.
     */
    @Test
    fun the_stickers_sheet_replaces_the_row_and_the_photo_keeps_its_size() {
        val viewModel = showEdit()
        moveTo(viewModel, null)
        val before = bounds(TOOL_CARD_TAG)
        // Text and Date sit past the row's right edge on this window since RGB joined the row (the
        // row scrolls), so they are not on screen.
        EditTool.entries.filter(predicate = { it != EditTool.TEXT && it != EditTool.DATE }).forEach(
            action = { compose.onNodeWithTag(editToolTag(it)).assertIsDisplayed() },
        )

        // Stickers is only partly in view; bring it fully into the row before tapping it.
        compose.scrollToolRowTo(EditTool.STICKERS)
        compose.onNodeWithTag(editToolTag(EditTool.STICKERS)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.STICKERS, viewModel.state.value.openTool)

        val after = bounds(TOOL_CARD_TAG)
        assertEquals("photo left", before.left, after.left, 0.5f)
        assertEquals("photo top", before.top, after.top, 0.5f)
        assertEquals("photo width", before.width, after.width, 0.5f)
        // The sheet takes exactly the row's and panel area's height, so the photo keeps its height.
        assertEquals("photo height", before.height, after.height, 0.5f)

        EditTool.entries.forEach { compose.onAllNodesWithTag(editToolTag(it)).assertCountEquals(0) }
        compose.onAllNodesWithTag(EDIT_TOOL_ROW_TAG).assertCountEquals(0)
        compose.onNodeWithTag(STICKERS_PANEL_TAG).assertIsDisplayed()

        compose.onNodeWithTag(LAYER_SHEET_CANCEL_TAG).assertIsDisplayed()
        compose.onNodeWithTag(LAYER_SHEET_CONFIRM_TAG).assertIsDisplayed()
        val sheet = bounds(EDIT_SHEET_TAG)
        val cancel = bounds(LAYER_SHEET_CANCEL_TAG)
        val confirm = bounds(LAYER_SHEET_CONFIRM_TAG)
        assertTrue("cross $cancel is not left of tick $confirm", cancel.right <= confirm.left + 0.5f)
        assertEquals("cross and tick are not on one row", cancel.top, confirm.top, 0.5f)
        listOf(cancel, confirm).forEach { target ->
            assertTrue("$target is not inside the sheet $sheet", target.left >= sheet.left - 0.5f && target.right <= sheet.right + 0.5f)
            assertTrue("$target is not in the sheet's top row", target.top >= sheet.top - 0.5f && target.top - sheet.top <= dp(24f))
            assertEquals("touch target width", dp(48f), target.width, 1f)
            assertEquals("touch target height", dp(48f), target.height, 1f)
        }
        compose.onNodeWithTag(LAYER_SHEET_CANCEL_TAG).assert(hasContentDescription(context.getString(R.string.stickers_sheet_cancel)))
        compose.onNodeWithTag(LAYER_SHEET_CONFIRM_TAG).assert(hasContentDescription(context.getString(R.string.stickers_sheet_confirm)))
    }

    /**
     * An open panel is an ordinary part of the screen, not a modal sheet: no dialog or popup
     * window is on screen with any tool open, so nothing dims the photo or swallows taps on it.
     */
    @Test
    fun an_open_panel_brings_no_dialog_or_popup() {
        val viewModel = showEdit()
        EditTool.entries.forEach { tool ->
            moveTo(viewModel, tool)
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onAllNodes(isPopup()).assertCountEquals(0)
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
                        if (tool == EditTool.STICKERS) {
                            // The Stickers sheet is drawn by the Edit panel with its own callbacks.
                            EditPanel(
                                openTool = tool,
                                settings = state.settings,
                                onChange = { _, _ -> },
                                onPreview = { _, _ -> },
                                onCommit = {},
                                onRatio = { _, _ -> },
                                onStickersCancel = viewModel::cancelLayers,
                                onStickersConfirm = viewModel::confirmLayers,
                            )
                        } else {
                            ToolPanel(
                                tool = Tool.EDIT,
                                state = state,
                                openTool = tool,
                                enabled = !state.comparing && state.processing is ToolUiState.Processing.Idle,
                                // Run 9: Edit's controls no longer take onApply (settings session instead).
                            )
                        }
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
        // Text opens with its keyboard up, which shrinks its sheet to the text area; the layout
        // under test is the sheet with the keyboard down.
        if (open == EditTool.TEXT) {
            viewModel.setTextEditing(false)
            compose.waitForIdle()
        }
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

    /** The made-up Uri of the fake photo and the layer sheet's height. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

    }

    /** True for the two sheets that take the row's place: Stickers and Text. */
    private fun EditTool?.isLayerSheet(): Boolean =
        this == EditTool.STICKERS || this == EditTool.TEXT || this == EditTool.DATE
}
