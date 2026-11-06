package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow.CELL_SIZE
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow.ColourRangeCircle
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow.TOUCH_SIZE
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow.hslRangeTag
import com.phongkaster.kanso.ui.tool.model.RgbChannel

/**
 * Returns the test tag of [channel]'s circle in the RGB panel. RGB draws HSL's own circle for the
 * channel's colour range (see [RgbChannel.range]), so the tag is that circle's, such as
 * `hsl-range-GREEN`; only one of the two sheets is ever on screen, so the tags never meet.
 */
fun editRgbChipTag(channel: RgbChannel): String = hslRangeTag(channel.range)

/** How far a circle's 48 dp touch area reaches past its 32 dp cell on each side: 8 dp. */
private val TOUCH_OVERFLOW = (TOUCH_SIZE - CELL_SIZE) / 2

/**
 * One row of RGB's three channel circles (Red, Green, Blue), drawn exactly like the HSL sheet's
 * colour-range circles ([ColourRangeCircle]) and in the same colours: each channel shows HSL's
 * circle for its [RgbChannel.range], so red, green and blue look the same in both tools. There is
 * no text; a screen reader still hears each circle's name ("Red", "Green", "Blue"), and the
 * chosen channel's name is shown beside the slider under the row.
 *
 * The [chosen] circle carries HSL's ring and check mark; a tap calls [onChoose] with that channel,
 * whose slider then takes the other's place under the row. Choosing a circle changes nothing in
 * the photo. While [enabled] is false every circle is disabled.
 *
 * Like HSL's row, the row is 32 dp tall and padded 8 dp at each end, so the first circle's 48 dp
 * touch area stays whole.
 */
@Composable
internal fun RgbChips(chosen: RgbChannel, enabled: Boolean, onChoose: (RgbChannel) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CELL_SIZE)
            .padding(horizontal = TOUCH_OVERFLOW),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            RgbChannel.entries.forEach(
                action = { channel ->
                    ColourRangeCircle(
                        range = channel.range,
                        chosen = channel == chosen,
                        onClick = { onChoose(channel) },
                        enabled = enabled,
                    )
                },
            )
        },
    )
}

/** The RGB circles with Red chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun RgbChipsPreview() {
    KansoTheme(
        themeMode = ThemeMode.System,
        content = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = { RgbChips(chosen = RgbChannel.RED, enabled = true, onChoose = {}) },
            )
        },
    )
}
