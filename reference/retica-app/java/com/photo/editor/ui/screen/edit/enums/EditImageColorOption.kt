package com.photo.editor.ui.screen.edit.enums

import com.photo.editor.R

enum class EditImageColorOption(
    val iconId: Int,
    val titleId: Int,
) {
    Curve(R.drawable.ic_curve, R.string.curve),
    HSL(R.drawable.ic_hsl, R.string.hsl),
    RGB(R.drawable.ic_rgb, R.string.rgb),
}