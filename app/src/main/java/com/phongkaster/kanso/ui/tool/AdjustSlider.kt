package com.phongkaster.kanso.ui.tool

import androidx.annotation.StringRes
import com.phongkaster.kanso.R
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_BRIGHTNESS_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_CONTRAST_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_SATURATION_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.EDIT_WARMTH_TAG

/**
 * The four sub-tools of the Edit screen's Adjust tool. Adjust shows one chip per entry and a
 * single slider for the chosen one, the way Adobe Lightroom does, so its panel is as short as the
 * other tools' and the photo above it never has to change size.
 *
 * [labelRes] names the sub-tool on its chip and beside its slider; [sliderTag] is the slider's
 * test tag, the same tag each slider carried when all four were shown at once.
 */
enum class AdjustSlider(@StringRes val labelRes: Int, val sliderTag: String) {
    /** How light the whole photo is. */
    BRIGHTNESS(R.string.edit_tool_brightness, EDIT_BRIGHTNESS_TAG),

    /** How far apart the darks and the lights are. */
    CONTRAST(R.string.edit_tool_contrast, EDIT_CONTRAST_TAG),

    /** How strong the colours are. */
    SATURATION(R.string.edit_tool_saturation, EDIT_SATURATION_TAG),

    /** Shifts the colours towards orange (warmer) or blue (cooler). */
    WARMTH(R.string.edit_tool_warmth, EDIT_WARMTH_TAG),
}
