package com.photo.editor.ui.screen.artistic.intro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import apero.aperosg.monetization.enums.AdStatus
import apero.aperosg.monetization.ui.NativeAdContent
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.GlobalVariables
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.outerShadow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.appTextStyle
import com.photo.editor.common.ui.theme.bold14
import com.photo.editor.common.ui.theme.bold24
import com.photo.editor.common.ui.theme.medium8
import com.photo.editor.common.ui.theme.regular18
import com.photo.editor.common.ui.theme.regular8
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
import com.photo.editor.ui.screen.artistic.common.ArtisticIntroInteraction
import com.photo.editor.ui.screen.artistic.common.ArtisticUiState
import com.photo.editor.ui.screen.artistic.common.ArtisticViewModel
import com.photo.editor.ui.screen.artistic.model.ArtisticStyle
import com.photo.editor.ui.screen.retouch.intro.RetouchIntroFragmentDirections
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.navigation.koinNavGraphViewModel

class ArtisticIntroFragment : BaseComposeFragment() {
    private val viewMode: ArtisticViewModel by koinNavGraphViewModel(R.id.artistic_graph)
    private val uiState: ArtisticUiState
        get() = viewMode.uiState

    private var pendingResumeAction = {}

    override fun onScreenView() {
        super.onScreenView()
        logEvent("ai_transfer_scr")
    }

    override fun onResume() {
        super.onResume()

        AdsProvider.nativePreview.loadAds(requireActivity())
        if (!GlobalVariables.watchedRewardArtistic) {
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
                                GlobalVariables.watchedRewardArtistic = true

                                pendingResumeAction = {
                                    safeNavigate(RetouchIntroFragmentDirections.actionIntroToGallery())
                                }
                            })
                    } else if (it == AdStatus.Failure) {
                        waitingForRewardAds = false
                        safeNavigate(RetouchIntroFragmentDirections.actionIntroToGallery())
                    }
                }
            }
        }

        ArtisticIntroScreen(
            uiState = uiState,
            rewardAdsLoading = rewardAdsLoading && waitingForRewardAds,
            interaction = ArtisticIntroInteraction(
                onBackClick = {
                    logEvent("ai_transfer_scr_back_click")
                    safePopBackstack()
                },
                onGalleryClick = {
                    logEvent("ai_transfer_scr_upload_click")

                    if (MainActivity.storagePermissionGranted) {
                        val shouldWatchRewardAds =
                            AdsProvider.rewardGen.enabled && !GlobalVariables.watchedRewardArtistic

                        if (!shouldWatchRewardAds) {
                            safeNavigate(RetouchIntroFragmentDirections.actionIntroToGallery())
                        } else {
                            waitingForRewardAds = true
                        }
                    } else {
                        toast(R.string.storage_permission_required)
                        MainActivity.showPermissionBottomSheet = true
                    }
                },
                onStyleSelected = {
                    if (waitingForRewardAds) return@ArtisticIntroInteraction
                    viewMode.selectStyle(it)
                },
            )
        )
    }
}

@Composable
private fun ArtisticIntroScreen(
    rewardAdsLoading: Boolean,
    uiState: ArtisticUiState = ArtisticUiState(),
    interaction: ArtisticIntroInteraction,
) {

    val shouldWatchRewardAds = AdsProvider.rewardGen.enabledFlow.collectAsState().value
            && !GlobalVariables.watchedRewardArtistic

    BackHandler {
        logEvent("ai_transfer_scr_back_click")
        interaction.onBackClick()
    }

    BaseScreen(
        topBar = {
            CommonTopBar(
                modifier = Modifier.background(Color.White),
                title = stringResource(R.string.ai_artistic),
                onBackClick = { interaction.onBackClick() },
                positionTitle = Alignment.CenterStart,
                textTitleStyle = regular18
            )
        },
        bottomBar = {
            val baseAdmobLayoutId =
                if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_preview) R.layout.native_ads_medium_above
                else R.layout.native_ads_medium
            val baseMetaLayoutId =
                if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_preview) R.layout.native_ads_medium_meta_above
                else R.layout.native_ads_medium_meta
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
        },
        backgroundColor = SubColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            SubcomposeAsyncImage(
                model = R.drawable.ai_art_intro_bg,
                contentDescription = "ai_art_intro_bg",
                loading = { SimpleShimmer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(412 / 378f),
                contentScale = ContentScale.FillWidth,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 50.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(R.string.ai_art_intro_title),
                        style = bold24,
                        lineHeight = 20.sp,
                        color = Color.Black,
                    )

                    Text(
                        modifier = Modifier.padding(top = 6.dp),
                        text = stringResource(R.string.ai_art_intro_subtitle),
                        style = bold24,
                        lineHeight = 20.sp,
                        color = Color.Black,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .outerShadow(8.dp, 2.dp, Color.Black.copy(0.2f), RoundedCornerShape(16.dp))
                        .rounded(16.dp)
                        .background(Color.White)
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .rounded(10.dp)
                            .clickable { interaction.onGalleryClick() }
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
                                    painter = painterResource(R.drawable.ic_ai_artistic),
                                    contentDescription = stringResource(R.string.remove_background),
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            Text(
                                text = stringResource(R.string.upload_your_photo).uppercase(),
                                style = bold14,
                                lineHeight = 15.sp,
                                color = Color.White
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

                    CategoriesListView(
                        selected = uiState.selectedStyle, interaction = interaction
                    )
                }
            }
        }

    }
}

@Composable
private fun CategoriesListView(selected: ArtisticStyle?, interaction: ArtisticIntroInteraction) {
    LazyHorizontalGrid(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .height(160.dp),
        rows = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        items(ArtisticStyle.entries) { style ->
            val isSelected = selected == style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(45 / 77f)
                    .rounded(4.dp)
                    .silentClickable {
                        interaction.onStyleSelected(style)
                    }) {

                SubcomposeAsyncImage(
                    model = getAssetAbsoluteFilePath(style.assetPath),
                    contentDescription = stringResource(style.titleId),
                    loading = { SimpleShimmer() },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (isSelected) {
                    CenterBox(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFFF8F0E1).copy(0.6f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Black,
                        )
                    }
                }

                CenterBox(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = stringResource(style.titleId),
                        style = medium8,
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.basicMarquee(Int.MAX_VALUE)
                    )
                }


            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ArtisticIntroScreen(
        rewardAdsLoading = false,
        interaction = ArtisticIntroInteraction(),
        uiState = ArtisticUiState(),
    )
}

