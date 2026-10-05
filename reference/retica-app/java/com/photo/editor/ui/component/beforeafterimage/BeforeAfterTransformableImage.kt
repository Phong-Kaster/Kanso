package com.photo.editor.ui.component.beforeafterimage

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.theme.VerticalClipRect
import com.photo.editor.ui.component.shimmer.SimpleShimmer

@Composable
fun BeforeAfterTransformableImage(
    leftImage: Any?,
    rightImage: Any?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val minimumScale = 1f

    var sliderPercent by remember { mutableFloatStateOf(0.5f) }
    var imageSize by remember { mutableStateOf(Size.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                // Zoom
                scale = (scale * zoom).coerceIn(minimumScale, minimumScale * 5)

                // Pan
                val tmp = scale / minimumScale
                val maxX = ((size.width * (tmp - 1f) + imageSize.width * scale - size.width * tmp) / 2f).coerceAtLeast(0f)
                val maxY = ((size.height * (tmp - 1f) + imageSize.height * scale - size.height * tmp) / 2f).coerceAtLeast(0f)

                offsetX = (offsetX + pan.x * zoom).coerceIn(-maxX, maxX)
                offsetY = (offsetY + pan.y * zoom).coerceIn(-maxY, maxY)
            }
        }
    ) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(VerticalClipRect(0f, sliderPercent))
        ) {
            SubcomposeAsyncImage(
                model = leftImage,
                contentDescription = null,
                loading = { SimpleShimmer(modifier = Modifier.matchParentSize()) },
                error = { SimpleShimmer(modifier = Modifier.matchParentSize()) },
                onSuccess = {
                    val contentScale = contentScale.computeScaleFactor(it.painter.intrinsicSize, Size(maxWidthPx, maxHeightPx))
                    imageSize = it.painter.intrinsicSize.times(contentScale)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale

                        translationX = offsetX
                        translationY = offsetY
                    },
                contentScale = contentScale,
                alignment = Alignment.Center,
            )
        }


        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(VerticalClipRect(sliderPercent, 1f))
        ) {
            SubcomposeAsyncImage(
                model = rightImage,
                contentDescription = null,
                loading = { SimpleShimmer(modifier = Modifier.matchParentSize()) },
                error = { SimpleShimmer(modifier = Modifier.matchParentSize()) },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale

                        translationX = offsetX
                        translationY = offsetY
                    },
                contentScale = contentScale,
                alignment = Alignment.Center,
            )
        }

        DiffSlider(
            modifier = Modifier
                .graphicsLayer {
                    translationX = size.width * (sliderPercent * 2 - 1f) / 2
                }
                .align(BiasAbsoluteAlignment(sliderPercent * 2 - 1f, 0f))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        sliderPercent = (sliderPercent + dragAmount / maxWidth.toPx()).coerceIn(0f, 1f)
                    }
                },
            verticalBias = 0f,
        )
    }
}

@Composable
private fun DiffSlider(
    modifier: Modifier = Modifier,
    verticalBias: Float = 0.4f,
) {
    Box(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(Color.White)
                .align(Alignment.Center)
        )
        Icon(
            painter = painterResource(R.drawable.ic_difference_slider),
            contentDescription = stringResource(R.string.slider),
            modifier = Modifier
                .size(35.dp)
                .align(BiasAlignment(0f, verticalBias)),
            tint = Color.White,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    BeforeAfterTransformableImage(
        leftImage = R.drawable.onboard_1_image,
        rightImage = R.drawable.remove_bg_image_10,
        modifier = Modifier.fillMaxSize(),
    )
}