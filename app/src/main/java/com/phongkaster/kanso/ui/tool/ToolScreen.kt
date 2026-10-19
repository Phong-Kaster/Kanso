package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.artistic.ArtisticProcessor
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.retouch.RetouchProcessor
import com.phongkaster.kanso.data.photo.displayMaxEdgePx
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.common.BlurredBackdrop
import com.phongkaster.kanso.ui.common.RoundBackButton
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag on the root of the tool screen. */
const val TOOL_SCREEN_TAG = "tool-screen"

/** The test tag on the round Back button at the top left of the tool screen. */
const val TOOL_BACK_TAG = "tool-back"

/** The test tag on the large rounded card that holds the photo. */
const val TOOL_CARD_TAG = "tool-card"

/**
 * The test tag on the inside of the photo card: the whole area the card holds, where the photo,
 * its blurred backdrop, the loading indicator, the "cannot be opened" message and the failure
 * message are drawn. It has the card's own bounds; the loading indicator sits in its middle.
 */
const val TOOL_BODY_TAG = "tool-body"

/** The test tag on the photo itself. */
const val TOOL_IMAGE_TAG = "tool-image"

/** The test tag on the "Replace" chip floating at the bottom of the photo card. */
const val TOOL_REPLACE_TAG = "tool-replace"

/** The test tag on the message shown when the photo cannot be opened. */
const val TOOL_UNREADABLE_TAG = "tool-unreadable"

/**
 * The test tag on the loading indicator shown in the middle of the photo card while the photo is
 * being decoded. It is gone as soon as the photo, or the message saying it cannot be opened, is
 * shown.
 */
const val TOOL_LOADING_TAG = "tool-loading"

/** The test tag on the Done button at the top right of every tool screen. */
const val TOOL_DONE_TAG = "tool-done"

/**
 * The test tag on the small spinner inside the Done button while the Edit screen renders the
 * finished picture.
 */
const val TOOL_DONE_PROGRESS_TAG = "tool-done-progress"

/**
 * The test tag on the options strip under the photo card, the slot that holds the tool's own
 * controls (see `ToolPanel`).
 */
const val EDITOR_PANEL_TAG = "editor-panel"

/**
 * The test tag on the progress indicator shown in the middle of the photo card while an edit
 * runs. It is gone as soon as the edit ends, well or badly.
 */
const val TOOL_PROGRESS_TAG = "tool-progress"

/** The test tag on the Undo control at the top right, which takes back the last edit. */
const val TOOL_UNDO_TAG = "tool-undo"

/** The test tag on the plain-words message shown when an edit did not work. */
const val TOOL_FAILURE_TAG = "tool-failure"

/** Test tag of the node around the failure card when no face was found (the Retouch tool). */
const val RETOUCH_NO_FACE_TAG = "retouch-no-face"

/**
 * A semantics property carrying the Uri (as text) of the photo the image node shows.
 *
 * Semantics are the facts about each element that accessibility services and tests read, such
 * as its text or its role. This extra, Kanso-only fact lets a test check which photo is on screen
 * without comparing pixels; it is invisible to people and to screen readers.
 */
val TOOL_PHOTO_URI_KEY = SemanticsPropertyKey<String>("PhotoUriKey")

