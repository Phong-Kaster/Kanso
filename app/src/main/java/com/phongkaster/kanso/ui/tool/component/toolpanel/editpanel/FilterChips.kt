package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel

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
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Returns the test tag of [preset]'s filter chip, such as `edit-filter-KELVIN`. */
fun editFilterTag(preset: FilterPreset): String = "edit-filter-${preset.name}"

/**
 * One row of filter chips, scrolling sideways: None and then one chip per preset of
 * [FilterPreset], the [chosen] one marked selected, so only one is ever on. A tap calls [onFilter]
 * with the chip's filter; while [enabled] is false every chip is disabled.
 */
@Composable
internal fun FilterChips(chosen: FilterPreset, enabled: Boolean, onFilter: (FilterPreset) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // NONE is the enum's first entry, so None leads the row.
        FilterPreset.entries.forEach { preset ->
            EditFeatureTile(
                icon = if (preset == FilterPreset.NONE) R.drawable.ic_hide_image else R.drawable.ic_filter_vintage,
                title = filterLabel(preset),
                onClick = { onFilter(preset) },
                selected = preset == chosen,
                enabled = enabled,
                modifier = Modifier.testTag(editFilterTag(preset)),
            )
        }
    }
}

/** The string resource naming filter [preset] on its tile. */
@StringRes
private fun filterLabel(preset: FilterPreset): Int = when (preset) {
    FilterPreset.NONE -> R.string.edit_tool_filter_none
    FilterPreset.LIGHT -> R.string.edit_tool_filter_light
    FilterPreset.SEVENTY_SEVEN -> R.string.edit_tool_filter_1977
    FilterPreset.KELVIN -> R.string.edit_tool_filter_kelvin
    FilterPreset.NASHVILLE -> R.string.edit_tool_filter_nashville
    FilterPreset.WALDEN -> R.string.edit_tool_filter_walden
    FilterPreset.HUDSON -> R.string.edit_tool_filter_hudson
    FilterPreset.AMARO -> R.string.edit_tool_filter_amaro
    FilterPreset.AUTUMN -> R.string.edit_tool_filter_autumn
}

/** The Filters chips with no filter chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun FilterChipsPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        FilterChips(chosen = FilterPreset.NONE, enabled = true, onFilter = {})
    }
}
