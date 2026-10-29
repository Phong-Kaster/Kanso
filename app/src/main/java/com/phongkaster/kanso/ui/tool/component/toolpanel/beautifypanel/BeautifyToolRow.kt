package com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EditToolEntry
import com.phongkaster.kanso.ui.tool.model.BeautifyEntry
import com.phongkaster.kanso.ui.tool.model.BeautifyGroup
import com.phongkaster.kanso.ui.tool.model.BeautifyTool

/** Test tag of the Back entry in a group's row, which returns to the top row. */
const val BEAUTIFY_BACK_TAG = "beautify-back"

/** Returns the test tag of [tool]'s entry, such as `beautify-tool-SMOOTH`. */
fun beautifyToolTag(tool: BeautifyTool): String = "beautify-tool-${tool.name}"

/** Returns the test tag of [group]'s entry in the top row, such as `beautify-group-FACE`. */
fun beautifyGroupTag(group: BeautifyGroup): String = "beautify-group-${group.name}"

/**
 * The Beautify panel's row of tools, on two levels, as in Meitu's Beautify:
 * - **Layer 1**, while [openGroup] is null: the top row ([BeautifyEntry.TOP_ROW]), Face, Body,
 *   Smooth. Tapping a group calls [onOpenGroup]; tapping a single tool calls [onTool].
 * - **Layer 2**, while [openGroup] is set: that group's tools on the left and a Back entry pinned
 *   at the right end ([GroupRow]). Tapping a tool calls [onTool]; Back calls [onBack].
 *
 * Going into a group the new row slides in from the right, and going back it slides in from the
 * left, with `AnimatedContent` (Compose's own animation API), so the person sees which way they
 * went. [openTool]'s entry is highlighted. An entry carries Edit's small dot when [changed] says its
 * tool holds a change, and a group's entry when any of its tools does. Every entry is the Edit
 * screen's own [EditToolEntry]; while [enabled] is false they are drawn faint and ignore taps.
 */
@Composable
fun BeautifyToolRow(
    openGroup: BeautifyGroup?,
    openTool: BeautifyTool?,
    changed: (BeautifyTool) -> Boolean,
    enabled: Boolean,
    onOpenGroup: (BeautifyGroup) -> Unit,
    onTool: (BeautifyTool) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = openGroup,
        transitionSpec = {
            // Into a group: the new row comes from the right; back out: from the left.
            val inward = targetState != null
            (slideInHorizontally(initialOffsetX = { if (inward) it else -it }) + fadeIn()) togetherWith
                (slideOutHorizontally(targetOffsetX = { if (inward) -it else it }) + fadeOut())
        },
        label = "beautify-row",
        modifier = modifier.fillMaxWidth(),
        content = { group ->
            if (group != null) {
                GroupRow(group = group, openTool = openTool, changed = changed, enabled = enabled, onTool = onTool, onBack = onBack)
                return@AnimatedContent
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                // The same 4 dp around the entries as Edit's row of tools.
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                content = {
                    BeautifyEntry.TOP_ROW.forEach(
                        action = { entry ->
                            when (entry) {
                                is BeautifyEntry.Group -> GroupEntry(
                                    group = entry.group,
                                    changed = entry.group.tools.any(predicate = changed),
                                    enabled = enabled,
                                    onOpen = onOpenGroup,
                                )
                                is BeautifyEntry.Single -> ToolEntry(
                                    tool = entry.tool,
                                    open = entry.tool == openTool,
                                    changed = changed(entry.tool),
                                    enabled = enabled,
                                    onTool = onTool,
                                )
                            }
                        },
                    )
                },
            )
        },
    )
}

/**
 * A group's row (layer 2), laid out like the Edit screen's row: the group's tools in a `LazyRow`
 * on the left, which scrolls sideways once there are more than fit, and **Back pinned at the right
 * end** behind a thin divider, so the way back is always in the same place and always in reach,
 * however many tools the group gets.
 */
@Composable
private fun GroupRow(
    group: BeautifyGroup,
    openTool: BeautifyTool?,
    changed: (BeautifyTool) -> Boolean,
    enabled: Boolean,
    onTool: (BeautifyTool) -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            // A lazy list composes only the entries on screen, so the row stays cheap as the
            // group grows; keys keep each entry's state while it scrolls.
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    items(
                        items = group.tools,
                        key = { it.name },
                        itemContent = { tool ->
                            ToolEntry(tool = tool, open = tool == openTool, changed = changed(tool), enabled = enabled, onTool = onTool)
                        },
                    )
                },
            )
            // A thin line separates the tools (which scroll) from Back (which stays put).
            VerticalDivider(modifier = Modifier.padding(horizontal = 4.dp).height(40.dp))
            BackEntry(enabled = enabled, onBack = onBack)
        },
    )
}

/** A tool's entry: its icon over its name, a tab that is selected while its slider is [open]. */
@Composable
private fun ToolEntry(tool: BeautifyTool, open: Boolean, changed: Boolean, enabled: Boolean, onTool: (BeautifyTool) -> Unit) {
    EditToolEntry(
        icon = toolIcon(tool),
        title = toolName(tool),
        modifier = Modifier.widthIn(min = ENTRY_MIN_WIDTH).testTag(beautifyToolTag(tool)),
        enabled = enabled,
        highlighted = open,
        changed = changed,
        dotTag = "beautify-tool-dot-${tool.name}",
        tap = {
            Modifier.selectable(selected = open, enabled = enabled, role = Role.Tab, onClick = { onTool(tool) })
        },
    )
}