/**
 * A tool screen: one tool working on one photo. Each of the five tools has a screen of its own,
 * opened for that tool and showing only it; this one function draws all five, told by its
 * [viewModel] which tool it is for. From top to bottom:
 * - a round Back button at the top left, the tool's name centred, and at the right an Undo
 *   control followed by the Done button;
 * - a large rounded card holding the photo (the edited one once an edit is made), as large as
 *   fits, over a soft blurred copy of itself that fills the rest of the card, with a "Replace"
 *   chip floating at the card's bottom;
 * - the options strip, holding the tool's own controls (`ToolPanel`).
 * There is no bar at the bottom: the finished picture leaves this screen through Done.
 *
 * The Edit screen differs (see [ToolContent]): its photo keeps 60% of the height, with no
 * "Replace" chip; under it a row of four tools (Crop, Rotate, Adjust, Filters) each opens its
 * controls in a sheet below; and Undo sits with a Compare switch in a pill at the photo's bottom
 * right instead of in the top bar, so the top bar's right end holds Done alone.
 *
 * While an edit runs, a progress ring fills in the middle of the card and Done and Undo are
 * switched off, so a half-made edit is never handed over by mistake; the screen stays responsive
 * because the edit itself runs off the main thread (see `ToolViewModel.apply`). When an edit
 * fails, a plain message under the top of the card says why, and the photo shown is unchanged.
 *
 * Done can be tapped once the photo is shown. It writes nothing: it hands the finished picture to
 * the Result screen (see `ToolViewModel.done`), which saves or shares it if asked. On the other
 * four tools that is the picture shown now; on Edit the current settings are first rendered at
 * full size from the original photo, and meanwhile Done shows a small spinner and ignores taps.
 * The editing session is not ended, so Back from Result returns here with everything intact.
 *
 * Passing data in a Navigation 3 key: the screen needs two facts to open, which tool and which
 * photo. They are not handed over in memory from the gallery; they travel inside the navigation
 * key, `ToolPhoto(tool, photoUri)`, the value pushed onto the back stack to open this screen. The
 * back stack is saved (every key is @Serializable), so when the phone rotates, or when Android
 * kills the app in the background and later restores it, the same key comes back and this screen
 * reopens with the same tool on the same photo. The navigation entry passes the key's values to
 * the [viewModel] it builds, which puts them in the screen's state as `tool` and `photoUri`.
 * Only small values belong in a key: the photo goes in as the text of its Uri, never as pixels.
 *
 * The photo is decoded to about twice the screen's longest edge (see [displayMaxEdgePx]), in the
 * background, and never at full size; meanwhile a loading indicator moves in the middle of the
 * card. If the photo cannot be opened, a plain message says so.
 *
 * Like Home, this screen is split in two (state hoisting; see `HomeScreen` for the full story).
 * This function is the *stateful* part: it gathers the screen size, the load and the Done events,
 * and hands the state and callbacks to the stateless [ToolContent], which does all the drawing.
 *
 * [onBack] is called when the person taps Back; the caller pops the back stack.
 * [onReplace] is called when the person taps "Replace"; the caller shows this tool's photo
 * gallery, so another photo can be picked for the same tool.
 * [onDone] is called once per finished Done, after the picture is in the hand-off store; the
 * caller opens the Result screen above this one.
 */
@Composable
fun ToolScreen(
    viewModel: ToolViewModel,
    onBack: () -> Unit,
    onReplace: () -> Unit = {},
    onDone: () -> Unit,
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

    // collectAsState turns the StateFlow into Compose state: when a new snapshot arrives, the
    // screen is drawn again with it.
    val state by viewModel.state.collectAsState()

    // Each finished Done reaches the caller exactly once (the ViewModel sends it through a
    // Channel). rememberUpdatedState lets the long-running collector always call the latest
    // onDone without restarting when the caller passes a new lambda.
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(viewModel) {
        viewModel.doneEvents.collect { currentOnDone() }
    }

    ToolContent(
        state = state,
        // The top bar's Back follows the same rule as the system Back (see the BackHandler below).
        onBack = { if (!(state.tool == Tool.EDIT && viewModel.onBackPressed())) onBack() },
        onDone = viewModel::done,
        onReplace = onReplace,
        modifier = modifier,
        // The tool's own controls, drawn in the options strip under the photo card.
        panel = {
            ToolPanel(
                tool = state.tool,
                state = state,
                onRun = { viewModel.runRegistered() },
                onRetouch = { strength ->
                    viewModel.runRegistered(fromOriginal = true) { processor ->
                        (processor as? RetouchProcessor)?.strength = strength
                    }
                },
                onStylize = { style, strength ->
                    viewModel.runRegistered(fromOriginal = true) { processor ->
                        (processor as? ArtisticProcessor)?.let {
                            it.style = style
                            it.strength = strength
                        }
                    }
                },
            )
        },
        // A method reference (viewModel::undo) is a ready-made lambda that calls that method.
        onUndo = viewModel::undo,
        onDismissFailure = viewModel::dismissFailure,
        // The Edit screen only: a tap on Crop, Rotate, Adjust or Filters opens (or closes) that
        // tool's sheet, and the pill's Compare switch shows the original photo while it is on.
        onOpenTool = viewModel::openTool,
        onCompare = viewModel::setComparing,
        // The Edit screen only: while Crop is open (and the photo is decoded) the photo card is the
        // real cropper on the original photo; a finished gesture is one crop action of the
        // settings session (one Undo step). Until the photo is Ready there is no cropper, so the
        // loading indicator or the unreadable message stays on screen.
        editCropper = (state.photo as? ToolUiState.PhotoState.Ready)?.let { ready ->
            {
                val context = LocalContext.current
                val processorFor: (EditSettings) -> ToolProcessor = remember(context) {
                    val appContext = context.applicationContext
                    val filters = FilterLut.fromAssets { path -> appContext.assets.open(path) }
                    val build: (EditSettings) -> ToolProcessor = { wanted -> EditProcessor(wanted, filters) }
                    build
                }
                EditCropper(
                    bitmap = ready.bitmap,
                    settings = state.settings,
                    enabled = !state.comparing && !state.finishing,
                    onCropFinished = { rect -> viewModel.commitCrop(rect, processorFor) },
                )
            }
        },
        // The Edit screen only: the open tool's controls, drawn in the sheet under the photo.
        // They are switched off while Compare shows the original and while Done is rendering the
        // final picture (`!state.comparing && !state.finishing`). An ordinary preview render does
        // not disable them. Every change goes to the ViewModel's one settings session, which
        // re-renders from the original photo.
        editSheet = { tool ->
            ToolPanel(
                tool = Tool.EDIT,
                state = state,
                openTool = tool,
                enabled = !state.comparing && !state.finishing,
                onEditChange = viewModel::changeSettings,
                onEditPreview = viewModel::previewSettings,
                onEditCommit = viewModel::commitSettings,
                onEditRatio = viewModel::chooseCropRatio,
            )
        },
    )

    // The Edit screen only: Back first closes an open sheet, then (with changes) asks before
    // leaving. BackHandler takes over the system Back only while it is enabled, so the other
    // tools keep the plain back behaviour.
    BackHandler(enabled = state.tool == Tool.EDIT && (state.openTool != null || !state.settings.isNeutral)) {
        if (!viewModel.onBackPressed()) onBack()
    }
    if (state.discardPrompt) {
        DiscardChangesDialog(
            onDiscard = {
                viewModel.confirmDiscard()
                onBack()
            },
            onKeepEditing = viewModel::dismissDiscardPrompt,
        )
    }
}

