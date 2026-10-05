package com.photo.editor.ui.screen.edit.enums

import com.photo.editor.R

enum class EditImageAdjustOption(
    val iconId: Int,
    val titleId: Int,
    val valueRange: ClosedFloatingPointRange<Float>,
) {
    Lux(R.drawable.ic_lux, R.string.lux, valueRange = -1f..1f),
    Exposure(R.drawable.ic_exposure, R.string.exposure, valueRange = -1f..1f),
    Brightness(R.drawable.ic_brightness, R.string.brightness, valueRange = -1f..1f),
    Contrast(R.drawable.ic_contrast, R.string.contrast, valueRange = -1f..1f),
    Saturation(R.drawable.ic_saturation, R.string.saturation, valueRange = -1f..1f),
    Vibrance(R.drawable.ic_vibrance, R.string.vibrance, valueRange = -1f..1f),
    Clarity(R.drawable.ic_clarity, R.string.clarity, valueRange = -1f..1f),
    Highlight(R.drawable.ic_highlight, R.string.highlight, valueRange = -1f..1f),
    Shadows(R.drawable.ic_shadows, R.string.shadows, valueRange = -1f..1f),
    Warmth(R.drawable.ic_warmth, R.string.warmth, valueRange = -1f..1f),
    Tint(R.drawable.ic_tint, R.string.tint, valueRange = -1f..1f),
    Fade(R.drawable.ic_fade, R.string.fade, valueRange = -1f..1f),
    Grain(R.drawable.ic_grain, R.string.grain, valueRange = 0f..1f),
    Vignette(R.drawable.ic_vignette, R.string.vignette, valueRange = -1f..1f),
    Mosaic(R.drawable.ic_mosaic, R.string.mosaic, valueRange = 0f..1f),
//    Dust(R.drawable.ic_dust, R.string.dust),
}