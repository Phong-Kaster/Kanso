package com.photo.editor.ui.screen.retouch.edit.component.drawer

import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAny

suspend fun AwaitPointerEventScope.awaitDragMotionEvent(
    onDragStart: AwaitPointerEventScope.(PointerInputChange) -> Unit = {},
    onDrag: AwaitPointerEventScope.(PointerInputChange) -> Unit = {},
    onDragEnd: AwaitPointerEventScope.(PointerInputChange) -> Unit = {}
) {
    // Wait for at least one pointer to press down, and set first contact position
    val down: PointerInputChange = awaitFirstDown()

    // Detect if user is doing transform gestures by zoom change
    // If so, wait until all pointers are released
    val zoom = withTimeoutOrNull(5) {
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
        return
    }


    onDragStart(down)

    var pointer = down

    // 🔥 Waits for drag threshold to be passed by pointer or it returns null if up event is triggered
    val change = awaitDragOrCancellation(down.id)
    change?.consume()

    if (change != null) {
        // 🔥 Calls awaitDragOrCancellation(pointer) in a while loop
        drag(change.id) { pointerInputChange: PointerInputChange ->
            pointer = pointerInputChange
            onDrag(pointer)
        }

        // All of the pointers are up
        onDragEnd(pointer)
    } else {
        // Drag threshold is not passed and last pointer is up
        onDragEnd(pointer)
    }
}


fun Modifier.dragMotionEvent(
    onDragStart: AwaitPointerEventScope.(PointerInputChange) -> Unit = {},
    onDrag: AwaitPointerEventScope.(PointerInputChange) -> Unit = {},
    onDragEnd: AwaitPointerEventScope.(PointerInputChange) -> Unit = {},
) = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitDragMotionEvent(onDragStart, onDrag, onDragEnd)
        }
    }
)