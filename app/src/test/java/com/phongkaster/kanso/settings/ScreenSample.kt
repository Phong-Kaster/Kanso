package com.phongkaster.kanso.settings

import androidx.compose.ui.graphics.Color

/**
 * What one drawing of the screen shows: the [background] colour near the bottom-left corner
 * (plain background on Home and Settings, never text) and the [ink] colour, the pixel inside a
 * text's bounds that differs most in luminance from that background, i.e. the text itself.
 */
internal class ScreenSample(val background: Color, val ink: Color)
