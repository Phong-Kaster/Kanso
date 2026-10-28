package com.phongkaster.kanso.ui.tool.component.toolpanel

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The empty, tagged slot of [tool]'s panel: the place its controls go, with none drawn. */
@Composable
internal fun EmptyPanel(tool: Tool, modifier: Modifier) {
    when (tool) {
        Tool.EDIT -> Box(modifier.testTag(EDIT_PANEL_TAG))
        Tool.REMOVE_BACKGROUND -> Box(modifier.testTag(CUTOUT_PANEL_TAG))
        Tool.ENHANCE -> Box(modifier.testTag(ENHANCE_PANEL_TAG))
        Tool.BEAUTIFY -> Box(modifier.testTag(BEAUTIFY_PANEL_TAG))
        Tool.ARTISTIC -> Box(modifier.testTag(ARTISTIC_PANEL_TAG))
    }
}

/** The empty panel for Remove background, in light and in dark. */
@PreviewLightDark
@Composable
private fun EmptyPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        EmptyPanel(tool = Tool.REMOVE_BACKGROUND, modifier = Modifier)
    }
}
