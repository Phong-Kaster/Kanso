package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * The Curves sheet, inside the 188 dp the edit sheet leaves for its content: a 48 dp row with the
 * Reset button at its end, and the graph below it filling the width and the remaining height
 * (about 132 dp). The graph is where a person bends the master (RGB) curve: it starts as a
 * straight diagonal and a drag on it moves a point.
 *
 * Only the master curve is edited here ([CurveChannel.RGB]). The graph reports a change as a new
 * [ToneCurve]; this panel puts it into a copy of [settings] with `curves.with(...)`, so the caller
 * only deals with whole [EditSettings] values, like every other tool.
 *
 * - [onPreview] is called while a point is being dragged: the photo follows the finger, no Undo
 *   step yet.
 * - [onCommit] is called once when the gesture ends, making the whole drag one Undo step.
 * - Reset is disabled while the curve is already straight; pressing it hands [onChange] the
 *   settings with a straight master curve, which is one commit.
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
) {
    val channel = CurveChannel.RGB
    val curve = settings.curves.curve(channel)
    // The same settings with the master curve replaced: what a drag or Reset hands upward.
    /**
     * Returns the settings with the master curve replaced by [changed]. It keeps drag and Reset
     * handing upward one whole settings object, so the ViewModel stays the only place that stores state.
     */
    fun withCurve(changed: ToneCurve) = settings.copy(curves = settings.curves.with(channel, changed))

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The empty space pushes Reset to the end of the row.
            Spacer(modifier = Modifier.weight(1f))
            CurveResetButton(
                enabled = enabled && !curve.isNeutral,
                onClick = { onChange(withCurve(ToneCurve.NEUTRAL)) },
            )
        }
        CurveGraph(
            curve = curve,
            description = stringResource(
                R.string.edit_curves_graph_description,
                stringResource(R.string.edit_curves_channel_rgb),
                curve.points.size,
            ),
            lineColor = MaterialTheme.colorScheme.primary,
            onPreview = { changed -> if (enabled) onPreview(withCurve(changed)) },
            onCommit = { changed ->
                if (enabled) {
                    // Hand over the final curve first, so the commit keeps exactly what was drawn.
                    onPreview(withCurve(changed))
                    onCommit()
                }
            },
            modifier = Modifier.weight(1f).fillMaxSize(),
        )
    }
}

/** The sheet with a bent master curve, in light and in dark. */
@PreviewLightDark
@Composable
private fun CurvesPanelPreview() {
    val bent = ToneCurve.NEUTRAL.added(CurvePoint(0.5f, 0.7f))
    val settings = EditSettings().let { it.copy(curves = it.curves.with(CurveChannel.RGB, bent)) }
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            CurvesPanel(
                settings = settings,
                onPreview = {},
                onCommit = {},
                onChange = {},
                modifier = Modifier.height(188.dp),
            )
        }
    }
}
