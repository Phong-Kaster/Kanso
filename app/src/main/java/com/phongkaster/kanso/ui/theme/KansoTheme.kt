package com.phongkaster.kanso.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
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
 * Kanso's theme, built on Material 3 Expressive: wraps [content] so every Material component
 * inside it takes its colours from the light or dark scheme that [themeMode] selects, in the
 * colours that [themeColour] selects, and moves with Material's expressive motion.
 *
 * [ThemeMode.System] follows the phone's dark-theme setting, [ThemeMode.Light] and
 * [ThemeMode.Dark] force one scheme. [ThemeColour.KansoBlue] (the default, on every Android
 * version) is the scheme generated from [KANSO_BLUE_SEED]; [ThemeColour.Wallpaper] uses the
 * phone's wallpaper colours (Android 12 and later; Kanso blue below), and [ThemeColour.Custom]
 * a scheme generated from the picked seed colour. Because both are parameters, passing a new
 * value re-draws everything below with the new colours at once, with no restart.
 *
 * What `MaterialExpressiveTheme` is: Material 3 Expressive is the newer, livelier style of
 * Material 3 (bolder shapes, emphasized text styles, bouncier movement). `MaterialExpressiveTheme`
 * is the theme function that switches it on. Like the plain Material 3 theme it hands a colour
 * scheme, shapes and typography down the tree, and it also hands down a motion scheme; where a
 * value is not given, it falls back to Material's defaults (for colours and motion, the
 * expressive ones). Kanso gives it its own colours, the expressive motion and a type scale (see
 * the next paragraph), and leaves the shapes to the default, whose shape scale already includes
 * the larger expressive corner sizes.
 *
 * What the typography is: a `Typography` is the app's type scale, the set of named text styles
 * (`titleLarge`, `headlineSmall`, `bodyMedium` and so on) that every Text and every component
 * reads through `MaterialTheme.typography`. Material 3 Expressive adds an `…Emphasized` twin to
 * each style, a bolder version for the words that matter most on a screen; Kanso's screens use
 * them for their titles. Kanso passes `Typography(fontFamily = FontFamily.SansSerif)` instead of
 * leaving the type scale to the theme because of a quirk of the library: the theme's fallback is
 * `Typography()` with no arguments, and with no arguments Kotlin picks the older constructor of
 * `Typography`, which only knows the regular styles and fills each `…Emphasized` style with a
 * copy of its regular twin, so an emphasized title would look exactly like a regular one. The
 * constructor that takes a font family builds every style, the emphasized ones included, from
 * Material's own design values (its "tokens"), drawn in the font family it is given. SansSerif
 * is the font those design values name anyway, so the regular styles come out exactly as
 * before; only the emphasized ones change, to their heavier weight.
 *
 * What the `MotionScheme` is: one set of animation settings that every Material component reads
 * when it moves (a button changing shape when pressed, a toolbar item becoming selected), so all
 * motion in the app feels the same. `MotionScheme.expressive()` makes those animations springs:
 * a spring has no fixed duration, it pulls the value towards its target with a given stiffness
 * and lets it overshoot a little and settle (its damping), which reads as lively and physical.
 * The standard scheme's springs are stiffer and barely overshoot. Components read the scheme
 * with `MaterialTheme.motionScheme`, for example `MaterialTheme.motionScheme.defaultSpatialSpec()`
 * for an animation that moves or resizes something, and Kanso's own animations can do the same.
 *
 * In Material3 1.5.0-beta01 `MaterialExpressiveTheme` and `MotionScheme.expressive()` are stable,
 * so this function needs no opt-in; only components that are still experimental (such as the
 * loading indicator) need `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` where they are used.
 */
@Composable
fun KansoTheme(
    themeMode: ThemeMode,
    themeColour: ThemeColour = ThemeColour.KansoBlue,
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

    // MaterialExpressiveTheme hands the colour scheme, the springy expressive motion scheme and
    // the type scale down the tree; any composable below reads them with
    // MaterialTheme.colorScheme, MaterialTheme.motionScheme and MaterialTheme.typography. Shapes
    // are not passed, so Material's default shape scale applies.
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        // The constructor with a font family gives the emphasized styles their own, bolder
        // design values; Typography() with no arguments would copy the regular ones into them
        // (see the KDoc above). SansSerif is Material's own font, so regular text is unchanged.
        typography = Typography(fontFamily = FontFamily.SansSerif),
    ) {
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
 * `dynamicDarkColorScheme`, Android 12 and later only); below Android 12, and for the Kanso
 * blue choice, Kanso blue is used; a custom colour is turned into a full scheme by
 * [seedColorScheme].
 */
@Composable
private fun colorSchemeFor(themeColour: ThemeColour, dark: Boolean): ColorScheme {
    val context = LocalContext.current
    return when (themeColour) {
        ThemeColour.Wallpaper ->
            if (isWallpaperColourAvailable()) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                rememberKansoBlueScheme(dark)
            }
        ThemeColour.KansoBlue -> rememberKansoBlueScheme(dark)
        // remember keeps a value across re-draws (recompositions) of this composable, and works
        // it out again only when one of its keys changes. Generating a scheme takes real work,
        // so it is done once per seed and light/dark look, not on every frame.
        is ThemeColour.Custom -> remember(themeColour.seedArgb, dark) {
            seedColorScheme(Color(themeColour.seedArgb), dark)
        }
    }
}

/**
 * Returns Kanso blue, the scheme generated from [KANSO_BLUE_SEED], in the [dark] or light look.
 * It is generated once per look and remembered (see `remember` in [colorSchemeFor]).
 */
@Composable
private fun rememberKansoBlueScheme(dark: Boolean): ColorScheme =
    remember(dark) { seedColorScheme(KANSO_BLUE_SEED, dark) }

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

/**
 * A sample of Kanso's colours, in Kanso blue: a few common Material 3 components (a title, a card
 * with text, a filled and an outlined button, a switch on and off) drawn on the theme, once in
 * light and once in dark.
 *
 * `@PreviewLightDark` asks Android Studio for two pictures, one with the phone in day mode and
 * one in night mode, and `ThemeMode.System` makes KansoTheme follow that setting. In the preview
 * pane the view is in "edit mode", so KansoTheme skips styling the system bars (there is no real
 * window there to style). The Surface gives the components the theme's background to sit on, as
 * a real screen's Scaffold does.
 */
@PreviewLightDark
@Composable
private fun KansoThemePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Kanso", style = MaterialTheme.typography.headlineSmall)
                Card {
                    Text(
                        "Cards sit on the surface colours of the scheme.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}, shapes = ButtonDefaults.shapes()) { Text("Save") }
                    OutlinedButton(onClick = {}, shapes = ButtonDefaults.shapes()) { Text("Retake") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = true, onCheckedChange = {})
                    Switch(checked = false, onCheckedChange = {})
                }
            }
        }
    }
}
