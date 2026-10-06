package com.phongkaster.kanso.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.phongkaster.kanso.ui.camera.CameraScreen
import com.phongkaster.kanso.ui.camera.CameraViewModel
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.home.HomeScreen
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.settings.SettingsScreen
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolViewModel
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
                    // Every tool opens the photo gallery to pick a photo from. The Camera tile
                    // opens the camera instead; a shot that is kept opens the Edit tool.
                    onToolClick = { tool ->
                        if (backStack.last() == Home) {
                            backStack.add(if (tool == Tool.CAMERA) Camera(Tool.EDIT) else Gallery(tool))
                        }
                    },
                    onSettingsClick = { if (backStack.last() == Home) backStack.add(Settings) },
                )
            }
            entry<Camera> { key ->
                val viewModel = koinViewModel<CameraViewModel>()
                CameraScreen(
                    viewModel = viewModel,
                    onBack = goBack,
                    // Keep replaces the camera with the tool screen: the shot is finished, so
                    // Back from the tool goes to where the camera was opened from. Only act
                    // while the camera is still on top, so a double tap cannot open two screens.
                    onKept = { photoUri ->
                        if (backStack.last() == key) {
                            backStack.removeLastOrNull()
                            backStack.add(ToolPhoto(key.tool, photoUri))
                        }
                    },
                )
            }
            entry<Gallery> { key ->
                val viewModel = koinViewModel<GalleryViewModel>(
                    parameters = { parametersOf(key.tool) },
                )
                GalleryScreen(
                    viewModel = viewModel,
                    onBack = goBack,
                    // Choosing a photo opens the tool on it. The photo goes inside the key (as
                    // text), so it is saved with the back stack. Only act while the gallery is
                    // still on top, so a double tap cannot open two tool screens.
                    onPhotoChosen = { tool, photoUri ->
                        if (backStack.last() == key) backStack.add(ToolPhoto(tool, photoUri))
                    },
                )
            }
            entry<ToolPhoto> { key ->
                val viewModel = koinViewModel<ToolViewModel>(
                    parameters = { parametersOf(key.tool, key.photoUri) },
                )
                ToolScreen(
                    viewModel = viewModel,
                    onBack = goBack,
                    // The Saved screen arrives in a later step; until then a saved photo just
                    // stays on the tool screen.
                    onSaved = {},
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
