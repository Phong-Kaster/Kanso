package com.photo.editor.ui.screen.artistic.common

import com.photo.editor.ui.screen.artistic.model.ArtisticStyle


data class ArtisticIntroInteraction(
    val onBackClick: () -> Unit = {},
    val onGalleryClick: () -> Unit = {},
    val onStyleSelected: (ArtisticStyle) -> Unit = {},
)


data class ArtisticEditInteraction(
    val onBackClick: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
    val onBlendRatioChange: (Float) -> Unit = {},
    val onStyleClick: (ArtisticStyle?) -> Unit = {},
)
