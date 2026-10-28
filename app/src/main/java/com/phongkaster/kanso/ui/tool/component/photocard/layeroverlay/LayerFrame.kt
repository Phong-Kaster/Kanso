package com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/**
 * The frame around the selected layer: an outline that follows the layer's centre, size and
 * turn, and four corner buttons, in the same places on every kind of layer (ADR-005). Top-left
 * deletes, bottom-left duplicates and bottom-right is the resize handle: drag it away from or
 * towards the layer to make it bigger or smaller, and round it to turn the layer, the picture's
 * shape never changing. Top-right is the one button that depends on the kind: it flips (mirrors)
 * a sticker, edits a text's words (opens the keyboard) when [onEdit] is given, and is left out
 * for a date (ADR-006).
 *
 * All sizes and positions are in pixels of the overlay (the shown picture). The frame is drawn
 * by [drawBehind] with a rotated rectangle, and each button is placed at the matching rotated
 * corner from [cornerX] and [cornerY], so the buttons stay upright and readable while the frame
 * turns.
 *
 * **The handle's own drag.** The handle has its own raw pointer handler instead of the overlay's,
 * because it must keep working while the finger leaves the sticker. It measures the finger's
 * distance and direction from the sticker's centre, once when the finger lands and again at every
 * move, and asks [onResize] for the width and turn those imply (`handleWidth` and
 * `handleDegrees`). The handle moves as the sticker grows, so the finger is measured in the
 * ROOT coordinates (`localToRoot`), which stand still, and the overlay's own place in them comes
 * from [overlayOrigin]. One drag ends with exactly one [onResizeEnd].
 *
 * @param centreX the sticker's centre x in overlay pixels.
 * @param centreY the sticker's centre y in overlay pixels.
 * @param widthPx the sticker's width in pixels.
 * @param heightPx the sticker's height in pixels.
 * @param degrees the sticker's turn, clockwise.
 * @param overlayOrigin the overlay's top-left corner in root pixels, read when a drag starts.
 * @param minWidthPx the narrowest the handle may make the sticker.
 * @param maxWidthPx the widest the handle may make the sticker.
 * @param onDelete the delete button was tapped.
 * @param onDuplicate the duplicate button was tapped.
 * @param onFlip the flip button was tapped (a sticker's top-right button); null for a layer with
 * no flip, a date, whose top-right corner then stays empty (mirrored digits cannot be read).
 * @param onEdit the edit button was tapped; when it is not null the top-right button edits a text
 * instead of flipping.
 * @param onResize the handle is dragged: the sticker should now be this wide and turned this far.
 * @param onResizeEnd the handle's finger lifted after at least one resize.
 */
