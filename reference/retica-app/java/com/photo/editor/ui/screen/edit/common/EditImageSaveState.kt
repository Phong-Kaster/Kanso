package com.photo.editor.ui.screen.edit.common

import com.photo.editor.overlaybrush.brush.BrushSaveState
import com.photo.editor.editimage.model.EditImageData

data class EditImageSaveState(
    val editData: EditImageData,
    val overlaySaveStates: List<Any>,
    val brushSaveState: BrushSaveState,
)
