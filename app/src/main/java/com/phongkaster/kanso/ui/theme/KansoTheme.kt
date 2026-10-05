package com.phongkaster.kanso.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.phongkaster.kanso.data.settings.ThemeMode

/**
 * Kanso's Material 3 theme: wraps [content] so every Material component inside it takes its
 * colours from the light or dark scheme that [themeMode] selects.
 *
 * [ThemeMode.System] follows the phone's dark-theme setting, [ThemeMode.Light] and
 * [ThemeMode.Dark] force one scheme. Because the mode is a parameter, passing a new value
 * re-draws everything below with the new colours at once, with no restart.
 */
@Composable
fun KansoTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = isDarkTheme(themeMode)
    val colorScheme = colorSchemeFor(dark)

    // The status and navigation bars sit on top of the app's own background (the app draws edge
    // to edge), so their icons, and the navigation bar's scrim on older Android versions, must
    // follow the app's choice, not the phone's night setting.
    val view = LocalView.current
    if (!view.isInEditMode) {
        // DisposableEffect(dark) runs its block once the screen is updated, and again only when
        // `dark` changes: the place to push Compose state out to code that is not Compose, such
        // as the window. There is nothing to undo, so onDispose is empty.
        DisposableEffect(dark) {
            (view.context.findActivity() as? ComponentActivity)?.applySystemBarStyle(dark)
            onDispose { }
        }
    }

    // MaterialTheme hands the colour scheme down the tree; any composable below reads it with
    // MaterialTheme.colorScheme.
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Tells whether [themeMode] means the dark scheme right now, asking the phone for System. */
@Composable
private fun isDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    // isSystemInDarkTheme reads the phone's current dark-theme setting from the configuration.
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/**
 * Returns the colour scheme for the dark or light look. These are plain Material 3 defaults
 * for now; a colour choice can later build them from a seed colour here.
 */
private fun colorSchemeFor(dark: Boolean): ColorScheme =
    if (dark) darkColorScheme() else lightColorScheme()

/**
 * Re-applies edge-to-edge drawing with bars styled for the app's [dark] choice: transparent
 * status bar with light or dark icons, and on Android versions that need one (below 10) a
 * navigation bar scrim in the matching colour, so the icons always stay readable.
 */
private fun ComponentActivity.applySystemBarStyle(dark: Boolean) {
    // SystemBarStyle.auto picks the light or dark variant by calling the last lambda; returning
    // the app's own `dark` makes the bars follow Kanso's theme mode instead of the phone's.
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(
            AndroidColor.TRANSPARENT,
            AndroidColor.TRANSPARENT,
        ) { dark },
        navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
    )
}

/** The navigation bar scrim on the light scheme: the same mostly-opaque white the library uses. */
private val LIGHT_SCRIM = 0xE6FFFFFF.toInt()

/** The navigation bar scrim on the dark scheme: the same half-opaque near-black the library uses. */
private val DARK_SCRIM = 0x801B1B1B.toInt()

/** Walks up the chain of wrapped contexts to the activity hosting this view, if there is one. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
