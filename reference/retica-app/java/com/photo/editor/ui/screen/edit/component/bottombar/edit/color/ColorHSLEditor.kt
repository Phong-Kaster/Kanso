package com.photo.editor.ui.screen.edit.component.bottombar.edit.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.foundation.SpaceBetweenCenterRow
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.ui.screen.edit.common.EditImageColorState
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.regular12

@Composable
fun ColorHSLEditor(
    colorState: EditImageColorState,
    modifier: Modifier = Modifier,
) {
    val colorHSL = colorState.colorDataFlow.collectAsState().value.colorHSL
    var currentOption by rememberSaveable { mutableStateOf(ColorHSLOption.Red) }
    val hue = colorState.getHueByOption(colorHSL, currentOption)
    val saturation = colorState.getSaturationByOption(colorHSL, currentOption)
    val lightness = colorState.getLightnessByOption(colorHSL, currentOption)

    val hueBrush by remember {
        derivedStateOf {
            Brush.horizontalGradient(
                listOf(
                    Color.hsl((currentOption.hueValue - 20).let { if (it < 0) it + 360 else it }, 0.5f, 0.5f),
                    Color.hsl(currentOption.hueValue, 0.5f, 0.5f),
                    Color.hsl(currentOption.hueValue + 20, 0.5f, 0.5f)
                )
            )
        }
    }

    val saturationBrush by remember {
        derivedStateOf {
            Brush.horizontalGradient(
                listOf(
                    Color.hsl(currentOption.hueValue, 0f, 0.5f),
                    Color.hsl(currentOption.hueValue, 0.5f, 0.5f),
                    Color.hsl(currentOption.hueValue, 1f, 0.5f)
                )
            )
        }
    }

    val lightnessBrush by remember {
        derivedStateOf {
            Brush.horizontalGradient(
                listOf(
                    Color.hsl(currentOption.hueValue, 0.5f, 0f),
                    Color.hsl(currentOption.hueValue, 0.5f, 0.25f),
                    Color.hsl(currentOption.hueValue, 0.5f, 0.5f),
                    Color.hsl(currentOption.hueValue, 0.5f, 0.75f),
                    Color.hsl(currentOption.hueValue, 0.5f, 1f),
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(0.7f))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        CenterRow(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            itemSpacing = 10.dp,
        ) {
            Spacer(modifier = Modifier)
            ColorHSLOption.entries.forEach {
                Spacer(
                    modifier = Modifier
                        .size(18.dp)
                        .circle()
                        .then(
                            if (currentOption == it) Modifier
                                .background(Color.White)
                                .border(6.dp, it.getColor(), CircleShape)
                            else Modifier
                                .border(2.dp, it.getColor(), CircleShape)
                                .clickable { currentOption = it }
                        )

                )
            }
            Spacer(modifier = Modifier)
        }

        SpaceBetweenCenterRow(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.hue),
                style = regular12,
                color = Color.White,
            )

            Text(
                text = (hue * 100).toInt().toString(),
                style = regular12,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }

        ColorSlider(
            value = hue,
            onValueChange = {
                colorState.updateHslHue(currentOption, it)
            },
            brush = hueBrush,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        )

        SpaceBetweenCenterRow(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.saturation),
                style = regular12,
                color = Color.White,
            )

            Text(
                text = (saturation * 100).toInt().toString(),
                style = regular12,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }

        ColorSlider(
            value = saturation,
            onValueChange = {
                colorState.updateHslSaturation(currentOption, it)
            },
            brush = saturationBrush,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        )

        SpaceBetweenCenterRow(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.lightness),
                style = regular12,
                color = Color.White,
            )

            Text(
                text = (lightness * 100).toInt().toString(),
                style = regular12,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }

        ColorSlider(
            value = lightness,
            onValueChange = {
                colorState.updateHslLightness(currentOption, it)
            },
            brush = lightnessBrush,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        )
    }
}

@Composable
private fun ColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    brush: Brush,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(20.dp)
            .drawBehind {
                drawLine(
                    brush = brush,
                    start = Offset(3.dp.toPx(), size.height / 2),
                    end = Offset(size.width - 3.dp.toPx(), size.height / 2),
                    cap = StrokeCap.Round,
                    strokeWidth = 3.dp.toPx()
                )
            },
        valueRange = valueRange,
        thumb = {
            CenterBox(
                modifier = Modifier
                    .size(16.dp)
                    .circle()
                    .background(SubColor)
            ) {
                Spacer(
                    modifier = Modifier
                        .size(6.dp)
                        .circle()
                        .background(AlternativeColor)
                )
            }
        },
        track = { sliderState ->
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    ColorHSLEditor(
        colorState = EditImageColorState(),
    )
}