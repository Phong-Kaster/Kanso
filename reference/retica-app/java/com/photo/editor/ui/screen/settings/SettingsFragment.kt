package com.photo.editor.ui.screen.settings

import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.MobileAds
import com.photo.editor.BuildConfig
import com.photo.editor.R
import com.photo.editor.common.Constants
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.medium14
import com.photo.editor.common.util.logEvent
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.topbar.CommonTopBar
import com.photo.editor.ui.compositionlocal.LocalNavController
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.util.UrlUtil.openUrl

class SettingsFragment : BaseComposeFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent("settings_scr")

        AdsProvider.disableAppResume()
    }

    override fun onDestroy() {
        super.onDestroy()

        AdsProvider.enableAppResume()
    }

    @Composable
    override fun ComposeView() {
        SettingsScreen(
            onBackClick = {
                safePopBackstack()
            },
        )
    }
}

@Composable
private fun SettingsScreen(
    onBackClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    BackHandler {
        onBackClick()
    }

    BaseScreen(
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.settings),
                onBackClick = {
                    onBackClick()
                },
                modifier = Modifier.background(Color.White)
            )
        },
        bottomBar = {
            BaseBannerAdContent(AdsProvider.bannerAll)
        },
        backgroundColor = SubColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .rounded(10.dp)
                    .background(Color.White),
            ) {
                SettingItem(
                    iconId = R.drawable.ic_settings_language,
                    title = stringResource(R.string.language)
                ) {
                    navController.safeNavigate(SettingsFragmentDirections.actionLanguageSettings())
                }

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F1F1), modifier = Modifier.padding(start = 48.dp))

                SettingItem(
                    iconId = R.drawable.ic_settings_privacy,
                    title = stringResource(R.string.privacy_policy)
                ) {
                    openUrl(context, Constants.PRIVACY_POLICY_URL)
                }

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F1F1), modifier = Modifier.padding(start = 48.dp))

                SettingItem(
                    iconId = R.drawable.ic_settings_tos,
                    title = stringResource(R.string.terms_of_service)
                ) {
                    openUrl(context, Constants.TERMS_OF_SERVICE_URL)
                }

                if (BuildConfig.DEBUG) {
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F1F1), modifier = Modifier.padding(start = 48.dp))

                    SettingItem(
                        iconId = R.drawable.ic_settings_privacy,
                        title = "Ad inspector"
                    ) {
                        MobileAds.openAdInspector(context) {  }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    iconId: Int,
    title: String,
    onClick: () -> Unit,
) {
    CenterRow(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        itemSpacing = 12.dp
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = Color.Black,
        )

        Text(
            text = title,
            style = medium14,
            color = Color.Black,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
private fun Preview() {
    SettingsScreen()
}