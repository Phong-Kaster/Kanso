package com.photo.editor.ui.screen.removebg.common

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.createBitmap

@Stable
class RemoveBgUiState {
    var originalImage by mutableStateOf(createBitmap(1, 1))
    var currentImage by mutableStateOf(createBitmap(1, 1))
    var showOriginalImage: Boolean by mutableStateOf(false)
    var exportedImage: Uri by mutableStateOf(Uri.EMPTY)
    var loading: Boolean by mutableStateOf(false)
    var failure: Boolean by mutableStateOf(false)
    var saving: Boolean by mutableStateOf(false)

    var background: RemoveBGBackground by mutableStateOf(RemoveBGBackground.SolidColor(0))
}