/**
 * The stateless part of the tool screen (see [ToolScreen] for why there are two): draws [state],
 * the screen's one snapshot, from top to bottom.
 *
 * - The top bar: the round Back button, the tool's name, centred, and at the right the Undo
 *   control (usable only when there is an edit to take back and no edit is running) and the Done
 *   button (usable once the photo is shown, while no edit runs and no Done is rendering).
 * - The photo card: whatever the photo part of [state] says (a loading indicator while the photo
 *   is decoded, the photo over its blurred backdrop when ready, a plain message when unreadable);
 *   the edited photo in place of the original once an edit has been made; a progress ring while
 *   an edit runs; the message about the last failed edit at its top; and the "Replace" chip at
 *   its bottom. It takes all the height the options strip leaves.
 * - The options strip: [panel], the tool's own controls, at the bottom. There is no bottom bar.
 *
 * `Scaffold` is Material's frame for a screen: it places a top bar and hands the content the
 * space left under it (`innerPadding`), so nothing is drawn under the bar.
 *
 * [panel] is a composable lambda, a slot: the caller decides what is drawn there and this function
 * only decides where. It is empty by default, so tests and previews that do not care about the
 * tool's controls need not pass one.
 *
 * The photo's Uri in [state] only marks the image node for tests. Taps are reported through
 * callbacks and nothing is changed here: [onBack] for Back, [onDone] for Done, [onReplace] for
 * "Replace", [onUndo] for Undo and [onDismissFailure] for a tap on the failure message, which
 * hides it.
 *
 * The root node sets `testTagsAsResourceId`, still marked experimental (hence the opt-in), so
 * tools such as uiautomator can find the tags.
 *
 * **The Edit screen is laid out differently.** Its body is [EditToolContent]: the photo card fixed
 * at 60% of the height below the top bar (without the "Replace" chip), the row of four tools
 * (Crop, Rotate, Adjust, Filters) under it, and the open tool's sheet in the rest. [editSheet] is
 * the slot for that sheet's controls; it is given the open tool, so one lambda serves all four.
 * [panel] is not used on Edit. The Undo control moves from the top bar into the pill floating at
 * the photo's bottom right, next to the Compare switch: [onCompare] is called with its new value,
 * and while it is on the card shows the untouched original photo. [onOpenTool] is a tool in the
 * row tapped. The other four tools never call these three.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ToolContent(
    state: ToolUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onReplace: () -> Unit = {},
    modifier: Modifier = Modifier,
    panel: @Composable () -> Unit = {},
    onUndo: () -> Unit = {},
    onDismissFailure: () -> Unit = {},
    onOpenTool: (EditTool) -> Unit = {},
    onCompare: (Boolean) -> Unit = {},
    editSheet: @Composable (EditTool) -> Unit = {},
    editCropper: (@Composable () -> Unit)? = null,
) {
    val isEdit = state.tool == Tool.EDIT
    Scaffold(
        modifier = modifier
            .testTag(TOOL_SCREEN_TAG)
            // Exposes every test tag below as an Android resource id, so tools outside the app
            // (UI Automator, a device dump) can find the screen's controls by tag.
            .semantics { testTagsAsResourceId = true },
        topBar = {
            ToolTopBar(
                tool = state.tool,
                onBack = onBack,
                // On Edit, Undo lives in the pill on the photo instead, so there is only one.
                showUndo = !isEdit,
                undoEnabled = state.canUndo && state.processing is ToolUiState.Processing.Idle &&
                    !state.finishing,
                onUndo = onUndo,
                doneEnabled = state.photo is ToolUiState.PhotoState.Ready &&
                    state.processing is ToolUiState.Processing.Idle && !state.finishing,
                finishing = state.finishing,
                onDone = onDone,
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar and the system bars take. consumeWindowInsets
        // tells the content below that those insets are already dealt with.
        val bodyModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            // With no bottom bar, the tool's controls are the lowest thing on screen, so they keep
            // clear of the gesture area too: with swipe gestures the navigation bar can be 0 dp
            // tall while a swipe up from the edge still means "go home". Only the part of it not
            // already in innerPadding is added, then a small margin.
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
            )
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        if (isEdit) {
            EditToolContent(
                state = state,
                onOpenTool = onOpenTool,
                onUndo = onUndo,
                onCompare = onCompare,
                photo = {
                    PhotoCard(
                        state = state,
                        onReplace = onReplace,
                        onDismissFailure = onDismissFailure,
                        showReplace = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                cropper = editCropper,
                sheet = { tool ->
                    // The same options-strip tag as the other tools' panels carry, so the open
                    // tool's controls are found in the same place on every tool screen.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(EDITOR_PANEL_TAG),
                    ) {
                        editSheet(tool)
                    }
                },
                modifier = bodyModifier,
            )
        } else {
            Column(modifier = bodyModifier) {
                // weight(1f) gives the card all the height the options strip leaves, so the photo
                // is as large as it can be.
                PhotoCard(
                    state = state,
                    onReplace = onReplace,
                    onDismissFailure = onDismissFailure,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                // The options strip: the slot for the tool's own controls, full width under the
                // card.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(EDITOR_PANEL_TAG),
                ) {
                    panel()
                }
            }
        }
    }
}

/**
 * The tool screen's top bar: the round Back button at the left (calls [onBack]), [tool]'s name
 * centred across the screen, and at the right the Undo control (calls [onUndo]; it does nothing
 * while [undoEnabled] is false) followed by the Done button (calls [onDone]; it does nothing while
 * [doneEnabled] is false, and shows a small spinner while [finishing]). With [showUndo] false (the
 * Edit screen, whose Undo sits in the pill on the photo) Done stands alone at the right end, so
 * the screen never has two Undo controls.
 *
 * `CenterAlignedTopAppBar` is Material 3's top bar whose title sits in the middle of the screen
 * rather than beside the Back button; it also keeps clear of the status bar at the top. The
 * tool's name appears exactly once as text on this screen: here ("Done" and "Undo" are never a
 * tool's name). Its `actions` slot holds the controls drawn at the bar's right end, in a row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolTopBar(
    tool: Tool,
    onBack: () -> Unit,
    showUndo: Boolean,
    undoEnabled: Boolean,
    onUndo: () -> Unit,
    doneEnabled: Boolean,
    finishing: Boolean,
    onDone: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(tool.labelRes),
                style = MaterialTheme.typography.titleLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            RoundBackButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag(TOOL_BACK_TAG),
            )
        },
        actions = {
            if (showUndo) {
                UndoButton(enabled = undoEnabled, onClick = onUndo)
            }
            DoneButton(
                enabled = doneEnabled,
                finishing = finishing,
                onClick = onDone,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
    )
}

/**
 * The Done button at the top right of every tool screen: a small filled button with the word
 * "Done", calling [onClick] to finish the edit and open the Result screen. While [enabled] is
 * false (the photo not shown yet, an edit running, or Done already working) it is drawn faint and
 * ignores taps.
 *
 * A filled button is Material's strongest button, used here because Done is the screen's main way
 * forward; its theme colours (`primary` behind `onPrimary` text) keep the word readable in light
 * and dark. Material gives every button a touch target of at least 48 dp, even where the drawn
 * button is smaller.
 *
 * While [finishing] (the Edit screen rendering the finished picture), a small spinner stands
 * before the word, so the person sees that Done is working. It is a `CircularProgressIndicator`
 * without a value: it turns for as long as it is shown and stops when the render ends. Its
 * colour is `LocalContentColor`, the colour the button hands to everything inside it, so it
 * always matches the word next to it. It has no description of its own: the word "Done" names the
 * button, and the button is switched off while it turns.
 */
