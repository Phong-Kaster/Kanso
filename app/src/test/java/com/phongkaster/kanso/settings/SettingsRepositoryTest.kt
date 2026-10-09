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
 * Checks the settings repository on the plain JVM against a real DataStore file: the default
 * theme mode, writing and reading it back, surviving a "restart" (a fresh DataStore over the
 * same file), and falling back to System for a value it does not recognise. It also checks the
 * last picked custom seed the Custom row's swatch shows: Kanso blue's seed when nothing was ever
 * picked, and the picked colour still after Kanso blue or the wallpaper is chosen.
 */
class SettingsRepositoryTest {

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

    /** With nothing saved yet, the theme mode is System. */
    @Test
    fun theme_mode_defaults_to_system() = runBlocking {
        val repository = SettingsRepository(openStore())

        assertEquals(ThemeMode.System, repository.themeMode.first())
    }

    /** After saving Dark, the theme mode reads Dark. */
    @Test
    fun saved_theme_mode_is_read_back() = runBlocking {
        val repository = SettingsRepository(openStore())

        repository.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.Light)
        assertEquals(ThemeMode.Light, repository.themeMode.first())
    }

    /**
     * A theme mode saved by one DataStore is read from disk by a fresh DataStore over the same
     * file, once the first is shut down, as after an app restart.
     */
    @Test
    fun saved_theme_mode_survives_a_fresh_store_over_the_same_file() = runBlocking {
        val firstScope = newScope()
        SettingsRepository(createSettingsDataStore(file, firstScope)).setThemeMode(ThemeMode.Dark)
        // Only one DataStore may be open on a file; cancelling its scope closes it.
        firstScope.coroutineContext[Job]!!.cancelAndJoin()

        val reopened = SettingsRepository(openStore())

        assertEquals(ThemeMode.Dark, reopened.themeMode.first())
    }

    /** A stored value that names no theme mode reads as System. */
    @Test
    fun unknown_stored_value_reads_as_system() = runBlocking {
        val store = openStore()
        store.edit { it[stringPreferencesKey("theme_mode")] = "Sepia" }

        assertEquals(ThemeMode.System, SettingsRepository(store).themeMode.first())
    }

    /**
     * A settings file holding garbage bytes (as a crash mid-write might leave) still opens: it
     * reads System, and a new choice can be saved and read back over it.
     */
    @Test
    fun corrupt_file_reads_system_and_can_be_written_again() = runBlocking {
        // Sixteen 0xFF bytes are not valid protocol buffer data: the varint never ends.
        file.writeBytes(ByteArray(16) { 0xFF.toByte() })
        val repository = SettingsRepository(openStore())

        assertEquals(ThemeMode.System, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, repository.themeMode.first())
    }

    /** With nothing saved yet, the last custom seed reads as Kanso blue's seed. */
    @Test
    fun custom_seed_defaults_to_the_kanso_blue_seed() = runBlocking {
        val repository = SettingsRepository(openStore(), sdkInt = 35)

        assertEquals(KANSO_BLUE_SEED.toArgb(), repository.customSeedArgb.first())
    }

    /**
     * Custom is chosen and its colour set to green, as the picker does; then Kanso blue and the
     * wallpaper are chosen in turn. Each time the choice changes, but the last custom seed still
     * reads green, the colour Custom would bring back.
     */
    @Test
    fun custom_seed_is_kept_while_kanso_blue_or_the_wallpaper_is_chosen() = runBlocking {
        val repository = SettingsRepository(openStore(), sdkInt = 35)
        repository.chooseCustomThemeColour()
        repository.setThemeColour(ThemeColour.Custom(GREEN_ARGB))
        assertEquals(GREEN_ARGB, repository.customSeedArgb.first())

        repository.setThemeColour(ThemeColour.KansoBlue)
        assertEquals(ThemeColour.KansoBlue, repository.themeColour.first())
        assertEquals(GREEN_ARGB, repository.customSeedArgb.first())

        repository.setThemeColour(ThemeColour.Wallpaper)
        assertEquals(ThemeColour.Wallpaper, repository.themeColour.first())
        assertEquals(GREEN_ARGB, repository.customSeedArgb.first())
    }

    /** Opens a DataStore on [file] in a new scope that is shut down after the test. */
    private fun openStore(): DataStore<Preferences> = createSettingsDataStore(file, newScope())

    /** Creates a scope for one DataStore and remembers it so it is cancelled after the test. */
    private fun newScope(): CoroutineScope =
        CoroutineScope(Job() + Dispatchers.IO).also { scopes += it }

    /** Values the tests use. */
    private companion object {
        /** The custom colour the tests pick: green #2E7D32, fully opaque, as ARGB. */
        val GREEN_ARGB = 0xFF2E7D32.toInt()
    }
}
