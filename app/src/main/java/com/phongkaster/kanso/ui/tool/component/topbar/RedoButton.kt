package com.phongkaster.kanso.ui.tool.component.topbar

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_REDO_TAG

/**
 * The Redo control, on the Edit screen only: [UndoButton]'s arrow mirrored, curving forward, as an
 * icon button, calling [onClick] to bring back the edit Undo took back last. While [enabled] is
 * false (nothing undone, a new edit made since, or Done rendering) it is drawn faint and ignores
 * taps. Its content description, "Redo", is what screen readers say for it.
 */
@Composable
internal fun RedoButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.testTag(TOOL_REDO_TAG),
        content = {
            Icon(
                painter = painterResource(R.drawable.ic_redo),
                contentDescription = stringResource(R.string.tool_redo),
            )
        },
    )
}

/** Redo in light and dark: usable, then switched off. */
@PreviewLightDark
@Composable
private fun RedoButtonPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Row(
            content = {
                RedoButton(enabled = true, onClick = {})
                RedoButton(enabled = false, onClick = {})
            },
        )
    }
}
