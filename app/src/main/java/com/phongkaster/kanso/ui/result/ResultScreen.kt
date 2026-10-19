package com.phongkaster.kanso.ui.result

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import java.io.File
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.photo.displayMaxEdgePx
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.common.BlurredBackdrop
import com.phongkaster.kanso.ui.common.RoundIconButton
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.launch

/** The test tag on the root of the Result screen (and of the photo viewer, which is the same screen). */
const val RESULT_SCREEN_TAG = "result-screen"

/** The test tag on the picture the screen shows. */
const val RESULT_PHOTO_TAG = "result-photo"

/**
 * The test tag on the blurred copy of the photo that fills the whole window behind it (see
 * [BlurredBackdrop]).
 */
const val RESULT_BACKDROP_TAG = "result-backdrop"

/** The test tag on the soft shade at the top of the screen, behind the top row. */
const val RESULT_SCRIM_TAG = "result-scrim"

/** The test tag on the photo viewer's round close button at the top left, which goes back to Home. */
const val RESULT_CLOSE_TAG = "result-close"

/** The test tag on the title at the top: "Your edit", or "My edits" for the viewer. */
const val RESULT_TITLE_TAG = "result-title"

/**
 * The test tag on the success mark, the small badge over the top of the photo that grows and
 * changes shape once when Save has succeeded (see [ResultMark]). The same node carries the mark's
 * content description, so a screen reader can say that the photo was saved. The photo viewer never
 * shows it.
 */
const val RESULT_MARK_TAG = "result-mark"

/**
 * The test tag on the loading indicator shown in the middle of the viewer while its photo is being
 * decoded. It is gone as soon as the photo is shown, or as soon as it turns out to be unreadable.
 */
const val RESULT_LOADING_TAG = "result-loading"

/** The test tag on the round Share button at the bottom left, in both uses of the screen. */
const val RESULT_SHARE_TAG = "result-share"

/** The test tag on the wide Save button, shown right after Done only. */
const val RESULT_SAVE_TAG = "result-save"

/**
 * The test tag on the small loading indicator inside the Save button while the picture is being
 * written. It animates without end, so a test that shows it pauses the test clock first.
 */
const val RESULT_SAVING_TAG = "result-saving"

/** The test tag on the round Go home button at the bottom right, shown right after Done only. */
const val RESULT_HOME_TAG = "result-home"

/** The test tag on the viewer's wide Edit button, which opens the photo in the Edit tool. */
const val RESULT_EDIT_TAG = "result-edit"

/** The test tag on the "Nothing to show" text, shown when no rendered picture was handed over. */
const val RESULT_EMPTY_TAG = "result-empty"

/**
 * The test tag on the line above the bottom row that says how Save went: where the photo was
 * saved, that it kept its rotation tag, that saving failed, or that the permission was refused.
 */
const val RESULT_MESSAGE_TAG = "result-message"

/**
 * A semantics property carrying the Uri (as text) of the photo the viewer shows, so a test can
 * check which photo is on screen without comparing pixels. The rendered picture right after Done
 * has no Uri, so it does not carry it. People and screen readers never see it.
 */
val RESULT_PHOTO_URI_KEY = SemanticsPropertyKey<String>("SavedPhotoUriKey")

/**
 * How tall the shade at the top is, below the status bar: enough to sit behind the top row and a
 * little more, so the shade ends softly rather than at the button's edge.
 */
private val SCRIM_HEIGHT = 120.dp

/**
 * How far below the status bar the shade keeps its full strength before it starts to fade: the whole
 * top row (8 dp, the 48 dp row, 8 dp), so the title is drawn over an evenly shaded background from
 * its first pixel row to its last, and does not sit in the fade where a white photo would lower its
 * contrast.
 */
private val SCRIM_HOLD = 64.dp

/**
 * The strength of the shade at its top: 85 % of the surface colour over the photo, enough for the
 * title and the status bar to read on any photo, while a hint of the photo still shows through.
 */
private const val SCRIM_TOP_ALPHA = 0.85f

/** The space around the top row and the bottom row. */
private val EDGE_MARGIN = 16.dp

/** The space above and below the top row. */
private val TOP_ROW_VERTICAL_PADDING = 8.dp

/** The height of the round close button, and so the least height of the top row without its padding. */
private val TOP_ROW_BUTTON_SIZE = 48.dp

/**
 * The width and height of the success mark: large enough to read at a glance, small enough not to
 * hide much of the photo it sits on.
 */
private val MARK_SIZE = 64.dp

/**
 * The size of the check drawn in the middle of the success mark: half the mark, so it stays well
 * inside the cookie's scallops.
 */
