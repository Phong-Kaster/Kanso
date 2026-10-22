package com.phongkaster.kanso.ui.tool.components.topbar

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_UNDO_TAG

/**
 * The Undo control: the word "Undo" as a text button, calling [onClick] to take back the last
 * edit, one at a time, down to the original photo. While [enabled] is false (nothing to take
 * back, or an edit running) it is drawn faint and ignores taps.
 *
 * A text button is Material's quietest button, a word with no container, the usual look for a
 * secondary action in a top bar. Its visible word is also what screen readers say for it.
 */
@Composable
internal fun UndoButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.testTag(TOOL_UNDO_TAG),
    ) {
        Text(stringResource(R.string.tool_undo))
    }
}

/** Undo in light and dark: usable, then switched off. */
@PreviewLightDark
@Composable
private fun UndoButtonPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Row {
            UndoButton(enabled = true, onClick = {})
            UndoButton(enabled = false, onClick = {})
        }
    }
}
