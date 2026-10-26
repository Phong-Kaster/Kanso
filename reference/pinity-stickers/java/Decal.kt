package com.apero.pinity.data.enums

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import com.apero.pinity.R
import com.apero.pinity.ui.screen.edit_image.decal.enums.GenericColor
import com.apero.pinity.ui.screen.onboard.component.OnboardIndication
import com.apero.pinity.ui.theme.OswaldFontFamily
import com.apero.pinity.ui.theme.PrimaryColor

enum class Decal(
    @StringRes val drawableId: Int = 0,
    val category: DecalCategory = DecalCategory.Food,
    val spec: DecalSpecification? = null,
) {
    Decal_Food_1(drawableId = R.drawable.img_sticker_avocado, category = DecalCategory.Food),
    Decal_Food_2(drawableId = R.drawable.img_sticker_banana, category = DecalCategory.Food),
    Decal_Food_3(drawableId = R.drawable.img_sticker_strawberry, category = DecalCategory.Food),
    Decal_Food_4(drawableId = R.drawable.img_sticker_watermelon, category = DecalCategory.Food),
    Decal_Food_5(drawableId = R.drawable.img_sticker_milktea, category = DecalCategory.Food),
    Decal_Food_6(drawableId = R.drawable.img_sticker_omelet, category = DecalCategory.Food),
    Decal_Food_7(drawableId = R.drawable.img_sticker_bread, category = DecalCategory.Food),
    Decal_Food_8(drawableId = R.drawable.img_sticker_pear, category = DecalCategory.Food),
    Decal_Food_9(drawableId = R.drawable.img_sticker_smoothie, category = DecalCategory.Food),
    Decal_Food_10(drawableId = R.drawable.img_sticker_cake, category = DecalCategory.Food),
    Decal_Food_11(drawableId = R.drawable.img_sticker_cake_1, category = DecalCategory.Food),
    Decal_Food_12(drawableId = R.drawable.img_sticker_coffee, category = DecalCategory.Food),
    Decal_Food_13(drawableId = R.drawable.img_sticker_buddha, category = DecalCategory.Food),
    Decal_Food_14(drawableId = R.drawable.img_sticker_apple, category = DecalCategory.Food),
    Decal_Food_15(drawableId = R.drawable.img_sticker_cake_2, category = DecalCategory.Food),
    Decal_Food_16(drawableId = R.drawable.img_sticker_chestnut, category = DecalCategory.Food),
    Decal_Food_17(drawableId = R.drawable.img_sticker_strawberry_1, category = DecalCategory.Food),

    Decal_Mood_1(drawableId = R.drawable.img_sticker_mood_1, category = DecalCategory.Mood),
    Decal_Mood_2(drawableId = R.drawable.img_sticker_mood_2, category = DecalCategory.Mood),
    Decal_Mood_3(drawableId = R.drawable.img_sticker_mood_3, category = DecalCategory.Mood),
    Decal_Mood_4(drawableId = R.drawable.img_sticker_mood_4, category = DecalCategory.Mood),
    Decal_Mood_5(drawableId = R.drawable.img_sticker_mood_5, category = DecalCategory.Mood),
    Decal_Mood_6(drawableId = R.drawable.img_sticker_mood_6, category = DecalCategory.Mood),
    Decal_Mood_7(drawableId = R.drawable.img_sticker_mood_7, category = DecalCategory.Mood),
    Decal_Mood_8(drawableId = R.drawable.img_sticker_mood_8, category = DecalCategory.Mood),
    Decal_Mood_9(drawableId = R.drawable.img_sticker_mood_9, category = DecalCategory.Mood),
    Decal_Mood_10(drawableId = R.drawable.img_sticker_mood_10, category = DecalCategory.Mood),
    Decal_Mood_11(drawableId = R.drawable.img_sticker_mood_11, category = DecalCategory.Mood),
    Decal_Mood_12(drawableId = R.drawable.img_sticker_mood_12, category = DecalCategory.Mood),
    Decal_Mood_13(drawableId = R.drawable.img_sticker_mood_13, category = DecalCategory.Mood),
    Decal_Mood_14(drawableId = R.drawable.img_sticker_mood_14, category = DecalCategory.Mood),
    Decal_Mood_15(drawableId = R.drawable.img_sticker_mood_15, category = DecalCategory.Mood),
    Decal_Mood_16(drawableId = R.drawable.img_sticker_mood_16, category = DecalCategory.Mood),
    Decal_Mood_17(drawableId = R.drawable.img_sticker_mood_17, category = DecalCategory.Mood),

    Dcael_Quote_1(drawableId = R.drawable.img_sticker_quote_1, category = DecalCategory.Quote),
    Dcael_Quote_2(drawableId = R.drawable.img_sticker_quote_2, category = DecalCategory.Quote),
    Dcael_Quote_3(drawableId = R.drawable.img_sticker_quote_3, category = DecalCategory.Quote),
    Dcael_Quote_4(drawableId = R.drawable.img_sticker_quote_4, category = DecalCategory.Quote),
    Dcael_Quote_5(drawableId = R.drawable.img_sticker_quote_5, category = DecalCategory.Quote),
    Dcael_Quote_6(drawableId = R.drawable.img_sticker_quote_6, category = DecalCategory.Quote),

    Decal_Emoji_1(drawableId = R.drawable.img_sticker_emoji_1, category = DecalCategory.Emoji),
    Decal_Emoji_2(drawableId = R.drawable.img_sticker_emoji_2, category = DecalCategory.Emoji),
    Decal_Emoji_3(drawableId = R.drawable.img_sticker_emoji_3, category = DecalCategory.Emoji),
    Decal_Emoji_4(drawableId = R.drawable.img_sticker_emoji_4, category = DecalCategory.Emoji),
    Decal_Emoji_5(drawableId = R.drawable.img_sticker_emoji_5, category = DecalCategory.Emoji),
    Decal_Emoji_6(drawableId = R.drawable.img_sticker_emoji_6, category = DecalCategory.Emoji),
    Decal_Emoji_7(drawableId = R.drawable.img_sticker_emoji_7, category = DecalCategory.Emoji),
    Decal_Emoji_8(drawableId = R.drawable.img_sticker_emoji_8, category = DecalCategory.Emoji),
    Decal_Emoji_9(drawableId = R.drawable.img_sticker_emoji_9, category = DecalCategory.Emoji),
    Decal_Emoji_10(drawableId = R.drawable.img_sticker_emoji_10, category = DecalCategory.Emoji),
    Decal_Emoji_11(drawableId = R.drawable.img_sticker_emoji_11, category = DecalCategory.Emoji),
    Decal_Emoji_12(drawableId = R.drawable.img_sticker_emoji_12, category = DecalCategory.Emoji),
    Decal_Emoji_13(drawableId = R.drawable.img_sticker_emoji_13, category = DecalCategory.Emoji),
    Decal_Emoji_14(drawableId = R.drawable.img_sticker_emoji_14, category = DecalCategory.Emoji),
    Decal_Emoji_15(drawableId = R.drawable.img_sticker_emoji_15, category = DecalCategory.Emoji),
    Decal_Emoji_16(drawableId = R.drawable.img_sticker_emoji_16, category = DecalCategory.Emoji),
    Decal_Emoji_17(drawableId = R.drawable.img_sticker_emoji_17, category = DecalCategory.Emoji),
    Decal_Emoji_18(drawableId = R.drawable.img_sticker_emoji_18, category = DecalCategory.Emoji),
    Decal_Emoji_19(drawableId = R.drawable.img_sticker_emoji_19, category = DecalCategory.Emoji),
    Decal_Emoji_20(drawableId = R.drawable.img_sticker_emoji_20, category = DecalCategory.Emoji),

    Decal_Weather_1(drawableId = R.drawable.decal_weather_1, category = DecalCategory.Weather),
    Decal_Weather_2(drawableId = R.drawable.decal_weather_2, category = DecalCategory.Weather),
    Decal_Weather_3(drawableId = R.drawable.decal_weather_3, category = DecalCategory.Weather),
    Decal_Weather_4(drawableId = R.drawable.decal_weather_4, category = DecalCategory.Weather),
    Decal_Weather_5(drawableId = R.drawable.decal_weather_5, category = DecalCategory.Weather),
    Decal_Weather_6(drawableId = R.drawable.decal_weather_6, category = DecalCategory.Weather),

    Decal_Time_1(
        drawableId = 0,
        category = DecalCategory.Time,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF95B67F),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            drawId = R.drawable.img_clock_green,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Time_2(
        drawableId = 1,
        category = DecalCategory.Time,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF000000),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            drawId = R.drawable.img_clock_black,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Time_3(
        drawableId = 2,
        category = DecalCategory.Time,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundColor = Color.Black,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            drawId = R.drawable.img_clock_white,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Time_4(
        drawableId = 3, category = DecalCategory.Time,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundColor = PrimaryColor,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            drawId = R.drawable.img_clock_white,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),

    Decal_Date_1(
        drawableId = 0, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF95B67F),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_2(
        drawableId = 1, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF000000),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_3(
        drawableId = 2, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundColor = PrimaryColor,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_4(
        drawableId = 3, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundColor = Color.Black,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_5(
        drawableId = 4, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF95B67F),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_6(
        drawableId = 5, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF000000),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_7(
        drawableId = 6, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundColor = PrimaryColor,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_8(
        drawableId = 7, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundColor = Color.Black,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_9(
        drawableId = 8, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF95B67F),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_10(
        drawableId = 9, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_11(
        drawableId = 10, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF000000),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),
    Decal_Date_12(
        drawableId = 11, category = DecalCategory.Date,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF007AFF),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true
        )
    ),


    Decal_Location_1(
        drawableId = 0, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF000000),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            borderColor = Color.Black,
            drawId = R.drawable.img_location_black,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Location_2(
        drawableId = 1, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF95B67F),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            borderColor = PrimaryColor,
            drawId = R.drawable.img_location_green,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Location_3(
        drawableId = 2, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF007AFF),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            borderColor = Color(0xFF007AFF),
            drawId = R.drawable.img_location_blue,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Location_4(
        drawableId = R.drawable.location_4, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF000000),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            drawId = R.drawable.img_location_black,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Location_5(
        drawableId = R.drawable.location_5, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF007AFF),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            drawId = R.drawable.img_location_blue,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Location_6(
        drawableId = R.drawable.location_6, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF34C759),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            drawId = R.drawable.img_location_lime,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Location_7(
        drawableId = R.drawable.location_7, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF95B67F),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            borderColor = PrimaryColor
        )
    ),
    Decal_Location_8(
        drawableId = 3, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            borderColor = Color.White,
            shadow = Shadow(color = Color(0x40000000), blurRadius = 10f)
        )
    ),
    Decal_Location_9(
        drawableId = R.drawable.location_9, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF000000),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            borderColor = Color.Black,
        )
    ),
    Decal_Location_10(
        drawableId = R.drawable.location_10, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF95B67F),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            drawId = R.drawable.img_location_green,
            drawSpacing = 5f,
            drawSize = 18f,
        )
    ),
    Decal_Location_11(
        drawableId = 4, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            borderColor = Color.White,
            drawId = R.drawable.img_location_white,
            drawSpacing = 5f,
            drawSize = 18f,
            shadow = Shadow(color = Color(0x40000000), blurRadius = 10f)
        )
    ),
    Decal_Location_12(
        drawableId = R.drawable.location_12, category = DecalCategory.Location,
        spec = DecalSpecification(
            fontSize = 11f,
            textColor = GenericColor.SolidColor(0xFF007AFF),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
            bold = true,
            borderColor = Color(0xFF007AFF),
        )
    ),

    Decal_Numeric_Clock_1(
        drawableId = 4,
        category = DecalCategory.Time,
        spec = DecalSpecification(
            textColor = GenericColor.SolidColor(0xFF000000),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
        )
    ),
    Decal_Numeric_Clock_2(
        drawableId = 4,
        category = DecalCategory.Time,
        spec = DecalSpecification(
            textColor = GenericColor.SolidColor(0xFF95B67F),
            backgroundColor = Color.White,
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
        )
    ),
    Decal_Numeric_Clock_3(
        drawableId = 4,
        category = DecalCategory.Time,
        spec = DecalSpecification(
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundColor = Color(0xFF95B67F),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
        )
    ),
    Decal_Numeric_Clock_4(
        drawableId = 4,
        category = DecalCategory.Time,
        spec = DecalSpecification(
            textColor = GenericColor.SolidColor(0xFFFFFFFF),
            backgroundColor = Color(0xFF000000),
            backgroundCornerRadius = 24f,
            fontFamily = OswaldFontFamily,
        )
    ),

    ;

    companion object {
        fun isNumericClockDecal(decal: Decal): Boolean {
            val list = listOf(
                Decal_Numeric_Clock_1,
                Decal_Numeric_Clock_2,
                Decal_Numeric_Clock_3,
                Decal_Numeric_Clock_4,
            )
            return list.contains(decal)
        }

        fun isFullDateDecal(decal: Decal): Boolean {
            val list = listOf(
                Decal_Date_1,
                Decal_Date_2,
                Decal_Date_3,
                Decal_Date_4
            )
            return list.contains(decal)
        }

        fun isDateAndMonthDecal(decal: Decal): Boolean {
            val list = listOf(
                Decal_Date_5,
                Decal_Date_6,
                Decal_Date_7,
                Decal_Date_8
            )
            return list.contains(decal)
        }

        fun isMonthAndYearDecal(decal: Decal): Boolean {
            val list = listOf(
                Decal_Date_9,
                Decal_Date_10,
                Decal_Date_11,
                Decal_Date_12
            )
            return list.contains(decal)
        }

        fun changeNextDecal(decal: Decal, decalCategory: DecalCategory): Decal {
            val listOfDecal: List<Decal> =
                entries.filter { decal -> decal.category == decalCategory }
            val index = listOfDecal.indexOf(decal)
            return listOfDecal.toTypedArray().getOrElse(index + 1) { listOfDecal.first() }
        }
    }
}