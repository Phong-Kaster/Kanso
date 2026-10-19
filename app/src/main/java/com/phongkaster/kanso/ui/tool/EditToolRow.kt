package com.phongkaster.kanso.ui.tool

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Edit screen's row of four tools under the photo. */
const val EDIT_TOOL_ROW_TAG = "edit-tool-row"

/** How faint a disabled entry is drawn: Material's usual opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

/** Returns the test tag of [tool]'s entry in the row of tools, such as `edit-tool-CROP`. */
fun editToolTag(tool: EditTool): String = "edit-tool-${tool.name}"

/**
 * Returns the test tag of the small dot on [tool]'s entry, such as `edit-tool-dot-CROP`, shown
 * while that tool's settings change the photo.
 */
fun editToolDotTag(tool: EditTool): String = "edit-tool-dot-${tool.name}"

/** The diameter of the "this tool holds a change" dot. */
private val DOT_SIZE = 6.dp

/**
 * The Edit screen's row of tools under the photo: Crop, Rotate, Adjust and Filters, each an icon
 * over its name. Tapping an entry calls [onOpen] with its tool; the ViewModel then opens that
 * tool's sheet (or closes it, when it was already open). The entry of [openTool] is highlighted
 * with the theme's secondary container colour, the colour Material uses for "this one is chosen".
 *
 * An entry whose part of [settings] (the session's current settings) changes the photo carries a
 * small dot by its icon ([EditSettings.isNeutral] of that tool is false), so the person sees at a
 * glance which tools hold an edit.
 *
 * While [enabled] is false (the Compare switch is on, or Done is rendering the final picture) every entry is drawn faint and ignores taps.
 *
 * A `LazyRow` is a row that only composes the entries it shows and scrolls sideways when they do
 * not fit; four entries usually fit, but a narrow phone with large text still reaches each one.
 */
@Composable
fun EditToolRow(
    openTool: EditTool?,
    enabled: Boolean,
    onOpen: (EditTool) -> Unit,
    modifier: Modifier = Modifier,
    settings: EditSettings = EditSettings(),
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag(EDIT_TOOL_ROW_TAG),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        // SpaceEvenly spreads the four entries across the width when they fit with room to spare.
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items(EditTool.entries) { tool ->
            EditToolEntry(
                tool = tool,
                open = tool == openTool,
                changed = !settings.isNeutral(tool),
                enabled = enabled,
                onClick = { onOpen(tool) },
            )
        }
    }
}

/**
 * One entry of the row of tools: [tool]'s icon over its name, at least 48 dp each way so it is
 * easy to tap. While [open] its background is the secondary container colour, faded in and out by
 * `animateColorAsState` (Compose's own animation API) rather than switched at once. While
 * [changed] a small primary-coloured dot sits at the icon's top right corner. It is "selectable" with the Tab role, so a screen reader says which
 * tool is open; tapping it calls [onClick] unless [enabled] is false, when it is drawn faint and
 * ignores taps.
 */
@Composable
private fun EditToolEntry(
    tool: EditTool,
    open: Boolean,
    changed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val changedText = stringResource(R.string.edit_session_tool_changed)
    val colours = MaterialTheme.colorScheme
    // The closed state is the same colour fully see-through, so the fade never passes through grey.
    val background by animateColorAsState(
        targetValue = if (open) colours.secondaryContainer else colours.secondaryContainer.copy(alpha = 0f),
        label = "edit-tool-background",
    )
    val content = if (open) colours.onSecondaryContainer else colours.onSurfaceVariant
    Column(
        modifier = Modifier
            .testTag(editToolTag(tool))
            .sizeIn(minWidth = 72.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .selectable(selected = open, enabled = enabled, role = Role.Tab, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // The name under the icon names the entry, so the icon itself says nothing to a screen reader.
        Box {
            Icon(painter = painterResource(toolIcon(tool)), contentDescription = null, tint = content)
            if (changed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        // Half outside the icon's corner, so it never covers the symbol itself.
                        .offset(x = DOT_SIZE / 2, y = -DOT_SIZE / 2)
                        .testTag(editToolDotTag(tool))
                        // Its own semantics node (not merged into the entry), so a test finds its
                        // tag and a screen reader says the tool holds a change.
                        .semantics(mergeDescendants = true) { contentDescription = changedText }
                        .size(DOT_SIZE)
                        .clip(CircleShape)
                        .background(colours.primary),
                )
            }
        }
        Text(
            text = stringResource(toolName(tool)),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
        )
    }
}

/** The Material Symbols drawable shown for [tool] in the row. */
@DrawableRes
private fun toolIcon(tool: EditTool): Int = when (tool) {
    EditTool.CROP -> R.drawable.ic_crop
    EditTool.ROTATE -> R.drawable.ic_rotate_right
    EditTool.ADJUST -> R.drawable.ic_tune
    EditTool.FILTERS -> R.drawable.ic_filter_vintage
}

/** The string resource naming [tool] under its icon. */
@StringRes
private fun toolName(tool: EditTool): Int = when (tool) {
    EditTool.CROP -> R.string.edit_layout_crop
    EditTool.ROTATE -> R.string.edit_layout_rotate
    EditTool.ADJUST -> R.string.edit_layout_adjust
    EditTool.FILTERS -> R.string.edit_layout_filters
}

/**
 * The row of tools with Adjust open, in light and in dark, for sample settings with brightness
 * raised and a filter chosen: Adjust and Filters carry the dot, Crop and Rotate do not. The outer
 * Box fills the preview first, because KansoTheme would otherwise stretch its single child to the
 * whole screen.
 */
@PreviewLightDark
@Composable
private fun EditToolRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            EditToolRow(
                openTool = EditTool.ADJUST,
                enabled = true,
                onOpen = {},
                settings = EditSettings(brightness = 0.4f, filter = FilterPreset.KELVIN),
            )
        }
    }
}

/**
 * The row of tools while Compare is on, in light and in dark: every entry is faint, Crop's
 * highlight still showing which sheet stays open underneath.
 */
@PreviewLightDark
@Composable
private fun EditToolRowDisabledPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            EditToolRow(openTool = EditTool.CROP, enabled = false, onOpen = {})
        }
    }
}
