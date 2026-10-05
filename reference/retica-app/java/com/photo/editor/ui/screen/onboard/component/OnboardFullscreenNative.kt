package com.photo.editor.ui.screen.onboard.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import apero.aperosg.monetization.adgroup.NativeAdGroup
import apero.aperosg.monetization.databinding.ShimmerLayoutBinding
import apero.aperosg.monetization.ui.NativeAdContent
import com.photo.editor.R
import com.photo.editor.common.util.logEvent
import com.photo.editor.common.util.staticStatusBarPadding
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import kotlinx.coroutines.launch

@Composable
fun OnboardFullscreenNative(
    adGroup: NativeAdGroup,
    requestLayoutIndex: Int,
    pagerState: PagerState,
) {
    val coroutineScope = rememberCoroutineScope()
    var isFacebookMediation by remember { mutableStateOf(false) }
    val forceMetaLayout = RemoteConfig.adsLayoutConfig.force_meta_layout_onboard_fullscr
    val forceAdmobLayout = RemoteConfig.adsLayoutConfig.force_admob_layout_onboard_fullscr

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .staticStatusBarPadding()
    ) {
        if (adGroup.skipFlow.collectAsState().value) {
            AndroidViewBinding(
                factory = { inflater, parent, attach ->
                    val binding = ShimmerLayoutBinding.inflate(inflater, parent, attach)
                    binding.root.addView(inflater.inflate(R.layout.native_ads_fullscr, parent, attach))
                    binding
                },
                modifier = Modifier.fillMaxSize(),
            )

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = pagerState.currentPage + 1,
                            animationSpec = tween(500)
                        )
                    }
                },
                modifier = Modifier
                    .padding(end = 4.dp, top = 16.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF777777)
                )
            }
        } else {
            NativeAdContent(
                adGroup = adGroup,
                layoutId = R.layout.native_ads_fullscr,
                modifier = Modifier.fillMaxSize(),
                requestLayoutIndex = requestLayoutIndex,
                onFacebookMediationShowed = { isFacebookMediation = it },
                autoLoadAds = false,
            )

            if (!forceAdmobLayout) {
                if (isFacebookMediation || forceMetaLayout) {
                    IconButton(
                        onClick = {
                            logEvent("onboard_full_scr_close_click")
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage + 1,
                                    animationSpec = tween(500)
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(end = 4.dp, top = 16.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF777777)
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
    OnboardFullscreenNative(
        adGroup = AdsProvider.nativeFullScr,
        requestLayoutIndex = 0,
        pagerState = rememberPagerState { 1 }
    )
}