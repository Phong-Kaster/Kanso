package com.phongkaster.kanso.ui.settings

import androidx.compose.ui.graphics.toArgb
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED

/**
 * Everything the Settings screen shows at one moment: the saved [themeMode], the saved
 * [themeColour] (Kanso blue, the wallpaper or a custom seed colour) and [customSeedArgb], the
 * last custom colour picked. It is Settings' UiState: for what a UiState is and how the screen
 * reads it, see HomeUiState. Before the settings file is read, SettingsViewModel starts it at
 * System with the repository's default colour, which is Kanso blue, and Kanso blue's seed as the
 * custom colour.
 *
 * [customSeedArgb] is a separate field because [themeColour] holds a seed only while Custom is
 * the choice. The settings file keeps the last picked seed while Kanso blue or the wallpaper is
 * chosen, and choosing Custom again brings it back, so the Custom row's swatch shows this field
 * whatever is selected: the colour a tap on Custom will give. It is an ARGB integer, as the file
 * keeps it, and Kanso blue's seed when no colour was ever picked. It has a default value so a
 * snapshot can be written with just the two choices, as the previews do.
 *
 * Whether the phone offers wallpaper colours (Android 12 and later) is not in here: it is a fact
 * about the phone, not a saved choice, so the drawing side asks for it itself.
 */
data class SettingsUiState(
    val themeMode: ThemeMode,
    val themeColour: ThemeColour,
    val customSeedArgb: Int = KANSO_BLUE_SEED.toArgb(),
)