@Composable
fun LayerFrame(
    centreX: Float,
    centreY: Float,
    widthPx: Float,
    heightPx: Float,
    degrees: Float,
    overlayOrigin: () -> Offset,
    minWidthPx: Float,
    maxWidthPx: Float,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onFlip: (() -> Unit)?,
    onResize: (widthPx: Float, degrees: Float) -> Unit,
    onResizeEnd: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
) {
    val outline = MaterialTheme.colorScheme.primary
    val strokePx = with(LocalDensity.current) { 2.dp.toPx() }
    val halfButtonPx = with(LocalDensity.current) { (LAYER_BUTTON_TARGET / 2).roundToPx() }

    // The handle's own gesture reads the latest values through these, so it never goes stale.
    val latestOrigin by rememberUpdatedState(overlayOrigin)
    val latestCentre by rememberUpdatedState(Offset(centreX, centreY))
    val latestWidth by rememberUpdatedState(widthPx)
    val latestDegrees by rememberUpdatedState(degrees)
    val latestMin by rememberUpdatedState(minWidthPx)
    val latestMax by rememberUpdatedState(maxWidthPx)
    val latestOnResize by rememberUpdatedState(onResize)
    val latestOnResizeEnd by rememberUpdatedState(onResizeEnd)
    var handleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                rotate(degrees = degrees, pivot = Offset(centreX, centreY)) {
                    drawRect(
                        color = outline,
                        topLeft = Offset(centreX - widthPx / 2f, centreY - heightPx / 2f),
                        size = Size(widthPx, heightPx),
                        style = Stroke(width = strokePx),
                    )
                }
            },
    ) {
        /** The button's top-left in the overlay: the corner's pixel position less half a target. */
        fun placeAt(sideX: Float, sideY: Float): Modifier = Modifier.offset {
            IntOffset(
                cornerX(centreX, centreY, widthPx, heightPx, degrees, sideX, sideY).roundToInt() - halfButtonPx,
                cornerY(centreX, centreY, widthPx, heightPx, degrees, sideX, sideY).roundToInt() - halfButtonPx,
            )
        }

        LayerCornerButton(
            iconRes = R.drawable.ic_close_circle,
            description = stringResource(R.string.sticker_frame_delete),
            tag = LAYER_DELETE_TAG,
            modifier = placeAt(SIDE_START, SIDE_START),
            onClick = onDelete,
        )
        LayerCornerButton(
            iconRes = R.drawable.ic_duplicate,
            description = stringResource(R.string.sticker_frame_duplicate),
            tag = LAYER_DUPLICATE_TAG,
            modifier = placeAt(SIDE_START, SIDE_END),
            onClick = onDuplicate,
        )
        if (onEdit != null) {
            LayerCornerButton(
                iconRes = R.drawable.ic_edit,
                description = stringResource(R.string.text_frame_edit),
                tag = LAYER_EDIT_TAG,
                modifier = placeAt(SIDE_END, SIDE_START),
                onClick = onEdit,
            )
        } else if (onFlip != null) {
            LayerCornerButton(
                iconRes = R.drawable.ic_flip,
                description = stringResource(R.string.sticker_frame_flip),
                tag = LAYER_FLIP_TAG,
                modifier = placeAt(SIDE_END, SIDE_START),
                onClick = onFlip,
            )
        }
        LayerCornerButton(
            iconRes = R.drawable.ic_resize_circle,
            description = stringResource(R.string.sticker_frame_resize),
            tag = LAYER_RESIZE_TAG,
            modifier = placeAt(SIDE_END, SIDE_END),
            gestures = Modifier
                .onGloballyPositioned { handleCoordinates = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val coordinates = handleCoordinates ?: return@awaitEachGesture
                        // Everything below is measured in root pixels, which do not move when the
                        // handle does. The centre is fixed for the drag: resizing keeps it.
                        val centre = latestOrigin() + latestCentre
                        val start = coordinates.localToRoot(down.position)
                        val startDistance = distanceBetween(centre.x, centre.y, start.x, start.y)
                        val startAngle = angleBetween(centre.x, centre.y, start.x, start.y)
                        val startWidth = latestWidth
                        val startDegrees = latestDegrees
                        down.consume()
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            // Every change of this pointer is ours: the overlay must not also act on it.
                            if (change.position == change.previousPosition) change.consume()
                            if (change.position != change.previousPosition) {
                                val now = coordinates.localToRoot(change.position)
                                latestOnResize(
                                    handleWidth(
                                        startWidth, startDistance,
                                        distanceBetween(centre.x, centre.y, now.x, now.y),
                                        latestMin, latestMax,
                                    ),
                                    handleDegrees(startDegrees, startAngle, angleBetween(centre.x, centre.y, now.x, now.y)),
                                )
                                moved = true
                                change.consume()
                            }
                        }
                        // Finger up (or lost): one drag, one end.
                        if (moved) latestOnResizeEnd()
                    }
                },
        )
    }
}

/**
 * The frame round a sample sticker, drawn on a coloured square (clipped like the real overlay),
 * with the sticker turned by [degrees]. The outer Box fills the preview first, because the app
 * theme would otherwise stretch its single child to the whole screen.
 */
@Composable
private fun SampleStickerFrame(degrees: Float) {
    val density = LocalDensity.current
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            with(density) {
                Box(
                    Modifier
                        .size(240.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clipToBounds(),
                ) {
                    LayerFrame(
                        centreX = 120.dp.toPx(),
                        centreY = 120.dp.toPx(),
                        widthPx = 120.dp.toPx(),
                        heightPx = 90.dp.toPx(),
                        degrees = degrees,
                        overlayOrigin = { Offset.Zero },
                        minWidthPx = 12.dp.toPx(),
                        maxWidthPx = 240.dp.toPx(),
                        onDelete = {},
                        onDuplicate = {},
                        onFlip = {},
                        onResize = { _, _ -> },
                        onResizeEnd = {},
                    )
                }
            }
        }
    }
}

/** The frame upright, in light and in dark. */
@PreviewLightDark
@Composable
private fun StickerFramePreview() {
    SampleStickerFrame(degrees = 0f)
}

/** The frame turned 25 degrees, its buttons following the corners, in light and in dark. */
@PreviewLightDark
@Composable
private fun StickerFrameTurnedPreview() {
    SampleStickerFrame(degrees = 25f)
}
