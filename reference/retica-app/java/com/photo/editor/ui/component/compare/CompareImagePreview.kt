package com.photo.editor.ui.component.compare

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.ui.component.shimmer.SimpleShimmer
import com.photo.editor.common.ui.theme.VerticalClipRect
import kotlinx.coroutines.isActive

@Composable
fun CompareImagePreview(
    leftImage: Any?,
    rightImage: Any?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    sliderIconPosition: Float = 0.5f,
    enableAnimation: Boolean = true,
) {
    var sliderPercent by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(Unit) {
        while(isActive && enableAnimation) {
            animate(sliderPercent, 0.75f, animationSpec = tween(2000)) { value, _ ->
                sliderPercent = value
            }
            animate(sliderPercent, 0.25f, animationSpec = tween(2000)) { value, _ ->
                sliderPercent = value
            }
        }
    }

    Box(
        modifier = modifier
    ) {
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
                modifier = Modifier
                    .fillMaxSize(),
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
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                alignment = Alignment.Center,
            )
        }

        DiffSlider(
            iconPosition = sliderIconPosition,
            modifier = Modifier
                .graphicsLayer {
                    translationX = size.width * (sliderPercent * 2 - 1f) / 2
                }
                .align(BiasAbsoluteAlignment(sliderPercent * 2 - 1f, 0f))
        )
    }
}

@Composable
private fun DiffSlider(
    iconPosition: Float = 0.5f,
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
                .align(BiasAlignment(0f, iconPosition * 2 - 1)),
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