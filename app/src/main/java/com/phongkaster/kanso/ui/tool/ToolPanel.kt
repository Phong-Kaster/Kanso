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
 * name every case, so a new tool cannot be added without deciding what its panel is. For now
 * every panel is an empty, tagged slot; each tool's own task fills in its branch with that
 * tool's controls, which run their edit through the ViewModel's `apply`.
 */
@Composable
fun ToolPanel(tool: Tool, modifier: Modifier = Modifier) {
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
