package com.phongkaster.kanso.a11y

import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.ui.settings.CUSTOM_COLOUR_PICKER_TAG
import java.util.Locale

/**
 * The one accessibility check every screen state is put through (DoD 18): a control that does
 * something must have a spoken name and be at least [MIN_SIZE] wide and tall.
 *
 * What counts as a control. The check reads semantics nodes, the description of the screen that
 * screen readers such as TalkBack and Compose tests read instead of pixels. A node is a control
 * when it can be tapped (it has the `OnClick` action), when it is a switch or a toggle (it has a
 * `ToggleableState`) or when it is one choice of a set (it has a `Selected` state, on or off).
 * The nodes should come from the *merged* tree, the one TalkBack reads: there a button and the
 * text and icon inside it are one node, so the button's name is the text inside it.
 *
 * The spoken name is the node's text or its content description, whichever is not blank. The
 * size is the largest of three measures, in each direction:
 * - `size`, the node's own laid-out size, never cut by the screen's edge, so a row scrolled half
 *   out of view still counts at its real size;
 * - `boundsInRoot`, where the node sits on screen, cut to what is visible;
 * - `touchBoundsInRoot`, the area a finger can hit. Compose widens the touch area of a small
 *   tappable node to the phone's minimum touch target (48 dp) by itself, and reports the widened
 *   area here, so a Material icon button drawn at 40 dp still offers a 48 dp target. This is what
 *   DoD 18 means by "max(boundsInRoot, touchBoundsInRoot)".
 *
 * Two kinds of node are left out, each for a stated reason:
 * - nodes inside the node tagged [CUSTOM_COLOUR_PICKER_TAG]: the engine's colour picker
 *   (`core/colorpicker`, whose source Kanso may not edit) sets no semantics of its own and draws
 *   controls under 48 dp, the one named exception of DoD 18;
 * - nodes that are not placed on the screen at all (a layout measured them but never put them
 *   anywhere, such as a button group's "more" button while every button fits). Nobody can see
 *   or reach such a node, and TalkBack skips it too.
 *
 * The check only reports; the test that calls it decides what a report means.
 */
object TouchTargetCheck {

    /** The smallest width and height a control may have: Android's recommended touch target. */
    val MIN_SIZE: Dp = 48.dp

    /** The words a report uses when a control has nothing a screen reader could say for it. */
    const val NO_NAME = "has no spoken name"

    /** The words a report uses when a control is narrower or shorter than the minimum. */
    const val TOO_SMALL = "is smaller than"

    /**
     * Half a pixel of slack when comparing sizes: a size in dp becomes a whole number of pixels,
     * so a control of exactly 48 dp may come out up to half a pixel short of 48 dp in pixels.
     */
    private const val ROUNDING_SLACK_PX = 0.5f

    /** Tells whether [node] is a control: it can be tapped, toggled or selected. */
    fun isControl(node: SemanticsNode): Boolean {
        val config = node.config
        return SemanticsActions.OnClick in config ||
            SemanticsProperties.ToggleableState in config ||
            SemanticsProperties.Selected in config
    }

    /**
     * Returns the nodes of [nodes] the check applies to: the controls that are placed on the
     * screen and are not inside the engine's colour picker (see the KDoc of this object).
     */
    fun controlsToCheck(nodes: List<SemanticsNode>): List<SemanticsNode> =
        nodes.filter { isControl(it) && isPlaced(it) && !isInsideColourPicker(it) }

    /**
     * Checks every control of [nodes] (see [controlsToCheck]) and returns one line per control
     * that fails, naming its test tag, role, text, description and measured size, and what is
     * wrong: [NO_NAME], or [TOO_SMALL] [minSize]. [density] turns dp into pixels. An empty list
     * means every control passed.
     */
    fun violations(nodes: List<SemanticsNode>, density: Density, minSize: Dp = MIN_SIZE): List<String> {
        val minPx = with(density) { minSize.toPx() }
        return controlsToCheck(nodes).mapNotNull { node ->
            val problems = mutableListOf<String>()
            if (spokenName(node) == null) {
                problems += "$NO_NAME (no text, no content description)"
            }
            val (widthPx, heightPx) = measuredSizePx(node)
            if (widthPx + ROUNDING_SLACK_PX < minPx || heightPx + ROUNDING_SLACK_PX < minPx) {
                problems += "$TOO_SMALL $minSize in at least one direction"
            }
            if (problems.isEmpty()) null else "${describe(node, density)} ${problems.joinToString("; ")}"
        }
    }

    /**
     * Returns what a screen reader would say as [node]'s name: its first non-blank content
     * description, else its first non-blank text, or null when it has neither.
     */
    fun spokenName(node: SemanticsNode): String? =
        (descriptionsOf(node) + textsOf(node)).firstOrNull { it.isNotBlank() }

    /**
     * Returns [node]'s width and height in pixels as the check measures them: in each direction
     * the largest of its laid-out size, its bounds on screen and its touch area (see the KDoc of
     * this object for why all three).
     */
    fun measuredSizePx(node: SemanticsNode): Pair<Float, Float> {
        val bounds = node.boundsInRoot
        val touch = node.touchBoundsInRoot
        val width = maxOf(node.size.width.toFloat(), bounds.width, touch.width)
        val height = maxOf(node.size.height.toFloat(), bounds.height, touch.height)
        return width to height
    }

    /**
     * Describes [node] for a report: its test tag, its role, its texts, its content descriptions
     * and its measured size in dp, so a failing node can be found in the code.
     */
    fun describe(node: SemanticsNode, density: Density): String {
        val config = node.config
        val tag = config.getOrNull(SemanticsProperties.TestTag) ?: "no tag"
        val role = config.getOrNull(SemanticsProperties.Role)?.toString() ?: "no role"
        val (widthPx, heightPx) = measuredSizePx(node)
        val width = with(density) { widthPx.toDp().value }
        val height = with(density) { heightPx.toDp().value }
        val size = String.format(Locale.ROOT, "%.1f x %.1f dp", width, height)
        return "control [tag: $tag, role: $role, text: ${textsOf(node)}, " +
            "description: ${descriptionsOf(node)}, size: $size]"
    }

    /** Returns the texts [node] carries (a merged button carries the text inside it). */
    private fun textsOf(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    /** Returns the content descriptions [node] carries (a merged icon button carries its icon's). */
    private fun descriptionsOf(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()

    /**
     * Tells whether [node] is the engine's colour picker or lies inside it: walks up the semantics
     * tree through each node's parent, looking for the [CUSTOM_COLOUR_PICKER_TAG] test tag.
     */
    private fun isInsideColourPicker(node: SemanticsNode): Boolean {
        var current: SemanticsNode? = node
        while (current != null) {
            if (current.config.getOrNull(SemanticsProperties.TestTag) == CUSTOM_COLOUR_PICKER_TAG) return true
            current = current.parent
        }
        return false
    }

    /**
     * Tells whether [node]'s layout, and every layout around it up to the screen's root, has
     * been placed. A layout that was only measured, never placed, is not on the screen.
     */
    private fun isPlaced(node: SemanticsNode): Boolean {
        var info: LayoutInfo? = node.layoutInfo
        while (info != null) {
            if (!info.isPlaced) return false
            info = info.parentInfo
        }
        return true
    }
}
