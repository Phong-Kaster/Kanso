package com.photo.editor.common.ui.foundation

import android.annotation.SuppressLint
import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.SubColor


@SuppressLint("DefaultLocale")
@Composable
fun BaseSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0)
    steps: Int = 0,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(20.dp)
            .drawBehind {
                drawLine(
                    color = AlternativeColor,
                    start = Offset(3.dp.toPx(), size.height / 2),
                    end = Offset(size.width - 3.dp.toPx(), size.height / 2),
                    cap = StrokeCap.Round,
                    strokeWidth = 3.dp.toPx()
                )

                if (value > valueRange.start) {
                    val startX = 3.dp.toPx()
                    val endX = (size.width * (value - valueRange.start) / (valueRange.endInclusive - valueRange.start) - 3.dp.toPx()).fastCoerceAtLeast(startX)
                    drawLine(
                        color = SubColor,
                        start = Offset(startX, size.height / 2),
                        end = Offset(endX, size.height / 2),
                        cap = StrokeCap.Round,
                        strokeWidth = 3.dp.toPx()
                    )
                }
            },
        valueRange = valueRange,
        steps = steps,
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

@SuppressLint("DefaultLocale")
@Composable
fun BaseSlider2(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0)
    steps: Int = 0,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(20.dp)
            .drawBehind {
                drawLine(
                    color = Color.Black,
                    start = Offset(3.dp.toPx(), size.height / 2),
                    end = Offset(size.width - 3.dp.toPx(), size.height / 2),
                    cap = StrokeCap.Round,
                    strokeWidth = 3.dp.toPx()
                )
            },
        valueRange = valueRange,
        steps = steps,
        thumb = {
            Spacer(
                modifier = Modifier
                    .size(16.dp)
                    .circle()
                    .background(Color.Black)
            )
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun Preview() {
    Column(modifier = Modifier.padding(16.dp)) {
        BaseSlider(value = 0f, onValueChange = {})
        BaseSlider(value = 0.01f, onValueChange = {})
        BaseSlider(value = 0.1f, onValueChange = {})
        BaseSlider(value = 0.5f, onValueChange = {})
        BaseSlider(value = 0.8f, onValueChange = {})
        BaseSlider(value = 1f, onValueChange = {})
        BaseSlider2(value = 0f, onValueChange = {})
        BaseSlider2(value = 0.1f, onValueChange = {})
        BaseSlider2(value = 0.5f, onValueChange = {})
        BaseSlider2(value = 0.8f, onValueChange = {})
        BaseSlider2(value = 1f, onValueChange = {})

    }
}