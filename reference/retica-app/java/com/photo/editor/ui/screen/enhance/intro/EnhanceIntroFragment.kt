package com.photo.editor.ui.screen.enhance.intro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import apero.aperosg.monetization.enums.AdStatus
import apero.aperosg.monetization.ui.NativeAdContent
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.GlobalVariables
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.outerShadow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.appTextStyle
import com.photo.editor.common.ui.theme.bold24
import com.photo.editor.common.ui.theme.extraBold10
import com.photo.editor.common.ui.theme.extraBold14
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.common.ui.theme.regular18
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.ui.activity.MainActivity
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.shimmer.SimpleShimmer
import com.photo.editor.ui.component.topbar.CommonTopBar
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.enhance.common.EnhanceIntroInteraction
import com.photo.editor.ui.screen.enhance.common.EnhanceViewModel
import com.photo.editor.ui.screen.enhance.model.EnhanceSample
import com.photo.editor.ui.screen.removebg.intro.RemoveBgIntroFragmentDirections
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.navigation.koinNavGraphViewModel

class EnhanceIntroFragment : BaseComposeFragment() {
    private val viewModel: EnhanceViewModel by koinNavGraphViewModel(R.id.enhance_graph)

    //    private val args: EnhanceGraphArgs by navArgs()
    private var pendingResumeAction = {}

    override fun onScreenView() {
        super.onScreenView()
        logEvent("ai_enhance_scr")
    }

    override fun onResume() {
        super.onResume()

        AdsProvider.nativePreview.loadAds(requireActivity())
        if (!GlobalVariables.watchedRewardEnhance) {
            AdsProvider.rewardGen.loadAds(requireActivity())
        }

        if (pendingResumeAction != {}) {
            pendingResumeAction()
            pendingResumeAction = {}
        }
    }

    @Composable
    override fun ComposeView() {
        val rewardAdsLoading =
            AdsProvider.rewardGen.statusFlow.collectAsState().value == AdStatus.Loading
        var waitingForRewardAds by remember { mutableStateOf(false) }

        LaunchedEffect(waitingForRewardAds) {
            if (waitingForRewardAds) {
                AdsProvider.rewardGen.statusFlow.collectLatest {
                    if (it == AdStatus.Ready) {
                        waitingForRewardAds = false
                        AdsProvider.rewardGen.showAds(
                            activity = requireActivity(),
                            onUserEarnedReward = {
                                GlobalVariables.watchedRewardEnhance = true

                                pendingResumeAction = {
                                    safeNavigate(RemoveBgIntroFragmentDirections.actionIntroToGallery())
                                }
                            })
                    } else if (it == AdStatus.Failure) {
                        waitingForRewardAds = false
                        safeNavigate(EnhanceIntroFragmentDirections.actionIntroToGallery())
                    }
                }
            }
        }


        EnhanceIntroScreen(
            rewardAdsLoading = rewardAdsLoading && waitingForRewardAds,
            interaction = EnhanceIntroInteraction(onBackClick = {
                logEvent("ai_enhance_scr_back_click")
                safePopBackstack()
            }, onUploadImageClick = {
                logEvent("ai_enhance_scr_upload_click")

                if (MainActivity.storagePermissionGranted) {
                    val shouldWatchRewardAds =
                        AdsProvider.rewardGen.enabled && !GlobalVariables.watchedRewardEnhance

                    if (!shouldWatchRewardAds) {
                        safeNavigate(EnhanceIntroFragmentDirections.actionIntroToGallery())
                    } else {
                        waitingForRewardAds = true
                    }
                } else {
                    toast(R.string.storage_permission_required)
                    MainActivity.showPermissionBottomSheet = true
                }

            }, onSampleClick = {
                if (waitingForRewardAds) return@EnhanceIntroInteraction
                viewModel.setImage(it.assetPath)
                safeNavigate(EnhanceIntroFragmentDirections.actionIntroToEdit())
            })
        )
    }
}

