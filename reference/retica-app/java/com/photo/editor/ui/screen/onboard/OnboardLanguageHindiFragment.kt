package com.photo.editor.ui.screen.onboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.util.logEvent
import com.photo.editor.common.util.toast
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.domain.model.Language
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.platform.app.App
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.selector.LanguageSelector
import com.photo.editor.ui.component.topbar.CommonTopBar
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.navigation.koinNavGraphViewModel

class OnboardLanguageHindiFragment : BaseComposeFragment() {
    private val args: OnboardLanguageHindiFragmentArgs by navArgs()
    private val viewModel by koinNavGraphViewModel<OnboardViewModel>(R.id.onboardGraph)

    override fun onScreenView() {
        super.onScreenView()
        AdsProvider.disableAppResume()

        if (args.alternative) logEvent("language_hi_fo_alt_scr")
        else logEvent("language_hi_fo_scr")

        // Preload native language alternative if current screen is language fo screen
        if (!args.alternative) {
            AdsProvider.nativeLanguageHindiDup.loadAds(requireActivity())
        }
    }

    private fun finishLanguageScreen() {
        val finishAction = {
            if (viewModel.selectedHindiLanguage != null) {
                viewModel.confirmHindiLanguage()
                requireActivity().recreate()
                safeNavigate(OnboardLanguageHindiFragmentDirections.actionOnboardWelcome())
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

    override fun onResume() {
        super.onResume()

        if (args.alternative) AdsProvider.nativeLanguageHindiDup.loadAds(requireActivity())
        else AdsProvider.nativeLanguageHindi.loadAds(requireActivity())
    }

    override fun onDestroy() {
        super.onDestroy()

        // Destroy native ads when leaving screen
        if (args.alternative) AdsProvider.nativeLanguageDup.releaseAll()
        else AdsProvider.nativeLanguage.releaseAll()
    }

    @Composable
    override fun ComposeView() {
        OnboardLanguageHindiScreen(
            selectedLanguage = viewModel.selectedHindiLanguage,
            onSelectedLanguageChange = { language ->
                // Track event
                if (args.alternative) logEvent("language_hi_fo_alt_scr_language_select")
                else logEvent("language_hi_fo_scr_language_select")

                viewModel.selectedHindiLanguage = language

                if (AdsProvider.nativeLanguageHindiDup.enabled) {
                    safeNavigate(
                        OnboardLanguageHindiFragmentDirections
                            .actionOnboardLanguageHindiSelf(alternative = true)
                    )
                }
            },
            onConfirmLanguage = {
                if (viewModel.selectedHindiLanguage != null) {
                    if (args.alternative) logEvent("language_hi_fo_alt_scr_confirm_click")
                    else logEvent("language_hi_fo_scr_confirm_click")

                    finishLanguageScreen()
                } else {
                    context?.toast("Please select a language")
                }
            },
            alternative = AdsProvider.nativeLanguageHindiDup.enabled && args.alternative,
            onBackClick = {
                safePopBackstack()
            }
        )
    }
}

@Composable
private fun OnboardLanguageHindiScreen(
    selectedLanguage: Language?,
    onSelectedLanguageChange: (Language) -> Unit = {},
    onConfirmLanguage: () -> Unit = {},
    alternative: Boolean = false, // Is this alternative language screen
    onBackClick: () -> Unit = { },
) {
    // Disable next button for a short amount of time to wait for native language dup finish
    // Enable regardless if it hasn't finished by then
    var enableNextButton by remember { mutableStateOf(!alternative) }
    val nativeLanguageDupStatus by AdsProvider.nativeLanguageHindiDup.statusFlow.collectAsState()

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
            CommonTopBar(
                title = stringResource(R.string.hindi_language),
                onBackClick = onBackClick,
                rightContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.next),
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp)
                            .rippleClickable {
                                if (!enableNextButton) return@rippleClickable
                                onConfirmLanguage()
                            },
                        tint = Color.Black,
                    )
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
                adGroup = if (alternative) AdsProvider.nativeLanguageHindiDup
                else AdsProvider.nativeLanguageHindi,
                layoutId = layoutId,
                metaLayoutId = metaLayoutId,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F3F3), rounded(10.dp)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            LanguageSelector(
                languages = Language.hindiList,
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
    onBackClick: () -> Unit = {},
    onConfirmLanguage: () -> Unit = {},
) {

}

@Composable
@Preview
private fun Preview() {
    OnboardLanguageHindiScreen(selectedLanguage = Language.Hindi)
}