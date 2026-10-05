package com.photo.editor.ui.screen.retouch.common

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.photo.editor.common.ui.transformableimage.ImageState
import com.photo.editor.domain.datastructure.Memento
import com.photo.editor.ui.screen.retouch.edit.component.drawer.RetouchDrawingState
import com.photo.editor.ui.screen.retouch.edit.component.toucher.RetouchTouchState
import com.photo.editor.ui.screen.retouch.model.RetouchMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
class RetouchUiState(
) {
    var originalImage by mutableStateOf(createBitmap(1, 1))

    var currentImage by mutableStateOf(createBitmap(1, 1))

    var exportedImage: Uri by mutableStateOf(Uri.EMPTY)

    var showOriginalImage: Boolean by mutableStateOf(false)

    var loading: Boolean by mutableStateOf(false)
    var failure: Boolean by mutableStateOf(false)
    var saving: Boolean by mutableStateOf(false)

    var retouchMode: RetouchMode by mutableStateOf(RetouchMode.Draw)

    var brushSize: Float by mutableFloatStateOf(30f)

    var canUndo: Boolean by mutableStateOf(false)

    var canRedo: Boolean by mutableStateOf(false)


    fun reset(){
        originalImage = createBitmap(1, 1)
        currentImage = createBitmap(1, 1)
        exportedImage = Uri.EMPTY
        showOriginalImage = false
        loading = false
        failure = false
        saving = false
        retouchMode = RetouchMode.Draw
        brushSize = 30f
        canUndo = false
        canRedo = false
    }
}