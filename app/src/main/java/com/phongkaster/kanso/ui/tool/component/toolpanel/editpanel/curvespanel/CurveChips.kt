package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Returns the test tag of [channel]'s entry, such as `curve-chip-RED`. */
internal fun curveChipTag(channel: CurveChannel): String = "curve-chip-${channel.name}"

/** Returns the string resource holding [channel]'s spoken and written name (Red, Green, ...). */
internal fun curveChannelLabelRes(channel: CurveChannel): Int = when (channel) {
    CurveChannel.RGB -> R.string.edit_curves_channel_rgb
    CurveChannel.RED -> R.string.edit_curves_channel_red
    CurveChannel.GREEN -> R.string.edit_curves_channel_green
    CurveChannel.BLUE -> R.string.edit_curves_channel_blue
}

/** Returns the string resource holding [channel]'s short visible label: RGB, R, G or B. */
internal fun curveChannelShortRes(channel: CurveChannel): Int = when (channel) {
    CurveChannel.RGB -> R.string.edit_curves_short_rgb
    CurveChannel.RED -> R.string.edit_curves_short_red
    CurveChannel.GREEN -> R.string.edit_curves_short_green
    CurveChannel.BLUE -> R.string.edit_curves_short_blue
}

/**
 * The row of four single-choice entries of the Curves sheet: RGB (the master curve), R, G and B.
 * Each shows a coloured [CurveChannelDot] and a short label on one line; a screen reader hears the
 * full name (such as "Red"). The [chosen] entry sits on a soft pill and is exposed as selected; a
 * tap calls [onChoose] with that channel. Choosing only changes which curve the graph shows: it
 * never touches the photo and adds no Undo step.
 *
 * Every entry is at least 48 dp tall and shares the width equally. While [enabled] is false every
 * entry is dimmed and ignores touches.
 */
@Composable
internal fun CurveChips(
    chosen: CurveChannel,
    enabled: Boolean,
    onChoose: (CurveChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurveChannel.entries.forEach { channel ->
            val selected = channel == chosen
            val spoken = stringResource(curveChannelLabelRes(channel))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .alpha(if (enabled) 1f else 0.38f)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onChoose(channel) },
                    )
                    .testTag(curveChipTag(channel))
                    .semantics { contentDescription = spoken }
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CurveChannelDot(channel)
                Text(
                    text = stringResource(curveChannelShortRes(channel)),
                    style = MaterialTheme.typography.labelLarge,
                    // Matches the pill behind it: onSecondaryContainer on secondaryContainer.
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    // One line always: a label never wraps.
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

/** The entries with Green chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun CurveChipsPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            CurveChips(chosen = CurveChannel.GREEN, enabled = true, onChoose = {})
        }
    }
}
