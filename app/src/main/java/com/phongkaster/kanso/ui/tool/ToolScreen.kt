package com.phongkaster.kanso.ui.tool

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.photo.displayMaxEdgePx
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag on the root of the tool screen (the editor). */
const val TOOL_SCREEN_TAG = "tool-screen"

/** The test tag on the photo area: the space between the top bar and the editor's panel. */
const val TOOL_BODY_TAG = "tool-body"

/** The test tag on the photo itself. */
const val TOOL_IMAGE_TAG = "tool-image"

/** The test tag on the message shown when the photo cannot be opened. */
const val TOOL_UNREADABLE_TAG = "tool-unreadable"

/** The test tag on the Save action in the top bar. */
const val TOOL_SAVE_TAG = "tool-save"

/** The test tag on the small spinner the Save action shows while a save runs. */
const val TOOL_SAVE_PROGRESS_TAG = "tool-save-progress"

/** The test tag on the message about the last save (failed, refused, or kept sideways). */
const val SAVE_MESSAGE_TAG = "save-message"

/** The test tag on the panel under the photo that says what the selected tool will offer. */
const val EDITOR_PANEL_TAG = "editor-panel"

/** The test tag on the floating toolbar that holds one toggle per editing tool. */
const val EDITOR_TOOLBAR_TAG = "editor-toolbar"

/**
 * Returns the test tag of [tool]'s toggle in the editor's toolbar, such as `editor-tool-RETOUCH`.
 * The toggles show no text, so tests find each one by this tag (or by its content description).
 */
fun editorToolTag(tool: Tool): String = "editor-tool-${tool.name}"

/**
 * A semantics property carrying the Uri (as text) of the photo the image node shows.
 *
 * Semantics are the facts about each element that accessibility services and tests read, such
 * as its text or its role. This extra, Kanso-only fact lets a test check which photo is on screen
 * without comparing pixels; it is invisible to people and to screen readers.
 */
val TOOL_PHOTO_URI_KEY = SemanticsPropertyKey<String>("PhotoUriKey")

/**
 * The editor: the screen where the selected tool works on one photo. A top bar with a back
 * arrow, the selected tool's name and a Save button; below it the photo, upright and fitted to
 * the space; under the photo a panel saying what the selected tool will offer; and at the bottom
 * a floating toolbar with one toggle per editing tool, to switch tools without leaving the photo.
 *
 * Passing data in a Navigation 3 key: the screen needs two facts to open, which tool and which
 * photo. They are not handed over in memory from the gallery; they travel inside the navigation
 * key, `ToolPhoto(tool, photoUri)`, the value pushed onto the back stack to open this screen. The
 * back stack is saved (every key is @Serializable), so when the phone rotates, or when Android
 * kills the app in the background and later restores it, the same key comes back and this screen
 * reopens on the same photo. The navigation entry passes the key's values to the [viewModel] it
 * builds, which puts them in the screen's state as `tool` and `photoUri`.
 * Only small values belong in a key: the photo goes in as the text of its Uri, never as pixels.
 *
 * The key's tool is only where the editor starts: the toolbar can switch to another tool, and the
 * key does not change (changing it would make Navigation 3 treat it as a new screen). So the
 * selection is kept in two places. The [viewModel] holds it while the app runs, which covers
 * rotation, because a ViewModel outlives the re-created screen. A copy goes into the screen's
 * saved state with `rememberSaveable`, which covers process death: when Android kills the app in
 * the background it keeps the saved state, and a new ViewModel started afterwards is told the
 * kept tool before the screen draws its first frame. Switching tools never decodes the photo
 * again.
 *
 * The photo is decoded to about twice the screen's longest edge (see [displayMaxEdgePx]), in the
 * background, and never at full size. If it cannot be opened, a plain message says so.
 *
 * Save sits in the top bar, clear of the gesture area at the bottom of the screen. It can be
 * tapped once the photo is shown, and writes a new upright JPEG into the phone's `Pictures/Kanso`
 * album (see `MediaStorePhotoSaver`); while it runs, Save shows a small spinner and "Saving…",
 * and the toolbar is switched off, because the tool selected when the save ends decides which
 * gallery "Edit another" opens on the Saved screen.
 * On Android 8.1 and 9 writing there needs the WRITE_EXTERNAL_STORAGE permission, so the first
 * tap asks for it; after a refusal a plain message says nothing was saved. Android 10 and later
 * ask for nothing. A failed save, or a photo too large to turn upright, is also explained by a
 * message at the bottom of the photo.
 *
 * Like Home, this screen is split in two (state hoisting; see `HomeScreen` for the full story).
 * This function is the *stateful* part: it gathers the screen size, the load, the kept tool, the
 * saved events and the write-permission launcher, and hands the state and callbacks to the
 * stateless [ToolContent], which does all the drawing.
 *
 * [onBack] is called when the user taps the back arrow; the caller pops the back stack.
 * [onSaved] is called once per successful save, with the new picture's Uri as text.
 */
