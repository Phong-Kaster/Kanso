package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The text size setting, opened from the Settings menu: the slider for Kanso's own text size. */
@Serializable
data object TextSizeSettings : NavKey
