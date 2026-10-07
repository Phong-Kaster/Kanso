package com.phongkaster.kanso.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme

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
 * The seed's transparency is ignored.
 */
fun seedColorScheme(seed: Color, dark: Boolean): ColorScheme =
    dynamicColorScheme(seedColor = seed.copy(alpha = 1f), isDark = dark)

/**
 * Returns the built-in Material 3 baseline colour scheme (primary purple `#6750A4` in light),
 * light or [dark]: the scheme used when no other colour source applies.
 */
fun baselineColorScheme(dark: Boolean): ColorScheme =
    if (dark) darkColorScheme() else lightColorScheme()
