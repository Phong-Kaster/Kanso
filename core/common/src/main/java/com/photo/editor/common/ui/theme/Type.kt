package com.photo.editor.common.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.photo.editor.common.R

val QuickSandFontFamily = FontFamily(
    Font(R.font.quicksand_bold, FontWeight.Bold),
    Font(R.font.quicksand_light, FontWeight.Light),
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_semibold, FontWeight.SemiBold),
)

val AboretoFontFamily = FontFamily(
    Font(R.font.aboreto_regular, FontWeight.Normal),
)
val OpenSansFontFamily = FontFamily(
    Font(R.font.opensans_extrabold, FontWeight.ExtraBold),
)
val PetitFormalScriptFontFamily = FontFamily(
    Font(R.font.petitformalscript_regular, FontWeight.Normal),
)
val PlayFairDisplayFontFamily = FontFamily(
    Font(R.font.playfairdisplay_regular, FontWeight.Normal),
)
val PridiFontFamily = FontFamily(
    Font(R.font.pridi_regular, FontWeight.Normal),
)
val ProstooneFontFamily = FontFamily(
    Font(R.font.prostoone_regular, FontWeight.Normal),
)
val QuanticoFontFamily = FontFamily(
    Font(R.font.quantico_regular, FontWeight.Normal),
)
val QuintessentialFontFamily = FontFamily(
    Font(R.font.quintessential_regular, FontWeight.Normal),
)
val RadioCanadaFontFamily = FontFamily(
    Font(R.font.radiocanada_regular, FontWeight.Normal),
)
val RedRoseFontFamily = FontFamily(
    Font(R.font.redrose_regular, FontWeight.Normal),
)
val ShadowIntoLightTwoFontFamily = FontFamily(
    Font(R.font.shadowsintolighttwo_regular, FontWeight.Normal),
)

val AbrilFataceFontFamily = FontFamily(
    Font(R.font.abril_fatface_regular, FontWeight.Normal),
)

val GoboldHollowFontFamily = FontFamily(
    Font(R.font.gobold_hollow_bold_italic, FontWeight.Normal),
)

val BebasbeueRegularFontFamily = FontFamily(
    Font(R.font.bebasbeue_regular, FontWeight.Normal),
)

fun appTextStyle(
    size: Int,
    weight: Int,
    color: Color = Color.Black,
    lineHeight: Int = (size * 1.5f).toInt(),
    brush: Brush? = null
): TextStyle = if (brush == null) TextStyle(
    fontFamily = QuickSandFontFamily,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight.sp,
    color = color,
) else TextStyle(
    fontFamily = QuickSandFontFamily,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight.sp,
    brush = brush,
)

val regular8 = appTextStyle(8, 400)
val regular10 = appTextStyle(10, 400)
val regular11 = appTextStyle(11, 400)
val regular12 = appTextStyle(12, 400)
val regular14 = appTextStyle(14, 400)
val regular16 = appTextStyle(16, 400)
val regular18 = appTextStyle(18, 400)

val medium8 = appTextStyle(8, 500)
val medium12 = appTextStyle(12, 500)
val medium14 = appTextStyle(14, 500)
val medium16 = appTextStyle(16, 500)
val medium18 = appTextStyle(18, 500)
val medium20 = appTextStyle(20, 500)
val medium24 = appTextStyle(24, 500)


val bold12 = appTextStyle(12, 600)
val bold14 = appTextStyle(14, 600)
val bold16 = appTextStyle(16, 600)
val bold18 = appTextStyle(18, 600)
val bold24 = appTextStyle(24, 600)

val extraBold10 = appTextStyle(10, 700)
val extraBold12 = appTextStyle(12, 700)
val extraBold14 = appTextStyle(14, 700)
val extraBold16 = appTextStyle(16, 700)
val extraBold18 = appTextStyle(18, 700)
val extraBold24 = appTextStyle(24, 700)