@Composable
fun ToolScreen(
    viewModel: ToolViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The screen size in pixels: LocalConfiguration gives it in dp, LocalDensity turns dp into
    // pixels (3 pixels per dp on an xxhdpi phone).
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val maxEdgePx = with(density) {
        displayMaxEdgePx(
            screenWidthPx = configuration.screenWidthDp.dp.roundToPx(),
            screenHeightPx = configuration.screenHeightDp.dp.roundToPx(),
        )
    }

    // LaunchedEffect runs its block once when the screen appears (and again only if maxEdgePx
    // changes). The ViewModel ignores a repeated request for the same size.
    LaunchedEffect(viewModel, maxEdgePx) {
        viewModel.load(maxEdgePx)
    }

    // rememberSaveable is remember plus the saved state: its value is written into the bundle
    // Android keeps when it kills the app in the background, and comes back from it when the
    // screen is rebuilt. It holds the selected tool's name, a plain String every saved state
    // can store, and is null until this screen has been drawn once.
    val keptToolName = rememberSaveable { mutableStateOf<String?>(null) }

    // Once per ViewModel, while the screen is first composed (so before its first frame): a tool
    // found in the saved state is handed to the ViewModel. After process death that ViewModel is
    // new and starts on the key's tool, so this puts the person's choice back. After a rotation
    // the ViewModel already holds the same tool, and selecting it again changes nothing.
    val stateFlow = remember(viewModel) {
        // A name that no tool has any more (an older version of the app saved it) is ignored.
        val keptTool = Tool.entries.firstOrNull { it.name == keptToolName.value }
        if (keptTool != null) viewModel.selectTool(keptTool)
        viewModel.state
    }

    // collectAsState turns the StateFlow into Compose state: when a new snapshot arrives, the
    // screen is drawn again with it.
    val state by stateFlow.collectAsState()

    // SideEffect runs after every successful drawing pass: it copies the selected tool into the
    // saved state, so the copy always matches what the ViewModel says (the first tool, from the
    // key, included). A tap on a toggle also copies it at once, in onSelectTool below.
    SideEffect { keptToolName.value = state.tool.name }

    // Each saved photo is handed to the caller exactly once (the ViewModel sends it through a
    // Channel). rememberUpdatedState lets the long-running collector always call the latest
    // onSaved without restarting when the caller passes a new lambda.
    val currentOnSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(viewModel) {
        viewModel.savedEvents.collect { uri -> currentOnSaved(uri) }
    }

    // The storage-write permission, needed only on Android 8.1 and 9. The launcher is
    // registered on every composition, not only when asking, so that an answer arriving after
    // the activity was re-created still comes back here (see PermissionGate's KDoc). Granted:
    // save. Refused: say so and write nothing.
    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.save() else viewModel.onWritePermissionDenied()
    }

    ToolContent(
        state = state,
        onBack = onBack,
        onSave = {
            if (viewModel.needsWritePermission()) {
                writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                viewModel.save()
            }
        },
        onSelectTool = { tool ->
            viewModel.selectTool(tool)
            // The saved copy is updated here, in the same step as the tap, not only by the
            // SideEffect above, which runs after the next frame: a rotation that came between
            // the tap and that frame would save the old tool, and the restore would then select
            // the old tool again, undoing the tap. It copies what the ViewModel now says rather
            // than the tapped tool, because the ViewModel ignores a switch while a save runs.
            keptToolName.value = viewModel.state.value.tool.name
        },
        modifier = modifier,
    )
}

