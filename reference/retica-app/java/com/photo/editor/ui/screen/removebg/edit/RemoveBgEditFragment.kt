package com.photo.editor.ui.screen.removebg.edit

import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import apero.aperosg.monetization.ui.NativeAdContent
import com.photo.editor.AppGraphDirections
import com.photo.editor.R
import com.photo.editor.common.ui.bottomsheet.ColorPickerBottomSheet
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.Dark70
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.medium18
import com.photo.editor.common.ui.transformableimage.ImageState
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.platform.app.App
import com.photo.editor.ui.activity.setLightStatusBar
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.gallery.GalleryViewModel
import com.photo.editor.ui.screen.home.HomeButtonModel
import com.photo.editor.ui.screen.removebg.common.RemoveBGBackground
import com.photo.editor.ui.screen.removebg.common.RemoveBgEditInteraction
import com.photo.editor.ui.screen.removebg.common.RemoveBgUiState
import com.photo.editor.ui.screen.removebg.common.RemoveBgViewModel
import com.photo.editor.ui.screen.removebg.edit.component.RemoveBgEditImage
import com.photo.editor.ui.screen.removebg.edit.component.RemoveBgEditMainButtons
import com.photo.editor.ui.screen.removebg.edit.component.RemoveBgEditPickerBottomBar
import com.photo.editor.ui.screen.removebg.edit.component.RemoveBgEditTopBar
import com.photo.editor.ui.screen.removebg.edit.component.refine.RefineDrawingState
import com.photo.gallery.model.GalleryBucket
import com.photo.gallery.model.GalleryImage
import kotlinx.coroutines.delay
import org.koin.androidx.navigation.koinNavGraphViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class RemoveBgEditFragment : BaseComposeFragment() {
    private val viewModel by koinNavGraphViewModel<RemoveBgViewModel>(R.id.remove_bg_graph)
    private val galleryViewModel: GalleryViewModel by activityViewModel()

    private val uiState: RemoveBgUiState
        get() = viewModel.uiState

    private val imageState: ImageState
        get() = viewModel.imageState

    private val drawingState: RefineDrawingState?
        get() = viewModel.drawingState

    private val imageBuckets: List<GalleryBucket>
        get() = galleryViewModel.imageBuckets.value

    private val allImages: List<GalleryImage>
        get() = galleryViewModel.allImages.value

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLightStatusBar(true)
        galleryViewModel.fetchMedia()
    }

    override fun onScreenView() {
        super.onScreenView()
        logEvent("ai_remove_bg_result_scr")
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
        logEvent("ai_remove_bg_result_scr_save_click")

        viewModel.exportImage(
            onSuccess = {
                runOnUiThread {
                    toast(R.string.image_saved)
                    showInterSave {
                        AdsProvider.nativeSaved.loadAds(App.instance)
                        safeNavigate(AppGraphDirections.actionExported(imageUri = it, eventName = HomeButtonModel.RemoveBG.eventName))
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
        RemoveBgEditScreen(
            uiState = uiState,
            imageState = imageState,
            drawingState = drawingState,
            interaction = RemoveBgEditInteraction(
                onBackClick = {
                    if (uiState.saving) return@RemoveBgEditInteraction
                    logEvent("ai_remove_bg_result_scr_back_click")
                    safePopBackstack()
                },
                onSaveClick = ::handleSaveClick
            ),

            imagesProvider = { allImages },
            bucketsProvider = { imageBuckets },
        )

        AnimatedVisibility(
            uiState.saving,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BackHandler { }

            CenterColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .silentClickable { }
                    .background(Color.Black.copy(0.8f)),
                itemSpacing = 24.dp
            ) {
                CircularProgressIndicator(color = Color.White)

                Text(
                    text = stringResource(R.string.saving_image),
                    style = medium18,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun RemoveBgEditScreen(
    uiState: RemoveBgUiState,
    imageState: ImageState,
    drawingState: RefineDrawingState? = null,
    interaction: RemoveBgEditInteraction = RemoveBgEditInteraction(),
    imagesProvider: () -> List<GalleryImage> = { listOf() },
    bucketsProvider: () -> List<GalleryBucket> = { listOf() },
) {

    var maximized by remember { mutableStateOf(false) }
    var showColorPickerBottomSheet by remember { mutableStateOf(false) }
    var showImagePickerBottomSheet by remember { mutableStateOf(false) }

    BackHandler {
        logEvent("ai_remove_bg_result_scr_back_click")
        interaction.onBackClick()
    }

    LaunchedEffect(uiState.showOriginalImage, uiState.currentImage, uiState.originalImage) {
        if (uiState.showOriginalImage) imageState.image = uiState.originalImage.asImageBitmap()
        else imageState.image = uiState.currentImage.asImageBitmap()
    }

    // Reload ads when maximize
    LaunchedEffect(maximized) {
        if (maximized) {
            delay(200)
        }
    }

    BaseScreen(
        topBar = {
            RemoveBgEditTopBar(interaction = interaction, maximized = maximized)
        },
        bottomBar = {
            BottomAdsView(maximized = maximized)
        },
        backgroundColor = SubColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .animateContentSize()
            ) {

                RemoveBgEditImage(
                    state = imageState,
                    drawingState = drawingState,
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
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
            }

            AnimatedVisibility(
                visible = !maximized,
                enter = slideInVertically { -it / 2 } + expandVertically { -it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + shrinkVertically { it / 2 } + fadeOut()
            ) {
                RemoveBgEditMainButtons(
                    onSolidColorClick = {
                        logEvent("ai_remove_bg_result_scr_add_color_click")
                        showColorPickerBottomSheet = true
                    },
                    onImageClick = {
                        logEvent("ai_remove_bg_result_scr_add_image_click")
                        showImagePickerBottomSheet = true
                    },
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                )
            }

            ColorPickerBottomSheet(
                showBottomSheet = showColorPickerBottomSheet,
                onDismissRequest = {
                    showColorPickerBottomSheet = false
                },
                initialColor =
                    if (uiState.background is RemoveBGBackground.SolidColor) Color((uiState.background as RemoveBGBackground.SolidColor).color) else Color.Transparent,
                onColorChanged = { color ->
                    uiState.background = RemoveBGBackground.SolidColor(
                        color
                            .toArgb()
                            .toLong()
                    )
                }
            )

            RemoveBgEditPickerBottomBar(
                showBottomSheet = showImagePickerBottomSheet,
                onDismissRequest = {
                    showImagePickerBottomSheet = false
                },
                imagesProvider = imagesProvider,
                bucketsProvider = bucketsProvider,
                onImageSelected = { image ->
                    uiState.background = RemoveBGBackground.Image(image.uri)
                },
                onSceneClick = { scene ->
                    uiState.background = RemoveBGBackground.Scene(scene)
                },
                onBackgroundClick = { background ->
                    uiState.background = RemoveBGBackground.Background(background)
                }
            )
        }
    }
}

@Composable
private fun ToolsView(
    modifier: Modifier,
    maximized: Boolean,
    onMaximizedClick: () -> Unit = {},
    uiState: RemoveBgUiState,
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
    RemoveBgEditScreen(
        uiState = RemoveBgUiState(),
        imageState = ImageState(),
        interaction = RemoveBgEditInteraction()
    )
}
