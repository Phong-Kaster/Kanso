package com.photo.editor.ui.screen.removebg.edit.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.common.ui.modifier.backgroundColor
import com.photo.editor.common.ui.transformableimage.ImageState
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.ui.component.shimmer.SimpleShimmer
import com.photo.editor.ui.screen.removebg.common.RemoveBGBackground
import com.photo.editor.ui.screen.removebg.common.RemoveBgUiState
import com.photo.editor.ui.screen.removebg.edit.component.refine.RefineDrawingState
import kotlin.math.ceil

@Composable
fun RemoveBgEditImage(
    state: ImageState,
    modifier: Modifier = Modifier,
    uiState: RemoveBgUiState = RemoveBgUiState(),
    drawingState: RefineDrawingState? = null,
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
            DpSize((originalSize.width * scaleFactor.scaleX).toDp(), (originalSize.height * scaleFactor.scaleY).toDp())
        }
    }

    Canvas(
        modifier = Modifier
            .align(Alignment.Center)
            .size(imageSize)
    ) { clipRect { drawCheckeredBackground() } }

    if (drawingState == null) {

        when (val currentBackground = uiState.background) {
            is RemoveBGBackground.SolidColor -> {
                Spacer(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(imageSize)
                        .backgroundColor(currentBackground.color)
                )
            }

            is RemoveBGBackground.Image -> {
                SubcomposeAsyncImage(
                    model = currentBackground.uri,
                    contentDescription = null,
                    loading = { SimpleShimmer() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(imageSize),
                    contentScale = ContentScale.Crop,
                )
            }

            is RemoveBGBackground.Scene -> {
                SubcomposeAsyncImage(
                    model = currentBackground.scene.imageId,
                    contentDescription = null,
                    loading = { SimpleShimmer() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(imageSize),
                    contentScale = ContentScale.Crop,
                )
            }

            is RemoveBGBackground.Background -> {
                SubcomposeAsyncImage(
                    model = currentBackground.background.imageId,
                    contentDescription = null,
                    loading = { SimpleShimmer() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(imageSize),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }

    drawingState?.let {
        Image(
            bitmap = uiState.originalImage.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.7f
                },
            colorFilter = ColorFilter.tint(Color.Red.copy(0.5f), BlendMode.SrcOver)
        )
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
}


private fun DrawScope.drawCheckeredBackground() {
    val darkColor = Color.LightGray
    val lightColor = Color.White

    val gridSizePx = 10.dp.toPx()
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