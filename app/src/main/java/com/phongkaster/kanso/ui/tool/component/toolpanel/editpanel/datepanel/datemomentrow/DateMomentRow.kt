package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.datepanel.datemomentrow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.DateFormat
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import java.time.LocalDateTime

/** Test tag of the Date sheet's day button, which opens the day picker. */
const val DATE_DAY_BUTTON_TAG = "date-day-button"

/** Test tag of the Date sheet's time button, which opens the time picker. */
const val DATE_TIME_BUTTON_TAG = "date-time-button"

/**
 * The Date sheet's two buttons for the moment a date shows: the day ("28 Oct 2026", with a
 * calendar icon) and the time ("14:30", with a clock icon), both read from [moment]. Each opens
 * its picker in a dialog ([DateDayDialog], [DateTimeDialog]); the picked moment goes out through
 * [onMoment]. Which dialog is open is the one thing this row remembers: no other part of the
 * screen needs it, and `rememberSaveable` keeps it open across a rotation.
 */
@Composable
internal fun DateMomentRow(
    moment: LocalDateTime,
    enabled: Boolean,
    onMoment: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingDay by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val day = DateFormat.DAY_SHORT_MONTH_YEAR.write(moment)
    val time = DateFormat.TIME_24.write(moment)
    val dayDescription = stringResource(R.string.date_pick_day, day)
    val timeDescription = stringResource(R.string.date_pick_time, time)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            AssistChip(
                onClick = { pickingDay = true },
                enabled = enabled,
                label = { Text(text = day) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar_today),
                        // The chip itself carries the spoken words.
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                modifier = Modifier
                    .testTag(DATE_DAY_BUTTON_TAG)
                    .semantics { contentDescription = dayDescription },
            )
            AssistChip(
                onClick = { pickingTime = true },
                enabled = enabled,
                label = { Text(text = time) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_schedule),
                        // The chip itself carries the spoken words.
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                modifier = Modifier
                    .testTag(DATE_TIME_BUTTON_TAG)
                    .semantics { contentDescription = timeDescription },
            )
        },
    )
    if (pickingDay) {
        DateDayDialog(moment = moment, onPick = onMoment, onDismiss = { pickingDay = false })
    }
    if (pickingTime) {
        DateTimeDialog(moment = moment, onPick = onMoment, onDismiss = { pickingTime = false })
    }
}

/** The two buttons for 28 October 2026 at 14:30, in light and in dark. */
@PreviewLightDark
@Composable
private fun DateMomentRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                DateMomentRow(moment = LocalDateTime.of(2026, 10, 28, 14, 30), enabled = true, onMoment = {})
            },
        )
    }
}
