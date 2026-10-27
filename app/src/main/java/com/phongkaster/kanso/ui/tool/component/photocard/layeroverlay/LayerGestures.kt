package com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.phongkaster.kanso.data.edit.edit.Sticker
import com.phongkaster.kanso.data.edit.edit.StickerMapping

/**
 * Where [this] sticker's centre is on the shown picture, in pixels of an overlay [size].
 * The sticker is stored as fractions of the ORIGINAL photo, so the position goes through the one
 * [StickerMapping] (which knows the crop, the quarter turns and the flips) and is then scaled up
 * from 0..1 to pixels. No other file does this arithmetic.
 */
internal fun Sticker.shownCentre(mapping: StickerMapping, size: Size): Offset {
    val p = mapping.originalToShown(StickerMapping.Point(centreX, centreY))
    return Offset(p.x * size.width, p.y * size.height)
}

/** This sticker's width on the shown picture in pixels of an overlay [size] (through [mapping]). */
internal fun Sticker.shownWidth(mapping: StickerMapping, size: Size): Float =
    mapping.originalWidthToShown(width) * size.width

/** This sticker's turn in degrees as the person sees it on the shown picture (through [mapping]). */
internal fun Sticker.shownDegrees(mapping: StickerMapping): Float =
    mapping.originalAngleToShown(degrees)

/** Whether this sticker is mirrored as the person sees it on the shown picture (through [mapping]). */
internal fun Sticker.shownMirrored(mapping: StickerMapping): Boolean =
    mapping.originalMirrorToShown(mirrored)

/**
 * A copy of this sticker moved, resized and turned to what the shown picture says: the [centre]
 * and [widthPx] in pixels of an overlay [size], and [degrees] as seen. They are carried back to
 * fractions of the original photo through [mapping], the exact inverse of the three functions
 * above. The picture and the mirror flag are not touched, so a move never mirrors anything.
 */
internal fun Sticker.placedAt(
    mapping: StickerMapping,
    size: Size,
    centre: Offset,
    widthPx: Float,
    degrees: Float,
): Sticker {
    val p = mapping.shownToOriginal(StickerMapping.Point(centre.x / size.width, centre.y / size.height))
    return copy(
        centreX = p.x,
        centreY = p.y,
        width = mapping.shownWidthToOriginal(widthPx / size.width),
        degrees = mapping.shownAngleToOriginal(degrees),
    )
}

/**
 * Puts the one handler on the sticker overlay that moves, pinches and turns the stickers.
 *
 * **Raw pointer input.** `pointerInput` gives a coroutine that reads every touch event itself.
 * `awaitEachGesture` repeats one gesture after another: `awaitFirstDown` waits for the first
 * finger (there is no touch slop here, the sticker moves from the first pixel), and
 * `awaitPointerEvent` then delivers each later event until no finger is left.
 *
 * **Why ONE handler for every sticker.** Stickers overlap and are turned, so the system's own
 * hit testing (a rectangle per node) cannot say which one is under a finger. This handler sits on
 * the whole overlay, takes the first finger's position, and tests the stickers itself from the
 * newest (drawn on top) to the oldest through [pointOnSticker], on the turned rectangle.
 *
 * **What a gesture does.** A finger down on a sticker selects it ([onSelect]) and starts the
 * gesture; one finger drags it by exactly the finger's move; two fingers scale it by the ratio of
 * their distance and turn it by the change of their angle, about the point between them. Every
 * move calls [onPreview] with the whole new list (the screen shows it at once and makes no undo
 * entry), and the last finger lifting calls [onCommit] exactly once, however many moves there
 * were: one gesture is one undo step. A touch that never moves calls neither. A finger down on
 * empty photo selects nothing ([onSelect] with null, only if something was selected).
 *
 * A finger down on a corner button of the selected sticker is left alone for the button to
 * handle. All the lambdas are read afresh at every gesture, so the handler never goes stale and
 * is composed only while the overlay is enabled.
 *
 * @param stickers the stickers now, oldest first (the last one is on top).
 * @param selectedKey the selected sticker's key, or null.
 * @param mapping the translation between the shown picture and the original photo.
 * @param size the overlay's size in pixels (the shown picture's size).
 * @param aspectOf a sticker's picture height divided by its width.
 * @param buttonRadiusPx how far from a corner of the selected sticker a touch belongs to its button.
 * @param onSelect called with the key now selected, or null for none.
 * @param onPreview called on each move with the complete new list of stickers.
 * @param onCommit called once when the gesture ends after at least one preview.
 * @param isDrawn whether a sticker is drawn (its picture is in the catalogue); only those can be touched.
 */
