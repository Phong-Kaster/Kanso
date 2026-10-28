package com.phongkaster.kanso.ui.tool.model

import androidx.annotation.StringRes
import com.phongkaster.kanso.R

/**
 * The tabs of the Text sheet, in the order of its row of tabs (the owner's decision, 2026-10-28):
 * the sheet shows one tab's control at a time under its pinned top bar, so its many settings never
 * have to share the sheet's small height. The first tab is the one the sheet opens on.
 *
 * @param labelRes the tab's name on its chip.
 * @param needsSelection true for a tab that styles one text, greyed out while none is selected.
 */
enum class TextTab(@StringRes val labelRes: Int, val needsSelection: Boolean) {
    /** The text area and its alignment button: typing here starts a new text when none is selected. */
    TEXT(R.string.text_tool_label, needsSelection = false),

    /** The font chips. */
    FONT(R.string.layer_tab_font, needsSelection = true),

    /** The size slider. */
    SIZE(R.string.layer_size, needsSelection = true),

    /** The letters' colours. */
    COLOUR(R.string.text_colour_title, needsSelection = true),

    /** The box behind the words: none, a ready colour, or any colour with transparency. */
    BACKGROUND(R.string.text_background_title, needsSelection = true),

    /** The opacity slider. */
    OPACITY(R.string.layer_opacity, needsSelection = true),
}
