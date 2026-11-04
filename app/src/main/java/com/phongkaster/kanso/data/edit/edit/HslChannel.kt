package com.phongkaster.kanso.data.edit.edit

/**
 * The three things the HSL tool can change in a colour range: which colour it is (hue), how vivid
 * it is (saturation) and how light it is (lightness).
 */
enum class HslChannel {
    /** The colour itself, turned round the wheel towards a neighbouring range. */
    HUE,

    /** How vivid the colour is, from grey to full colour. */
    SATURATION,

    /** How light the colour is, from dark to light. */
    LIGHTNESS,
}
