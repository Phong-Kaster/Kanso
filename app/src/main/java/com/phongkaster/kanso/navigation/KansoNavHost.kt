package com.phongkaster.kanso.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.phongkaster.kanso.ui.home.HomeScreen
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.placeholder.PlaceholderScreen
import com.phongkaster.kanso.ui.placeholder.PlaceholderViewModel
import com.phongkaster.kanso.ui.settings.SettingsScreen
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The whole app's navigation: holds the back stack and shows the screen on top of it.
 *
 * MainActivity calls this once inside its theme. It does not start Koin; KansoApplication does
 * that before any screen exists, so the `koinViewModel()` calls below always find it.
 *
 * Back behaviour: Back on a tool screen or on Settings pops it and shows Home again; Back on
 * Home (the only key left) closes the app.
 */
@Composable
fun KansoNavHost(modifier: Modifier = Modifier) {
    // The back stack is a list of NavKeys, Home at the bottom; the last key is the screen shown.
    // To navigate, add a key; to go back, remove the last one. rememberNavBackStack also saves
    // the list (the keys are @Serializable), so it survives rotation and process death.
    val backStack = rememberNavBackStack(Home)

    // The activity is needed to close the app when Back is pressed on Home.
    val activity = LocalActivity.current

    // NavDisplay calls this on system Back, even with only Home left. Above Home it pops the top
    // key; on Home it closes the app by finishing the activity (an empty back stack has nothing
    // to show, so Home is never popped).
    // A fast double tap must not stack two screens, so the click handlers below only navigate
    // while Home is still the top key.
    val goBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull() else activity?.finish()
    }

    // NavDisplay shows the entry for the last key on the back stack and animates between them.
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        // Called on system Back (and predictive back); goBack above decides what that means.
        onBack = goBack,
        // Entry decorators wrap every screen with extra per-entry behaviour:
        // - the saveable state holder keeps each screen's rememberSaveable state (scroll
        //   positions, text fields) separately, and saves it across rotation;
        // - the ViewModel store decorator gives each entry its own ViewModelStore. A ViewModel
        //   asked for inside an entry is therefore scoped to that entry: it survives rotation,
        //   and is cleared as soon as its key is popped off the back stack.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        // The entry provider maps each kind of key to the composable that draws it.
        entryProvider = entryProvider {
            entry<Home> {
                // koinViewModel asks Koin for the ViewModel, stored in this entry's ViewModelStore:
                // the first call builds it, later calls (after rotation, say) return the same one.
                val viewModel = koinViewModel<HomeViewModel>()
                HomeScreen(
                    viewModel = viewModel,
                    onToolClick = { tool -> if (backStack.last() == Home) backStack.add(Placeholder(tool)) },
                    onSettingsClick = { if (backStack.last() == Home) backStack.add(Settings) },
                )
            }
            entry<Placeholder> { key ->
                // parametersOf hands the tool to the Koin recipe that builds this ViewModel.
                val viewModel = koinViewModel<PlaceholderViewModel>(
                    parameters = { parametersOf(key.tool) },
                )
                PlaceholderScreen(
                    viewModel = viewModel,
                    onBack = goBack,
                )
            }
            entry<Settings> {
                val viewModel = koinViewModel<SettingsViewModel>()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = goBack,
                )
            }
        },
    )
}
