package com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The width of a tool's entry in the scrolling row (both layers). */
internal val TOOL_ENTRY_WIDTH = 60.dp

/**
 * One tool's entry in either layer: its icon over its name, tagged [editToolTag], a tab selected
 * while [openTool] is [tool], carrying the "changed" dot while [settings] changes the photo for it.
 * The first row's singles and the Colour group's three tools both use it.
 */
@Composable
internal fun ToolRowEntry(
    tool: EditTool,
    openTool: EditTool?,
    settings: EditSettings,
    enabled: Boolean,
    onOpen: (EditTool) -> Unit,
) {
    EditToolEntry(
        icon = toolIcon(tool),
        title = toolName(tool),
        modifier = Modifier.width(TOOL_ENTRY_WIDTH).testTag(editToolTag(tool)),
        highlighted = tool == openTool,
        changed = !settings.isNeutral(tool),
        dotTag = editToolDotTag(tool),
        enabled = enabled,
        tap = {
            Modifier.selectable(
                selected = tool == openTool,
                enabled = enabled,
                role = Role.Tab,
                onClick = { onOpen(tool) },
            )
        },
    )
}

/** The Material Symbols drawable shown for [tool] in the row. */
@DrawableRes
private fun toolIcon(tool: EditTool): Int = when (tool) {
    EditTool.CROP -> R.drawable.ic_crop
    EditTool.ROTATE -> R.drawable.ic_rotate_right
    EditTool.ADJUST -> R.drawable.ic_tune
    EditTool.RGB -> R.drawable.ic_instant_mix
    EditTool.HSL -> R.drawable.ic_invert_colors
    EditTool.CURVES -> R.drawable.ic_show_chart
    EditTool.FILTERS -> R.drawable.ic_filter_vintage
    EditTool.STICKERS -> R.drawable.ic_stickers
    EditTool.TEXT -> R.drawable.ic_text_fields
    EditTool.DATE -> R.drawable.ic_calendar_today
}

/** The string resource naming [tool] under its icon. */
@StringRes
private fun toolName(tool: EditTool): Int = when (tool) {
    EditTool.CROP -> R.string.edit_layout_crop
    EditTool.ROTATE -> R.string.edit_layout_rotate
    EditTool.ADJUST -> R.string.edit_layout_adjust
    EditTool.RGB -> R.string.edit_layout_rgb
    EditTool.HSL -> R.string.edit_hsl_label
    EditTool.CURVES -> R.string.edit_curves_tool_name
    EditTool.FILTERS -> R.string.edit_layout_filters
    EditTool.STICKERS -> R.string.stickers_tool_label
    EditTool.TEXT -> R.string.text_tool_label
    EditTool.DATE -> R.string.date_tool_label
}

/**
 * An open RGB entry next to a plain Crop entry, in light and in dark: the red channel is raised in
 * the sample settings, so RGB is not neutral and carries the dot, while Crop does not.
 */
@PreviewLightDark
@Composable
private fun ToolRowEntryPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            val settings = EditSettings(red = 0.3f)
            ToolRowEntry(tool = EditTool.RGB, openTool = EditTool.RGB, settings = settings, enabled = true, onOpen = {})
            ToolRowEntry(tool = EditTool.CROP, openTool = EditTool.RGB, settings = settings, enabled = true, onOpen = {})
        }
    }
}
