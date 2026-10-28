package com.phongkaster.kanso.ui.tool.model

import androidx.annotation.StringRes
import com.phongkaster.kanso.R

/**
 * The tabs of the Stickers sheet, in the order of its row of tabs (the owner's decision,
 * 2026-10-28): one tab's control at a time under the pinned top bar, the first being the one the
 * sheet opens on.
 *
 * @param labelRes the tab's name on its chip.
 * @param needsSelection true for a tab that changes one sticker, greyed out while none is selected.
 */
enum class StickersTab(@StringRes val labelRes: Int, val needsSelection: Boolean) {
    /** The category chips and the grid of stickers: a tile adds a sticker. */
    STICKERS(R.string.stickers_tool_label, needsSelection = false),

    /** The size slider. */
    SIZE(R.string.layer_size, needsSelection = true),

    /** The opacity slider. */
    OPACITY(R.string.layer_opacity, needsSelection = true),
}
