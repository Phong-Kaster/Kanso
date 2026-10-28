package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The width of a slider's name, so the sliders line up one under another. */
private val LABEL_WIDTH = 96.dp

/** The width of the number beside a slider's name, room for "-100". */
private val VALUE_WIDTH = 44.dp

/**
 * A row at least 48 dp tall with [label] in a fixed-width column on the left, then, when given,
 * [value] (a slider's number, tagged [valueTag]) right beside it, and [control] filling the rest,
 * so labelled controls line up one under another.
 */
@Composable
internal fun LabelledRow(
    @StringRes label: Int,
    value: String? = null,
    valueTag: String? = null,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.width(LABEL_WIDTH),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .width(VALUE_WIDTH)
                    .padding(end = 8.dp)
                    .then(if (valueTag != null) Modifier.testTag(valueTag) else Modifier),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            control()
        }
    }
}

/** A labelled row with a value and a line of text standing in for the control, in light and in dark. */
@PreviewLightDark
@Composable
private fun LabelledRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        LabelledRow(label = R.string.edit_layout_adjust, value = "40") {
            Text(text = "control")
        }
    }
}
