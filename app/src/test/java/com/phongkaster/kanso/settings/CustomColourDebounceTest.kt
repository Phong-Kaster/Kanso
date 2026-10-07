package com.phongkaster.kanso.settings

import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Checks that the custom colour picker saves the theme once the drag settles, not on every
 * movement: fifty colour changes 16 ms apart write nothing, the last colour is written exactly
 * once after the settle interval, a colour still waiting when Settings closes is saved, and a
 * later Wallpaper choice cancels a waiting colour.
 *
 * It runs on the plain JVM in virtual time: a test dispatcher stands in for the main thread and
 * for the background, so `advanceTimeBy` moves the clock without really waiting. Writes are
 * counted by an in-memory DataStore behind a real SettingsRepository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomColourDebounceTest {

    /** The in-memory settings store the repository writes to, counting each write. */
    private val store = CountingPreferencesStore()

    /** The real repository under the ViewModel, acting as an Android 15 phone. */
    private val repository = SettingsRepository(store, sdkInt = 35)

    /** Makes viewModelScope run on a test dispatcher, since the plain JVM has no main thread. */
    @Before
    fun useTestMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    /** Puts the real main dispatcher back for the next test. */
    @After
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    /** Fifty colour changes 16 ms apart, as a drag sends them, write nothing while it lasts. */
    @Test
    fun a_drag_of_fifty_changes_writes_nothing_while_it_lasts() = runTest {
        val viewModel = viewModel()

        drag(viewModel)
        // Just before the settle interval ends after the last change, still nothing is written.
        advanceTimeBy(SETTLE_MILLIS - FRAME_MILLIS - 1)
        runCurrent()

        assertEquals(0, store.writes)
    }

    /** Once the drag settles, exactly one write holds the last colour, made fully opaque. */
    @Test
    fun the_settled_drag_writes_the_last_colour_once() = runTest {
        val viewModel = viewModel()

        drag(viewModel)
        advanceUntilIdle()

        assertEquals(1, store.writes)
        assertEquals(ThemeColour.Custom(GREEN_ARGB), repository.themeColour.first())
    }

    /** A colour still waiting when the ViewModel is cleared is written all the same. */
    @Test
    fun clearing_the_view_model_writes_the_waiting_colour() = runTest {
        val owner = ViewModelStore()
        val viewModel = viewModel(owner)

        drag(viewModel)
        assertEquals(0, store.writes)
        owner.clear()
        advanceUntilIdle()

        assertEquals(1, store.writes)
        assertEquals(ThemeColour.Custom(GREEN_ARGB), repository.themeColour.first())
    }

    /** Clearing after the colour was already saved writes nothing more. */
    @Test
    fun clearing_after_the_drag_settled_writes_nothing_more() = runTest {
        val owner = ViewModelStore()
        val viewModel = viewModel(owner)

        drag(viewModel)
        advanceUntilIdle()
        owner.clear()
        advanceUntilIdle()

        assertEquals(1, store.writes)
    }

    /** Choosing Wallpaper while a colour is waiting cancels it, so Wallpaper stays saved. */
    @Test
    fun a_later_wallpaper_choice_cancels_the_waiting_colour() = runTest {
        val viewModel = viewModel()

        drag(viewModel)
        viewModel.onWallpaperChosen()
        advanceUntilIdle()

        assertEquals(1, store.writes)
        assertEquals(ThemeColour.Wallpaper, repository.themeColour.first())
    }

    /**
     * Builds the ViewModel through [owner], as the app does, so clearing [owner] clears it. Its
     * delayed colour save runs in a scope on the test clock.
     */
    private fun TestScope.viewModel(owner: ViewModelStore = ViewModelStore()): SettingsViewModel {
        val built = SettingsViewModel(
            repository,
            colourSettleMillis = SETTLE_MILLIS,
            // Not backgroundScope: advanceUntilIdle skips background tasks, so the save would never run.
            colourWriteScope = CoroutineScope(StandardTestDispatcher(testScheduler)),
        )
        val factory = object : ViewModelProvider.Factory {
            /** Hands over the ViewModel built above. */
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = built as T
        }
        return ViewModelProvider(owner, factory)[SettingsViewModel::class.java]
    }

    /**
     * Sends fifty colour changes 16 ms apart, as the picker does during a drag; the last one is
     * a half-transparent green, whose transparency the save must drop.
     */
    private fun TestScope.drag(viewModel: SettingsViewModel) {
        repeat(CHANGES - 1) { step ->
            val passingColour = Color(red = step / CHANGES.toFloat(), green = 0.4f, blue = 0.6f)
            viewModel.onCustomColourChosen(passingColour)
            advanceTimeBy(FRAME_MILLIS)
        }
        viewModel.onCustomColourChosen(TRANSLUCENT_GREEN)
        advanceTimeBy(FRAME_MILLIS)
    }

    /**
     * An in-memory stand-in for the settings DataStore that counts how many writes reach it.
     * It needs no file, so it avoids the Windows file-renaming trouble of a real store.
     */
    private class CountingPreferencesStore : DataStore<Preferences> {

        /** The stored preferences, which [data] emits whenever they change. */
        private val current = MutableStateFlow(emptyPreferences())

        /** How many writes have reached the store. */
        var writes = 0
            private set

        override val data: Flow<Preferences> = current

        /** Applies [transform] to the stored preferences and counts it as one write. */
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            writes++
            return transform(current.value).also { current.value = it }
        }
    }

    /** Values the tests use. */
    private companion object {
        /** The settle interval the ViewModel is given, the same as the app's. */
        const val SETTLE_MILLIS = SettingsViewModel.COLOUR_SETTLE_MILLIS

        /** The time between two colour changes during a drag, one 60 Hz frame. */
        const val FRAME_MILLIS = 16L

        /** How many colour changes one drag sends. */
        const val CHANGES = 50

        /** The last colour of the drag: green #2E7D32 at half transparency. */
        val TRANSLUCENT_GREEN = Color(0x802E7D32)

        /** The colour that must be saved: the same green, fully opaque, as ARGB. */
        val GREEN_ARGB = 0xFF2E7D32.toInt()
    }
}
