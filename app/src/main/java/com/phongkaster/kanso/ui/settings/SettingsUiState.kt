package com.phongkaster.kanso.ui.settings

import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode

/**
 * Everything the Settings screen shows at one moment: the saved [themeMode] and the saved
 * [themeColour] (wallpaper, baseline purple or a custom seed colour). It is Settings' UiState:
 * for what a UiState is and how the screen reads it, see HomeUiState.
 *
 * Whether the phone offers wallpaper colours (Android 12 and later) is not in here: it is a fact
 * about the phone, not a saved choice, so the drawing side asks for it itself.
 */
data class SettingsUiState(val themeMode: ThemeMode, val themeColour: ThemeColour)
