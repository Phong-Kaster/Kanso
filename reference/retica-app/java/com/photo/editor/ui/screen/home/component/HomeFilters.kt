package com.photo.editor.ui.screen.home.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.VerticalClipRect
import com.photo.editor.common.ui.theme.extraBold10
import com.photo.editor.common.ui.theme.extraBold16
import com.photo.editor.editimage.model.filter.FilterDirectPreset
import com.photo.editor.editimage.model.filter.FilterPreset

private enum class HomeFilterItem(
    val nameId: Int,
    val beforeImageId: Int,
    val afterImageId: Int,
    val filterPreset: FilterPreset,
) {
    DreamyGlow(
        nameId = FilterDirectPreset.Amaro.nameId,
        beforeImageId = R.drawable.filter_dreamy_glow_before,
        afterImageId = R.drawable.filter_dreamy_glow_after,
        filterPreset = FilterDirectPreset.Amaro,
    ),
    MorningDew(
        nameId = FilterDirectPreset.EarlyBird.nameId,
        beforeImageId = R.drawable.filter_morning_dew_before,
        afterImageId = R.drawable.filter_morning_dew_after,
        filterPreset = FilterDirectPreset.EarlyBird,
    ),
    SunsetWarmth(
        nameId = FilterDirectPreset.Kelvin.nameId,
        beforeImageId = R.drawable.filter_sunset_warmth_before,
        afterImageId = R.drawable.filter_sunset_warmth_after,
        filterPreset = FilterDirectPreset.Kelvin,
    ),
    UrbanEdge(
        nameId = FilterDirectPreset.Brannan.nameId,
        beforeImageId = R.drawable.filter_urban_edge_before,
        afterImageId = R.drawable.filter_urban_edge_after,
        filterPreset = FilterDirectPreset.Brannan,
    ),
    FoggyEvening(
        nameId = FilterDirectPreset.Sutro.nameId,
        beforeImageId = R.drawable.filter_foggy_evening_before,
        afterImageId = R.drawable.filter_foggy_evening_after,
        filterPreset = FilterDirectPreset.Sutro,
    )
}

private val DividerGradient = Brush.verticalGradient(
    0f to Color.Transparent,
    0.5f to Color.White,
    1f to Color.Transparent,
)

@Composable
fun HomeFilters(
    onItemClick: (FilterPreset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animationProgress by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CenterRow(
            modifier = Modifier
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.retro_filter).uppercase(),
                style = extraBold16,
                color = Color.Black,
                modifier = Modifier.weight(1f),
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            items(HomeFilterItem.entries) {
                FilterItem(
                    filterItem = it,
                    onClick = { onItemClick(it.filterPreset) },
                    animationProgress = animationProgress,
                )
            }
        }
    }
}

@Composable
private fun FilterItem(
    filterItem: HomeFilterItem,
    onClick: () -> Unit,
    animationProgress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(140.dp)
            .height(180.dp)
            .rounded(5.dp)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(filterItem.beforeImageId),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .clip(VerticalClipRect(0f, animationProgress)),
            contentScale = ContentScale.Crop,
        )

        Image(
            painter = painterResource(filterItem.afterImageId),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .clip(VerticalClipRect(animationProgress, 1f)),
            contentScale = ContentScale.Crop,
        )

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(DividerGradient)
                .align(BiasAlignment(animationProgress * 2 - 1f, 0f))
        )

        Text(
            text = stringResource(filterItem.nameId).uppercase(),
            style = extraBold10,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(6.dp)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    HomeFilters()
}