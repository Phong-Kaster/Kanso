package com.photo.editor.common.ui.transformableimage

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import com.photo.editor.common.ui.transformableimage.ImageState

@Composable
fun TransformableImage(
    state: ImageState,
    modifier: Modifier = Modifier,
    enableTransformation: Boolean = true,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val transformModifier = Modifier
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

    Image(
        bitmap = state.image,
        contentDescription = null,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                state.contentScale = contentScale.computeScaleFactor(
                    srcSize = Size(state.image.width.toFloat(), state.image.height.toFloat()),
                    dstSize = it.toSize(),
                ).scaleX
            }
            .then(if (enableTransformation) transformModifier else Modifier),
        contentScale = contentScale,
    )
}