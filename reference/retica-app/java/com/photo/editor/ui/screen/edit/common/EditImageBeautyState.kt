package com.photo.editor.ui.screen.edit.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.photo.editor.editimage.model.BeautyData
import com.photo.editor.editimage.model.makeup.Blush
import com.photo.editor.editimage.model.makeup.EyeLens
import com.photo.editor.editimage.model.makeup.Eyebrows
import com.photo.editor.editimage.model.makeup.Eyelashes
import com.photo.editor.editimage.model.makeup.Eyeliner
import com.photo.editor.editimage.model.makeup.Eyeshadow
import com.photo.editor.editimage.model.makeup.BeautyMark
import com.photo.editor.editimage.model.makeup.Lipstick
import com.photo.editor.editimage.model.makeup.MakeupData
import com.photo.editor.ui.screen.edit.enums.EditImageBeautyOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Stable
class EditImageBeautyState {
    private val _beautyDataFlow = MutableStateFlow(BeautyData())
    val beautyDataFlow = _beautyDataFlow.asStateFlow()

    private val _makeupDataFlow = MutableStateFlow(MakeupData())
    val makeupDataFlow = _makeupDataFlow.asStateFlow()

    fun setBeautyData(beautyData: BeautyData) = _beautyDataFlow.update { beautyData }
    fun setMakeupData(makeupData: MakeupData) = _makeupDataFlow.update { makeupData }

    @Composable
    fun getBeautyValueByOption(beautyOption: EditImageBeautyOption?): Float {
        val beautyData by beautyDataFlow.collectAsState()
        return when (beautyOption) {
            EditImageBeautyOption.Makeup -> 0f
            EditImageBeautyOption.Slim -> beautyData.slim
            EditImageBeautyOption.Length -> beautyData.length
            EditImageBeautyOption.Brighten -> beautyData.brighten
            EditImageBeautyOption.Eyes -> beautyData.eyes
            EditImageBeautyOption.Smooth -> beautyData.smooth
            EditImageBeautyOption.Nose -> beautyData.nose
            EditImageBeautyOption.Teeth -> beautyData.teeth
            else -> 0f
        }
    }

    fun setBeautyValueByOption(beautyOption: EditImageBeautyOption, value: Float) {
        _beautyDataFlow.update {
            when (beautyOption) {
                EditImageBeautyOption.Makeup -> it
                EditImageBeautyOption.Slim -> it.copy(slim = value)
                EditImageBeautyOption.Length -> it.copy(length = value)
                EditImageBeautyOption.Brighten -> it.copy(brighten = value)
                EditImageBeautyOption.Eyes -> it.copy(eyes = value)
                EditImageBeautyOption.Smooth -> it.copy(smooth = value)
                EditImageBeautyOption.Nose -> it.copy(nose = value)
                EditImageBeautyOption.Teeth -> it.copy(teeth = value)
            }
        }
    }

    fun setLipstick(lipstick: Lipstick?, intensity: Float) {
        _makeupDataFlow.update { it.copy(lipstick = lipstick, lipstickIntensity = intensity) }
    }

    fun setEyelashes(eyelashes: Eyelashes?, intensity: Float) {
        _makeupDataFlow.update { it.copy(eyelashes = eyelashes, eyelashesIntensity = intensity) }
    }

    fun setEyebrows(eyebrows: Eyebrows?, intensity: Float) {
        _makeupDataFlow.update { it.copy(eyebrows = eyebrows, eyebrowsIntensity = intensity) }
    }

    fun setBlush(blush: Blush?, intensity: Float) {
        _makeupDataFlow.update { it.copy(blush = blush, blushIntensity = intensity) }
    }

    fun setEyeshadow(eyeshadow: Eyeshadow?, intensity: Float) {
        _makeupDataFlow.update { it.copy(eyeshadow = eyeshadow, eyeshadowIntensity = intensity) }
    }

    fun setEyeliner(eyeliner: Eyeliner?, intensity: Float) {
        _makeupDataFlow.update { it.copy(eyeliner = eyeliner, eyelinerIntensity = intensity) }
    }

    fun setEyeLens(eyeLens: EyeLens?, intensity: Float) {
        _makeupDataFlow.update { it.copy(eyeLens = eyeLens, eyeLensIntensity = intensity) }
    }

    fun setFreckle(beautyMark: BeautyMark?, intensity: Float) {
        _makeupDataFlow.update { it.copy(beautyMark = beautyMark, freckleIntensity = intensity) }
    }
}