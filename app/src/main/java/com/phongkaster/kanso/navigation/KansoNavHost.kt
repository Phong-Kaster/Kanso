package com.phongkaster.kanso.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.camera.CameraScreen
import com.phongkaster.kanso.ui.camera.CameraViewModel
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.home.HomeContent
import com.phongkaster.kanso.ui.home.HomeScreen
import com.phongkaster.kanso.ui.home.HomeUiState
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.saved.SavedScreen
import com.phongkaster.kanso.ui.saved.SavedViewModel
import com.phongkaster.kanso.ui.settings.SettingsScreen
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import com.phongkaster.kanso.ui.theme.KansoTheme
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

    // Entry decorators wrap every screen with extra per-entry behaviour:
    // - the saveable state holder keeps each screen's rememberSaveable state (scroll
    //   positions, text fields) separately, and saves it across rotation;
    // - the ViewModel store decorator gives each entry its own ViewModelStore. A ViewModel
    //   asked for inside an entry is therefore scoped to that entry: it survives rotation,
    //   and is cleared as soon as its key is popped off the back stack.
    val entryDecorators: List<NavEntryDecorator<NavKey>> = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        rememberViewModelStoreNavEntryDecorator<NavKey>(),
    )

    // The drawing itself is done by the stateless KansoNavDisplay below; this function only
    // gathers what it needs (the saved back stack, Back handling, decorators and screens).
    KansoNavDisplay(
        backStack = backStack,
        modifier = modifier,
        // Called on system Back (and predictive back); goBack above decides what that means.
        onBack = goBack,
        entryDecorators = entryDecorators,
        // The entry provider maps each kind of key to the composable that draws it.
        entryProvider = entryProvider<NavKey> {
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
                    // The camera button opens the camera; a kept shot opens this gallery's own
                    // tool. Only act while the gallery is on top.
                    onOpenCamera = { tool ->
                        if (backStack.last() == key) backStack.add(Camera(tool))
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
                    // A saved photo replaces everything above Home with the Saved screen, so
                    // Back from Saved goes to Home, not to the tool screen. Only act while the
                    // tool screen is on top.
                    onSaved = { photoUri ->
                        if (backStack.last() == key) {
                            while (backStack.size > 1) backStack.removeLastOrNull()
                            backStack.add(Saved(key.tool, photoUri))
                        }
                    },
                )
            }
            entry<Saved> { key ->
                // The saved photo's Uri travels inside the key and reaches the ViewModel as a Koin
                // parameter, the same way the tool screen's ViewModel gets its photo.
                val viewModel = koinViewModel<SavedViewModel>(
                    parameters = { parametersOf(key.photoUri) },
                )
                SavedScreen(
                    viewModel = viewModel,
                    onShare = {},
                    // Edit another: back to [Home, Gallery(tool)]. Home: back to [Home].
                    onEditAnother = {
                        if (backStack.last() == key) {
                            backStack.removeLastOrNull()
                            backStack.add(Gallery(key.tool))
                        }
                    },
                    onHome = { if (backStack.last() == key) backStack.removeLastOrNull() },
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

/**
 * The stateless part of the app's navigation (see [KansoNavHost] for the stateful part): shows
 * the screen for the last key of [backStack].
 *
 * It is a thin wrapper around Navigation 3's `NavDisplay`. NavDisplay owns no back stack of its
 * own: it reads the list it is given, takes the last key, and asks [entryProvider] for the
 * `NavEntry` of that key. A NavEntry is the key paired with the composable that draws it, so the
 * entry provider is simply the app's table of "this kind of key is drawn by that screen". When
 * the list changes (a key added or removed), NavDisplay animates from the old top screen to the
 * new one.
 *
 * Each entry is wrapped by every decorator in [entryDecorators] before it is drawn; that is how
 * a screen gets its own saved state or its own ViewModelStore. System Back (including the
 * predictive back gesture) calls [onBack]; deciding what Back means is the caller's job.
 *
 * Because this function takes only plain values and lambdas, a preview can call it with a
 * one-key back stack and a sample screen, without Koin or a ViewModel.
 */
@Composable
fun KansoNavDisplay(
    backStack: List<NavKey>,
    onBack: () -> Unit,
    entryDecorators: List<NavEntryDecorator<NavKey>>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = onBack,
        entryDecorators = entryDecorators,
        entryProvider = entryProvider,
    )
}

/** Sample data for the preview: every tool the app has, the same list the real Home shows. */
private val sampleHomeTools: List<Tool> = Tool.entries

/**
 * The navigation host as the app opens, drawn once in light and once in dark.
 *
 * The back stack is a plain list holding only [Home], so nothing needs saving or restoring. Only
 * the saveable state decorator is used: the ViewModel store decorator needs a ViewModelStore
 * owner, which a preview does not have. The Home entry draws [HomeContent] with sample tools, and
 * the taps do nothing, because a preview cannot be tapped into another screen.
 */
@PreviewLightDark
@Composable
private fun KansoNavHostPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        KansoNavDisplay(
            backStack = listOf<NavKey>(Home),
            onBack = {},
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>()),
            entryProvider = entryProvider<NavKey> {
                entry<Home> {
                    HomeContent(
                        state = HomeUiState(tools = sampleHomeTools),
                        onToolClick = {},
                        onSettingsClick = {},
                    )
                }
            },
        )
    }
}
