package com.photo.editor.ui.screen.edit.enums

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

enum class ColorCurveType(
    val brush: Brush,
    val lineColor: Color,
) {
    BlackWhite(
        Brush.linearGradient(
            0.25f to Color(0xFF002EFF),
            0.5f to Color(0xFF75FF00),
            0.75f to Color(0xFFF71C29),
        ),
        Color.White,
    ),
    Red(
        SolidColor(Color(0xFFFF0000)),
        Color(0xFFFF0000),
    ),
    Green(
        SolidColor(Color(0xFF75FF00)),
        Color(0xFF75FF00)
    ),
    Blue(
        SolidColor(Color(0xFF002EFF)),
        Color(0xFF002EFF)
    ),
}