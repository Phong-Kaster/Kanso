package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The theme mode setting, opened from the Settings menu: choose System default, Light or Dark. */
@Serializable
data object ThemeModeSettings : NavKey
