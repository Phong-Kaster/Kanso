package com.phongkaster.kanso.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the Settings screen's state: the saved choices it shows, as one [SettingsUiState], and
 * the actions that change them.
 *
 * Like every screen ViewModel in Kanso, Koin builds it and the Settings navigation entry owns it,
 * so it survives rotation and is cleared when the user leaves Settings. A further setting gets
 * one field in [SettingsUiState] and one setter here, beside the theme mode and theme colour;
 * the screen still reads them all through the one [state].
 *
 * [colourSettleMillis] is how long the custom colour must stay unchanged before it is saved, and
 * [colourWriteScope] is where that delayed save runs. Its default lives longer than this
 * ViewModel, so a colour still waiting when Settings closes is saved all the same. Tests pass
 * their own scope to control time; the app uses the defaults.
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val colourSettleMillis: Long = COLOUR_SETTLE_MILLIS,
    private val colourWriteScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : ViewModel() {

    /** Guards [pendingColour] and [pendingColourJob], which the picker and the delayed save share. */
    private val pendingLock = Any()

    /** The custom colour picked but not saved yet, or null when nothing is waiting. */
    private var pendingColour: ThemeColour.Custom? = null

    /** The delayed save of [pendingColour], or null when none was started. */
    private var pendingColourJob: Job? = null

    /**
     * What Settings shows: the saved theme mode, the saved theme colour and the last picked
     * custom colour together, as one snapshot the screen can read at any moment (see HomeUiState
     * for what a UiState is).
     *
     * The repository gives the three values as three separate Flows. `combine` joins them into
     * one: once each Flow has given its first value, and again every time any of them gives a
     * new one, it calls the block it is given with the latest value of all three. The block
     * builds a new SettingsUiState from them, so each snapshot holds the three side by side, and
     * the screen has only one StateFlow to read instead of one per setting.
     *
     * A StateFlow is a Flow that always holds one current value. stateIn turns the combined Flow
     * into one: it collects the Flow in viewModelScope (a scope cancelled when this ViewModel is
     * cleared), starts at once (Eagerly), and shows System with this phone's default colour until
     * the file is read; the custom colour starts at SettingsUiState's own default, Kanso blue's
     * seed, which is also what the file gives when no colour was ever picked.
     */
    val state: StateFlow<SettingsUiState> = combine(
        repository.themeMode,
        repository.themeColour,
        repository.customSeedArgb,
        repository.textScale,
    ) { themeMode, themeColour, customSeedArgb, textScale ->
        SettingsUiState(
            themeMode = themeMode,
            themeColour = themeColour,
            customSeedArgb = customSeedArgb,
            textScale = textScale,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState(ThemeMode.System, repository.defaultThemeColour),
    )

    /** Saves [mode] as the theme mode; the whole app re-themes as soon as it is written. */
    fun setThemeMode(mode: ThemeMode) {
        // launch starts the write in the background so the tap never blocks the screen.
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    /** Saves [scale] as the text size multiplier; the whole app is redrawn at that size at once. */
    fun setTextScale(scale: Float) {
        viewModelScope.launch { repository.setTextScale(scale) }
    }

    /**
     * Takes the app's colours from the wallpaper (offered on Android 12 and later only). A custom
     * colour still waiting to be saved is dropped first, so it cannot overwrite this choice.
     */
    fun onWallpaperChosen() {
        val pending = dropPendingColour()
        // Runs in colourWriteScope, not viewModelScope, so leaving Settings at once cannot
        // cancel it.
        colourWriteScope.launch {
            // Wait until the dropped save has really stopped before writing the new choice.
            pending?.cancelAndJoin()
            repository.setThemeColour(ThemeColour.Wallpaper)
        }
    }

    /**
     * Uses Kanso blue, the app's own colours (offered on every Android version). A custom
     * colour still waiting to be saved is dropped first, so it cannot overwrite this choice.
     */
    fun onKansoBlueChosen() {
        val pending = dropPendingColour()
        // Runs in colourWriteScope, not viewModelScope, so leaving Settings at once cannot
        // cancel it.
        colourWriteScope.launch {
            // Wait until the dropped save has really stopped before writing the new choice.
            pending?.cancelAndJoin()
            repository.setThemeColour(ThemeColour.KansoBlue)
        }
    }

    /**
     * Selects the Custom option with the last picked colour (or Kanso blue's seed, when none was
     * ever picked), which also shows the colour picker.
     */
    fun onCustomChosen() {
        viewModelScope.launch { repository.chooseCustomThemeColour() }
    }

    /**
     * Uses [color] as the custom seed colour. The colour picker calls this on every movement of
     * a drag, so the colour is saved, and the whole app re-themes from it, only once the drag
     * has settled. Its transparency is dropped, since a colour scheme is built from an opaque
     * colour.
     */
    fun onCustomColourChosen(color: Color) {
        val colour = ThemeColour.Custom(color.copy(alpha = 1f).toArgb())
        // This is "debouncing": instead of saving every colour the drag passes through, each new
        // colour cancels the save planned for the previous one and plans its own save a short
        // while ahead. While the finger keeps moving, every planned save is cancelled before its
        // wait ends, so nothing is written; once the colour stops changing for
        // colourSettleMillis, the last plan runs and writes the last colour once.
        // delay() pauses only this background task, never the screen.
        synchronized(pendingLock) {
            pendingColour = colour
            pendingColourJob?.cancel()
            pendingColourJob = colourWriteScope.launch {
                delay(colourSettleMillis)
                val settled = takePendingColour() ?: return@launch
                repository.setThemeColour(settled)
            }
        }
    }

    /**
     * Saves a custom colour still waiting for the drag to settle when Settings closes, so the
     * last colour picked is never lost. viewModelScope is already cancelled by now, which is why
     * the save runs in [colourWriteScope].
     */
    override fun onCleared() {
        val colour = synchronized(pendingLock) {
            val waiting = pendingColour ?: return@synchronized null
            // The planned save has not taken the colour yet, so it is stopped and the colour
            // is saved right away below instead.
            pendingColourJob?.cancel()
            pendingColourJob = null
            pendingColour = null
            waiting
        }
        if (colour != null) colourWriteScope.launch { repository.setThemeColour(colour) }
        super.onCleared()
    }

    /** Hands the waiting custom colour to the save that will write it, leaving none waiting. */
    private fun takePendingColour(): ThemeColour.Custom? = synchronized(pendingLock) {
        pendingColour.also { pendingColour = null }
    }

    /**
     * Forgets the waiting custom colour and cancels its planned save, returning that save so
     * the caller can wait for it to stop.
     */
    private fun dropPendingColour(): Job? = synchronized(pendingLock) {
        val job = pendingColourJob
        job?.cancel()
        pendingColourJob = null
        pendingColour = null
        job
    }

    /** Values shared by every Settings ViewModel. */
    companion object {
        /**
         * How long, in milliseconds, the custom colour must stay unchanged before it is saved:
         * long enough to skip the colours a drag passes through, short enough to feel prompt.
         */
        const val COLOUR_SETTLE_MILLIS: Long = 300L
    }
}
