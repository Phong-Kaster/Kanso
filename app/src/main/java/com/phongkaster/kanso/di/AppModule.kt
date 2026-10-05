package com.phongkaster.kanso.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.phongkaster.kanso.data.settings.SETTINGS_SCOPE
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.createSettingsDataStore
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.placeholder.PlaceholderViewModel
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The app's Koin module: the recipe book Koin uses to build objects when a screen asks for one.
 *
 * Koin is a dependency-injection library. Instead of a screen creating its ViewModel itself,
 * it asks Koin (with `koinViewModel()`), and Koin builds it from the recipes declared here,
 * filling in constructor arguments with `get()`. KansoApplication starts Koin with this module.
 *
 * - `single { ... }` builds one shared instance for the whole app.
 * - `viewModel { ... }` builds a ViewModel; Android keeps it in a ViewModelStore so it survives
 *   rotation, and clears it when its owner (here, a navigation entry) goes away for good.
 */
val appModule = module {
    single { ViewModelClearedProbe() }

    viewModel { HomeViewModel(probe = get()) }

    // `params` are the values the screen passes with parametersOf(...): here, the tool shown.
    viewModel { params -> PlaceholderViewModel(tool = params.get(), probe = get()) }

    // The scope the settings file is read and written in. It is a named singleton so a test can
    // cancel it, which releases the file, and then open the same file again like a restart.
    single(SETTINGS_SCOPE) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // The settings live in one small file in the app's private storage (Preferences DataStore).
    single<DataStore<Preferences>> {
        createSettingsDataStore(
            file = androidContext().filesDir.resolve("datastore/settings.preferences_pb"),
            scope = get<CoroutineScope>(SETTINGS_SCOPE),
        )
    }

    single { SettingsRepository(dataStore = get()) }

    viewModel { SettingsViewModel(repository = get()) }
}
