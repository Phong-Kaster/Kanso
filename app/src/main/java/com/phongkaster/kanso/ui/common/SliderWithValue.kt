package com.phongkaster.kanso.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The width of the number box beside the slider: wide enough for "-100" and "100%" at large text. */
private val VALUE_BOX_WIDTH = 64.dp

/**
 * A [ClassicSlider] with its current number beside it, so the person reads the exact value while
 * dragging: the slider fills the row and the number sits at its right end in a rounded box in the
 * theme's secondary container colour, in a larger, bolder type than a label so it is easy to see.
 *
 * [valueText] is the text to show (the caller formats it, "55" or "55%"); [valueTag] is the test
 * tag of that text. The other parameters go to the slider unchanged. The number box has a fixed
 * width, so the slider does not change length as the number gains or loses digits.
 */
@Composable
fun SliderWithValue(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueText: String,
    modifier: Modifier = Modifier,
    valueTag: String? = null,
    sliderModifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ClassicSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            enabled = enabled,
            modifier = sliderModifier.weight(1f),
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.width(VALUE_BOX_WIDTH),
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .then(if (valueTag != null) Modifier.testTag(valueTag) else Modifier),
            )
        }
    }
}

/** The slider with its number, in light and in dark: one at 40% and one at the far end. */
@PreviewLightDark
@Composable
private fun SliderWithValuePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column {
                    SliderWithValue(value = 0.4f, onValueChange = {}, valueText = "40%")
                    SliderWithValue(value = 1f, onValueChange = {}, valueText = "100%")
                }
            }
        }
    }
}
