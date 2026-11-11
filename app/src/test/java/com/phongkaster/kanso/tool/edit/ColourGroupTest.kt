package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.CurveAdjustments
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.HslChannel
import com.phongkaster.kanso.data.edit.edit.ToneCurve
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EDIT_COMPARE_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EDIT_GROUP_BACK_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EditToolRow
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editGroupDotTag
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editGroupTag
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolDotTag
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.model.EditToolGroup
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
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

/**
 * Checks on Android 15 the Colour group of the Edit screen's row (DoD 1 to 8 and the one-line part
 * of 11): the first row reads Crop, Rotate, Adjust, Colour, Filters, Stickers, Text, Date then
 * Compare; tapping Colour swaps it for Back, RGB, HSL, Curves then Compare; the dots, the sheet
 * closing, the discard question, Compare disabling every entry but itself, and the group surviving
 * a rotation.
 *
 * The test draws the real `EditToolRow` fed by a real [ToolViewModel] on a fake photo, wired as
 * the screen wires it (`onOpen` is the ViewModel's `openTool`), without the sheets: what the
 * sheets show is the ViewModel's `openTool`, which the test reads. Positions are read with
 * `positionInRoot`, never `boundsInRoot`, which clips a scrolled node.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w900dp-h891dp-xxhdpi")
class ColourGroupTest {

    /** Starts an empty activity to host the row. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test, for its strings. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * DoD 1: the first row reads Crop, Rotate, Adjust, Colour, Filters, Stickers, Text, Date left to
     * right, with Compare at the end, and RGB, HSL and Curves are not in it.
     */
    @Test
    fun the_first_row_reads_crop_rotate_adjust_colour_filters_stickers_text_date_then_compare() {
        showRow()
        val expected = listOf(
            editToolTag(EditTool.CROP), editToolTag(EditTool.ROTATE), editToolTag(EditTool.ADJUST),
            editGroupTag(EditToolGroup.COLOUR), editToolTag(EditTool.FILTERS), editToolTag(EditTool.STICKERS),
            editToolTag(EditTool.TEXT), editToolTag(EditTool.DATE), EDIT_COMPARE_TAG,
        )
        assertAscending(expected)
        EditToolGroup.COLOUR.tools.forEach { compose.onNodeWithTag(editToolTag(it)).assertDoesNotExist() }
        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).assertDoesNotExist()
    }

    /**
     * DoD 2: tapping Colour changes the row to Back, RGB, HSL, Curves with Compare still at the
     * end, and tapping Back brings the first row back. Colour opens no tool.
     */
    @Test
    fun tapping_colour_shows_back_rgb_hsl_curves_then_compare_and_back_returns() {
        val viewModel = showRow()
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).performClick()
        compose.waitForIdle()

        assertNull("Colour opened a tool", viewModel.state.value.openTool)
        assertAscending(
            listOf(
                EDIT_GROUP_BACK_TAG, editToolTag(EditTool.RGB), editToolTag(EditTool.HSL),
                editToolTag(EditTool.CURVES), EDIT_COMPARE_TAG,
            ),
        )
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).assertDoesNotExist()
        compose.onNodeWithTag(editToolTag(EditTool.CROP)).assertDoesNotExist()

        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).assertExists()
        compose.onNodeWithTag(editToolTag(EditTool.CROP)).assertExists()
        compose.onNodeWithTag(editToolTag(EditTool.RGB)).assertDoesNotExist()
        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).assertDoesNotExist()
    }

    /** DoD 3: tapping Colour while another tool's sheet is open closes that sheet. */
    @Test
    fun tapping_colour_while_another_tool_is_open_closes_it() {
        val viewModel = showRow()
        compose.onNodeWithTag(editToolTag(EditTool.ADJUST)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.ADJUST, viewModel.state.value.openTool)

        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).performClick()
        compose.waitForIdle()
        assertNull("Adjust's sheet stayed open", viewModel.state.value.openTool)
        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).assertExists()
    }

    /**
     * DoD 4: Colour carries the dot while RGB, HSL or Curves holds a change, and inside the group
     * each of the three shows its own dot and not the others'.
     */
    @Test
    fun colour_and_each_of_its_tools_show_their_dots() {
        val viewModel = showRow()
        val colourDot = editGroupDotTag(EditToolGroup.COLOUR)
        compose.onNodeWithTag(colourDot, useUnmergedTree = true).assertDoesNotExist()

        change(viewModel, viewModel.state.value.settings.copy(red = 0.3f))
        compose.onNodeWithTag(colourDot, useUnmergedTree = true).assertExists()

        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(editToolDotTag(EditTool.RGB), useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(editToolDotTag(EditTool.HSL), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(editToolDotTag(EditTool.CURVES), useUnmergedTree = true).assertDoesNotExist()

        change(viewModel, viewModel.state.value.settings.copy(hsl = hslChange()))
        compose.onNodeWithTag(editToolDotTag(EditTool.HSL), useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(editToolDotTag(EditTool.CURVES), useUnmergedTree = true).assertDoesNotExist()

        change(viewModel, viewModel.state.value.settings.copy(curves = curveChange()))
        compose.onNodeWithTag(editToolDotTag(EditTool.CURVES), useUnmergedTree = true).assertExists()
    }

    /** DoD 4, on its own: with only Curves changed, the first row's Colour entry carries the dot. */
    @Test
    fun colour_shows_its_dot_for_a_curves_only_change() {
        val viewModel = showRow()
        change(viewModel, viewModel.state.value.settings.copy(curves = curveChange()))
        compose.onNodeWithTag(editGroupDotTag(EditToolGroup.COLOUR), useUnmergedTree = true).assertExists()
    }

    /**
     * DoD 5: going Back from an open colour tool closes its sheet and keeps the change, and the
     * first row returns.
     */
    @Test
    fun back_from_an_open_colour_tool_closes_it_and_keeps_the_change() {
        val viewModel = showRow()
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(editToolTag(EditTool.RGB)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.RGB, viewModel.state.value.openTool)
        val changed = viewModel.state.value.settings.copy(red = 0.3f)
        change(viewModel, changed)

        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).performClick()
        compose.waitForIdle()
        assertNull("the sheet stayed open", viewModel.state.value.openTool)
        assertEquals("Back lost the change", changed, viewModel.state.value.settings)
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).assertExists()
        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).assertDoesNotExist()
    }

    /** DoD 6: with only a colour change made, leaving Edit still asks whether to discard it. */
    @Test
    fun leaving_with_only_a_colour_change_asks_to_discard() {
        val viewModel = showRow()
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(editToolTag(EditTool.HSL)).performClick()
        compose.waitForIdle()
        change(viewModel, viewModel.state.value.settings.copy(hsl = hslChange()))
        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).performClick()
        compose.waitForIdle()
        assertFalse(viewModel.state.value.discardPrompt)

        assertTrue("Back left Edit with a colour change", viewModel.onBackPressed())
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(viewModel.state.value.discardPrompt)
    }

    /**
     * DoD 7: while Compare is held every entry of the first row is disabled except Compare, which
     * stays enabled (the way back from comparing).
     */
    @Test
    fun comparing_disables_every_entry_of_the_first_row_but_compare() {
        val viewModel = showRow()
        compare(viewModel, true)
        listOf(EditTool.CROP, EditTool.ROTATE, EditTool.ADJUST, EditTool.FILTERS, EditTool.STICKERS, EditTool.TEXT, EditTool.DATE)
            .forEach { compose.onNodeWithTag(editToolTag(it)).assertIsNotEnabled() }
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).assertIsNotEnabled()
        compose.onNodeWithTag(EDIT_COMPARE_TAG).assertIsEnabled()
    }

    /** DoD 7, on the Colour group's row: Back, RGB, HSL and Curves are disabled, Compare is not. */
    @Test
    fun comparing_disables_every_entry_of_the_colour_row_but_compare() {
        val viewModel = showRow()
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).performClick()
        compose.waitForIdle()
        compare(viewModel, true)
        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).assertIsNotEnabled()
        EditToolGroup.COLOUR.tools.forEach { compose.onNodeWithTag(editToolTag(it)).assertIsNotEnabled() }
        compose.onNodeWithTag(EDIT_COMPARE_TAG).assertIsEnabled()

        compare(viewModel, false)
        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).assertIsEnabled()
        EditToolGroup.COLOUR.tools.forEach { compose.onNodeWithTag(editToolTag(it)).assertIsEnabled() }
    }

    /**
     * DoD 8: the row stays on the Colour group across a screen rotation. A rotation saves the
     * instance state and rebuilds the composition; `StateRestorationTester` does the same to the
     * row, whose "Colour is open" flag is a `rememberSaveable`.
     */
    @Test
    fun the_row_stays_on_the_colour_group_after_a_rotation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme {
                EditToolRow(openTool = null, enabled = true, onOpen = {})
            }
        }
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).assertExists()

        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_GROUP_BACK_TAG).assertExists()
        compose.onNodeWithTag(editToolTag(EditTool.RGB)).assertExists()
        compose.onNodeWithTag(editGroupTag(EditToolGroup.COLOUR)).assertDoesNotExist()
    }

    /**
     * DoD 11, in part: "Colour" is on one line under its icon, judged by the text layout's line
     * count (never by `didOverflowWidth`, which reports the unconstrained width in a test).
     */
    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
    fun the_colour_label_is_on_one_line() {
        showRow()
        val label = compose.onNode(
            hasText("Colour") and hasAnyAncestor(hasTestTag(editGroupTag(EditToolGroup.COLOUR))),
            useUnmergedTree = true,
        ).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        val read = label.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        assertTrue("the label has no text layout", read?.action?.invoke(layouts) == true)
        val layout = layouts.first()
        assertEquals("the label does not fit on one line", 1, layout.lineCount)
        // The entry sets maxLines = 1 with an ellipsis, so lineCount alone is always 1: also
        // require that the one line was not cut short.
        assertFalse("the label is cut with an ellipsis", layout.isLineEllipsized(0))
        assertEquals("the label's line stops before its last letter", "Colour".length, layout.getLineEnd(0))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds a ToolViewModel on a fake photo, loads it, and draws the real [EditToolRow] wired to
     * it as the screen wires it. Returns the ViewModel; no tool is open yet.
     */
    private fun showRow(): ToolViewModel {
        val viewModel = ToolViewModel(
            Tool.EDIT,
            PHOTO_URI,
            FakePhotoLoader(context, Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)),
            ViewModelClearedProbe(),
            workDispatcher = Dispatchers.Unconfined,
        )
        viewModel.load(1000)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        compose.setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                EditToolRow(
                    openTool = state.openTool,
                    enabled = !state.comparing,
                    onOpen = viewModel::openTool,
                    settings = state.settings,
                    comparing = state.comparing,
                    onCompare = viewModel::setComparing,
                )
            }
        }
        compose.waitForIdle()
        return viewModel
    }

    /** Makes [new] the session's settings through the ViewModel and lets the screen redraw. */
    private fun change(viewModel: ToolViewModel, new: EditSettings) {
        viewModel.changeSettings(new, editProcessorFor(context))
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    /** Switches Compare on or off through the ViewModel and lets the screen redraw. */
    private fun compare(viewModel: ToolViewModel, on: Boolean) {
        viewModel.setComparing(on)
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        assertEquals(on, viewModel.state.value.comparing)
    }

    /** Asserts the nodes tagged [tags] stand left to right in that order, by `positionInRoot`. */
    private fun assertAscending(tags: List<String>) {
        val lefts = tags.map { compose.onNodeWithTag(it).fetchSemanticsNode().positionInRoot.x }
        lefts.zipWithNext().forEachIndexed { index, (a, b) ->
            assertTrue("${tags[index + 1]} ($b) is not right of ${tags[index]} ($a)", b > a)
        }
    }

    /** HSL settings with one value away from 0. */
    private fun hslChange() = EditSettings().hsl.with(ColourRange.entries.first(), HslChannel.HUE, 0.3f)

    /** Curves with the master curve bent through one middle point. */
    private fun curveChange() =
        CurveAdjustments.NEUTRAL.with(CurveChannel.RGB, ToneCurve.NEUTRAL.added(CurvePoint(0.5f, 0.3f)))

    /** The made-up Uri of the fake photo. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"
    }
}