@Composable
private fun EnhanceIntroScreen(
    rewardAdsLoading: Boolean = false,
    interaction: EnhanceIntroInteraction = EnhanceIntroInteraction(),
) {

    val shouldWatchRewardAds = AdsProvider.rewardGen.enabledFlow.collectAsState().value
            && !GlobalVariables.watchedRewardEnhance

    BackHandler {
        logEvent("ai_enhance_scr_back_click")
        interaction.onBackClick()
    }

    BaseScreen(
        topBar = {
            CommonTopBar(
                modifier = Modifier.background(Color.White),
                title = stringResource(R.string.ai_enhance),
                onBackClick = { interaction.onBackClick() },
                positionTitle = Alignment.CenterStart,
                textTitleStyle = regular18
            )
        }, bottomBar = {
            val baseAdmobLayoutId =
                if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_preview) R.layout.native_ads_medium_above
                else R.layout.native_ads_large
            val baseMetaLayoutId =
                if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_preview) R.layout.native_ads_medium_meta_above
                else R.layout.native_ads_large_meta
            val admobLayoutId =
                if (RemoteConfig.adsLayoutConfig.force_meta_layout_native_preview) baseMetaLayoutId else baseAdmobLayoutId
            val metaLayoutId =
                if (RemoteConfig.adsLayoutConfig.force_admob_layout_native_preview) baseAdmobLayoutId else baseMetaLayoutId

            NativeAdContent(
                adGroup = AdsProvider.nativePreview,
                layoutId = admobLayoutId,
                metaLayoutId = metaLayoutId,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlternativeColor),
                reloadOnLifecycleEvent = Lifecycle.Event.ON_RESUME,
                alwaysOccupySpace = true
            )
        }, backgroundColor = SubColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Image(
                painter = painterResource(R.drawable.ai_enhance_intro_bg),
                contentDescription = stringResource(R.string.background),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(412 / 380f),
                contentScale = ContentScale.FillWidth,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 50.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(R.string.enhance_intro_title),
                        style = bold24,
                        lineHeight = 20.sp,
                        color = Color.White,
                    )

                    Text(
                        text = stringResource(R.string.enhance_intro_subtitle),
                        style = regular14,
                        color = Color.White,
                        lineHeight = 20.sp,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .outerShadow(8.dp, 2.dp, Color.Black.copy(0.2f), RoundedCornerShape(16.dp))
                        .rounded(16.dp)
                        .background(Color.White)
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .rounded(10.dp)
                            .clickable { interaction.onUploadImageClick() }
                            .background(Color.Black),
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(
                                0.dp, Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (rewardAdsLoading) {
                                CircularProgressIndicator(color = Color.White)
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.ic_ai_enhance),
                                    contentDescription = stringResource(R.string.remove_background),
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        extraBold10
                                            .copy(SubColor)
                                            .toSpanStyle()
                                    ) {
                                        append(stringResource(R.string.upload_photo_for).uppercase())
                                        append("\n")
                                    }
                                    withStyle(
                                        extraBold14
                                            .copy(SubColor)
                                            .toSpanStyle()
                                    ) {
                                        append(stringResource(R.string.ai_enhance).uppercase())
                                    }
                                }, lineHeight = 15.sp
                            )
                        }

                        if (shouldWatchRewardAds) {
                            CenterRow(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(vertical = 5.dp, horizontal = 8.dp), itemSpacing = 5.dp
                            ) {
                                Text(
                                    text = stringResource(R.string.watch_an_ad),
                                    style = appTextStyle(9, 400, SubColor),
                                )

                                Icon(
                                    painter = painterResource(R.drawable.ic_watch_ad),
                                    contentDescription = stringResource(R.string.watch_an_ad),
                                    modifier = Modifier.size(10.dp),
                                    tint = SubColor,
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.remove_bg_intro_try),
                        style = regular14,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            12.dp, Alignment.CenterHorizontally
                        ),
                    ) {
                        EnhanceSample.entries.forEach { sample ->
                            SubcomposeAsyncImage(
                                model = getAssetAbsoluteFilePath(sample.assetPath),
                                contentDescription = stringResource(R.string.sample),
                                loading = { SimpleShimmer() },
                                modifier = Modifier
                                    .size(52.dp)
                                    .rounded(10.dp)
                                    .clickable { interaction.onSampleClick(sample) },
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun Preview() {
    EnhanceIntroScreen()
}