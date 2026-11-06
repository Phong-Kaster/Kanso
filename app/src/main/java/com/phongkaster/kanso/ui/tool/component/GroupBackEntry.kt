package com.phongkaster.kanso.ui.tool.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EditToolEntry

/** The narrowest the Back entry gets, so its short name still makes a comfortable target. */
private val BACK_ENTRY_MIN_WIDTH = 72.dp

/**
 * The Back entry of a group's row: an arrow over the word Back, a button that returns to the top
 * row. Each row decides where it pins it (Edit at the left, Beautify at the right). Beautify's and
 * Edit's group rows both use it, each passing its own [testTag] so a test can find the right one. While [enabled] is false it is drawn faint and ignores taps.
 */
@Composable
internal fun GroupBackEntry(enabled: Boolean, onClick: () -> Unit, modifier: Modifier, testTag: String) {
    EditToolEntry(
        icon = R.drawable.ic_arrow_back,
        title = R.string.back,
        modifier = modifier.widthIn(min = BACK_ENTRY_MIN_WIDTH).testTag(testTag),
        enabled = enabled,
        tap = { Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) },
    )
}

/** The Back entry, in light and in dark. */
@PreviewLightDark
@Composable
private fun GroupBackEntryPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            GroupBackEntry(enabled = true, onClick = {}, modifier = Modifier, testTag = "group-back")
        }
    }
}
