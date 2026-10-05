package com.photo.editor.ui.screen.removebg.loading

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import apero.aperosg.monetization.ui.NativeAdContent
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.medium14
import com.photo.editor.common.ui.theme.regular18
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.dialog.ConfirmBackDialog
import com.photo.editor.ui.component.topbar.CommonTopBar
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.removebg.common.RemoveBgUiState
import com.photo.editor.ui.screen.removebg.common.RemoveBgViewModel
import com.photo.editor.ui.screen.removebg.loading.component.ScanningView
import kotlinx.coroutines.delay
import org.koin.androidx.navigation.koinNavGraphViewModel

class RemoveBgLoadingFragment : BaseComposeFragment() {

    private val viewModel by koinNavGraphViewModel<RemoveBgViewModel>(R.id.remove_bg_graph)
    private val uiState: RemoveBgUiState
        get() = viewModel.uiState


    override fun onScreenView() {
        super.onScreenView()
        logEvent("ai_remove_bg_loading_scr")
    }

    override fun onResume() {
        super.onResume()

        AdsProvider.nativeLoading.loadAds(requireActivity())
    }

    @Composable
    override fun ComposeView() {

        var showConfirmDialog by remember { mutableStateOf(false) }
        var allowNavigation by remember { mutableStateOf(false) }

        // Delay 5s before allow navigating to next screen
        LaunchedEffect(Unit) {
            delay(5000)
            allowNavigation = true
        }

        // Wait for signal to allow to navigate and also wait to image to done processing before navigating
        LaunchedEffect(allowNavigation, uiState.loading) {
            if (allowNavigation && !uiState.loading) {
                safeNavigate(RemoveBgLoadingFragmentDirections.actionLoadingToEdit())
            }
        }

        RemoveBGLoadingScreen(
            uiState = uiState,
            onBackClick = {
                showConfirmDialog = true
            }
        )

        ConfirmBackDialog(
            showDialog = showConfirmDialog,
            onDismissRequest = { showConfirmDialog = false },
            onConfirmBack = {
                safePopBackstack()
            },
        )
    }
}

@Composable
private fun RemoveBGLoadingScreen(
    uiState: RemoveBgUiState = RemoveBgUiState(),
    onBackClick: () -> Unit = {},
) {
    BackHandler { onBackClick() }

    BaseScreen(
        topBar = {
            CommonTopBar(
                modifier = Modifier.background(Color.White),
                title = stringResource(R.string.remove_background),
                onBackClick = { onBackClick() },
                positionTitle = Alignment.CenterStart,
                textTitleStyle = regular18
            )
        },
        bottomBar = {
            val baseAdmobLayoutId =
                if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_loading) R.layout.native_ads_medium_above
                else R.layout.native_ads_medium
            val baseMetaLayoutId =
                if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_loading) R.layout.native_ads_medium_meta_above
                else R.layout.native_ads_medium_meta
            val admobLayoutId =
                if (RemoteConfig.adsLayoutConfig.force_meta_layout_native_loading) baseMetaLayoutId else baseAdmobLayoutId
            val metaLayoutId =
                if (RemoteConfig.adsLayoutConfig.force_admob_layout_native_loading) baseAdmobLayoutId else baseMetaLayoutId


            NativeAdContent(
                adGroup = AdsProvider.nativeLoading,
                layoutId = admobLayoutId,
                metaLayoutId = metaLayoutId,

                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlternativeColor),
                reloadOnLifecycleEvent = Lifecycle.Event.ON_RESUME,
            )

        },
        backgroundColor = SubColor
    ) {
        CenterColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 48.dp),
            itemSpacing = 12.dp
        ) {
            Text(
                text = stringResource(R.string.removing_background),
                style = medium14,
                color = Color.Black,
            )

            ScanningView {
                Image(
                    bitmap = uiState.originalImage.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(uiState.originalImage.width / uiState.originalImage.height.toFloat())
                        .rounded(10.dp)
                )
            }
        }
    }
}


@Preview
@Composable
private fun Preview() {

    RemoveBGLoadingScreen(
        uiState = RemoveBgUiState()
    )
}