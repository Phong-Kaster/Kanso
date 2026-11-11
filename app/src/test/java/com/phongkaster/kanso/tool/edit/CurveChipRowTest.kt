package com.phongkaster.kanso.tool.edit

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.a11y.TouchTargetCheck
import com.phongkaster.kanso.data.edit.edit.CurveAdjustments
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.ToneCurve
import com.phongkaster.kanso.ui.tool.component.editbottombar.EditSheet
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EditPanelContent
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.CURVE_GRAPH_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.CURVE_RESET_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.curveChannelLabelRes
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.curveChannelShortRes
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel.curveChipTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the Curves chip row after the Meitu-style rework: on every curve (RGB, Red, Green, Blue)
 * each of the four entries shows its short label (RGB, R, G, B) on one line and speaks its full
 * name (RGB, Red, Green, Blue); Reset is an icon button (no "Reset" text), fully visible and
 * tappable inside the sheet and the window, to the right of the graph; and the plot is between
 * 1.8 and 2.6 times as wide as it is tall. This runs on a 411 dp and a 360 dp wide phone, in light
 * and in dark, at the default text size. There is no Lightness chip any more.
 *
 * The owner once saw a long chip name wrap onto two lines and Reset pushed off the sheet.
 * `maxLines = 1` alone does not prove a fix, since a one-line label can still be cut short,
 * so each chip's text is checked through its laid-out text: one line holding the whole label, not
 * ellipsized, the drawn line starting and ending inside its box, its line not taller than its box,
 * and the box inside the chip; each chip's touch area is at least 48 x 48 dp. Reset is checked by
 * where it is (inside both the sheet and the window, right of the graph) and by its size (laid out
 * and touchable at 48 x 48 dp or more, so a squeezed Reset fails); a tap on it must straighten the
 * chosen bent curve.
 *
 * Text is measured with Robolectric's real drawing code (`GraphicsMode.Mode.NATIVE`): in the default
 * legacy mode a text's width is not its real width, so a too-long label would never wrap and the
 * test could not fail on the bug it guards against.
 *
 * Two of `TextLayoutResult`'s own overflow flags are deliberately not asserted, because the chip's
 * label is laid out with `softWrap = false`:
 * - `hasVisualOverflow` also includes `didExceedMaxLines`, which attempt 1 of T-006 saw true even
 *   for a three-letter "RGB" without any measurable clipping.
 * - `didOverflowWidth` compares the paragraph's width with the box, and a paragraph that may not
 *   wrap is laid out at a huge width (attempt 2 saw 2904 px for "RGB" in a 74 px box), so it is true
 *   for every label. For the same reason `multiParagraph.width` is not the text's width.
 * The width is judged instead by where the one drawn line starts and ends (`getLineLeft`,
 * `getLineRight`), which is what a person sees.
 *
 * The sheet is the real `EditSheet` around the stateless `EditPanelContent`, as in `CurvesSheetTest`.
 * The chosen curve is set directly rather than by tapping a chip: at 360 dp the last chip may
 * sit partly past the row's edge (the row scrolls), and a scroll in a test that does not need one
 * stalls the test clock.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CurveChipRowTest {

    /** Starts an empty activity to host the sheet. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The settings the sheet draws: every curve starts bent, so Reset is enabled on each. */
    private var settings by mutableStateOf(bentEverywhere())

    /** The curve the chips have chosen, as the ViewModel holds it in the app. */
    private var channel by mutableStateOf(CurveChannel.RGB)

    /** A 411 dp wide phone (the owner's Galaxy A13), light theme. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun at_411_dp_in_light_every_chip_is_one_line_and_reset_is_tappable() {
        checkEveryCurve(dark = false, allChipsFit = true)
    }

    /** A 411 dp wide phone, dark theme. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun at_411_dp_in_dark_every_chip_is_one_line_and_reset_is_tappable() {
        checkEveryCurve(dark = true, allChipsFit = true)
    }

    /** A 360 dp wide phone, light theme. */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun at_360_dp_in_light_every_chip_is_one_line_and_reset_is_tappable() {
        checkEveryCurve(dark = false, allChipsFit = false)
    }

    /** A 360 dp wide phone, dark theme. */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun at_360_dp_in_dark_every_chip_is_one_line_and_reset_is_tappable() {
        checkEveryCurve(dark = true, allChipsFit = false)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Draws the sheet, then for each of the four curves in turn: chooses it, checks every chip's
     * label and Reset's place, and taps Reset, which must straighten exactly that curve. With
     * [allChipsFit] every chip must also lie wholly inside the sheet (true at 411 dp, where the row
     * needs no scrolling; at 360 dp the owner accepted a row that scrolls).
     */
    private fun checkEveryCurve(dark: Boolean, allChipsFit: Boolean) {
        show(dark)
        CurveChannel.entries.forEach { curve ->
            compose.runOnIdle { channel = curve }
            compose.waitForIdle()
            compose.onNodeWithTag(curveChipTag(curve)).assertIsSelected()

            CurveChannel.entries.forEach { chip -> assertChipLabelIsWhole(chip, curve, allChipsFit) }
            assertResetIsVisibleAndTappable(curve)

            compose.onNodeWithTag(CURVE_RESET_TAG).performClick()
            compose.waitForIdle()
            assertTrue("Reset on $curve did not straighten it", settings.curves.curve(curve).isNeutral)
        }
        // Every curve was reset once, by its own Reset tap.
        assertEquals(EditSettings().curves, settings.curves)
    }

    /**
     * The [chip]'s text is its whole name on one line, neither ellipsized nor clipped, while [curve]
     * is chosen; with [mustFit] the chip also lies wholly inside the sheet.
     */
    private fun assertChipLabelIsWhole(chip: CurveChannel, curve: CurveChannel, mustFit: Boolean) {
        val name = compose.activity.getString(curveChannelShortRes(chip))
        val spoken = compose.activity.getString(curveChannelLabelRes(chip))
        assertEquals("the $chip chip's spoken name", SPOKEN.getValue(chip), spoken)
        compose.onNodeWithTag(curveChipTag(chip)).assertContentDescriptionEquals(spoken)
        val chipNode = compose.onNodeWithTag(curveChipTag(chip)).fetchSemanticsNode()
        val text = assertOneWholeLine(
            what = "the $chip chip with $curve chosen",
            name = name,
            ownerTag = curveChipTag(chip),
        )

        // The chip's surface clips to its shape, so the text's box must lie inside the chip.
        val where = "the $chip chip with $curve chosen"
        val chipBounds = boundsOf(chipNode)
        assertInside("$where: the text in its chip", boundsOf(text), chipBounds)
        // A chip is drawn 32 dp tall, but its touch area must reach 48 dp each way (DoD 14).
        assertTouchAreaIsAtLeast48Dp(where, chipNode)
        if (mustFit) {
            val sheet = boundsOf(compose.onNodeWithTag(SHEET_TAG).fetchSemanticsNode())
            assertInside("$where: the chip in the sheet", chipBounds, sheet)
        }
    }

    /**
     * Reset is displayed, wholly inside the sheet and the window, enabled on the bent [curve], and
     * clickable; it is not squeezed (laid out and touchable at 48 x 48 dp or more) and its label is
     * its whole name on one line.
     */
    private fun assertResetIsVisibleAndTappable(curve: CurveChannel) {
        val reset = compose.onNodeWithTag(CURVE_RESET_TAG)
        reset.assertIsDisplayed()
        reset.assertHasClickAction()
        reset.assertIsEnabled()

        val node = reset.fetchSemanticsNode()
        val bounds = boundsOf(node)
        val sheet = boundsOf(compose.onNodeWithTag(SHEET_TAG).fetchSemanticsNode())
        val root = compose.onRoot().fetchSemanticsNode().size
        val window = Rect(0f, 0f, root.width.toFloat(), root.height.toFloat())
        val where = "Reset with $curve chosen"
        assertInside("$where, in the sheet", bounds, sheet)
        assertInside("$where, in the window", bounds, window)

        // The touch area alone could hide a squeeze (Compose pads a small control's touch area out
        // to 48 dp), so Reset's own laid-out size must reach 48 dp too: the button asks for it.
        assertTouchAreaIsAtLeast48Dp(where, node)
        val minPx = minTouchPx()
        assertTrue(
            "$where: laid out ${node.size.width} x ${node.size.height} px, under 48 dp ($minPx px)",
            node.size.width + SLACK >= minPx && node.size.height + SLACK >= minPx,
        )

        // Reset is an icon: its spoken name is the word, but no text node shows it.
        val word = compose.activity.getString(R.string.edit_curves_reset)
        reset.assertContentDescriptionEquals(word)
        compose.onAllNodes(hasText(word), useUnmergedTree = true).assertCountEquals(0)

        // Reset sits to the right of the graph, not over it.
        val graph = boundsOf(compose.onNodeWithTag(CURVE_GRAPH_TAG).fetchSemanticsNode())
        assertTrue(
            "$where: $bounds is not to the right of the graph $graph",
            bounds.left + SLACK >= graph.right,
        )
        // The plot is wide, not squeezed into a narrow strip.
        val ratio = graph.width / graph.height
        assertTrue("$where: the plot is ${graph.width} x ${graph.height} px, ratio $ratio", ratio in 1.8f..2.6f)
    }

    /**
     * Finds the text node showing [name] under the node tagged [ownerTag] (in the unmerged tree,
     * since a chip or button merges its text into itself) and fails, naming [what], unless the text
     * is laid out as the whole of [name] on one line: not ellipsized, the drawn line starting and
     * ending inside its box, and its bottom not below the box. Returns the text node.
     *
     * `didOverflowWidth` and `hasVisualOverflow` are not used: they are true for every label laid
     * out with `softWrap = false` (see the class KDoc).
     */
    private fun assertOneWholeLine(what: String, name: String, ownerTag: String): SemanticsNode {
        val text = compose.onNode(
            hasText(name) and hasAnyAncestor(hasTestTag(ownerTag)),
            useUnmergedTree = true,
        ).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        val action = text.config[SemanticsActions.GetTextLayoutResult].action
        assertTrue("$what: the text has no layout", action != null && action(layouts))
        val layout = layouts.single()
        // The drawn line's extent: with softWrap = false the paragraph itself is far wider than the
        // line, so only the line's own left and right say how much room the name really takes.
        val lineLeft = layout.getLineLeft(0)
        val lineRight = layout.getLineRight(0)
        val lineWidth = lineRight - lineLeft
        // Every number that explains a failure, so a red test says which way the text spills.
        val where = "$what (box ${layout.size}, " +
            "line $lineLeft..$lineRight = $lineWidth px wide, bottom ${layout.getLineBottom(0)}, " +
            "lines ${layout.lineCount})"
        assertEquals("$where: the text is not the whole name", name, layout.layoutInput.text.text)
        assertEquals("$where: \"$name\" takes ${layout.lineCount} lines", 1, layout.lineCount)
        assertEquals("$where: the line does not hold all of \"$name\"", name.length, layout.getLineEnd(0))
        assertFalse("$where: \"$name\" is ellipsized", layout.isLineEllipsized(0))
        assertTrue("$where: \"$name\" starts left of its box", lineLeft >= -SLACK)
        assertTrue("$where: \"$name\" ends right of its box", lineRight <= layout.size.width + SLACK)
        assertTrue("$where: \"$name\" is wider than its box", lineWidth <= layout.size.width + SLACK)
        assertTrue(
            "$where: the line's bottom is below the box",
            layout.getLineBottom(0) <= layout.size.height + SLACK,
        )
        return text
    }

    /** Fails, naming [what], unless [node]'s touch area is at least 48 dp wide and 48 dp tall. */
    private fun assertTouchAreaIsAtLeast48Dp(what: String, node: SemanticsNode) {
        val touch = node.touchBoundsInRoot
        val minPx = minTouchPx()
        assertTrue(
            "$what: touch area ${touch.width} x ${touch.height} px, under 48 dp ($minPx px)",
            touch.width + SLACK >= minPx && touch.height + SLACK >= minPx,
        )
    }

    /** 48 dp, the smallest touch size DoD 14 allows, in this test screen's pixels. */
    private fun minTouchPx(): Float = with(Density(compose.activity)) { TouchTargetCheck.MIN_SIZE.toPx() }

    /** Fails with [what] unless [inner] lies wholly inside [outer] (half a pixel of rounding allowed). */
    private fun assertInside(what: String, inner: Rect, outer: Rect) {
        assertTrue(
            "$what: $inner is not inside $outer",
            inner.left + SLACK >= outer.left && inner.top + SLACK >= outer.top &&
                inner.right <= outer.right + SLACK && inner.bottom <= outer.bottom + SLACK,
        )
    }

    /** The bounds of [node] in the window, in pixels, from `positionInRoot` and its size (never `boundsInRoot`, which clips). */
    private fun boundsOf(node: SemanticsNode): Rect {
        val position = node.positionInRoot
        return Rect(
            position.x,
            position.y,
            position.x + node.size.width,
            position.y + node.size.height,
        )
    }

    /** Settings in which all four curves hold one middle point at (0.5, 0.75). */
    private fun bentEverywhere(): EditSettings {
        val bend = ToneCurve.NEUTRAL.added(CurvePoint(0.5f, 0.75f))
        val curves = CurveChannel.entries.fold(CurveAdjustments.NEUTRAL) { all, curve -> all.with(curve, bend) }
        return EditSettings(curves = curves)
    }

    /** Draws the Curves sheet inside the real edit sheet, in a light or [dark] colour scheme. */
    private fun show(dark: Boolean) {
        compose.setContent(
            composable = {
                MaterialTheme(
                    colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
                    content = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            EditSheet(
                                openTool = EditTool.CURVES,
                                modifier = Modifier.testTag(SHEET_TAG),
                                sheet = {
                                    EditPanelContent(
                                        openTool = EditTool.CURVES,
                                        settings = settings,
                                        enabled = true,
                                        onChange = { new -> settings = new },
                                        onPreview = { new -> settings = new },
                                        onCommit = {},
                                        onRatio = {},
                                        curveChannel = channel,
                                        onCurveChannel = { picked -> channel = picked },
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

    /** Names used only by this test. */
    private companion object {
        /** The test tag put on the edit sheet, whose bounds Reset and the chips must stay inside. */
        const val SHEET_TAG = "curve-row-test-sheet"

        /** Half a pixel of rounding allowed when comparing laid-out sizes and positions. */
        const val SLACK = 0.5f

        /** The names a screen reader speaks for the four chips. */
        val SPOKEN = mapOf(
            CurveChannel.RGB to "RGB",
            CurveChannel.RED to "Red",
            CurveChannel.GREEN to "Green",
            CurveChannel.BLUE to "Blue",
        )
    }
}
