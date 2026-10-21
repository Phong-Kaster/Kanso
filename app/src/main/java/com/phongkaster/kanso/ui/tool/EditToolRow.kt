package com.phongkaster.kanso.ui.tool

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Edit screen's row under the photo: four tools, Undo and Compare. */
const val EDIT_TOOL_ROW_TAG = "edit-tool-row"

/** Test tag of the row's Compare switch, which shows the original photo while it is on. */
const val EDIT_COMPARE_TAG = "edit-compare"

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

/** The width of a tool's entry in the scrolling row. */
private val TOOL_ENTRY_WIDTH = 84.dp

/** The width of the pinned Undo and Compare entries. */
private val ACTION_ENTRY_WIDTH = 68.dp

/** The size of an entry's icon. */
private val ENTRY_ICON_SIZE = 28.dp

/**
 * The Edit screen's one row under the photo: the four tools (Crop, Rotate, Adjust, Filters), then
 * Undo and Compare, each an icon over its name, all six the same width. Tapping a tool calls
 * [onOpen] with it; the ViewModel then opens that tool's sheet (or closes it, when it was already
 * open). The entry of [openTool] is highlighted with the theme's secondary container colour, the
 * colour Material uses for "this one is chosen".
 *
 * - **Undo** calls [onUndo] to take back the last edit; it is faint and ignores taps while
 *   [undoEnabled] is false (nothing to take back, an edit running, or Compare on).
 * - **Compare** is a switch, on while [comparing] and highlighted like an open tool; flipping it
 *   calls [onCompare] with the new value. Compare being on never disables it, because it is the
 *   way back from comparing; only [compareEnabled] false (Done is rendering) does.
 *
 * An entry whose part of [settings] (the session's current settings) changes the photo carries a
 * small dot by its icon ([EditSettings.isNeutral] of that tool is false), so the person sees at a
 * glance which tools hold an edit.
 *
 * While [enabled] is false (the Compare switch is on, or Done is rendering the final picture) the
 * four tools are drawn faint and ignore taps.
 *
 * The four tools sit in a row that scrolls sideways, each [TOOL_ENTRY_WIDTH] wide, so they stay
 * large and a tool past the edge is reached by swiping. Undo and Compare stay pinned at the right,
 * [ACTION_ENTRY_WIDTH] wide each, behind a thin divider, so they are always in reach. A name too
 * long for its entry is cut short with an ellipsis.
 */
@Composable
fun EditToolRow(
    openTool: EditTool?,
    enabled: Boolean,
    onOpen: (EditTool) -> Unit,
    modifier: Modifier = Modifier,
    settings: EditSettings = EditSettings(),
    undoEnabled: Boolean = false,
    comparing: Boolean = false,
    compareEnabled: Boolean = true,
    onUndo: () -> Unit = {},
    onCompare: (Boolean) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(EDIT_TOOL_ROW_TAG)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The tools take the free width and scroll sideways when they do not all fit, so more
        // tools can be added later without shrinking the entries. `rememberScrollState` keeps the
        // scroll position across recompositions.
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            EditTool.entries.forEach { tool ->
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
        }
        // A thin line separates the tools (which scroll) from Undo and Compare (which stay put).
        VerticalDivider(modifier = Modifier.padding(horizontal = 4.dp).height(40.dp))
        EditToolEntry(
            icon = R.drawable.ic_undo,
            title = R.string.edit_layout_undo,
            modifier = Modifier.width(ACTION_ENTRY_WIDTH).testTag(TOOL_UNDO_TAG),
            enabled = undoEnabled,
            tap = { Modifier.clickable(enabled = undoEnabled, role = Role.Button, onClick = onUndo) },
        )
        EditToolEntry(
            icon = R.drawable.ic_compare,
            title = R.string.edit_layout_compare,
            modifier = Modifier.width(ACTION_ENTRY_WIDTH).testTag(EDIT_COMPARE_TAG),
            highlighted = comparing,
            enabled = compareEnabled,
            // A toggle, so a screen reader reads it as one switch named Compare, on or off.
            tap = {
                Modifier.toggleable(
                    value = comparing,
                    enabled = compareEnabled,
                    role = Role.Switch,
                    onValueChange = onCompare,
                )
            },
        )
    }
}

/**
 * One entry of the row: [icon] over [title], at least 48 dp tall so it is easy to tap. While
 * [highlighted] its background is the secondary container colour, faded in and out by
 * `animateColorAsState` (Compose's own animation API) rather than switched at once. While
 * [changed] a small primary-coloured dot (tagged [dotTag]) sits at the icon's top right corner.
 * [tap] builds the touch behaviour (a selectable tab, a button or a switch); the entry is drawn
 * faint while [enabled] is false.
 */
@Composable
private fun EditToolEntry(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    modifier: Modifier,
    enabled: Boolean,
    tap: () -> Modifier,
    highlighted: Boolean = false,
    changed: Boolean = false,
    dotTag: String? = null,
) {
    val changedText = stringResource(R.string.edit_session_tool_changed)
    val colours = MaterialTheme.colorScheme
    // The closed state is the same colour fully see-through, so the fade never passes through grey.
    val background by animateColorAsState(
        targetValue = if (highlighted) colours.secondaryContainer else colours.secondaryContainer.copy(alpha = 0f),
        label = "edit-tool-background",
    )
    val content = if (highlighted) colours.onSecondaryContainer else colours.onSurfaceVariant
    Column(
        modifier = modifier
            .sizeIn(minHeight = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .then(tap())
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        // The name under the icon names the entry, so the icon itself says nothing to a screen reader.
        Box {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(ENTRY_ICON_SIZE),
            )
            if (changed && dotTag != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        // Half outside the icon's corner, so it never covers the symbol itself.
                        .offset(x = DOT_SIZE / 2, y = -DOT_SIZE / 2)
                        .testTag(dotTag)
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
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
