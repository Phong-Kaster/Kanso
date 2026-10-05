package com.phongkaster.kanso.ui.settings

import androidx.lifecycle.ViewModel

/**
 * Holds the Settings screen's state. It is empty for now; the settings themselves come later.
 *
 * Like every screen ViewModel in Kanso, Koin builds it and the Settings navigation entry owns it,
 * so it survives rotation and is cleared when the user leaves Settings.
 */
class SettingsViewModel : ViewModel()
