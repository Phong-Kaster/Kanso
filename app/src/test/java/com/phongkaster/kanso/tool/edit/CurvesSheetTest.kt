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
 * stateless `EditPanelContent` (DoD 3, 4, 5, 6 and 14 of Run 12): the graph and Reset are there, the
 * graph has a spoken description naming the RGB curve, Reset is at least 48 dp and is disabled while
 * the curve is straight, a tap on empty space adds a point, a drag moves the point and commits once
 * when the finger lifts, a long press removes a middle point, and at ten points a tap adds nothing.
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

    /** A tap on an empty spot adds a point there: one commit, and the callbacks hold a 3-point curve. */
    @Test
    fun a_tap_on_empty_space_adds_a_point() {
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.5f, 0.8f)) }
        compose.waitForIdle()

        assertEquals(1, commits)
        assertEquals(3, previews.last().curves.rgb.points.size)
        val added = settings.curves.rgb.points[1]
        assertEquals(0.5f, added.x, 0.03f)
        assertEquals(0.8f, added.y, 0.03f)
    }

    /** Dragging the middle point moves it: previews while moving, one commit at the end, final y where the finger let go. */
    @Test
    fun a_drag_moves_the_point_and_commits_once() {
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.5f, 0.8f)) }
        compose.waitForIdle()
        val commitsBefore = commits
        val previewsBefore = previews.size

        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput {
            down(spot(0.5f, 0.8f))
            moveTo(spot(0.5f, 0.55f))
            moveTo(spot(0.5f, 0.3f))
            up()
        }
        compose.waitForIdle()

        assertTrue("no live preview while dragging", previews.size > previewsBefore)
        assertEquals("the drag was not one commit", commitsBefore + 1, commits)
        assertEquals(3, settings.curves.rgb.points.size)
        assertEquals(0.3f, settings.curves.rgb.points[1].y, 0.03f)
    }

    /** A long press on the middle point removes it, with one commit. */
    @Test
    fun a_long_press_removes_a_middle_point() {
        settings = bentSettings()
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { longClick(spot(0.5f, 0.75f)) }
        compose.waitForIdle()

        assertEquals(1, commits)
        assertTrue(settings.curves.rgb.isNeutral)
    }

    /** With ten points a tap on empty space adds nothing and commits nothing. */
    @Test
    fun at_ten_points_a_tap_adds_nothing() {
        val ten = ToneCurve(
            List(ToneCurve.MAX_POINTS) { i ->
                val x = i / (ToneCurve.MAX_POINTS - 1).toFloat()
                CurvePoint(x, 0.25f + 0.5f * x)
            },
        )
        settings = EditSettings(curves = CurveAdjustments.NEUTRAL.with(CurveChannel.RGB, ten))
        show()
        compose.onNodeWithTag(CURVE_GRAPH_TAG).performTouchInput { click(spot(0.55f, 0.97f)) }
        compose.waitForIdle()

        assertEquals(0, commits)
        assertEquals(ToneCurve.MAX_POINTS, settings.curves.rgb.points.size)
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
