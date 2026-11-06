package com.phongkaster.kanso.tool.edit

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.a11y.TouchTargetCheck
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.HslAdjustments
import com.phongkaster.kanso.data.edit.edit.HslChannel
import com.phongkaster.kanso.ui.tool.component.editbottombar.EDIT_SHEET_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.EditSheet
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EditToolRow
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.EDIT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EditPanelContent
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.HSL_CHIPS_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.HSL_SLIDER_ROW_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.HSL_SLIDER_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow.HSL_RANGE_ROW_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow.hslRangeTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.hslChipTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks on Android 15 the Edit screen's HSL sheet, drawn inside the real 220 dp `EditSheet` by the
 * stateless `EditPanelContent` (DoD 2, 14, 17 and 21 of Run 11):
 *
 * - nine circles in the order Red 0, Orange 40, Light green 80, Green 120, Light blue 160, Blue 200,
 *   Navy blue 240, Purple 280 and Pink 320 degrees, each with its name; the chosen one is selected
 *   and a radio button; a tap chooses one and changes no setting;
 * - three chips, Hue, Saturation and Lightness, and one slider from -100 to 100;
 * - the sheet stays 220 dp tall with all three rows fully inside it, and every circle, chip and the
 *   slider is at least 48 dp to touch (measured as the accessibility walk does, never by the clipped
 *   `boundsInRoot`);
 * - dragging the slider reports live previews and letting go reports one commit;
 * - there is no reset button;
 * - the tool row holds an HSL entry, right after RGB and before Filters.
 *
 * The test holds the settings and the chosen range and channel itself in `mutableStateOf`s, as the
 * ViewModel holds them for the real screen. No ViewModel, Koin or photo is involved.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class HslSheetTest {

    /** Starts an empty activity to host the sheet. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The settings the sheet draws, held by the test as the ViewModel holds them in the app. */
    private var settings by mutableStateOf(EditSettings())

    /** The chosen colour range, held by the test as the ViewModel holds it in the app. */
    private var range by mutableStateOf(ColourRange.RED)

    /** The chosen channel, held by the test as the ViewModel holds it in the app. */
    private var channel by mutableStateOf(HslChannel.HUE)

    /** Every set of settings handed to `onPreview`, in order. */
    private val previews = mutableListOf<EditSettings>()

    /** Every set of settings handed to `onChange`, in order. */
    private val changes = mutableListOf<EditSettings>()

    /** How many times `onCommit` was called. */
    private var commits = 0

    /** The nine ranges with the name each shows and the degrees of its centre, in the required order. */
    private val expectedRanges = listOf(
        Triple(ColourRange.RED, "Red", 0f),
        Triple(ColourRange.ORANGE, "Orange", 40f),
        Triple(ColourRange.LIGHT_GREEN, "Light green", 80f),
        Triple(ColourRange.GREEN, "Green", 120f),
        Triple(ColourRange.LIGHT_BLUE, "Light blue", 160f),
        Triple(ColourRange.BLUE, "Blue", 200f),
        Triple(ColourRange.NAVY_BLUE, "Navy blue", 240f),
        Triple(ColourRange.PURPLE, "Purple", 280f),
        Triple(ColourRange.PINK, "Pink", 320f),
    )

    /** The three chips with the name each shows. */
    private val expectedChips = listOf(
        HslChannel.HUE to "Hue",
        HslChannel.SATURATION to "Saturation",
        HslChannel.LIGHTNESS to "Lightness",
    )

    /**
     * The nine circles exist in the order Red to Pink, left to right by `positionInRoot`, each named
     * after its range and standing 40 degrees after the one before (the model's centres).
     */
    @Test
    fun nine_circles_in_order_with_their_names() {
        show()
        assertEquals(expectedRanges.map(transform = { it.first }), ColourRange.entries)
        assertEquals(expectedRanges.map(transform = { it.third }), ColourRange.entries.map(transform = { it.centreDegrees }))
        expectedRanges.zipWithNext().forEach(
            action = { (a, b) -> assertEquals(40f, b.third - a.third, 0f) },
        )
        expectedRanges.forEach(
            action = { (circle, name, _) ->
                compose.onNodeWithTag(hslRangeTag(circle)).assertContentDescriptionEquals(name)
            },
        )
        val lefts = expectedRanges.map(
            transform = { (circle, _, _) -> compose.onNodeWithTag(hslRangeTag(circle)).fetchSemanticsNode().positionInRoot.x },
        )
        lefts.zipWithNext().forEachIndexed(
            action = { index, (a, b) -> assertTrue("circle ${index + 1} is not right of circle $index", b > a) },
        )
    }

    /**
     * The chosen circle is selected and the other eight are not; every circle is a radio button.
     * Choosing another moves the mark.
     */
    @Test
    fun the_chosen_circle_is_selected_and_a_radio_button() {
        show()
        ColourRange.entries.forEach(
            action = { chosen ->
                compose.runOnIdle(action = { range = chosen })
                compose.waitForIdle()
                ColourRange.entries.forEach(
                    action = { other ->
                        val node = compose.onNodeWithTag(hslRangeTag(other))
                        if (other == chosen) node.assertIsSelected() else node.assertIsNotSelected()
                        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                    },
                )
            },
        )
    }

    /** Tapping a circle chooses it, changes no setting and reports no preview, change or commit. */
    @Test
    fun tapping_a_circle_chooses_it_and_changes_nothing() {
        show()
        ColourRange.entries.forEach(
            action = { target ->
                // No scrolling: all nine circles fit this 411 dp window (368 dp), and asking a row
                // with nothing to scroll to scroll never let Compose's test clock go idle.
                compose.onNodeWithTag(hslRangeTag(target)).performClick()
                compose.waitForIdle()
                assertEquals(target, range)
                compose.onNodeWithTag(hslRangeTag(target)).assertIsSelected()
            },
        )
        assertEquals("a circle changed the settings", EditSettings(), settings)
        assertTrue("a circle made a change", changes.isEmpty() && previews.isEmpty() && commits == 0)
    }

    /**
     * The three chips are named Hue, Saturation and Lightness; the chosen one is selected, and
     * tapping one chooses it without changing the settings.
     */
    @Test
    fun three_chips_choose_the_channel() {
        show()
        expectedChips.forEach(
            action = { (chip, name) ->
                compose.onNodeWithTag(hslChipTag(chip)).performClick()
                compose.waitForIdle()
                assertEquals(chip, channel)
                compose.onNodeWithTag(hslChipTag(chip)).assertTextEquals(name)
                expectedChips.forEach(
                    action = { (other, _) ->
                        val node = compose.onNodeWithTag(hslChipTag(other))
                        if (other == chip) node.assertIsSelected() else node.assertIsNotSelected()
                    },
                )
            },
        )
        assertEquals(EditSettings(), settings)
        assertTrue(changes.isEmpty() && previews.isEmpty() && commits == 0)
    }

    /**
     * Exactly one slider is shown, from -1 to 1 and at 0, its number reads 0, and it shows the
     * value stored for the chosen range and channel: 0.4 for Green saturation reads 40 there and
     * 0 on the Hue chip.
     */
    @Test
    fun one_slider_shows_the_value_of_the_chosen_range_and_channel() {
        settings = EditSettings(hsl = HslAdjustments.NEUTRAL.with(ColourRange.GREEN, HslChannel.SATURATION, 0.4f))
        show()
        compose.onAllNodes(
            hasAnyAncestor(hasTestTag(EDIT_PANEL_TAG)) and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
        ).assertCountEquals(1)
        val info = rangeInfo()
        assertEquals(-1f, info.range.start, 0f)
        assertEquals(1f, info.range.endInclusive, 0f)
        assertEquals(0f, info.current, 0f)
        compose.onNodeWithTag("$HSL_SLIDER_TAG-value").assertTextEquals("0")

        compose.runOnIdle(action = { range = ColourRange.GREEN })
        compose.onNodeWithTag(hslChipTag(HslChannel.SATURATION)).performClick()
        compose.waitForIdle()
        assertEquals(0.4f, rangeInfo().current, 0.0001f)
        compose.onNodeWithTag("$HSL_SLIDER_TAG-value").assertTextEquals("40")
    }

    /**
     * DoD 21: the sheet is 220 dp tall, and the three rows (circles, chips, slider) lie inside it
     * from top to bottom without overlapping. The rows are measured by the room each takes in the
     * layout (32 dp): the circles' and the slider's 48 dp touch areas are meant to overflow their
     * rows. Each circle lies inside the window's width and the
     * sheet, its centre on its row (all nine fit this 411 dp window, so no scrolling is needed). Positions come from `positionInRoot` and the laid-out size,
     * never from `boundsInRoot`.
     */
    @Test
    fun the_sheet_is_220_dp_and_all_three_rows_are_fully_inside_it() {
        show()
        val density = Density(compose.activity).density
        val area = compose.onNodeWithTag(SHEET_AREA_TAG).fetchSemanticsNode()
        assertEquals("the sheet area is not 220 dp tall", 220f, area.size.height / density, 0.5f)

        val sheet = bounds(compose.onNodeWithTag(EDIT_SHEET_TAG).fetchSemanticsNode())
        val rows = listOf(HSL_RANGE_ROW_TAG, HSL_CHIPS_TAG, HSL_SLIDER_ROW_TAG).map(
            transform = { tag -> tag to bounds(compose.onNodeWithTag(tag).fetchSemanticsNode()) },
        )
        rows.forEach(
            action = { (tag, row) ->
                assertTrue(
                    "$tag $row is not inside the sheet $sheet",
                    row.top >= sheet.top - 0.5f && row.bottom <= sheet.bottom + 0.5f &&
                        row.left >= sheet.left - 0.5f && row.right <= sheet.right + 0.5f,
                )
            },
        )
        rows.zipWithNext().forEach(
            action = { (above, below) ->
                assertTrue("${above.first} overlaps ${below.first}", above.second.bottom <= below.second.top + 0.5f)
            },
        )
        val rootWidth = compose.onRoot().fetchSemanticsNode().size.width.toFloat()
        val circleRow = rows.first().second
        ColourRange.entries.forEach(
            action = { circle ->
                // All nine circles fit this window, so none is scrolled out of the row.
                compose.waitForIdle()
                val box = bounds(compose.onNodeWithTag(hslRangeTag(circle)).fetchSemanticsNode())
                assertTrue("$circle $box leaves the window", box.left >= -0.5f && box.right <= rootWidth + 0.5f)
                assertTrue(
                    "$circle $box is not inside the sheet $sheet",
                    box.top >= sheet.top - 0.5f && box.bottom <= sheet.bottom + 0.5f,
                )
                val centreY = (box.top + box.bottom) / 2f
                assertTrue(
                    "$circle $box is not centred on its row $circleRow",
                    centreY >= circleRow.top && centreY <= circleRow.bottom,
                )
            },
        )
    }

    /**
     * DoD 21: each of the nine circles, the three chips and the slider is at least 48 x 48 dp to
     * touch, measured as `TouchTargetCheck` does (the largest of the laid-out size and the touch
     * bounds), and each has a spoken name. Nothing is scrolled: all nine circles fit this 411 dp
     * window, so each is measured whole, as a finger would meet it.
     */
    @Test
    fun every_circle_chip_and_the_slider_is_at_least_48_dp_to_touch() {
        show()
        val minPx = with(Density(compose.activity)) { TouchTargetCheck.MIN_SIZE.toPx() }
        val circleTags = ColourRange.entries.map(transform = { hslRangeTag(it) })
        val tags = circleTags + HslChannel.entries.map(transform = { hslChipTag(it) }) + HSL_SLIDER_TAG
        assertEquals(13, tags.size)
        tags.forEach(
            action = { tag ->
                compose.waitForIdle()
                val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
                assertTrue("$tag has no spoken name", TouchTargetCheck.spokenName(node) != null)
                val (width, height) = TouchTargetCheck.measuredSizePx(node)
                assertTrue(
                    "$tag is $width x $height px, under 48 dp ($minPx px)",
                    width + 0.5f >= minPx && height + 0.5f >= minPx,
                )
            },
        )
    }

    /**
     * DoD 17, the sheet's half: dragging the slider calls `onPreview` while the finger moves (the
     * settings change for the Red hue only) and never `onCommit`; letting go calls `onCommit`. No
     * `onChange` (a whole action) is made.
     */
    @Test
    fun dragging_reports_previews_live_and_letting_go_commits() {
        show()
        val slider = compose.onNodeWithTag(HSL_SLIDER_TAG)
        slider.performTouchInput(
            block = {
                down(center)
                // Several small moves, so the drag passes the touch slop and then follows the finger.
                repeat(times = DRAG_STEPS, action = { moveBy(Offset(width * DRAG_STEP_FRACTION, 0f)) })
            },
        )
        compose.waitForIdle()
        assertTrue("dragging made no preview", previews.size >= 2)
        assertEquals("a commit came before the finger was lifted", 0, commits)
        val dragged = settings.hsl[ColourRange.RED, HslChannel.HUE]
        assertTrue("the drag did not raise the red hue", dragged > 0.1f)

        slider.performTouchInput(block = { up() })
        compose.waitForIdle()
        assertTrue("letting go made no commit", commits >= 1)
        assertTrue("a slider is never an action", changes.isEmpty())
        assertEquals(
            "the drag moved another of the 27 values",
            HslAdjustments.NEUTRAL.with(ColourRange.RED, HslChannel.HUE, dragged),
            settings.hsl,
        )
    }

    /**
     * With `enabled = false` (Compare on, or Done rendering) all nine circles, the three chips and
     * the slider are disabled, and tapping a circle or a chip changes nothing.
     */
    @Test
    fun a_disabled_sheet_cannot_be_used() {
        show(enabled = false)
        val tags = ColourRange.entries.map(transform = { hslRangeTag(it) }) +
            HslChannel.entries.map(transform = { hslChipTag(it) }) + HSL_SLIDER_TAG
        tags.forEach(action = { tag -> compose.onNodeWithTag(tag).assertIsNotEnabled() })
        compose.onNodeWithTag(hslRangeTag(ColourRange.BLUE)).performClick()
        compose.onNodeWithTag(hslChipTag(HslChannel.LIGHTNESS)).performClick()
        compose.waitForIdle()
        assertEquals(ColourRange.RED, range)
        assertEquals(HslChannel.HUE, channel)
        assertEquals(EditSettings(), settings)
        assertTrue(changes.isEmpty() && previews.isEmpty() && commits == 0)
    }

    /** DoD 14: the HSL sheet holds no reset button, by text or by spoken name. */
    @Test
    fun the_sheet_has_no_reset_button() {
        show()
        compose.onAllNodes(
            hasAnyAncestor(hasTestTag(EDIT_PANEL_TAG)) and (
                hasText("reset", substring = true, ignoreCase = true) or
                    hasContentDescription("reset", substring = true, ignoreCase = true)
                ),
        ).assertCountEquals(0)
    }

    /**
     * The row of tools has an HSL entry named "HSL" that is there once the row is scrolled to it,
     * and not selected while no tool is open.
     */
    @Test
    fun the_tool_row_has_an_hsl_entry() {
        showRow()
        compose.scrollToolRowTo(EditTool.HSL)
        compose.waitForIdle()
        compose.onNodeWithTag(editToolTag(EditTool.HSL)).assertExists()
        compose.onNodeWithTag(editToolTag(EditTool.HSL)).assert(hasText("HSL"))
    }

    /**
     * On a window wide enough to hold every entry, HSL sits right after RGB and before Filters, by
     * `positionInRoot` from left to right (a lazy row composes only what is on screen, hence the
     * wide window).
     */
    @Test
    @Config(sdk = [35], qualifiers = "w900dp-h891dp-xxhdpi")
    fun the_hsl_entry_sits_between_rgb_and_filters() {
        showRow()
        val tools = EditTool.entries
        assertEquals(tools.indexOf(EditTool.RGB) + 1, tools.indexOf(EditTool.HSL))
        assertEquals(tools.indexOf(EditTool.HSL) + 1, tools.indexOf(EditTool.FILTERS))
        val rgb = compose.onNodeWithTag(editToolTag(EditTool.RGB)).fetchSemanticsNode().positionInRoot.x
        val hsl = compose.onNodeWithTag(editToolTag(EditTool.HSL)).fetchSemanticsNode().positionInRoot.x
        val filters = compose.onNodeWithTag(editToolTag(EditTool.FILTERS)).fetchSemanticsNode().positionInRoot.x
        assertTrue("HSL ($hsl) is not right of RGB ($rgb)", hsl > rgb)
        assertTrue("Filters ($filters) is not right of HSL ($hsl)", filters > hsl)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Draws the HSL sheet inside the real 220 dp sheet area at the bottom of the window, from
     * [settings], [range] and [channel]; every callback updates or records the test's state.
     * [enabled] false draws it as while Compare is on or Done renders.
     */
    private fun show(enabled: Boolean = true) {
        compose.setContent(
            composable = {
                MaterialTheme(
                    content = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            EditSheet(
                                openTool = EditTool.HSL,
                                modifier = Modifier.testTag(SHEET_AREA_TAG),
                                sheet = {
                                    EditPanelContent(
                                        openTool = EditTool.HSL,
                                        settings = settings,
                                        enabled = enabled,
                                        onChange = { new ->
                                            changes += new
                                            settings = new
                                        },
                                        onPreview = { new ->
                                            previews += new
                                            settings = new
                                        },
                                        onCommit = { commits++ },
                                        onRatio = {},
                                        hslRange = range,
                                        hslChannel = channel,
                                        onHslRange = { range = it },
                                        onHslChannel = { channel = it },
                                    )
                                },
                            )
                        }
                    },
                )
            },
        )
        compose.waitForIdle()
    }

    /** Draws the row of tools alone, with no tool open. */
    private fun showRow() {
        compose.setContent(
            composable = {
                MaterialTheme(
                    content = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            EditToolRow(openTool = null, enabled = true, onOpen = {})
                        }
                    },
                )
            },
        )
        compose.waitForIdle()
    }

    /** The range and current value the HSL slider reports. */
    private fun rangeInfo(): ProgressBarRangeInfo =
        compose.onNodeWithTag(HSL_SLIDER_TAG).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]

    /** The bounds of [node] in the window, in pixels, from `positionInRoot` and its size. */
    private fun bounds(node: SemanticsNode): Bounds {
        val position = node.positionInRoot
        return Bounds(position.x, position.y, position.x + node.size.width, position.y + node.size.height)
    }

    /** A rectangle in pixels, for the position checks of this test. */
    private data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** Values shared by the tests. */
    private companion object {
        /** The tag put on the 220 dp sheet area. */
        const val SHEET_AREA_TAG = "hsl-test-sheet-area"

        /** The number of moves in one drag. */
        const val DRAG_STEPS = 5

        /** Each move's length as a fraction of the slider's width. */
        const val DRAG_STEP_FRACTION = 0.07f
    }
}
