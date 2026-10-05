package com.photo.editor.ui.screen.edit.component.bottombar.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageSubBottomBarTitle
import com.photo.editor.editimage.model.ImageFrame
import com.photo.editor.ui.component.shimmer.SimpleShimmer

@Composable
fun FrameBottomBar(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    val selectedFrame by uiState.frameFlow.collectAsState()

    Column(
        modifier = modifier
            .background(Color.White)
            .padding(top = 10.dp),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            items(ImageFrame.entries) { frame ->
                CenterBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .rounded(5.dp)
                        .clickable {
                            if (selectedFrame == frame) uiState.setFrame(null)
                            else uiState.setFrame(frame)
                        }
                ) {
                    SubcomposeAsyncImage(
                        model = getAssetAbsoluteFilePath(frame.previewAssetPath),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        loading = { SimpleShimmer() },
                        contentScale = ContentScale.FillBounds,
                    )

                    if (selectedFrame == frame) {
                        Spacer(
                            modifier = Modifier
                                .matchParentSize()
                                .background(AlternativeColor.copy(0.7f))
                        )

                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    }
                }
            }
        }

        EditImageSubBottomBarTitle(
            title = stringResource(R.string.frame),
            onConfirmClick = {
                uiState.saveState()
                uiState.selectedEditOption = null
            },
            onCancelClick = {
                uiState.restoreLastState()
                uiState.selectedEditOption = null
            },
        )
    }
}

@Preview
@Composable
private fun Preview() {
    FrameBottomBar(
        uiState = EditImageUiState()
    )
}