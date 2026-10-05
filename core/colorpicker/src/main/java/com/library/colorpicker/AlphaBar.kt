package com.library.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * https://github.com/godaddy/compose-color-picker/blob/main/color-picker/src/commonMain/kotlin/com/godaddy/android/colorpicker/AlphaBar.kt
 * Alpha side bar Component that invokes onAlphaChanged when the value is mutated.
 *
 * @param modifier modifiers to set on the Alpha Bar
 * @param currentColor the initial color to set on the alpha bar.
 * @param onAlphaChanged the callback that is invoked when alpha value changes. 0 - 1.
 */
@Composable
internal fun AlphaBar(
    modifier: Modifier = Modifier,
    refreshKey: Any? = null,
    currentColor: HSVColor,
    onAlphaChanged: (Float) -> Unit,
) {
    val currentColorToAlphaBrush = remember(currentColor) {
        Brush.horizontalGradient(
            listOf(
                Color(0x00ffffff),
                currentColor.copy(alpha = 1.0f).toColor(),
            )
        )
    }

    Canvas(
        modifier = modifier
            .pointerInput(refreshKey) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onAlphaChanged(
                        getAlphaFromPosition(
                            down.position.x,
                            this.size.width.toFloat()
                        ).coerceIn(0f, 1f)
                    )
                    drag(down.id) { change ->
                        change.consume()
                        onAlphaChanged(
                            getAlphaFromPosition(
                                change.position.x,
                                this.size.width.toFloat()
                            ).coerceIn(0f, 1f)
                        )
                    }
                }
            }
    ) {
        clipRect { drawCheckeredBackground() }
        drawAlphaBar(currentColorToAlphaBrush)

        val position = getPositionFromAlpha(
            color = currentColor,
            maxWidth = this.size.width
        )
        drawSelectorPoint(amount = position)
    }
}

private fun DrawScope.drawSelectorPoint(amount: Float) {
    val strokeThickness = 1.dp.toPx()
    val radius = size.height / 2 - strokeThickness / 2

    val offset = Offset(y = size.height / 2, x = amount)

    drawCircle(
        color = Color.White,
        center = offset,
        style = Stroke(strokeThickness),
        radius = radius
    )
}

private fun DrawScope.drawAlphaBar(alphaBrush: Brush) {
    drawRect(alphaBrush)
}

private fun DrawScope.drawCheckeredBackground() {
    val darkColor = Color.LightGray
    val lightColor = Color.White

    val gridSizePx = 2.5.dp.toPx()
    val cellCountX = ceil(this.size.width / gridSizePx).toInt()
    val cellCountY = ceil(this.size.height / gridSizePx).toInt()
    for (i in 0 until cellCountX) {
        for (j in 0 until cellCountY) {
            val color = if ((i + j) % 2 == 0) darkColor else lightColor

            val x = i * gridSizePx
            val y = j * gridSizePx
            drawRect(color, Offset(x, y), Size(gridSizePx, gridSizePx))
        }
    }
}

private fun getPositionFromAlpha(color: HSVColor, maxWidth: Float): Float {
    val alpha = color.alpha
    return maxWidth * alpha
}

/**
 * @return new alpha calculated from the maxWidth
 */
private fun getAlphaFromPosition(x: Float, maxWidth: Float): Float {
    return x / maxWidth
}

@Preview
@Composable
private fun Preview() {
    var hsvColor by remember { mutableStateOf(HSVColor.from(Color(0xFFFF0000))) }

    AlphaBar(
        currentColor = hsvColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) { hsvColor = hsvColor.copy(alpha = it) }
}