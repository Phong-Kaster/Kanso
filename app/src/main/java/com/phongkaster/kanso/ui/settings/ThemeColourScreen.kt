package com.phongkaster.kanso.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.data.settings.isWallpaperColourAvailable
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag on the root of the Theme colour screen. */
const val THEME_COLOUR_SCREEN_TAG = "theme-colour-screen"

/**
 * The Theme colour setting on a screen of its own, opened from the Settings menu: the colour rows
 * (Kanso blue, From wallpaper on Android 12 and later, Custom colour) and, while Custom is
 * chosen, the colour picker below them. Having the whole screen to itself, the picker and its
 * hex box can be reached and typed into without the rest of Settings in the way; the screen keeps
 * clear of the keyboard and scrolls. Back returns to the menu.
 *
 * This is the stateful half (see `HomeScreen` for the split): it reads the saved choices from
 * [viewModel] and the phone's wallpaper colour (null below Android 12), and hands them to
 * [ThemeColourContent]. [onBack] is the Back button.
 */
@Composable
fun ThemeColourScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val wallpaperColour = if (isWallpaperColourAvailable()) {
        dynamicLightColorScheme(context).primary
    } else {
        null
    }
    ThemeColourContent(
        state = state,
        wallpaperColour = wallpaperColour,
        onKansoBlueChosen = viewModel::onKansoBlueChosen,
        onWallpaperChosen = viewModel::onWallpaperChosen,
        onCustomChosen = viewModel::onCustomChosen,
        onCustomColourChosen = viewModel::onCustomColourChosen,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * The stateless part of the Theme colour screen: the framed screen (see [SettingsDetailScreen])
 * holding [ThemeColourChoice] for the colour of [state], with a swatch on each row;
 * [wallpaperColour] is the wallpaper row's swatch and null leaves that row out. The callbacks are
 * the row taps and every colour picked in the picker.
 */
@Composable
fun ThemeColourContent(
    state: SettingsUiState,
    wallpaperColour: Color?,
    onKansoBlueChosen: () -> Unit,
    onWallpaperChosen: () -> Unit,
    onCustomChosen: () -> Unit,
    onCustomColourChosen: (Color) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDetailScreen(
        title = stringResource(R.string.theme_colour_title),
        onBack = onBack,
        testTag = THEME_COLOUR_SCREEN_TAG,
        modifier = modifier,
    ) {
        ThemeColourChoice(
            selected = state.themeColour,
            customSeedArgb = state.customSeedArgb,
            wallpaperColour = wallpaperColour,
            onKansoBlue = onKansoBlueChosen,
            onWallpaper = onWallpaperChosen,
            onCustom = onCustomChosen,
            onCustomColourChanged = onCustomColourChosen,
        )
    }
}

/** A custom colour for the preview: a deep green, #2E7D32, as an opaque ARGB integer. */
private val sampleCustomColour = ThemeColour.Custom(seedArgb = 0xFF2E7D32.toInt())

/** A wallpaper colour for the preview: a warm amber, #8B5000; the real one comes from the phone. */
private val sampleWallpaperColour = Color(0xFF8B5000)

/** The Theme colour screen with a custom colour chosen, so the picker shows, in light and in dark. */
@PreviewLightDark
@Composable
private fun ThemeColourContentPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.background) {
                ThemeColourContent(
                    state = SettingsUiState(themeMode = ThemeMode.System, themeColour = sampleCustomColour),
                    wallpaperColour = sampleWallpaperColour,
                    onKansoBlueChosen = {},
                    onWallpaperChosen = {},
                    onCustomChosen = {},
                    onCustomColourChosen = {},
                    onBack = {},
                )
            }
        }
    }
}
