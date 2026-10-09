package com.phongkaster.kanso.ui.tool

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.photo.displayMaxEdgePx
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.common.BackArrowIcon
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag on the root of the tool screen. */
const val TOOL_SCREEN_TAG = "tool-screen"

/** The test tag on the area below the top bar where the photo is shown. */
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

/**
 * A semantics property carrying the Uri (as text) of the photo the image node shows.
 *
 * Semantics are the facts about each element that accessibility services and tests read, such
 * as its text or its role. This extra, Kanso-only fact lets a test check which photo is on screen
 * without comparing pixels; it is invisible to people and to screen readers.
 */
val TOOL_PHOTO_URI_KEY = SemanticsPropertyKey<String>("PhotoUriKey")

/**
 * The screen where a tool works on one photo: a top bar with a back arrow, the tool's name and a
 * Save action, and below it the photo, upright and fitted to the space.
 *
 * Passing data in a Navigation 3 key: the screen needs two facts, which tool and which photo.
 * They are not handed over in memory from the gallery; they travel inside the navigation key,
 * `ToolPhoto(tool, photoUri)`, the value pushed onto the back stack to open this screen. The
 * back stack is saved (every key is @Serializable), so when the phone rotates, or when Android
 * kills the app in the background and later restores it, the same key comes back and this screen
 * reopens on the same tool and photo with nothing else to restore. The navigation entry passes
 * the key's values to the [viewModel] it builds, which puts them in the screen's state as `tool`
 * and `photoUri`.
 * Only small values belong in a key: the photo goes in as the text of its Uri, never as pixels.
 *
 * The photo is decoded to about twice the screen's longest edge (see [displayMaxEdgePx]), in the
 * background, and never at full size. If it cannot be opened, a plain message says so.
 *
 * Save sits in the top bar, clear of the gesture area at the bottom of the screen. It can be
 * tapped once the photo is shown, and writes a new upright JPEG into the phone's `Pictures/Kanso`
 * album (see `MediaStorePhotoSaver`); while it runs, Save shows a small spinner and "Saving…".
 * On Android 8.1 and 9 writing there needs the WRITE_EXTERNAL_STORAGE permission, so the first
 * tap asks for it; after a refusal a plain message says nothing was saved. Android 10 and later
 * ask for nothing. A failed save, or a photo too large to turn upright, is also explained by a
 * message under the photo.
 *
 * Like Home, this screen is split in two (state hoisting; see `HomeScreen` for the full story).
 * This function is the *stateful* part: it gathers the screen size, the load, the saved events and
 * the write-permission launcher, and hands the state and callbacks to the stateless [ToolContent],
 * which does all the drawing.
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

    // collectAsState turns the StateFlow into Compose state: when a new snapshot arrives, the
    // screen is drawn again with it.
    val state by viewModel.state.collectAsState()

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
        modifier = modifier,
    )
}

/**
 * The stateless part of the tool screen (see [ToolScreen] for why there are two): draws [state],
 * the screen's one snapshot. At the top, the bar with the back arrow, the name of the tool and
 * the Save action; below it whatever the photo part of [state] says (nothing while loading, the
 * photo when ready, a plain message when unreadable), plus the message about the last save when
 * the save part has one.
 *
 * The photo's Uri in [state] only marks the image node for tests. Taps are reported through
 * callbacks and nothing is saved here: [onBack] for the back arrow and [onSave] for Save (the
 * caller decides whether the storage permission must be asked for first).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolContent(
    state: ToolUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(TOOL_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(state.tool.labelRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = BackArrowIcon,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    val saving = state.save == ToolUiState.SaveState.Saving
                    // Always visible, so the bar does not jump; tappable once the photo is shown
                    // and no save is running.
                    TextButton(
                        onClick = onSave,
                        enabled = state.photo is ToolUiState.PhotoState.Ready && !saving,
                        modifier = Modifier.testTag(TOOL_SAVE_TAG),
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
                            Text(stringResource(R.string.tool_save))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar (and system bars) take; the photo avoids it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(TOOL_BODY_TAG),
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
}

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
 * The tool screen with a photo shown and nothing said about saving: the everyday look, drawn once
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
                save = ToolUiState.SaveState.Idle,
            ),
            onBack = {},
            onSave = {},
        )
    }
}

/**
 * The tool screen when the photo cannot be opened: the plain message in the middle and Save
 * switched off.
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
        )
    }
}

/**
 * The tool screen after a failed save: the photo with the explaining message on its card at the
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
        )
    }
}
