package com.phongkaster.kanso.data.edit.edit

/**
 * What a [Layer] shows. A `sealed interface` is a closed family: every kind is declared right
 * here, so a `when` over a content must name them all and the compiler says when one is missing.
 * The kinds hold only what makes their picture; where the layer sits is the [Layer]'s business.
 */
sealed interface LayerContent {

    /** A ready-made picture from [StickerCatalog], named by its [pictureId]. */
    data class Picture(val pictureId: String) : LayerContent
}