@Composable
private fun DoneButton(
    enabled: Boolean,
    finishing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.testTag(TOOL_DONE_TAG),
    ) {
        // AnimatedVisibility grows the spinner in when the render starts and out when it ends.
        // Inside the button's row this is the RowScope version, which grows it sideways.
        AnimatedVisibility(visible = finishing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(TOOL_DONE_PROGRESS_TAG),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
        }
        Text(stringResource(R.string.tool_done))
    }
}

/**
 * The Undo control: the word "Undo" as a text button, calling [onClick] to take back the last
 * edit, one at a time, down to the original photo. While [enabled] is false (nothing to take
 * back, or an edit running) it is drawn faint and ignores taps.
 *
 * A text button is Material's quietest button, a word with no container, the usual look for a
 * secondary action in a top bar. Its visible word is also what screen readers say for it.
 */
@Composable
private fun UndoButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.testTag(TOOL_UNDO_TAG),
    ) {
        Text(stringResource(R.string.tool_undo))
    }
}

/**
 * The large rounded card holding the photo for [state], full width, with the "Replace" chip
 * floating at its bottom (calls [onReplace]).
 *
 * A `Card` is Material's container for one piece of content: a rounded surface in the theme's
 * container colour. Its shape is the theme's `extraLarge` corner, the biggest rounding of the
 * theme's shape scale, and it clips what it holds to that shape, so the photo and its backdrop
 * get the same rounded corners.
 *
 * Inside it ([TOOL_BODY_TAG]): a loading indicator while the photo is decoded; the photo when it
 * is ready, as large as fits, over a [BlurredBackdrop] of itself that fills the rest of the card;
 * a plain message when it cannot be opened; a progress ring ([EditProgress]) while an edit runs;
 * and, at the top, the message about the last failed edit, if any (a tap hides it, calling
 * [onDismissFailure]). The "Replace" chip floats 16 dp above the card's bottom edge over all of it.
 *
 * Once an edit has been made, the card shows `state.edited` instead of the decoded original; a
 * failed edit leaves `edited` as it was, so the photo shown never changes on a failure. While
 * `state.comparing` is on (the Edit screen's Compare switch), the card shows the decoded original
 * instead, untouched; `edited` is kept, so turning Compare off brings the edited photo back.
 *
 * Replace is switched off while an edit runs, so the screen cannot be left under work that has
 * not finished. With [showReplace] false (the Edit screen) no Replace chip is drawn at
 * all, so the bottom right of the photo is free for the Undo and Compare pill.
 */
