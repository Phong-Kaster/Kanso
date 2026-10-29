package com.phongkaster.kanso.ui.tool.model

/**
 * The Beautify tools, each with one slider. A tool with a [group] lives in that group's row
 * (layer 2: tap the group, then the tool); a tool without one sits in the top row itself
 * (layer 1). Tapping a tool opens its slider above the row; tapping it again closes it.
 */
enum class BeautifyTool(val group: BeautifyGroup?) {
    /** Smoothing the skin: a 0..100 slider, straight in the top row. */
    SMOOTH(group = null),

    /** Face width: a -100..+100 slider, wider to the left (negative), narrower to the right, as in Meitu. */
    FACE_WIDTH(group = BeautifyGroup.FACE),

    /** Face lift: a 0..100 slider, from the face as it is to the jawline raised and tightened the most. */
    FACE_LIFT(group = BeautifyGroup.FACE),
}
