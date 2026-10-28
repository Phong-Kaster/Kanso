package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.datepanel.datemomentrow

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import java.time.LocalDate
import java.time.LocalDateTime

/** Test tag of the day picker dialog. */
const val DATE_DAY_DIALOG_TAG = "date-day-dialog"

/** Test tag of the day picker's OK button. */
const val DATE_DAY_OK_TAG = "date-day-ok"

/** How many milliseconds one day lasts: the day picker counts days in milliseconds. */
private const val DAY_MS = 86_400_000L

/**
 * Material's day picker in a dialog, opened by the Date sheet's day button. It starts on
 * [moment]'s day; OK calls [onPick] with that moment on the chosen day (the time of day is kept),
 * and Cancel or a tap outside calls [onDismiss].
 *
 * **Why milliseconds at midnight UTC.** Material's `DatePicker` stores a day as the milliseconds
 * from 1 January 1970 to that day's midnight in UTC, whatever the phone's time zone. A day number
 * times [DAY_MS] is exactly that, and the picker's answer divided by [DAY_MS] is the day back,
 * so no time zone ever shifts the day by one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateDayDialog(
    moment: LocalDateTime,
    onPick: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = moment.toLocalDate().toEpochDay() * DAY_MS)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(DATE_DAY_DIALOG_TAG),
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) onPick(LocalDate.ofEpochDay(millis / DAY_MS).atTime(moment.toLocalTime()))
                    onDismiss()
                },
                modifier = Modifier.testTag(DATE_DAY_OK_TAG),
                content = { Text(text = stringResource(R.string.date_picker_ok)) },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, content = { Text(text = stringResource(R.string.date_picker_cancel)) })
        },
        content = { DatePicker(state = state) },
    )
}

/** The day picker on 28 October 2026, in light and in dark. */
@PreviewLightDark
@Composable
private fun DateDayDialogPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        DateDayDialog(moment = LocalDateTime.of(2026, 10, 28, 14, 30), onPick = {}, onDismiss = {})
    }
}
