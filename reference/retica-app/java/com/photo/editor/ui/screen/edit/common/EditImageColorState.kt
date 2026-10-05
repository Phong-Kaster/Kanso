package com.photo.editor.ui.screen.edit.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.photo.editor.editimage.model.color.ColorData
import com.photo.editor.editimage.model.color.ColorHSLData
import com.photo.editor.editimage.model.color.ColorTintData
import com.photo.editor.ui.screen.edit.enums.ColorCurveType
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption.Blue
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption.Green
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption.LightBlue
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption.LightGreen
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption.NavyBlue
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption.Orange
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption.Pink
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption.Purple
import com.photo.editor.ui.screen.edit.enums.ColorHSLOption.Red
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Stable
class EditImageColorState {
    private val _colorDataFlow = MutableStateFlow(ColorData())
    val colorDataFlow = _colorDataFlow.asStateFlow()

    fun setColorData(colorData: ColorData) = _colorDataFlow.update { colorData }

    fun updateColorCurve(type: ColorCurveType, points: List<Pair<Float, Float>>) {
        when (type) {
            ColorCurveType.BlackWhite -> _colorDataFlow.update { it.copy(blackWhiteCurvePoints = points.toList()) }
            ColorCurveType.Red -> _colorDataFlow.update { it.copy(redCurvePoints = points.toList()) }
            ColorCurveType.Green -> _colorDataFlow.update { it.copy(greenCurvePoints = points.toList()) }
            ColorCurveType.Blue -> _colorDataFlow.update { it.copy(blueCurvePoints = points.toList()) }
        }
    }

    fun updateHslHue(option: ColorHSLOption, value: Float) {
        _colorDataFlow.update {
            val colorHSL = it.colorHSL
            it.copy(
                colorHSL = when (option) {
                    Red -> colorHSL.copy(red = colorHSL.red.copy(hue = value))
                    Orange -> colorHSL.copy(orange = colorHSL.orange.copy(hue = value))
                    LightGreen -> colorHSL.copy(lightGreen = colorHSL.lightGreen.copy(hue = value))
                    Green -> colorHSL.copy(green = colorHSL.green.copy(hue = value))
                    LightBlue -> colorHSL.copy(lightBlue = colorHSL.lightBlue.copy(hue = value))
                    Blue -> colorHSL.copy(blue = colorHSL.blue.copy(hue = value))
                    NavyBlue -> colorHSL.copy(navyBlue = colorHSL.navyBlue.copy(hue = value))
                    Purple -> colorHSL.copy(purple = colorHSL.purple.copy(hue = value))
                    Pink -> colorHSL.copy(pink = colorHSL.pink.copy(hue = value))
                }
            )
        }
    }

    fun updateHslSaturation(option: ColorHSLOption, value: Float) {
        _colorDataFlow.update {
            val colorHSL = it.colorHSL
            it.copy(
                colorHSL = when (option) {
                    Red -> colorHSL.copy(red = colorHSL.red.copy(saturation = value))
                    Orange -> colorHSL.copy(orange = colorHSL.orange.copy(saturation = value))
                    LightGreen -> colorHSL.copy(lightGreen = colorHSL.lightGreen.copy(saturation = value))
                    Green -> colorHSL.copy(green = colorHSL.green.copy(saturation = value))
                    LightBlue -> colorHSL.copy(lightBlue = colorHSL.lightBlue.copy(saturation = value))
                    Blue -> colorHSL.copy(blue = colorHSL.blue.copy(saturation = value))
                    NavyBlue -> colorHSL.copy(navyBlue = colorHSL.navyBlue.copy(saturation = value))
                    Purple -> colorHSL.copy(purple = colorHSL.purple.copy(saturation = value))
                    Pink -> colorHSL.copy(pink = colorHSL.pink.copy(saturation = value))
                }
            )
        }

    }

    fun updateHslLightness(option: ColorHSLOption, value: Float) {
        _colorDataFlow.update {
            val colorHSL = it.colorHSL
            it.copy(
                colorHSL = when (option) {
                    Red -> colorHSL.copy(red = colorHSL.red.copy(lightness = value))
                    Orange -> colorHSL.copy(orange = colorHSL.orange.copy(lightness = value))
                    LightGreen -> colorHSL.copy(lightGreen = colorHSL.lightGreen.copy(lightness = value))
                    Green -> colorHSL.copy(green = colorHSL.green.copy(lightness = value))
                    LightBlue -> colorHSL.copy(lightBlue = colorHSL.lightBlue.copy(lightness = value))
                    Blue -> colorHSL.copy(blue = colorHSL.blue.copy(lightness = value))
                    NavyBlue -> colorHSL.copy(navyBlue = colorHSL.navyBlue.copy(lightness = value))
                    Purple -> colorHSL.copy(purple = colorHSL.purple.copy(lightness = value))
                    Pink -> colorHSL.copy(pink = colorHSL.pink.copy(lightness = value))
                }
            )
        }

    }

    fun getHueByOption(colorHSL: ColorHSLData, option: ColorHSLOption): Float {
        return when (option) {
            Red -> colorHSL.red.hue
            Orange -> colorHSL.orange.hue
            Green -> colorHSL.green.hue
            Blue -> colorHSL.blue.hue
            NavyBlue -> colorHSL.navyBlue.hue
            Purple -> colorHSL.purple.hue
            Pink -> colorHSL.pink.hue
            LightGreen -> colorHSL.lightGreen.hue
            LightBlue -> colorHSL.lightBlue.hue
        }
    }

    fun getSaturationByOption(colorHSL: ColorHSLData, option: ColorHSLOption): Float {
        return when (option) {
            Red -> colorHSL.red.saturation
            Orange -> colorHSL.orange.saturation
            Green -> colorHSL.green.saturation
            Blue -> colorHSL.blue.saturation
            NavyBlue -> colorHSL.navyBlue.saturation
            Purple -> colorHSL.purple.saturation
            Pink -> colorHSL.pink.saturation
            LightGreen -> colorHSL.lightGreen.saturation
            LightBlue -> colorHSL.lightBlue.saturation
        }
    }

    fun getLightnessByOption(colorHSL: ColorHSLData, option: ColorHSLOption): Float {
        return when (option) {
            Red -> colorHSL.red.lightness
            Orange -> colorHSL.orange.lightness
            Green -> colorHSL.green.lightness
            Blue -> colorHSL.blue.lightness
            NavyBlue -> colorHSL.navyBlue.lightness
            Purple -> colorHSL.purple.lightness
            Pink -> colorHSL.pink.lightness
            LightGreen -> colorHSL.lightGreen.lightness
            LightBlue -> colorHSL.lightBlue.lightness
        }
    }

    fun updateColorTint(colorTint: ColorTintData) = _colorDataFlow.update { it.copy(colorTint = colorTint) }
}