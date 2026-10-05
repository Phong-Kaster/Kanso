package com.photo.editor.ui.screen.splash

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import apero.aperosg.monetization.adgroup.AdUnitGroup
import apero.aperosg.monetization.adgroup.InterstitialAdGroup
import apero.aperosg.monetization.enums.AdStatus
import apero.aperosg.monetization.ui.BannerAdContent
import apero.aperosg.monetization.ui.LoadingAdsShimmer
import com.ads.control.admob.AdsConsentManager
import com.ads.control.ads.AperoAd
import com.photo.editor.R
import com.photo.editor.common.GlobalVariables
import com.photo.editor.common.NetworkManager
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.PetitFormalScriptFontFamily
import com.photo.editor.common.ui.theme.appTextStyle
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.common.ui.theme.regular12
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.AdsConfig
import com.photo.editor.data.remoteconfig.CommonConfig
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.NativeSplashDialog
import com.photo.editor.platform.app.App
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SplashFragment : BaseComposeFragment() {
    private val args: SplashFragmentArgs by navArgs()

    // Lock to prevent calling splash complete more than once
    private val isSplashCompleted = AtomicBoolean(false)
    // Lock to prevent calling show inter_splash more than once
    private val isInterSplashShown = AtomicBoolean(false)

    // Flag to indicate whether to show banner
    private var showBanner: Boolean by mutableStateOf(false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Track event based on origin
        logEvent("splash_scr") {
            param("app_open_origin", args.origin)
        }

        GlobalVariables.resetSession()
        AdsProvider.disableAppResume()

        lifecycleScope.launch {
            // Wait for application to initialize
            App.instance.appInitFinishedFlow.filter { it }.first()
            // Wait for ads config initialized
            AdsProvider.adsConfigSet.filter { it }.first()

            startSplashFlow()
        }
    }

    private suspend fun startSplashFlow() {
        // Log event
        if (!NetworkManager.isNetworkConnected) {
            logEvent("splash_scr_no_internet")
        }

        // Wait for network to be connected
        if (RemoteConfig.commonConfig.no_internet_dialog) {
            NetworkManager.networkConnectedFlow.filter { it }.first()
        } else if (!NetworkManager.isNetworkConnected) {
            onSplashComplete()
            return
        }

        loadSplashAds()

        val adsConsentManager = AdsConsentManager(requireActivity())
        adsConsentManager.requestUMP(
            true, "DF966CE93E04B2AACAB39705598718A9", false
        ) { canRequestAds ->
            if (canRequestAds) {
                AperoAd.getInstance().initAdsNetwork()
                showBanner = true
                registerSplashAdsObserver()
            } else {
                AdsProvider.config = AdsConfig.disableAllAdsConfig
                onSplashComplete()
            }
        }
    }

    private fun loadSplashAds() {
        // Reset splash ads status (if needed)
        AdsProvider.interSplash.releaseAll()
        AdsProvider.bannerSplash.releaseAll()

        // Start loading ads
        AdsProvider.bannerSplash.loadAds(requireActivity())
        AdsProvider.interSplash.loadAds(requireActivity())

        // Preload native ads so that it can show under inter splash when inter splash shows
        loadNextScreenAds()
        registerSplashAdsObserver()
    }

    private fun loadNextScreenAds() {
        if (!settingsRepository.isOnboarded()) {
            GlobalScope.launch {
                AdsProvider.nativeLanguage.loadAds(App.instance)
                delay(1000)
                AdsProvider.nativeLanguageDup.loadIndexes(App.instance, 0)
            }
        }
    }

    private fun registerSplashAdsObserver() {
        if (!AdsProvider.interSplash.enabled) {
            onSplashComplete()
            return
        }
        observeOnlyInter()
    }

    private fun observeOnlyInter() {
        if (view == null) return

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(
                    AdsProvider.interSplash.statusFlow,
                    AdsProvider.bannerSplash.statusFlow,
                    NetworkManager.networkConnectedFlow,
                ) { interSplashStatus, bannerStatus, networkConnected ->
                    val isBannerFinished = bannerStatus == AdStatus.Shown || bannerStatus == AdStatus.Failure || bannerStatus == AdStatus.None
                    val interSplashLoading = interSplashStatus == AdStatus.Loading || interSplashStatus == AdStatus.None

                    // Do nothing if inter_splash is still loading
                    if (interSplashLoading) return@combine

                    if (interSplashStatus == AdStatus.Ready) {
                        showSplashAds(AdsProvider.interSplash, isBannerFinished)
                    }
                    // If inter_splash failed to load, finish screen
                    else if (interSplashStatus == AdStatus.Failure) {
                        showSplashAds(null, isBannerFinished)
                    }
                }.collectLatest {  }
            }
        }
    }

    private suspend fun showSplashAds(adToShow: InterstitialAdGroup?, isBannerFinished: Boolean) {
        if (isInterSplashShown.getAndSet(true)) return

        if (adToShow != null) {
            // Delay additional time to wait for banner if it hasn't done loading
            if (!isBannerFinished) delay(5000)
            delay(500)
            showInterSplash(adToShow)
        } else {
            onSplashComplete()
        }
    }

    private fun showInterSplash(adToShow: InterstitialAdGroup) {
        adToShow.showAds(
            activity = requireActivity(),
            onAdImpression = { _, adName ->
                AdsProvider.interSplashShown = true
                if (adName != "inter_splash") {
                    AdsProvider.experimentalFeatureEnabled = true
                }
            },
            onAdShown = { _, _ ->
                // Preload native_language_dup_high_2
                AdsProvider.interSplashShowing = true
                GlobalScope.launch {
                    // Do not preload native_language_dup if already onboarded
                    if (settingsRepository.isOnboarded()) return@launch

                    // Wait until native_language_dup has done loading
                    AdsProvider.nativeLanguageDup.statusFlow.filter { it != AdStatus.Loading }.first()

                    // Do not proceed if inter_splash is closed
                    if (!AdsProvider.interSplashShowing) return@launch

                    // Do not proceed if native_language_dup loaded
                    if (AdsProvider.nativeLanguageDup.isAdReady()) return@launch

                    AdsProvider.nativeLanguageDup.loadIndexes(App.instance, 1)
                }
            },
            onNextAction = {
                onSplashComplete()
            },
        )
    }

    private fun onSplashComplete(force: Boolean = false) {
        if (!isSplashCompleted.getAndSet(true) || force) {
            if (!settingsRepository.isOnboarded()) {
                safeNavigate(SplashFragmentDirections.actionSplashToOnboard())
            } else {
                safeNavigate(SplashFragmentDirections.actionSplashToHome(args.destination))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (isSplashCompleted.get()) {
            onSplashComplete(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Release all ads
        AdsProvider.interSplash.releaseAll()
        AdsProvider.bannerSplash.releaseAll()
    }

    @Composable
    override fun ComposeView() {
        SplashScreen(showBanner = showBanner)
    }
}

@Composable
private fun SplashScreen(
    showBanner: Boolean = false,
) {
    BaseScreen(
        modifier = Modifier
            .fillMaxSize(),
        backgroundColor = Color.Transparent,
        bottomBar = {
            if (showBanner) {
                BannerAdContent(
                    bannerAdGroup = AdsProvider.bannerSplash,
                    fastReload = false,
                )
            } else {
                LoadingAdsShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
        }
    ) {
        CenterBox(modifier = Modifier.fillMaxSize()) {

            CenterColumn(itemSpacing = 24.dp) {
                Image(
                    modifier = Modifier
                        .size(150.dp)
                        .rounded(30.dp),
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight
                )

                Text(
                    text = stringResource(R.string.app_name),
                    style = appTextStyle(32, 600, Color.Black),
                    fontFamily = PetitFormalScriptFontFamily,
                )
            }

            CenterColumn(modifier = Modifier.align(Alignment.BottomCenter), itemSpacing = 16.dp) {
                CircularProgressIndicator(color = Color.Black)

                Text(
                    text = stringResource(R.string.this_action_may_contain_advertising),
                    style = regular12,
                )

                Spacer(modifier = Modifier.fillMaxHeight(.05f))
            }
        }
    }
}


@Composable
@Preview
private fun SplashScreenPreview() {
    SplashScreen()
}