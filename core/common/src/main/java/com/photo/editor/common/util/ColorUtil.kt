package com.photo.editor.common.util

fun Long.toColorHexWithAlpha(): String {
    return buildString {
        val alpha = ((this@toColorHexWithAlpha shr 24) and 0x000000ff).toString(16)
        val red = ((this@toColorHexWithAlpha shr 16) and 0x000000ff).toString(16)
        val green = ((this@toColorHexWithAlpha shr 8) and 0x000000ff).toString(16)
        val blue = (this@toColorHexWithAlpha and 0x000000ff).toString(16)
        if (alpha.length == 1) append("0")
        append(alpha)
        if (red.length == 1) append("0")
        append(red)
        if (green.length == 1) append("0")
        append(green)
        if (blue.length == 1) append("0")
        append(blue)
    }.uppercase()
}