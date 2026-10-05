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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Hue side bar Component that invokes onHueChanged when the value is mutated.
 *
 * @param modifier modifiers to set to the hue bar.
 * @param currentColor the initial color to set on the hue bar.
 * @param onHueChanged the callback that is invoked when hue value changes. Hue is between 0 - 360.
 */
@Composable
internal fun HueBar(
    modifier: Modifier = Modifier,
    refreshKey: Any? = null,
    currentColor: HSVColor,
    onHueChanged: (Float) -> Unit
) {
    val rainbowBrush = remember { Brush.horizontalGradient(getRainbowColors()) }

    Canvas(
        modifier = modifier
            .pointerInput(refreshKey) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onHueChanged(getHueFromPoint(down.position.x, size.width.toFloat()))
                    drag(down.id) { change ->
                        change.consume()
                        onHueChanged(getHueFromPoint(change.position.x, size.width.toFloat()))
                    }
                }
            }
    ) {
        drawRect(rainbowBrush)
        val huePoint = getPointFromHue(color = currentColor, width = this.size.width)
        drawSelectorPoint(huePoint)
    }
}

private fun DrawScope.drawSelectorPoint(huePoint: Float) {
    val strokeThickness = 1.dp.toPx()
    val radius = size.height / 2 - strokeThickness / 2

    val offset = Offset(y = size.height / 2, x = huePoint)

    drawCircle(
        color = Color.White,
        center = offset,
        style = Stroke(strokeThickness),
        radius = radius
    )
}

private fun getRainbowColors(): List<Color> {
    return listOf(
        Color(0xFFFF0040),
        Color(0xFFFF00FF),
        Color(0xFF8000FF),
        Color(0xFF0000FF),
        Color(0xFF0080FF),
        Color(0xFF00FFFF),
        Color(0xFF00FF80),
        Color(0xFF00FF00),
        Color(0xFF80FF00),
        Color(0xFFFFFF00),
        Color(0xFFFF8000),
        Color(0xFFFF0000)
    )
}

private fun getPointFromHue(color: HSVColor, width: Float): Float {
    return width - (color.hue * width / 360f)
}

private fun getHueFromPoint(x: Float, width: Float): Float {
    val newX = x.coerceIn(0f, width)
    return 360f - newX * 360f / width
}

@Preview
@Composable
private fun Preview() {
    var hsvColor by remember { mutableStateOf(HSVColor.from(Color.Blue)) }

    HueBar(
        currentColor = hsvColor,
        modifier = Modifier
        .fillMaxWidth()
        .height(8.dp),
        onHueChanged = { hsvColor = hsvColor.copy(hue = it) }
    )
}