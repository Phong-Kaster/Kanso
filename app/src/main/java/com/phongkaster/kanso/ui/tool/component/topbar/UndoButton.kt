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
import com.phongkaster.kanso.ui.tool.TOOL_UNDO_TAG

/**
 * The Undo control: an arrow curving back, as an icon button, calling [onClick] to take back the
 * last edit, one at a time, down to the original photo. While [enabled] is false (nothing to take
 * back, or an edit running) it is drawn faint and ignores taps.
 *
 * An icon button is Material's smallest button, a 48 dp touch target around a 24 dp icon, so Undo
 * and the Edit tool's Redo fit beside Done without crowding the centred title. The icon has no
 * words, so its content description, "Undo", is what screen readers say for it.
 */
@Composable
internal fun UndoButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.testTag(TOOL_UNDO_TAG),
        content = {
            Icon(
                painter = painterResource(R.drawable.ic_undo),
                contentDescription = stringResource(R.string.tool_undo),
            )
        },
    )
}

/** Undo in light and dark: usable, then switched off. */
@PreviewLightDark
@Composable
private fun UndoButtonPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Row(
            content = {
                UndoButton(enabled = true, onClick = {})
                UndoButton(enabled = false, onClick = {})
            },
        )
    }
}
