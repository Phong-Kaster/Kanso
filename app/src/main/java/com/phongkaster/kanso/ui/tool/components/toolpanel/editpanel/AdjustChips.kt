package com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel

import androidx.annotation.DrawableRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.AdjustSlider

/** Returns the test tag of [slider]'s chip in the Adjust panel, such as `edit-adjust-CONTRAST`. */
fun editAdjustChipTag(slider: AdjustSlider): String = "edit-adjust-${slider.name}"

/**
 * One row of Adjust's sub-tool chips (Brightness, Contrast, Saturation, Warmth), scrolling
 * sideways if the screen is narrow. The [chosen] one is marked selected; a tap calls [onChoose]
 * with the chip's sub-tool, whose slider then takes the other's place under the row. Choosing a
 * chip changes nothing in the photo. While [enabled] is false every chip is disabled.
 */
@Composable
internal fun AdjustChips(chosen: AdjustSlider, enabled: Boolean, onChoose: (AdjustSlider) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdjustSlider.entries.forEach { slider ->
            EditFeatureTile(
                icon = adjustIcon(slider),
                title = slider.labelRes,
                onClick = { onChoose(slider) },
                selected = slider == chosen,
                enabled = enabled,
                modifier = Modifier.testTag(editAdjustChipTag(slider)),
            )
        }
    }
}

/** The icon drawn on [slider]'s tile in Adjust. */
@DrawableRes
private fun adjustIcon(slider: AdjustSlider): Int = when (slider) {
    AdjustSlider.BRIGHTNESS -> R.drawable.ic_brightness
    AdjustSlider.CONTRAST -> R.drawable.ic_contrast
    AdjustSlider.SATURATION -> R.drawable.ic_saturation
    AdjustSlider.WARMTH -> R.drawable.ic_warmth
}

/** The Adjust chips with the first slider chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun AdjustChipsPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        AdjustChips(chosen = AdjustSlider.entries.first(), enabled = true, onChoose = {})
    }
}
