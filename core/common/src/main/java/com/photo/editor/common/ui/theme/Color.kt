package com.photo.editor.common.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val SignatureGradient = Brush.horizontalGradient(
    0f to Color(0xFFFFE8A1),
    1f to Color(0xFFFFD147),
)

val PrimaryYellowLightHover = Color(0xFFFFF8E3)
val PrimaryYellowLight = Color(0xFFFFFAED)
val PrimaryYellowLightActive = Color(0xFFFFF1C6)
val PrimaryYellowLightPrimary = Color(0xFFFFD147)
val DarkNeutralTitle = Color(0xFF090B0E)

val BorderStroke = Color(0xFFE9E9E9)

val PrimaryColor = PrimaryYellowLightPrimary
val AlternativeColor = Color(0xFFEDE6D2)
val SubColor = Color(0xFFF8F0E1)
val BorderColor = Color(0xFFE9E9E9)

val SemiTransparent = Color(0x80252525)
val MintColor = Color(0xFF00C7BE)
val Neutral300 = Color(0xFFE1E1E1)
val Neutral800 = Color(0xFF434343)

val DarkGreen = Color(0xFF115A66)


val PrimaryRed = Color(0xFFFF0000)
val PrimaryRedLight = Color(0x00FF0000)


val Dark70 = Color(0x70000000)

val SignatureGradient2 = Brush.verticalGradient(
    0f to Color(0xFFF8F0E1).copy(0.8f),
    1f to Color(0xFFF8F0E1),
)

val SignatureGradient3 = Brush.horizontalGradient(
    0f to Color(0xFFF8F0E1).copy(0.5f),
    1f to Color(0xFFF8F0E1).copy(0f),
)

val GradientOb2 = Brush.verticalGradient(
    0f to Color(0xFFEDE6D2).copy(0f),
    1f to Color(0xFFEDE6D2).copy(0.5f),
)

val HomePagerOverlayGradient = Brush.verticalGradient(
    0f to Color(0x00F8F0E1),
    .7f to Color(0xFFF8F0E1),
)