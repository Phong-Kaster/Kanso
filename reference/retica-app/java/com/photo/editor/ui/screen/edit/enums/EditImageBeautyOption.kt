package com.photo.editor.ui.screen.edit.enums

import com.photo.editor.R

enum class EditImageBeautyOption(
    val iconId: Int,
    val titleId: Int
) {
    Makeup(
        iconId = R.drawable.ic_makeup,
        titleId = R.string.makeup
    ),
    Slim(
        iconId = R.drawable.ic_slim,
        titleId = R.string.slim
    ),
    Length(
        iconId = R.drawable.ic_length,
        titleId = R.string.length
    ),
    Brighten(
        iconId = R.drawable.ic_brighten,
        titleId = R.string.brighten
    ),
    Eyes(
        iconId = R.drawable.ic_eyes,
        titleId = R.string.eyes
    ),
    Smooth(
        iconId = R.drawable.ic_smooth,
        titleId = R.string.smooth
    ),
    Nose(
        iconId = R.drawable.ic_nose,
        titleId = R.string.nose
    ),
    Teeth(
        iconId = R.drawable.ic_teeth,
        titleId = R.string.teeth
    )
}