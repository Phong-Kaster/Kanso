package com.phongkaster.kanso.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.settings.SettingsRepository
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
 * StateFlow and one setter each here, beside the theme mode.
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

    /** Saves [mode] as the theme mode; the whole app re-themes as soon as it is written. */
    fun setThemeMode(mode: ThemeMode) {
        // launch starts the write in the background so the tap never blocks the screen.
        viewModelScope.launch { repository.setThemeMode(mode) }
    }
}
