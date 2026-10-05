package com.photo.editor.ui.screen.onboard.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import apero.aperosg.monetization.ui.NativeAdContent
import com.photo.editor.R
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.medium20
import com.photo.editor.common.ui.theme.medium24
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.ui.component.topbar.BaseTopBar

@Composable
fun OnboardPage1(
    onNextClick: () -> Unit = {},
) {
    val animationProgress by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse,
        )
    )

    val rotation = lerp(-8f, 8f, animationProgress)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.onboard_1_background),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            BaseTopBar { }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.onboard_1_image),
                    contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .rotate(rotation)
                )

                Text(
                    text = stringResource(R.string.onboard_1_1),
                    style = medium24,
                    color = Color.Black,
                )

                Text(
                    text = stringResource(R.string.onboard_1_2),
                    style = regular14,
                    color = Color.Black,
                )
            }

            OnboardIndicator(
                pageIndex = 0,
                totalPage = 3,
                onNextClick = onNextClick,
            )

            val baseAdmobLayoutId = if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_onboard) R.layout.native_ads_large_above
            else R.layout.native_ads_large
            val baseMetaLayoutId = if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_onboard) R.layout.native_ads_large_meta_above
            else R.layout.native_ads_large_meta

            val layoutId = if (RemoteConfig.adsLayoutConfig.force_meta_layout_onboard) baseMetaLayoutId else baseAdmobLayoutId
            val metaLayoutId = if (RemoteConfig.adsLayoutConfig.force_admob_layout_onboard) baseAdmobLayoutId else baseMetaLayoutId

            NativeAdContent(
                adGroup = AdsProvider.nativeOnboard,
                layoutId = layoutId,
                metaLayoutId = metaLayoutId,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlternativeColor, rounded(10.dp))
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    OnboardPage1()
}