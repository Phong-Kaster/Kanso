package com.photo.editor.ui.component.bottomsheet

import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.photo.editor.common.ui.bottomsheet.BaseBottomSheet
import com.photo.editor.ui.screen.gallery.GalleryScreen
import com.photo.gallery.model.GalleryBucket
import com.photo.gallery.model.GalleryImage


@Composable
fun GalleryBottomSheet(
    showBottomSheet: Boolean,
    onDismissRequest: () -> Unit,
    bucketsProvider: () -> List<GalleryBucket>,
    imagesProvider: () -> List<GalleryImage>,
    onImageSelected: (GalleryImage) -> Unit,
    showCaptureButton: Boolean = false,
    onCaptureClick: () -> Unit = {},
) {
    BaseBottomSheet(
        showBottomSheet = showBottomSheet,
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        showHandler = false,
    ) {
        GalleryScreen(
            buckets = bucketsProvider(),
            images = imagesProvider(),
            onImageClick = onImageSelected,
            isSubScreen = true,
            onBackClick = { onDismissRequest() },
//            showCaptureButton = showCaptureButton,
//            onCaptureClick = onCaptureClick,
        )
    }
}