package com.photo.editor.ui.screen.edit.enums

import com.photo.editor.R

enum class EditImageRotateOption(
    val iconId: Int,
    val titleId: Int,
) {
    Left(
        iconId = R.drawable.ic_rotate_left,
        titleId = R.string.rotate_left
    ),
    Right(
        iconId = R.drawable.ic_rotate_right,
        titleId = R.string.rotate_right
    ),
    FlipHorizontally(
        iconId = R.drawable.ic_flip_horizontally,
        titleId = R.string.flip_horizontally
    ),
    FlipVertically(
        iconId = R.drawable.ic_flip_vertically,
        titleId = R.string.flip_vertically
    )
}