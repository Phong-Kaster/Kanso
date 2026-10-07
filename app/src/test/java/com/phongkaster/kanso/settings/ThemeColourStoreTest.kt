package com.phongkaster.kanso.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.data.settings.createSettingsDataStore
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
 * DataStore file: the default on new and old phones, saving each option and the custom seed,
 * and surviving a "restart" (a fresh DataStore over the same file).
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

    /** With nothing saved, Android 12 and later start on the wallpaper colours. */
    @Test
    fun default_is_wallpaper_on_android_12_and_later() = runBlocking {
        assertEquals(ThemeColour.Wallpaper, repository(sdkInt = 31).themeColour.first())
    }

    /** With nothing saved, phones below Android 12 start on the baseline purple. */
    @Test
    fun default_is_baseline_below_android_12() = runBlocking {
        assertEquals(ThemeColour.Baseline, repository(sdkInt = 30).themeColour.first())
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

    /** A Baseline choice is read from disk by a fresh DataStore. */
    @Test
    fun baseline_choice_survives_a_fresh_store_over_the_same_file() = runBlocking {
        writeThenReopen { it.setThemeColour(ThemeColour.Baseline) }
            .let { assertEquals(ThemeColour.Baseline, it.themeColour.first()) }
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

    /** Choosing Custom before any colour was picked starts from the baseline purple. */
    @Test
    fun choosing_custom_first_starts_from_the_baseline_purple() = runBlocking {
        val repository = repository(sdkInt = 35)

        repository.chooseCustomThemeColour()

        assertEquals(
            ThemeColour.Custom(ThemeColour.BASELINE_SEED_ARGB),
            repository.themeColour.first(),
        )
    }

    /** A Wallpaper choice read on a phone below Android 12 falls back to the baseline. */
    @Test
    fun wallpaper_reads_as_baseline_below_android_12() = runBlocking {
        val store = openStore()
        SettingsRepository(store, sdkInt = 35).setThemeColour(ThemeColour.Wallpaper)

        assertEquals(ThemeColour.Baseline, SettingsRepository(store, sdkInt = 30).themeColour.first())
    }

    /** A stored source name the app does not know reads as the default. */
    @Test
    fun unknown_source_reads_as_the_default() = runBlocking {
        val store = openStore()
        store.edit { it[stringPreferencesKey("theme_colour_source")] = "Sepia" }

        assertEquals(ThemeColour.Wallpaper, SettingsRepository(store, sdkInt = 35).themeColour.first())
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
    }
}
