package com.photo.editor.ui.screen.artistic.model

import androidx.annotation.StringRes
import com.photo.editor.R

enum class ArtisticStyle(
    @StringRes val titleId: Int,
    val assetPath: String,
) {

    Illustration(
        titleId = R.string.style_illustration,
        assetPath = "style_transfer_style/illustration.webp",
    ),

    Picasso(
        titleId = R.string.style_picasso,
        assetPath = "style_transfer_style/picasso.webp",
    ),

    VanGogh(
        titleId = R.string.style_van_gogh,
        assetPath = "style_transfer_style/van_gogh.webp",
    ),

    Digital(
        titleId = R.string.style_digital,
        assetPath = "style_transfer_style/digital.webp",
    ),
    
    OilColor(
        titleId = R.string.style_oil_color,
        assetPath = "style_transfer_style/oil_color.webp",
    ),


    WaterColor(
        titleId = R.string.style_water_color,
        assetPath = "style_transfer_style/water_color.webp",
    ),

    Candy(
        titleId = R.string.style_candy,
        assetPath = "style_transfer_style/candy.webp",
    ),


    Cubism(
        titleId = R.string.style_cubism,
        assetPath = "style_transfer_style/cubism.webp",
    ),

    NeoClassic(
        titleId = R.string.style_neo_classic,
        assetPath = "style_transfer_style/neo_classic.webp",
    ),

    InkColor(
        titleId = R.string.style_ink_color,
        assetPath = "style_transfer_style/ink_color.webp",
    ),

    Mosaic(
        titleId = R.string.style_mosaic,
        assetPath = "style_transfer_style/mosaic.webp",
    ),

    PopArt(
        titleId = R.string.style_pop_art,
        assetPath = "style_transfer_style/pop_art.webp",
    ),
}