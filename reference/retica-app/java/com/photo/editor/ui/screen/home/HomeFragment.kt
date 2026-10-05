package com.photo.editor.ui.screen.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import apero.aperosg.monetization.ui.NativeAdContent
import apero.aperosg.monetization.update.InAppUpdateManager
import com.ads.control.admob.AppOpenManager
import com.photo.editor.AppGraphDirections
import com.photo.editor.R
import com.photo.editor.common.GlobalVariables
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.ui.activity.MainActivity
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.navigation.NavigationConstant
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.screen.edit.common.EditImageNavigationAction
import com.photo.editor.ui.screen.edit.common.EditImageViewModel
import com.photo.editor.ui.screen.home.component.HomeFilters
import com.photo.editor.ui.screen.home.component.HomeFrames
import com.photo.editor.ui.screen.home.component.HomePager
import com.photo.editor.ui.screen.home.component.HomePromotionDialog

class HomeFragment : BaseComposeFragment() {
    private lateinit var args: HomeFragmentArgs

    private var screenViewed by mutableStateOf(false)


    private val inAppUpdateLauncher =
        registerForActivityResult(StartIntentSenderForResult()) { result ->
            Log.d(TAG, "Update flow result code: " + result.resultCode)
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                AppOpenManager
                    .getInstance()
                    .disableAdResumeByClickAction()
            } else {
//            if (RemoteConfig.commonConfig.force_update) AdsProvider.disableAppResume()
//            else AdsProvider.enableAppResume()
            }

