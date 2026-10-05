package com.photo.editor.ui.component.compare

import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.ui.component.shimmer.SimpleShimmer
import com.photo.editor.common.ui.theme.VerticalClipRect
import kotlin.math.min

@Composable
fun CompareImage(
    leftImage: Any?,
    rightImage: Any?,
    modifier: Modifier = Modifier,
    enableTransformation: Boolean = true,
    contentScale: ContentScale = ContentScale.Fit,
) {
    var sliderPercent by remember { mutableFloatStateOf(0.5f) }
    var imageSize by remember { mutableStateOf(Size.Zero) }
    var minimumScale by remember { mutableFloatStateOf(1f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .let {
                if (enableTransformation) it.pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Zoom
                        scale = (scale * zoom).coerceIn(minimumScale, minimumScale * 10)

                        // Pan
                        val tmp = scale / minimumScale
                        val maxX = ((size.width * (tmp - 1f) + imageSize.width * scale - size.width * tmp) / 2f).coerceAtLeast(0f)
                        val maxY = ((size.height * (tmp - 1f) + imageSize.height * scale - size.height * tmp) / 2f).coerceAtLeast(0f)

                        offsetX = (offsetX + pan.x * zoom).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y * zoom).coerceIn(-maxY, maxY)
                    }
                } else it
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
                    val widthScale = maxWidthPx / it.painter.intrinsicSize.width
                    val heightScale = maxHeightPx / it.painter.intrinsicSize.height
                    val size = it.painter.intrinsicSize.times(min(widthScale, heightScale))

                    imageSize = size
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
                onSuccess = {
                    // IntrinsicSize sometimes doesn't match component size, we have to recalculate size
                    val widthScale = maxWidthPx / it.painter.intrinsicSize.width
                    val heightScale = maxHeightPx / it.painter.intrinsicSize.height
                    val size = it.painter.intrinsicSize.times(min(widthScale, heightScale))

                    imageSize = size
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
        )
    }
}

@Composable
private fun DiffSlider(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(0.5.dp)
                .background(Color.White)
                .align(Alignment.Center)
        )
        Image(
            painter = painterResource(R.drawable.ic_compare_slider),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .align(BiasAlignment(0f, 0f)),
        )
    }
}

@Preview
@Composable
private fun Preview() {
    CompareImage(
        leftImage = null,
        rightImage = null,
    )
}