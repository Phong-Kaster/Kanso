package com.phongkaster.kanso.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.data.settings.isWallpaperColourAvailable

/** The test tag on the layout node KansoTheme wraps its content in. */
const val KANSO_THEME_TAG = "kanso-theme"

/**
 * A semantics property holding the colour scheme KansoTheme handed to its content.
 *
 * Semantics are the description of the screen that accessibility services and tests read.
 * Publishing the scheme there lets a test check the exact colours every screen is drawn with
 * (for example Home's `primary`), which no pixel of the screen shows exactly.
 */
val KansoColorSchemeKey = SemanticsPropertyKey<ColorScheme>("KansoColorScheme")

/** Writes the scheme into [KansoColorSchemeKey] of the semantics node it is set on. */
private var SemanticsPropertyReceiver.kansoColorScheme by KansoColorSchemeKey

/**
 * Kanso's Material 3 theme: wraps [content] so every Material component inside it takes its
 * colours from the light or dark scheme that [themeMode] selects, in the colours that
 * [themeColour] selects.
 *
 * [ThemeMode.System] follows the phone's dark-theme setting, [ThemeMode.Light] and
 * [ThemeMode.Dark] force one scheme. [ThemeColour.Wallpaper] uses the phone's wallpaper colours
 * (Android 12 and later, the baseline scheme below), [ThemeColour.Baseline] the Material 3
 * baseline purple, and [ThemeColour.Custom] a scheme generated from the picked seed colour.
 * Because both are parameters, passing a new value re-draws everything below with the new
 * colours at once, with no restart.
 */
@Composable
fun KansoTheme(
    themeMode: ThemeMode,
    themeColour: ThemeColour = ThemeColour.Baseline,
    content: @Composable () -> Unit,
) {
    val dark = isDarkTheme(themeMode)
    val colorScheme = colorSchemeFor(themeColour, dark)

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
    MaterialTheme(colorScheme = colorScheme) {
        // A Box that only passes its size on (propagateMinConstraints) and publishes the scheme
        // the content sees, read back from MaterialTheme, so tests can check it.
        val applied = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .testTag(KANSO_THEME_TAG)
                .semantics { kansoColorScheme = applied },
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
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
 * Returns the colour scheme for [themeColour] in the dark or light look.
 *
 * The wallpaper schemes come from the system (`dynamicLightColorScheme` and
 * `dynamicDarkColorScheme`, Android 12 and later only); everywhere else, and for the baseline
 * choice, the Material 3 baseline scheme is used; a custom colour is turned into a full scheme
 * by [seedColorScheme].
 */
@Composable
private fun colorSchemeFor(themeColour: ThemeColour, dark: Boolean): ColorScheme {
    val context = LocalContext.current
    return when (themeColour) {
        ThemeColour.Wallpaper ->
            if (isWallpaperColourAvailable()) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                baselineColorScheme(dark)
            }
        ThemeColour.Baseline -> baselineColorScheme(dark)
        // remember keeps a value across re-draws (recompositions) of this composable, and works
        // it out again only when one of its keys changes. Generating a scheme takes real work,
        // so it is done once per seed and light/dark look, not on every frame.
        is ThemeColour.Custom -> remember(themeColour.seedArgb, dark) {
            seedColorScheme(Color(themeColour.seedArgb), dark)
        }
    }
}

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
