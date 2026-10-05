package com.photo.editor.ui.screen.export

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import apero.aperosg.monetization.ui.NativeAdContent
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.AppGraphDirections
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.extraBold12
import com.photo.editor.common.ui.theme.extraBold14
import com.photo.editor.common.ui.theme.medium12
import com.photo.editor.common.util.logEvent
import com.photo.editor.data.remoteconfig.RemoteConfig
import com.photo.editor.domain.model.ShareTarget
import com.photo.editor.domain.usecase.ShareImageUC
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.topbar.CommonTopBar
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.home.HomeButtonModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ExportedFragment : BaseComposeFragment() {
    private val args: ExportedFragmentArgs by navArgs()

    private val shareImageUC: ShareImageUC by inject()

    override fun onScreenView() {
        super.onScreenView()
        logEvent("export_scr")
    }

    private fun handleEditAnotherClick() {
        logEvent("export_scr_edit_another_click")
        safeNavigate(AppGraphDirections.actionHome())
        when (args.eventName) {
            HomeButtonModel.Artistic.eventName -> safeNavigate(AppGraphDirections.actionArtistic())
            HomeButtonModel.Enhance.eventName -> safeNavigate(AppGraphDirections.actionEnhance())
            HomeButtonModel.RemoveBG.eventName -> safeNavigate(AppGraphDirections.actionRemoveBG())
            HomeButtonModel.Retouch.eventName -> safeNavigate(AppGraphDirections.actionRetouch())
            else ->
                safeNavigate(AppGraphDirections.actionGallery())
        }
    }

    private fun handleEditMoreClick() {
        logEvent("export_scr_edit_more_click")
        when(args.eventName){
            HomeButtonModel.Artistic.eventName,
            HomeButtonModel.Enhance.eventName,
            HomeButtonModel.RemoveBG.eventName,
            HomeButtonModel.Retouch.eventName -> safeNavigate(AppGraphDirections.actionExportToEdit(imageUri = args.imageUri))
            else -> safePopBackstack()
        }
    }


    @Composable
    override fun ComposeView() {
        ExportedScreen(
            imageUri = args.imageUri,
            interaction = ExportedInteraction(
                onBackClick = {
                    logEvent("export_scr_back_click")
                    safePopBackstack()
                },
                onHomeClick = {
                    logEvent("export_scr_home_click")
                    safeNavigate(AppGraphDirections.actionHome())
                },
                onEditAnotherClick = ::handleEditAnotherClick,
                onEditMoreClick = ::handleEditMoreClick,
                onShareClick = {
                    logEvent("export_scr_share_click")
                    lifecycleScope.launch {
                        shareImageUC(
                            context = requireContext(),
                            imageUri = args.imageUri,
                            shareTarget = it,
                            onFailure = {
                                runOnUiThread { toast(it.message.orEmpty()) }
                            }
                        )
                    }
                },
            )
        )
    }
}

@Composable
private fun ExportedScreen(
    imageUri: Uri,
    interaction: ExportedInteraction = ExportedInteraction(),
) {
    BackHandler { interaction.onBackClick() }

    BaseScreen(
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.image_saved),
                onBackClick = interaction.onBackClick,
                rightContent = {
                    CenterColumn(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .height(48.dp)
                            .rippleClickable {
                                interaction.onHomeClick()
                            }
                            .padding(horizontal = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_home),
                            contentDescription = stringResource(R.string.home),
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )

                        Text(
                            text = stringResource(R.string.home),
                            style = medium12,
                            color = Color.Black,
                        )
                    }
                },
                modifier = Modifier.background(Color.White)
            )
        },
        bottomBar = {
            val baseAdmobLayoutId =
                if (RemoteConfig.adsLayoutConfig.admob_layout_cta_above_native_save) R.layout.native_ads_large_above
                else R.layout.native_ads_large
            val baseMetaLayoutId =
                if (RemoteConfig.adsLayoutConfig.meta_layout_cta_above_native_save) R.layout.native_ads_large_meta_above
                else R.layout.native_ads_large_meta
            val admobLayoutId =
                if (RemoteConfig.adsLayoutConfig.force_meta_layout_save) baseMetaLayoutId else baseAdmobLayoutId
            val metaLayoutId =
                if (RemoteConfig.adsLayoutConfig.force_admob_layout_save) baseAdmobLayoutId else baseMetaLayoutId

            NativeAdContent(
                adGroup = AdsProvider.nativeSaved,
                layoutId = admobLayoutId,
                metaLayoutId = metaLayoutId,
                reloadOnLifecycleEvent = Lifecycle.Event.ON_RESUME,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlternativeColor, rounded(10.dp))
            )
        },
        backgroundColor = SubColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            SubcomposeAsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .rounded(16.dp)
                    .background(Color(0xFFF0F0F0)),
            )

            CenterRow(modifier = Modifier.padding(top = 10.dp), itemSpacing = 10.dp) {
                CenterRow(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .rounded(5.dp)
                        .clickable { interaction.onEditAnotherClick() }
                        .background(Color(0xFF232323))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_camera),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )

                    Text(
                        text = stringResource(R.string.select_another),
                        style = extraBold14,
                        color = Color.White,
                        maxLines = 1,
                    )
                }

                CenterRow(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .rounded(5.dp)
                        .clickable { interaction.onEditMoreClick() }
                        .background(Color.White)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_photo),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )

                    Text(
                        text = stringResource(R.string.edit),
                        style = extraBold14,
                        color = Color.Black,
                        maxLines = 1,
                    )
                }
            }

            Text(
                text = stringResource(R.string.share_to),
                style = extraBold12,
                color = Color.Black,
                modifier = Modifier.padding(top = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                ShareTarget.entries.forEachIndexed { index, target ->
                    CenterColumn(
                        modifier = Modifier
                            .weight(if (index == ShareTarget.entries.size - 1) 0.7f else 1f)
                            .rippleClickable { interaction.onShareClick(target) },
                        itemSpacing = 4.dp
                    ) {
                        Icon(
                            painter = painterResource(target.iconId),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )

                        Text(
                            text = stringResource(target.titleId),
                            style = extraBold12,
                            color = Color.Black,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ExportedScreen(
        imageUri = Uri.EMPTY,
    )
}