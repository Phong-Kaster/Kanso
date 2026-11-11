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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.a11y.TouchTargetCheck
import com.phongkaster.kanso.data.edit.edit.CurveAdjustments
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.ToneCurve
import com.phongkaster.kanso.ui.tool.component.editbottombar.EditSheet
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EditPanelContent
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.CURVE_DELETE_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.CURVE_GRAPH_PADDING_DP
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.CURVE_GRAPH_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.CURVE_RESET_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.curveChipTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks on Android 15 the Edit screen's Curves sheet, drawn inside the real `EditSheet` by the
 * stateless `EditPanelContent`: the graph and Reset are there, the graph has a spoken description
 * naming the RGB curve, Reset is at least 48 dp and is disabled while the curve is straight, a tap on
 * the line adds and selects a point on the line, a tap away adds nothing and clears the selection, a
 * point within reach wins over the line, a drag from a point or from the line commits once when the
 * finger lifts, a long press keeps the point, at ten points a tap on the line adds nothing, and the
 * delete button works on the selected point (disabled with nothing selected or two points).
 *
 * The test holds the settings itself in a `mutableStateOf`, as the ViewModel holds them in the app.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class CurvesSheetTest {

    /** Starts an empty activity to host the sheet. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The settings the sheet draws. */
    private var settings by mutableStateOf(EditSettings())

    /** Every set of settings handed to `onPreview`, in order. */
    private val previews = mutableListOf<EditSettings>()

    /** Every set of settings handed to `onChange`, in order. */
    private val changes = mutableListOf<EditSettings>()

    /** How many times `onCommit` was called. */
    private var commits = 0

    /** The curve the chips have chosen, as the ViewModel holds it in the app. */
    private var channel by mutableStateOf(CurveChannel.RGB)

    /** Every channel handed to `onCurveChannel`, in order. */
    private val chosen = mutableListOf<CurveChannel>()

    /** The graph is drawn and has a spoken description naming the RGB curve; Reset is drawn too. */
    @Test
    fun the_sheet_shows_the_graph_and_reset() {
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).assertExists()
        compose.onNodeWithTag(CURVE_RESET_TAG).assertExists()
        val description = compose.onNodeWithTag(CURVE_GRAPH_TAG).fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].joinToString(" ")
        assertTrue("the description is \"$description\"", description.contains("RGB"))
    }

    /** Reset is disabled while the curve is straight and enabled after a bend; pressing it hands up a straight curve. */
    @Test
    fun reset_is_disabled_while_straight_and_enabled_after_a_bend() {
        show()
        compose.onNodeWithTag(CURVE_RESET_TAG).assertIsNotEnabled()

        settings = bentSettings()
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_RESET_TAG).assertIsEnabled()

        compose.onNodeWithTag(CURVE_RESET_TAG).performClick()
        compose.waitForIdle()
        assertEquals(1, changes.size)
        assertTrue(changes.last().curves.rgb.isNeutral)
        compose.onNodeWithTag(CURVE_RESET_TAG).assertIsNotEnabled()
    }

    /** Reset has a spoken name and is at least 48 x 48 dp to touch; the graph is far larger. */
    @Test
    fun reset_is_at_least_48_dp_to_touch() {
        show()
        val minPx = with(Density(compose.activity)) { TouchTargetCheck.MIN_SIZE.toPx() }
        val node = compose.onNodeWithTag(CURVE_RESET_TAG).fetchSemanticsNode()
        assertTrue("Reset has no spoken name", TouchTargetCheck.spokenName(node) != null)
        val (width, height) = TouchTargetCheck.measuredSizePx(node)
        assertTrue("Reset is $width x $height px, under 48 dp ($minPx px)", width + 0.5f >= minPx && height + 0.5f >= minPx)
        val graph = compose.onNodeWithTag(CURVE_GRAPH_TAG).fetchSemanticsNode()
        assertTrue(graph.size.width > minPx * 4 && graph.size.height > minPx)
    }

    /**
     * A tap near the straight line (a little above it, so the finger is not exactly on it) adds a point
     * at the line's own height, not the finger's: one commit, a 3-point curve, and the new point is
     * selected (so the delete button is enabled).
     */
    @Test
    fun a_tap_on_the_line_adds_a_point_on_the_line_and_selects_it() {
        show()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsNotEnabled()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.5f, 0.58f)) }
        compose.waitForIdle()

        assertEquals(1, commits)
        assertEquals(3, previews.last().curves.rgb.points.size)
        val added = settings.curves.rgb.points[1]
        assertEquals(0.5f, added.x, 0.03f)
        // The straight line is at 0.5 there; the finger was at 0.58.
        assertEquals(0.5f, added.y, 0.02f)
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()
    }

    /** A tap far from the line and from every point adds nothing, commits nothing and clears the selection. */
    @Test
    fun a_tap_away_from_the_line_adds_nothing_and_clears_the_selection() {
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.5f, 0.5f)) }
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()
        val commitsBefore = commits

        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.5f, 0.97f)) }
        compose.waitForIdle()

        assertEquals(commitsBefore, commits)
        assertEquals(3, settings.curves.rgb.points.size)
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsNotEnabled()
    }

    /** Tapping exactly on a point selects it and changes nothing else. */
    @Test
    fun a_tap_on_a_point_selects_it() {
        settings = bentSettings()
        show()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsNotEnabled()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.5f, 0.75f)) }
        compose.waitForIdle()

        assertEquals(0, commits)
        assertEquals(bentSettings(), settings)
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()
    }

    /** A tap that is within reach of both a point and the line picks the point: no new point is added. */
    @Test
    fun a_point_within_reach_wins_over_the_line() {
        settings = bentSettings()
        show()
        // About 0.08 below the point is under 24 dp from it and also from the line.
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.5f, 0.67f)) }
        compose.waitForIdle()

        assertEquals(0, commits)
        assertEquals(3, settings.curves.rgb.points.size)
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()
    }

    /** Dragging from a point moves it: previews while moving, one commit at the end, final y where the finger let go. */
    @Test
    fun a_drag_moves_the_point_and_commits_once() {
        settings = bentSettings()
        show()
        val previewsBefore = previews.size

        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput {
            down(spot(0.5f, 0.75f))
            moveTo(spot(0.5f, 0.55f))
            moveTo(spot(0.5f, 0.3f))
            up()
        }
        compose.waitForIdle()

        assertTrue("no live preview while dragging", previews.size > previewsBefore)
        assertEquals("the drag was not one commit", 1, commits)
        assertEquals(3, settings.curves.rgb.points.size)
        assertEquals(0.3f, settings.curves.rgb.points[1].y, 0.03f)
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()
    }

    /** A drag that starts on the line away from any point adds a point, the previews follow, and lifting is one commit. */
    @Test
    fun a_drag_from_the_line_adds_a_point_and_commits_once() {
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput {
            down(spot(0.5f, 0.5f))
            moveTo(spot(0.5f, 0.7f))
            moveTo(spot(0.5f, 0.9f))
            up()
        }
        compose.waitForIdle()

        assertTrue("the previews did not follow the finger", previews.size >= 2)
        assertEquals("the drag was not one commit", 1, commits)
        assertEquals(3, settings.curves.rgb.points.size)
        assertEquals(0.9f, settings.curves.rgb.points[1].y, 0.03f)
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()
    }

    /** A long press no longer removes a point: the point stays, nothing is committed, and it is selected. */
    @Test
    fun a_long_press_keeps_the_point() {
        settings = bentSettings()
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { longClick(spot(0.5f, 0.75f)) }
        compose.waitForIdle()

        assertEquals(0, commits)
        assertTrue(changes.isEmpty())
        assertEquals(bentSettings(), settings)
    }

    /**
     * With ten unevenly spaced points (nine close together, then a gap far wider than 48 dp) a tap on
     * the line inside the gap adds nothing and commits nothing.
     */
    @Test
    fun at_ten_points_a_tap_adds_nothing() {
        val ten = ToneCurve(
            List(ToneCurve.MAX_POINTS) { i ->
                val x = if (i < ToneCurve.MAX_POINTS - 1) i * 0.05f else 1f
                CurvePoint(x, 0.25f + 0.5f * x)
            },
        )
        settings = EditSettings(curves = CurveAdjustments.NEUTRAL.with(CurveChannel.RGB, ten))
        show()
        val lineY = ten.valueAt(0.7f)
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.7f, lineY)) }
        compose.waitForIdle()

        assertEquals(0, commits)
        assertEquals(ToneCurve.MAX_POINTS, settings.curves.rgb.points.size)
    }

    /** The delete button is disabled with nothing selected, and with only two points even when an end is selected. */
    @Test
    fun delete_is_disabled_with_nothing_selected_and_with_two_points() {
        settings = bentSettings()
        show()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsNotEnabled()

        settings = EditSettings()
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0f, 0f)) }
        compose.waitForIdle()
        assertEquals(2, settings.curves.rgb.points.size)
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsNotEnabled()
    }

    /** Delete removes the selected middle point in one change, and selects its left neighbour. */
    @Test
    fun delete_removes_the_selected_point_and_selects_the_left_neighbour() {
        val points = listOf(CurvePoint(0f, 0f), CurvePoint(0.3f, 0.4f), CurvePoint(0.7f, 0.6f), CurvePoint(1f, 1f))
        settings = curvesSettings(points)
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.7f, 0.6f)) }
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_DELETE_TAG).performClick()
        compose.waitForIdle()

        assertEquals(1, changes.size)
        assertEquals(listOf(points[0], points[1], points[3]), settings.curves.rgb.points)

        // The left neighbour (0.3, 0.4) is selected now: a second delete removes it, not the last point.
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()
        compose.onNodeWithTag(CURVE_DELETE_TAG).performClick()
        compose.waitForIdle()
        assertEquals(2, changes.size)
        assertEquals(listOf(points[0], points[3]), settings.curves.rgb.points)
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsNotEnabled()
    }

    /** Deleting the first point selects the new first point, so a second delete removes that one. */
    @Test
    fun deleting_the_first_point_selects_the_new_first_point() {
        val points = listOf(CurvePoint(0f, 0f), CurvePoint(0.3f, 0.4f), CurvePoint(0.7f, 0.6f), CurvePoint(1f, 1f))
        settings = curvesSettings(points)
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0f, 0f)) }
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_DELETE_TAG).performClick()
        compose.waitForIdle()
        assertEquals(listOf(points[1], points[2], points[3]), settings.curves.rgb.points)

        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()
        compose.onNodeWithTag(CURVE_DELETE_TAG).performClick()
        compose.waitForIdle()
        assertEquals(listOf(points[2], points[3]), settings.curves.rgb.points)
    }

    /** Reset clears the selection as well as straightening the curve. */
    @Test
    fun reset_clears_the_selection() {
        settings = bentSettings()
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.5f, 0.75f)) }
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()

        compose.onNodeWithTag(CURVE_RESET_TAG).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsNotEnabled()
        compose.onNodeWithTag(CURVE_RESET_TAG).assertIsNotEnabled()
    }

    /** A change that did not come from the graph (Undo or Redo) and a channel change both clear the selection. */
    @Test
    fun undo_redo_and_a_channel_change_clear_the_selection() {
        settings = bentSettings()
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.5f, 0.75f)) }
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()

        // Somebody else (Undo) puts a different curve in.
        settings = EditSettings(
            curves = CurveAdjustments.NEUTRAL.with(
                CurveChannel.RGB,
                ToneCurve.NEUTRAL.added(CurvePoint(0.4f, 0.6f)),
            ),
        )
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsNotEnabled()

        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.4f, 0.6f)) }
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsEnabled()
        compose.onNodeWithTag(curveChipTag(CurveChannel.RED)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(curveChipTag(CurveChannel.RGB)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_DELETE_TAG).assertIsNotEnabled()
    }

    /** The four chips are there with spoken names, RGB is chosen first, and each is at least 48 dp to touch. */
    @Test
    fun the_four_chips_have_names_and_are_at_least_48_dp() {
        show()
        assertEquals(
            listOf(CurveChannel.RGB, CurveChannel.RED, CurveChannel.GREEN, CurveChannel.BLUE),
            CurveChannel.entries.toList(),
        )
        val minPx = with(Density(compose.activity)) { TouchTargetCheck.MIN_SIZE.toPx() }
        CurveChannel.entries.forEach { channel ->
            val node = compose.onNodeWithTag(curveChipTag(channel)).fetchSemanticsNode()
            assertTrue("the $channel chip has no spoken name", TouchTargetCheck.spokenName(node) != null)
            val (width, height) = TouchTargetCheck.measuredSizePx(node)
            assertTrue(
                "the $channel chip is $width x $height px, under 48 dp ($minPx px)",
                width + 0.5f >= minPx && height + 0.5f >= minPx,
            )
        }
        compose.onNodeWithTag(curveChipTag(CurveChannel.RGB)).assertIsSelected()
        compose.onNodeWithTag(curveChipTag(CurveChannel.RED)).assertIsNotSelected()
    }

    /** Tapping a chip hands its channel up, changes no setting and makes no commit; the graph names the chosen curve. */
    @Test
    fun tapping_a_chip_chooses_it_and_the_graph_names_it() {
        show()
        CurveChannel.entries.forEach { channel ->
            compose.onNodeWithTag(curveChipTag(channel)).performClick()
            compose.waitForIdle()
            compose.onNodeWithTag(curveChipTag(channel)).assertIsSelected()
            val description = compose.onNodeWithTag(CURVE_GRAPH_TAG).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription].joinToString(" ")
            val name = when (channel) {
                CurveChannel.RGB -> "RGB"
                CurveChannel.RED -> "Red"
                CurveChannel.GREEN -> "Green"
                CurveChannel.BLUE -> "Blue"
            }
            assertTrue("the description is \"$description\"", description.contains(name))
        }
        assertEquals(CurveChannel.entries.toList(), chosen)
        assertEquals(EditSettings(), settings)
        assertEquals(0, commits)
        assertTrue(changes.isEmpty() && previews.isEmpty())
    }

    /** Reset puts only the chosen curve back to straight, and is disabled while that curve is straight. */
    @Test
    fun reset_resets_only_the_chosen_curve() {
        val bend = ToneCurve.NEUTRAL.added(CurvePoint(0.5f, 0.75f))
        settings = EditSettings(
            curves = CurveAdjustments.NEUTRAL.with(CurveChannel.RGB, bend).with(CurveChannel.RED, bend),
        )
        channel = CurveChannel.RED
        show()
        compose.onNodeWithTag(CURVE_RESET_TAG).assertIsEnabled()
        compose.onNodeWithTag(CURVE_RESET_TAG).performClick()
        compose.waitForIdle()

        assertEquals(1, changes.size)
        assertTrue(settings.curves.red.isNeutral)
        assertEquals(bend, settings.curves.rgb)
        compose.onNodeWithTag(CURVE_RESET_TAG).assertIsNotEnabled()

        // The master curve is still bent, so Reset is enabled again once RGB is chosen.
        compose.onNodeWithTag(curveChipTag(CurveChannel.RGB)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(CURVE_RESET_TAG).assertIsEnabled()
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Settings with a master curve holding one middle point at (0.5, 0.75). */
    private fun bentSettings(): EditSettings = EditSettings(
        curves = CurveAdjustments.NEUTRAL.with(
            CurveChannel.RGB,
            ToneCurve.NEUTRAL.added(CurvePoint(0.5f, 0.75f)),
        ),
    )

    /** Settings whose master curve holds exactly [points]. */
    private fun curvesSettings(points: List<CurvePoint>): EditSettings = EditSettings(
        curves = CurveAdjustments.NEUTRAL.with(CurveChannel.RGB, ToneCurve(points)),
    )

    /** The touch position, in the graph's own pixels, of curve coordinates ([x], [y]); y is up. */
    private fun androidx.compose.ui.test.TouchInjectionScope.spot(x: Float, y: Float): Offset {
        val pad = CURVE_GRAPH_PADDING_DP * density
        return Offset(
            pad + x * (width - 2 * pad),
            pad + (1f - y) * (height - 2 * pad),
        )
    }

    /** Draws the Curves sheet inside the real edit sheet; every callback updates or records the test's state. */
    private fun show(enabled: Boolean = true) {
        compose.setContent(
            composable = {
                MaterialTheme(
                    content = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            EditSheet(
                                openTool = EditTool.CURVES,
                                modifier = Modifier.testTag("curves-test-sheet-area"),
                                sheet = {
                                    EditPanelContent(
                                        openTool = EditTool.CURVES,
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
                                        curveChannel = channel,
                                        onCurveChannel = { picked ->
                                            chosen += picked
                                            channel = picked
                                        },
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
}
