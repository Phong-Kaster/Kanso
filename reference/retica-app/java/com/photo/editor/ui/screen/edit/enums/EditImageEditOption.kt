package com.photo.editor.ui.screen.edit.enums

import com.photo.editor.R

enum class EditImageEditOption(
    val iconId: Int,
    val titleId: Int
) {
    Filter(R.drawable.ic_filter, R.string.filter),
    Adjust(R.drawable.ic_adjust, R.string.adjust),
    Crop(R.drawable.ic_crop, R.string.crop),
    Rotate(R.drawable.ic_rotate, R.string.rotate),
//    Angle(R.drawable.ic_angle, R.string.angle),
    Color(R.drawable.ic_color, R.string.color),
    Sticker(R.drawable.ic_sticker, R.string.sticker),
    Text(R.drawable.ic_text, R.string.text),
    Brush(R.drawable.ic_brush, R.string.brush),
    Frame(R.drawable.ic_frame, R.string.frame),
}