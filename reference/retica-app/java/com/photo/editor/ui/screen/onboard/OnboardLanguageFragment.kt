package com.photo.editor.ui.screen.onboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import apero.aperosg.monetization.enums.AdStatus
import apero.aperosg.monetization.ui.NativeAdContent
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.SpaceBetweenCenterRow
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.DarkNeutralTitle
import com.photo.editor.common.ui.theme.extraBold18
import com.photo.editor.common.util.logEvent
import com.photo.editor.common.util.toast
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.domain.model.Language
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.platform.app.App
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.selector.LanguageSelector
import com.photo.editor.ui.component.topbar.BaseTopBar
import com.photo.editor.ui.navigation.safeNavigate
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.navigation.koinNavGraphViewModel

class OnboardLanguageFragment : BaseComposeFragment() {
    private val args: OnboardLanguageFragmentArgs by navArgs()
    private val viewModel by koinNavGraphViewModel<OnboardViewModel>(R.id.onboardGraph)

    override fun onScreenView() {
        super.onScreenView()
        AdsProvider.disableAppResume()

        if (args.alternative) logEvent("language_fo_alt_scr")
        else logEvent("language_fo_scr")

        // Preload native language alternative if current screen is language fo screen
        if (!args.alternative) {
            GlobalScope.launch {
                // Wait until native_language_dup has done loading
                AdsProvider.nativeLanguageDup.statusFlow.filter { it != AdStatus.Loading }.first()

                // Do not proceed if native_language_dup loaded
                if (AdsProvider.nativeLanguageDup.isAdReady()) return@launch
                AdsProvider.nativeLanguageDup.loadIndexes(App.instance, 2)
            }
        } else {
            AdsProvider.nativeWelcome.loadAds(requireActivity())
        }
    }

    private fun finishLanguageScreen() {
        val finishAction = {
            // Finish language flow if didn't select hindi from the beginning
            if (viewModel.selectedLanguage != Language.Hindi) {
                viewModel.confirmLanguage()
                requireActivity().recreate()
                safeNavigate(OnboardLanguageFragmentDirections.actionOnboardWelcome())
            }
            // Go to hindi screen
            else if (viewModel.selectedLanguage == Language.Hindi) {
                safeNavigate(OnboardLanguageFragmentDirections.actionOnboardLanguageHindi())
            } else {

            }
        }

        // Try to show inter splash if it hasn't shown
        if (!AdsProvider.interSplashShown) {
            AdsProvider.interSplash.showAds(
                activity = requireActivity(),
                onNextAction = {
                    AdsProvider.interSplashShown = true
                    finishAction()
                }
            )
        } else {
            finishAction()
        }
    }

    private fun reloadNativeLanguageDup() {
        // Custom reload native_language_dup
        if (RemoteConfig.commonConfig.reload_native_seq) {
            AdsProvider.nativeLanguageDup.loadAds(App.instance)
        } else {
            val indexToLoad = when (AdsProvider.impressionNativeLanguageDupName) {
                "native_language_dup_high" -> 0
                "native_language_dup_high_2" -> 1
                "native_language_dup_high_3" -> 2
                else -> 3
            }
            AdsProvider.nativeLanguageDup.loadIndexes(App.instance, indexToLoad)
        }
    }

    override fun onResume() {
        super.onResume()

        if (args.alternative) reloadNativeLanguageDup()
        else AdsProvider.nativeLanguage.loadAds(requireActivity())
    }

    override fun onDestroy() {
        super.onDestroy()

        // Destroy native ads when leaving screen
        if (args.alternative) AdsProvider.nativeLanguageDup.releaseAll()
        else AdsProvider.nativeLanguage.releaseAll()
    }

    @Composable
    override fun ComposeView() {
        BackHandler {
            requireActivity().finish()
        }

        OnboardLanguageScreen(
            selectedLanguage = viewModel.selectedLanguage,
            onSelectedLanguageChange = { language ->
                // Track event
                if (args.alternative) logEvent("language_fo_alt_scr_language_select")
                else logEvent("language_fo_scr_language_select")

                viewModel.selectedLanguage = language

                if (AdsProvider.nativeLanguageDup.enabled) {
                    safeNavigate(OnboardLanguageFragmentDirections.actionOnboardLanguageSelf(alternative = true))
                }
            },
            onConfirmLanguage = {
                if (viewModel.selectedLanguage != null) {
                    if (args.alternative) logEvent("language_fo_alt_scr_confirm_click")
                    else logEvent("language_fo_scr_confirm_click")

                    finishLanguageScreen()
                } else {
                    context?.toast("Please select a language")
                }
            },
            alternative = AdsProvider.nativeLanguageDup.enabled && args.alternative,
            onAdClick = {
                if (args.alternative) {
                    reloadNativeLanguageDup()
                }
            },
        )
    }
}

