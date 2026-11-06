package com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.GroupBackEntry
import com.phongkaster.kanso.ui.tool.model.EditToolGroup

/** Test tag of the Back entry at the left of the Colour group's row, which returns to the first row. */
const val EDIT_GROUP_BACK_TAG = "edit-group-back"

/**
 * The Colour group's row (layer 2 of the Edit row): **Back pinned at the left**, then a `LazyRow`
 * of RGB, HSL and Curves ([EditToolGroup.COLOUR]), then the divider and Compare pinned at the
 * right, the same Compare as the first row ([CompareEntry]).
 *
 * It is stateless: [onOpen] is called with a tapped tool, [onBack] when Back is tapped, and the
 * caller ([EditToolRow]) decides what each does. [openTool]'s entry is highlighted, and an entry
 * carries its own "changed" dot while [settings] changes the photo for that tool. While [enabled]
 * is false (Compare is held, or Done is rendering) Back and the three tools are faint and ignore
 * taps; Compare follows [compareEnabled] and [comparing] alone. The list keeps the
 * [EDIT_TOOL_LIST_TAG] tag, as in the first row.
 */
@Composable
internal fun EditColourGroupRow(
    openTool: EditTool?,
    enabled: Boolean,
    onOpen: (EditTool) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    settings: EditSettings = EditSettings(),
    comparing: Boolean = false,
    compareEnabled: Boolean = true,
    onCompare: (Boolean) -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back first, at the left, where the eye starts: the way out is easy to find.
        GroupBackEntry(enabled = enabled, onClick = onBack, modifier = Modifier, testTag = EDIT_GROUP_BACK_TAG)
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .testTag(EDIT_TOOL_LIST_TAG),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                items(
                    items = EditToolGroup.COLOUR.tools,
                    key = { it.name },
                    itemContent = { tool ->
                        ToolRowEntry(tool = tool, openTool = openTool, settings = settings, enabled = enabled, onOpen = onOpen)
                    },
                )
            },
        )
        CompareEntry(comparing = comparing, compareEnabled = compareEnabled, onCompare = onCompare)
    }
}

/** The Colour group's row with RGB open and brightness-free sample settings, in light and in dark. */
@PreviewLightDark
@Composable
private fun EditColourGroupRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            EditColourGroupRow(
                openTool = EditTool.RGB,
                enabled = true,
                onOpen = {},
                onBack = {},
                settings = EditSettings(red = 0.8f),
            )
        }
    }
}

/** The Colour group's row while Compare is held, every entry but Compare faint, in light and in dark. */
@PreviewLightDark
@Composable
private fun EditColourGroupRowComparingPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            EditColourGroupRow(
                openTool = null,
                enabled = false,
                onOpen = {},
                onBack = {},
                comparing = true,
            )
        }
    }
}
