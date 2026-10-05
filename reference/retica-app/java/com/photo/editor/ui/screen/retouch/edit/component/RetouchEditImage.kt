package com.photo.editor.ui.screen.retouch.edit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize
import com.photo.editor.common.ui.transformableimage.ImageState
import com.photo.editor.ui.screen.retouch.common.RetouchEditInteraction
import com.photo.editor.ui.screen.retouch.common.RetouchUiState
import com.photo.editor.ui.screen.retouch.edit.component.drawer.RetouchDrawingState
import com.photo.editor.ui.screen.retouch.edit.component.drawer.RetouchDrawingSurface
import com.photo.editor.ui.screen.retouch.edit.component.toucher.RetouchTouchState
import com.photo.editor.ui.screen.retouch.edit.component.toucher.RetouchTouchSurface
import com.photo.editor.ui.screen.retouch.model.RetouchMode

@Composable
fun RetouchEditImage(
    state: ImageState,
    modifier: Modifier = Modifier,
    uiState: RetouchUiState = RetouchUiState(),
    interaction: RetouchEditInteraction = RetouchEditInteraction(),
    drawingState: RetouchDrawingState = RetouchDrawingState(),
    touchState: RetouchTouchState = RetouchTouchState(),
) = BoxWithConstraints(
    modifier = modifier
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                state.transformState.scaleAndTranslate(zoom, pan.x, pan.y)
            }
        }
        .graphicsLayer {
            scaleX = state.transformState.scale
            scaleY = state.transformState.scale

            translationX = state.transformState.translateX
            translationY = state.transformState.translateY
        }
) {
    val density = LocalDensity.current
    val imageSize = remember(state.image, maxWidth, maxHeight) {
        with(density) {
            val originalSize = Size(state.image.width.toFloat(), state.image.height.toFloat())
            val scaledSize = Size(maxWidth.toPx(), maxHeight.toPx())
            val scaleFactor = ContentScale.Fit.computeScaleFactor(originalSize, scaledSize)
            DpSize(
                (originalSize.width * scaleFactor.scaleX).toDp(),
                (originalSize.height * scaleFactor.scaleY).toDp()
            )
        }
    }

    Image(
        bitmap = state.image,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                state.contentScale = ContentScale.Fit.computeScaleFactor(
                    srcSize = Size(state.image.width.toFloat(), state.image.height.toFloat()),
                    dstSize = it.toSize(),
                ).scaleX
            },
        contentScale = ContentScale.Fit,
    )

    when (uiState.retouchMode) {
        RetouchMode.Draw -> RetouchDrawingSurface(
            drawingState = drawingState,
            modifier = Modifier
                .align(Alignment.Center)
                .size(imageSize),
            onDrawFinished = interaction.onRetouchDrawFinished,
        )

        RetouchMode.Touch -> RetouchTouchSurface(
            touchState = touchState,
            modifier = Modifier
                .align(Alignment.Center)
                .size(imageSize),
            onPointAdded = interaction.onRetouchPointAdded,
        )
    }

}