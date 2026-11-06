package com.phongkaster.kanso.ui.tool.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EDIT_BLUE_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EDIT_GREEN_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EDIT_RED_TAG

/**
 * The three colour channels of the Edit screen's RGB tool. RGB shows one chip per entry and a
 * single slider for the chosen one, the way Adjust does (see [AdjustSlider]), so its sheet is as
 * short as the others'.
 *
 * [labelRes] names the channel beside its slider; [sliderTag] is the slider's test tag. [range]
 * is the HSL colour range of the same name, whose circle RGB shows as the channel's chip, so red,
 * green and blue look the same in the RGB and HSL sheets. [lightTint] and [darkTint] colour the
 * slider's track and thumb (see [tintOn]), so the person sees which channel they are moving
 * without reading: a deep shade on the light sheet, a pale one on the dark sheet, each chosen to
 * stand out from the sheet by at least the 3:1 contrast WCAG asks of graphics. (HSL's circle
 * colours cannot colour the slider: its green is about 1.7:1 on the light sheet.)
 */
enum class RgbChannel(
    @StringRes val labelRes: Int,
    val sliderTag: String,
    val range: ColourRange,
    val lightTint: Color,
    val darkTint: Color,
) {
    /** The red channel: lowering it pushes the photo towards cyan, raising it towards red. */
    RED(R.string.edit_tool_red, EDIT_RED_TAG, ColourRange.RED, lightTint = Color(0xFFC62828), darkTint = Color(0xFFFF8A80)),

    /** The green channel: lowering it pushes the photo towards magenta, raising it towards green. */
    GREEN(R.string.edit_tool_green, EDIT_GREEN_TAG, ColourRange.GREEN, lightTint = Color(0xFF2E7D32), darkTint = Color(0xFF81C784)),

    /** The blue channel: lowering it pushes the photo towards yellow, raising it towards blue. */
    BLUE(R.string.edit_tool_blue, EDIT_BLUE_TAG, ColourRange.BLUE, lightTint = Color(0xFF1565C0), darkTint = Color(0xFF64B5F6)),
    ;

    /**
     * This channel's tint for a theme whose surface is [surface]: [darkTint] when that surface is
     * dark, [lightTint] when it is light. Reading the surface, rather than a setting, works
     * whichever way the theme was chosen (the phone's setting, or Kanso's own light or dark).
     */
    fun tintOn(surface: Color): Color = if (surface.luminance() < HALF_LUMINANCE) darkTint else lightTint

    /** The surface luminance below which a theme counts as dark. */
    private companion object {
        /** The luminance (0 black .. 1 white) below which a surface counts as dark. */
        const val HALF_LUMINANCE = 0.5f
    }
}
