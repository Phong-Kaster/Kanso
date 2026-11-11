package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.edit.edit.CurvePoint
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.ToneCurve
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.model.RgbChannel

/**
 * The Curves sheet, inside the 188 dp the edit sheet leaves for its content: a 48 dp row of chips,
 * and below it the graph filling the remaining height (about 136 dp) and the width the Reset button
 * leaves. The delete (trash) and Reset buttons sit in a 48 dp column to the right of the graph, delete
 * above Reset. The graph is where a person bends a curve: it starts
 * as a straight diagonal; a tap or drag on the line adds a point and selects it, and the trash
 * button above Reset deletes the selected point (while more than two remain).
 *
 * Reset is not in the chips' row because the chips and Reset do not fit side by side on a
 * phone (the owner once saw a chip name wrap and Reset vanish): the four chips share the whole width
 * equally and never scroll, and Reset can never be pushed off the sheet.
 *
 * The top row holds the channel chips ([CurveChips]) choosing which curve is edited: [curveChannel], with
 * [onCurveChannel] called on a chip tap (which changes nothing in the photo). The graph reports a
 * change as a new [ToneCurve]; this panel puts it into a copy of [settings] with
 * `curves.with(channel, ...)`, so the caller only deals with whole [EditSettings] values, like every
 * other tool. Reset only straightens the chosen curve.
 *
 * - [onPreview] is called while a point is being dragged: the photo follows the finger, no Undo
 *   step yet.
 * - [onCommit] is called once when the gesture ends, making the whole drag one Undo step.
 * - Reset is disabled while the chosen curve is already straight; pressing it hands [onChange] the
 *   settings with the chosen curve ([curveChannel]) straightened and the other three untouched,
 *   which is one commit.
 *
 * While [enabled] is false (Compare is on, or Done is rendering) Reset is disabled and the graph's
 * edits are ignored, so nothing changes under the render.
 *
 * [settings] is only read here. The one thing this panel remembers is which point is selected (UI-only
 * state, never part of the photo's settings), so the preview shows it from sample settings.
 */
@Composable
internal fun CurvesPanel(
    settings: EditSettings,
    onPreview: (EditSettings) -> Unit,
    onCommit: () -> Unit,
    onChange: (EditSettings) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    curveChannel: CurveChannel = CurveChannel.RGB,
    onCurveChannel: (CurveChannel) -> Unit = {},
) {
    val channel = curveChannel
    val curve = settings.curves.curve(channel)
    /**
     * Returns the settings with the chosen channel's curve replaced by [changed]. It keeps drag and
     * Reset handing upward one whole settings object, so the ViewModel stays the only place that
     * stores state.
     */
    fun withCurve(changed: ToneCurve) = settings.copy(curves = settings.curves.with(channel, changed))

    // The selected point's index, or null. Keyed on the channel, so opening another channel clears it.
    var selected by rememberSaveable(channel) { mutableStateOf<Int?>(null) }
    // Every settings value this panel handed up and has not yet seen come back. Settings arriving that
    // are not in the list were changed by somebody else (Undo, Redo, Reset, another tool), which clears
    // the selection. A list rather than one value, because during a fast drag the screen can recompose
    // with an older preview than the newest one handed up; that is still ours and must not clear it.
    val handed = remember(channel) { mutableListOf(settings) }
    LaunchedEffect(settings) {
        val at = handed.indexOf(settings)
        if (at >= 0) {
            repeat(at) { handed.removeAt(0) }
        } else {
            selected = null
            handed.clear()
            handed.add(settings)
        }
    }
    // A stale index (the curve shrank) is never shown or deleted.
    val chosen = selected?.takeIf { it in curve.points.indices }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The chips have the first row to themselves. When Reset shared it, the chips' row
        // was measured first and took the whole width, so Reset was squeezed off the sheet.
        // A chip tap only calls onCurveChannel: nothing in the photo changes, no Undo step.
        CurveChips(
            chosen = channel,
            enabled = enabled,
            onChoose = onCurveChannel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        )
        // The graph and the button column share the rest of the height: the graph takes all the width Reset
        // leaves, and Reset sits in its own narrow column (in the column beside it), so they are
        // always on screen.
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            CurveGraph(
                curve = curve,
                description = stringResource(
                    R.string.edit_curves_graph_description,
                    stringResource(curveChannelLabelRes(channel)),
                    curve.points.size,
                ),
                lineColor = curveLineColor(channel),
                enabled = enabled,
                selected = chosen,
                onSelect = { selected = it },
                onPreview = { changed ->
                    if (enabled) {
                        handed.add(withCurve(changed))
                        onPreview(withCurve(changed))
                    }
                },
                onCommit = { changed ->
                    if (enabled) {
                        // Hand over the final curve first, so the commit keeps exactly what was drawn.
                        handed.add(withCurve(changed))
                        onPreview(withCurve(changed))
                        onCommit()
                    }
                },
                // A Row measures its unweighted children (Reset) first and gives the weighted graph
                // whatever width is left, so Reset can never be pushed out.
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            // The button column: delete on top, Reset below it. Both always fit, as the column is unweighted.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CurveDeleteButton(
                    enabled = enabled && chosen != null && curve.points.size > 2,
                    onClick = {
                        val index = chosen
                        if (index != null) {
                            val removed = curve.removed(index)
                            handed.add(withCurve(removed))
                            // The left neighbour becomes selected; deleting the first point selects the new first.
                            selected = (index - 1).coerceAtLeast(0)
                            onChange(withCurve(removed))
                        }
                    },
                )
                CurveResetButton(
                    enabled = enabled && !curve.isNeutral,
                    onClick = { onChange(withCurve(ToneCurve.NEUTRAL)) },
                )
            }
        }
    }
}

/** Returns the colour of [channel]'s curve line in the current theme; see [curveLineColorFor]. */
@Composable
internal fun curveLineColor(channel: CurveChannel): Color =
    curveLineColorFor(MaterialTheme.colorScheme, channel)

/**
 * Returns the colour of [channel]'s curve line in [scheme]: the theme's text colour for the master
 * (RGB) curve, and for Red, Green and Blue the same tint the RGB sheet uses ([RgbChannel.tintOn]),
 * chosen against `surfaceContainerLowest`, the colour the graph is drawn on, so it stays visible in
 * light and dark. It is a plain function so the contrast test can call it too.
 */
internal fun curveLineColorFor(scheme: ColorScheme, channel: CurveChannel): Color {
    val surface = scheme.surfaceContainerLowest
    return when (channel) {
        CurveChannel.RGB -> scheme.onSurface
        CurveChannel.RED -> RgbChannel.RED.tintOn(surface)
        CurveChannel.GREEN -> RgbChannel.GREEN.tintOn(surface)
        CurveChannel.BLUE -> RgbChannel.BLUE.tintOn(surface)
    }
}

/** The sheet with a bent Red curve, in light and in dark. */
@PreviewLightDark
@Composable
private fun CurvesPanelPreview() {
    val bent = ToneCurve.NEUTRAL.added(CurvePoint(0.5f, 0.7f))
    val settings = EditSettings().let { it.copy(curves = it.curves.with(CurveChannel.RED, bent)) }
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            CurvesPanel(
                settings = settings,
                onPreview = {},
                onCommit = {},
                onChange = {},
                curveChannel = CurveChannel.RED,
                modifier = Modifier.height(188.dp),
            )
        }
    }
}