/** A group's entry in the top row: a button that opens the group's own row. */
@Composable
private fun GroupEntry(group: BeautifyGroup, changed: Boolean, enabled: Boolean, onOpen: (BeautifyGroup) -> Unit) {
    EditToolEntry(
        icon = groupIcon(group),
        title = groupName(group),
        modifier = Modifier.widthIn(min = ENTRY_MIN_WIDTH).testTag(beautifyGroupTag(group)),
        enabled = enabled,
        changed = changed,
        dotTag = "beautify-group-dot-${group.name}",
        tap = { Modifier.clickable(enabled = enabled, role = Role.Button, onClick = { onOpen(group) }) },
    )
}

/** The Back entry at the end of a group's row: a button that returns to the top row. */
@Composable
private fun BackEntry(enabled: Boolean, onBack: () -> Unit) {
    EditToolEntry(
        icon = R.drawable.ic_arrow_back,
        title = R.string.back,
        modifier = Modifier.widthIn(min = ENTRY_MIN_WIDTH).testTag(BEAUTIFY_BACK_TAG),
        enabled = enabled,
        tap = { Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onBack) },
    )
}

/** The drawable shown for [tool] (Width's, Lift's, Waist's and Arms' are drawn for Kanso; Smooth's is a Material Symbol). */
@DrawableRes
private fun toolIcon(tool: BeautifyTool): Int = when (tool) {
    BeautifyTool.SMOOTH -> R.drawable.ic_face_retouching_natural
    BeautifyTool.FACE_WIDTH -> R.drawable.ic_face_width
    BeautifyTool.FACE_LIFT -> R.drawable.ic_face_lift
    BeautifyTool.WAIST -> R.drawable.ic_body_waist
    BeautifyTool.ARMS -> R.drawable.ic_body_arms
}

/** The string resource naming [tool] under its icon. */
@StringRes
private fun toolName(tool: BeautifyTool): Int = when (tool) {
    BeautifyTool.SMOOTH -> R.string.beautify_tool_smooth
    BeautifyTool.FACE_WIDTH -> R.string.beautify_tool_width
    BeautifyTool.FACE_LIFT -> R.string.beautify_tool_lift
    BeautifyTool.WAIST -> R.string.beautify_tool_waist
    BeautifyTool.ARMS -> R.string.beautify_tool_arms
}

/** The drawable shown for [group] (drawn for Kanso). */
@DrawableRes
private fun groupIcon(group: BeautifyGroup): Int = when (group) {
    BeautifyGroup.FACE -> R.drawable.ic_face_group
    BeautifyGroup.BODY -> R.drawable.ic_body_group
}

/** The string resource naming [group] under its icon. */
@StringRes
private fun groupName(group: BeautifyGroup): Int = when (group) {
    BeautifyGroup.FACE -> R.string.beautify_group_face
    BeautifyGroup.BODY -> R.string.beautify_group_body
}

/** The narrowest an entry gets, so a short name still makes a comfortable target. */
private val ENTRY_MIN_WIDTH = 72.dp

/** The top row (layer 1), Face carrying a change, in light and in dark. */
@PreviewLightDark
@Composable
private fun BeautifyToolRowTopPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            BeautifyToolRow(
                openGroup = null,
                openTool = null,
                changed = { it == BeautifyTool.FACE_WIDTH },
                enabled = true,
                onOpenGroup = {},
                onTool = {},
                onBack = {},
            )
        }
    }
}

/** One tool's entry, open and carrying a change, in light and in dark. */
@PreviewLightDark
@Composable
private fun ToolEntryPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            ToolEntry(tool = BeautifyTool.FACE_WIDTH, open = true, changed = true, enabled = true, onTool = {})
        }
    }
}

/** The Face group's entry, in light and in dark. */
@PreviewLightDark
@Composable
private fun GroupEntryPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            GroupEntry(group = BeautifyGroup.FACE, changed = false, enabled = true, onOpen = {})
        }
    }
}

/** The Back entry, in light and in dark. */
@PreviewLightDark
@Composable
private fun BackEntryPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            BackEntry(enabled = true, onBack = {})
        }
    }
}

/** Face's row on its own: Width on the left, Back pinned at the right, in light and in dark. */
@PreviewLightDark
@Composable
private fun GroupRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            GroupRow(group = BeautifyGroup.FACE, openTool = null, changed = { false }, enabled = true, onTool = {}, onBack = {})
        }
    }
}

/** Face's row (layer 2) with Width open, then Back, in light and in dark. */
@PreviewLightDark
@Composable
private fun BeautifyToolRowFacePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            BeautifyToolRow(
                openGroup = BeautifyGroup.FACE,
                openTool = BeautifyTool.FACE_WIDTH,
                changed = { false },
                enabled = true,
                onOpenGroup = {},
                onTool = {},
                onBack = {},
            )
        }
    }
}