@Composable
private fun PhotoCard(
    state: ToolUiState,
    onReplace: () -> Unit,
    onDismissFailure: () -> Unit,
    modifier: Modifier = Modifier,
    showReplace: Boolean = true,
) {
    val running = state.processing as? ToolUiState.Processing.Running
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.testTag(TOOL_CARD_TAG),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TOOL_BODY_TAG),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = state.photo) {
                // Decoding takes a moment; meanwhile an indicator moves in the middle of the card.
                ToolUiState.PhotoState.Loading -> PhotoLoadingIndicator()
                is ToolUiState.PhotoState.Ready -> {
                    // The edited photo when there is one, else the decoded original; while Compare
                    // is on, always the original, so the person sees what they started from.
                    val shown = if (state.comparing) current.bitmap else state.edited ?: current.bitmap
                    // Compose draws an ImageBitmap; wrapping the Android Bitmap is cheap, and
                    // remember keeps the wrapper between redraws.
                    val image = remember(shown) { shown.asImageBitmap() }
                    if (state.tool == Tool.REMOVE_BACKGROUND && shown.hasAlpha()) {
                        // A cut-out has see-through parts: show a checkerboard behind them
                        // instead of the blurred copy of the photo.
                        Checkerboard(Modifier.fillMaxSize())
                    } else {
                        BlurredBackdrop(photo = image)
                    }
                    if (state.tool == Tool.EDIT && !state.comparing) {
                        // The Edit screen draws the photo live: the settings' crop, turns, flips and
                        // colour sliders are applied by Compose as it draws, and only a filter is
                        // a re-rendered bitmap (state.filterPreview). Nothing is rendered in full
                        // until Done.
                        // The node is sized to the shape of what is drawn (the cropped, turned
                        // photo), so its bounds are exactly where the photo is.
                        val crop = state.settings.crop
                        val cropWidth = (crop.right - crop.left).coerceAtLeast(0.01f) * current.bitmap.width
                        val cropHeight = (crop.bottom - crop.top).coerceAtLeast(0.01f) * current.bitmap.height
                        val turned = state.settings.quarterTurns % 2 != 0
                        val photoDescription = stringResource(R.string.tool_photo_description)
                        Box(
                            modifier = Modifier
                                .aspectRatio(if (turned) cropHeight / cropWidth else cropWidth / cropHeight)
                                .testTag(TOOL_IMAGE_TAG)
                                .semantics {
                                    this[TOOL_PHOTO_URI_KEY] = state.photoUri
                                    contentDescription = photoDescription
                                },
                        ) {
                            LivePreviewImage(
                                bitmap = state.filterPreview ?: current.bitmap,
                                settings = state.settings,
                            )
                        }
                    } else {
                        PhotoView(image = image, photoUri = state.photoUri)
                    }
                }
                ToolUiState.PhotoState.Unreadable -> Text(
                    text = stringResource(R.string.tool_photo_unreadable),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .testTag(TOOL_UNREADABLE_TAG),
                )
            }
            // AnimatedVisibility fades the ring in when an edit starts and out when it ends; while
            // it fades out the edit is over, so the ring is drawn full.
            androidx.compose.animation.AnimatedVisibility(visible = running != null, enter = fadeIn(), exit = fadeOut()) {
                EditProgress(progress = running?.progress ?: 1f)
            }
            // The failed edit's message sits at the top of the card.
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.failure?.let { failure ->
                    // The no-face message also sits in a node of its own, tagged for Retouch, the
                    // one tool that can fail this way; its card keeps the common failure tag.
                    Box(
                        if (failure == EditFailure.NoFace) Modifier.testTag(RETOUCH_NO_FACE_TAG) else Modifier,
                    ) {
                        FailureMessage(
                            text = stringResource(failureMessageRes(failure)),
                            onDismiss = onDismissFailure,
                        )
                    }
                }
            }
            if (showReplace) {
                ReplaceChip(
                    enabled = running == null,
                    onClick = onReplace,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

/**
 * The "Replace" chip at the bottom of the photo card: a pill with the swap icon and the word
 * "Replace", calling [onClick] when tapped (and doing nothing while [enabled] is false).
 *
 * A chip is Material's small, rounded control for one quick action next to some content. The
 * elevated assist chip has a solid container in the theme's `surfaceContainerLow` colour and a
 * small shadow, so its word stays readable over any photo (a flat chip is see-through). Its
 * default corners are small; `CircleShape` rounds them fully into a pill. Its touch target is at
 * least 48 dp tall although the drawn chip is 32 dp.
 */
@Composable
private fun ReplaceChip(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedAssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(stringResource(R.string.tool_screen_replace)) },
        leadingIcon = {
            // No description of its own: the word "Replace" next to it already says it.
            Icon(
                painter = painterResource(R.drawable.ic_swap_horiz),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        shape = CircleShape,
        modifier = modifier.testTag(TOOL_REPLACE_TAG),
    )
}

/**
 * The loading indicator in the middle of the photo card while the photo is being decoded, so the
 * person sees that the photo is on its way instead of an empty card.
 *
 * `LoadingIndicator` is the Material 3 Expressive way of saying "please wait". Instead of the
 * classic spinning ring, a small rounded shape keeps morphing into the next one (a soft burst, a
 * cookie, a pentagon, a pill and so on) while it slowly turns. Called without a progress value,
 * as here, it does not know how long the wait will last, so it animates for as long as it is
 * shown; that is why no preview shows the loading state (a preview draws one still picture).
 * Here it has its standard 48 dp box and its default colour, the theme's primary blue.
 * It goes away by itself as soon as the photo is ready or turns out to be unreadable, because
 * the card then draws that instead.
 *
 * It shows no text, so the tool's name stays the only text naming the tool on this screen. Its
 * content description, which screen readers say, tells that the photo is being opened.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PhotoLoadingIndicator() {
    val description = stringResource(R.string.tool_loading)
    LoadingIndicator(
        modifier = Modifier
            .testTag(TOOL_LOADING_TAG)
            // Set on the indicator's own modifier, the description becomes its spoken name.
            .semantics { contentDescription = description },
    )
}

/**
 * The progress ring in the middle of the photo card while an edit runs, filled to [progress]
 * (0 = just started, 1 = done).
 *
 * It is a determinate `CircularProgressIndicator`: given a value, it draws that share of the ring
 * and does not move by itself, so it never keeps a test or a preview waiting. The value the edit
 * reports jumps in steps; `animateFloatAsState` turns each jump into a short smooth glide from the
 * old value to the new one, so the ring fills steadily. It sits on a small round surface in the
 * theme's `surfaceContainerHigh` colour, so it stays visible over any photo.
 *
 * It shows no text; its content description, which screen readers say, tells that the photo is
 * being edited, and the indicator itself reports how far along it is.
 */
@Composable
private fun EditProgress(progress: Float) {
    val description = stringResource(R.string.tool_progress_description)
    // The animated value follows `progress`, gliding to each new one instead of jumping.
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "edit progress",
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = CircleShape,
    ) {
        CircularProgressIndicator(
            // A lambda, so only the drawing is redone as the value glides, not the layout.
            progress = { shown },
            modifier = Modifier
                .padding(12.dp)
                .size(48.dp)
                .testTag(TOOL_PROGRESS_TAG)
                .semantics { contentDescription = description },
        )
    }
}

/**
 * Returns the plain-words message for [failure]: what went wrong, and that the photo is
 * unchanged. The `when` has no `else` on purpose: a new kind of [EditFailure] would not compile
 * until it is given a message here.
 */
private fun failureMessageRes(failure: EditFailure): Int = when (failure) {
    EditFailure.OutOfMemory -> R.string.tool_failed_out_of_memory
    EditFailure.NoSubject -> R.string.tool_failed_no_subject
    EditFailure.NoFace -> R.string.tool_failed_no_face
    EditFailure.ModelUnavailable -> R.string.tool_failed_model_unavailable
    EditFailure.Other -> R.string.tool_failed_other
}

/**
 * Shows [text], why the last edit did not work, on a card in the theme's error-container colours
 * at the top of the photo card. A tap on it calls [onDismiss], which hides it; the next edit
 * replaces it too. Screen readers announce it as soon as it appears.
 */
@Composable
private fun FailureMessage(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    // Surface with onClick is a tappable card: it gets press feedback and a button's semantics.
    Surface(
        onClick = onDismiss,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .padding(16.dp)
            .testTag(TOOL_FAILURE_TAG)
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
 * Draws the decoded photo [image] as large as fits in the card, keeping its shape, and marks the
 * image node with [photoUri] so tests can tell which photo is shown.
 */
@Composable
private fun PhotoView(image: ImageBitmap, photoUri: String) {
    val description = stringResource(R.string.tool_photo_description)
    Image(
        bitmap = image,
        contentDescription = description,
        // Fit scales the photo down (or up) until it fits whole: never cropped, never stretched.
        contentScale = ContentScale.Fit,
        // aspectRatio sizes the image node itself to the photo's shape, the largest that fits
        // the card, so the node's bounds are exactly where the photo is drawn: it fills the card
        // in one direction, and the blurred backdrop shows in the space left in the other.
        modifier = Modifier
            .aspectRatio(image.width.toFloat() / image.height.toFloat())
            .testTag(TOOL_IMAGE_TAG)
            .semantics { this[TOOL_PHOTO_URI_KEY] = photoUri },
    )
}

/** The Uri text the previews pretend their photo came from; only the image node's semantics use it. */
private const val SAMPLE_PHOTO_URI = "content://media/external/images/media/42"

/**
 * A photo for the previews, drawn in code: a 4:3 landscape picture with a sky-to-sea gradient
 * and a round "sun". No file and no decoder is involved, so a preview can build it.
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
 * A narrow photo for the previews, drawn in code: a 300 x 800 portrait picture with a warm top,
 * a cool bottom and a round "sun", so the card shows the blurred backdrop at both sides of it.
 */
private fun sampleNarrowPhoto(): Bitmap {
    val bitmap = Bitmap.createBitmap(300, 800, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(
        0f, 0f, 0f, 800f,
        0xFFFF7043.toInt(), 0xFF283593.toInt(),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, 300f, 800f, paint)
    paint.shader = null
    paint.color = 0xFFFFF59D.toInt()
    canvas.drawCircle(150f, 220f, 70f, paint)
    return bitmap
}

/**
 * The Edit screen with a photo shown and Done ready at the top right: the everyday look, drawn once
 * in light and once in dark. `ThemeMode.System` makes KansoTheme follow the preview's day or
 * night; the state is a snapshot made up here from sample data, and the taps do nothing, a
 * preview only draws.
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
            ),
            onBack = {},
            onDone = {},
            onReplace = {},
            panel = { ToolPanel(Tool.EDIT) },
        )
    }
}

/**
 * The Enhance screen on the same photo: the title and the controls in the options strip are
 * Enhance's own, and the top bar's right end holds Undo (off: nothing to take back) and Done.
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
            ),
            onBack = {},
            onDone = {},
            onReplace = {},
            panel = { ToolPanel(Tool.ENHANCE) },
        )
    }
}

/**
 * A tall, narrow photo on the Retouch screen: the photo fills the card's height, and the blurred
 * copy of it fills the space at its left and right.
 */
@PreviewLightDark
@Composable
private fun ToolNarrowPhotoPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.RETOUCH,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(sampleNarrowPhoto()),
            ),
            onBack = {},
            onDone = {},
            onReplace = {},
            panel = { ToolPanel(Tool.RETOUCH) },
        )
    }
}

