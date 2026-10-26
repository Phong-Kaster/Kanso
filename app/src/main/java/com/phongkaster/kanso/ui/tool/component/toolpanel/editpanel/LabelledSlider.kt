package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.component.SliderWithValue
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/**
 * The ending of the test tag of the number shown beside a slider's name: the brightness slider's
 * number is tagged `edit-brightness-value` (see [editSliderValueTag]).
 */
const val EDIT_SLIDER_VALUE = "-value"

/** A slider value of 1 is shown as this number: the -1..1 settings are shown as -100..100. */
private const val SHOWN_SCALE = 100f

/**
 * Returns the test tag of the number beside the slider tagged [sliderTag], such as
 * `edit-brightness-value`.
 */
fun editSliderValueTag(sliderTag: String): String = sliderTag + EDIT_SLIDER_VALUE

/**
 * A slider with its [label] and its number on the left, at least 48 dp tall: shows [value] within
 * [valueRange] (by default the colour sliders' -1..1, whose centre means no change), calls
 * [onValueChange] while it moves and [onValueChangeFinished] when the person lets go. The slider
 * only ever shows [value], which the caller takes from the session's settings, so it stays where
 * it was let go instead of springing back.
 *
 * The number is [value] times 100, rounded: -100..100 for a colour slider, 0..100 for the
 * intensity; it carries the test tag [editSliderValueTag] of [tag]. While [enabled] is false the
 * slider is disabled. The slider also carries the label as its description, so a screen reader
 * names it.
 */
@Composable
internal fun LabelledSlider(
    @StringRes label: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    tag: String,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float> = EditSettings.MIN..EditSettings.MAX,
) {
    val text = stringResource(label)
    val shown = stringResource(R.string.edit_session_slider_value, (value * SHOWN_SCALE).roundToInt())
    // The number sits at the slider's right end (see SliderWithValue), where the eye is while dragging.
    LabelledRow(label = label) {
        SliderWithValue(
            value = value,
            onValueChange = onValueChange,
            valueText = shown,
            valueTag = editSliderValueTag(tag),
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            sliderModifier = Modifier
                .testTag(tag)
                .semantics { contentDescription = text },
        )
    }
}

/** A slider a little right of its middle, in light and in dark. */
@PreviewLightDark
@Composable
private fun LabelledSliderPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        LabelledSlider(
            label = R.string.edit_layout_adjust,
            value = 0.4f,
            onValueChange = {},
            onValueChangeFinished = {},
            tag = "sample-slider",
            enabled = true,
        )
    }
}
