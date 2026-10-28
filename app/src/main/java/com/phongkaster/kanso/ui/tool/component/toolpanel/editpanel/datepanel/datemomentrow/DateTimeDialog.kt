package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.datepanel.datemomentrow

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import java.time.LocalDateTime

/** Test tag of the time picker dialog. */
const val DATE_TIME_DIALOG_TAG = "date-time-dialog"

/** Test tag of the time picker's OK button. */
const val DATE_TIME_OK_TAG = "date-time-ok"

/**
 * Material's clock-face time picker in a dialog, opened by the Date sheet's time button. It starts
 * on [moment]'s time; OK calls [onPick] with that moment at the chosen hour and minute (the day is
 * kept), and Cancel or a tap outside calls [onDismiss]. The dial counts 24 hours or 12 with AM/PM
 * as the phone's own clock does; the date's words follow its format, not this choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimeDialog(
    moment: LocalDateTime,
    onPick: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
) {
    // Android's own clock setting; spelled out in full because Kanso has a DateFormat of its own.
    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(initialHour = moment.hour, initialMinute = moment.minute, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(DATE_TIME_DIALOG_TAG),
        confirmButton = {
            TextButton(
                onClick = {
                    onPick(moment.withHour(state.hour).withMinute(state.minute))
                    onDismiss()
                },
                modifier = Modifier.testTag(DATE_TIME_OK_TAG),
                content = { Text(text = stringResource(R.string.date_picker_ok)) },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, content = { Text(text = stringResource(R.string.date_picker_cancel)) })
        },
        text = { TimePicker(state = state) },
    )
}

/** The time picker on 14:30, in light and in dark. */
@PreviewLightDark
@Composable
private fun DateTimeDialogPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        DateTimeDialog(moment = LocalDateTime.of(2026, 10, 28, 14, 30), onPick = {}, onDismiss = {})
    }
}
