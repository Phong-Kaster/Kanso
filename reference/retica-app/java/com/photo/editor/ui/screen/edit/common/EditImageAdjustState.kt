package com.photo.editor.ui.screen.edit.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.photo.editor.editimage.model.AdjustData
import com.photo.editor.ui.screen.edit.enums.EditImageAdjustOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Stable
class EditImageAdjustState {
    private val _adjustDataFlow = MutableStateFlow(AdjustData())
    val adjustDataFlow = _adjustDataFlow.asStateFlow()

    fun setAdjustData(adjustData: AdjustData) {
        _adjustDataFlow.value = adjustData
    }

    // Convenience functions for Compose
    @Composable
    fun getAdjustValueByOption(option: EditImageAdjustOption): Float {
        val adjustData by adjustDataFlow.collectAsState()
        return when (option) {
            EditImageAdjustOption.Lux -> adjustData.lux
            EditImageAdjustOption.Exposure -> adjustData.exposure
            EditImageAdjustOption.Brightness -> adjustData.brightness
            EditImageAdjustOption.Contrast -> adjustData.contrast
            EditImageAdjustOption.Saturation -> adjustData.saturation
            EditImageAdjustOption.Vibrance -> adjustData.vibrance
            EditImageAdjustOption.Clarity -> adjustData.clarity
            EditImageAdjustOption.Highlight -> adjustData.highlight
            EditImageAdjustOption.Shadows -> adjustData.shadows
            EditImageAdjustOption.Warmth -> adjustData.warmth
            EditImageAdjustOption.Tint -> adjustData.tint
            EditImageAdjustOption.Fade -> adjustData.fade
            EditImageAdjustOption.Grain -> adjustData.grain
            EditImageAdjustOption.Vignette -> adjustData.vignette
            EditImageAdjustOption.Mosaic -> adjustData.mosaic
//            EditImageAdjustOption.Dust -> adjustData.dustValue
        }
    }

    fun setAdjustValueByOption(option: EditImageAdjustOption, value: Float) {
        when (option) {
            EditImageAdjustOption.Lux -> _adjustDataFlow.update { it.copy(lux = value) }
            EditImageAdjustOption.Exposure -> _adjustDataFlow.update { it.copy(exposure = value) }
            EditImageAdjustOption.Brightness -> _adjustDataFlow.update { it.copy(brightness = value) }
            EditImageAdjustOption.Contrast -> _adjustDataFlow.update { it.copy(contrast = value) }
            EditImageAdjustOption.Saturation -> _adjustDataFlow.update { it.copy(saturation = value) }
            EditImageAdjustOption.Vibrance -> _adjustDataFlow.update { it.copy(vibrance = value) }
            EditImageAdjustOption.Clarity -> _adjustDataFlow.update { it.copy(clarity = value) }
            EditImageAdjustOption.Highlight -> _adjustDataFlow.update { it.copy(highlight = value) }
            EditImageAdjustOption.Shadows -> _adjustDataFlow.update { it.copy(shadows = value) }
            EditImageAdjustOption.Warmth -> _adjustDataFlow.update { it.copy(warmth = value) }
            EditImageAdjustOption.Tint -> _adjustDataFlow.update { it.copy(tint = value) }
            EditImageAdjustOption.Fade -> _adjustDataFlow.update { it.copy(fade = value) }
            EditImageAdjustOption.Grain -> _adjustDataFlow.update { it.copy(grain = value) }
            EditImageAdjustOption.Vignette -> _adjustDataFlow.update { it.copy(vignette = value) }
            EditImageAdjustOption.Mosaic -> _adjustDataFlow.update { it.copy(mosaic = value) }
//            EditImageAdjustOption.Dust -> _adjustDataFlow.update { it.copy(dustValue = value) }
        }
    }
}