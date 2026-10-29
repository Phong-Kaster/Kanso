package com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.component.SliderWithValue
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.editSliderValueTag
import kotlin.math.roundToInt

/**
 * One Beautify slider, on two lines: its [label] on a line of its own, and under it the slider the
 * full width of the panel with its number at the right end (the app's [SliderWithValue]), so the
 * slider is as long as it can be and a small change is easy to make.
 *
 * It shows [value] within [valueRange], calls [onValueChange] while it moves and
 * [onValueChangeFinished] when the finger lifts. The number is [value] times 100, rounded, as on
 * the Edit screen's sliders: 0..100 for Smooth, -100..100 for Face width and Face lift (whose range
 * runs through 0, so the slider is drawn two-sided, coloured from 0 toward the thumb). The slider carries the
 * test tag [tag] and its number [editSliderValueTag] of it; a screen reader names the slider by
 * [label]. While [enabled] is false it is switched off.
 */
@Composable
fun BeautifySlider(
    @StringRes label: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    tag: String,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    val text = stringResource(label)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = {
            // The same type as the Edit screen's slider names.
            Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            SliderWithValue(
                value = value,
                onValueChange = onValueChange,
                valueText = stringResource(R.string.edit_session_slider_value, (value * SHOWN_SCALE).roundToInt()),
                valueTag = editSliderValueTag(tag),
                valueRange = valueRange,
                onValueChangeFinished = onValueChangeFinished,
                enabled = enabled,
                sliderModifier = Modifier
                    // 48 dp is the smallest touch target Android's accessibility guidance allows.
                    .heightIn(min = 48.dp)
                    .testTag(tag)
                    .semantics { contentDescription = text },
            )
        },
    )
}

/** A slider value of 1 is shown as this number: -1..1 is shown as -100..100. */
private const val SHOWN_SCALE = 100f

/** The Face width slider a little left of 0, in light and in dark. */
@PreviewLightDark
@Composable
private fun BeautifySliderPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            BeautifySlider(
                label = R.string.beautify_width_label,
                value = -0.3f,
                onValueChange = {},
                onValueChangeFinished = {},
                tag = "sample-slider",
                enabled = true,
                valueRange = -1f..1f,
            )
        }
    }
}
