package com.phongkaster.kanso.di

import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.placeholder.PlaceholderViewModel
import com.phongkaster.kanso.ui.settings.SettingsViewModel
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

    viewModel { SettingsViewModel() }
}