/**
 * The tool screen when the photo cannot be opened: the plain message in the middle of the card
 * and Done switched off.
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
            ),
            onBack = {},
            onDone = {},
            onReplace = {},
            panel = { ToolPanel(Tool.EDIT) },
        )
    }
}

/**
 * The Edit screen after Done's full-size render ran out of memory: the person stays on the
 * screen, the plain message sits at the top of the card, and Done can be tapped again.
 */
@PreviewLightDark
@Composable
private fun ToolDoneFailedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.EDIT,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                failure = EditFailure.OutOfMemory,
            ),
            onBack = {},
            onDone = {},
            onReplace = {},
            panel = { ToolPanel(Tool.EDIT) },
        )
    }
}

/**
 * The Remove background screen while an edit runs, a little over half done: the progress ring
 * in the middle of the photo, and Done, Undo and Replace switched off. The ring has a fixed
 * value and does not move by itself, so the preview draws one still picture.
 */
@PreviewLightDark
@Composable
private fun ToolEditingPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.REMOVE_BACKGROUND,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                canUndo = true,
                processing = ToolUiState.Processing.Running(progress = 0.6f),
            ),
            onBack = {},
            onDone = {},
            onReplace = {},
            panel = { ToolPanel(Tool.REMOVE_BACKGROUND) },
        )
    }
}

