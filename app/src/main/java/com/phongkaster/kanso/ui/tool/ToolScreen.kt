package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.artistic.ArtisticProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.beautify.BeautifyProcessor
import com.phongkaster.kanso.data.photo.displayMaxEdgePx
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.DiscardChangesDialog
import com.phongkaster.kanso.ui.tool.component.EditCropper
import com.phongkaster.kanso.ui.tool.component.EditPhotoArea
import com.phongkaster.kanso.ui.tool.component.editbottombar.EditBottomBar
import com.phongkaster.kanso.ui.tool.component.photocard.PhotoCard
import com.phongkaster.kanso.ui.tool.component.StickerLimitMessage
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.LayerSize
import com.phongkaster.kanso.ui.tool.component.toolpanel.ToolPanel
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EditPanel
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.EditPanelContent
import com.phongkaster.kanso.ui.tool.component.topbar.ToolTopBar
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.datepanel.DatePanel
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TextPanel
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
import com.phongkaster.kanso.ui.tool.model.BeautifyAmounts
import com.phongkaster.kanso.ui.tool.model.BeautifyGroup
import java.time.LocalDateTime
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection

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

/** Test tag of the node around the failure card when no face was found (the Beautify tool). */
const val BEAUTIFY_NO_FACE_TAG = "beautify-no-face"

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
 * The Edit screen differs (see [ToolContent]): a row of four tools (Crop, Rotate, Adjust,
 * Filters) sits in the screen's bottom bar, and a tap on one opens its controls in a panel just
 * above the row, the photo shrinking to make room, as in Adobe Lightroom. The photo has no
 * "Replace" chip, and Undo sits with a Compare switch at the end of the row of tools
 * instead of in the top bar, so the top bar's right end holds Done alone.
 *
 * While an edit runs, a progress ring fills in the middle of the card and Done and Undo are
 * switched off, so a half-made edit is never handed over by mistake; the screen stays responsive
 * because the edit itself runs off the main thread (see `ToolViewModel.apply`). When an edit
 * fails, a plain message under the top of the card says why, and the photo shown is unchanged.
 *
 * Done can be tapped once the photo is shown. It writes nothing: it hands the finished picture to
 * the Result screen (see `ToolViewModel.done`), which saves or shares it if asked. On the other
 * four tools that is the picture shown now; on Edit the current settings are first rendered from
 * the decoded photo (the screen-sized decode described below, shrunk further to at most 2000
 * pixels on its longest edge), and meanwhile Done shows a small spinner and ignores taps.
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

    // THE recipe that turns settings into the processor drawing them (filters AND sticker
    // pictures), made once. The cropper, the sheets' changes, the sticker taps and so Done's final
    // render all use this one function, so a sticker never disappears in one of them.
    val context = LocalContext.current
    val processorFor: (EditSettings) -> ToolProcessor = remember(context) { editProcessorFor(context) }

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
                // A Beautify slider let go: the full-quality render, quietly in the background.
                onBeautify = { amounts ->
                    viewModel.renderRegistered(configure = { processor -> (processor as? BeautifyProcessor)?.use(amounts) })
                },
                // While a Beautify slider is dragged: a quick live frame with the same values.
                onBeautifyPreview = { amounts ->
                    viewModel.previewRegistered(configure = { processor -> (processor as? BeautifyProcessor)?.use(amounts) })
                },
                // The whole photo while Body is open: the zoom onto the face would hide the waist.
                // Opening Body also starts the pose model, so the first body drag is already live.
                onBeautifyGroup = { group ->
                    viewModel.showWholePhoto(group == BeautifyGroup.BODY)
                    if (group == BeautifyGroup.BODY) viewModel.prepareBodies()
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
        // tool's sheet, and the row's Compare switch shows the original photo while it is on.
        onOpenTool = viewModel::openTool,
        onCompare = viewModel::setComparing,
        // The Edit screen only: while Crop is open (and the photo is decoded) the photo card is the
        // real cropper on the original photo; a finished gesture is one crop action of the
        // settings session (one Undo step). Until the photo is Ready there is no cropper, so the
        // loading indicator or the unreadable message stays on screen.
        editCropper = (state.photo as? ToolUiState.PhotoState.Ready)?.let { ready ->
            {
                EditCropper(
                    bitmap = ready.bitmap,
                    settings = state.settings,
                    enabled = !state.comparing && !state.finishing,
                    onCropFinished = { rect -> viewModel.commitCrop(rect, processorFor) },
                    // The session's layers stay visible on the photo being cropped.
                    layers = state.settings.layers,
                )
            }
        },
        // The Edit screen only: the open tool's controls, drawn in the sheet under the photo.
        // They are switched off while Compare shows the original and while Done is rendering the
        // final picture (`!state.comparing && !state.finishing`). An ordinary preview render does
        // not disable them. Every change goes to the ViewModel's one settings session, which
        // re-renders from the original photo.
        editSheet = { tool ->
            // Whatever recipe a panel offers, the screen's own (with sticker pictures) is the one
            // the ViewModel keeps, so Done always draws the layers.
            if (tool == EditTool.TEXT) {
                val selectedText = state.settings.layers.firstOrNull {
                    it.key == state.selectedLayerKey && it.content is LayerContent.Text
                }
                // The size number of a text needs its words measured; the ViewModel does it with
                // the same fonts it sizes texts with.
                val size = remember(state.settings, state.selectedLayerKey) { viewModel.selectedLayerSize() }
                TextPanel(
                    selected = selectedText,
                    size = size ?: LayerSize.TEXT_DEFAULT,
                    editing = state.textEditing,
                    enabled = !state.comparing && !state.finishing,
                    onWords = { viewModel.typeText(it, processorFor) },
                    onEditingChange = viewModel::setTextEditing,
                    onAlignment = { viewModel.cycleTextAlignment(processorFor) },
                    onFont = { viewModel.chooseTextFont(it, processorFor) },
                    onColour = { viewModel.chooseTextColour(it, processorFor) },
                    onCustomColour = { viewModel.chooseTextColour(it, processorFor, preview = true) },
                    onCustomColourDone = viewModel::commitSettings,
                    onBackground = { viewModel.chooseTextBackground(it, processorFor) },
                    onCustomBackground = { viewModel.chooseTextBackground(it, processorFor, preview = true) },
                    onCustomBackgroundDone = viewModel::commitSettings,
                    onSize = { viewModel.previewLayerSize(it, processorFor) },
                    onOpacity = { viewModel.previewLayerOpacity(it, processorFor) },
                    onSliderDone = viewModel::commitSettings,
                    onCancel = viewModel::cancelLayers,
                    onConfirm = viewModel::confirmLayers,
                )
            } else if (tool == EditTool.DATE) {
                val selectedDate = state.settings.layers.firstOrNull {
                    it.key == state.selectedLayerKey && it.content is LayerContent.Date
                }
                // A date's size number needs its words measured, like a text's.
                val size = remember(state.settings, state.selectedLayerKey) { viewModel.selectedLayerSize() }
                // The ViewModel sets the next moment to now when the sheet opens; this stand-in
                // only covers a frame drawn before that (remember is never called conditionally).
                val standIn = remember { LocalDateTime.now() }
                DatePanel(
                    selected = selectedDate,
                    nextMoment = state.dateMoment ?: standIn,
                    size = size ?: LayerSize.TEXT_DEFAULT,
                    enabled = !state.comparing && !state.finishing,
                    onMoment = { viewModel.chooseDateMoment(it, processorFor) },
                    onFormat = { viewModel.chooseDateFormat(it, processorFor) },
                    onFont = { viewModel.chooseDateFont(it, processorFor) },
                    onColour = { viewModel.chooseDateColour(it, processorFor) },
                    onCustomColour = { viewModel.chooseDateColour(it, processorFor, preview = true) },
                    onCustomColourDone = viewModel::commitSettings,
                    onBackground = { viewModel.chooseDateBackground(it, processorFor) },
                    onCustomBackground = { viewModel.chooseDateBackground(it, processorFor, preview = true) },
                    onCustomBackgroundDone = viewModel::commitSettings,
                    onSize = { viewModel.previewLayerSize(it, processorFor) },
                    onOpacity = { viewModel.previewLayerOpacity(it, processorFor) },
                    onSliderDone = viewModel::commitSettings,
                    onCancel = viewModel::cancelLayers,
                    onConfirm = viewModel::confirmLayers,
                )
            } else if (tool == EditTool.STICKERS) {
                EditPanel(
                    openTool = tool,
                    settings = state.settings,
                    onChange = { changed, _ -> viewModel.changeSettings(changed, processorFor) },
                    onPreview = { changed, _ -> viewModel.previewSettings(changed, processorFor) },
                    onCommit = viewModel::commitSettings,
                    onRatio = { ratio, _ -> viewModel.chooseCropRatio(ratio, processorFor) },
                    enabled = !state.comparing && !state.finishing,
                    stickerCategory = state.stickerCategory,
                    onStickerCategoryChosen = viewModel::chooseStickerCategory,
                    onStickerChosen = { viewModel.addSticker(it, processorFor) },
                    onStickersCancel = viewModel::cancelLayers,
                    onStickersConfirm = viewModel::confirmLayers,
                    selectedLayerKey = state.selectedLayerKey,
                )
            } else {
                ToolPanel(
                    tool = Tool.EDIT,
                    state = state,
                    openTool = tool,
                    enabled = !state.comparing && !state.finishing,
                    onEditChange = { changed, _ -> viewModel.changeSettings(changed, processorFor) },
                    onEditPreview = { changed, _ -> viewModel.previewSettings(changed, processorFor) },
                    onEditCommit = viewModel::commitSettings,
                    onEditRatio = { ratio, _ -> viewModel.chooseCropRatio(ratio, processorFor) },
                    onEditAdjustSlider = viewModel::chooseAdjustSlider,
                )
            }
        },
        onStickerLimitShown = viewModel::onLayerLimitMessageShown,
        onLayerSelect = viewModel::selectLayer,
        onLayerPreview = { list -> viewModel.previewLayers(list, processorFor) },
        onLayerCommit = viewModel::commitSettings,
        onLayerDelete = { viewModel.deleteLayer(it, processorFor) },
        onLayerDuplicate = { viewModel.duplicateLayer(it, processorFor) },
        onLayerFlip = { viewModel.flipLayer(it, processorFor) },
        onLayerEdit = viewModel::editText,
        onLayerTap = { viewModel.tapLayer(it, processorFor) },
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
 * - The options strip: [panel], the tool's own controls, at the bottom. There is no bottom bar
 *   (except on Edit and Beautify, below).
 *
 * **Beautify borrows Edit's frame.** Its [panel] (the row of Face and Smooth, with Edit's
 * fixed-height panel area above it) goes in the `Scaffold`'s `bottomBar` slot instead of the
 * options strip, and the photo card fills all the space above it, as on Edit, so the tools have
 * room and the photo stays as large as it can be. The card keeps its "Replace" chip.
 *
 * `Scaffold` is Material's frame for a screen: it places a top bar and a bottom bar and hands the
 * content the space left between them (`innerPadding`), so nothing is drawn under either bar.
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
 * **The Edit screen is laid out differently.** The `Scaffold`'s `bottomBar` slot holds
 * [EditBottomBar]: the row of four tools (Crop, Rotate, Adjust, Filters) at the very bottom, and
 * the open tool's panel appearing just above it; Undo and Compare are the last two entries of that
 * same row. The body is [EditPhotoArea]: the photo card
 * (without the "Replace" chip) filling all the space between the top bar and the bottom bar. The
 * bar keeps one height whatever sheet is open, and the photo is sized from that resting height, so
 * the photo never changes size, not even while the Text keyboard lifts the bar over it. [editSheet] is the slot for that panel's controls; it is given
 * the open tool, so one lambda serves all four. [panel] is not used on Edit. The Undo control
 * moves from the top bar into the row; [onCompare] is called with the Compare switch's new value,
 * and while it is on the card shows the untouched original photo. [onOpenTool] is a tool in the
 * row tapped. The other four tools never call these three.
 *
 * The `onLayer...` callbacks are the layer overlay's events (select, a gesture's new list, the
 * gesture's end, delete, duplicate, a sticker's flip, a text's edit, a tap on the selected layer),
 * handed on to the photo card;
 * only Edit uses them.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
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
    onStickerLimitShown: () -> Unit = {},
    onLayerSelect: (Long?) -> Unit = {},
    onLayerPreview: (List<Layer>) -> Unit = {},
    onLayerCommit: () -> Unit = {},
    onLayerDelete: (Long) -> Unit = {},
    onLayerDuplicate: (Long) -> Unit = {},
    onLayerFlip: (Long) -> Unit = {},
    onLayerEdit: (Long) -> Unit = {},
    onLayerTap: (Long) -> Unit = {},
) {
    val isEdit = state.tool == Tool.EDIT
    // Beautify borrows Edit's frame: its row of tools and panel area ([panel]) in the bottom bar,
    // and the photo filling everything above, so the tools have room and the photo stays large.
    val panelInBottomBar = state.tool == Tool.BEAUTIFY
    // The Edit bottom bar's height with the keyboard down, in pixels (0 until measured). The photo
    // is sized from this, not from the bar's height now, so when the Text keyboard pushes the bar
    // up, the bar slides over the photo instead of shrinking it: the photo never changes size.
    var restingBarPx by remember { mutableIntStateOf(0) }
    val keyboardUp by rememberUpdatedState(WindowInsets.isImeVisible)
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
                showUndo = true,
                // Undo sits beside Done on every tool, Edit included. It is off while Compare shows the original.
                undoEnabled = state.canUndo && state.processing is ToolUiState.Processing.Idle &&
                    !state.finishing && !state.comparing && !state.rendering,
                onUndo = onUndo,
                doneEnabled = state.photo is ToolUiState.PhotoState.Ready &&
                    state.processing is ToolUiState.Processing.Idle && !state.finishing && !state.rendering,
                // Done's small spinner also shows while a slider tool renders quietly.
                finishing = state.finishing || state.rendering,
                onDone = onDone,
            )
        },
        // Edit and Beautify have a bottom bar: a row of tools, with the open tool's panel appearing
        // just above it. The other tools keep their controls in the body.
        bottomBar = {
            if (panelInBottomBar) {
                // Clear of the navigation bar and of the bottom gesture area (a swipe up there means
                // "go home" even when the navigation bar is 0 dp tall), as Edit's bar is. The padding
                // sits outside the tagged box, so the controls themselves end above that area.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
                        )
                        .padding(bottom = 8.dp)
                        .testTag(EDITOR_PANEL_TAG),
                ) {
                    panel()
                }
            }
            if (isEdit) {
                EditBottomBar(
                    state = state,
                    onOpenTool = onOpenTool,
                    onCompare = onCompare,
                    modifier = Modifier.onSizeChanged { if (!keyboardUp) restingBarPx = it.height },
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
                )
            }
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar, the bottom bar (on Edit) and the system bars
        // take. consumeWindowInsets tells the content below that those insets are already dealt
        // with.
        val insetModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        if (isEdit) {
            // The bottom edge comes from the bar's resting height (see restingBarPx), so the photo
            // keeps its size while the keyboard is up; until the bar is measured, innerPadding's.
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val bottom = if (restingBarPx > 0) with(density) { restingBarPx.toDp() } else innerPadding.calculateBottomPadding()
            val fixed = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = bottom,
            )
            val editModifier = Modifier
                .fillMaxSize()
                .padding(fixed)
                .consumeWindowInsets(fixed)
            // The bottom bar already keeps clear of the navigation bar and the gesture area, so the
            // photo only needs a small gap above it.
            EditPhotoArea(
                state = state,
                photo = {
                    Box(Modifier.fillMaxSize()) {
                        PhotoCard(
                            state = state,
                            onReplace = onReplace,
                            onDismissFailure = onDismissFailure,
                            showReplace = false,
                            onLayerSelect = onLayerSelect,
                            onLayerPreview = onLayerPreview,
                            onLayerCommit = onLayerCommit,
                            onLayerDelete = onLayerDelete,
                            onLayerDuplicate = onLayerDuplicate,
                            onLayerFlip = onLayerFlip,
                            onLayerEdit = onLayerEdit,
                            onLayerTap = onLayerTap,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // The "20 stickers at most" card, a moment on the photo's top edge.
                        StickerLimitMessage(
                            visible = state.layerLimitShown,
                            onShown = onStickerLimitShown,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                },
                cropper = editCropper,
                modifier = editModifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        } else if (panelInBottomBar) {
            // As on Edit, the photo card takes all the space between the top bar and the bottom
            // bar, with the same small gap above the bar.
            PhotoCard(
                state = state,
                onReplace = onReplace,
                onDismissFailure = onDismissFailure,
                modifier = insetModifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        } else {
            // With no bottom bar, the tool's controls are the lowest thing on screen, so they keep
            // clear of the gesture area too: with swipe gestures the navigation bar can be 0 dp
            // tall while a swipe up from the edge still means "go home". Only the part of it not
            // already in innerPadding is added, then a small margin.
            val bodyModifier = insetModifier
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
                )
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
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
 * A tall, narrow photo on the Beautify screen: the photo fills the card's height, and the blurred
 * copy of it fills the space at its left and right.
 */
@PreviewLightDark
@Composable
private fun ToolNarrowPhotoPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.BEAUTIFY,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(sampleNarrowPhoto()),
            ),
            onBack = {},
            onDone = {},
            onReplace = {},
            panel = { ToolPanel(Tool.BEAUTIFY) },
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
 * The Beautify screen after an edit failed because no face was found: the plain message at the
 * top of the card, the photo unchanged under it, and Undo usable because an earlier edit
 * (the narrow sample photo standing in for it) can still be taken back.
 */
@PreviewLightDark
@Composable
private fun ToolEditFailedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ToolContent(
            state = ToolUiState(
                tool = Tool.BEAUTIFY,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                edited = sampleNarrowPhoto(),
                canUndo = true,
                failure = EditFailure.NoFace,
            ),
            onBack = {},
            onDone = {},
            onReplace = {},
            panel = { ToolPanel(Tool.BEAUTIFY) },
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
 * The Edit screen with Adjust's panel open and an edit to undo, drawn once in light and once in
 * dark: the row of four tools at the bottom, Adjust's sliders in the panel just above it, and the
 * photo filling the rest, with no "Replace" chip.
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
 * Hands every Beautify slider's value in [amounts] to this processor, just before a live frame or
 * a full render. Each value goes to the processor's own setting of the same meaning.
 */
private fun BeautifyProcessor.use(amounts: BeautifyAmounts) {
    strength = amounts.smoothing
    width = amounts.faceWidth
    lift = amounts.faceLift
    waist = amounts.waist
    arms = amounts.arms
    chest = amounts.chest
    legs = amounts.legs
}
