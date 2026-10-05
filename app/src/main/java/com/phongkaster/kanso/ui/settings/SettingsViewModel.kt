package com.phongkaster.kanso.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the Settings screen's state: the saved choices it shows and the actions that change them.
 *
 * Like every screen ViewModel in Kanso, Koin builds it and the Settings navigation entry owns it,
 * so it survives rotation and is cleared when the user leaves Settings. Further settings get one
 * StateFlow and one setter each here, beside the theme mode and theme colour.
 */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    /**
     * The saved theme mode, as state the screen can read at any moment.
     *
     * A StateFlow is a Flow that always holds one current value. stateIn turns the repository's
     * Flow into one: it collects the Flow in viewModelScope (a scope cancelled when this
     * ViewModel is cleared), starts at once (Eagerly), and shows System until the file is read.
     */
    val themeMode: StateFlow<ThemeMode> = repository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.System,
    )

    /**
     * The saved theme colour (wallpaper, baseline or a custom seed), as state the screen can
     * read at any moment; it shows this phone's default until the file is read.
     */
    val themeColour: StateFlow<ThemeColour> = repository.themeColour.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = repository.defaultThemeColour,
    )

    /** Saves [mode] as the theme mode; the whole app re-themes as soon as it is written. */
    fun setThemeMode(mode: ThemeMode) {
        // launch starts the write in the background so the tap never blocks the screen.
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    /** Takes the app's colours from the wallpaper (offered on Android 12 and later only). */
    fun onWallpaperChosen() {
        viewModelScope.launch { repository.setThemeColour(ThemeColour.Wallpaper) }
    }

    /** Uses the built-in baseline purple colours (offered below Android 12 instead of wallpaper). */
    fun onBaselineChosen() {
        viewModelScope.launch { repository.setThemeColour(ThemeColour.Baseline) }
    }

    /**
     * Selects the Custom option with the last picked colour (or the baseline purple), which
     * also shows the colour picker.
     */
    fun onCustomChosen() {
        viewModelScope.launch { repository.chooseCustomThemeColour() }
    }

    /**
     * Uses [color] as the custom seed colour: the colour picker calls this on every change, and
     * the whole app re-themes from it as soon as it is saved. Its transparency is dropped,
     * since a colour scheme is built from an opaque colour.
     */
    fun onCustomColourChosen(color: Color) {
        val seedArgb = color.copy(alpha = 1f).toArgb()
        viewModelScope.launch { repository.setThemeColour(ThemeColour.Custom(seedArgb)) }
    }
}
