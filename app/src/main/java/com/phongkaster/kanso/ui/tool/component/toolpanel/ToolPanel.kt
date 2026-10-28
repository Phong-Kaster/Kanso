package com.phongkaster.kanso.ui.tool.component.toolpanel

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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.artistic.ArtisticStyle
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.model.AdjustSlider
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EditPanel
import com.phongkaster.kanso.ui.tool.component.toolpanel.enhancepanel.EnhancePanel
import com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel.BeautifyPanel

/** Test tag of the Edit tool's controls, in the options strip under the photo. */
const val EDIT_PANEL_TAG = "edit-panel"

/** Test tag of the Remove background tool's controls, in the options strip under the photo. */
const val CUTOUT_PANEL_TAG = "cutout-panel"

/** Test tag of the Enhance tool's controls, in the options strip under the photo. */
const val ENHANCE_PANEL_TAG = "enhance-panel"

/** Test tag of the Beautify tool's controls, in the options strip under the photo. */
const val BEAUTIFY_PANEL_TAG = "beautify-panel"

/** Test tag of the Artistic tool's controls, in the options strip under the photo. */
const val ARTISTIC_PANEL_TAG = "artistic-panel"

/**
 * The controls of [tool], shown in the options strip under the photo on that tool's screen.
 *
 * Each tool has its own panel, picked here by a `when` over the tool. A `when` on an enum must
 * name every case, so a new tool cannot be added without deciding what its panel is. The Edit sheets' four
 * `onEdit...` callbacks go to the ViewModel's settings session; [onRun] runs the one the registry holds for the
 * tool (Remove background, Enhance). [onBeautify] and [onStylize] run Beautify and Artistic with
 * the strength (and face width, or style) the person set, all through the ViewModel;
 * [onBeautifyPreview] asks for a live frame while a Beautify slider is dragged; [state]
 * gives the panel the photos and whether an edit is running. With the defaults a preview or test
 * gets the panel without any edit behind it.
 *
 * The Edit tool is split into four smaller tools (Crop, Rotate, Adjust, Filters), each opened in
 * its own sheet under the photo. [openTool] says which of them the sheet shows; while it is null
 * (no sheet open) the Edit panel draws nothing, only its empty, tagged slot. [enabled] switches
 * the Edit controls off, which the screen does while an edit runs or while Compare shows the
 * original photo. [onEditAdjustSlider] is one of Adjust's sub-tool chips tapped; the chosen one
 * comes from [state]. The other four tools ignore all three.
 */
@Composable
fun ToolPanel(
    tool: Tool,
    modifier: Modifier = Modifier,
    state: ToolUiState? = null,
    onRun: () -> Unit = {},
    onBeautify: (strength: Float, width: Float) -> Unit = { _, _ -> },
    onBeautifyPreview: (strength: Float, width: Float) -> Unit = { _, _ -> },
    onStylize: (style: ArtisticStyle, strength: Float) -> Unit = { _, _ -> },
    openTool: EditTool? = null,
    enabled: Boolean = true,
    onEditChange: (EditSettings, (EditSettings) -> ToolProcessor) -> Unit = { _, _ -> },
    onEditPreview: (EditSettings, (EditSettings) -> ToolProcessor) -> Unit = { _, _ -> },
    onEditCommit: () -> Unit = {},
    onEditRatio: (CropRatio, (EditSettings) -> ToolProcessor) -> Unit = { _, _ -> },
    onEditAdjustSlider: (AdjustSlider) -> Unit = {},
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
            EditPanel(
                openTool = openTool,
                settings = state.settings,
                onChange = onEditChange,
                onPreview = onEditPreview,
                onCommit = onEditCommit,
                onRatio = onEditRatio,
                modifier = modifier,
                enabled = enabled,
                adjustSlider = state.adjustSlider,
                onAdjustSlider = onEditAdjustSlider,
            )
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
        Tool.BEAUTIFY -> BeautifyPanel(onApply = onBeautify, onPreview = onBeautifyPreview, modifier = modifier, enabled = idle)
        Tool.ARTISTIC -> ArtisticPanelHost(onApply = onStylize, modifier = modifier, enabled = idle)
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
