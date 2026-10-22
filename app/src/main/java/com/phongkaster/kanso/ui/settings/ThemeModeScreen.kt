package com.phongkaster.kanso.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag on the root of the Theme mode screen. */
const val THEME_MODE_SCREEN_TAG = "theme-mode-screen"

/**
 * The Theme mode setting on a screen of its own, opened from the Settings menu: three rows,
 * System default, Light and Dark, with the saved one marked. A tap saves the mode and the whole
 * app changes at once; Back returns to the menu.
 *
 * This is the stateful half (see `HomeScreen` for the split): it reads the saved choices from
 * [viewModel] (the same [SettingsViewModel] as the menu; Navigation 3 gives each screen its own
 * instance, and all of them read the one settings file) and hands them to [ThemeModeContent].
 * [onBack] is the Back button.
 */
@Composable
fun ThemeModeScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    ThemeModeContent(
        state = state,
        onThemeModeSelected = viewModel::setThemeMode,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * The stateless part of the Theme mode screen: the framed screen (see [SettingsDetailScreen])
 * holding [ThemeModeChoice] with the mode of [state] marked; a tap on a row calls
 * [onThemeModeSelected] with that mode.
 */
@Composable
fun ThemeModeContent(
    state: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDetailScreen(
        title = stringResource(R.string.theme_mode),
        onBack = onBack,
        testTag = THEME_MODE_SCREEN_TAG,
        modifier = modifier,
    ) {
        ThemeModeChoice(selected = state.themeMode, onSelect = onThemeModeSelected)
    }
}

/** The Theme mode screen with Dark chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun ThemeModeContentPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.background) {
                ThemeModeContent(
                    state = SettingsUiState(themeMode = ThemeMode.Dark, themeColour = ThemeColour.KansoBlue),
                    onThemeModeSelected = {},
                    onBack = {},
                )
            }
        }
    }
}
