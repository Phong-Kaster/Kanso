package com.phongkaster.kanso.ui.tool.model

import androidx.annotation.StringRes
import com.phongkaster.kanso.R

/**
 * The twelve ready colours of the Text sheet's colour row, in the order the row shows them: white
 * and black first (the two that read on almost any photo), then the colours of the rainbow, then
 * brown and grey. [argb] is the opaque colour and [label] its spoken name. Any other colour comes
 * from the row's last circle, which opens the colour picker.
 */
enum class TextSwatch(val argb: Int, @StringRes val label: Int) {

    /** Pure white, a new text's colour. */
    WHITE(0xFFFFFFFF.toInt(), R.string.text_colour_white),

    /** Pure black. */
    BLACK(0xFF000000.toInt(), R.string.text_colour_black),

    /** A strong red. */
    RED(0xFFE53935.toInt(), R.string.text_colour_red),

    /** A warm orange. */
    ORANGE(0xFFFB8C00.toInt(), R.string.text_colour_orange),

    /** A bright yellow. */
    YELLOW(0xFFFDD835.toInt(), R.string.text_colour_yellow),

    /** A leaf green. */
    GREEN(0xFF43A047.toInt(), R.string.text_colour_green),

    /** A blue-green teal. */
    TEAL(0xFF00897B.toInt(), R.string.text_colour_teal),

    /** A clear blue. */
    BLUE(0xFF1E88E5.toInt(), R.string.text_colour_blue),

    /** A deep purple. */
    PURPLE(0xFF8E24AA.toInt(), R.string.text_colour_purple),

    /** A vivid pink. */
    PINK(0xFFD81B60.toInt(), R.string.text_colour_pink),

    /** A dark brown. */
    BROWN(0xFF6D4C41.toInt(), R.string.text_colour_brown),

    /** A middle grey. */
    GREY(0xFF757575.toInt(), R.string.text_colour_grey),
}
