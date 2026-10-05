package com.photo.editor.ui.screen.edit.component.bottombar.edit.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.editimage.model.color.ColorTintData
import com.photo.editor.common.ui.foundation.BaseSlider
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.foundation.SpaceBetweenCenterRow
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.ui.screen.edit.common.EditImageColorState
import com.photo.editor.common.ui.theme.bold12
import com.photo.editor.common.ui.theme.regular12

@Composable
fun ColorRGBEditor(
    colorState: EditImageColorState,
    modifier: Modifier = Modifier
) {
    var shadowActive by rememberSaveable { mutableStateOf(true) }
    val colorTint = colorState.colorDataFlow.collectAsState().value.colorTint
    val sliderValue = if (shadowActive) colorTint.shadowIntensity else colorTint.highlightIntensity

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(0.7f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SpaceBetweenCenterRow(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.intensity),
                style = regular12,
                color = Color.White,
            )

            Text(
                text = (sliderValue * 100).toInt().toString(),
                style = regular12,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }

        BaseSlider(
            value = sliderValue,
            onValueChange = {
                val newState = if (shadowActive) colorTint.copy(shadowIntensity = it)
                else colorTint.copy(highlightIntensity = it)

                colorState.updateColorTint(newState)
            },
            modifier = Modifier
                .fillMaxWidth()
        )

        CenterRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            ColorTintData.HighlightShadow.entries.forEach {
                CenterBox(modifier = Modifier.weight(1f)) {
                    Spacer(
                        modifier = Modifier
                            .size(24.dp)
                            .circle()
                            .border(2.dp, Color(it.presentationColor), CircleShape)
                            .then(
                                if (shadowActive && colorTint.shadowColor == it)
                                    Modifier
                                        .border(8.dp, Color(it.presentationColor), CircleShape)
                                        .background(Color.White)
                                else if (!shadowActive && colorTint.highlightColor == it)
                                    Modifier
                                        .border(8.dp, Color(it.presentationColor), CircleShape)
                                        .background(Color.White)
                                else Modifier.clickable {
                                    if (shadowActive) colorState.updateColorTint(colorTint.copy(shadowColor = it))
                                    else colorState.updateColorTint(colorTint.copy(highlightColor = it))
                                }
                            )
                    )
                }
            }
        }

        CenterRow(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.shadows),
                style = bold12,
                color = if (shadowActive) Color.White else Color.White.copy(0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable { shadowActive = true }
                    .padding(vertical = 12.dp)
            )

            Text(
                text = stringResource(R.string.highlight),
                style = bold12,
                color = if (!shadowActive) Color.White else Color.White.copy(0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable { shadowActive = false }
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Text(
            text = value.toInt().toString(),
            style = regular12,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(16.dp)
                .align(
                    BiasAlignment(
                        (value * 2 - 100) / 100,
                        -1f
                    )
                )
        )

        BaseSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(16.dp),
            valueRange = 0f..100f,
            steps = 101,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    ColorRGBEditor(
        colorState = EditImageColorState()
    )
}