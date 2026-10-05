package com.photo.editor.ui.screen.edit.enums

import androidx.annotation.StringRes
import com.photo.editor.R

enum class EditImageMakeupOption(
    @StringRes val titleId: Int,
) {
    Lipstick(
        titleId = R.string.lipstick,
    ),
    Eyelashes(
        titleId = R.string.eyelash,
    ),
    Eyebrows(
        titleId = R.string.eyebrow,
    ),
    Blush(
        titleId = R.string.blush,
    ),
    Eyeshadow(
        titleId = R.string.eyeshadow,
    ),
    Eyeliner(
        titleId = R.string.eyeliner,
    ),
    EyeLen(
        titleId = R.string.eyelen,
    ),
    BeautyMark(
        titleId = R.string.mark,
    ),
}