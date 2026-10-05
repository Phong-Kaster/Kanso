package com.photo.editor.ui.screen.enhance.common

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.createBitmap

@Stable
class EnhanceUiState {
    var loading: Boolean by mutableStateOf(false)
    var failure: Boolean by mutableStateOf(false)
    var saving: Boolean by mutableStateOf(false)

    var enhanceProgress by mutableFloatStateOf(0f)

    var scaledOriginalImage by mutableStateOf(createBitmap(1, 1))
    var scaledEnhancedImage by mutableStateOf(createBitmap(1, 1))

    var exportedImage: Uri by mutableStateOf(Uri.EMPTY)

    var showOriginalImage: Boolean by mutableStateOf(false)
}
