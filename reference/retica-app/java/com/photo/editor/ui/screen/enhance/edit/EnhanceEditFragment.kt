package com.photo.editor.ui.screen.enhance.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import apero.aperosg.monetization.ui.NativeAdContent
import com.photo.editor.AppGraphDirections
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.Dark70
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.platform.app.App
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.beforeafterimage.BeforeAfterTransformableImage
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.enhance.common.EnhanceEditInteraction
import com.photo.editor.ui.screen.enhance.common.EnhanceUiState
import com.photo.editor.ui.screen.enhance.common.EnhanceViewModel
import com.photo.editor.ui.screen.enhance.edit.component.EnhanceEditTopBar
import com.photo.editor.ui.screen.enhance.edit.component.EnhanceProcessingOverlay
import com.photo.editor.ui.screen.home.HomeButtonModel
import kotlinx.coroutines.delay
import org.koin.androidx.navigation.koinNavGraphViewModel
import kotlin.math.roundToInt

class EnhanceEditFragment : BaseComposeFragment() {
    private val viewModel: EnhanceViewModel by koinNavGraphViewModel(R.id.enhance_graph)

    private val uiState: EnhanceUiState
        get() = viewModel.uiState

    override fun onScreenView() {
        super.onScreenView()
        logEvent("ai_enhance_result_scr")
    }

    override fun onResume() {
        super.onResume()

        AdsProvider.interSave.loadAds(requireActivity())
    }

    private fun showInterSave(action: () -> Unit) {
        AdsProvider.interSave.showAds(
            activity = requireActivity(),
            onNextAction = {
                action()
            }
        )
    }

    private fun handleSaveClick() {
        logEvent("ai_enhance_result_scr_save_click")

        viewModel.exportImage(
            onSuccess = {
                runOnUiThread {
                    toast(R.string.image_saved)
                    showInterSave {
                        AdsProvider.nativeSaved.loadAds(App.instance)
                        safeNavigate(AppGraphDirections.actionExported(imageUri = it, eventName = HomeButtonModel.Enhance.eventName))
                    }
                }
            },
            onFailure = {
                runOnUiThread { toast(it) }
            }
        )
    }

    @Composable
    override fun ComposeView() {
        EnhanceEditScreen(
            uiState = uiState,
            interaction = EnhanceEditInteraction(
                onBackClick = {
                    if (uiState.saving) return@EnhanceEditInteraction

                    logEvent("ai_enhance_result_scr_back_click")
                    safePopBackstack()
                },
                onSaveClick = ::handleSaveClick
            ),
        )

        if (uiState.enhanceProgress < 1f && !uiState.failure) {

            // Block back click
            BackHandler { }

            EnhanceProcessingOverlay(
                progress = (uiState.enhanceProgress * 100).roundToInt(),
            )
        }
    }
}

@Composable
private fun EnhanceEditScreen(
    uiState: EnhanceUiState = EnhanceUiState(),
    interaction: EnhanceEditInteraction = EnhanceEditInteraction(),
) {

    var maximized by remember { mutableStateOf(false) }

    // Reload ads when maximize
    LaunchedEffect(maximized) {
        if (maximized) {
            delay(200)
        }
    }

    BackHandler {
        logEvent("ai_enhance_result_scr_back_click")
        interaction.onBackClick()
    }

    BaseScreen(
        topBar = {
            EnhanceEditTopBar(interaction = interaction, maximized = maximized)
        },
        bottomBar = {
            BottomAdsView(maximized = maximized)
        },
        backgroundColor = SubColor
    ) {
        BeforeAfterTransformableImage(
            leftImage = uiState.scaledOriginalImage,
            rightImage = uiState.scaledEnhancedImage,
            modifier = Modifier.fillMaxSize(),
        )

        ToolsView(
            modifier = Modifier
                .align(Alignment.TopEnd),
            maximized = maximized,
            onMaximizedClick = {
                maximized = !maximized
            },
        )
    }
}

@Composable
private fun ToolsView(
    modifier: Modifier,
    maximized: Boolean,
    onMaximizedClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CenterBox(
            modifier = Modifier
                .size(40.dp)
                .rounded(10.dp)
                .background(Dark70)
                .clickable { onMaximizedClick() }) {
            Icon(
                painter = painterResource(if (maximized) R.drawable.ic_minimize else R.drawable.ic_maximize),
                contentDescription = stringResource(if (maximized) R.string.minimize else R.string.maximize),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun BottomAdsView(maximized: Boolean) {
    AnimatedVisibility(
        visible = !maximized,
        enter = slideInVertically() + expandVertically() + fadeIn(),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        BaseBannerAdContent(AdsProvider.bannerAll)
    }
}

@Preview
@Composable
private fun Preview() {
    EnhanceEditScreen()
}