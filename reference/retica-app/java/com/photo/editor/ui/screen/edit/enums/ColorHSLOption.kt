package com.photo.editor.ui.screen.edit.enums

import androidx.compose.ui.graphics.Color

enum class ColorHSLOption(val hueValue: Float) {
    Red(0f),
    Orange(40f),
    LightGreen(80f),
    Green(120f),
    LightBlue(160f),
    Blue(200f),
    NavyBlue(240f),
    Purple(280f),
    Pink(320f);

    fun getColor(): Color {
        return Color.hsl(hueValue, 0.5f, 0.5f)
    }
}