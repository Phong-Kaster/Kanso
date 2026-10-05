package com.photo.editor.ui.screen.retouch.model

import androidx.annotation.DrawableRes
import com.photo.editor.R

enum class RetouchSample(
    @DrawableRes
    val imageId: Int,
) {
    Image1(imageId = R.drawable.retouch_sample_1),
    Image2(imageId = R.drawable.retouch_sample_2),
    Image3(imageId = R.drawable.retouch_sample_3),
    Image4(imageId = R.drawable.retouch_sample_4),
}
