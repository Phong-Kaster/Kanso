package com.phongkaster.kanso.data.settings

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Where the app takes its accent colours from.
 *
 * A "sealed" interface lists every possible kind of value in one place, so code that handles a
 * ThemeColour with `when` must handle each kind, and the compiler says so if one is missing.
 */
sealed interface ThemeColour {

    /**
     * Colours taken from the phone's wallpaper (Android 12 and later), which the system turns
     * into a colour scheme and changes when the wallpaper changes.
     */
    data object Wallpaper : ThemeColour

    /**
     * The built-in Material 3 baseline scheme, whose primary colour is purple `#6750A4`. It is
     * the default on phones older than Android 12, which cannot take colours from the wallpaper.
     */
    data object Baseline : ThemeColour

    /**
     * A colour the person picked: [seedArgb] is that colour as an ARGB integer (always fully
     * opaque), from which a complete light and dark colour scheme is generated.
     */
    data class Custom(val seedArgb: Int) : ThemeColour

    /** Values shared by every kind of theme colour. */
    companion object {
        /** The Material 3 baseline purple, the first colour the picker shows. */
        val BASELINE_SEED_ARGB: Int = 0xFF6750A4.toInt()

        /**
         * Returns the theme colour a fresh install starts with on a phone running Android API
         * level [sdkInt]: the wallpaper on Android 12 (API 31) and later, the baseline purple
         * below.
         */
        fun defaultFor(sdkInt: Int): ThemeColour =
            if (sdkInt >= Build.VERSION_CODES.S) Wallpaper else Baseline
    }
}

/**
 * Tells whether this phone can take its colours from the wallpaper, which needs Android 12
 * (API 31) or later. The annotation lets Android's lint treat a call to it as an API check.
 */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun isWallpaperColourAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