/**
 * The Retouch screen after an edit failed because no face was found: the plain message at the
 * top of the card, the photo unchanged under it, and Undo usable because an earlier edit
 * (the narrow sample photo standing in for it) can still be taken back.
 */
@PreviewLightDark
@Composable
private fun ToolEditFailedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.RETOUCH,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                edited = sampleNarrowPhoto(),
                canUndo = true,
                failure = EditFailure.NoFace,
            ),
            onBack = {},
            onDone = {},
            onReplace = {},
            panel = { ToolPanel(Tool.RETOUCH) },
        )
    }
}

/**
 * The sheet content the Edit previews draw: the stateless Edit panel for [tool] with its controls
 * at rest, every touch ignored, and [enabled] passed through. It is the stateless half on purpose,
 * so a preview never reads the app's assets for the filter images.
 */
@Composable
private fun SampleEditSheet(tool: EditTool, enabled: Boolean) {
    EditPanelContent(
        openTool = tool,
        settings = EditSettings(),
        onChange = {},
        onPreview = {},
        onCommit = {},
        onRatio = {},
        enabled = enabled,
    )
}

/**
 * The Edit screen with Adjust's sheet open and an edit to undo, drawn once in light and once in
 * dark: the photo in its fixed 60%, the Undo and Compare pill at its bottom right, no "Replace"
 * chip, the row of four tools and Adjust's sliders in the sheet under it.
 */
