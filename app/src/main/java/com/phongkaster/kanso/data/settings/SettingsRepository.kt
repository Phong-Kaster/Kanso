package com.phongkaster.kanso.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
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
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

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

    /** The keys this repository stores its values under. */
    private companion object {
        /** The theme mode, saved as the [ThemeMode] entry's name, such as "Dark". */
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

        /** Turns a stored [name] back into a [ThemeMode], or System when it is missing or unknown. */
        fun parseThemeMode(name: String?): ThemeMode =
            ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.System
    }
}
