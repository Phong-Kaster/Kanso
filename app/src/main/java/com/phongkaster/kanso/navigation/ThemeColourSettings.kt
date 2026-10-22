package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The theme colour setting, opened from the Settings menu: Kanso blue, wallpaper colours or a picked colour. */
@Serializable
data object ThemeColourSettings : NavKey