@PreviewLightDark
@Composable
private fun ToolEditSheetPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.EDIT,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                edited = sampleNarrowPhoto(),
                canUndo = true,
                openTool = EditTool.ADJUST,
            ),
            onBack = {},
            onDone = {},
            editSheet = { tool -> SampleEditSheet(tool, enabled = true) },
        )
    }
}

/**
 * The Edit screen while Compare is on, in light and in dark: the card shows the untouched original
 * (the wide sample) although an edit (the narrow sample) has been made, and the row of tools,
 * Undo and the open sheet's sliders are all switched off.
 */
@PreviewLightDark
@Composable
private fun ToolEditComparingPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.EDIT,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                edited = sampleNarrowPhoto(),
                canUndo = true,
                openTool = EditTool.ADJUST,
                comparing = true,
            ),
            onBack = {},
            onDone = {},
            editSheet = { tool -> SampleEditSheet(tool, enabled = false) },
        )
    }
}

/**
 * The top bar of one of the four other tools (Retouch here), in light and in dark, twice: above,
 * Undo and Done both usable; below, both switched off, as while the photo is still opening or an
 * edit runs. Undo always stands left of Done. The bars sit in a full-size box first, because
 * KansoTheme would otherwise stretch the first one over the whole preview.
 */
@PreviewLightDark
@Composable
private fun ToolTopBarPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Column {
                ToolTopBar(
                    tool = Tool.RETOUCH,
                    onBack = {},
                    showUndo = true,
                    undoEnabled = true,
                    onUndo = {},
                    doneEnabled = true,
                    finishing = false,
                    onDone = {},
                )
                ToolTopBar(
                    tool = Tool.RETOUCH,
                    onBack = {},
                    showUndo = true,
                    undoEnabled = false,
                    onUndo = {},
                    doneEnabled = false,
                    finishing = false,
                    onDone = {},
                )
            }
        }
    }
}

/**
 * The Edit screen's top bar, in light and in dark, twice: above, Done usable; below, Done
 * switched off, as before the photo is shown. Edit's Undo lives in the pill on the photo, so Done
 * stands alone at the right end. The rendering state (a turning spinner inside Done) is not shown:
 * a preview draws one still picture, and a spinner never stops turning.
 */
@PreviewLightDark
@Composable
private fun ToolTopBarEditPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Column {
                ToolTopBar(
                    tool = Tool.EDIT,
                    onBack = {},
                    showUndo = false,
                    undoEnabled = false,
                    onUndo = {},
                    doneEnabled = true,
                    finishing = false,
                    onDone = {},
                )
                ToolTopBar(
                    tool = Tool.EDIT,
                    onBack = {},
                    showUndo = false,
                    undoEnabled = false,
                    onUndo = {},
                    doneEnabled = false,
                    finishing = false,
                    onDone = {},
                )
            }
        }
    }
}
