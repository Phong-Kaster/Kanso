package com.phongkaster.kanso.data.edit.edit

import androidx.annotation.DrawableRes

/**
 * One ready-made sticker in [StickerCatalog]: what the Stickers sheet shows and what the
 * processor later draws.
 *
 * It is not the same as a [Sticker]: this is the picture on offer (one of 60, fixed in the app),
 * while a [Sticker] is one copy of it placed on the photo with its own position, size and turn.
 * The two are linked by [pictureId].
 */
data class CatalogSticker(
    /** The drawable's own name, for example `img_sticker_mood_3`; stored in [Sticker.pictureId]. */
    val pictureId: String,
    /** The drawable resource of the picture, used to show it in the sheet and to decode it. */
    @DrawableRes val res: Int,
    /** The tab of the Stickers sheet this sticker is listed under. */
    val category: StickerCategory,
)
