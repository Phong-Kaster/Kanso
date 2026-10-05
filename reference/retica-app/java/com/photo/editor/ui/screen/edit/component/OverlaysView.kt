package com.photo.editor.ui.screen.edit.component

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.photo.editor.common.ui.transformableimage.TransformState
import com.photo.editor.overlaybrush.overlay.CombineOverlaySurface
import com.photo.editor.ui.screen.edit.common.EditImageOverlayState
import com.photo.editor.ui.screen.edit.common.EditImageUiState

@Composable
fun OverlaysView(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    val editOverlayState = uiState.overlayState

    CombineOverlaySurface(
        overlayStates = editOverlayState.overlayStates.collectAsState().value,
        transformState = uiState.transformState,
        focusedOverlay = editOverlayState.focusedOverlay,
        onFocusedOverlayChanged = {
            if (it == null) editOverlayState.unFocusOverlay()
            else editOverlayState.focusOverlayAndMoveOnTop(it)
        },
        onRemoveOverlay = { editOverlayState.removeOverlay(it) },
        editingTextOverlay = editOverlayState.editingTextOverlay,
        onEditingTextOverlayChange = { editOverlayState.editingTextOverlay = it },
        modifier = modifier.fillMaxSize()
    )
}

@Preview
@Composable
private fun Preview() {
    val context = LocalContext.current
    val surfaceSize by remember { mutableStateOf(Size(500f, 500f)) }
    val editOverlayState = remember {
        EditImageOverlayState(context, TransformState().apply { this.surfaceSize = surfaceSize }).apply {
            addTextOverlay()
            val overlay = overlayStates.value.first()
            focusOverlayAndMoveOnTop(overlay)
        }
    }

    OverlaysView(
        uiState = EditImageUiState().apply {
            overlayState.setOverlays(editOverlayState.overlayStates.value)
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    )
}