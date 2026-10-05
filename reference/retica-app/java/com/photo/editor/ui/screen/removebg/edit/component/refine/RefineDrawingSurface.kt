package com.photo.editor.ui.screen.removebg.edit.component.refine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize

operator fun Offset.div(size: IntSize): Offset = Offset(x / size.width, y / size.height)
operator fun Offset.div(size: Size): Offset = Offset(x / size.width, y / size.height)

@Composable
fun RefineDrawingSurface(
    drawingState: RefineDrawingState,
    modifier: Modifier = Modifier,
    moveMode: Boolean = false,
) {
    val drawModifier = modifier.dragMotionEvent(
        onDragStart = { pointerInputChange ->
            drawingState.motionEvent = RefineMotionEvent.Down
            drawingState.currentPosition = pointerInputChange.position
            pointerInputChange.consume()

        },
        onDrag = { pointerInputChange ->
            drawingState.motionEvent = RefineMotionEvent.Move
            drawingState.currentPosition = pointerInputChange.position
            pointerInputChange.consume()

        },
        onDragEnd = { pointerInputChange ->
            drawingState.motionEvent = RefineMotionEvent.Up
            pointerInputChange.consume()
        },
    )

    Canvas(modifier = if (moveMode) modifier else drawModifier) {
        when (drawingState.motionEvent) {
            RefineMotionEvent.Down -> {
                drawingState.currentPath.moveTo(drawingState.currentPosition.x, drawingState.currentPosition.y)
                drawingState.currentPathPoints.add(drawingState.currentPosition / size)

                drawingState.previousPosition = drawingState.currentPosition

            }

            RefineMotionEvent.Move -> {
                drawingState.currentPath.quadraticTo(
                    drawingState.previousPosition.x,
                    drawingState.previousPosition.y,
                    (drawingState.previousPosition.x + drawingState.currentPosition.x) / 2,
                    (drawingState.previousPosition.y + drawingState.currentPosition.y) / 2
                )

                drawingState.previousPosition = drawingState.currentPosition
                drawingState.currentPathPoints.add(drawingState.currentPosition / size)
            }

            RefineMotionEvent.Up -> {
                drawingState.currentPath.lineTo(drawingState.currentPosition.x, drawingState.currentPosition.y)
                drawingState.currentPathPoints.add(drawingState.currentPosition / size)

                // Pointer is up save current path
                // Save the current path points
                drawingState.paths.add(Pair(drawingState.currentPathPoints.toList(), drawingState.currentPathProperty))

                // Since paths are keys for map, use new one for each key
                // and have separate path for each down-move-up gesture cycle
                drawingState.currentPath = Path()
                drawingState.currentPathPoints.clear()

                // Create new instance of path properties to have new path and properties
                // only for the one currently being drawn
                drawingState.currentPathProperty = RefinePathProperties(
                    strokeWidth = drawingState.currentPathProperty.strokeWidth,
                    eraseMode = drawingState.currentPathProperty.eraseMode
                )

                // Since new path is drawn no need to store paths to undone
                drawingState.pathsUndone.clear()

                // If we leave this state at MotionEvent.Up it causes current path to draw
                // line from (0,0) if this composable recomposes when draw mode is changed
                drawingState.currentPosition = Offset.Unspecified
                drawingState.previousPosition = drawingState.currentPosition
                drawingState.motionEvent = RefineMotionEvent.Idle
            }

            else -> Unit
        }

        with(drawContext.canvas.nativeCanvas) {

            val checkPoint = saveLayer(null, null)

            drawingState.paths.forEach { (points, property) ->
                val path = Path().apply {
                    points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x * size.width, point.y * size.height)
                        else quadraticTo(
                            (points[index - 1].x + point.x) / 2 * size.width,
                            (points[index - 1].y + point.y) / 2 * size.height,
                            point.x * size.width,
                            point.y * size.height
                        )
                    }
                }

                drawPath(
                    color = Color.Transparent,
                    path = path,
                    style = Stroke(
                        width = property.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                    blendMode = BlendMode.Clear,
                )

                drawPath(
                    color = if (!property.eraseMode) Color.Red.copy(0.5f) else Color.Green.copy(0.5f),
                    path = path,
                    style = Stroke(
                        width = property.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                )
            }

            if (drawingState.motionEvent != RefineMotionEvent.Idle) {
                drawPath(
                    color = Color.Transparent,
                    path = drawingState.currentPath,
                    style = Stroke(
                        width = drawingState.currentPathProperty.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                    blendMode = BlendMode.Clear
                )

                drawPath(
                    color = if (!drawingState.currentPathProperty.eraseMode) Color.Red.copy(0.5f) else Color.Green.copy(0.5f),
                    path = drawingState.currentPath,
                    style = Stroke(
                        width = drawingState.currentPathProperty.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                )
            }
            restoreToCount(checkPoint)
        }

    }
}

@Preview
@Composable
private fun PreviewDrawingSurface() {
    RefineDrawingSurface(
        drawingState = remember { RefineDrawingState() },
        modifier = Modifier.fillMaxSize()
    )
}