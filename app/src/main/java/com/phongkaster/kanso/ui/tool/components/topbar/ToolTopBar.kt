package com.phongkaster.kanso.ui.tool.components.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.common.RoundBackButton
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_BACK_TAG

/**
 * The tool screen's top bar: the round Back button at the left (calls [onBack]), [tool]'s name
 * centred across the screen, and at the right the Undo control (calls [onUndo]; it does nothing
 * while [undoEnabled] is false) followed by the Done button (calls [onDone]; it does nothing while
 * [doneEnabled] is false, and shows a small spinner while [finishing]). With [showUndo] false (the
 * Edit screen, whose Undo sits in the row under the photo) Done stands alone at the right end, so
 * the screen never has two Undo controls.
 *
 * `CenterAlignedTopAppBar` is Material 3's top bar whose title sits in the middle of the screen
 * rather than beside the Back button; it also keeps clear of the status bar at the top. The
 * tool's name appears exactly once as text on this screen: here ("Done" and "Undo" are never a
 * tool's name). Its `actions` slot holds the controls drawn at the bar's right end, in a row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolTopBar(
    tool: Tool,
    onBack: () -> Unit,
    showUndo: Boolean,
    undoEnabled: Boolean,
    onUndo: () -> Unit,
    doneEnabled: Boolean,
    finishing: Boolean,
    onDone: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(tool.labelRes),
                style = MaterialTheme.typography.titleLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            RoundBackButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag(TOOL_BACK_TAG),
            )
        },
        actions = {
            if (showUndo) {
                UndoButton(enabled = undoEnabled, onClick = onUndo)
            }
            DoneButton(
                enabled = doneEnabled,
                finishing = finishing,
                onClick = onDone,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
    )
}

/**
 * The top bar of one of the four other tools (Retouch here), in light and in dark, twice: above,
 * Undo and Done both usable; below, both switched off, as while the photo is still opening or an
 * edit runs. Undo always stands left of Done. The bars sit in a full-size box first, because
 * KansoTheme would otherwise stretch the first one over the whole preview.
 */
@PreviewLightDark
@Composable
internal fun ToolTopBarPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Column {
                ToolTopBar(
                    tool = Tool.RETOUCH,
                    onBack = {},
                    showUndo = true,
                    undoEnabled = true,
                    onUndo = {},
                    doneEnabled = true,
                    finishing = false,
                    onDone = {},
                )
                ToolTopBar(
                    tool = Tool.RETOUCH,
                    onBack = {},
                    showUndo = true,
                    undoEnabled = false,
                    onUndo = {},
                    doneEnabled = false,
                    finishing = false,
                    onDone = {},
                )
            }
        }
    }
}

/**
 * The Edit screen's top bar, in light and in dark, twice: above, Done usable; below, Done
 * switched off, as before the photo is shown. Edit's Undo lives in the row under the photo, so Done
 * stands alone at the right end. The rendering state (a turning spinner inside Done) is not shown:
 * a preview draws one still picture, and a spinner never stops turning.
 */
@PreviewLightDark
@Composable
internal fun ToolTopBarEditPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Column {
                ToolTopBar(
                    tool = Tool.EDIT,
                    onBack = {},
                    showUndo = false,
                    undoEnabled = false,
                    onUndo = {},
                    doneEnabled = true,
                    finishing = false,
                    onDone = {},
                )
                ToolTopBar(
                    tool = Tool.EDIT,
                    onBack = {},
                    showUndo = false,
                    undoEnabled = false,
                    onUndo = {},
                    doneEnabled = false,
                    finishing = false,
                    onDone = {},
                )
            }
        }
    }
}
