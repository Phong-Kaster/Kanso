package com.photo.editor.ui.screen.home

import com.photo.editor.editimage.model.ImageFrame
import com.photo.editor.editimage.model.filter.FilterPreset

data class HomeInteraction(
    val onSettingsClick: () -> Unit = {},

    val onEditClick: () -> Unit = {},
    val onCameraClick: () -> Unit = {},

    val onHomeButtonClick: (HomeButtonModel) -> Unit = {},

    val onFilterItemClick: (FilterPreset) -> Unit = {},
    val onFrameItemClick: (ImageFrame) -> Unit = {},
)
