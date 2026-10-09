package com.phongkaster.kanso.data.settings

import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Reads and writes the user's saved settings.
 *
 * It is the only class that knows how settings are stored, so screens deal in plain values such
 * as [ThemeMode] and never in DataStore keys. New settings get a key and a Flow/setter pair here.
 *
 * [sdkInt] is the phone's Android API level, which decides whether a saved wallpaper choice can
 * be honoured (and where the default theme colour is decided); tests pass one in to act as an
 * older or newer phone.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {

    /**
     * The saved theme mode, emitted once at first and again every time it changes.
     *
     * A Flow is a stream of values over time: whoever collects it gets the current value and
     * then each new one. A missing or unrecognised stored value reads as [ThemeMode.System].
     */
    val themeMode: Flow<ThemeMode> = dataStore.data
        // catch handles an error raised while reading the file: a read failure (IOException)
        // counts as "nothing saved", so the defaults apply; any other error is a real bug and is
        // passed on.
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> parseThemeMode(preferences[THEME_MODE_KEY]) }
        // Other settings changing in the same file should not re-emit an unchanged theme mode.
        .distinctUntilChanged()

    /** Saves [mode] as the theme mode; collectors of [themeMode] then receive it. */
    suspend fun setThemeMode(mode: ThemeMode) {
        // edit { } reads the current file, applies the change and writes it back in one step.
        dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode.name }
    }

    /** The theme colour a fresh install starts with on this phone (see [ThemeColour.defaultFor]). */
    val defaultThemeColour: ThemeColour = ThemeColour.defaultFor(sdkInt)

    /**
     * The saved theme colour, emitted once at first and again every time it changes.
     *
     * Nothing saved, or a value that cannot be read, gives [defaultThemeColour] (Kanso blue).
     * A saved Wallpaper choice on a phone older than Android 12 (possible only through a
     * restored backup) reads as [ThemeColour.KansoBlue], since the wallpaper colours do not
     * exist there; so does the first default colour an older Kanso saved, which is gone.
     */
    val themeColour: Flow<ThemeColour> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            parseThemeColour(
                source = preferences[THEME_COLOUR_SOURCE_KEY],
                seed = preferences[THEME_COLOUR_SEED_KEY],
            )
        }
        .distinctUntilChanged()

    /**
     * Saves [colour] as the theme colour. For [ThemeColour.Custom] the seed colour is saved too;
     * for the other kinds the last picked seed is kept, so picking Custom again starts from it.
     */
    suspend fun setThemeColour(colour: ThemeColour) {
        dataStore.edit { preferences ->
            when (colour) {
                ThemeColour.Wallpaper -> {
                    preferences[THEME_COLOUR_SOURCE_KEY] = SOURCE_WALLPAPER
                }
                ThemeColour.KansoBlue -> {
                    preferences[THEME_COLOUR_SOURCE_KEY] = SOURCE_KANSO_BLUE
                }
                is ThemeColour.Custom -> {
                    preferences[THEME_COLOUR_SOURCE_KEY] = SOURCE_CUSTOM
                    preferences[THEME_COLOUR_SEED_KEY] = colour.seedArgb
                }
            }
        }
    }

    /**
     * Switches the theme colour to Custom with the last picked seed colour, or Kanso blue's
     * seed when none was ever picked, so the picker opens on the colour the app already shows.
     * Used when the person selects the Custom option before touching the picker.
     */
    suspend fun chooseCustomThemeColour() {
        dataStore.edit { preferences ->
            preferences[THEME_COLOUR_SOURCE_KEY] = SOURCE_CUSTOM
            if (preferences[THEME_COLOUR_SEED_KEY] == null) {
                // The settings file keeps colours as ARGB integers; toArgb() turns the Compose
                // colour into one.
                preferences[THEME_COLOUR_SEED_KEY] = KANSO_BLUE_SEED.toArgb()
            }
        }
    }

    /**
     * Turns the stored [source] name and [seed] colour back into a [ThemeColour], falling back
     * to [defaultThemeColour] when they are missing or do not make sense together.
     */
    private fun parseThemeColour(source: String?, seed: Int?): ThemeColour = when (source) {
        SOURCE_KANSO_BLUE -> ThemeColour.KansoBlue
        // The first default colour of an older Kanso no longer exists; Kanso blue replaces it.
        SOURCE_BASELINE -> ThemeColour.KansoBlue
        SOURCE_WALLPAPER ->
            if (sdkInt >= Build.VERSION_CODES.S) ThemeColour.Wallpaper else ThemeColour.KansoBlue
        SOURCE_CUSTOM -> if (seed != null) ThemeColour.Custom(seed) else defaultThemeColour
        else -> defaultThemeColour
    }

    /** The keys this repository stores its values under. */
    private companion object {
        /** The theme mode, saved as the [ThemeMode] entry's name, such as "Dark". */
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

        /** Where the theme colour comes from, saved as one of the SOURCE_ names below. */
        val THEME_COLOUR_SOURCE_KEY = stringPreferencesKey("theme_colour_source")

        /** The last picked custom seed colour, saved as an ARGB integer. */
        val THEME_COLOUR_SEED_KEY = intPreferencesKey("theme_colour_seed")

        /** The stored name of [ThemeColour.Wallpaper]; never change it, it is on disk. */
        const val SOURCE_WALLPAPER = "Wallpaper"

        /** The stored name of [ThemeColour.KansoBlue]; never change it, it is on disk. */
        const val SOURCE_KANSO_BLUE = "KansoBlue"

        /**
         * The name an older Kanso stored for its first default colour. It is never written any
         * more, only read, as Kanso blue; keep it so those settings files still open in blue.
         */
        const val SOURCE_BASELINE = "Baseline"

        /** The stored name of [ThemeColour.Custom]; never change it, it is on disk. */
        const val SOURCE_CUSTOM = "Custom"

        /** Turns a stored [name] back into a [ThemeMode], or System when it is missing or unknown. */
        fun parseThemeMode(name: String?): ThemeMode =
            ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.System
    }
}
