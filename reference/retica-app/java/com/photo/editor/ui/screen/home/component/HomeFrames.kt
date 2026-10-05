package com.photo.editor.ui.screen.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.extraBold16
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.editimage.model.ImageFrame
import com.photo.editor.ui.component.shimmer.SimpleShimmer

@Composable
fun HomeFrames(
    onItemClick: (ImageFrame) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.frame).uppercase(),
            style = extraBold16,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            items(ImageFrame.entries) { frame ->
                CenterBox(
                    modifier = Modifier
                        .height(180.dp)
                        .aspectRatio(0.75f)
                        .rounded(5.dp)
                        .clickable {
                            onItemClick(frame)
                        }
                ) {
                    SubcomposeAsyncImage(
                        model = getAssetAbsoluteFilePath(frame.previewAssetPath),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        loading = { SimpleShimmer() },
                        contentScale = ContentScale.FillBounds,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    HomeFrames()
}