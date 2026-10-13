package com.phongkaster.kanso.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
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
import com.phongkaster.kanso.ui.myedits.MyEditsScreen
import com.phongkaster.kanso.ui.myedits.MyEditsViewModel
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
 * Back behaviour: Back on a tool screen pops it; Back on Settings or My edits (a tab, which sits
 * directly above Home) shows Home again; Back on Home (the only key left) closes the app.
 *
 * The three tabs (Home, My edits, Settings) are shown inside [KansoTabShell], which floats the tab
 * bar over whichever tab is on top and hides it on every other screen. Each tab is a key on the
 * same back stack: picking a tab clears the stack down to Home and adds the tab's key, so tabs
 * never pile up (see BackStackActions.kt).
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

    // The key on top decides two things the shell needs: whether the bar is shown (only over a
    // tab) and which tab looks selected.
    val top = backStack.lastOrNull()

    // The fade between two tabs is per-entry metadata, built once from the theme's motion scheme
    // (read here because MaterialTheme.motionScheme only works while Compose is drawing).
    val tabMetadata = kansoTabEntryMetadata(MaterialTheme.motionScheme)

    // The floating bar covers the bottom of a tab screen, so each tab screen leaves this much
    // room under its content (the bar, its margins and the system bars).
    val tabPadding = PaddingValues(bottom = tabBarClearance())

    // The shell draws the screens and floats the tab bar over them; the drawing of the screens
    // themselves is done by the stateless KansoNavDisplay inside it. This function only gathers
    // what it needs (the saved back stack, Back handling, decorators and screens).
    KansoTabShell(
        showBar = top != null && isTabKey(top),
        selected = KansoTab.entries.firstOrNull { it.key == top } ?: KansoTab.HOME,
        // Picking a tab clears the stack down to Home and adds that tab's key. Only act while a
        // tab is on top, so a tap during a move to another screen cannot disturb the stack.
        onTabSelected = { tab -> if (top != null && isTabKey(top)) backStack.selectTab(tab) },
        // The Camera button opens the camera; a shot that is kept opens the editor with the Edit
        // tool selected. Only act while a tab is on top, so a fast double tap opens one camera.
        onCamera = { if (backStack.lastOrNull()?.let(::isTabKey) == true) backStack.add(Camera(Tool.EDIT)) },
        modifier = modifier,
    ) {
        KansoNavDisplay(
        backStack = backStack,
        // Called on system Back (and predictive back); goBack above decides what that means.
        onBack = goBack,
        entryDecorators = entryDecorators,
        // The entry provider maps each kind of key to the composable that draws it.
        entryProvider = entryProvider<NavKey> {
            entry<Home>(metadata = tabMetadata) {
                // koinViewModel asks Koin for the ViewModel, stored in this entry's ViewModelStore:
                // the first call builds it, later calls (after rotation, say) return the same one.
                val viewModel = koinViewModel<HomeViewModel>()
                HomeScreen(
                    viewModel = viewModel,
                    // "Open a photo" is the main action: it opens the gallery to pick from, and the
                    // photo picked there opens in the editor with the Edit tool selected.
                    onOpenPhoto = { if (backStack.last() == Home) backStack.add(Gallery(Tool.EDIT)) },
                    // Every tool card opens the photo gallery, and the photo picked there opens
                    // in the editor with that tool selected.
                    onToolClick = { tool ->
                        if (backStack.last() == Home) backStack.add(Gallery(tool))
                    },
                    contentPadding = tabPadding,
                )
            }
            entry<MyEdits>(metadata = tabMetadata) {
                val viewModel = koinViewModel<MyEditsViewModel>()
                MyEditsScreen(
                    viewModel = viewModel,
                    // Opening one of the edited photos arrives with the photo viewer (T-004).
                    onPhotoClick = {},
                    contentPadding = tabPadding,
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
                    // Replace: back to this tool's gallery to pick another photo, with Back
                    // from there still going where Back from this screen went.
                    onReplace = { if (backStack.last() == key) backStack.replaceWithGallery(key.tool) },
                    // A saved photo replaces everything above Home with the Saved screen, so
                    // Back from Saved goes to Home, not to the tool screen. Only act while the
                    // tool screen is on top. The Saved key carries this screen's tool, so
                    // "Edit another" opens that tool's gallery.
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
            // Settings is a tab: no Back arrow, the bar's Home tab (or system Back) leaves it.
            entry<Settings>(metadata = tabMetadata) {
                val viewModel = koinViewModel<SettingsViewModel>()
                SettingsScreen(
                    viewModel = viewModel,
                    contentPadding = tabPadding,
                )
            }
        },
        )
    }
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
 * How the screens move is set here too: the push, pop and back-gesture transitions come from
 * KansoTransitions.kt, built from the theme's motion scheme, so a screen slides and fades with the
 * Expressive springs. The manifest's `enableOnBackInvokedCallback` switches predictive back on, so
 * the back gesture drives the last of the three; that one departs from the springs for the top
 * screen's slide, which follows the swipe's progress along a straight line, to the right whichever
 * edge the swipe starts from (the reason is in its KDoc).
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
    // The theme's motion scheme (Expressive: springs, which the screens' moves use) is read here,
    // while Compose is drawing, because MaterialTheme.motionScheme only works inside a composable
    // function and the spec lambdas NavDisplay calls later are not composable. The three functions
    // below take the scheme and build the lambdas; see KansoTransitions.kt for what each one does.
    val motionScheme = MaterialTheme.motionScheme
    NavDisplay(
        backStack = backStack,
        // While two screens slide past each other the old one fades out faster than the new one
        // arrives, so for a moment part of the window shows through. Painting the theme's own
        // background behind both keeps that strip in the app's light or dark colour; the window's
        // own background follows the phone's night setting, which can differ from the app's choice.
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        onBack = onBack,
        entryDecorators = entryDecorators,
        // How the screens move when a screen opens, when one closes, and while the back gesture
        // is still under the finger (predictive back).
        transitionSpec = kansoPushTransition(motionScheme),
        popTransitionSpec = kansoPopTransition(motionScheme),
        predictivePopTransitionSpec = kansoPredictivePopTransition(motionScheme),
        entryProvider = entryProvider,
    )
}

/**
 * Sample data for the preview: the two groups of cards the real Home shows, the AI tools and then
 * Edit.
 */
private val sampleHomeState = HomeUiState(
    aiTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
    editTools = listOf(Tool.EDIT),
)

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
                        state = sampleHomeState,
                        onOpenPhoto = {},
                        onToolClick = {},
                    )
                }
            },
        )
    }
}
