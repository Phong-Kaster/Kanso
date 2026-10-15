package com.phongkaster.kanso.ui.tool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the small pill floating on the Edit screen's photo, holding Undo and Compare. */
const val EDIT_PILL_TAG = "edit-pill"

/** Test tag of the pill's Compare switch, which shows the original photo while it is on. */
const val EDIT_COMPARE_TAG = "edit-compare"

/**
 * The small rounded pill that floats over the bottom-right corner of the Edit screen's photo,
 * holding the two controls that must stay in reach whatever sheet is open: Undo and Compare.
 *
 * - Undo (an undo icon) calls [onUndo] to take back the last edit; it is faint and ignores taps
 *   while [undoEnabled] is false (nothing to take back, an edit running, or Compare on).
 * - Compare is a switch, on while [comparing]; flipping it calls [onCompare] with the new value.
 *   It is never disabled, because it is the way back from comparing.
 *
 * The pill is a `Surface` in the theme's high surface-container colour with a shadow, so it reads
 * over a light or a dark photo alike. Both controls keep Material's 48 dp minimum touch target.
 */
@Composable
fun EditPill(
    undoEnabled: Boolean,
    comparing: Boolean,
    onUndo: () -> Unit,
    onCompare: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val compareText = stringResource(R.string.edit_layout_compare)
    Surface(
        modifier = modifier.testTag(EDIT_PILL_TAG),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // An icon button is 48 dp square by default, Material's minimum touch target.
            IconButton(
                onClick = onUndo,
                enabled = undoEnabled,
                modifier = Modifier.testTag(TOOL_UNDO_TAG),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = stringResource(R.string.edit_layout_undo),
                )
            }
            // The whole icon-and-switch group is one toggle, so it is easy to hit and a screen
            // reader reads it as one switch named "Compare" that is on or off. The Switch inside
            // takes no taps of its own (its callback is null): the row handles them.
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .toggleable(value = comparing, role = Role.Switch, onValueChange = onCompare)
                    .semantics { contentDescription = compareText }
                    .testTag(EDIT_COMPARE_TAG)
                    .padding(start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_compare), contentDescription = null)
                Switch(checked = comparing, onCheckedChange = null)
            }
        }
    }
}

/**
 * The pill with an edit to take back and Compare off, in light and in dark. The outer Box fills
 * the preview first, because KansoTheme would otherwise stretch its single child to the screen.
 */
@PreviewLightDark
@Composable
private fun EditPillPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            EditPill(undoEnabled = true, comparing = false, onUndo = {}, onCompare = {})
        }
    }
}

/** The pill while Compare is on, in light and in dark: the switch is on and Undo is faint. */
@PreviewLightDark
@Composable
private fun EditPillComparingPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            EditPill(undoEnabled = false, comparing = true, onUndo = {}, onCompare = {})
        }
    }
}
