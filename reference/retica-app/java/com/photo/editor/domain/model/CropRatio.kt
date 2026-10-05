package com.photo.editor.domain.model

import com.photo.editor.R

enum class CropRatio(
    val iconId: Int,
    val titleId: Int,
    val numerator: Int,
    val denominator: Int,
    val ratio: Float = numerator.toFloat() / denominator
) {
//    Original(R.drawable.ic_crop_original, R.string.ratio_original, 1, 1),
    Free(R.drawable.ic_crop_free, R.string.ratio_free, 1, 1),
    Square(R.drawable.ic_crop_1_1, R.string.ratio_square, 1, 1),
    Ratio916(R.drawable.ic_crop_9_16, R.string.ratio_9_16, 9, 16),
    Ratio169(R.drawable.ic_crop_16_9, R.string.ratio_16_9, 16, 9),
    Ratio23(R.drawable.ic_crop_2_3, R.string.ratio_2_3, 2, 3),
    Ratio32(R.drawable.ic_crop_3_2, R.string.ratio_3_2, 3, 2),
    Ratio34(R.drawable.ic_crop_3_4, R.string.ratio_3_4, 3, 4),
    Ratio43(R.drawable.ic_crop_4_3, R.string.ratio_4_3, 4, 3),
}