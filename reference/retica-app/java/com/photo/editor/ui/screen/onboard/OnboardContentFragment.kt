package com.photo.editor.ui.screen.onboard

import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import apero.aperosg.monetization.enums.AdStatus
import com.photo.editor.OnboardGraphDirections
import com.photo.editor.R
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.platform.app.App
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.onboard.component.OnboardFullscreenNative
import com.photo.editor.ui.screen.onboard.component.OnboardPage1
import com.photo.editor.ui.screen.onboard.component.OnboardPage2
import com.photo.editor.ui.screen.onboard.component.OnboardPage3
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.androidx.navigation.koinNavGraphViewModel
import kotlin.random.Random

class OnboardContentFragment : BaseComposeFragment() {
    private val viewModel: OnboardViewModel by koinNavGraphViewModel(R.id.onboardGraph)
    private var currentPageFlow = MutableStateFlow(0)
    private var experimentalFeature = false

    override fun onScreenView() {
        super.onScreenView()

        if (settingsRepository.experimentalFeature1) {
            logEvent("experimental_feature_1_already_done")
        } else {
            if (AdsProvider.experimentalFeatureEnabled) {
                logEvent("experimental_feature_1_hf")
                experimentalFeature = true

                val percentage = (RemoteConfig.commonConfig.experimental_feature_1 / 100f).coerceIn(0f, 1f)
                val roll = Random.nextFloat()
                if (roll <= percentage) {
                    logEvent("experimental_feature_1_lucky")
                } else {
                    logEvent("experimental_feature_1_unlucky")
                    experimentalFeature = false
                    settingsRepository.experimentalFeature1 = true
                }
            } else {
                logEvent("experimental_feature_1_no_hf")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fullscreenDisabled = !AdsProvider.nativeFullScr.enabled && !AdsProvider.nativeFullScr2.enabled

        // Collect onboard fullscreen status to reload fullscreen and fullscreen sec
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                currentPageFlow.collectLatest { page ->
                    if (!fullscreenDisabled) {
                        when (page) {
                            // Onboard 1
                            0 -> {
                                launch {
                                    AdsProvider.nativeOnboard.loadAds(App.instance)
                                    delay(200)
                                    AdsProvider.nativeFullScr.loadAds(App.instance)
                                }
                            }

                            // Full screen 1
                            1 -> {
                                if (experimentalFeature) {
                                    logEvent("experimental_feature_1_done")
                                    settingsRepository.experimentalFeature1 = true
                                    toast(R.string.experimental_feature_1_msg)
                                    requireActivity().finish()
                                } else {
                                    launch {
                                        AdsProvider.nativeOnboard.loadAds(App.instance)
                                        delay(200)
                                        AdsProvider.nativeFullScr.loadAds(App.instance)
                                        delay(200)
                                        AdsProvider.nativeFullScr2.loadAds(App.instance)
                                        delay(200)
                                        AdsProvider.bannerOnboard1.loadAds(App.instance)
                                    }
                                }
                            }

                            // Onboard 2
                            2 -> {
                                AdsProvider.nativeFullScr.loadAds(App.instance)
                                delay(200)
                                AdsProvider.nativeFullScr2.loadAds(App.instance)
                            }

                            // Full screen 2
                            3 -> {
                                AdsProvider.nativeFullScr2.loadAds(App.instance)
                                delay(200)
                                AdsProvider.bannerOnboard2.loadAds(App.instance)
                            }

                            // Onboard 3
                            4 -> {
                                AdsProvider.nativeFullScr2.loadAds(App.instance)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        AdsProvider.nativeFullScr.releaseAll()
        AdsProvider.nativeFullScr2.releaseAll()
        AdsProvider.nativeOnboard.releaseAll()
    }

    @Composable
    override fun ComposeView() {
        OnboardContentScreen(
            onPageChange = {
                currentPageFlow.value = it
            },
            onStartClick = {
                viewModel.setOnboarded()
                safeNavigate(OnboardGraphDirections.actionOnboardToHome())
            },
            onBackClick = {
                safePopBackstack()
            }
        )
    }
}

@Composable
private fun OnboardContentScreen(
    onPageChange: (Int) -> Unit = {},
    onStartClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    val fullscreenDisabled = !AdsProvider.nativeFullScr.enabled && !AdsProvider.nativeFullScr2.enabled

    val pagerState = rememberPagerState { if (fullscreenDisabled) 3 else 5 }
    var currentPage by remember { mutableIntStateOf(0) }
    var requestLayoutIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            onPageChange(it)
            currentPage = it
            requestLayoutIndex++

            if (fullscreenDisabled) {
                when (it) {
                    0 -> logEvent("onb_1_scr")
                    1 -> logEvent("onb_2_scr")
                    2 -> logEvent("onb_3_scr")
                }
            } else {
                when (it) {
                    0 -> logEvent("onb_1_scr")
                    1 -> logEvent("onboard_full_scr")
                    2 -> logEvent("onb_2_scr")
                    3 -> logEvent("onboard_full_scr_2")
                    4 -> logEvent("onb_3_scr")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            onPageChange(it)
            currentPage = it
            requestLayoutIndex++
        }
    }

    BackHandler {
        if (currentPage > 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    pagerState.currentPage - 1,
                    animationSpec = tween(500)
                )
            }
        } else {
            logEvent("onb_scr_back_click")
            onBackClick()
        }
    }


    val onboard1 = @Composable {
        OnboardPage1 {
            logEvent("onb_1_scr_next_click")
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = pagerState.currentPage + 1,
                    animationSpec = tween(500)
                )
            }
        }
    }

    val onboard2 = @Composable {
        OnboardPage2 {
            logEvent("onb_2_scr_next_click")
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = pagerState.currentPage + 1,
                    animationSpec = tween(500)
                )
            }
        }
    }

    val onboard3 = @Composable {
        OnboardPage3 {
            logEvent("onb_3_scr_next_click")
            onStartClick()
        }
    }

    val onboardFullscr1 = @Composable {
        OnboardFullscreenNative(
            adGroup = AdsProvider.nativeFullScr,
            requestLayoutIndex = requestLayoutIndex,
            pagerState = pagerState
        )
    }

    val onboardFullscr2 = @Composable {
        OnboardFullscreenNative(
            adGroup = AdsProvider.nativeFullScr2,
            requestLayoutIndex = requestLayoutIndex,
            pagerState = pagerState
        )
    }

    BaseScreen(
        backgroundColor = Color(0xFFF8F0E1)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(LocalConfiguration.current.screenWidthDp.dp - 1.dp),
            contentPadding = PaddingValues(horizontal = 0.5.dp),
            beyondViewportPageCount = 3
        ) { page ->
            if (fullscreenDisabled) {
                when (page) {
                    0 -> onboard1()
                    1 -> onboard2()
                    2 -> onboard3()
                }
            } else {
                when (page) {
                    0 -> onboard1()
                    1 -> onboardFullscr1()
                    2 -> onboard2()
                    3 -> onboardFullscr2()
                    4 -> onboard3()
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    OnboardContentScreen {}
}