@Composable
private fun OnboardLanguageScreen(
    selectedLanguage: Language?,
    onSelectedLanguageChange: (Language) -> Unit = {},
    onConfirmLanguage: () -> Unit = {},
    alternative: Boolean = false, // Is this alternative language screen
    onAdClick: () -> Unit = { },
) {
    // Disable next button for a short amount of time to wait for native language dup finish
    // Enable regardless if it hasn't finished by then
    var enableNextButton by remember { mutableStateOf(!alternative) }
    val nativeLanguageDupStatus by AdsProvider.nativeLanguageDup.statusFlow.collectAsState()

    LaunchedEffect(nativeLanguageDupStatus) {
        if (!alternative || enableNextButton) return@LaunchedEffect
        if (nativeLanguageDupStatus == AdStatus.Shown || nativeLanguageDupStatus == AdStatus.Failure) {
            enableNextButton = true
        }
    }

    LaunchedEffect(Unit) {
        delay(1000)
        enableNextButton = true
    }

    BaseScreen(
        topBar = {
            OnboardLanguageTopBar(
                onConfirmLanguage = {
                    if (!enableNextButton) return@OnboardLanguageTopBar
                    onConfirmLanguage()
                }
            )
        },
        bottomBar = {
            val baseAdmobLayoutId = if (!alternative) {
                if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_language) R.layout.native_ads_large_above
                else R.layout.native_ads_large
            } else {
                if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_language_dup) R.layout.native_ads_large_above
                else R.layout.native_ads_large
            }

            val baseMetaLayoutId = if (!alternative) {
                if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_language) R.layout.native_ads_large_meta_above
                else R.layout.native_ads_large_meta
            } else {
                if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_language_dup) R.layout.native_ads_large_meta_above
                else R.layout.native_ads_large_meta
            }

            val layoutId = if (!alternative) {
                if (RemoteConfig.adsLayoutConfig.force_meta_layout_language) baseMetaLayoutId
                else baseAdmobLayoutId
            } else {
                if (RemoteConfig.adsLayoutConfig.force_meta_layout_language_dup) baseMetaLayoutId
                else baseAdmobLayoutId
            }
            val metaLayoutId = if (!alternative) {
                if (RemoteConfig.adsLayoutConfig.force_admob_layout_language) baseAdmobLayoutId
                else baseMetaLayoutId
            } else {
                if (RemoteConfig.adsLayoutConfig.force_admob_layout_language_dup) baseAdmobLayoutId
                else baseMetaLayoutId
            }

            NativeAdContent(
                adGroup = if (alternative) AdsProvider.nativeLanguageDup
                else AdsProvider.nativeLanguage,
                layoutId = layoutId,
                metaLayoutId = metaLayoutId,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F3F3), rounded(10.dp)),
                autoLoadAds = !alternative,
                onClickCallback = onAdClick,
                // Implement custom click
                reloadOnClick = !alternative,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            LanguageSelector(
                languages = Language.defaultList,
                selectedLanguage = selectedLanguage,
                onSelectLanguage = {
                    onSelectedLanguageChange(it)
                },
                modifier = Modifier
                    .fillMaxSize()

            )
        }
    }
}

@Composable
private fun OnboardLanguageTopBar(
    onConfirmLanguage: () -> Unit = {},
) {
    BaseTopBar {
        SpaceBetweenCenterRow(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_a_language),
                style = extraBold18,
                color = DarkNeutralTitle,
            )

            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.next),
                modifier = Modifier
                    .size(24.dp)
                    .rippleClickable {
                        onConfirmLanguage()
                    },
                tint = Color.Black,
            )
        }
    }
}

@Composable
@Preview
private fun OnboardLanguageScreenPreview() {
    OnboardLanguageScreen(selectedLanguage = Language.English)
}