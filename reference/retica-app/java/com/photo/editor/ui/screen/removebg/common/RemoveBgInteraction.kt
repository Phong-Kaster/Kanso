package com.photo.editor.ui.screen.removebg.common

data class RemoveBgEditInteraction(
    val onBackClick: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
)

data class RemoveBgIntroInteraction(
    val onBackClick: () -> Unit = {},
    val onGalleryClick: () -> Unit = {},
    val onSampleClick: (Int) -> Unit = {},
)