fun Modifier.stickerGestures(
    stickers: () -> List<Sticker>,
    selectedKey: () -> Long?,
    mapping: () -> StickerMapping,
    size: () -> Size,
    aspectOf: (Sticker) -> Float,
    buttonRadiusPx: Float,
    onSelect: (Long?) -> Unit,
    onPreview: (List<Sticker>) -> Unit,
    onCommit: () -> Unit,
    isDrawn: (Sticker) -> Boolean = { true },
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val list = stickers()
        val map = mapping()
        val area = size()
        if (area.width <= 0f || area.height <= 0f) return@awaitEachGesture

        // A touch on the selected sticker's corner button belongs to that button.
        val selected = list.firstOrNull { it.key == selectedKey() }
        if (selected != null && isDrawn(selected) && touchesButton(selected, map, area, aspectOf(selected), down.position, buttonRadiusPx)) {
            return@awaitEachGesture
        }

        // The newest sticker is last in the list, so search from the end.
        val hit = list.lastOrNull { s ->
            // A sticker whose picture is not in the catalogue is not drawn, so it is not touchable.
            if (!isDrawn(s)) return@lastOrNull false
            val centre = s.shownCentre(map, area)
            val w = s.shownWidth(map, area)
            pointOnSticker(
                down.position.x, down.position.y, centre.x, centre.y,
                w, w * aspectOf(s), s.shownDegrees(map),
            )
        }
        if (hit == null) {
            // Empty photo: nothing is selected. awaitEachGesture waits for the finger to lift.
            if (selectedKey() != null) onSelect(null)
            return@awaitEachGesture
        }
        down.consume()
        if (selectedKey() != hit.key) onSelect(hit.key)

        // The sticker's live look on the shown picture, updated by every move of this gesture.
        var centre = hit.shownCentre(map, area)
        var widthPx = hit.shownWidth(map, area)
        var degrees = hit.shownDegrees(map)
        var previewed = false

        var vanished = false
        do {
            val event = awaitPointerEvent()
            // A pointer the resize handle (or anything else) already consumed is not ours.
            val pressed = event.changes.filter { it.pressed && !it.isConsumed }.sortedBy { it.id.value }
            if (pressed.isNotEmpty() && pressed.any { it.position != it.previousPosition }) {
                var scale = 1f
                var turn = 0f
                val oldPivot: Offset
                val newPivot: Offset
                if (pressed.size == 1) {
                    oldPivot = pressed[0].previousPosition
                    newPivot = pressed[0].position
                } else {
                    val a = pressed[0]
                    val b = pressed[1]
                    oldPivot = (a.previousPosition + b.previousPosition) / 2f
                    newPivot = (a.position + b.position) / 2f
                    val oldDistance = distanceBetween(
                        a.previousPosition.x, a.previousPosition.y, b.previousPosition.x, b.previousPosition.y,
                    )
                    val newDistance = distanceBetween(a.position.x, a.position.y, b.position.x, b.position.y)
                    if (oldDistance > 1f) scale = newDistance / oldDistance
                    if (oldDistance > 1f && newDistance > 1f) {
                        turn = angleChange(
                            angleBetween(a.previousPosition.x, a.previousPosition.y, b.previousPosition.x, b.previousPosition.y),
                            angleBetween(a.position.x, a.position.y, b.position.x, b.position.y),
                        )
                    }
                }
                // Keep the width within its limits; the centre follows the scale really applied.
                val newWidth = (widthPx * scale).coerceIn(
                    MIN_STICKER_WIDTH_FRACTION * area.width,
                    MAX_STICKER_WIDTH_FRACTION * area.width,
                )
                val applied = newWidth / widthPx
                // The centre may not leave the shown picture, or the sticker could never be grasped again.
                centre = clampCentre(
                    Offset(
                        pinchCentreX(centre.x, centre.y, oldPivot.x, oldPivot.y, newPivot.x, newPivot.y, applied, turn),
                        pinchCentreY(centre.x, centre.y, oldPivot.x, oldPivot.y, newPivot.x, newPivot.y, applied, turn),
                    ),
                    area,
                )
                widthPx = newWidth
                degrees += turn
                // Build from the sticker AS IT IS NOW, so a flip made meanwhile (a second finger on
                // the flip button) is kept; only the place, size and turn come from this gesture.
                val current = stickers().firstOrNull { it.key == hit.key }
                if (current == null) {
                    // Deleted or undone away mid-gesture: nothing left to move, and no commit.
                    vanished = true
                    break
                }
                val moved = current.placedAt(map, area, centre, widthPx, degrees)
                onPreview(stickers().map { if (it.key == hit.key) moved else it })
                previewed = true
                event.changes.forEach(PointerInputChange::consume)
            }
        } while (event.changes.any { it.pressed })

        // The last finger is up: one gesture ends, so one commit.
        if (previewed && !vanished) onCommit()
    }
}

/**
 * [centre] pulled back inside the overlay rectangle of [area] (0..width, 0..height), so a sticker
 * dragged past the picture's edge still has its centre on the picture and can be grasped again.
 */
internal fun clampCentre(centre: Offset, area: Size): Offset =
    Offset(centre.x.coerceIn(0f, area.width), centre.y.coerceIn(0f, area.height))

/**
 * Whether [position] is within [radiusPx] of one of the four corners of [sticker]'s frame, where
 * its corner buttons sit. Used so the overlay's handler leaves those touches to the buttons.
 */
private fun touchesButton(
    sticker: Sticker,
    mapping: StickerMapping,
    area: Size,
    aspect: Float,
    position: Offset,
    radiusPx: Float,
): Boolean {
    val centre = sticker.shownCentre(mapping, area)
    val w = sticker.shownWidth(mapping, area)
    val degrees = sticker.shownDegrees(mapping)
    return listOf(SIDE_START, SIDE_END).any { sx ->
        listOf(SIDE_START, SIDE_END).any { sy ->
            val x = cornerX(centre.x, centre.y, w, w * aspect, degrees, sx, sy)
            val y = cornerY(centre.x, centre.y, w, w * aspect, degrees, sx, sy)
            distanceBetween(x, y, position.x, position.y) <= radiusPx
        }
    }
}
