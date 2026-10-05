package com.photo.editor.ui.screen.camera

data class CameraInteraction(
    val onBackClick: () -> Unit = {},
    val onCaptureClick: () -> Unit = {},
    val onLibraryClick: () -> Unit = {},
    val onCameraZoomChange: (Float) -> Unit = {},
    val onSwitchCameraClick: () -> Unit = {},
    val onConfirmCapturedImage: () -> Unit = {},
    val onDismissCapturedImage: () -> Unit = {},
)
