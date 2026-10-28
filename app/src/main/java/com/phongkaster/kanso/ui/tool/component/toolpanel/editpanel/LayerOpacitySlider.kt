package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.component.SliderWithValue
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/** Test tag of a layer sheet's opacity slider. */
const val LAYER_OPACITY_TAG = "layer-opacity"

/** How many percent an opacity of 1 is. */
private const val PERCENT = 100f

/**
 * The Opacity tab of every layer sheet (Stickers, Text, Date): one slider from 10 to 100 percent
 * for the selected layer ([opacity], 0.1 to 1), its percentage at the right end. It calls
 * [onOpacityChange] while it moves (shown at once, no undo step) and [onChangeFinished] when the
 * finger lets go (the drag becomes one undo step). Greyed out while [enabled] is false.
 */
@Composable
internal fun LayerOpacitySlider(
    opacity: Float,
    enabled: Boolean,
    onOpacityChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opacityText = stringResource(R.string.layer_opacity)
    Box(
        modifier = modifier,
        content = {
            LabelledRow(
                label = R.string.layer_opacity,
                control = {
                    SliderWithValue(
                        value = opacity,
                        onValueChange = onOpacityChange,
                        valueText = stringResource(R.string.layer_opacity_value, (opacity * PERCENT).roundToInt()),
                        valueTag = editSliderValueTag(LAYER_OPACITY_TAG),
                        valueRange = Layer.MIN_OPACITY..1f,
                        onValueChangeFinished = onChangeFinished,
                        enabled = enabled,
                        sliderModifier = Modifier
                            .testTag(LAYER_OPACITY_TAG)
                            .semantics { contentDescription = opacityText },
                    )
                },
            )
        },
    )
}

/** The opacity slider at 80 percent, in light and in dark. */
@PreviewLightDark
@Composable
private fun LayerOpacitySliderPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = { LayerOpacitySlider(opacity = 0.8f, enabled = true, onOpacityChange = {}, onChangeFinished = {}) },
        )
    }
}
