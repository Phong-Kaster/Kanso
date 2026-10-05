package com.photo.editor.ui.screen.enhance.common

import com.photo.editor.ui.screen.enhance.model.EnhanceSample

data class EnhanceEditInteraction(
    val onBackClick: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
)

data class EnhanceIntroInteraction(
    val onBackClick: () -> Unit = {},
    val onSampleClick: (EnhanceSample) -> Unit = {},
    val onUploadImageClick: () -> Unit = {},
)
