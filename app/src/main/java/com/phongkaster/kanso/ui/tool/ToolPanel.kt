package com.phongkaster.kanso.ui.tool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.artistic.ArtisticStyle
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Edit tool's controls, in the options strip under the photo. */
const val EDIT_PANEL_TAG = "edit-panel"

/** Test tag of the Remove background tool's controls, in the options strip under the photo. */
const val CUTOUT_PANEL_TAG = "cutout-panel"

/** Test tag of the Enhance tool's controls, in the options strip under the photo. */
const val ENHANCE_PANEL_TAG = "enhance-panel"

/** Test tag of the Retouch tool's controls, in the options strip under the photo. */
const val RETOUCH_PANEL_TAG = "retouch-panel"

/** Test tag of the Artistic tool's controls, in the options strip under the photo. */
const val ARTISTIC_PANEL_TAG = "artistic-panel"

/**
 * The controls of [tool], shown in the options strip under the photo on that tool's screen.
 *
 * Each tool has its own panel, picked here by a `when` over the tool. A `when` on an enum must
 * name every case, so a new tool cannot be added without deciding what its panel is. [onApply]
 * runs a processor the panel built (Edit) and [onRun] runs the one the registry holds for the
 * tool (Remove background, Enhance). [onRetouch] and [onStylize] run Retouch and Artistic with
 * the strength (and style) the person set, all through the ViewModel; [state]
 * gives the panel the photos and whether an edit is running. With the defaults a preview or test
 * gets the panel without any edit behind it.
 *
 * The Edit tool is split into four smaller tools (Crop, Rotate, Adjust, Filters), each opened in
 * its own sheet under the photo. [openTool] says which of them the sheet shows; while it is null
 * (no sheet open) the Edit panel draws nothing, only its empty, tagged slot. [enabled] switches
 * the Edit controls off, which the screen does while an edit runs or while Compare shows the
 * original photo. The other four tools ignore both.
 */
@Composable
fun ToolPanel(
    tool: Tool,
    modifier: Modifier = Modifier,
    state: ToolUiState? = null,
    onApply: (ToolProcessor) -> Unit = {},
    onRun: () -> Unit = {},
    onRetouch: (strength: Float) -> Unit = {},
    onStylize: (style: ArtisticStyle, strength: Float) -> Unit = { _, _ -> },
    openTool: EditTool? = null,
    enabled: Boolean = true,
) {
    // With no state (a preview, or a test of the screen's frame) the slot stays an empty, tagged
    // box, so those pictures show the frame and not a tool's controls; the real screen passes it.
    if (state == null) {
        EmptyPanel(tool, modifier)
        return
    }
    val idle = state.processing is ToolUiState.Processing.Idle
    when (tool) {
        // No Edit sheet open: keep the empty, tagged slot. Otherwise draw the open tool's controls.
        Tool.EDIT -> if (openTool == null) {
            EmptyPanel(tool, modifier)
        } else {
            EditPanel(openTool = openTool, onApply = onApply, modifier = modifier, enabled = enabled)
        }
        // The checkerboard is drawn behind the photo by the screen, so the panel draws none.
        Tool.REMOVE_BACKGROUND -> CutoutPanel(
            onRun = onRun,
            modifier = modifier,
            enabled = idle,
            showCheckerboard = false,
        )
        Tool.ENHANCE -> EnhancePanel(
            onRun = onRun,
            original = (state.photo as? ToolUiState.PhotoState.Ready)?.bitmap,
            enhanced = state.edited,
            modifier = modifier,
        )
        Tool.RETOUCH -> RetouchPanel(onApply = onRetouch, modifier = modifier, enabled = idle)
        Tool.ARTISTIC -> ArtisticPanelHost(onApply = onStylize, modifier = modifier, enabled = idle)
    }
}

/** The empty, tagged slot of [tool]'s panel: the place its controls go, with none drawn. */
@Composable
private fun EmptyPanel(tool: Tool, modifier: Modifier) {
    when (tool) {
        Tool.EDIT -> Box(modifier.testTag(EDIT_PANEL_TAG))
        Tool.REMOVE_BACKGROUND -> Box(modifier.testTag(CUTOUT_PANEL_TAG))
        Tool.ENHANCE -> Box(modifier.testTag(ENHANCE_PANEL_TAG))
        Tool.RETOUCH -> Box(modifier.testTag(RETOUCH_PANEL_TAG))
        Tool.ARTISTIC -> Box(modifier.testTag(ARTISTIC_PANEL_TAG))
    }
}

/**
 * Every tool's panel, one under another, drawn once in light and once in dark. The sample data is
 * simply the list of tools; each panel is still an empty slot, so each is given a fixed height
 * on a surface to show where it sits. The outer Box fills the preview first, because KansoTheme
 * would otherwise stretch its single child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun ToolPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Tool.entries.forEach { tool ->
                        ToolPanel(tool = tool, modifier = Modifier.fillMaxWidth().height(48.dp))
                    }
                }
            }
        }
    }
}
