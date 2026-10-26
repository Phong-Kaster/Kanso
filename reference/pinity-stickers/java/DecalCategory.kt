package com.apero.pinity.data.enums

import androidx.annotation.StringRes
import com.apero.pinity.R

enum class DecalCategory(
    @StringRes val text: Int,
    val type: DecalType = DecalType.Image
) {
    Food(text = R.string.food, type = DecalType.Image),
    Mood(text = R.string.mood, type = DecalType.Image),
    Quote(text = R.string.quote, type = DecalType.Image),
    Emoji(text = R.string.emoji, type = DecalType.Image),
    Weather(text = R.string.weather, type = DecalType.Image),
//    Forecast(text = R.string.forecast, type = DecalType.Forecast),
    Location(text = R.string.location, type = DecalType.Location),
    Time(text = R.string.time, type = DecalType.Time),
    Date(text = R.string.date, type = DecalType.Date),
}