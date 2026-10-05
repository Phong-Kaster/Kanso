package com.photo.editor.ui.screen.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.photo.editor.R

enum class HomeButtonModel(
    @DrawableRes val iconId: Int,
    @StringRes val titleId: Int,
    val eventName: String,
    val hot: Boolean = false,
) {
    Artistic(
        iconId = R.drawable.ic_ai_artistic,
        titleId = R.string.ai_artistic,
        eventName = "artistic",
    ),
    RemoveBG(
        iconId = R.drawable.ic_remove_bg,
        titleId = R.string.bg_remove,
        eventName = "remove_bg",
    ),

    Retouch(
        iconId = R.drawable.ic_retouch,
        titleId = R.string.retouch,
        eventName = "retouch",
    ),


    Enhance(
        iconId = R.drawable.ic_ai_enhance,
        titleId = R.string.ai_enhance,
        eventName = "enhance",
    ),

    Filter(
        iconId = R.drawable.ic_filter,
        titleId = R.string.filter,
        eventName = "filter",
    ),

    Beautify(
        iconId = R.drawable.ic_beauty,
        titleId = R.string.beauty,
        eventName = "beauty",
    ),


    Makeup(
        iconId = R.drawable.ic_makeup,
        titleId = R.string.makeup,
        eventName = "makeup",
    ),

    Sticker(
        iconId = R.drawable.ic_sticker,
        titleId = R.string.sticker,
        eventName = "sticker",
    ),

    Frame(
        iconId = R.drawable.ic_frame,
        titleId = R.string.frame,
        eventName = "frame",
    ),

    Brush(
        iconId = R.drawable.ic_brush,
        titleId = R.string.brush,
        eventName = "brush",
    ),
}