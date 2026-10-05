package com.photo.editor.ui.screen.settings

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.photo.editor.R
import com.photo.editor.domain.model.Language
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.component.selector.LanguageSelector
import com.photo.editor.ui.component.topbar.CommonTopBar
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.common.ui.theme.PrimaryColor
import com.photo.editor.common.util.logEvent
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.platform.notification.AppNotificationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LanguageSettingsFragment : BaseComposeFragment() {
    private val viewModel: LanguageSettingsViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logEvent("language_settings_scr")
    }

    @Composable
    override fun ComposeView() {
        LanguageSettingsScreen(
            selectedLanguage = viewModel.selectedLanguage,
            onSelectedLanguageChange = { viewModel.selectedLanguage = it },
            onConfirm = {
                lifecycleScope.launch {
                    viewModel.onConfirmLanguage()
                    delay(500)
                    requireActivity().recreate()
                    AppNotificationManager.sendShortcutNotification(requireActivity())
                    safePopBackstack()
                }
            },
            onBackClick = {
                safePopBackstack()
            }
        )
    }
}

@Composable
private fun LanguageSettingsScreen(
    selectedLanguage: Language,
    onSelectedLanguageChange: (Language) -> Unit,
    onBackClick: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    BaseScreen(
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.language),
                onBackClick = { onBackClick() },
                rightContent = {
                    IconButton(onClick = onConfirm, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = stringResource(R.string.confirm),
                            tint = PrimaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BaseBannerAdContent(bannerAdGroup = AdsProvider.bannerAll)
        },
    ) {
        LanguageSelector(
            languages = Language.getSortedList(),
            selectedLanguage = selectedLanguage,
            onSelectLanguage = onSelectedLanguageChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    LanguageSettingsScreen(selectedLanguage = Language.English, onSelectedLanguageChange = {})
}