            // If force update is applied, reopen in app update flow when user go back
            if (RemoteConfig.commonConfig.force_update) {
                checkInAppUpdate()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Get arguments and then clear it
        args = HomeFragmentArgs.fromBundle(arguments ?: bundleOf())
        arguments?.clear()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onScreenView() {
        super.onScreenView()
        screenViewed = true
        logEvent("home_scr")

        if (!GlobalVariables.inAppUpdateChecked) {
            checkInAppUpdate()
            GlobalVariables.inAppUpdateChecked = true
        }

        checkPermissionsAndShowBottomSheet()
        resolveNavigationDestination()
    }

    private fun resolveNavigationDestination() {
        when (args.destination) {
            NavigationConstant.DESTINATION_CAMERA -> {
                if (MainActivity.cameraPermissionGranted) safeNavigate(AppGraphDirections.actionCamera())
                else MainActivity.showPermissionBottomSheet = true
            }

            NavigationConstant.DESTINATION_EDIT -> {
                EditImageViewModel.navigationAction = null
                if (MainActivity.storagePermissionGranted) safeNavigate(AppGraphDirections.actionGallery())
                else MainActivity.showPermissionBottomSheet = true
            }
        }
    }

    private fun checkPermissionsAndShowBottomSheet() {
        if (
            !MainActivity.notificationPermissionGranted ||
            !MainActivity.storagePermissionGranted ||
            !MainActivity.cameraPermissionGranted
        ) {
            MainActivity.showPermissionBottomSheet = true
        }
    }

    private fun checkInAppUpdate() {
        InAppUpdateManager.checkInAppUpdate(
            inAppUpdateLauncher,
            RemoteConfig.commonConfig.force_update
        )
    }

    private fun showInterItem(action: () -> Unit) {
        AdsProvider.interItem.showAds(
            activity = requireActivity(),
            onNextAction = {
                action()
            }
        )
    }

    private fun openGallery() {
        if (MainActivity.storagePermissionGranted) {
            showInterItem {
                safeNavigate(AppGraphDirections.actionGallery())
            }
        } else {
            MainActivity.showPermissionBottomSheet = true
        }
    }

    private fun openCamera() {
        if (MainActivity.cameraPermissionGranted) {
            showInterItem {
                safeNavigate(AppGraphDirections.actionCamera())
            }
        } else {
            MainActivity.showPermissionBottomSheet = true
        }
    }

    override fun onResume() {
        super.onResume()

        AdsProvider.interItem.loadAds(requireActivity())
    }

    private fun handleButtonClick(feature: HomeButtonModel) {
        logEvent("home_scr_${feature.eventName}_click")
        when (feature) {
            HomeButtonModel.Filter -> {
                EditImageViewModel.navigationAction = EditImageNavigationAction.OpenFilter
                openGallery()
            }

            HomeButtonModel.Beautify -> {
                EditImageViewModel.navigationAction = EditImageNavigationAction.OpenBeauty
                openGallery()
            }

            HomeButtonModel.Makeup -> {
                EditImageViewModel.navigationAction = EditImageNavigationAction.OpenMakeup
                openGallery()
            }

            HomeButtonModel.Sticker -> {
                EditImageViewModel.navigationAction = EditImageNavigationAction.OpenSticker
                openGallery()
            }

            HomeButtonModel.Frame -> {
                EditImageViewModel.navigationAction = EditImageNavigationAction.OpenFrame
                openGallery()
            }

            HomeButtonModel.Brush -> {
                EditImageViewModel.navigationAction = EditImageNavigationAction.OpenBrush
                openGallery()
            }

            HomeButtonModel.RemoveBG -> {
                safeNavigate(AppGraphDirections.actionRemoveBG())
            }

            HomeButtonModel.Retouch -> {
                safeNavigate(AppGraphDirections.actionRetouch())
            }

            HomeButtonModel.Artistic -> {
                safeNavigate(AppGraphDirections.actionArtistic())
            }

            HomeButtonModel.Enhance -> {
                safeNavigate(AppGraphDirections.actionEnhance())
            }
        }
    }

    @Composable
    override fun ComposeView() {
        HomeScreen(
            interaction = HomeInteraction(
                onSettingsClick = {
                    logEvent("home_scr_settings_click")
                    safeNavigate(AppGraphDirections.actionSettings())
                },
                onEditClick = {
                    logEvent("home_scr_edit_click")
                    EditImageViewModel.navigationAction = null
                    openGallery()
                },
                onCameraClick = {
                    logEvent("home_scr_camera_click")
                    EditImageViewModel.navigationAction = null
                    openCamera()
                },
                onFilterItemClick = { filter ->
                    logEvent("home_scr_filter_item_click")
                    EditImageViewModel.navigationAction =
                        EditImageNavigationAction.UseFilter(filter)
                    openGallery()
                },
                onFrameItemClick = { frame ->
                    logEvent("home_scr_frame_item_click")
                    EditImageViewModel.navigationAction = EditImageNavigationAction.UseFrame(frame)
                    openGallery()
                },

                onHomeButtonClick = { feature ->
                    handleButtonClick(feature)
                }
            )
        )

        HomePromotionDialog(
            promotionIndexProvider = { settingsRepository.getPromotionDialogIndex() },
            screenViewed = screenViewed,
            onTryClick = { index ->
                // Mark promotion dialog as shown
                when (index) {
                    0 -> {
                        settingsRepository.stickerPromotionShown = true
                        settingsRepository.filterPromotionShown = false
                    }

                    1 -> {
                        settingsRepository.filterPromotionShown = true
                        settingsRepository.removeBgPromotionShown = false
                    }

                    2 -> {
                        settingsRepository.removeBgPromotionShown = true
                        settingsRepository.stickerPromotionShown = false
                    }
                }

                // Open destination
                when (index) {
                    0 -> {
                        safeNavigate(AppGraphDirections.actionRetouch())
                    }

                    1 -> {
                        EditImageViewModel.navigationAction = EditImageNavigationAction.OpenFilter
                        openGallery()
                    }

                    2 -> {
                        safeNavigate(AppGraphDirections.actionRemoveBG())
                    }
                }
            },
            onDismiss = { index ->
                // Mark promotion dialog as shown
                when (index) {
                    0 -> {
                        settingsRepository.stickerPromotionShown = true
                        settingsRepository.filterPromotionShown = false
                    }

                    1 -> {
                        settingsRepository.filterPromotionShown = true
                        settingsRepository.removeBgPromotionShown = false
                    }

                    2 -> {
                        settingsRepository.removeBgPromotionShown = true
                        settingsRepository.stickerPromotionShown = false
                    }
                }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    interaction: HomeInteraction = HomeInteraction(),
) {
    val baseAdmobLayoutId =
        if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_home) R.layout.native_ads_medium_above
        else R.layout.native_ads_medium
    val baseMetaLayoutId =
        if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_home) R.layout.native_ads_medium_meta_above
        else R.layout.native_ads_medium_meta

    val admobLayoutId =
        if (RemoteConfig.adsLayoutConfig.force_meta_layout_home) baseMetaLayoutId else baseAdmobLayoutId
    val metaLayoutId =
        if (RemoteConfig.adsLayoutConfig.force_admob_layout_home) baseAdmobLayoutId else baseMetaLayoutId

    BaseScreen(
        backgroundColor = SubColor,
        bottomBar = {
            BaseBannerAdContent(AdsProvider.bannerAll)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HomePager(interaction = interaction)

            NativeAdContent(
                adGroup = AdsProvider.nativeHome,
                layoutId = admobLayoutId,
                metaLayoutId = metaLayoutId,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
                    .background(AlternativeColor, rounded(10.dp)),
                reloadOnLifecycleEvent = Lifecycle.Event.ON_RESUME,
            )

            HomeFilters(
                onItemClick = interaction.onFilterItemClick,
                modifier = Modifier.padding(top = 8.dp)
            )

            HomeFrames(
                onItemClick = interaction.onFrameItemClick,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

@Preview
@Composable
private fun Preview() {
    HomeScreen()
}