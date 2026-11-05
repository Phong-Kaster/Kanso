package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.phongkaster.kanso.data.edit.edit.HslChannel
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag of the row that holds the three channel chips. */
const val HSL_CHIPS_TAG = "hsl-chips"

/** Returns the test tag of [channel]'s chip in the HSL sheet, such as `hsl-chip-HUE`. */
fun hslChipTag(channel: HslChannel): String = "hsl-chip-${channel.name}"

/**
 * One row of three single-choice chips, Hue, Saturation and Lightness, that picks which channel the
 * HSL slider changes. The [selected] one is marked; a tap calls [onSelected] with that chip's
 * channel. Choosing a chip changes nothing in the photo.
 *
 * The row is 32 dp tall in the layout and so is each chip. A Material chip enlarges its touch area
 * to 48 dp by itself (the "minimum touch target") without taking more room, so every chip is still
 * at least 48 dp to touch. While [enabled] is false every chip is disabled.
 */
@Composable
internal fun HslChips(
    selected: HslChannel,
    onSelected: (HslChannel) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .testTag(HSL_CHIPS_TAG),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HslChannel.entries.forEach { channel ->
            FilterChip(
                selected = channel == selected,
                onClick = { onSelected(channel) },
                enabled = enabled,
                label = { Text(text = stringResource(channel.labelRes()), maxLines = 1) },
                modifier = Modifier.height(32.dp).testTag(hslChipTag(channel)),
            )
        }
    }
}

/** The chips with Saturation chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun HslChipsPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HslChips(selected = HslChannel.SATURATION, onSelected = {})
        }
    }
}
