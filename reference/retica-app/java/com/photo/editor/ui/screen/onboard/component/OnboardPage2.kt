package com.photo.editor.ui.screen.onboard.component

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.theme.GradientOb2
import com.photo.editor.common.ui.theme.medium24
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun OnboardPage2(
    onNextClick: () -> Unit = {},
) {
    var anim1 by remember { mutableFloatStateOf(0f) }
    var anim2 by remember { mutableFloatStateOf(0f) }
    var anim3 by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            anim1 = 0f
            anim2 = 0f
            anim3 = 1f

            val job1 = launch {
                animate(0f, 1f, animationSpec = tween(1500, easing = FastOutLinearInEasing)) { value, _ ->
                    anim1 = value
                }
            }

            val job2 = launch {
                delay(1000)
                animate(0f, 1f, animationSpec = tween(1000, easing = LinearEasing)) { value, _ ->
                    anim2 = value
                }
                animate(1f, 0f, animationSpec = tween(500, easing = LinearEasing)) { value, _ ->
                    anim2 = value
                    anim3 = value
                }
            }

            job1.join()
            job2.join()

            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.onboard_2_4),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Image(
            painter = painterResource(R.drawable.onboard_2_1),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = anim3 },
        )

        if (anim2 > 0 && anim2 <= 1f) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFD8258).copy(0.45f * anim2))
            )
        }

        Image(
            painter = painterResource(R.drawable.onboard_2_2),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )

        if (anim1 > 0f && anim1 < 1f) {
            AsyncImage(
                model = R.drawable.onboard_2_3,
                contentScale = ContentScale.FillHeight,
                clipToBounds = false,
                alignment = Alignment.Center,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer {
                        alpha = 0.7f
                        translationY = (anim1 * 2 - 1) * size.height
                    }
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(GradientOb2)
                .align(Alignment.BottomCenter)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.onboard_2_5),
                    style = medium24,
                    color = Color.White,
                )

                Text(
                    text = stringResource(R.string.onboard_2_6),
                    style = regular14,
                    color = Color.Black,
                )
            }

            OnboardIndicator(
                pageIndex = 1,
                totalPage = 3,
                onNextClick = onNextClick,
                modifier = Modifier
                    .padding(bottom = 12.dp, top = 24.dp),
                inactiveColor = Color.White
            )

            BaseBannerAdContent(AdsProvider.bannerOnboard1)
        }
    }
}

@Preview
@Composable
private fun Preview() {
    OnboardPage2()
}