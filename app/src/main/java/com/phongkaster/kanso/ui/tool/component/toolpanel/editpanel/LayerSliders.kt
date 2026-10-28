package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.phongkaster.kanso.data.edit.edit.LayerSize
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.component.SliderWithValue
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/** Test tag of a layer sheet's size slider. */
const val LAYER_SIZE_TAG = "layer-size"

/** Test tag of a layer sheet's opacity slider. */
const val LAYER_OPACITY_TAG = "layer-opacity"

/** How many percent an opacity of 1 is. */
private const val PERCENT = 100f

/**
 * The two sliders every layer sheet (Stickers and Text) shows for the selected layer, always in
 * the same place under the sheet's top bar: **Size**, from 1 to 100 ([size], see [LayerSize] for
 * what the number means for each kind), and **Opacity**, from 10 to 100 percent ([opacity], 0.1
 * to 1).
 *
 * The sliders are always drawn, so the sheet never jumps when a layer is selected; with nothing
 * selected [enabled] is false and they are greyed out. Each slider calls its change callback
 * ([onSizeChange], [onOpacityChange]) while it moves, which the screen shows at once without an
 * undo step, and [onChangeFinished] when the finger lets go, which makes the whole drag one undo
 * step, like the Adjust sliders.
 */
@Composable
internal fun LayerSliders(
    size: Float,
    opacity: Float,
    enabled: Boolean,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizeText = stringResource(R.string.layer_size)
    val opacityText = stringResource(R.string.layer_opacity)
    Column(
        modifier = modifier,
        content = {
            LabelledRow(
                label = R.string.layer_size,
                control = {
                    SliderWithValue(
                        value = size,
                        onValueChange = onSizeChange,
                        valueText = stringResource(R.string.edit_session_slider_value, size.roundToInt()),
                        valueTag = editSliderValueTag(LAYER_SIZE_TAG),
                        valueRange = LayerSize.MIN..LayerSize.MAX,
                        onValueChangeFinished = onChangeFinished,
                        enabled = enabled,
                        sliderModifier = Modifier
                            .testTag(LAYER_SIZE_TAG)
                            .semantics { contentDescription = sizeText },
                    )
                },
            )
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

/** The sliders of a selected layer at size 30 and 80 percent opacity, in light and in dark. */
@PreviewLightDark
@Composable
private fun LayerSlidersPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                LayerSliders(
                    size = 30f,
                    opacity = 0.8f,
                    enabled = true,
                    onSizeChange = {},
                    onOpacityChange = {},
                    onChangeFinished = {},
                )
            },
        )
    }
}

/** The sliders greyed out, as they are while no layer is selected, in light and in dark. */
@PreviewLightDark
@Composable
private fun LayerSlidersDisabledPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                LayerSliders(
                    size = LayerSize.MIN,
                    opacity = 1f,
                    enabled = false,
                    onSizeChange = {},
                    onOpacityChange = {},
                    onChangeFinished = {},
                )
            },
        )
    }
}
