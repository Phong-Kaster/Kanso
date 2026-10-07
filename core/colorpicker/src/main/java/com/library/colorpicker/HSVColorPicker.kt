package com.library.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.library.colorpicker.foundation.CenterRow
import com.library.colorpicker.modifier.circle
import com.library.colorpicker.modifier.rounded

@Composable
fun HSVColorPicker(
    color: Color = Color.White,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp),
    refreshKey: Any? = null,
    aspectRatio: Float = 2f,
    showAlpha: Boolean = true,
) {
    var colorPickerValue by rememberSaveable(refreshKey, stateSaver = HSVColor.Saver) {
        mutableStateOf(HSVColor.from(color))
    }
    val updatedOnColorChanged by rememberUpdatedState(onColorChanged)
    var inputColorCode by remember(refreshKey) { mutableStateOf(colorPickerValue.toHex()) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CenterRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            itemSpacing = 8.dp
        ) {
            BasicTextField(
                value = inputColorCode.uppercase(),
                onValueChange = {
                    inputColorCode = it.filter { char -> char in '0'..'9' || char in 'A'..'F' }.take(6)
                    tryWithoutCatch {
                        val colorValue = inputColorCode.toLong()
                        colorPickerValue = HSVColor.from(Color(colorValue))
                        updatedOnColorChanged(colorPickerValue.toColor())
                    }
                },
                modifier = Modifier.weight(1f),
                textStyle = textStyle.copy(Color(0xFF090B0E)),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .width(IntrinsicSize.Max)
                            .circle()
                            .background(Color.White)
                            .border(1.dp, Color(0xFFA3ADBB), CircleShape)
                            .padding(vertical = 6.dp, horizontal = 16.dp),
                    ) {
                        Text(
                            text = "Hex",
                            style = textStyle,
                            color = Color(0xFF606060),
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        Text(text = "#", style = textStyle, color = Color(0xFF090B0E))

                        innerTextField()
                    }
                },
            )

            if (showAlpha) {
            Box(
                modifier = Modifier
                    .circle()
                    .background(Color.White)
                    .border(1.dp, Color(0xFFA3ADBB), CircleShape)
                    .padding(vertical = 6.dp, horizontal = 16.dp),
            ) {
                Text(text = "100%", style = textStyle, color = Color.Transparent)
                Text(text = "${(colorPickerValue.alpha * 100).toInt()}%", style = textStyle, color = Color.Black)
            }
            }
        }

        SaturationValueArea(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .rounded(4.dp),
            refreshKey = refreshKey,
            currentColor = colorPickerValue,
            onSaturationValueChanged = { saturation, value ->
                colorPickerValue = colorPickerValue.copy(saturation = saturation, value = value)
                updatedOnColorChanged(colorPickerValue.toColor())
                inputColorCode = colorPickerValue.toHex()
            }
        )

        HueBar(
            currentColor = colorPickerValue,
            refreshKey = refreshKey,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .circle(),
            onHueChanged = { newHue ->
                colorPickerValue = colorPickerValue.copy(hue = newHue)
                updatedOnColorChanged(colorPickerValue.toColor())
                inputColorCode = colorPickerValue.toHex()
            }
        )

        if (showAlpha) {
        AlphaBar(
            currentColor = colorPickerValue,
            refreshKey = refreshKey,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .circle(),
            onAlphaChanged = { newAlpha ->
                colorPickerValue = colorPickerValue.copy(alpha = newAlpha)
                updatedOnColorChanged(colorPickerValue.toColor())
                inputColorCode = colorPickerValue.toHex()
            }
        )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    HSVColorPicker(
        onColorChanged = {},
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    )
}