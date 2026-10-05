package com.photo.editor.ui.screen.home.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.extraBold14
import com.photo.editor.common.util.logEvent
import com.photo.editor.ui.screen.home.HomeButtonModel
import com.photo.editor.ui.screen.home.HomeInteraction
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeButtons(
    interaction: HomeInteraction = HomeInteraction(),
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    var gridProgress by remember { mutableFloatStateOf(0f) }
    val currentIndicatorIndex = if (gridProgress < 0.8f) 0 else 1

    LaunchedEffect(Unit) {
        snapshotFlow {
            (gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.toFloat() ?: 0f) /
                    gridState.layoutInfo.totalItemsCount.coerceAtLeast(1)
        }.collectLatest {
            gridProgress = it
        }
    }


    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CenterRow(itemSpacing = 10.dp, modifier = Modifier.padding(horizontal = 10.dp)) {
            MajorButton(
                iconId = R.drawable.ic_edit_photo,
                titleId = R.string.edit_photo,
                foregroundColor = Color.Black,
                backgroundColor = Color.White,
                onClick = interaction.onEditClick,
                modifier = Modifier.weight(1f),
            )

            MajorButton(
                iconId = R.drawable.ic_camera,
                titleId = R.string.take_a_photo,
                foregroundColor = Color.White,
                backgroundColor = Color.Black,
                onClick = interaction.onCameraClick,
                modifier = Modifier.weight(1f),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(SubColor)
        ) {
            val buttonWidth = (maxWidth - 80.dp) / 3

            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                state = gridState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .fillMaxWidth()
                    .height(72.dp * 2 + 10.dp)
            ) {
                items(HomeButtonModel.entries) {
                    MinorButton(
                        iconId = it.iconId,
                        titleId = it.titleId,
                        onClick = {
                            interaction.onHomeButtonClick(it)
                        },
                        modifier = Modifier.width(buttonWidth),
                    )
                }
            }

            HomePagerIndicator(
                modifier = Modifier.align(Alignment.BottomCenter), currentIndicatorIndex
            )

        }
    }
}

@Composable
private fun HomePagerIndicator(modifier: Modifier, currentIndicatorIndex: Int) {
    CenterRow(
        modifier = modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .height(20.dp),

        itemSpacing = 8.dp
    ) {
        repeat(currentIndicatorIndex) {
            Spacer(
                modifier = Modifier
                    .size(5.dp)
                    .circle()
                    .background(Color.White)
            )
        }

        Spacer(
            modifier = Modifier
                .width(40.dp)
                .height(5.dp)
                .circle()
                .background(Color.Black)
        )

        repeat(1 - currentIndicatorIndex) {
            Spacer(
                modifier = Modifier
                    .size(5.dp)
                    .circle()
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun MajorButton(
    @DrawableRes iconId: Int,
    @StringRes titleId: Int,
    foregroundColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterColumn(
        modifier = modifier
            .height(80.dp)
            .rounded(5.dp)
            .background(backgroundColor)
            .clickable { onClick() }) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = stringResource(titleId),
            modifier = Modifier.size(32.dp),
            tint = foregroundColor,
        )

        Text(
            text = stringResource(titleId).uppercase(),
            style = extraBold14,
            color = foregroundColor,
            modifier = Modifier.basicMarquee(Int.MAX_VALUE)
        )
    }
}

@Composable
private fun MinorButton(
    @DrawableRes iconId: Int,
    @StringRes titleId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterColumn(
        modifier = modifier
            .height(72.dp)
            .rounded(5.dp)
            .background(Color.Transparent)
            .clickable { onClick() }
//            .padding(horizontal = 8.dp)
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = stringResource(titleId),
            modifier = Modifier.size(32.dp),
            tint = Color.Black,
        )

        Text(
            text = stringResource(titleId).uppercase(),
            style = extraBold14,
            color = Color.Black,
            modifier = Modifier
                .basicMarquee(Int.MAX_VALUE)
                .padding(top = 2.dp)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    HomeButtons()
}