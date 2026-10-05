package com.photo.editor.ui.compositionlocal

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import java.util.Locale

val LocalLocale = staticCompositionLocalOf { Locale.getDefault() }
val LocalNavController = staticCompositionLocalOf<NavController?> { null }