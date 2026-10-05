package com.photo.editor.ui.screen.onboard

import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import apero.aperosg.monetization.ui.NativeAdContent
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.medium14
import com.photo.editor.common.ui.theme.medium18
import com.photo.editor.common.ui.theme.medium20
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.platform.app.App
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.topbar.BaseTopBar
import com.photo.editor.ui.navigation.safeNavigate
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.navigation.koinNavGraphViewModel

class OnboardWelcomeFragment : BaseComposeFragment() {
    private val viewModel: OnboardViewModel by koinNavGraphViewModel(R.id.onboardGraph)
    private var alternative by mutableStateOf(false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alternative = false

        lifecycleScope.launch {
            snapshotFlow { alternative }.collectLatest { alternative ->
                if (!alternative) logEvent("welcome_scr")
                else logEvent("welcome_dup_scr")
            }
        }
    }

    override fun onScreenView() {
        super.onScreenView()

        AdsProvider.nativeWelcomeDup.loadAds(requireActivity())
        GlobalScope.launch {
            delay(1000)
            AdsProvider.nativeOnboard.loadAds(App.instance)
            delay(1000)
            AdsProvider.nativeFullScr.loadIndexes(App.instance, 0)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!alternative) {
            AdsProvider.nativeWelcome.loadAds(requireActivity())
        } else {
            AdsProvider.nativeWelcomeDup.loadAds(requireActivity())
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        AdsProvider.nativeWelcome.releaseAll()
        AdsProvider.nativeWelcomeDup.releaseAll()
    }

    @Composable
    override fun ComposeView() {
        BackHandler {
            safeNavigate(OnboardWelcomeFragmentDirections.actionOnboardLanguage())
        }

        OnboardWelcomeScreen(
            alternative = alternative,
            selectedIndices = viewModel.selectedWelcomeIndices,
            onItemSelected = { index ->
                if (!viewModel.selectedWelcomeIndices.remove(index)) {
                    viewModel.selectedWelcomeIndices.add(index)
                }
                alternative = true
            },
            onNextClick = {
                if (viewModel.selectedWelcomeIndices.isEmpty()) {
                    toast(R.string.welcome_select_one)
                    alternative = true
                } else {
                    logEvent("welcome_dup_scr_start_click")
                    safeNavigate(OnboardWelcomeFragmentDirections.actionOnboardContent())
                }
            }
        )
    }
}

@Composable
private fun OnboardWelcomeScreen(
    alternative: Boolean = false,
    selectedIndices: List<OnboardWelcomeItem> = emptyList(),
    onItemSelected: (OnboardWelcomeItem) -> Unit = {},
    onNextClick: () -> Unit = {},
) {
    BaseScreen(
        topBar = { BaseTopBar {} },
        bottomBar = {
            val baseAdmobLayoutId = if (!alternative) {
                if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_welcome) R.layout.native_ads_large_above
                else R.layout.native_ads_large
            } else {
                if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_welcome_dup) R.layout.native_ads_large_above
                else R.layout.native_ads_large
            }

            val baseMetaLayoutId = if (!alternative) {
                if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_welcome) R.layout.native_ads_large_meta_above
                else R.layout.native_ads_large_meta
            } else {
                if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_welcome_dup) R.layout.native_ads_large_meta_above
                else R.layout.native_ads_large_meta
            }

            val layoutId = if (!alternative) {
                if (RemoteConfig.adsLayoutConfig.force_meta_layout_welcome) baseMetaLayoutId
                else baseAdmobLayoutId
            } else {
                if (RemoteConfig.adsLayoutConfig.force_meta_layout_welcome_dup) baseMetaLayoutId
                else baseAdmobLayoutId
            }
            val metaLayoutId = if (!alternative) {
                if (RemoteConfig.adsLayoutConfig.force_admob_layout_welcome) baseAdmobLayoutId
                else baseMetaLayoutId
            } else {
                if (RemoteConfig.adsLayoutConfig.force_admob_layout_welcome_dup) baseAdmobLayoutId
                else baseMetaLayoutId
            }

            NativeAdContent(
                adGroup = if (!alternative) AdsProvider.nativeWelcome else AdsProvider.nativeWelcomeDup,
                layoutId = layoutId,
                metaLayoutId = metaLayoutId,
                alwaysOccupySpace = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlternativeColor, rounded(10.dp)),
            )
        },
        backgroundColor = Color(0xFFF8F0E1),
    ) {
        CenterColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(
                text = stringResource(R.string.welcome_title),
                style = medium20,
                color = Color.Black,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = medium14,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 10.dp)
            )


            CenterRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp),
                itemSpacing = 10.dp
            ) {
                OnboardWelcomeItem.entries.take(3).forEach { item ->
                    WelcomeItem(
                        imageId = item.imageId,
                        titleId = item.titleId,
                        selected = selectedIndices.contains(item),
                        onClick = { onItemSelected(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            CenterRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 10.dp),
                itemSpacing = 4.dp
            ) {
                OnboardWelcomeItem.entries.takeLast(3).forEach { item ->
                    WelcomeItem(
                        imageId = item.imageId,
                        titleId = item.titleId,
                        selected = selectedIndices.contains(item),
                        onClick = { onItemSelected(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = stringResource(R.string.next),
                style = medium18,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 16.dp)
                    .padding(4.dp)
                    .rounded(2.dp)
                    .clickable { onNextClick() }
                    .padding(2.dp)
            )
        }
    }
}

@Composable
private fun WelcomeItem(
    imageId: Int,
    titleId: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .offset(x = 3.dp, y = 3.dp)
                .matchParentSize()
                .rounded(4.dp)
                .background(Color(0xFFE0D8CB))
        )

        Column(
            modifier = Modifier
                .rounded(4.dp)
                .background(Color.White)
                .clickable { onClick() }
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painter = painterResource(imageId),
                contentDescription = stringResource(titleId),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .rounded(2.dp),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )

            CenterRow(
                modifier = Modifier.fillMaxWidth(),
                itemSpacing = 4.dp
            ) {
                Text(
                    text = stringResource(titleId),
                    style = medium14,
                    color = Color.Black,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(Int.MAX_VALUE),
                )

                Image(
                    painter = painterResource(if (selected) R.drawable.checkbox_selected else R.drawable.checkbox_unselected),
                    contentDescription = stringResource(R.string.checkbox),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    OnboardWelcomeScreen()
}