package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.HslChannel
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow.ColourRangeRow

/**
 * The HSL sheet: three rows of 32 dp with 4 dp between them (104 dp, exactly the room the 136 dp
 * edit sheet leaves for its content), so the whole sheet needs no scrolling. The circles and the
 * slider are 48 dp to touch: their touch areas overflow their 32 dp rows (see [ColourRangeRow] and
 * [HslSlider]) without changing the layout.
 *
 * 1. the nine colour-range circles ([ColourRangeRow]), the [range] chosen marked;
 * 2. three chips, Hue, Saturation and Lightness ([HslChips]), the [channel] chosen marked;
 * 3. one slider ([HslSlider]) that moves the chosen channel of the chosen range, from -100 to 100.
 *
 * It has no reset button. The slider shows `settings.hsl[range, channel]`, so reopening the sheet,
 * or choosing another range or chip, shows the value stored for that pair. [onRangeChosen] and
 * [onChannelChosen] change only what the sheet shows, never the photo. [onValueChange] is called
 * while the thumb moves with the new value; the caller turns it into new settings for a live
 * preview. [onValueChangeFinished] is the finger let go, which makes the whole drag one Undo step.
 *
 * While [enabled] is false (Compare is on, or Done is rendering) the chips and the slider are
 * disabled.
 *
 * [settings] is only read here: this composable keeps no state, so it has a preview.
 */
@Composable
internal fun HslPanel(
    settings: EditSettings,
    range: ColourRange,
    channel: HslChannel,
    onRangeChosen: (ColourRange) -> Unit,
    onChannelChosen: (HslChannel) -> Unit,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth().height(104.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ColourRangeRow(chosen = range, onChosen = onRangeChosen, enabled = enabled)
        // The circles' and the slider's 48 dp touch areas overflow their 32 dp rows into the
        // chips' row by 4 dp. Raising the chips makes a touch on a chip reach the chip first.
        HslChips(selected = channel, onSelected = onChannelChosen, enabled = enabled, modifier = Modifier.zIndex(1f))
        HslSlider(
            range = range,
            channel = channel,
            value = settings.hsl[range, channel],
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
        )
    }
}

/** The sheet with Light green and Saturation chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun HslPanelPreview() {
    val settings = EditSettings(hsl = EditSettings().hsl.with(ColourRange.LIGHT_GREEN, HslChannel.SATURATION, 0.4f))
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            HslPanel(
                settings = settings,
                range = ColourRange.LIGHT_GREEN,
                channel = HslChannel.SATURATION,
                onRangeChosen = {},
                onChannelChosen = {},
                onValueChange = {},
                onValueChangeFinished = {},
            )
        }
    }
}
