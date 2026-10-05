package com.photo.editor.ui.screen.edit.component.bottombar.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.editimage.model.StickerCategory
import com.photo.editor.ui.component.shimmer.SimpleShimmer
import com.photo.editor.ui.screen.edit.common.EditImageUiState

@Composable
fun StickerBottomBar(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    var category by rememberSaveable { mutableStateOf(StickerCategory.entries.first()) }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(top = 12.dp)
        ) {
            key(category) {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .fillMaxWidth()
                        .height(62.dp * 2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    itemsIndexed(category.stickerAssets) { _, item ->
                        SubcomposeAsyncImage(
                            model = getAssetAbsoluteFilePath(item),
                            contentDescription = null,
                            loading = { SimpleShimmer() },
                            modifier = Modifier
                                .width(60.dp)
                                .rounded(5.dp)
                                .clickable {
                                    uiState.overlayState.addImageOverlay(item)
                                }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.height(50.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CenterBox(
                    modifier = Modifier
                        .size(50.dp)
                        .rippleClickable(25.dp) {
                            uiState.saveState()
                            uiState.selectedEditOption = null
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                        tint = Color.Black,
                        modifier = Modifier
                            .size(32.dp),
                    )
                }

                LazyRow(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(StickerCategory.entries) {
                        Column(
                            modifier = Modifier
                                .height(50.dp)
                                .width(IntrinsicSize.Min)
                                .rounded(4.dp)
                                .clickable {
                                    category = it
                                }
                        ) {
                            CenterBox(
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                SubcomposeAsyncImage(
                                    model = getAssetAbsoluteFilePath(it.iconAssetPath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(60.dp)
                                        .fillMaxHeight(),
                                    loading = { SimpleShimmer() }
                                )
                            }

                            Spacer(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (category == it) Color.Black else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    StickerBottomBar(
        uiState = EditImageUiState(),
    )
}