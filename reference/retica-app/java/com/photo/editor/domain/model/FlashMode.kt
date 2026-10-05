package com.photo.editor.domain.model

import androidx.annotation.DrawableRes
import com.photo.editor.R

enum class FlashMode(
    @DrawableRes val iconId: Int,
) {
    On(R.drawable.ic_flash_on),
    Off(R.drawable.ic_flash_off),
    Auto(R.drawable.ic_flash_auto),
}