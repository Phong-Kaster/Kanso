package com.phongkaster.kanso.data.edit.edit

import androidx.annotation.StringRes
import com.phongkaster.kanso.R

/**
 * The four tabs of the Stickers sheet, in the order they are shown. Each sticker in
 * [StickerCatalog] belongs to exactly one.
 *
 * Each case carries the string resource of its tab's name, so the sheet can write the name with
 * `stringResource(category.label)` and the words stay translatable in `strings_stickers.xml`.
 */
enum class StickerCategory(
    /** The string resource holding the tab's visible name. */
    @StringRes val label: Int,
) {

    /** Fruit, drinks, cakes and other things to eat. */
    FOOD(R.string.stickers_tab_food),

    /** Faces and small drawings that say how you feel. */
    MOOD(R.string.stickers_tab_mood),

    /** Short sayings drawn as pictures. */
    QUOTE(R.string.stickers_tab_quote),

    /** Round emoji faces. */
    EMOJI(R.string.stickers_tab_emoji),
}
