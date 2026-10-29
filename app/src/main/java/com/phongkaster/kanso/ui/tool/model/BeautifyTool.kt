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

    /** Face lift: a -100..+100 slider, the jaw dropped to the left (negative), raised and tightened to the right, as in Meitu. */
    FACE_LIFT(group = BeautifyGroup.FACE),

    /** Temple: a -100..+100 slider, hollower to the left (negative), fuller to the right, as in Meitu's Face > Temple. */
    TEMPLE(group = BeautifyGroup.FACE),

    /** Waist: a -100..+100 slider, wider to the left (negative), slimmer to the right, as in Meitu's Body > Waist. */
    WAIST(group = BeautifyGroup.BODY),

    /** Arms: a -100..+100 slider, thicker to the left (negative), slimmer to the right, as in Meitu's Body > Arms. */
    ARMS(group = BeautifyGroup.BODY),

    /** Chest: a -100..+100 slider, flatter to the left (negative), fuller to the right, as in Meitu's Body > Chest. */
    CHEST(group = BeautifyGroup.BODY),

    /** Legs: a -100..+100 slider, thicker to the left (negative), slimmer to the right, as Meitu's Body > Legs. */
    LEGS(group = BeautifyGroup.BODY),
}
