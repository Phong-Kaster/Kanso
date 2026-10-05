package com.photo.editor.ui.screen.edit.component.bottombar.edit.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.photo.editor.common.ui.foundation.CenterBox
import kotlin.math.abs

@Stable
class ColorCurveState(
    initialPoints: List<Pair<Float, Float>> = listOf(
        0f to 0f,
        1f to 1f,
    )
) {
    // A point present position percentage of width and height in the curve
    val points = mutableStateListOf(*initialPoints.toTypedArray())

    var activePointIndex by mutableIntStateOf(-1)
        private set

    init {
        if (points.isEmpty()) {
            points.add(0f to 0f)
            points.add(1f to 1f)
        }
    }

    fun addPointOrUpdateCurrentNearbyPoint(x: Float, y: Float) {
        // Check if there is a point nearby the coordinate
        val nearbyIndex = points.indexOfFirst { abs(it.first - x) <= 0.05f }
        if (nearbyIndex >= 0) {
            // Update the nearby point
            points[nearbyIndex] = x to y
            // Set that point to active
            activePointIndex = nearbyIndex
            return
        }

        val index = points.indexOfFirst { it.first >= x }.coerceAtLeast(0)
        points.add(index, x to y)
        activePointIndex = index
    }

    fun moveActivePoint(x: Float, y: Float) {
        val index = activePointIndex
        if (index < 0 || index >= points.size) return

        val clampedX = if (index == 0) 0f
        else if (index == points.size - 1) 1f
        else run {
            val prev = points[index - 1]
            val next = points[index + 1]
            try {
                x.coerceIn(prev.first + 0.03f, next.first - 0.03f)
            } catch (_: Exception) {
                x
            }
        }
        val clampedY = y.coerceIn(0f, 1f)

        points[index] = clampedX to clampedY
    }

    fun deleteActivePoint() {
        if (activePointIndex == 0 || activePointIndex == points.size - 1) return
        points.removeAt(activePointIndex)
        activePointIndex = -1
    }
}

@Composable
fun BaseColorCurveEditor(
    state: ColorCurveState,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White,
    onCurveChange: () -> Unit = {},
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Canvas(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(state) {
                detectDragGestures(
                    onDragStart = {
                        state.addPointOrUpdateCurrentNearbyPoint(
                            x = it.x / size.width.coerceAtLeast(1),
                            y = 1 - it.y / size.height.coerceAtLeast(1)
                        )
                    }
                ) { change, _ ->
                    state.moveActivePoint(
                        x = change.position.x / size.width.coerceAtLeast(1),
                        y = 1 - change.position.y / size.height.coerceAtLeast(1)
                    )
                    onCurveChange()
                }
            }
            .pointerInput(state) {
                detectTapGestures {
                    state.addPointOrUpdateCurrentNearbyPoint(
                        x = it.x / size.width.coerceAtLeast(1),
                        y = 1 - it.y / size.height.coerceAtLeast(1)
                    )
                    onCurveChange()
                }
            }
            .clipToBounds()
    ) {
        // Draw grids
        drawPath(
            path = Path().apply {
                moveTo(0f, 0f)
                lineTo(0f, size.height.toFloat())
                lineTo(size.width.toFloat(), size.height.toFloat())
                lineTo(size.width.toFloat(), 0f)
                close()
            },
            color = Color.White,
            style = Stroke(1.dp.toPx())
        )
        for (i in 1..3) {
            val x = i * size.width / 4f
            drawLine(
                color = Color.White,
                alpha = 0.3f,
                start = Offset(x, 0f),
                end = Offset(x, size.height.toFloat()),
                strokeWidth = 1.dp.toPx(),
            )
            val y = i * size.height / 4f
            drawLine(
                color = Color.White,
                alpha = 0.3f,
                start = Offset(0f, y),
                end = Offset(size.width.toFloat(), y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw curve line
        val points = state.points.map { it.toOffset(this.size) }
        if (points.size == 2) {
            drawLine(
                color = lineColor,
                start = points[0],
                end = points[1],
                strokeWidth = 1.dp.toPx()
            )
        } else {
            val path = Path()

            path.apply {
                moveTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p0 = if (i == 0) points[i] else points[i - 1]
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]

                    // Calculate control points
                    val d1 = distance(p0, p1)
                    val d2 = distance(p1, p2)
                    val d3 = distance(p2, p3)

                    val a = 2 * d1 / (d1 + d2)
                    val b = 2 * d2 / (d2 + d3)

                    val b1 = Offset(
                        p1.x + (p2.x - p0.x) / 6 * a,
                        p1.y + (p2.y - p0.y) / 6 * a
                    )

                    val b2 = Offset(
                        p2.x - (p3.x - p1.x) / 6 * b,
                        p2.y - (p3.y - p1.y) / 6 * b
                    )

                    path.cubicTo(b1.x, b1.y, b2.x, b2.y, p2.x, p2.y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(1.dp.toPx())
            )
        }

        // Draw points
        points.forEachIndexed { index, offset ->
            val color = if (index == state.activePointIndex) Color.Yellow else Color.White
            drawCircle(color = color, radius = 6.dp.toPx(), center = offset)
        }
    }
}

private fun Pair<Float, Float>.toOffset(size: Size): Offset {
    return Offset(first * size.width, (1f - second) * size.height)
}

private fun distance(p1: Offset, p2: Offset): Float {
    return Math.sqrt(((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y)).toDouble()).toFloat()
}

@Preview
@Composable
private fun Preview() {
    CenterBox {
        BaseColorCurveEditor(
            state = remember { ColorCurveState() },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
        )
    }
}