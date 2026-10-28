package com.phongkaster.kanso.ui.tool.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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

/** Test tag of the "Discard your changes?" dialog. */
const val DISCARD_DIALOG_TAG = "discard-dialog"

/** Test tag of the dialog's "Discard" button, which leaves the Edit screen and loses the edits. */
const val DISCARD_CONFIRM_TAG = "discard-confirm"

/** Test tag of the dialog's "Keep editing" button, which closes the dialog and stays. */
const val DISCARD_KEEP_TAG = "discard-keep"

/**
 * The question asked when the person leaves the Edit screen with changes not saved: "Discard your
 * changes?", with "Discard" ([onDiscard]: leave, the edits are lost) and "Keep editing"
 * ([onKeepEditing]: stay, nothing lost). Tapping outside the dialog or pressing Back counts as
 * Keep editing, the safe answer.
 *
 * It is stateless: the screen shows it while its UiState asks for it (`discardPrompt`) and
 * forwards both answers to the ViewModel. An `AlertDialog` is Material 3's small modal question:
 * it sits above the screen in its own window, so nothing behind it can be tapped meanwhile.
 */
@Composable
fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(text = stringResource(R.string.edit_session_discard_title)) },
        confirmButton = {
            // Discard throws the session away, so it is drawn in the error colour: it must not look
            // the same as the safe Keep editing beside it.
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag(DISCARD_CONFIRM_TAG),
            ) {
                Text(text = stringResource(R.string.edit_session_discard_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing, modifier = Modifier.testTag(DISCARD_KEEP_TAG)) {
                Text(text = stringResource(R.string.edit_session_discard_keep))
            }
        },
        modifier = modifier.testTag(DISCARD_DIALOG_TAG),
    )
}

/**
 * The "Discard your changes?" dialog, in light and in dark, over an empty screen; both buttons do
 * nothing here. The outer Box fills the preview first, because KansoTheme would otherwise stretch
 * its single child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun DiscardChangesDialogPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            DiscardChangesDialog(onDiscard = {}, onKeepEditing = {})
        }
    }
}
