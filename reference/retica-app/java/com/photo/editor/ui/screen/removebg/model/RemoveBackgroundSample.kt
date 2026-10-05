package com.photo.editor.ui.screen.removebg.model

import androidx.annotation.DrawableRes
import com.photo.editor.R

enum class RemoveBackgroundSample(
    @DrawableRes
    val imageId: Int,
) {
    Image1(imageId = R.drawable.remove_bg_sample_1),
    Image2(imageId = R.drawable.remove_bg_sample_2),
    Image3(imageId = R.drawable.remove_bg_sample_3),
    Image4(imageId = R.drawable.remove_bg_sample_4),
}
