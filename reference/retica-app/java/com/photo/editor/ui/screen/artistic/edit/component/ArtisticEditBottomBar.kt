package com.photo.editor.ui.screen.artistic.edit.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.regular12
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.ui.component.shimmer.SimpleShimmer
import com.photo.editor.ui.screen.artistic.common.ArtisticEditInteraction
import com.photo.editor.ui.screen.artistic.common.ArtisticUiState
import com.photo.editor.ui.screen.artistic.model.ArtisticStyle

@Composable
fun ArtisticEditBottomBar(
    modifier: Modifier = Modifier,
    uiState: ArtisticUiState,
    interaction: ArtisticEditInteraction,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        AnimatedVisibility(uiState.selectedStyle != null) {
            ArtisticEditValueSlider(
                value = uiState.blendRatio,
                onValueChange = {
                    if (uiState.selectedStyle == null) return@ArtisticEditValueSlider
                    interaction.onBlendRatioChange(it)
                },
                title = stringResource(R.string.blend_ratio),
            )
        }

        LazyRow(
            modifier = Modifier
                .height(104.dp)
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 10.dp, bottom = 10.dp),
            state = rememberLazyListState(
                ArtisticStyle.entries
                    .indexOf(uiState.selectedStyle)
                    .coerceAtLeast(0)
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(84.dp)
                        .rounded(5.dp)
                        .clickable {
                            interaction.onStyleClick(null)
                        }
                        .then(
                            if (uiState.selectedStyle == null) Modifier.border(
                                2.dp,
                                Color.Black,
                                RoundedCornerShape(5.dp)
                            )
                            else Modifier.border(
                                0.5.dp,
                                Color.Transparent,
                                RoundedCornerShape(5.dp)
                            )
                        )
                        .background(AlternativeColor)
                ) {
                    SubcomposeAsyncImage(
                        model = R.drawable.empty_slash,
                        loading = { SimpleShimmer() },
                        contentDescription = "empty_slash",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )

                    CenterBox(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.none),
                            style = regular12,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(Int.MAX_VALUE)
                        )
                    }
                }

            }

            items(ArtisticStyle.entries) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(84.dp)
                        .rounded(5.dp)
                        .clickable {
                            if (it != uiState.selectedStyle) interaction.onBlendRatioChange(0f)
                            interaction.onStyleClick(it)
                        }
                        .then(
                            if (uiState.selectedStyle == it) Modifier.border(
                                2.dp,
                                Color.Black,
                                RoundedCornerShape(5.dp)
                            )
                            else Modifier.border(
                                0.5.dp,
                                Color.Transparent,
                                RoundedCornerShape(5.dp)
                            )
                        )

                ) {
                    SubcomposeAsyncImage(
                        model = getAssetAbsoluteFilePath(it.assetPath),
                        loading = { SimpleShimmer() },
                        contentDescription = stringResource(it.titleId),
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )

                    CenterBox(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = stringResource(it.titleId),
                            style = regular12,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(Int.MAX_VALUE)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ArtisticEditBottomBar(
        Modifier,
        uiState = ArtisticUiState(),
        interaction = ArtisticEditInteraction()
    )
}