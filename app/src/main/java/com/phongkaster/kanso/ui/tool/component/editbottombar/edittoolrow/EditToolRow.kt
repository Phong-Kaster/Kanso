package com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.phongkaster.kanso.ui.tool.component.groupRowTransition
import com.phongkaster.kanso.ui.tool.model.EditRowEntry
import com.phongkaster.kanso.ui.tool.model.EditToolGroup

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

/** Returns the test tag of [group]'s entry in the first row, such as `edit-group-COLOUR`. */
fun editGroupTag(group: EditToolGroup): String = "edit-group-${group.name}"

/**
 * Returns the test tag of the small dot on [group]'s entry, such as `edit-group-dot-COLOUR`, shown
 * while any tool of the group holds a change.
 */
fun editGroupDotTag(group: EditToolGroup): String = "edit-group-dot-${group.name}"

/**
 * The Edit screen's one row under the photo, on two layers, as in Beautify:
 * - **Layer 1** ([EditRowEntry.TOP_ROW]): Crop, Rotate, Adjust, Colour, Filters, Stickers, Text,
 *   Date, then Compare. Colour is a group: it opens no tool, it swaps the row for layer 2.
 * - **Layer 2** ([EditColourGroupRow]): Back, then RGB, HSL, Curves, then Compare.
 *
 * Tapping a tool calls [onOpen] with it; the ViewModel then opens that tool's sheet (or closes it,
 * when it was already open). The entry of [openTool] is highlighted with the theme's secondary
 * container colour. There is no extra callback for groups: closing a sheet is the same toggling
 * [onOpen], so
 * - tapping Colour while a non-colour tool is open calls `onOpen(openTool)` to close it, then
 *   shows layer 2;
 * - tapping Back while a colour tool is open calls `onOpen(openTool)` (its sheet closes, the
 *   change is kept), then shows layer 1.
 *
 * "Colour is open" is a `rememberSaveable` flag local to this composable, so it survives a screen
 * rotation. It is not in the ViewModel because it is only a view of the row. It is lost whenever
 * the row leaves the composition: when Stickers, Text or Date are tapped, and also when a sticker
 * or text layer is touched on the photo (that layer's sheet replaces the row, and after Confirm the
 * row returns on layer 1). That is harmless: those are layer-1 tools. Layer 2
 * also shows whenever [openTool] is RGB, HSL or Curves, whatever the flag says.
 *
 * - **Compare** is a switch, on while [comparing] and highlighted like an open tool; flipping it
 *   calls [onCompare] with the new value. Compare being on never disables it, because it is the
 *   way back from comparing; only [compareEnabled] false (Done is rendering) does.
 *
 * An entry whose part of [settings] changes the photo carries a small dot by its icon
 * ([EditSettings.isNeutral] of that tool is false); Colour's dot shows while any of its three
 * tools holds a change.
 *
 * While [enabled] is false (the Compare switch is on, or Done is rendering the final picture) every
 * entry but Compare is drawn faint and ignores taps, Colour and Back included.
 *
 * The entries sit in a `LazyRow`, each 60 dp wide, that scrolls sideways. A lazy list composes
 * only the entries on screen, so the row stays cheap however many tools are added later. Compare
 * stays pinned at the right, 68 dp wide, behind a thin divider, so it is always in reach. Switching
 * layers uses `AnimatedContent` with [groupRowTransition].
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
    // Survives rotation (saved in the instance state bundle), like Beautify's open group.
    var colourOpen by rememberSaveable { mutableStateOf(false) }
    val colourTools = EditToolGroup.COLOUR.tools
    val onLayerTwo = colourOpen || openTool in colourTools
    AnimatedContent(
        targetState = onLayerTwo,
        // Into the group: the new row comes from the right; back out: from the left.
        transitionSpec = { groupRowTransition(inward = targetState) },
        label = "edit-row",
        modifier = modifier.fillMaxWidth().testTag(EDIT_TOOL_ROW_TAG),
        content = { layerTwo ->
            if (layerTwo) {
                EditColourGroupRow(
                    openTool = openTool,
                    enabled = enabled,
                    onOpen = onOpen,
                    onBack = {
                        // A colour tool's sheet closes first; its change is kept.
                        if (openTool != null && openTool in colourTools) onOpen(openTool)
                        colourOpen = false
                    },
                    settings = settings,
                    comparing = comparing,
                    compareEnabled = compareEnabled,
                    onCompare = onCompare,
                )
                return@AnimatedContent
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The entries take the free width and scroll sideways when they do not all fit.
                // `items` composes one entry each, keyed by name, so an entry keeps its state
                // while the list scrolls; only the entries on screen are composed.
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(EDIT_TOOL_LIST_TAG),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = {
                        items(
                            items = EditRowEntry.TOP_ROW,
                            key = { entry ->
                                when (entry) {
                                    is EditRowEntry.Group -> "group-${entry.group.name}"
                                    is EditRowEntry.Single -> entry.tool.name
                                }
                            },
                            itemContent = { entry ->
                                when (entry) {
                                    is EditRowEntry.Single -> ToolRowEntry(
                                        tool = entry.tool,
                                        openTool = openTool,
                                        settings = settings,
                                        enabled = enabled,
                                        onOpen = onOpen,
                                    )
                                    is EditRowEntry.Group -> EditToolEntry(
                                        icon = R.drawable.ic_palette,
                                        title = R.string.edit_colour_group,
                                        modifier = Modifier.width(TOOL_ENTRY_WIDTH).testTag(editGroupTag(entry.group)),
                                        changed = entry.group.tools.any { !settings.isNeutral(it) },
                                        dotTag = editGroupDotTag(entry.group),
                                        enabled = enabled,
                                        tap = {
                                            Modifier.clickable(
                                                enabled = enabled,
                                                role = Role.Button,
                                                onClick = {
                                                    // A non-colour sheet closes first (onOpen toggles).
                                                    if (openTool != null && openTool !in entry.group.tools) onOpen(openTool)
                                                    colourOpen = true
                                                },
                                            )
                                        },
                                    )
                                }
                            },
                        )
                    },
                )
                CompareEntry(comparing = comparing, compareEnabled = compareEnabled, onCompare = onCompare)
            }
        },
    )
}

/**
 * The first row with Adjust open, in light and in dark, for sample settings with brightness
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
 * The row while Compare is on, in light and in dark: every entry is faint, Crop's highlight still
 * showing which sheet stays open underneath.
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
