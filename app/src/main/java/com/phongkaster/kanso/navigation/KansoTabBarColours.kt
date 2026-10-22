package com.phongkaster.kanso.navigation

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.phongkaster.kanso.ui.theme.contrastRatio

/**
 * The colours of the floating tab bar, taken from the theme's colour roles so the bar follows the
 * app's colour choice.
 *
 * In the light theme the bar is the inverse of the screen: a dark `inverseSurface` pill with
 * `inverseOnSurface` tabs and an `inversePrimary` pill under the selected one, so it stands apart
 * from whatever scrolls under it.
 *
 * In the dark theme the inverse surface is a light grey, which glared against the dark screens
 * (the human's call, 2026-10-14: "too bright while I am on dark mode"). There the bar is the
 * theme's highest surface container instead: a dark grey a step above the background, set apart
 * by its shadow, with `onSurfaceVariant` tabs and the selected one on a `primary` pill.
 *
 * @property bar the bar's own colour.
 * @property onBar the unselected icons and labels.
 * @property pill the small pill under the selected tab.
 * @property onPill the selected tab's icon, drawn on [pill].
 * @property selectedLabel the selected tab's label, drawn on [bar].
 */
data class KansoTabBarColours(
    val bar: Color,
    val onBar: Color,
    val pill: Color,
    val onPill: Color,
    val selectedLabel: Color,
) {
    /** Builds the bar's colours from a colour scheme. */
    companion object {
        /** The contrast ratio the bar's icons and labels need against what they sit on (WCAG AA). */
        private const val READABLE = 4.5f

        /**
         * Returns [colour] if it has the contrast [READABLE] against [ground], else [colour] mixed
         * with white a tenth at a time until it has (white itself at the end).
         */
        private fun readableOn(ground: Color, colour: Color): Color {
            var mixed = colour
            var step = 0
            while (contrastRatio(mixed, ground) < READABLE && step < 10) {
                step++
                mixed = lerp(colour, Color.White, step / 10f)
            }
            return mixed
        }

        /** Returns white or near-black, whichever reads better on [ground]. */
        private fun readableText(ground: Color): Color =
            if (contrastRatio(Color.White, ground) >= contrastRatio(Color.Black, ground)) Color.White else Color.Black

        /**
         * Returns the bar's colours for [scheme]: the inverse look when the scheme is light, the
         * dark surface look when it is dark (its background's luminance is below one half).
         */
        fun of(scheme: ColorScheme): KansoTabBarColours =
            if (scheme.background.luminance() < 0.5f) {
                // A colour the person picked is used exactly as `primary`, and may be too dark to
                // stand out on the dark bar; the selected tab then gets a lightened copy of it.
                val accent = readableOn(scheme.surfaceContainerHighest, scheme.primary)
                KansoTabBarColours(
                    bar = scheme.surfaceContainerHighest,
                    onBar = scheme.onSurfaceVariant,
                    pill = accent,
                    onPill = if (contrastRatio(scheme.onPrimary, accent) >= READABLE) scheme.onPrimary else readableText(accent),
                    selectedLabel = accent,
                )
            } else {
                KansoTabBarColours(
                    bar = scheme.inverseSurface,
                    onBar = scheme.inverseOnSurface,
                    pill = scheme.inversePrimary,
                    onPill = scheme.inverseSurface,
                    selectedLabel = scheme.inversePrimary,
                )
            }
    }
}
