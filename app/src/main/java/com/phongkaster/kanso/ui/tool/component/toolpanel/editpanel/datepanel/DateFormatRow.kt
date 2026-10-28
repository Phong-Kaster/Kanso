package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.datepanel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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

/** Test tag of the Date sheet's row of format chips. */
const val DATE_FORMATS_TAG = "date-formats"

/** Returns the test tag of the chip for [format], such as `date-format-YEAR`. */
fun dateFormatTag(format: DateFormat): String = "date-format-${format.name}"

/**
 * The Date sheet's row of the 20 formats ([DateFormat]), scrolling sideways. Each chip shows
 * [moment] written in its format, so the person reads exactly what the photo will say ("2026",
 * "Mon, 28 October 2026") instead of a format's name. The chip of [chosen] (the selected date's
 * format, or null with no date selected) is marked; a tap calls [onChoose], which adds a new date
 * in that format or rewrites the selected one (ADR-006). Every chip keeps Material's 48 dp touch
 * target.
 */
@Composable
internal fun DateFormatRow(
    moment: LocalDateTime,
    chosen: DateFormat?,
    enabled: Boolean,
    onChoose: (DateFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.date_formats_description)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(DATE_FORMATS_TAG)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            DateFormat.entries.forEach(
                action = { format ->
                    FilterChip(
                        selected = format == chosen,
                        onClick = { onChoose(format) },
                        enabled = enabled,
                        label = { Text(text = format.write(moment)) },
                        modifier = Modifier.testTag(dateFormatTag(format)),
                    )
                },
            )
        },
    )
}

/** The format row for 28 October 2026 at 14:30, the first format chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun DateFormatRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                DateFormatRow(
                    moment = LocalDateTime.of(2026, 10, 28, 14, 30),
                    chosen = DateFormat.WEEKDAY_FULL,
                    enabled = true,
                    onChoose = {},
                )
            },
        )
    }
}
