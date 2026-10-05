package com.photo.editor.ui.screen.camera

import androidx.camera.core.CameraSelector
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.photo.editor.domain.model.AspectRatio
import com.photo.editor.domain.model.FlashMode
import com.photo.editor.editimage.model.filter.FilterPreset

@Stable
class CameraUiState {
    // Camera control

    var cameraLensFacing by mutableIntStateOf(CameraSelector.LENS_FACING_BACK)

    var zoomRatio by  mutableFloatStateOf(1f)
    var minZoomRatio by  mutableFloatStateOf(1f)
    var maximumZoomRatio by  mutableFloatStateOf(1f)

    var aspectRatio by mutableStateOf(AspectRatio.RatioFull)
    var flashMode by mutableStateOf(FlashMode.Auto)

    //  Camera control ends

    var selectedFilter: FilterPreset? by mutableStateOf(null)

    var filterIntensity: Float by mutableFloatStateOf(0f)

    var imageCaptured by mutableStateOf(false)

}