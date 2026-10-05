package com.photo.editor.ui.screen.edit.common

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import com.photo.editor.common.util.assetToBitmap
import com.photo.editor.overlaybrush.overlay.ImageOverlayState
import com.photo.editor.overlaybrush.overlay.OverlayState
import com.photo.editor.overlaybrush.overlay.textoverlay.common.TextOverlayState
import com.photo.editor.overlaybrush.overlay.textoverlay.common.TextOverlayData
import com.photo.editor.common.ui.transformableimage.TransformState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Stable
class EditImageOverlayState(
    val context: Context?,
    val transformState: TransformState,
) {
    private val _overlayStates = MutableStateFlow<List<OverlayState>>(listOf())
    val overlayStates = _overlayStates.asStateFlow()

    fun setOverlays(overlays: List<OverlayState>) = _overlayStates.update { overlays }

    var focusedOverlay: OverlayState? by mutableStateOf(null)
        private set

    var editingTextOverlay: TextOverlayState? by mutableStateOf(null)

    fun addTextOverlay() {
        val newTextOverlayData = TextOverlayState(context!!, transformState, TextOverlayData())
        _overlayStates.value += newTextOverlayData
        editingTextOverlay = newTextOverlayData
    }

    fun addImageOverlay(assetPath: String) {
        val imageOverlay = ImageOverlayState(
            transformState = transformState,
            overlayImage = assetToBitmap(context!!, assetPath)!!.asImageBitmap(),
        )
        _overlayStates.value += imageOverlay
        focusedOverlay = imageOverlay
    }

    fun removeOverlay(overlayState: OverlayState) {
        _overlayStates.value -= overlayState
        if (focusedOverlay == overlayState) {
            focusedOverlay = null
        }
    }

    fun removeLastImageOverlay() {
        // Remove last element of overlays that is sticker overlay
        val overlays = _overlayStates.value.toMutableList()
        val lastSticker = overlays.lastOrNull { it is ImageOverlayState }
        lastSticker?.let { overlays.remove(it) }
        _overlayStates.value = overlays
    }

    fun focusOverlayAndMoveOnTop(overlayState: OverlayState) {
        focusedOverlay = overlayState
        val overlays = _overlayStates.value.toMutableList()
        overlays.remove(overlayState)
        overlays.add(overlayState)
        _overlayStates.value = overlays
    }

    fun unFocusOverlay() {
        focusedOverlay = null
    }
}