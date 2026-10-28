package com.phongkaster.kanso.ui.tool.model

import androidx.annotation.StringRes
import com.phongkaster.kanso.R

/**
 * The tabs of the Date sheet, in the order of its row of tabs (the owner's decision, 2026-10-28):
 * one tab's control at a time under the pinned top bar. The first tab is the one the sheet opens
 * on. The first two work with no date selected: a format chip adds a date, and the day and time
 * set the moment the next date gets.
 *
 * @param labelRes the tab's name on its chip.
 * @param needsSelection true for a tab that styles one date, greyed out while none is selected.
 */
enum class DateTab(@StringRes val labelRes: Int, val needsSelection: Boolean) {
    /** The 20 format chips. */
    FORMAT(R.string.date_tab_format, needsSelection = false),

    /** The day and time buttons, which open the pickers. */
    MOMENT(R.string.date_tab_moment, needsSelection = false),

    /** The font chips. */
    FONT(R.string.layer_tab_font, needsSelection = true),

    /** The size slider. */
    SIZE(R.string.layer_size, needsSelection = true),

    /** The letters' colours. */
    COLOUR(R.string.text_colour_title, needsSelection = true),

    /** The box behind the words. */
    BACKGROUND(R.string.text_background_title, needsSelection = true),

    /** The opacity slider. */
    OPACITY(R.string.layer_opacity, needsSelection = true),
}
