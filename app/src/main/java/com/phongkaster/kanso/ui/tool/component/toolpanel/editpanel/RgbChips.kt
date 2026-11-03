package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.model.RgbChannel

/** Returns the test tag of [channel]'s chip in the RGB panel, such as `edit-rgb-GREEN`. */
fun editRgbChipTag(channel: RgbChannel): String = "edit-rgb-${channel.name}"

/**
 * One row of RGB's channel chips (Red, Green, Blue), each with a dot in its channel's colour, the
 * way Adjust shows its sub-tool chips ([AdjustChips]). The [chosen] one is marked selected; a tap
 * calls [onChoose] with the chip's channel, whose slider then takes the other's place under the
 * row. Choosing a chip changes nothing in the photo. While [enabled] is false every chip is
 * disabled.
 */
@Composable
internal fun RgbChips(chosen: RgbChannel, enabled: Boolean, onChoose: (RgbChannel) -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            RgbChannel.entries.forEach(
                action = { channel ->
                    EditFeatureTile(
                        icon = R.drawable.ic_circle,
                        title = channel.labelRes,
                        onClick = { onChoose(channel) },
                        selected = channel == chosen,
                        enabled = enabled,
                        iconTint = channel.tintOn(surface),
                        modifier = Modifier.testTag(editRgbChipTag(channel)),
                    )
                },
            )
        },
    )
}

/** The RGB chips with Red chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun RgbChipsPreview() {
    KansoTheme(
        themeMode = ThemeMode.System,
        content = { RgbChips(chosen = RgbChannel.RED, enabled = true, onChoose = {}) },
    )
}
