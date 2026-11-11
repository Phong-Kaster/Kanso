package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.ToneCurve
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag of the graph. */
internal const val CURVE_GRAPH_TAG = "curve-graph"

/**
 * The room, in dp, kept free around the plot on every side, so the end points (which sit exactly on
 * the plot's corners) are drawn whole and are easy to grab. Tests use it to find a point on screen.
 */
internal const val CURVE_GRAPH_PADDING_DP = 12

/** How near, in dp, a touch must be to a point to grab it. */
private const val HIT_RADIUS_DP = 24

/** The gesture ended with the finger lifted. */
private const val OUTCOME_UP = 1

/** The gesture moved beyond the touch slop (the start of a drag). */
private const val OUTCOME_MOVED = 2

/** The gesture was taken over by somebody else, or the finger vanished. */
private const val OUTCOME_CANCELLED = 3

/**
 * The curve editor: a wide plot (about twice as wide as tall) where the horizontal axis is the
 * brightness going in (left is black) and the vertical axis the brightness coming out (up is
 * brighter).
 *
 * It draws, from the back: a frame with three vertical lines (four columns), [curve] as a line
 * (flat beyond its first and last point), and each control point as a circle. All colours are
 * roles of the current Material theme, and the curve itself takes [lineColor].
 *
 * One touch handler does everything (Compose lets one `pointerInput` read the whole gesture):
 *
 * - **tap on a point** (within 24 dp) selects it ([onSelect]); a point wins over the line;
 * - **tap on the line** (within 24 dp of the drawn line) adds a point there, at the line's own
 *   height, selects it and reports [onCommit] once. At ten points nothing is added;
 * - **tap anywhere else** only clears the selection ([onSelect] with null);
 * - **drag from a point**, or from the line away from any point (which adds one first), selects that
 *   point and moves it. The two end points move sideways as well as up and down, and a point cannot
 *   pass its neighbours (the curve's own rules in [ToneCurve.moved]). [onPreview] is called at every
 *   step so the photo follows the finger, and [onCommit] once, with the final curve, when the finger
 *   lifts: the whole drag is one Undo step.
 *
 * A long press does nothing. The point at [selected] is drawn as background inside a ring of the
 * line's colour; the others as the line's colour inside a ring of the background.
 *
 * There is no double tap on purpose: waiting to tell a double tap from a tap would delay every tap.
 *
 * While a finger is down the graph draws its own copy of the curve, so the line follows the finger
 * even before the caller has handed a new [curve] back. [description] is what a screen reader says
 * (it names the curve, such as "Red curve, 3 points"). While [enabled] is false the graph still
 * draws but ignores every touch.
 */
