package com.photo.editor.ui.screen.home.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.util.staticStatusBarPadding
import com.photo.editor.ui.component.topbar.BaseTopBar
import com.photo.editor.ui.screen.home.HomeButtonModel
import com.photo.editor.ui.screen.home.HomeInteraction
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun HomePager(
    interaction: HomeInteraction = HomeInteraction(),
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(1000) { 10000 }
    val isDragging = pagerState.interactionSource.collectIsDraggedAsState().value

    LaunchedEffect(isDragging) {
        if (isDragging) return@LaunchedEffect

        while (isActive) {
            delay(3000)
            pagerState.animateScrollToPage(
                page = pagerState.currentPage + 1,
                animationSpec = tween(1000)
            )
        }
    }

    Box(modifier = modifier.aspectRatio(0.55f)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
                .aspectRatio(0.7f)
        ) { index ->
            val page = index % 3

            when (page) {
                0 -> HomePagerPage1(
                    modifier = Modifier
                        .fillMaxSize()
                        .silentClickable { interaction.onHomeButtonClick(HomeButtonModel.Makeup) }
                )

//                1 -> HomePagerPage2(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .silentClickable { interaction.onHomeButtonClick(HomeButtonModel.Filter) }
//                )

                1 -> HomePagerPage3(
                    modifier = Modifier
                        .fillMaxSize()
                        .silentClickable { interaction.onHomeButtonClick(HomeButtonModel.Enhance) }
                )

                2 -> HomePagerPage4(
                    modifier = Modifier
                        .fillMaxSize()
                        .silentClickable { interaction.onHomeButtonClick(HomeButtonModel.Sticker) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(10.dp)
            ) {
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) },
                ) { index ->
                    val page = index % 3

                    when (page) {
                        0 -> HomePagerPage1Text()
//                        1 -> HomePagerPage2Text()
                        1 -> HomePagerPage3Text()
                        2 -> HomePagerPage4Text()
                    }
                }
            }

            HomePagerIndicator(
                pageIndex = pagerState.currentPage % 3,
                totalPages = 3,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 6.dp, start = 16.dp, end = 16.dp)
            )

            HomeButtons(
                interaction = interaction,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }


        CenterBox(
            modifier = Modifier
                .staticStatusBarPadding()
                .align(Alignment.TopEnd)
                .padding(end = 16.dp)
                .size(32.dp)
                .rippleClickable { interaction.onSettingsClick() }

        ) {
            Image(
                painter = painterResource(R.drawable.ic_settings_shadow),
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier.size(32.dp),
            )
        }

        BaseTopBar {}
    }
}

@Preview
@Composable
private fun Preview() {
    HomePager()
}