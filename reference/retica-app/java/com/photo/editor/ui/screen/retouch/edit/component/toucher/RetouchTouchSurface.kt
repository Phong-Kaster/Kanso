package com.photo.editor.ui.screen.retouch.edit.component.toucher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny

operator fun Offset.div(size: IntSize): Offset = Offset(x / size.width, y / size.height)
operator fun Offset.div(size: Size): Offset = Offset(x / size.width, y / size.height)

operator fun Offset.times(size: IntSize): Offset = Offset(x * size.width, y * size.height)
operator fun Offset.times(size: Size): Offset = Offset(x * size.width, y * size.height)

@Composable
fun RetouchTouchSurface(
    touchState: RetouchTouchState,
    modifier: Modifier = Modifier,
    onPointAdded: (Offset) -> Unit,
) {
    val touchModifier = modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()

            // Detect if user is doing transform gestures by zoom change
            // If so, wait until all pointers are released
            val zoom = withTimeoutOrNull(20) {
                val event = awaitPointerEvent()
                val zoomChange = event.calculateZoom()
                zoomChange
            }
            if (zoom != null) {
                // Wait until all pointers are released
                do {
                    val event = awaitPointerEvent()
                    val canceled = event.changes.fastAny { it.isConsumed }
                } while (!canceled && event.changes.fastAny { it.pressed })
                return@awaitEachGesture
            }

            onPointAdded(down.position / size)
//            touchState.points.add(down.position / size)
        }
    }

    Canvas(modifier = touchModifier) {
        touchState.points.forEach { point ->
            drawCircle(
                color = Color.Red,
                radius = 10f,
                center = point * size
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    RetouchTouchSurface(
        touchState = remember { RetouchTouchState() },
        modifier = Modifier.fillMaxSize(),
        onPointAdded = {}
    )
}