private val MARK_CHECK_SIZE = 32.dp

/** How far below the photo's top edge the success mark sits, when the photo starts high enough. */
private val MARK_PHOTO_INSET = 16.dp

/**
 * The lowest the success mark's centre may be, as a share of the window's height: 0.2 keeps it in
 * the top quarter of the window, over the top of the photo, whatever the photo's shape.
 */
private const val MARK_MAX_CENTRE_FRACTION = 0.2f

/**
 * The Result screen, in its two uses.
 *
 * Right after Done on a tool screen it shows the picture Done rendered (read from the hand-off
 * store by [ResultViewModel]), titled "Your edit", with three actions at the bottom: Share, Save
 * and Go home. Nothing is in the gallery until Save is tapped. Share sends a temporary copy from
 * the app's cache, Save writes the picture into `Pictures/Kanso` once (the button then reads
 * "Saved" and a success mark plays over the photo), and Go home ([onClose]) ends the session. If
 * no picture was handed over (the app was restarted), the screen says "Nothing to show"; Go home
 * and system Back still leave it.
 *
 * As the photo viewer, a photo opened from the My edits tab, it shows that photo (decoded from the
 * phone, upright and no larger than twice the screen's longest edge), titled "My edits", with the
 * round close button at the top ([onClose], Go home), Share and a wide Edit button ([onEdit]).
 *
 * Like Home, this screen is split in two (state hoisting; see `HomeScreen`). This function is the
 * stateful part: it reads the ViewModel's state, asks for the viewer's photo, owns the storage
 * permission request and runs Share, and hands the results to the stateless [ResultContent], which
 * does the drawing and is what the previews show.
 *
 * Save on Android 8.1 and 9 needs the WRITE_EXTERNAL_STORAGE permission. When the saver says it is
 * missing, the system asks the person first: granted, the picture is saved; refused, nothing is
 * written and the screen says why.
 *
 * Share stays here rather than in the ViewModel because opening the share sheet needs the
 * Activity's context; a second tap while a share is being prepared does nothing. Right after Done
 * the picture is written to a file in the cache and shared through FileProvider (see
 * [shareableBitmapUri]); in the viewer the photo's MediaStore Uri is shared (see [shareableUri]).
 * [shareFileUri] names the cached file as a Uri; only a test on a desktop JVM replaces it.
 */
@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onClose: () -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier,
    shareFileUri: (Context, File) -> Uri = ::fileProviderUri,
) {
    val context = LocalContext.current

    // The screen size in pixels, worked out as on the tool screen.
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val maxEdgePx = with(density) {
        displayMaxEdgePx(
            screenWidthPx = configuration.screenWidthDp.dp.roundToPx(),
            screenHeightPx = configuration.screenHeightDp.dp.roundToPx(),
        )
    }

    // LaunchedEffect runs its block once when the screen appears, and again only if one of its
    // keys changes. Only the viewer decodes anything; the ViewModel ignores a repeated request.
    LaunchedEffect(viewModel, maxEdgePx) {
        viewModel.load(maxEdgePx)
    }

    // collectAsState turns the ViewModel's StateFlow into Compose state: every new snapshot
    // draws the screen again.
    val state by viewModel.state.collectAsState()

    // The storage-write permission, needed only on Android 8.1 and 9. The launcher is registered
    // on every composition, not only when asking, so that an answer arriving after the activity
    // was re-created still comes back here. Granted: save. Refused: say so and write nothing.
    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.save() else viewModel.onWritePermissionDenied()
    }
    val save: () -> Unit = {
        if (viewModel.needsWritePermission()) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.save()
        }
    }

    // Share: find a Uri another app may read, off the main thread, then open the share sheet.
    val scope = rememberCoroutineScope()
    val chooserTitle = stringResource(R.string.result_share_chooser)
    var sharing by remember { mutableStateOf(false) }
    val share: () -> Unit = {
        if (!sharing) {
            sharing = true
            // Read the state now, so the picture shared is the one on screen at the tap.
            val current = viewModel.state.value
            scope.launch {
                try {
                    val target = shareTarget(context, current, shareFileUri)
                    if (target != null) {
                        try {
                            context.startActivity(shareChooserIntent(target.first, chooserTitle, target.second))
                        } catch (_: ActivityNotFoundException) {
                            // Every phone has the share sheet; this only guards against a broken one.
                        }
                    }
                } finally {
                    sharing = false
                }
            }
        }
    }

    ResultContent(
        state = state,
        onShare = share,
        onSave = save,
        onClose = onClose,
        onEdit = onEdit,
        sharing = sharing,
        modifier = modifier,
    )
}

