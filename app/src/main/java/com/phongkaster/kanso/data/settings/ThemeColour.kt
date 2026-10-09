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
     * Kanso blue, the app's own colours: a full light and dark scheme generated from one blue
     * seed colour (`KANSO_BLUE_SEED` in the theme package). It is what every fresh install
     * starts with, on every Android version, and it is offered in Settings on every version.
     *
     * A `data object` is a single value (there is only one Kanso blue) that still prints its
     * name and compares equal like a data class, which keeps tests and logs readable.
     */
    data object KansoBlue : ThemeColour

    /**
     * A colour the person picked: [seedArgb] is that colour as an ARGB integer (always fully
     * opaque), from which a complete light and dark colour scheme is generated.
     */
    data class Custom(val seedArgb: Int) : ThemeColour

    /** Values shared by every kind of theme colour. */
    companion object {
        /**
         * Returns the theme colour a fresh install starts with on a phone running Android API
         * level [sdkInt]: Kanso blue, on every version. The wallpaper colours stay one tap away
         * in Settings on Android 12 and later. The level is still asked for so the settings code
         * keeps one place to decide a default per Android version, should one ever differ.
         */
        fun defaultFor(sdkInt: Int): ThemeColour = KansoBlue
    }
}

/**
 * Tells whether this phone can take its colours from the wallpaper, which needs Android 12
 * (API 31) or later. The annotation lets Android's lint treat a call to it as an API check.
 */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun isWallpaperColourAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
