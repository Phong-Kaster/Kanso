package com.photo.editor.ui.screen.artistic.common

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.createBitmap
import com.photo.editor.ui.screen.artistic.model.ArtisticStyle

@Stable
class ArtisticUiState {
    var originalBitmap by mutableStateOf(createBitmap(1, 1))
    var generatedBitmap by mutableStateOf(createBitmap(1, 1))
    var selectedStyle: ArtisticStyle? by mutableStateOf(ArtisticStyle.entries.first())
    var blendRatio by mutableFloatStateOf(0f)
    var showOriginalImage: Boolean by mutableStateOf(false)
    var processing: Boolean by mutableStateOf(false)
    var exportedImage: Uri by mutableStateOf(Uri.EMPTY)
}