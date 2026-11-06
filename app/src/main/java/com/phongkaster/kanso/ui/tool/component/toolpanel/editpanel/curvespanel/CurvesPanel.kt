package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

/**
 * The Curves sheet, inside the 188 dp the edit sheet leaves for its content: a 48 dp row of chips,
 * and below it the graph filling the remaining height (about 136 dp) and the width the Reset button
 * leaves; Reset sits at the graph's top right. The graph is where a person bends a curve: it starts
 * as a straight diagonal and a drag on it moves a point.
 *
 * Reset is not in the chips' row because the five chips and Reset do not fit side by side on a
 * phone (the owner saw "Lightness" wrap and Reset vanish): the chips get the whole width, scroll
 * sideways only if even that is too narrow, and Reset can never be pushed off the sheet.
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
 *   settings with the chosen curve ([curveChannel]) straightened and the other four untouched,
 *   which is one commit.
 *
 * While [enabled] is false (Compare is on, or Done is rendering) Reset is disabled and the graph's
 * edits are ignored, so nothing changes under the render.
 *
 * [settings] is only read here: this composable keeps no state, so it has a preview.
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The chips have the first row to themselves. When Reset shared it, the chips' scrolling row
        // was measured first and took the whole width, so Reset was squeezed off the sheet.
        // A chip tap only calls onCurveChannel: nothing in the photo changes, no Undo step.
        CurveChips(
            chosen = channel,
            enabled = enabled,
            onChoose = onCurveChannel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        )
        // The graph and Reset share the rest of the height: the graph takes all the width Reset
        // leaves, and Reset sits at the top of its own narrow column, so it is always on screen.
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
                onPreview = { changed -> if (enabled) onPreview(withCurve(changed)) },
                onCommit = { changed ->
                    if (enabled) {
                        // Hand over the final curve first, so the commit keeps exactly what was drawn.
                        onPreview(withCurve(changed))
                        onCommit()
                    }
                },
                // A Row measures its unweighted children (Reset) first and gives the weighted graph
                // whatever width is left, so Reset can never be pushed out.
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            CurveResetButton(
                enabled = enabled && !curve.isNeutral,
                onClick = { onChange(withCurve(ToneCurve.NEUTRAL)) },
            )
        }
    }
}

/**
 * Returns the colour of [channel]'s curve line: the theme's text colour for the master (RGB) and Lightness curves, and a
 * clearly red, green or blue tone for the others. The tone is darker on a light sheet and lighter
 * on a dark one, so it stays visible against either background.
 */
@Composable
internal fun curveLineColor(channel: CurveChannel): Color {
    val scheme = MaterialTheme.colorScheme
    val hue = when (channel) {
        CurveChannel.RGB, CurveChannel.LIGHTNESS -> return scheme.onSurface
        CurveChannel.RED -> 0f
        CurveChannel.GREEN -> 120f
        CurveChannel.BLUE -> 220f
    }
    val darkSheet = scheme.surfaceContainerHigh.luminance() < 0.5f
    return if (darkSheet) Color.hsv(hue, 0.55f, 1f) else Color.hsv(hue, 0.95f, 0.75f)
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