/**
 * Returns the Uri to share for [state] and its MIME type, or null when there is nothing to share.
 * In the viewer it is the photo's MediaStore content Uri (see [shareableUri]); right after Done it
 * is a FileProvider Uri of a copy of the rendered picture in the cache (see [shareableBitmapUri]),
 * so sharing never writes to the gallery.
 */
private suspend fun shareTarget(
    context: Context,
    state: ResultUiState,
    shareFileUri: (Context, File) -> Uri,
): Pair<Uri, String>? {
    if (state.viewer) {
        val photoUri = state.photoUri ?: return null
        val uri = shareableUri(context, photoUri) ?: return null
        return uri to shareMimeType(context, uri)
    }
    val bitmap = (state.photo as? ResultPhoto.Ready)?.bitmap ?: return null
    val uri = shareableBitmapUri(context, bitmap, shareFileUri) ?: return null
    return uri to bitmapShareMimeType(bitmap)
}

/**
 * The stateless part of the Result screen (see [ResultScreen] for why there are two): draws
 * [state] in layers, back to front, and reports every tap through a callback.
 *
 * 1. The window's background colour, which shows while the viewer's photo is still being decoded
 *    (with [ResultLoadingIndicator] in the middle), if it cannot be read, or with "Nothing to show"
 *    ([NothingToShow]) when no picture was handed over.
 * 2. Once there is a picture: a [BlurredBackdrop] of it filling the whole window, and on it the
 *    picture itself ([PhotoView]), whole, centred and as large as fits.
 * 3. The shade at the top ([TopShade]) and, over it, the top row ([TopRow]).
 * 4. Once Save has succeeded: the success mark ([ResultMark]) over the top of the photo.
 * 5. The bottom ([BottomActions]): how Save went, then the row of actions, lifted clear of the
 *    phone's gesture zone.
 *
 * `BoxWithConstraints` is a Box that also tells its content how much room it has (`maxWidth`,
 * `maxHeight`); the mark uses it to work out where the top of the photo is (see [markCentreY]).
 *
 * [onShare], [onSave], [onClose] (Go home) and [onEdit] are the buttons; [sharing] is true while a
 * share is being prepared, and Share then ignores taps. Nothing here loads a picture, saves or opens
 * the share sheet, so a preview can draw it from sample data.
 */