/**
 * The stateless part of the editor (see [ToolScreen] for why there are two): draws [state], the
 * screen's one snapshot, from top to bottom.
 *
 * - The top bar: the back arrow, the selected tool's name and the Save button.
 * - The photo area: whatever the photo part of [state] says (nothing while loading, the photo
 *   when ready, a plain message when unreadable), plus the message about the last save at its
 *   bottom when the save part has one. It takes all the height the other parts leave.
 * - The panel: a short message saying what the selected tool will offer.
 * - The floating toolbar: one toggle per editing tool, the selected one on. While a save runs
 *   the toggles are switched off, so the tool cannot change under a save that is still going.
 *
 * The photo's Uri in [state] only marks the image node for tests. Taps are reported through
 * callbacks and nothing is changed here: [onBack] for the back arrow, [onSave] for Save (the
 * caller decides whether the storage permission must be asked for first) and [onSelectTool]
 * with the tool whose toggle was tapped.
 */
@Composable
fun ToolContent(
    state: ToolUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSelectTool: (Tool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(TOOL_SCREEN_TAG),
        topBar = {
            EditorTopBar(state = state, onBack = onBack, onSave = onSave)
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar and the system bars take, the navigation bar at
        // the bottom included. consumeWindowInsets tells the content below that those insets are
        // already dealt with, so the toolbar's own inset padding only adds what is still missing.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // weight(1f) gives the photo area all the height the panel and the toolbar leave, so
            // the photo is as large as it can be and the controls stay at the bottom.
            PhotoArea(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            EditorPanel(
                tool = state.tool,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            EditorToolbar(
                selected = state.tool,
                // The tool the person saved with decides which gallery "Edit another" opens
                // afterwards, so it cannot be switched until the save is done.
                enabled = state.save != ToolUiState.SaveState.Saving,
                onSelectTool = onSelectTool,
                // Window insets are the parts of the screen the system draws over or listens on.
                // The system gesture area at the bottom (where a swipe means "go home") is not
                // always the navigation bar's height, so the toolbar also clears whatever part of
                // it the Scaffold's padding has not; then it floats a standard distance above.
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom))
                    .padding(bottom = FloatingToolbarDefaults.ScreenOffset),
            )
        }
    }
}

/**
 * The editor's top bar for [state]: the back arrow (calls [onBack]), the selected tool's name as
 * the title, and the Save button (calls [onSave]).
 *
 * Back and Save use the Material 3 Expressive `shapes` versions of their buttons: given a set of
 * shapes instead of one shape, a button changes its shape while it is pressed (a round button
 * squares its corners a little), animated with the theme's motion scheme.
 *
 * Save is always visible, so the bar does not jump, and can be tapped once the photo is shown and
 * no save is running. While a save runs it shows a small spinner and "Saving…".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(state: ToolUiState, onBack: () -> Unit, onSave: () -> Unit) {
    TopAppBar(
        title = {
            // The tool's name appears exactly once as text on this screen: the toggles below
            // carry it only as a content description, read by screen readers.
            Text(
                text = stringResource(state.tool.labelRes),
                style = MaterialTheme.typography.titleLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                shapes = IconButtonDefaults.shapes(),
                // 48 dp, the smallest touch target Android recommends, for the button's own
                // bounds too (its drawn shape would otherwise be 40 dp).
                modifier = Modifier.size(48.dp),
            ) {
                // The drawable paints white; Icon tints it with the bar's content colour.
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        actions = {
            val saving = state.save == ToolUiState.SaveState.Saving
            Button(
                onClick = onSave,
                shapes = ButtonDefaults.shapes(),
                enabled = state.photo is ToolUiState.PhotoState.Ready && !saving,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .heightIn(min = 48.dp)
                    .testTag(TOOL_SAVE_TAG),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .testTag(TOOL_SAVE_PROGRESS_TAG),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tool_saving))
                } else {
                    // No description of its own: the word "Save" next to it already says it.
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.tool_save))
                }
            }
        },
    )
}

/**
 * The photo area of the editor for [state]: nothing while the photo loads, the photo when it is
 * ready, a plain message when it cannot be opened, and the message about the last save, if any,
 * on a card at its bottom.
 */
