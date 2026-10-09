package com.phongkaster.kanso.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.data.settings.createSettingsDataStore
import com.phongkaster.kanso.ui.settings.SettingsUiState
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Checks the one state SettingsViewModel hands the Settings screen (DoD 8): it starts as System
 * mode with this phone's default theme colour (and Kanso blue's seed as the last custom seed),
 * and then follows what is saved, both the theme mode set through the ViewModel and a theme
 * colour written to the settings file, every snapshot holding the choices together. The last
 * custom seed stays in the snapshot after another colour is chosen, so the Custom row can show it.
 *
 * It runs on the plain JVM. The repository sits on a real DataStore file in a temporary folder,
 * created with createSettingsDataStore, whose storage also works on Windows. The ViewModel's own
 * coroutines run on a test dispatcher standing in for the main thread, which runs only when the
 * test runs it; the file itself is read and written on background threads in real time, so each
 * wait polls the state in real time too instead of trusting a test clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsUiStateTest {

    /** Gives each test its own empty folder for the settings file, deleted afterwards. */
    @get:Rule
    val folder = TemporaryFolder()

    /** Stands in for the main thread: viewModelScope runs here, only when the test runs it. */
    private val mainDispatcher = StandardTestDispatcher()

    /** The scope the DataStore works in; cancelled after each test so the file is released. */
    private val storeScope = CoroutineScope(Job() + Dispatchers.IO)

    /** Owns the ViewModel, as a navigation entry does in the app; clearing it clears the ViewModel. */
    private val owner = ViewModelStore()

    /** The real repository under the ViewModel, acting as an Android 15 phone. */
    private lateinit var repository: SettingsRepository

    /** Makes viewModelScope run on [mainDispatcher] and opens the settings file of this test. */
    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        val file = File(folder.root, "settings.preferences_pb")
        repository = SettingsRepository(createSettingsDataStore(file, storeScope), sdkInt = 35)
    }

    /**
     * Clears the ViewModel and lets its cancelled work finish, puts the real main dispatcher
     * back, then shuts the DataStore down so its file can be deleted.
     */
    @After
    fun tearDown() = runBlocking {
        owner.clear()
        mainDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
        storeScope.coroutineContext[Job]!!.cancelAndJoin()
    }

    /**
     * Before anything has run, and so before the file is read, the state is System mode with
     * the repository's default theme colour for this phone.
     */
    @Test
    fun state_starts_as_system_mode_and_the_default_colour() {
        val viewModel = viewModel()

        assertEquals(
            SettingsUiState(ThemeMode.System, repository.defaultThemeColour),
            viewModel.state.value,
        )
    }

    /** Each theme mode set through the ViewModel shows up in the state once it is saved. */
    @Test
    fun state_follows_the_theme_mode_set_through_the_view_model() {
        val viewModel = viewModel()

        viewModel.setThemeMode(ThemeMode.Dark)
        awaitState(viewModel, SettingsUiState(ThemeMode.Dark, repository.defaultThemeColour))

        viewModel.setThemeMode(ThemeMode.Light)
        awaitState(viewModel, SettingsUiState(ThemeMode.Light, repository.defaultThemeColour))
    }

    /**
     * A theme colour saved to the file shows up in the state, and the theme mode saved before it
     * stays in the same snapshot.
     */
    @Test
    fun state_follows_a_saved_colour_and_keeps_the_saved_mode() {
        val viewModel = viewModel()
        viewModel.setThemeMode(ThemeMode.Dark)
        awaitState(viewModel, SettingsUiState(ThemeMode.Dark, repository.defaultThemeColour))

        runBlocking { repository.setThemeColour(ThemeColour.Custom(GREEN_ARGB)) }

        // A saved custom colour is also the last custom seed, which the snapshot carries too.
        awaitState(
            viewModel,
            SettingsUiState(ThemeMode.Dark, ThemeColour.Custom(GREEN_ARGB), customSeedArgb = GREEN_ARGB),
        )
    }

    /**
     * After a custom colour is saved and Kanso blue is chosen through the ViewModel, the state
     * shows Kanso blue as the choice and still holds the custom colour as the last custom seed,
     * which the Custom row's swatch shows.
     */
    @Test
    fun state_keeps_the_last_custom_seed_after_kanso_blue_is_chosen() {
        val viewModel = viewModel()
        runBlocking { repository.setThemeColour(ThemeColour.Custom(GREEN_ARGB)) }
        awaitState(
            viewModel,
            SettingsUiState(ThemeMode.System, ThemeColour.Custom(GREEN_ARGB), customSeedArgb = GREEN_ARGB),
        )

        viewModel.onKansoBlueChosen()

        awaitState(
            viewModel,
            SettingsUiState(ThemeMode.System, ThemeColour.KansoBlue, customSeedArgb = GREEN_ARGB),
        )
    }

    /**
     * Builds the ViewModel over [repository] through [owner], as the app does, so that clearing
     * [owner] after the test clears it.
     */
    private fun viewModel(): SettingsViewModel {
        val built = SettingsViewModel(repository)
        val factory = object : ViewModelProvider.Factory {
            /** Hands over the ViewModel built above. */
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = built as T
        }
        return ViewModelProvider(owner, factory)[SettingsViewModel::class.java]
    }

    /**
     * Waits, in real time, until [viewModel]'s state is [expected], then fails if it never became
     * that. The file is read and written on background threads, which hand their results back to
     * the stand-in main thread; running its queued work every few milliseconds lets the ViewModel
     * take them in.
     */
    private fun awaitState(viewModel: SettingsViewModel, expected: SettingsUiState) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MILLIS
        while (true) {
            mainDispatcher.scheduler.advanceUntilIdle()
            if (viewModel.state.value == expected || System.currentTimeMillis() > deadline) break
            Thread.sleep(POLL_MILLIS)
        }
        assertEquals(expected, viewModel.state.value)
    }

    /** Values the tests use. */
    private companion object {
        /** How long a wait for the state may take before the test fails. */
        const val TIMEOUT_MILLIS = 10_000L

        /** How long to sleep between two looks at the state. */
        const val POLL_MILLIS = 20L

        /** The custom colour saved by the tests: green #2E7D32, fully opaque, as ARGB. */
        val GREEN_ARGB = 0xFF2E7D32.toInt()
    }
}
