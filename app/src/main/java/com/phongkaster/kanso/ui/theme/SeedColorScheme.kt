package com.phongkaster.kanso.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.dynamicColorScheme

/**
 * The number behind Kanso blue's seed, in ARGB form: alpha FF (fully opaque), red 1A, green 73,
 * blue E8. A hex literal this large does not fit in an Int, so Kotlin makes it a Long, which is
 * the form Compose's `Color` function takes for a 32-bit ARGB number.
 *
 * It is private: the rest of the app uses [KANSO_BLUE_SEED], the same colour as a Compose `Color`.
 */
private const val KANSO_BLUE_SEED_ARGB = 0xFF1A73E8

/**
 * Kanso blue's seed: the one colour Kanso's own colour scheme grows from, a clear, fully opaque
 * blue. "Kanso blue" is the scheme [seedColorScheme] makes from it, in the light or dark look.
 * The seed is the colour generator's input, not a colour any screen paints.
 *
 * This is the only place the colour is written down (its number is [KANSO_BLUE_SEED_ARGB], just
 * above). Everything else that needs it (the theme, the settings that start a custom colour from
 * it, the tests) refers to this constant, so changing Kanso's colour is a one-line change. Where
 * a plain ARGB number is needed, as in the settings file, `KANSO_BLUE_SEED.toArgb()` gives it.
 */
val KANSO_BLUE_SEED: Color = Color(KANSO_BLUE_SEED_ARGB)

/**
 * Builds a complete Material 3 colour scheme, light or [dark], from one [seed] colour.
 *
 * The MaterialKolor library runs the same algorithm Android uses for wallpaper colours: it
 * derives tonal palettes (one hue in many lightness steps) for primary, secondary, tertiary,
 * neutral and error from the seed, then fills every colour role of the scheme (primary,
 * onPrimary, containers, surfaces, surface containers, outline, inverse colours and so on)
 * from those palettes, so each "on" colour stays readable on the colour it sits on. It is a
 * pure calculation: same seed, same scheme, and nothing is fetched from the network.
 *
 * Kanso blue is `seedColorScheme(KANSO_BLUE_SEED, dark)`; a custom colour picked in Settings
 * goes through the same function. The seed's transparency is ignored.
 *
 * With [exactPrimary] false (Kanso blue) `primary`, the colour of buttons and switches, is the
 * generated one: a fixed shade of the seed's palette (lighter in dark mode, and softer than the
 * seed), as Material 3 normally does. With it true (a colour the person picked) `primary` is the
 * picked colour itself, in light and dark, so a button is exactly the hex that was chosen. Its
 * text colour, `onPrimary`, is then white when white has a contrast ratio of at least 4 against
 * the colour (the usual minimum for small text, WCAG AA), and near-black otherwise, so labels stay
 * readable on any pick, including very light ones. `surfaceTint` follows `primary`, as Material
 * defines it. Containers, surfaces and the other roles keep their generated colours. (A pick
 * too dark to stand out on the dark tab bar is handled there: see `KansoTabBarColours`.)
 */
fun seedColorScheme(seed: Color, dark: Boolean, exactPrimary: Boolean = false): ColorScheme {
    val opaque = seed.copy(alpha = 1f)
    val generated = dynamicColorScheme(seedColor = opaque, isDark = dark)
    if (!exactPrimary) return generated
    return generated.copy(
        primary = opaque,
        onPrimary = if (contrastRatio(Color.White, opaque) >= MIN_TEXT_CONTRAST) Color.White else NEAR_BLACK,
        surfaceTint = opaque,
    )
}

/** The smallest contrast ratio at which white text counts as readable on a picked colour (WCAG AA). */
private const val MIN_TEXT_CONTRAST = 4f

/** The text colour on a picked colour that is too light for white text. */
private val NEAR_BLACK = Color(0xFF1B1B1B)

/**
 * Returns the WCAG contrast ratio of two opaque colours: (lighter luminance + 0.05) divided by
 * (darker luminance + 0.05), from 1 (identical) to 21 (black on white). `luminance()` is
 * Compose's relative luminance, the measure the formula is defined with.
 */
internal fun contrastRatio(a: Color, b: Color): Float {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
