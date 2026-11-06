package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Returns the test tag of [channel]'s chip, such as `curve-chip-RED`. */
internal fun curveChipTag(channel: CurveChannel): String = "curve-chip-${channel.name}"

/** Returns the string resource holding [channel]'s spoken and written name. */
internal fun curveChannelLabelRes(channel: CurveChannel): Int = when (channel) {
    CurveChannel.RGB -> R.string.edit_curves_channel_rgb
    CurveChannel.RED -> R.string.edit_curves_channel_red
    CurveChannel.GREEN -> R.string.edit_curves_channel_green
    CurveChannel.BLUE -> R.string.edit_curves_channel_blue
}

/**
 * The row of four single-choice chips of the Curves sheet: RGB (the master curve), Red, Green and
 * Blue. The [chosen] chip is drawn selected (and a screen reader hears it as selected, which
 * Material's [FilterChip] exposes by itself); a tap calls [onChoose] with that channel. Choosing a
 * chip only changes which curve the graph shows: it never touches the photo and adds no Undo step.
 *
 * Each chip keeps Material's 48 dp minimum touch height. While [enabled] is false every chip is
 * disabled.
 */
@Composable
internal fun CurveChips(
    chosen: CurveChannel,
    enabled: Boolean,
    onChoose: (CurveChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurveChannel.entries.forEach { channel ->
            FilterChip(
                selected = channel == chosen,
                onClick = { onChoose(channel) },
                label = { Text(stringResource(curveChannelLabelRes(channel))) },
                enabled = enabled,
                modifier = Modifier.testTag(curveChipTag(channel)),
            )
        }
    }
}

/** The chips with Green chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun CurveChipsPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            CurveChips(chosen = CurveChannel.GREEN, enabled = true, onChoose = {})
        }
    }
}