@Composable
private fun PhotoArea(state: ToolUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.testTag(TOOL_BODY_TAG),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state.photo) {
            // Decoding takes a moment; the area simply stays empty meanwhile.
            ToolUiState.PhotoState.Loading -> Unit
            is ToolUiState.PhotoState.Ready -> PhotoView(
                current = current,
                photoUri = state.photoUri,
            )
            ToolUiState.PhotoState.Unreadable -> Text(
                text = stringResource(R.string.tool_photo_unreadable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(24.dp)
                    .testTag(TOOL_UNREADABLE_TAG),
            )
        }
        saveMessageRes(state.save)?.let { messageRes ->
            SaveMessage(
                text = stringResource(messageRes),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * The panel under the photo: a short message saying what [tool] will offer once its controls
 * exist. When the person switches tools the old message fades out while the new one fades in.
 *
 * AnimatedContent draws its content for one value (here the tool) and, when the value changes,
 * animates from the old content to the new one with the transition it is given. The fade uses the
 * theme's motion scheme: `defaultEffectsSpec` is its spring for colour and opacity changes, read
 * here while composing because the transition block itself is not a composable.
 */
@Composable
private fun EditorPanel(tool: Tool, modifier: Modifier = Modifier) {
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.testTag(EDITOR_PANEL_TAG),
    ) {
        AnimatedContent(
            targetState = tool,
            transitionSpec = { fadeIn(animationSpec = fade) togetherWith fadeOut(animationSpec = fade) },
            label = "editorPanelMessage",
        ) { shownTool ->
            panelMessageRes(shownTool)?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/**
 * Returns the panel message for [tool]: what that tool will offer once its controls exist.
 * Returns null for a tool the editor does not offer (the editor never opens on one).
 */
private fun panelMessageRes(tool: Tool): Int? = when (tool) {
    Tool.EDIT -> R.string.editor_panel_edit
    Tool.REMOVE_BACKGROUND -> R.string.editor_panel_remove_background
    Tool.ENHANCE -> R.string.editor_panel_enhance
    Tool.RETOUCH -> R.string.editor_panel_retouch
    Tool.ARTISTIC -> R.string.editor_panel_artistic
    else -> null
}

/**
 * The editor's floating toolbar: one toggle per editing tool (`Tool.editingTools`, in that
 * order), with [selected]'s toggle on. Tapping a toggle calls [onSelectTool] with its tool. When
 * [enabled] is false (while a save runs) the toggles stay on screen, with their names for screen
 * readers, but cannot be tapped.
 *
 * A floating toolbar is a pill-shaped bar of actions that floats a little above the bottom of
 * the screen, instead of stretching from edge to edge like a classic bottom bar. Material 3
 * Expressive uses it for the actions of an editing screen. `HorizontalFloatingToolbar` lays its
 * content out in a row inside the pill and colours the pill from the theme; it does not place
 * itself on the screen, so the caller puts it at the bottom.
 */
@Composable
private fun EditorToolbar(
    selected: Tool,
    enabled: Boolean,
    onSelectTool: (Tool) -> Unit,
    modifier: Modifier = Modifier,
) {
    // expanded = true: the toolbar always shows its content (it has no extra slots to hide).
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.testTag(EDITOR_TOOLBAR_TAG),
    ) {
        Tool.editingTools.forEach { tool ->
            ToolToggle(
                tool = tool,
                checked = tool == selected,
                enabled = enabled,
                onSelect = { onSelectTool(tool) },
            )
        }
    }
}

/**
 * The toolbar toggle for [tool]: its icon, on when [checked], and calling [onSelect] when tapped.
 *
 * A toggle button stays on or off, like a light switch, instead of doing something once. Here
 * the five toggles act as one choice: exactly one is on, the selected tool. A tap does not flip
 * the toggle by itself; it calls [onSelect], the ViewModel selects the tool, and the new state
 * turns this toggle on and the previous one off. Tapping the toggle that is already on keeps it
 * on.
 *
 * The toggle shows only its icon. Its name is its content description, which screen readers say
 * and tests read; it is never shown as text, because the title above already shows it.
 *
 * Two things move when the selection changes:
 * - The shape. Given `IconButtonDefaults.toggleableShapes()`, the button is a circle when off and
 *   a rounded square when on (smaller corners still while pressed), and Material morphs from one
 *   shape to the other itself, with the theme's motion scheme.
 * - The colours. `animateColorAsState` returns a colour that does not jump to a new target but
 *   moves towards it over a few frames, following the animation spec it is given. Here that is
 *   the motion scheme's `defaultEffectsSpec`, the spring Material 3 Expressive uses for colour
 *   (its bouncy spatial springs are for movement and size, where a little overshoot reads as
 *   lively, while a colour that overshoots would flash). The container fades from clear to the
 *   theme's primary blue and the icon from grey to the colour drawn on primary.
 *
 * When [enabled] is false the toggle cannot be tapped and its icon fades to a faint version of
 * its colour, the usual Material sign of a control that is switched off for now. It keeps its
 * size and its content description, so screen readers still find it and say it is unavailable.
 */
@Composable
private fun ToolToggle(
    tool: Tool,
    checked: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MaterialTheme.colorScheme
    // Read in composition: MaterialTheme.motionScheme is only available while composing.
    val colourSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    // Off, the container is the same blue made fully see-through, so only its opacity moves.
    val containerColour by animateColorAsState(
        targetValue = if (checked) colours.primary else colours.primary.copy(alpha = 0f),
        animationSpec = colourSpec,
        label = "toolToggleContainer",
    )
    val iconColour = if (checked) colours.onPrimary else colours.onSurfaceVariant
    val contentColour by animateColorAsState(
        // Switched off, the icon keeps its colour at a lower opacity (see the KDoc above).
        targetValue = if (enabled) iconColour else iconColour.copy(alpha = DISABLED_ICON_ALPHA),
        animationSpec = colourSpec,
        label = "toolToggleContent",
    )
    IconToggleButton(
        checked = checked,
        // The tap asks for this tool; the state decides what is on (see the KDoc above).
        onCheckedChange = { onSelect() },
        shapes = IconButtonDefaults.toggleableShapes(),
        enabled = enabled,
        // The same animated colours whether on, off or switched off: the animation (which
        // already dims the icon when the toggle is switched off), not the button, decides.
        colors = IconToggleButtonColors(
            containerColor = containerColour,
            contentColor = contentColour,
            disabledContainerColor = containerColour,
            disabledContentColor = contentColour,
            checkedContainerColor = containerColour,
            checkedContentColor = contentColour,
        ),
        // 48 dp: the recommended touch target, for the drawn button and its bounds alike.
        modifier = modifier
            .size(48.dp)
            .testTag(editorToolTag(tool)),
    ) {
        // The drawable paints white; Icon tints it with the toggle's content colour.
        Icon(
            painter = painterResource(tool.iconRes),
            contentDescription = stringResource(tool.labelRes),
        )
    }
}

/**
 * The opacity of a toolbar icon while its toggle is switched off: 38 %, the value Material uses
 * for the content of a disabled control.
 */
private const val DISABLED_ICON_ALPHA = 0.38f

/**
 * Returns the message to show for [save], or null when there is nothing to say: a running save
 * shows its spinner in the Save action, and a normal successful save says nothing here.
 */
private fun saveMessageRes(save: ToolUiState.SaveState): Int? = when (save) {
    is ToolUiState.SaveState.Saved ->
        if (save.keptOriginalOrientation) R.string.save_kept_orientation else null
    ToolUiState.SaveState.Failed -> R.string.save_failed
    ToolUiState.SaveState.PermissionDenied -> R.string.save_permission_denied
    ToolUiState.SaveState.Idle, ToolUiState.SaveState.Saving -> null
}

/**
 * Shows [text], a message about the last save, on a tinted card at the bottom of the photo area.
 * It stays until the next save replaces it. Screen readers announce it as soon as it appears.
 */
@Composable
private fun SaveMessage(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .padding(16.dp)
            .testTag(SAVE_MESSAGE_TAG)
            // A polite live region: TalkBack reads the message without interrupting itself.
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/**
 * Draws the decoded photo of [current] as large as fits, keeping its shape, and marks the image
 * node with [photoUri] so tests can tell which photo is shown.
 */
@Composable
private fun PhotoView(current: ToolUiState.PhotoState.Ready, photoUri: String) {
    val bitmap = current.bitmap
    // Compose draws an ImageBitmap; wrapping the Android Bitmap is cheap, and remember keeps the
    // wrapper between redraws.
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val description = stringResource(R.string.tool_photo_description)
    Image(
        bitmap = image,
        contentDescription = description,
        // Fit scales the photo down (or up) until it fits whole: never cropped, never stretched.
        contentScale = ContentScale.Fit,
        // aspectRatio sizes the image node itself to the photo's shape, the largest that fits
        // the area, so the node's bounds are exactly where the photo is drawn.
        modifier = Modifier
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .testTag(TOOL_IMAGE_TAG)
            .semantics { this[TOOL_PHOTO_URI_KEY] = photoUri },
    )
}

/** The Uri text the previews pretend their photo came from; only the image node's semantics use it. */
private const val SAMPLE_PHOTO_URI = "content://media/external/images/media/42"

/**
 * A photo for the previews, drawn in code: a 4:3 landscape picture with a sky-to-sea gradient
 * and a round "sun". No file, no MediaStore and no decoder is involved, so a preview can build it.
 */
private fun samplePhoto(): Bitmap {
    val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // A vertical gradient from warm sky to deep blue.
    paint.shader = LinearGradient(
        0f, 0f, 0f, 480f,
        0xFFFFB74D.toInt(), 0xFF1565C0.toInt(),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, 640f, 480f, paint)
    paint.shader = null
    paint.color = 0xFFFFF59D.toInt()
    canvas.drawCircle(460f, 150f, 60f, paint)
    return bitmap
}

/**
 * The editor on the Edit tool with a photo shown and nothing said about saving: the everyday
 * look, drawn once in light and once in dark. `ThemeMode.System` makes KansoTheme follow the
 * preview's day or night; the state is a snapshot made up here from sample data, and the taps do
 * nothing, a preview only draws.
 */
@PreviewLightDark
@Composable
private fun ToolPhotoPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.EDIT,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                save = ToolUiState.SaveState.Idle,
            ),
            onBack = {},
            onSave = {},
            onSelectTool = {},
        )
    }
}

/**
 * The editor after switching to Enhance: the title, the panel's message and the toolbar's
 * selected toggle all follow the tool, while the photo stays the same.
 */
@PreviewLightDark
@Composable
private fun ToolEnhancePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.ENHANCE,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                save = ToolUiState.SaveState.Idle,
            ),
            onBack = {},
            onSave = {},
            onSelectTool = {},
        )
    }
}

/**
 * The editor when the photo cannot be opened: the plain message in the middle and Save switched
 * off.
 */
@PreviewLightDark
@Composable
private fun ToolUnreadablePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.EDIT,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Unreadable,
                save = ToolUiState.SaveState.Idle,
            ),
            onBack = {},
            onSave = {},
            onSelectTool = {},
        )
    }
}

/**
 * The editor after a failed save: the photo with the explaining message on its card at the
 * bottom, which is how [SaveMessage] is drawn.
 */
@PreviewLightDark
@Composable
private fun ToolSaveFailedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.EDIT,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                save = ToolUiState.SaveState.Failed,
            ),
            onBack = {},
            onSave = {},
            onSelectTool = {},
        )
    }
}
