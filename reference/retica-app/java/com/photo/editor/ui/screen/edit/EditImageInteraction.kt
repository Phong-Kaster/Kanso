package com.photo.editor.ui.screen.edit

data class EditImageInteraction(
    val onBackClick: () -> Unit = {},
    val onSaveClick: () -> Unit = {},

)
