package com.photo.editor.ui.screen.edit.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.photo.editor.common.geometry.UnifyRect
import com.photo.editor.editimage.model.CropData
import com.photo.editor.domain.model.CropRatio
import com.photo.editor.ui.screen.edit.enums.EditImageRotateOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Stable
class EditImageCropState {
    private val _cropDataFlow = MutableStateFlow(CropData())
    val cropDataFlow = _cropDataFlow.asStateFlow()

    var cropRatio: CropRatio by mutableStateOf(CropRatio.Free)

    var tempCropFullRegion: Rect by mutableStateOf(Rect.Zero)
    var tempCropRegion: Rect by mutableStateOf(Rect.Zero)

    fun setCropData(cropData: CropData) = _cropDataFlow.update { cropData }

    fun rotateWithOption(option: EditImageRotateOption) {
        when (option) {
            EditImageRotateOption.Left -> _cropDataFlow.update {
                var targetRotation = it.rotateDegrees - 90
                if (targetRotation < 0) targetRotation += 360
                it.copy(rotateDegrees = targetRotation)
            }

            EditImageRotateOption.Right -> _cropDataFlow.update {
                val targetRotation = (it.rotateDegrees + 90) % 360
                it.copy(rotateDegrees = targetRotation)
            }

            EditImageRotateOption.FlipHorizontally -> _cropDataFlow.update { it.copy(flipHorizontally = !it.flipHorizontally) }
            EditImageRotateOption.FlipVertically -> _cropDataFlow.update { it.copy(flipVertically = !it.flipVertically) }
        }
    }

    fun confirmCropRegion() {
        val cropFulLRegion = tempCropFullRegion
        val cropRegion = tempCropRegion
        if (cropFulLRegion == Rect.Zero || cropRegion == Rect.Zero) {
            return _cropDataFlow.update { it.copy(cropRegion = null) }
        }
        val normalizedCropRegion = UnifyRect(
            left = cropRegion.left / cropFulLRegion.width,
            top = cropRegion.top / cropFulLRegion.height,
            right = cropRegion.right / cropFulLRegion.width,
            bottom = cropRegion.bottom / cropFulLRegion.height
        )
        _cropDataFlow.update { it.copy(cropRegion = normalizedCropRegion) }
    }
}