package com.phongkaster.kanso.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.phongkaster.kanso.R

/**
 * The three tabs of the floating tab bar, in the order the bar shows them, left to right: Home,
 * My edits and Settings.
 *
 * A tab is a top-level place in the app that is always one tap away, whatever the person was
 * doing on another tab. Each tab has the string resource of its label ([labelRes]), the drawable
 * resource of its icon ([iconRes]) and [key], the Navigation 3 key of the screen it shows. The
 * key is how a tab lives on the back stack: choosing a tab puts its key on top of [Home] (see
 * `selectTab` in BackStackActions.kt), and the bar shows the tab whose key is on top as selected.
 *
 * The icons are Material Symbols Rounded vector drawables, drawn with `Icon(painterResource(...))`
 * so they take the bar's colours instead of the white the files paint with.
 */
enum class KansoTab(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int, val key: NavKey) {
    /** Home: the tools, at the bottom of the back stack. */
    HOME(R.string.tab_home, R.drawable.ic_home, Home),

    /** My edits: the photos Kanso has saved. */
    MY_EDITS(R.string.tab_my_edits, R.drawable.ic_photo_library, MyEdits),

    /** Settings: the theme mode and the theme colour. */
    SETTINGS(R.string.tab_settings, R.drawable.ic_settings, Settings),
}
