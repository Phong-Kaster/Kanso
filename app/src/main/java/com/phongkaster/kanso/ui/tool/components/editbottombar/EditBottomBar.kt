package com.phongkaster.kanso.ui.tool.components.editbottombar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.components.editbottombar.edittoolrow.EditToolRow

/**
 * The Edit screen's bottom bar, the lowest thing on screen: one row of Crop, Rotate, Adjust,
 * Filters and Compare, and just above it the fixed-height panel area, which holds the open tool's
 * panel ([sheet]) or a hint when none is open. It is passed to `Scaffold`'s `bottomBar` slot, so
 * the photo area above always ends where this bar begins, and since the bar never changes height
 * neither does the photo.
 *
 * The bar keeps clear of the navigation bar and of the bottom gesture area: with swipe gestures
 * the navigation bar can be 0 dp tall while a swipe up from the edge still means "go home".
 *
 * **Why the panel is not a modal bottom sheet.** Material's `ModalBottomSheet` dims everything
 * behind it and swallows touches outside it, so the entries of the row could not
 * be tapped while a panel is open. Here the panel is an ordinary surface in the bar: nothing dims
 * the photo and the row stays in reach.
 *
 * [onOpenTool] is a tool entry tapped (the ViewModel opens that tool, or closes it if it was
 * open). While Compare is on, or Done is rendering, the row is disabled; the screen also passes
 * that flag to the panel's controls, which disable themselves.
 */
@Composable
fun EditBottomBar(
    state: ToolUiState,
    onOpenTool: (EditTool) -> Unit,
    sheet: @Composable (EditTool) -> Unit,
    modifier: Modifier = Modifier,
    onCompare: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
            )
            .padding(bottom = 8.dp),
    ) {
        EditSheet(
            openTool = state.openTool,
            sheet = sheet,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        // The session's settings tell the row which tools hold a change (their dot).
        EditToolRow(
            openTool = state.openTool,
            enabled = !state.comparing && !state.finishing,
            onOpen = onOpenTool,
            settings = state.settings,
            comparing = state.comparing,
            // Compare is the way back from comparing, so only Done's render switches it off.
            compareEnabled = !state.finishing,
            onCompare = onCompare,
        )
    }
}

/**
 * The bottom bar with Adjust's panel open, in light and in dark, pinned to the bottom of a
 * full-size box as the Scaffold pins it. A line of text stands in for the panel's controls.
 */
@PreviewLightDark
@Composable
private fun EditBottomBarPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            EditBottomBar(
                state = ToolUiState(
                    tool = Tool.EDIT,
                    photoUri = "content://sample/photo/1",
                    openTool = EditTool.ADJUST,
                ),
                onOpenTool = {},
                sheet = { tool -> Text(text = tool.name) },
            )
        }
    }
}
