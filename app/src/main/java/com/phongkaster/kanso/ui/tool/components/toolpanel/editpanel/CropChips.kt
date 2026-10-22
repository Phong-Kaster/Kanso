package com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Returns the test tag of [ratio]'s crop chip, such as `edit-ratio-SQUARE`. */
fun editRatioTag(ratio: CropRatio): String = "edit-ratio-${ratio.name}"

/**
 * One row of crop chips, scrolling sideways if the screen is narrow. Each is a filter chip marked
 * selected while it is the [chosen] shape: a fixed shape crops at once to the largest centred
 * rectangle of that shape and locks the cropper's rectangle to it; Free unlocks it, so its edges
 * move freely. A tap calls [onRatio] with the chip's shape; while [enabled] is false every chip is
 * disabled.
 */
@Composable
internal fun CropChips(chosen: CropRatio?, cropIsFull: Boolean, enabled: Boolean, onRatio: (CropRatio) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CropRatio.entries.forEach { ratio ->
            EditFeatureTile(
                icon = ratioIcon(ratio),
                title = ratioLabel(ratio),
                onClick = { onRatio(ratio) },
                // Original reads as chosen only while the whole photo is kept, not after the handles moved.
                selected = ratio == chosen && (ratio != CropRatio.ORIGINAL || cropIsFull),
                enabled = enabled,
                modifier = Modifier.testTag(editRatioTag(ratio)),
            )
        }
    }
}

/** The icon drawn on crop shape [ratio]'s tile: a rectangle of that shape ([CropRatio.FREE] shows the crop icon). */
@DrawableRes
private fun ratioIcon(ratio: CropRatio): Int = when (ratio) {
    CropRatio.ORIGINAL -> R.drawable.ic_refresh
    CropRatio.FREE -> R.drawable.ic_crop
    CropRatio.SQUARE -> R.drawable.ic_ratio_square
    CropRatio.FOUR_THREE -> R.drawable.ic_ratio_4_3
    CropRatio.THREE_FOUR -> R.drawable.ic_ratio_3_4
    CropRatio.SIXTEEN_NINE -> R.drawable.ic_ratio_16_9
    CropRatio.NINE_SIXTEEN -> R.drawable.ic_ratio_9_16
}

/** The string resource naming crop shape [ratio] on its tile. */
@StringRes
private fun ratioLabel(ratio: CropRatio): Int = when (ratio) {
    CropRatio.ORIGINAL -> R.string.edit_tool_ratio_original
    CropRatio.FREE -> R.string.edit_tool_ratio_free
    CropRatio.SQUARE -> R.string.edit_tool_ratio_square
    CropRatio.FOUR_THREE -> R.string.edit_tool_ratio_4_3
    CropRatio.THREE_FOUR -> R.string.edit_tool_ratio_3_4
    CropRatio.SIXTEEN_NINE -> R.string.edit_tool_ratio_16_9
    CropRatio.NINE_SIXTEEN -> R.string.edit_tool_ratio_9_16
}

/** The Crop chips with the first ratio chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun CropChipsPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        CropChips(chosen = CropRatio.entries.first(), cropIsFull = false, enabled = true, onRatio = {})
    }
}