@Composable
internal fun CurveGraph(
    curve: ToneCurve,
    description: String,
    lineColor: Color,
    onPreview: (ToneCurve) -> Unit,
    onCommit: (ToneCurve) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Int? = null,
    onSelect: (Int?) -> Unit = {},
) {
    // The curve being edited by a finger right now, or null when no gesture is running.
    var working by remember { mutableStateOf<ToneCurve?>(null) }
    // The long-lived pointer handler below must always see the newest values, not its first ones.
    val latestCurve by rememberUpdatedState(curve)
    val latestPreview by rememberUpdatedState(onPreview)
    val latestCommit by rememberUpdatedState(onCommit)
    val latestSelect by rememberUpdatedState(onSelect)

    val density = LocalDensity.current
    val padPx = with(density) { CURVE_GRAPH_PADDING_DP.dp.toPx() }
    val hitPx = with(density) { HIT_RADIUS_DP.dp.toPx() }
    val pointRadiusPx = with(density) { 7.dp.toPx() }
    val strokePx = with(density) { 3.dp.toPx() }
    val hairlinePx = with(density) { 1.dp.toPx() }

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val ringColor = MaterialTheme.colorScheme.surfaceContainerLowest
    // While disabled the graph shows only the caller's curve, never a finger's working copy.
    val shown = if (enabled) working ?: curve else curve

    Canvas(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(12.dp))
            .testTag(CURVE_GRAPH_TAG)
            .semantics { contentDescription = description }
            // Keyed on [enabled]: switching it restarts the handler, which drops any gesture in
            // progress; while disabled it installs no handler, so every touch is ignored.
            .pointerInput(enabled) {
                // A gesture cut short (the handler restarts when [enabled] flips) must not leave the
                // finger's working copy behind, or the line would stay where the finger left it.
                working = null
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val plot = Size(size.width.toFloat(), size.height.toFloat())
                    val start = latestCurve
                    // The nearest point within reach of the finger, if any.
                    val hit = start.points.indices
                        .minByOrNull { (toScreen(start.points[it], plot, padPx) - down.position).getDistance() }
                        ?.takeIf { (toScreen(start.points[it], plot, padPx) - down.position).getDistance() <= hitPx }
                    // Whether the finger landed on the drawn line (only matters when no point was hit).
                    val onLine = hit == null &&
                        distanceToLine(start, down.position, plot, padPx, hitPx) <= hitPx
                    val grabs = hit != null || (onLine && start.points.size < ToneCurve.MAX_POINTS)

                    // Phase one: wait to see what the finger does. Lifting means a tap, moving beyond the
                    // touch slop means a drag. There is no long press any more, so there is no timeout.
                    var lastPosition = down.position
                    var outcome: Int? = null
                    while (outcome == null) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        outcome = when {
                            change == null || change.isConsumed -> OUTCOME_CANCELLED
                            change.changedToUpIgnoreConsumed() -> OUTCOME_UP
                            (change.position - down.position).getDistance() > viewConfiguration.touchSlop -> {
                                lastPosition = change.position
                                // Claim a drag that starts on a point or the line, so a parent that
                                // scrolls vertically does not take it over.
                                if (grabs) change.consume()
                                OUTCOME_MOVED
                            }
                            else -> null
                        }
                    }

                    when (outcome) {
                        OUTCOME_UP -> {
                            if (hit != null) {
                                // A tap on a point selects it; the curve does not change.
                                latestSelect(hit)
                            } else if (onLine) {
                                // A tap on the line adds a point on the line (at the line's own height,
                                // not the finger's) and selects it: one commit.
                                val added = addOnLine(start, down.position, plot, padPx)
                                if (added != null) {
                                    latestCommit(added.first)
                                    latestSelect(added.second)
                                }
                            } else {
                                // A tap on empty space only clears the selection.
                                latestSelect(null)
                            }
                            down.consume()
                        }
                        OUTCOME_MOVED -> {
                            // The curve to drag and the index of the point the finger carries: a point
                            // under the finger, or a new point added on the line where the finger landed.
                            var work = start
                            var index: Int? = hit
                            if (hit == null && onLine) {
                                val added = addOnLine(start, down.position, plot, padPx)
                                if (added != null) {
                                    work = added.first
                                    index = added.second
                                }
                            }
                            val grabbed = index
                            if (grabbed != null) {
                                latestSelect(grabbed)
                                /**
                                 * Moves the grabbed point to the finger's [position] and reports the new curve.
                                 * It runs on every drag event so the preview updates while the finger moves.
                                 */
                                fun follow(position: Offset) {
                                    val at = fromScreen(position, plot, padPx)
                                    work = work.moved(grabbed, at.x, at.y)
                                    working = work
                                    latestPreview(work)
                                }
                                follow(lastPosition)
                                drag(down.id) { change ->
                                    follow(change.position)
                                    change.consume()
                                }
                                // One commit with the final curve: the drag is one Undo step.
                                if (work != start) latestCommit(work)
                                working = null
                            } else {
                                // A swipe over empty space does nothing; let the finger finish.
                                waitForUpOrCancellation()
                            }
                        }
                        else -> Unit
                    }
                }
            },
    ) {
        val plot = Size(size.width, size.height)
        // The frame, and three vertical lines inside it (four columns). No horizontal lines and no
        // diagonal guide: the plot stays quiet so the curve is what the eye finds.
        drawRect(
            gridColor,
            topLeft = Offset(padPx, padPx),
            size = Size(plot.width - 2 * padPx, plot.height - 2 * padPx),
            style = Stroke(width = hairlinePx),
        )
        for (i in 1 until GRID_COLUMNS) {
            val x = padPx + i / GRID_COLUMNS.toFloat() * (plot.width - 2 * padPx)
            drawLine(gridColor, Offset(x, padPx), Offset(x, plot.height - padPx), hairlinePx)
        }
        // The curve: its 256-entry table drawn as one connected line.
        val table = shown.table()
        val path = Path()
        table.forEachIndexed { i, value ->
            val at = toScreen(CurvePoint(i / (table.size - 1).toFloat(), value / 255f), plot, padPx)
            if (i == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
        }
        drawPath(path, lineColor, style = Stroke(width = strokePx))
        // The control points, all in one style: filled with the line colour, ringed in the sheet's
        // lowest surface colour so each stands out from the line.
        shown.points.forEachIndexed { i, point ->
            val centre = toScreen(point, plot, padPx)
            val outer = pointRadiusPx + hairlinePx * 2
            if (i == selected) {
                // The selected point is the reverse: background inside a thicker ring of the line's colour.
                drawCircle(lineColor, outer, centre)
                drawCircle(ringColor, outer - strokePx, centre)
            } else {
                drawCircle(ringColor, outer, centre)
                drawCircle(lineColor, pointRadiusPx, centre)
            }
        }
    }
}

