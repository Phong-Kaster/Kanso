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
import com.phongkaster.kanso.data.edit.edit.LayerSize
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.component.SliderWithValue
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/** Test tag of a layer sheet's size slider. */
const val LAYER_SIZE_TAG = "layer-size"

/**
 * The Size tab of every layer sheet (Stickers, Text, Date): one slider from 1 to 100 for the
 * selected layer ([size]; see [LayerSize] for what the number means for each kind), its number at
 * the right end. It calls [onSizeChange] while it moves, which the screen shows at once without an
 * undo step, and [onChangeFinished] when the finger lets go, which makes the whole drag one undo
 * step, like the Adjust sliders. Greyed out while [enabled] is false.
 */
@Composable
internal fun LayerSizeSlider(
    size: Float,
    enabled: Boolean,
    onSizeChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizeText = stringResource(R.string.layer_size)
    Box(
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
        },
    )
}

/** The size slider at 30, in light and in dark. */
@PreviewLightDark
@Composable
private fun LayerSizeSliderPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = { LayerSizeSlider(size = 30f, enabled = true, onSizeChange = {}, onChangeFinished = {}) },
        )
    }
}
