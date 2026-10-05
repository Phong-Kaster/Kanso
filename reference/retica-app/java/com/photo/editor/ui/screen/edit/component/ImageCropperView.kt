package com.photo.editor.ui.screen.edit.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.mr0xf00.easycrop.CropState
import com.mr0xf00.easycrop.CropperStyle
import com.mr0xf00.easycrop.CropperStyleGuidelines
import com.mr0xf00.easycrop.LocalCropperStyle
import com.mr0xf00.easycrop.images.ImageBitmapSrc
import com.mr0xf00.easycrop.ui.CropperPreview
import com.mr0xf00.easycrop.utils.setAspect
import com.photo.editor.common.ui.theme.MintColor
import com.photo.editor.domain.model.CropRatio
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ImageCropperView(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) = BoxWithConstraints(modifier = modifier) {
    val density = LocalDensity.current
    val maxWidth = maxWidth
    val maxHeight = maxHeight
    val cropData by uiState.cropState.cropDataFlow.collectAsState()

    val cropState = remember {
        val image = uiState.originalBitmap
        val componentWidth = with(density) { maxWidth.roundToPx() }
        val componentHeight = with(density) { maxHeight.roundToPx() }
        val widthRatio = componentWidth / image.height.toFloat()
        val heightRatio = componentHeight / image.height.toFloat()
        val scaleRatio = min(widthRatio, heightRatio)
        val scaledDownImage = if (scaleRatio < 1)
            image.scale((image.width * scaleRatio).roundToInt(), (image.height * scaleRatio).roundToInt())
        else image
        CropState(ImageBitmapSrc(scaledDownImage.asImageBitmap()))
    }

    // Observe and record current crop region
    LaunchedEffect(cropState.region, cropState.imgRect) {
        uiState.cropState.tempCropFullRegion = cropState.imgRect
        uiState.cropState.tempCropRegion = cropState.region
    }

    LaunchedEffect(uiState.cropState.cropRatio) {
        when (uiState.cropState.cropRatio) {
            CropRatio.Free -> {
                cropState.aspectLock = false
                cropState.region = cropState.imgRect
            }

            else -> {
                cropState.aspectLock = false
                cropState.region = cropState.imgRect.setAspect(uiState.cropState.cropRatio.ratio)
                cropState.aspectLock = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        CompositionLocalProvider(
            LocalCropperStyle provides CropperStyle(
                backgroundColor = Color(0xFFF0F0F0),
                overlay = Color.Transparent,
                handleColor = MintColor,
                touchRad = 20.dp,
                rectStrokeWidth = 1.dp,
                guidelines = CropperStyleGuidelines(color = Color.White.copy(0.3f))
            )
        ) {
            var showCropper by remember { mutableStateOf(false) }

            LaunchedEffect(showCropper) {
                if (!showCropper) {
                    showCropper = true
                }
            }

            AnimatedVisibility(
                showCropper,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                CropperPreview(
                    state = cropState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ImageCropperView(
        uiState = EditImageUiState().apply {
            originalBitmap = createBitmap(200, 200)
        }
    )
}