@Composable
internal fun ResultContent(
    state: ResultUiState,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    sharing: Boolean = false,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(
        modifier = modifier
            .testTag(RESULT_SCREEN_TAG)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        val photo = state.photo
        when (photo) {
            // Decoding takes a moment; meanwhile an indicator moves in the middle of the window.
            ResultPhoto.Loading -> ResultLoadingIndicator()
            // A viewer photo that cannot be read leaves the window plain.
            ResultPhoto.Unreadable -> Unit
            ResultPhoto.Empty -> NothingToShow()
            is ResultPhoto.Ready -> {
                // Compose draws an ImageBitmap; wrapping the Android Bitmap is cheap, and remember
                // keeps the wrapper between redraws, so the backdrop's tiny copy is made once.
                val image = remember(photo.bitmap) { photo.bitmap.asImageBitmap() }
                BlurredBackdrop(photo = image, modifier = Modifier.testTag(RESULT_BACKDROP_TAG))
                PhotoView(image = image, photoUri = state.photoUri, viewer = state.viewer)
            }
        }

        TopShade(
            statusBarTop = statusBarTop,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        TopRow(
            viewer = state.viewer,
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopStart),
        )

        // The mark celebrates a save that just happened; the viewer never shows it.
        if (!state.viewer && state.save is ResultUiState.SaveState.Saved) {
            val centreY = markCentreY(
                photo = photo,
                boxWidth = maxWidth,
                boxHeight = maxHeight,
                statusBarTop = statusBarTop,
            )
            ResultMark(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // offset moves the mark down from the top edge; its node moves with it.
                    .offset(y = centreY - MARK_SIZE / 2),
            )
        }

        BottomActions(
            state = state,
            sharing = sharing,
            onShare = onShare,
            onSave = onSave,
            onClose = onClose,
            onEdit = onEdit,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Returns how far from the window's top the success mark's centre goes, in a window [boxWidth] by
 * [boxHeight] with a status bar [statusBarTop] tall, for [photo].
 *
 * The mark sits over the top of the photo: [MARK_PHOTO_INSET] below the photo's top edge. A photo
 * shown whole is centred, so a wide one starts low; the mark then stays at most
 * [MARK_MAX_CENTRE_FRACTION] of the window down, in the top quarter, where the eye looks first.
 * And it never rises into the top row: its top stays 8 dp below it.
 */
private fun markCentreY(photo: ResultPhoto, boxWidth: Dp, boxHeight: Dp, statusBarTop: Dp): Dp {
    val lowest = boxHeight * MARK_MAX_CENTRE_FRACTION
    val highest = statusBarTop + TOP_ROW_VERTICAL_PADDING * 2 + TOP_ROW_BUTTON_SIZE + 8.dp + MARK_SIZE / 2
    val overPhoto = if (photo is ResultPhoto.Ready && boxWidth > 0.dp && boxHeight > 0.dp) {
        val photoRatio = photo.bitmap.width.toFloat() / photo.bitmap.height.toFloat()
        val boxRatio = boxWidth / boxHeight
        // A photo wider (in shape) than the window fills its width; a taller one fills its height.
        val photoHeight = if (photoRatio >= boxRatio) boxWidth / photoRatio else boxHeight
        val photoTop = (boxHeight - photoHeight) / 2
        photoTop + MARK_PHOTO_INSET + MARK_SIZE / 2
    } else {
        lowest
    }
    return minOf(overPhoto, lowest).coerceAtLeast(highest)
}

/**
 * The soft shade across the top of the screen, behind the top row, so the title and the status bar
 * read on any photo, light or dark. It is [SCRIM_HEIGHT] tall plus the status bar ([statusBarTop]),
 * full width; [modifier] places it.
 *
 * What a `Brush` is: whatever Compose paints an area with. A gradient brush paints a colour that
 * changes smoothly across the area: `Brush.verticalGradient` goes from top to bottom through a few
 * "colour stops", each a position (0 the top edge, 1 the bottom) paired with a colour.
 *
 * The colour is the theme's `surface` role at every stop, only its alpha changes: [SCRIM_TOP_ALPHA]
 * at the top, held over the status bar and [SCRIM_HOLD] below it, then fading to 0. Fading the same
 * colour, rather than to `Color.Transparent` ("black with alpha 0"), avoids greyish tones halfway,
 * and the title in `onSurface` always stands out on it.
 */
@Composable
private fun TopShade(statusBarTop: Dp, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    val height = statusBarTop + SCRIM_HEIGHT
    // Where, from 0 to 1, the shade stops holding its full strength and starts fading.
    val holdEnd = (statusBarTop + SCRIM_HOLD) / height
    val brush = Brush.verticalGradient(
        0f to surface.copy(alpha = SCRIM_TOP_ALPHA),
        holdEnd to surface.copy(alpha = SCRIM_TOP_ALPHA),
        1f to surface.copy(alpha = 0f),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .testTag(RESULT_SCRIM_TAG)
            .background(brush),
    )
}

/**
 * The top row, over the shade and below the status bar. In the viewer ([viewer] true): the round
 * close button at the left (calls [onClose]; the navigation goes Home) and the title "My edits".
 * Right after Done: the title "Your edit" alone, since Go home sits with the other actions at the
 * bottom and system Back returns to the tool. The row is at least as tall as the close button
 * either way, so the title sits in the same place. The title is marked as a heading, so a screen
 * reader can jump to it. [modifier] places the row.
 */
@Composable
private fun TopRow(viewer: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(if (viewer) R.string.result_layout_viewer_title else R.string.result_title)
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Below the status bar, whose height the system reports as a window inset.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = EDGE_MARGIN, vertical = TOP_ROW_VERTICAL_PADDING)
            .heightIn(min = TOP_ROW_BUTTON_SIZE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (viewer) {
            RoundIconButton(
                onClick = onClose,
                iconRes = R.drawable.ic_close,
                contentDescription = stringResource(R.string.result_layout_close),
                modifier = Modifier.testTag(RESULT_CLOSE_TAG),
            )
        }
        Text(
            text = title,
            // titleLargeEmphasized is one of the "Emphasized" text styles Material 3 Expressive
            // adds: the size of titleLarge, but bolder, so the title stands out over the photo.
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .testTag(RESULT_TITLE_TAG)
                .semantics { heading() },
        )
    }
}

/**
 * The bottom of the screen for [state]: first, right after Done, the line saying how Save went
 * ([ResultMessage], when there is something to say), then one row of actions.
 *
 * - Viewer: the round Share button (calls [onShare], ignored while [sharing]) and the wide Edit
 *   button filling the rest (calls [onEdit]).
 * - After Done, with a picture: round Share, the wide Save button ([SaveButton], calls [onSave])
 *   and the round Go home button (calls [onClose]).
 * - After Done, with nothing to show: Go home alone, centred.
 *
 * The line appears and changes with `AnimatedContent`, which fades the old line out and the new
 * one in when its target (here the message's string id, or null for no line) changes.
 *
 * Window insets are the strips along the screen's edges that the system draws on or listens on.
 * The one that matters here is at the bottom: the gesture zone, where a swipe up means "go home".
 * With swipe gestures on and the hint hidden, the navigation bar is 0 dp tall while the gesture
 * zone is still there, so the block pads itself by whichever of the two is taller (`union`), then
 * keeps a small margin above that. [modifier] places the whole block.
 */
@Composable
private fun BottomActions(
    state: ResultUiState,
    sharing: Boolean,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasPicture = state.photo is ResultPhoto.Ready
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
            )
            .padding(horizontal = EDGE_MARGIN, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.viewer) {
            AnimatedContent(targetState = resultMessageRes(state.save), label = "result-message") { messageRes ->
                if (messageRes != null) ResultMessage(messageRes)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (state.viewer || hasPicture) {
                Arrangement.spacedBy(12.dp)
            } else {
                Arrangement.Center
            },
        ) {
            when {
                state.viewer -> {
                    ShareButton(sharing = sharing, onShare = onShare)
                    EditButton(onEdit = onEdit, modifier = Modifier.weight(1f))
                }
                hasPicture -> {
                    ShareButton(sharing = sharing, onShare = onShare)
                    // weight(1f) gives Save all the width the two round buttons leave.
                    SaveButton(save = state.save, onSave = onSave, modifier = Modifier.weight(1f))
                    HomeButton(onClose = onClose)
                }
                else -> HomeButton(onClose = onClose)
            }
        }
    }
}

/**
 * Returns the string that says how Save went for [save], or null when there is nothing to say:
 * where the picture went (or, for a very large photo, that it kept its rotation tag), that it
 * failed, or that the storage permission was refused.
 */
@StringRes
private fun resultMessageRes(save: ResultUiState.SaveState): Int? = when (save) {
    is ResultUiState.SaveState.Saved ->
        if (save.keptOriginalOrientation) R.string.save_kept_orientation else R.string.result_finished_message
    ResultUiState.SaveState.Failed -> R.string.save_failed
    ResultUiState.SaveState.PermissionDenied -> R.string.save_permission_denied
    ResultUiState.SaveState.Idle, ResultUiState.SaveState.Saving -> null
}

/**
 * The round Share button (48 dp, the least touch target). The stateful screen already ignores a
 * second tap while a share is being prepared ([sharing]); the check here keeps the stateless part
 * honest on its own. Calls [onShare].
 */
@Composable
private fun ShareButton(sharing: Boolean, onShare: () -> Unit) {
    RoundIconButton(
        onClick = { if (!sharing) onShare() },
        iconRes = R.drawable.ic_share,
        contentDescription = stringResource(R.string.result_share),
        modifier = Modifier.testTag(RESULT_SHARE_TAG),
    )
}

/** The round Go home button (48 dp), shown right after Done; calls [onClose], which ends the session. */
@Composable
private fun HomeButton(onClose: () -> Unit) {
    RoundIconButton(
        onClick = onClose,
        iconRes = R.drawable.ic_home,
        contentDescription = stringResource(R.string.result_home),
        modifier = Modifier.testTag(RESULT_HOME_TAG),
    )
}

/**
 * The wide Save button, Material 3 Expressive's 56 dp medium button, for [save]:
 *
 * - Idle, failed or refused: "Save" with its icon; a tap calls [onSave].
 * - Saving: switched off, with a small loading indicator ([SavingIndicator]) and "Saving…".
 * - Saved: switched off, with a check and "Saved", so the picture cannot be written twice.
 *
 * A switched-off button normally fades to a faint grey that would barely show over a photo, so its
 * switched-off colours are set to the theme's secondary container pair, which keeps the label
 * readable. [modifier] sizes it in the row.
 */
@Composable
private fun SaveButton(save: ResultUiState.SaveState, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val height = ButtonDefaults.MediumContainerHeight
    val saving = save == ResultUiState.SaveState.Saving
    val saved = save is ResultUiState.SaveState.Saved
    Button(
        onClick = onSave,
        // A set of shapes rather than one: the button squares its corners a little while pressed,
        // animated with the theme's motion scheme.
        shapes = ButtonDefaults.shapesFor(height),
        enabled = !saving && !saved,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = true),
        modifier = modifier
            .heightIn(min = height)
            .testTag(RESULT_SAVE_TAG),
    ) {
        if (saving) {
            SavingIndicator()
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(height)))
            Text(stringResource(R.string.tool_saving), style = ButtonDefaults.textStyleFor(height), maxLines = 1)
        } else {
            // No description of its own: the word next to it already says it.
            Icon(
                painter = painterResource(if (saved) R.drawable.ic_check else R.drawable.ic_save),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(height)))
            Text(
                text = stringResource(if (saved) R.string.result_finished_label else R.string.tool_save),
                style = ButtonDefaults.textStyleFor(height),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The small loading indicator inside the Save button while the picture is being written, drawn
 * just before "Saving…". `LoadingIndicator` is Material 3 Expressive's "please wait": a rounded
 * shape that keeps morphing while it turns, for as long as it is shown, so no preview shows it.
 * It is 24 dp here to fit beside the text, in the button's content colour, and has no description
 * of its own: "Saving…" already says it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SavingIndicator() {
    LoadingIndicator(
        modifier = Modifier
            .size(24.dp)
            .testTag(RESULT_SAVING_TAG),
        color = LocalContentColor.current,
    )
}

/**
 * The viewer's wide Edit button, Material 3 Expressive's 56 dp medium button with a pencil icon;
 * calls [onEdit], which opens this photo in the Edit tool. [modifier] sizes it in the row.
 */
@Composable
private fun EditButton(onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val height = ButtonDefaults.MediumContainerHeight
    Button(
        onClick = onEdit,
        shapes = ButtonDefaults.shapesFor(height),
        contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = true),
        modifier = modifier
            .heightIn(min = height)
            .testTag(RESULT_EDIT_TAG),
    ) {
        // No description of its own: the label next to it already says it.
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(height)))
        Text(
            text = stringResource(R.string.result_layout_viewer_edit),
            style = ButtonDefaults.textStyleFor(height),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The line above the bottom row saying how Save went, the string [messageRes]. It sits on a small
 * rounded card in the theme's `surfaceContainerHigh` colour, with text in `onSurface`, so it reads
 * over any photo in light and in dark.
 */
@Composable
private fun ResultMessage(@StringRes messageRes: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.testTag(RESULT_MESSAGE_TAG),
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/**
 * "Nothing to show", in the middle of the window, when no rendered picture was handed over (for
 * example after Android closed the app in the background). The text is in `onBackground`, the
 * colour made to read on the window's background.
 */
@Composable
private fun NothingToShow() {
    Text(
        text = stringResource(R.string.result_nothing),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(horizontal = EDGE_MARGIN)
            .testTag(RESULT_EMPTY_TAG),
    )
}

/**
 * The success mark over the top of the photo, with its animation: it plays once when Save has
 * succeeded and then rests. The drawing itself is [ResultMarkBadge]; this function only moves the
 * two numbers that the drawing reads.
 *
 * The mark is told in two steps, one after the other:
 *
 * 1. It grows from nothing to its full size as a filled circle with a check in the middle, on the
 *    motion scheme's fast spatial spring (in KansoTheme's expressive scheme: stiffness 800, damping
 *    ratio 0.6). A damping ratio below 1 lets a spring overshoot, so the circle grows almost 10 %
 *    too far and springs back: a small, physical "pop".
 * 2. The circle then turns into a cookie with nine scallops, the outline of a seal, on the slow
 *    spatial spring (stiffness 200, damping ratio 0.8), so the eye can follow the change.
 *
 * `Animatable` is the Compose animation API for a value that a coroutine drives: it holds a number,
 * and `animateTo` moves it to a target frame by frame along the spring it is given, suspending until
 * it gets there. `LaunchedEffect(Unit)` starts that coroutine once, when the mark first appears.
 * Reading `.value` of an `Animatable` here subscribes this function to it, so every frame draws the
 * mark again with the new numbers.
 *
 * Why it rests: Compose ends a spring once it is within 0.01 of its target and has almost stopped,
 * well under a second after it starts, so it is never an endless animation. In Android Studio's
 * preview pane (`LocalInspectionMode`) and once it has played (`rememberSaveable` keeps that across
 * a rotation) the mark starts at rest. [modifier] is applied to the mark.
 */
@Composable
private fun ResultMark(modifier: Modifier = Modifier) {
    var played by rememberSaveable { mutableStateOf(false) }
    val startAtRest = played || LocalInspectionMode.current
    // remember keeps the same two Animatables across the frames of the animation.
    val growth = remember { Animatable(if (startAtRest) 1f else 0f) }
    val shapeChange = remember { Animatable(if (startAtRest) 1f else 0f) }
    // The springs are read from the theme here, in composition, and handed to the coroutine below.
    val growthSpring = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val shapeSpring = MaterialTheme.motionScheme.slowSpatialSpec<Float>()

    LaunchedEffect(Unit) {
        // Each call suspends until its spring has settled, so the shape starts changing only once
        // the mark has finished growing. A value already at 1 returns at once.
        growth.animateTo(1f, animationSpec = growthSpring)
        shapeChange.animateTo(1f, animationSpec = shapeSpring)
        played = true
    }

    ResultMarkBadge(
        growth = growth.value,
        morphProgress = shapeChange.value,
        modifier = modifier,
    )
}

/**
 * Draws the success mark at one moment of its animation: a filled shape in the theme's primary
 * colour with a check in the middle, in `onPrimary`, the colour that reads on primary.
 *
 * [growth] is the mark's size as a share of [MARK_SIZE]: 0 draws nothing, 1 is the full size, and
 * a little above 1 while the spring overshoots. [morphProgress] is how far the shape has changed
 * from a circle into a cookie: 0 is the circle, 1 the cookie. [modifier] is applied to the mark.
 *
 * What `MaterialShapes` is: Material 3 Expressive's ready-made set of shapes (circles, cookies,
 * clovers and more), each a `RoundedPolygon` described inside a 1 x 1 square. What `Morph` is: a
 * smooth in-between of two such shapes; `morph.toPath(progress)` gives the outline at any point
 * between them (0 the first shape, 1 the second). Matching two outlines takes some work, so the
 * morph is built once and kept with `remember`.
 *
 * The outline is stretched from the 1 x 1 square to the mark's size, then drawn grown or shrunk
 * around the centre by [growth]. The morph's progress is kept between 0 and 1, because a spring
 * overshoots and a morph pushed past its last shape would bend the cookie beyond its design. The
 * node keeps its full size whatever the growth, so a test finds it the same way on every frame.
 *
 * The node carries the content description "Photo saved" and the role of an image, so a screen
 * reader announces the mark; its test tag is [RESULT_MARK_TAG].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResultMarkBadge(growth: Float, morphProgress: Float, modifier: Modifier = Modifier) {
    // From a plain circle to the nine-scalloped cookie of the Expressive shape set.
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    // A path and a matrix reused on every frame, so drawing creates no new objects.
    val path = remember { Path() }
    val scaleMatrix = remember { Matrix() }
    val badgeColour = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.result_mark_description)

    Box(
        modifier = modifier
            // The tag comes before the size, so the tagged node covers the whole mark.
            .testTag(RESULT_MARK_TAG)
            .semantics {
                contentDescription = description
                role = Role.Image
            }
            .size(MARK_SIZE)
            .drawBehind {
                if (growth > 0f) {
                    // The outline at this moment of the morph, inside the 1 x 1 square.
                    morph.toPath(progress = morphProgress.coerceIn(0f, 1f), path = path)
                    // Stretch it from the 1 x 1 square to the node's size, in pixels.
                    scaleMatrix.reset()
                    scaleMatrix.scale(x = size.width, y = size.height)
                    path.transform(scaleMatrix)
                    // Draw it grown or shrunk around the centre of the node.
                    scale(growth) {
                        drawPath(path = path, color = badgeColour)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            // The mark's node already carries the description; the check adds nothing to say.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(MARK_CHECK_SIZE)
                .graphicsLayer {
                    scaleX = growth
                    scaleY = growth
                },
        )
    }
}

/**
 * The loading indicator in the middle of the window while the viewer's photo is being decoded, so
 * the person sees that the photo is on its way instead of an empty screen.
 *
 * `LoadingIndicator` is Material 3 Expressive's way of saying "please wait": a small rounded shape
 * that keeps morphing while it slowly turns, for as long as it is shown; it goes away as soon as
 * the photo is ready or turns out to be unreadable. It is still marked experimental in Material3
 * 1.5.0-beta01, hence the opt-in. Its content description, set on its own modifier, is what a
 * screen reader says. Because it never stops by itself, no preview shows it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResultLoadingIndicator() {
    val description = stringResource(R.string.result_loading)
    LoadingIndicator(
        modifier = Modifier
            .testTag(RESULT_LOADING_TAG)
            // Set on the indicator's own modifier, the description becomes its spoken name.
            .semantics { contentDescription = description },
    )
}

/**
 * Draws [image] whole, as large as fits in the window, keeping its shape. In the viewer the image
 * node is marked with [photoUri] so tests can tell which photo is shown; the picture rendered by
 * Done has no Uri ([photoUri] null). The spoken description says whether it is the saved photo
 * ([viewer]) or the edit not yet saved.
 */
@Composable
private fun PhotoView(image: ImageBitmap, photoUri: String?, viewer: Boolean) {
    val description = stringResource(
        if (viewer) R.string.result_viewer_photo_description else R.string.result_photo_description,
    )
    Image(
        bitmap = image,
        contentDescription = description,
        // Fit: never cropped, never stretched.
        contentScale = ContentScale.Fit,
        // aspectRatio sizes the image node itself to the photo's shape, the largest that fits the
        // window, so the node's bounds are exactly where the photo is drawn.
        modifier = Modifier
            .aspectRatio(image.width.toFloat() / image.height.toFloat())
            .testTag(RESULT_PHOTO_TAG)
            .then(
                if (photoUri != null) Modifier.semantics { this[RESULT_PHOTO_URI_KEY] = photoUri } else Modifier,
            ),
    )
}

/**
 * A made-up photo for the previews: a warm sky-to-sand gradient with a sun, 600 by 800 pixels
 * (portrait), drawn with Android's Canvas so no file or camera is needed.
 */
private fun sampleBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, 800f,
        0xFF5B8DEF.toInt(), 0xFFF2D6A2.toInt(),
        android.graphics.Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, 600f, 800f, paint)
    paint.shader = null
    paint.color = 0xFFFFE08A.toInt()
    canvas.drawCircle(420f, 260f, 90f, paint)
    return bitmap
}

