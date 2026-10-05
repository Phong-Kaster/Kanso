package com.photo.editor.domain.model

import androidx.annotation.DrawableRes
import com.photo.editor.R

enum class AspectRatio(
    val title: String,
    @DrawableRes val iconId: Int,
    val numerator: Int,
    val denominator: Int,
    val value: Float = numerator.toFloat()/ denominator,
) {
    Ratio11(
        title = "1:1",
        iconId = R.drawable.ic_ratio_1_1,
        numerator = 1,
        denominator = 1,
    ),
    Ratio34(
        title = "3:4",
        iconId = R.drawable.ic_ratio_3_4,
        numerator = 3,
        denominator = 4,
    ),
    Ratio916(
        title = "9:16",
        iconId = R.drawable.ic_ratio_9_16,
        numerator = 9,
        denominator = 16,
    ),
    RatioFull(
        title = "Full",
        iconId = R.drawable.ic_ratio_full,
        numerator = 1,
        denominator = 1,
    );
}