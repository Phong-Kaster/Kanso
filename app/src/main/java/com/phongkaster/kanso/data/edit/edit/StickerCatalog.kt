package com.phongkaster.kanso.data.edit.edit

import com.phongkaster.kanso.R

/**
 * The list of every ready-made sticker the Edit screen offers: 60 pictures in four categories
 * (17 food, 17 mood, 6 quote, 20 emoji), in the order the Stickers sheet shows them.
 *
 * Each entry names its drawable with an explicit `R.drawable.…` reference, never a lookup by
 * name at run time: the build then checks every picture exists, and the resource shrinker keeps
 * them. An entry's `pictureId` is the drawable's own name, which is also the text stored in a
 * [Layer], so a saved photo's sticker can always be found again with [byId].
 *
 * Only picture layers are listed; the text, clock, date, location and weather layers of the
 * app this was ported from are not part of Kanso.
 */
object StickerCatalog {

    /** Builds one entry, taking the id from the drawable's name so the two can never disagree. */
    private fun entry(name: String, res: Int, category: StickerCategory) =
        CatalogSticker(pictureId = name, res = res, category = category)

    /** All 60 layers, in display order: food, then mood, then quote, then emoji. */
    val all: List<CatalogSticker> = listOf(
        entry("img_sticker_avocado", R.drawable.img_sticker_avocado, StickerCategory.FOOD),
        entry("img_sticker_banana", R.drawable.img_sticker_banana, StickerCategory.FOOD),
        entry("img_sticker_strawberry", R.drawable.img_sticker_strawberry, StickerCategory.FOOD),
        entry("img_sticker_watermelon", R.drawable.img_sticker_watermelon, StickerCategory.FOOD),
        entry("img_sticker_milktea", R.drawable.img_sticker_milktea, StickerCategory.FOOD),
        entry("img_sticker_omelet", R.drawable.img_sticker_omelet, StickerCategory.FOOD),
        entry("img_sticker_bread", R.drawable.img_sticker_bread, StickerCategory.FOOD),
        entry("img_sticker_pear", R.drawable.img_sticker_pear, StickerCategory.FOOD),
        entry("img_sticker_smoothie", R.drawable.img_sticker_smoothie, StickerCategory.FOOD),
        entry("img_sticker_cake", R.drawable.img_sticker_cake, StickerCategory.FOOD),
        entry("img_sticker_cake_1", R.drawable.img_sticker_cake_1, StickerCategory.FOOD),
        entry("img_sticker_coffee", R.drawable.img_sticker_coffee, StickerCategory.FOOD),
        entry("img_sticker_buddha", R.drawable.img_sticker_buddha, StickerCategory.FOOD),
        entry("img_sticker_apple", R.drawable.img_sticker_apple, StickerCategory.FOOD),
        entry("img_sticker_cake_2", R.drawable.img_sticker_cake_2, StickerCategory.FOOD),
        entry("img_sticker_chestnut", R.drawable.img_sticker_chestnut, StickerCategory.FOOD),
        entry("img_sticker_strawberry_1", R.drawable.img_sticker_strawberry_1, StickerCategory.FOOD),

        entry("img_sticker_mood_1", R.drawable.img_sticker_mood_1, StickerCategory.MOOD),
        entry("img_sticker_mood_2", R.drawable.img_sticker_mood_2, StickerCategory.MOOD),
        entry("img_sticker_mood_3", R.drawable.img_sticker_mood_3, StickerCategory.MOOD),
        entry("img_sticker_mood_4", R.drawable.img_sticker_mood_4, StickerCategory.MOOD),
        entry("img_sticker_mood_5", R.drawable.img_sticker_mood_5, StickerCategory.MOOD),
        entry("img_sticker_mood_6", R.drawable.img_sticker_mood_6, StickerCategory.MOOD),
        entry("img_sticker_mood_7", R.drawable.img_sticker_mood_7, StickerCategory.MOOD),
        entry("img_sticker_mood_8", R.drawable.img_sticker_mood_8, StickerCategory.MOOD),
        entry("img_sticker_mood_9", R.drawable.img_sticker_mood_9, StickerCategory.MOOD),
        entry("img_sticker_mood_10", R.drawable.img_sticker_mood_10, StickerCategory.MOOD),
        entry("img_sticker_mood_11", R.drawable.img_sticker_mood_11, StickerCategory.MOOD),
        entry("img_sticker_mood_12", R.drawable.img_sticker_mood_12, StickerCategory.MOOD),
        entry("img_sticker_mood_13", R.drawable.img_sticker_mood_13, StickerCategory.MOOD),
        entry("img_sticker_mood_14", R.drawable.img_sticker_mood_14, StickerCategory.MOOD),
        entry("img_sticker_mood_15", R.drawable.img_sticker_mood_15, StickerCategory.MOOD),
        entry("img_sticker_mood_16", R.drawable.img_sticker_mood_16, StickerCategory.MOOD),
        entry("img_sticker_mood_17", R.drawable.img_sticker_mood_17, StickerCategory.MOOD),

        entry("img_sticker_quote_1", R.drawable.img_sticker_quote_1, StickerCategory.QUOTE),
        entry("img_sticker_quote_2", R.drawable.img_sticker_quote_2, StickerCategory.QUOTE),
        entry("img_sticker_quote_3", R.drawable.img_sticker_quote_3, StickerCategory.QUOTE),
        entry("img_sticker_quote_4", R.drawable.img_sticker_quote_4, StickerCategory.QUOTE),
        entry("img_sticker_quote_5", R.drawable.img_sticker_quote_5, StickerCategory.QUOTE),
        entry("img_sticker_quote_6", R.drawable.img_sticker_quote_6, StickerCategory.QUOTE),

        entry("img_sticker_emoji_1", R.drawable.img_sticker_emoji_1, StickerCategory.EMOJI),
        entry("img_sticker_emoji_2", R.drawable.img_sticker_emoji_2, StickerCategory.EMOJI),
        entry("img_sticker_emoji_3", R.drawable.img_sticker_emoji_3, StickerCategory.EMOJI),
        entry("img_sticker_emoji_4", R.drawable.img_sticker_emoji_4, StickerCategory.EMOJI),
        entry("img_sticker_emoji_5", R.drawable.img_sticker_emoji_5, StickerCategory.EMOJI),
        entry("img_sticker_emoji_6", R.drawable.img_sticker_emoji_6, StickerCategory.EMOJI),
        entry("img_sticker_emoji_7", R.drawable.img_sticker_emoji_7, StickerCategory.EMOJI),
        entry("img_sticker_emoji_8", R.drawable.img_sticker_emoji_8, StickerCategory.EMOJI),
        entry("img_sticker_emoji_9", R.drawable.img_sticker_emoji_9, StickerCategory.EMOJI),
        entry("img_sticker_emoji_10", R.drawable.img_sticker_emoji_10, StickerCategory.EMOJI),
        entry("img_sticker_emoji_11", R.drawable.img_sticker_emoji_11, StickerCategory.EMOJI),
        entry("img_sticker_emoji_12", R.drawable.img_sticker_emoji_12, StickerCategory.EMOJI),
        entry("img_sticker_emoji_13", R.drawable.img_sticker_emoji_13, StickerCategory.EMOJI),
        entry("img_sticker_emoji_14", R.drawable.img_sticker_emoji_14, StickerCategory.EMOJI),
        entry("img_sticker_emoji_15", R.drawable.img_sticker_emoji_15, StickerCategory.EMOJI),
        entry("img_sticker_emoji_16", R.drawable.img_sticker_emoji_16, StickerCategory.EMOJI),
        entry("img_sticker_emoji_17", R.drawable.img_sticker_emoji_17, StickerCategory.EMOJI),
        entry("img_sticker_emoji_18", R.drawable.img_sticker_emoji_18, StickerCategory.EMOJI),
        entry("img_sticker_emoji_19", R.drawable.img_sticker_emoji_19, StickerCategory.EMOJI),
        entry("img_sticker_emoji_20", R.drawable.img_sticker_emoji_20, StickerCategory.EMOJI),
    )

    /** Every sticker by its picture id, built once so [byId] is a single lookup. */
    private val byPictureId: Map<String, CatalogSticker> = all.associateBy { it.pictureId }

    /** The layers of one [category], in display order. */
    fun inCategory(category: StickerCategory): List<CatalogSticker> =
        all.filter { it.category == category }

    /** The sticker whose picture id is [pictureId], or null when the catalog has no such picture. */
    fun byId(pictureId: String): CatalogSticker? = byPictureId[pictureId]
}
