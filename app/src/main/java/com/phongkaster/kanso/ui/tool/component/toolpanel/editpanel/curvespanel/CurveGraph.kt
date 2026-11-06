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
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.ToneCurve
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.withTimeoutOrNull

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
 * The curve editor: a square-ish plot where the horizontal axis is the brightness going in (left is
 * black) and the vertical axis the brightness coming out (up is brighter).
 *
 * It draws, from the back: a faint 4 by 4 grid, the straight diagonal as a guide (what "no change"
 * looks like), [curve] as a smooth line, and each control point as a circle. All colours are
 * roles of the current Material theme, and the curve itself takes [lineColor].
 *
 * One touch handler does everything (Compose lets one `pointerInput` read the whole gesture):
 *
 * - **tap on empty space** adds a point there, reported with [onCommit] once;
 * - **drag from a point** (the nearest one within 24 dp of the finger) moves it. The two end points
 *   move only up and down, and a middle point cannot pass its neighbours (the curve's own rules in
 *   [ToneCurve.moved]). [onPreview] is called at every step so the photo follows the finger, and
 *   [onCommit] once, with the final curve, when the finger lifts: the whole drag is one Undo step;
 * - **long press on a middle point** removes it, reported with [onCommit] once.
 *
 * There is no double tap on purpose: waiting to tell a double tap from a tap would delay every tap.
 *
 * While a finger is down the graph draws its own copy of the curve, so the line follows the finger
 * even before the caller has handed a new [curve] back. [description] is what a screen reader says.
 */
@Composable
internal fun CurveGraph(
    curve: ToneCurve,
    description: String,
    lineColor: Color,
    onPreview: (ToneCurve) -> Unit,
    onCommit: (ToneCurve) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The curve being edited by a finger right now, or null when no gesture is running.
    var working by remember { mutableStateOf<ToneCurve?>(null) }
    // The long-lived pointer handler below must always see the newest values, not its first ones.
    val latestCurve by rememberUpdatedState(curve)
    val latestPreview by rememberUpdatedState(onPreview)
    val latestCommit by rememberUpdatedState(onCommit)

    val density = LocalDensity.current
    val padPx = with(density) { CURVE_GRAPH_PADDING_DP.dp.toPx() }
    val hitPx = with(density) { HIT_RADIUS_DP.dp.toPx() }
    val pointRadiusPx = with(density) { 7.dp.toPx() }
    val strokePx = with(density) { 3.dp.toPx() }
    val hairlinePx = with(density) { 1.dp.toPx() }

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val guideColor = MaterialTheme.colorScheme.outline
    val ringColor = MaterialTheme.colorScheme.surface
    val shown = working ?: curve

    Canvas(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(12.dp))
            .testTag(CURVE_GRAPH_TAG)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val plot = Size(size.width.toFloat(), size.height.toFloat())
                    val start = latestCurve
                    // The nearest point within reach of the finger, if any.
                    val hit = start.points.indices
                        .minByOrNull { (toScreen(start.points[it], plot, padPx) - down.position).getDistance() }
                        ?.takeIf { (toScreen(start.points[it], plot, padPx) - down.position).getDistance() <= hitPx }

                    // Phase one: wait to see what the finger does. Lifting means a tap, moving beyond the
                    // touch slop means a drag, and staying still until the long-press time runs out
                    // means a long press (withTimeoutOrNull then gives null).
                    var lastPosition = down.position
                    val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var result: Int? = null
                        while (result == null) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            result = when {
                                change == null || change.isConsumed -> OUTCOME_CANCELLED
                                change.changedToUpIgnoreConsumed() -> OUTCOME_UP
                                (change.position - down.position).getDistance() > viewConfiguration.touchSlop -> {
                                    lastPosition = change.position
                                    // Claim a drag that starts on a point, so a parent that scrolls
                                    // vertically does not take it over.
                                    if (hit != null) change.consume()
                                    OUTCOME_MOVED
                                }
                                else -> null
                            }
                        }
                        result
                    }

                    when (outcome) {
                        OUTCOME_UP -> if (hit == null) {
                            // A tap on empty space: add a point where the finger was.
                            val added = start.added(fromScreen(down.position, plot, padPx))
                            if (added != start) latestCommit(added)
                            down.consume()
                        }
                        OUTCOME_MOVED -> if (hit != null) {
                            var work = start
                            /**
                             * Moves the grabbed point to the finger's [position] and reports the new curve.
                             * It runs on every drag event so the preview updates while the finger moves.
                             */
                            fun follow(position: Offset) {
                                val at = fromScreen(position, plot, padPx)
                                work = work.moved(hit, at.x, at.y)
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
                        OUTCOME_CANCELLED -> Unit
                        else -> {
                            // The finger stayed down: a long press. On a middle point it removes it.
                            if (hit != null) {
                                val removed = start.removed(hit)
                                if (removed != start) latestCommit(removed)
                                down.consume()
                            }
                            waitForUpOrCancellation()
                        }
                    }
                }
            },
    ) {
        val plot = Size(size.width, size.height)
        // The faint 4 by 4 grid: five lines each way, the outer ones being the plot's frame.
        for (i in 0..GRID_CELLS) {
            val fraction = i / GRID_CELLS.toFloat()
            val x = padPx + fraction * (plot.width - 2 * padPx)
            val y = padPx + fraction * (plot.height - 2 * padPx)
            drawLine(gridColor, Offset(x, padPx), Offset(x, plot.height - padPx), hairlinePx)
            drawLine(gridColor, Offset(padPx, y), Offset(plot.width - padPx, y), hairlinePx)
        }
        // The straight diagonal: what "no change" looks like.
        drawLine(
            guideColor,
            toScreen(CurvePoint(0f, 0f), plot, padPx),
            toScreen(CurvePoint(1f, 1f), plot, padPx),
            hairlinePx,
        )
        // The curve: its 256-entry table drawn as one connected line.
        val table = shown.table()
        val path = Path()
        table.forEachIndexed { i, value ->
            val at = toScreen(CurvePoint(i / (table.size - 1).toFloat(), value / 255f), plot, padPx)
            if (i == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
        }
        drawPath(path, lineColor, style = Stroke(width = strokePx))
        // The control points: a filled circle with a ring in the surface colour so it stands out.
        shown.points.forEach { point ->
            val centre = toScreen(point, plot, padPx)
            drawCircle(ringColor, pointRadiusPx + hairlinePx * 2, centre)
            drawCircle(lineColor, pointRadiusPx, centre)
        }
    }
}

/** The grid has this many cells along each side. */
private const val GRID_CELLS = 4

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
                    description = "RGB curve, 4 points",
                    lineColor = MaterialTheme.colorScheme.primary,
                    onPreview = {},
                    onCommit = {},
                    modifier = Modifier.width(320.dp).height(140.dp),
                )
            }
        }
    }
}
