package com.phongkaster.kanso.data.edit.edit

/**
 * How the lines of a text layer line up inside its block. The Text sheet's alignment button
 * cycles through them in the order declared ([next]): centre, then left, then right.
 */
enum class TextAlignment {

    /** Every line centred: the default for a new text. */
    CENTRE,

    /** Every line starts at the block's left edge. */
    LEFT,

    /** Every line ends at the block's right edge. */
    RIGHT;

    /** The alignment the button moves to from this one: centre → left → right → centre. */
    fun next(): TextAlignment = entries[(ordinal + 1) % entries.size]
}