/**
 * A second made-up photo for the viewer's preview: a 4:3 landscape picture, a warm
 * orange-to-violet sunset gradient with a pale moon, so the blurred backdrop shows above and below it.
 */
private fun sampleWideBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 800f, 600f,
        0xFFFB8C00.toInt(), 0xFF5E35B1.toInt(),
        android.graphics.Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, 800f, 600f, paint)
    paint.shader = null
    paint.color = 0xFFFFF8E1.toInt()
    canvas.drawCircle(560f, 200f, 70f, paint)
    return bitmap
}

/** The sample picture the Result previews show, wrapped the way a rendered picture is in the state. */
private fun samplePhoto(): ResultPhoto = ResultPhoto.Ready(sampleBitmap())

/**
 * The Result screen right after Done, before saving, drawn once in light and once in dark from a
 * [ResultUiState] built out of sample data: the picture over its blurred copy, the title "Your
 * edit", and Share, Save and Go home at the bottom. `ThemeMode.System` makes KansoTheme follow the
 * preview's own light or dark picture; the taps do nothing. No preview shows Saving: its indicator
 * never stops moving.
 */
@PreviewLightDark
@Composable
private fun ResultPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ResultContent(
            state = ResultUiState(photo = samplePhoto()),
            onShare = {},
            onSave = {},
            onClose = {},
            onEdit = {},
        )
    }
}