/**
 * How far, in pixels, [position] is from the drawn line of [curve] (flat end parts included), looking
 * only [reach] pixels to each side. It samples the curve every 2 px and returns the smallest distance,
 * or [Float.MAX_VALUE] when the plot has no x inside that window. [ToneCurve.valueAt] gives the height.
 */
private fun distanceToLine(curve: ToneCurve, position: Offset, plot: Size, pad: Float, reach: Float): Float {
    var best = Float.MAX_VALUE
    var x = (position.x - reach).coerceAtLeast(pad)
    val end = (position.x + reach).coerceAtMost(plot.width - pad)
    while (x <= end) {
        val cx = fromScreen(Offset(x, 0f), plot, pad).x
        val on = toScreen(CurvePoint(cx, curve.valueAt(cx)), plot, pad)
        best = minOf(best, (on - position).getDistance())
        x += 2f
    }
    return best
}

/**
 * Adds a point on [curve]'s line at the x of [position], at the line's own height. Returns the new
 * curve with the new point's index after sorting, or null when nothing was added (ten points already,
 * or too close to a neighbour).
 */
private fun addOnLine(curve: ToneCurve, position: Offset, plot: Size, pad: Float): Pair<ToneCurve, Int>? {
    val x = fromScreen(position, plot, pad).x
    val added = curve.added(CurvePoint(x, curve.valueAt(x)))
    if (added == curve) return null
    val index = added.points.indices.minByOrNull { kotlin.math.abs(added.points[it].x - x) } ?: return null
    return added to index
}

/** The plot is divided into this many columns by vertical lines (so three lines inside the frame). */
private const val GRID_COLUMNS = 4

/** Where [point] is on screen: x grows to the right, y is flipped because up is brighter. */
private fun toScreen(point: CurvePoint, plot: Size, pad: Float): Offset = Offset(
    pad + point.x * (plot.width - 2 * pad),
    pad + (1f - point.y) * (plot.height - 2 * pad),
)

/** The curve coordinates (0..1 each) of a place on screen; the inverse of [toScreen], kept inside the plot. */
private fun fromScreen(position: Offset, plot: Size, pad: Float): CurvePoint = CurvePoint(
    ((position.x - pad) / (plot.width - 2 * pad)).coerceIn(0f, 1f),
    (1f - (position.y - pad) / (plot.height - 2 * pad)).coerceIn(0f, 1f),
)

/** The graph with a bent curve, in light and in dark. */
@PreviewLightDark
@Composable
private fun CurveGraphPreview() {
    val bent = ToneCurve.NEUTRAL.added(CurvePoint(0.3f, 0.15f)).added(CurvePoint(0.7f, 0.85f))
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box(modifier = Modifier.fillMaxSize()) {
                CurveGraph(
                    curve = bent,
                    description = "Blue curve, 4 points",
                    lineColor = curveLineColor(CurveChannel.BLUE),
                    onPreview = {},
                    onCommit = {},
                    modifier = Modifier.width(320.dp).height(140.dp),
                )
            }
        }
    }
}
