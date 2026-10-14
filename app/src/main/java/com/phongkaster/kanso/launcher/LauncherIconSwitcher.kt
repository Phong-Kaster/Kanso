package com.phongkaster.kanso.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.isWallpaperColourAvailable

/**
 * Makes the launcher show the [LauncherIcon] that matches the theme colour.
 *
 * Each icon is a launcher entry (an `activity-alias`) that can be switched on and off with
 * PackageManager. Switching one on and the others off makes the launcher redraw Kanso's icon in
 * that colour, usually within a few seconds. Kanso blue is the one enabled in the manifest.
 */
class LauncherIconSwitcher(private val context: Context) {

    /**
     * Returns the icon for [themeColour]: Kanso blue for Kanso blue, and for a custom colour or
     * the wallpaper's colours the icon closest to that colour (see [LauncherIcon.closestTo]).
     */
    fun iconFor(themeColour: ThemeColour): LauncherIcon = when (themeColour) {
        ThemeColour.KansoBlue -> LauncherIcon.KansoBlue
        is ThemeColour.Custom -> LauncherIcon.closestTo(themeColour.seedArgb)
        // system_accent1_500 is the middle tone of the wallpaper's main accent colour, the one
        // the wallpaper colour scheme is built around (Android 12 and later).
        ThemeColour.Wallpaper ->
            if (isWallpaperColourAvailable()) {
                LauncherIcon.closestTo(context.getColor(android.R.color.system_accent1_500))
            } else {
                LauncherIcon.KansoBlue
            }
    }

    /**
     * Enables [icon]'s launcher entry and disables every other one. The new entry is enabled
     * first, so the launcher never has a moment with no Kanso icon at all. Entries already in the
     * wanted state are left alone, so an unchanged colour does not make the launcher redraw.
     *
     * DONT_KILL_APP keeps Kanso running: without it, Android stops the app whenever one of its
     * components is switched on or off.
     */
    fun show(icon: LauncherIcon) {
        val packageManager = context.packageManager
        LauncherIcon.entries.sortedBy { it != icon }.forEach { entry ->
            val component = ComponentName(context.packageName, entry.aliasClassName)
            val wanted = entry == icon
            if (isEnabled(packageManager, component, entry) != wanted) {
                packageManager.setComponentEnabledSetting(
                    component,
                    if (wanted) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }

    /**
     * Tells whether [component] is enabled now. "Default" means it was never switched, so the
     * manifest decides, and the manifest enables only Kanso blue's entry.
     */
    private fun isEnabled(
        packageManager: PackageManager,
        component: ComponentName,
        entry: LauncherIcon,
    ): Boolean = when (packageManager.getComponentEnabledSetting(component)) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> entry == LauncherIcon.KansoBlue
        else -> false
    }
}
