package com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Edit screen's row under the photo: the tools and Compare. */
const val EDIT_TOOL_ROW_TAG = "edit-tool-row"

/** Test tag of the lazy list of tools inside the row (it scrolls; Compare does not). */
const val EDIT_TOOL_LIST_TAG = "edit-tool-list"

/** Test tag of the row's Compare switch, which shows the original photo while it is on. */
const val EDIT_COMPARE_TAG = "edit-compare"

/** Returns the test tag of [tool]'s entry in the row of tools, such as `edit-tool-CROP`. */
fun editToolTag(tool: EditTool): String = "edit-tool-${tool.name}"

/**
 * Returns the test tag of the small dot on [tool]'s entry, such as `edit-tool-dot-CROP`, shown
 * while that tool's settings change the photo.
 */
fun editToolDotTag(tool: EditTool): String = "edit-tool-dot-${tool.name}"

/** The width of a tool's entry in the scrolling row. */
private val TOOL_ENTRY_WIDTH = 60.dp

/** The width of the pinned Compare entry. */
private val ACTION_ENTRY_WIDTH = 68.dp

/**
 * The Edit screen's one row under the photo: the tools (Crop, Rotate, Adjust, Filters, Stickers,
 * Text, Date), then Compare, each an icon over its name. Tapping a tool calls
 * [onOpen] with it; the ViewModel then opens that tool's sheet (or closes it, when it was already
 * open). The entry of [openTool] is highlighted with the theme's secondary container colour, the
 * colour Material uses for "this one is chosen".
 *
 * - **Compare** is a switch, on while [comparing] and highlighted like an open tool; flipping it
 *   calls [onCompare] with the new value. Compare being on never disables it, because it is the
 *   way back from comparing; only [compareEnabled] false (Done is rendering) does.
 *
 * An entry whose part of [settings] (the session's current settings) changes the photo carries a
 * small dot by its icon ([EditSettings.isNeutral] of that tool is false), so the person sees at a
 * glance which tools hold an edit.
 *
 * While [enabled] is false (the Compare switch is on, or Done is rendering the final picture) the
 * five tools are drawn faint and ignore taps.
 *
 * The tools sit in a `LazyRow`, each [TOOL_ENTRY_WIDTH] (60 dp) wide, that scrolls sideways. A lazy
 * list composes only the entries on screen (plus a few beyond), so the row stays cheap however
 * many tools are added later: one more [EditTool] is one more entry, reached by swiping. On a
 * 411 dp phone five tools fit beside Compare. Compare stays pinned at the right,
 * [ACTION_ENTRY_WIDTH] wide, behind a thin divider, so they are always in reach. A name too
 * long for its entry is cut short with an ellipsis.
 */
@Composable
fun EditToolRow(
    openTool: EditTool?,
    enabled: Boolean,
    onOpen: (EditTool) -> Unit,
    modifier: Modifier = Modifier,
    settings: EditSettings = EditSettings(),
    comparing: Boolean = false,
    compareEnabled: Boolean = true,
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
        // tools can be added later without shrinking the entries. `items` composes one entry per tool, keyed by the tool, so an entry keeps its state while
        // the list scrolls; only the entries on screen are composed.
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .testTag(EDIT_TOOL_LIST_TAG),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                items(
                    items = EditTool.entries,
                    key = { it.name },
                    itemContent = { tool ->
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
                    },
                )
            },
        )
        // A thin line separates the tools (which scroll) from Compare (which stays put).
        VerticalDivider(modifier = Modifier.padding(horizontal = 4.dp).height(40.dp))
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

/** The Material Symbols drawable shown for [tool] in the row. */
@DrawableRes
private fun toolIcon(tool: EditTool): Int = when (tool) {
    EditTool.CROP -> R.drawable.ic_crop
    EditTool.ROTATE -> R.drawable.ic_rotate_right
    EditTool.ADJUST -> R.drawable.ic_tune
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
    EditTool.FILTERS -> R.string.edit_layout_filters
    EditTool.STICKERS -> R.string.stickers_tool_label
    EditTool.TEXT -> R.string.text_tool_label
    EditTool.DATE -> R.string.date_tool_label
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
                settings = EditSettings(
                    brightness = 0.4f,
                    filter = FilterPreset.KELVIN,
                    layers = listOf(Layer(key = 1L, content = LayerContent.Picture("apple"), centreX = 0.5f, centreY = 0.5f, width = 0.3f, degrees = 0f, mirrored = false)),
                ),
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
