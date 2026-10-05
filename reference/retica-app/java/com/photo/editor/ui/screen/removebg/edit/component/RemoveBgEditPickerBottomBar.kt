package com.photo.editor.ui.screen.removebg.edit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.bottomsheet.BaseBottomSheet
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.medium16
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.ui.component.bottomsheet.GalleryBottomSheet
import com.photo.editor.ui.component.shimmer.SimpleShimmer
import com.photo.editor.ui.screen.removebg.model.RemoveBackgroundImage
import com.photo.editor.ui.screen.removebg.model.RemoveBackgroundScene
import com.photo.gallery.model.GalleryBucket
import com.photo.gallery.model.GalleryImage

@Composable
fun RemoveBgEditPickerBottomBar(
    showBottomSheet: Boolean,
    onDismissRequest: () -> Unit,
    bucketsProvider: () -> List<GalleryBucket>,
    imagesProvider: () -> List<GalleryImage>,
    onImageSelected: (GalleryImage) -> Unit,
    onSceneClick: (RemoveBackgroundScene) -> Unit,
    onBackgroundClick: (RemoveBackgroundImage) -> Unit,
) {
    BaseBottomSheet(
        showBottomSheet = showBottomSheet,
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        var showGalleryBottomSheet by remember { mutableStateOf(false) }

        BGImagePickerBottomSheetContent(
            onLibraryClick = { showGalleryBottomSheet = true },
            onSceneClick = onSceneClick,
            onBackgroundClick = onBackgroundClick
        )

        GalleryBottomSheet(
            showBottomSheet = showGalleryBottomSheet,
            onDismissRequest = { showGalleryBottomSheet = false },
            bucketsProvider = bucketsProvider,
            imagesProvider = imagesProvider,
            onImageSelected = {
                onImageSelected(it)
                showGalleryBottomSheet = false
            },
        )
    }
}

@Composable
private fun BGImagePickerBottomSheetContent(
    onLibraryClick: () -> Unit,
    onSceneClick: (RemoveBackgroundScene) -> Unit,
    onBackgroundClick: (RemoveBackgroundImage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.select_image),
            style = medium16,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        CenterColumn(
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(69.dp)
                .rounded(5.dp)
                .clickable { onLibraryClick() }
                .background(SubColor),
            itemSpacing = 6.dp
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gallery),
                contentDescription = stringResource(R.string.library),
                tint = Color(0xFF284C58),
                modifier = Modifier.size(24.dp),
            )

            Text(
                text = stringResource(R.string.library),
                style = regular14,
                color = Color(0xFF284C58),
            )
        }


        Text(
            text = stringResource(R.string.scene),
            style = medium16,
            color = Color.Black,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp)
        )

        LazyRow(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(RemoveBackgroundScene.entries) {
                SubcomposeAsyncImage(
                    model = it.imageId,
                    contentDescription = it.name,
                    loading = { SimpleShimmer() },
                    modifier = Modifier
                        .size(72.dp)
                        .rounded(5.dp)
                        .clickable {
                            onSceneClick(it)
                        },
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Text(
            text = stringResource(R.string.background),
            style = medium16,
            color = Color.Black,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp)
        )

        LazyRow(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(RemoveBackgroundImage.entries) {
                SubcomposeAsyncImage(
                    model = it.imageId,
                    loading = { SimpleShimmer() },
                    contentDescription = it.name,
                    modifier = Modifier
                        .size(72.dp)
                        .rounded(5.dp)
                        .clickable {
                            onBackgroundClick(it)
                        },
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    BGImagePickerBottomSheetContent({}, {}, {})
}