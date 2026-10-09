package com.phongkaster.kanso.settings

import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.data.settings.createSettingsDataStore
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Checks how the settings repository keeps the theme colour, on the plain JVM against a real
 * DataStore file: the default on new and old phones (Kanso blue on both), saving each option and
 * the custom seed, surviving a "restart" (a fresh DataStore over the same file), and reading the
 * values older versions of Kanso stored.
 */
class ThemeColourStoreTest {

    /** Gives each test its own empty folder, deleted afterwards. */
    @get:Rule
    val folder = TemporaryFolder()

    /** The settings file every DataStore in a test opens. */
    private lateinit var file: File

    /** The scopes of the DataStores a test opened, cancelled after it so their files are released. */
    private val scopes = mutableListOf<CoroutineScope>()

    /** Picks the settings file inside the fresh temporary folder. */
    @Before
    fun pickFile() {
        file = File(folder.root, "settings.preferences_pb")
    }

    /** Shuts down every DataStore the test opened. */
    @After
    fun closeStores() = runBlocking {
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
    }

    /** With nothing saved, Android 12 and later start on Kanso blue, not the wallpaper colours. */
    @Test
    fun default_is_kanso_blue_on_android_12_and_later() = runBlocking {
        // One DataStore per file: both phones are read through the same, empty store.
        val store = openStore()
        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 31).themeColour.first())
        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 35).themeColour.first())
    }

    /** With nothing saved, phones below Android 12 start on Kanso blue. */
    @Test
    fun default_is_kanso_blue_below_android_12() = runBlocking {
        val store = openStore()
        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 27).themeColour.first())
        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 30).themeColour.first())
    }

    /** A saved custom seed is read back, and so is a later switch to the wallpaper. */
    @Test
    fun saved_options_are_read_back() = runBlocking {
        val repository = repository(sdkInt = 35)

        repository.setThemeColour(ThemeColour.Custom(GREEN_ARGB))
        assertEquals(ThemeColour.Custom(GREEN_ARGB), repository.themeColour.first())

        repository.setThemeColour(ThemeColour.Wallpaper)
        assertEquals(ThemeColour.Wallpaper, repository.themeColour.first())
    }

    /** The custom seed #2E7D32 is read from disk by a fresh DataStore, as after a restart. */
    @Test
    fun custom_seed_survives_a_fresh_store_over_the_same_file() = runBlocking {
        writeThenReopen { it.setThemeColour(ThemeColour.Custom(GREEN_ARGB)) }
            .let { assertEquals(ThemeColour.Custom(GREEN_ARGB), it.themeColour.first()) }
    }

    /** A Wallpaper choice is read from disk by a fresh DataStore, after a custom colour was used. */
    @Test
    fun wallpaper_choice_survives_a_fresh_store_over_the_same_file() = runBlocking {
        writeThenReopen {
            it.setThemeColour(ThemeColour.Custom(GREEN_ARGB))
            it.setThemeColour(ThemeColour.Wallpaper)
        }.let { assertEquals(ThemeColour.Wallpaper, it.themeColour.first()) }
    }

    /**
     * A Kanso blue choice is read from disk by a fresh DataStore, after a custom colour was used
     * (so the default cannot pass for it).
     */
    @Test
    fun kanso_blue_choice_survives_a_fresh_store_over_the_same_file() = runBlocking {
        writeThenReopen {
            it.setThemeColour(ThemeColour.Custom(GREEN_ARGB))
            it.setThemeColour(ThemeColour.KansoBlue)
        }.let { assertEquals(ThemeColour.KansoBlue, it.themeColour.first()) }
    }

    /** The Kanso blue choice is stored under the colour source key as "KansoBlue". */
    @Test
    fun kanso_blue_is_stored_as_kanso_blue() = runBlocking {
        val store = openStore()
        SettingsRepository(store, sdkInt = 35).setThemeColour(ThemeColour.KansoBlue)

        assertEquals("KansoBlue", store.data.first()[SOURCE_KEY])
    }

    /**
     * The name an older Kanso stored for its first default colour, "Baseline", reads as Kanso
     * blue on Android 11 and on Android 15.
     */
    @Test
    fun stored_baseline_reads_as_kanso_blue_on_android_11_and_15() = runBlocking {
        val store = openStore()
        store.edit { it[SOURCE_KEY] = "Baseline" }

        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 30).themeColour.first())
        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 35).themeColour.first())
    }

    /**
     * A stored "Custom" source with no seed colour beside it (a file that lost its seed) cannot
     * be drawn as a custom colour, so it reads as the default, Kanso blue, on Android 11 and 15.
     */
    @Test
    fun stored_custom_without_a_seed_reads_as_kanso_blue() = runBlocking {
        val store = openStore()
        // Only the source is written; the seed key stays empty.
        store.edit { it[SOURCE_KEY] = "Custom" }

        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 30).themeColour.first())
        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 35).themeColour.first())
    }

    /** A stored "Wallpaper" stays the wallpaper choice on Android 15, which has wallpaper colours. */
    @Test
    fun stored_wallpaper_stays_wallpaper_on_android_15() = runBlocking {
        val store = openStore()
        store.edit { it[SOURCE_KEY] = "Wallpaper" }

        assertEquals(ThemeColour.Wallpaper, SettingsRepository(store, sdkInt = 35).themeColour.first())
    }

    /** The theme mode and the theme colour are kept side by side in the same file. */
    @Test
    fun theme_mode_and_colour_are_kept_together() = runBlocking {
        val reopened = writeThenReopen {
            it.setThemeMode(ThemeMode.Dark)
            it.setThemeColour(ThemeColour.Custom(GREEN_ARGB))
        }
        assertEquals(ThemeMode.Dark, reopened.themeMode.first())
        assertEquals(ThemeColour.Custom(GREEN_ARGB), reopened.themeColour.first())
    }

    /** Choosing Custom again after Wallpaper brings back the last picked seed. */
    @Test
    fun choosing_custom_again_keeps_the_last_seed() = runBlocking {
        val repository = repository(sdkInt = 35)
        repository.setThemeColour(ThemeColour.Custom(GREEN_ARGB))
        repository.setThemeColour(ThemeColour.Wallpaper)

        repository.chooseCustomThemeColour()

        assertEquals(ThemeColour.Custom(GREEN_ARGB), repository.themeColour.first())
    }

    /** Choosing Custom before any colour was picked starts from Kanso blue's seed. */
    @Test
    fun choosing_custom_first_starts_from_kanso_blue() = runBlocking {
        val repository = repository(sdkInt = 35)

        repository.chooseCustomThemeColour()

        assertEquals(
            ThemeColour.Custom(KANSO_BLUE_SEED.toArgb()),
            repository.themeColour.first(),
        )
    }

    /** A Wallpaper choice read on a phone below Android 12 falls back to Kanso blue. */
    @Test
    fun wallpaper_reads_as_kanso_blue_below_android_12() = runBlocking {
        val store = openStore()
        SettingsRepository(store, sdkInt = 35).setThemeColour(ThemeColour.Wallpaper)

        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 30).themeColour.first())
    }

    /** A stored source name the app does not know reads as the default, Kanso blue. */
    @Test
    fun unknown_source_reads_as_the_default() = runBlocking {
        val store = openStore()
        store.edit { it[SOURCE_KEY] = "Sepia" }

        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 35).themeColour.first())
        assertEquals(ThemeColour.KansoBlue, SettingsRepository(store, sdkInt = 30).themeColour.first())
    }

    /**
     * Runs [write] against a repository on the file, shuts that DataStore down as an app exit
     * would, and returns a repository over a fresh DataStore on the same file.
     */
    private suspend fun writeThenReopen(write: suspend (SettingsRepository) -> Unit): SettingsRepository {
        val firstScope = newScope()
        write(SettingsRepository(createSettingsDataStore(file, firstScope), sdkInt = 35))
        // Only one DataStore may be open on a file; cancelling its scope closes it.
        firstScope.coroutineContext[Job]!!.cancelAndJoin()
        return repository(sdkInt = 35)
    }

    /** Returns a repository acting as a phone on API level [sdkInt], over a new DataStore. */
    private fun repository(sdkInt: Int): SettingsRepository =
        SettingsRepository(openStore(), sdkInt = sdkInt)

    /** Opens a DataStore on [file] in a new scope that is shut down after the test. */
    private fun openStore(): DataStore<Preferences> = createSettingsDataStore(file, newScope())

    /** Creates a scope for one DataStore and remembers it so it is cancelled after the test. */
    private fun newScope(): CoroutineScope =
        CoroutineScope(Job() + Dispatchers.IO).also { scopes += it }

    /** Values the tests use. */
    private companion object {
        /** The custom colour of the DoD, green #2E7D32, as ARGB. */
        val GREEN_ARGB = 0xFF2E7D32.toInt()

        /** The stored name of the colour source, as the settings file keeps it (never renamed). */
        val SOURCE_KEY = stringPreferencesKey("theme_colour_source")
    }
}
