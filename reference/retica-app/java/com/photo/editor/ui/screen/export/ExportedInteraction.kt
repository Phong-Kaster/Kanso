package com.photo.editor.ui.screen.export

import com.photo.editor.domain.model.ShareTarget


data class ExportedInteraction(
    val onBackClick: () -> Unit = {},
    val onHomeClick: () -> Unit = {},
    val onEditAnotherClick: () -> Unit = {},
    val onEditMoreClick: () -> Unit = {},
    val onShareClick: (ShareTarget?) -> Unit = {},
)
