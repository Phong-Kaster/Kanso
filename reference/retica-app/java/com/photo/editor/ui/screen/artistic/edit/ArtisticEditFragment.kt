package com.photo.editor.ui.screen.artistic.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.AppGraphDirections
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.Dark70
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.transformableimage.ImageState
import com.photo.editor.common.ui.transformableimage.TransformableImage
import com.photo.editor.common.util.logEvent
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.platform.app.App
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.artistic.common.ArtisticEditInteraction
import com.photo.editor.ui.screen.artistic.common.ArtisticUiState
import com.photo.editor.ui.screen.artistic.common.ArtisticViewModel
import com.photo.editor.ui.screen.artistic.edit.component.ArtisticEditBottomBar
import com.photo.editor.ui.screen.artistic.edit.component.ArtisticEditLoadingDialog
import com.photo.editor.ui.screen.artistic.edit.component.ArtisticEditTopBar
import com.photo.editor.ui.screen.home.HomeButtonModel
import kotlinx.coroutines.delay
import org.koin.androidx.navigation.koinNavGraphViewModel

class ArtisticEditFragment : BaseComposeFragment() {
    private val viewModel: ArtisticViewModel by koinNavGraphViewModel(R.id.artistic_graph)
    private val uiState: ArtisticUiState
        get() = viewModel.uiState

    override fun onScreenView() {
        super.onScreenView()
        logEvent("ai_transfer_adjust_scr")
    }

    override fun onResume() {
        super.onResume()

        AdsProvider.interSave.loadAds(requireContext())
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
        logEvent("ai_transfer_adjust_scr_save_click")

        viewModel.exportImage(
            onSuccess = {
                runOnUiThread {
                    toast(R.string.image_saved)
                    showInterSave {
                        AdsProvider.nativeSaved.loadAds(App.instance)
                        safeNavigate(AppGraphDirections.actionExported(imageUri = it, eventName = HomeButtonModel.Artistic.eventName))
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
        ArtisticEditScreen(
            uiState = uiState,
            interaction = ArtisticEditInteraction(
                onBackClick = {
                    logEvent("ai_transfer_adjust_scr_back_click")
                    safePopBackstack()
                },
                onSaveClick = ::handleSaveClick,
                onBlendRatioChange = viewModel::updateBlendRatio,
                onStyleClick = viewModel::selectStyle
            )
        )

        AnimatedVisibility(
            uiState.processing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BackHandler { }

            ArtisticEditLoadingDialog()
        }
    }
}

@Composable
private fun ArtisticEditScreen(
    uiState: ArtisticUiState = ArtisticUiState(),
    interaction: ArtisticEditInteraction = ArtisticEditInteraction(),
) {
    val imageState = remember { ImageState() }
    var maximized by remember { mutableStateOf(false) }


    BackHandler {
        logEvent("ai_transfer_adjust_scr_back_click")
        interaction.onBackClick()
    }

    LaunchedEffect(uiState.showOriginalImage, uiState.originalBitmap, uiState.generatedBitmap) {
        imageState.image = if (uiState.showOriginalImage)
            uiState.originalBitmap.asImageBitmap()
        else uiState.generatedBitmap.asImageBitmap()
    }

    // Reload ads when maximize
    LaunchedEffect(maximized) {
        if (maximized) {
            delay(200)
        }
    }

    BaseScreen(
        topBar = {
            ArtisticEditTopBar(uiState, interaction, maximized)
        },
        bottomBar = {
            BottomAdsView(maximized = maximized)
        },
        backgroundColor = SubColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            TransformableImage(
                state = imageState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 166.dp)
                    .statusBarsPadding()
            )

            ToolsView(
                modifier = Modifier
                    .align(Alignment.TopEnd),
                maximized = maximized,
                onMaximizedClick = {
                    maximized = !maximized
                },
                uiState = uiState
            )
            AnimatedVisibility(
                modifier = Modifier.align(alignment = Alignment.BottomCenter),
                visible = !maximized) {
                // Height : 62 + 104
                ArtisticEditBottomBar(
                    uiState = uiState,
                    interaction = interaction
                )
            }
        }
    }
}

@Composable
private fun ToolsView(
    modifier: Modifier,
    maximized: Boolean,
    onMaximizedClick: () -> Unit = {},
    uiState: ArtisticUiState,
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

        if (uiState.selectedStyle != null) {
            CenterBox(
                modifier = Modifier
                    .size(40.dp)
                    .rounded(10.dp)
                    .background(Dark70)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            uiState.showOriginalImage = true
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })
                            uiState.showOriginalImage = false
                        }
                    }) {
                Image(
                    painter = painterResource(R.drawable.ic_preview),
                    contentDescription = stringResource(R.string.preview),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomAdsView(maximized: Boolean) {
    AnimatedVisibility(
        modifier = Modifier.background(Color.White),
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
    ArtisticEditScreen()
}