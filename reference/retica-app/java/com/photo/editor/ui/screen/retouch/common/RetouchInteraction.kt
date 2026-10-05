package com.photo.editor.ui.screen.retouch.common

import androidx.compose.ui.geometry.Offset
import com.photo.editor.ui.screen.retouch.model.RetouchMode

data class RetouchEditInteraction(
    val onBackClick: () -> Unit = {},
    val onSaveClick: () -> Unit = {},

    val onRetouchPointAdded: (Offset) -> Unit = {},
    val onRetouchDrawFinished: () -> Unit = {},
    val onRetouchModeChanged: (RetouchMode) -> Unit = {},
    val onBrushSizeChanged: (Float) -> Unit = {},
    val onUndoClick: () -> Unit = {},
    val onRedoClick: () -> Unit = {},
)

data class RetouchIntroInteraction(
    val onBackClick: () -> Unit = {},
    val onGalleryClick: () -> Unit = {},
    val onSampleClick: (Int) -> Unit = {},
)

