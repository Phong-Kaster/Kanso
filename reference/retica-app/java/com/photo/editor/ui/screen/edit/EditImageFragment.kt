package com.photo.editor.ui.screen.edit

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import com.photo.editor.AppGraphDirections
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.ui.theme.medium18
import com.photo.editor.common.util.logEvent
import com.photo.editor.overlaybrush.brush.BrushDrawingSurface
import com.photo.editor.common.ui.transformableimage.TransformState
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.platform.app.App
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.dialog.ConfirmBackDialog
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.common.EditImageViewModel
import com.photo.editor.ui.screen.edit.component.EditImageTopBar
import com.photo.editor.ui.screen.edit.component.ImageCropperView
import com.photo.editor.ui.screen.edit.component.OverlaysView
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageBottomBar
import com.photo.editor.ui.screen.edit.enums.EditImageEditOption
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditImageFragment : BaseComposeFragment() {
    private val viewModel: EditImageViewModel by viewModel()
    private val uiState: EditImageUiState
        get() = viewModel.uiState
    private val transformState: TransformState
        get() = viewModel.uiState.transformState

    private var editSurface: SurfaceView? by mutableStateOf(null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initEditSurface()
    }

    override fun onScreenView() {
        super.onScreenView()
        logEvent("edit_scr")
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

    private fun initEditSurface() {
        editSurface = SurfaceView(requireContext()).apply {
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Log.d(TAG, "surfaceCreated: ")
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    Log.d(TAG, "surfaceChanged: $format $width $height")
                    viewModel.setSurface(holder.surface, width, height)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    Log.d(TAG, "surfaceDestroyed: ")
                    viewModel.setSurface(null, 0, 0)
                }
            })
        }
    }

    @Composable
    override fun ComposeView() {
        var showConfirmBackDialog by remember { mutableStateOf(false) }

        EditImageScreen(
            surface = editSurface,
            transformState = transformState,
            uiState = uiState,
            interaction = EditImageInteraction(
                onBackClick = {
                    logEvent("edit_scr_back_click")
                    if (uiState.canUndo || uiState.canRedo)
                        showConfirmBackDialog = true
                    else safePopBackstack()
                },
                onSaveClick = {
                    logEvent("edit_scr_save_click")
                    viewModel.exportImage(
                        onSuccess = {
                            runOnUiThread {
                                toast(R.string.image_saved)
                                showInterSave {
                                    AdsProvider.nativeSaved.loadAds(App.instance)
                                    safeNavigate(AppGraphDirections.actionExported(it))
                                }
                            }
                        },
                        onFailure = {
                            runOnUiThread { toast(it) }
                        }
                    )
                },
            )
        )

        ConfirmBackDialog(
            showDialog = showConfirmBackDialog,
            onDismissRequest = { showConfirmBackDialog = false },
            onConfirmBack = {
                showConfirmBackDialog = false
                safePopBackstack()
            }
        )

        // Auto switch back to edit if no face landmark detected
        LaunchedEffect(uiState.isSelectedEditOption, uiState.isDetectingFaceLandmark) {
            if (!uiState.isSelectedEditOption &&
                !uiState.isDetectingFaceLandmark &&
                uiState.faceLandmarkDetected &&
                uiState.faceLandmarks.isEmpty()
            ) {
                toast(R.string.beauty_no_facelandmark)
                uiState.isSelectedEditOption = true
            }
        }
    }
}

@Suppress("LABEL_NAME_CLASH")
@Composable
private fun EditImageScreen(
    surface: SurfaceView? = null,
    transformState: TransformState = TransformState(),
    uiState: EditImageUiState = EditImageUiState(),
    interaction: EditImageInteraction = EditImageInteraction(),
) {
    val bannerPadding = if (AdsProvider.bannerAll.enabled) 60.dp else 0.dp

    BackHandler { interaction.onBackClick() }

    BaseScreen {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF0F0F0))
                    .padding(bottom = (if (uiState.selectedEditOption == EditImageEditOption.Crop) 120.dp else 84.dp) + bannerPadding)
                    .statusBarsPadding()
            ) {
                if (uiState.selectedEditOption == EditImageEditOption.Crop) {
                    return@Box ImageCropperView(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                surface?.let {
                    AndroidView(
                        factory = { surface },
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged {
                                transformState.surfaceSize = it.toSize()
                                transformState.scaleAndTranslate(1f, 0f, 0f)
                            }
                            .transformable(rememberTransformableState { zoomChange, offsetChange, _ ->
                                transformState.scaleAndTranslate(zoomChange, offsetChange.x, offsetChange.y)
                            }),
                    )
                }

                if (!uiState.showOriginalImageFlow.collectAsState().value) {
                    BrushDrawingSurface(
                        drawingState = uiState.brushDrawingState,
                        enableDrawing = uiState.selectedEditOption == EditImageEditOption.Brush,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Hide overlays when using brush
                    if (uiState.selectedEditOption != EditImageEditOption.Brush) {
                        OverlaysView(
                            uiState = uiState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            EditImageTopBar(
                uiState = uiState,
                interaction = interaction,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            EditImageBottomBar(
                uiState = uiState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    AnimatedVisibility(
        !uiState.isSelectedEditOption && uiState.isDetectingFaceLandmark,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BackHandler { uiState.isSelectedEditOption = true }

        CenterColumn(
            modifier = Modifier
                .fillMaxSize()
                .silentClickable { }
                .background(Color.Black.copy(0.8f)),
            itemSpacing = 24.dp
        ) {
            CircularProgressIndicator(color = Color.White)

            Text(
                text = stringResource(R.string.preparing_beauty_data),
                style = medium18,
                color = Color.White,
            )
        }
    }

    AnimatedVisibility(
        uiState.exportingImage,
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

@Preview
@Composable
private fun Preview() {
    EditImageScreen()
}