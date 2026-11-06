package com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The width of the pinned Compare entry. */
private val ACTION_ENTRY_WIDTH = 68.dp

/**
 * The thin divider and the pinned Compare switch that end both layers, so the two rows share one
 * definition. It must sit inside a [Row].
 */
@Composable
internal fun CompareEntry(comparing: Boolean, compareEnabled: Boolean, onCompare: (Boolean) -> Unit) {
    // A thin line separates the entries (which scroll) from Compare (which stays put).
    VerticalDivider(modifier = Modifier.padding(horizontal = 4.dp).height(40.dp))
    EditToolEntry(
        icon = R.drawable.ic_compare,
        title = R.string.edit_layout_compare,
        modifier = Modifier.width(ACTION_ENTRY_WIDTH).testTag(EDIT_COMPARE_TAG),
        highlighted = comparing,
        enabled = compareEnabled,
        // A toggle, so a screen reader reads it as one switch named Compare, on or off.
        tap = {
            Modifier.toggleable(
                value = comparing,
                enabled = compareEnabled,
                role = Role.Switch,
                onValueChange = onCompare,
            )
        },
    )
}

/** The divider and the Compare switch switched on, in light and in dark. */
@PreviewLightDark
@Composable
private fun CompareEntryPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            CompareEntry(comparing = true, compareEnabled = true, onCompare = {})
        }
    }
}
