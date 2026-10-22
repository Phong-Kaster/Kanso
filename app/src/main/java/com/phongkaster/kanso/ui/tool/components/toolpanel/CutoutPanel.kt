package com.phongkaster.kanso.ui.tool.components.toolpanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.components.Checkerboard

/**
 * Remove background's controls, shown in the tool screen's options strip: one button, "Cut out
 * the subject", that runs the cut-out on the photo.
 *
 * The panel is stateless: it does not build the edit itself. Tapping the button calls [onRun],
 * which the screen points at the ViewModel's `runRegistered()`; the ViewModel then runs the
 * processor registered for Remove background (a `CutoutProcessor` over the engine's selfie
 * segmentation) and shows its progress, result or failure on the photo card. [enabled] is false
 * while an edit runs, so the button cannot start a second one.
 *
 * When [showCheckerboard] is true the panel draws a [Checkerboard] behind its button, a hint that
 * the result will be see-through. The tool screen may draw the checkerboard behind the photo
 * instead; it then passes false so only one checkerboard is on screen. The root carries
 * [CUTOUT_PANEL_TAG].
 */
@Composable
fun CutoutPanel(
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showCheckerboard: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CUTOUT_PANEL_TAG),
        contentAlignment = Alignment.Center,
    ) {
        if (showCheckerboard) {
            // matchParentSize sizes the checks to the Box once the button has set its size, so
            // the checkerboard never makes the panel taller.
            Checkerboard(Modifier.matchParentSize())
        }
        Button(
            onClick = onRun,
            enabled = enabled,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                // 48 dp is the smallest comfortable touch target.
                .heightIn(min = 48.dp),
        ) {
            Text(text = stringResource(R.string.cutout_run))
        }
    }
}

/**
 * The Remove background panel on the options strip's surface, drawn once in light and once in
 * dark: ready to run, with the checkerboard behind the button. The outer Box fills the preview
 * first, because KansoTheme would otherwise stretch its single child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun CutoutPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                CutoutPanel(onRun = {}, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
