package com.phongkaster.kanso.ui.tool.model

/**
 * The tools in the Beautify panel's row, in the order shown, like the Edit screen's row of tools.
 * Tapping one opens its slider above the row; tapping it again closes it.
 */
enum class BeautifyTool {
    /** Smoothing the skin: a 0..100 slider. */
    SMOOTH,

    /** Face width: a -100..+100 slider, wider to the left (negative), narrower to
     * the right (positive), as in Meitu. */
    FACE_WIDTH,
}
