package com.photo.editor.ui.screen.onboard.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.theme.HorizontalClipRect
import com.photo.editor.common.ui.theme.bold18
import com.photo.editor.common.ui.theme.medium24
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent

@Composable
fun OnboardPage3(
    onNextClick: () -> Unit = {},
) {
    val animationProgress by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        )
    )
    val anim1 = animationProgress.coerceIn(0f, 1f)
    val anim2 = (animationProgress - 1).coerceIn(0f, 1f)

    Box {
        Image(
            painter = painterResource(R.drawable.onboard_3_1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        AnimatedVisibility(
            visible = anim1 > 0f,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(500)),
        ) {
            Image(
                painter = painterResource(R.drawable.onboard_3_2),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(HorizontalClipRect(0f, anim1)),
                contentScale = ContentScale.Crop,
            )
        }

        AnimatedVisibility(
            visible = LocalInspectionMode.current || anim2 > 0f,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box {
                Image(
                    painter = painterResource(R.drawable.onboard_3_3),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                Text(
                    text = stringResource(R.string.onboard_2_1),
                    style = bold18,
                    color = Color.White,
                    modifier = Modifier.align(BiasAlignment(-0.65f, -0.5f))
                )

                Text(
                    text = stringResource(R.string.onboard_2_2),
                    style = bold18,
                    color = Color.White,
                    modifier = Modifier.align(BiasAlignment(0.85f, -0.15f))
                )
            }
        }

        Column(
            modifier = Modifier.align(BiasAlignment(0f, 0.5f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.onboard_2_3),
                style = medium24,
                color = Color.Black,
            )

            Text(
                text = stringResource(R.string.onboard_2_4),
                style = regular14,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            OnboardIndicator(
                pageIndex = 2,
                totalPage = 3,
                onNextClick = onNextClick,
                modifier = Modifier.padding(bottom = 12.dp),
                inactiveColor = Color.White
            )

            BaseBannerAdContent(AdsProvider.bannerOnboard2)
        }
    }
}

@Preview
@Composable
private fun Preview() {
    OnboardPage3()
}