/**
 * The Result screen once Save has succeeded, drawn in light and dark: the success mark over the
 * top of the photo, the line saying it is in Pictures/Kanso, and the Save button switched off and
 * reading "Saved".
 */
@PreviewLightDark
@Composable
private fun ResultSavedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ResultContent(
            state = ResultUiState(
                photo = samplePhoto(),
                save = ResultUiState.SaveState.Saved(keptOriginalOrientation = false),
            ),
            onShare = {},
            onSave = {},
            onClose = {},
            onEdit = {},
        )
    }
}

/**
 * The Result screen when no picture was handed over, drawn in light and dark: "Nothing to show"
 * in the middle and Go home alone at the bottom.
 */
@PreviewLightDark
@Composable
private fun ResultNothingPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ResultContent(
            state = ResultUiState(photo = ResultPhoto.Empty),
            onShare = {},
            onSave = {},
            onClose = {},
            onEdit = {},
        )
    }
}

/**
 * The photo viewer, a photo opened from My edits, drawn once in light and once in dark: a wide
 * sample photo over its blurred copy, the close button and the title "My edits", no success mark
 * and no line, and Share beside the wide "Edit" button. The taps do nothing.
 */
@PreviewLightDark
@Composable
private fun PhotoViewerPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ResultContent(
            state = ResultUiState(
                photoUri = "content://media/external/images/media/43",
                photo = ResultPhoto.Ready(sampleWideBitmap()),
                viewer = true,
            ),
            onShare = {},
            onSave = {},
            onClose = {},
            onEdit = {},
        )
    }
}

/**
 * The success mark at rest, as it stays once its animation has played: the cookie with nine
 * scallops in the theme's primary colour with the check on it, drawn once in light and once in
 * dark on the screen's background colour. [ResultMarkBadge] is given fixed numbers, so nothing
 * moves here.
 */
@PreviewLightDark
@Composable
private fun ResultMarkPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ResultMarkBadge(
                growth = 1f,
                morphProgress = 1f,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
