package com.phongkaster.kanso.ui.saved

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
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
import androidx.compose.runtime.rememberUpdatedState
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

/** The test tag on the root of the Saved screen (and of the photo viewer, which is the same screen). */
const val SAVED_SCREEN_TAG = "saved-screen"

/** The test tag on the saved photo. */
const val SAVED_PHOTO_TAG = "saved-photo"

/**
 * The test tag on the blurred copy of the photo that fills the whole window behind it (see
 * [BlurredBackdrop]).
 */
const val SAVED_BACKDROP_TAG = "saved-backdrop"

/** The test tag on the soft shade at the top of the screen, behind the close button and the title. */
const val SAVED_SCRIM_TAG = "saved-scrim"

/** The test tag on the round close button at the top left, which goes back to Home. */
const val SAVED_CLOSE_TAG = "saved-close"

/** The test tag on the title beside the close button: "Saved", or "My edits" for the viewer. */
const val SAVED_TITLE_TAG = "saved-title"

/**
 * The test tag on the success mark, the small badge over the top of the photo that grows and
 * changes shape once when the screen appears after a save (see [SavedMark]). The same node carries
 * the mark's content description, so a screen reader can say that the photo was saved. The photo
 * viewer never shows it.
 */
const val SAVED_MARK_TAG = "saved-mark"

/** The test tag on the line saying where the photo was saved, shown only right after a save. */
const val SAVED_MESSAGE_TAG = "saved-message"

/**
 * The test tag on the loading indicator shown in the middle of the screen while the photo is being
 * decoded. It is gone as soon as the photo is shown, or as soon as the photo turns out to be
 * unreadable.
 */
const val SAVED_LOADING_TAG = "saved-loading"

/** The test tag on the round Share button at the bottom left. */
const val SAVED_SHARE_TAG = "saved-share"

/**
 * The test tag on the wide button beside Share: "Edit another" after a save, "Edit" in the photo
 * viewer.
 */
const val SAVED_EDIT_ANOTHER_TAG = "saved-edit-another"

/**
 * The test tag on the icon inside the wide button. The icon has no content description (the label
 * beside it names the action), and a button merges what is inside it into one node of the
 * semantics tree (the tree tests and screen readers read), so a test finds the icon by this tag in
 * the unmerged tree. A test tag is never read aloud.
 */
const val SAVED_EDIT_ANOTHER_ICON_TAG = "saved-edit-another-icon"

/**
 * A semantics property carrying the Uri (as text) of the photo the Saved screen shows, so a test
 * can check which photo is on screen without comparing pixels. People and screen readers never
 * see it.
 */
val SAVED_PHOTO_URI_KEY = SemanticsPropertyKey<String>("SavedPhotoUriKey")

/**
 * How tall the shade at the top is, below the status bar: enough to sit behind the close button,
 * the title and a little more, so the shade ends softly rather than at the button's edge.
 */
private val SCRIM_HEIGHT = 120.dp

/**
 * How far below the status bar the shade keeps its full strength before it starts to fade: the whole
 * top row (8 dp, the 48 dp close button, 8 dp), so the title is drawn over an evenly shaded
 * background from its first pixel row to its last, and does not sit in the fade where a white photo
 * would lower its contrast.
 */
private val SCRIM_HOLD = 64.dp

/**
 * The strength of the shade at its top: 85 % of the surface colour over the photo, enough for the
 * title and the status bar to read on any photo, while a hint of the photo still shows through.
 */
private const val SCRIM_TOP_ALPHA = 0.85f

/** The space around the top row (close button and title) and the bottom row (Share and the wide button). */
private val EDGE_MARGIN = 16.dp

/** The space above and below the top row's 48 dp close button. */
private val TOP_ROW_VERTICAL_PADDING = 8.dp

/** The height of the round close button, and so of the top row without its padding. */
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
 * The Saved screen, in its two uses: right after a photo is saved, and as the photo viewer, a
 * photo opened from the My edits tab. Either way it shows one finished photo, whole and as large
 * as fits, over a soft blurred copy of itself that fills the window; a shade at the top behind a
 * round close button and the title; and at the bottom a round Share button beside one wide
 * button. After a save the title is "Saved", a success mark plays once over the top of the photo,
 * a line says the photo is in Pictures/Kanso, and the wide button is "Edit another". In the viewer
 * the title is "My edits", there is no mark and no line, and the wide button is "Edit".
 *
 * [viewModel] holds what the screen shows: the photo's Uri (the text of the Uri the save returned,
 * or of the tile tapped in My edits), whether this is the viewer, and that photo decoded as the
 * tool screen decodes one, in the background, upright, and no larger than twice the screen's
 * longest edge. This function works out that size and asks the ViewModel for the photo. The
 * navigation entry builds the ViewModel from the screen's key; tests build it with a loader that
 * hands back a fixed picture.
 *
 * Like Home, this screen is split in two (state hoisting; see `HomeScreen`). This function is the
 * stateful part: it reads the ViewModel's state, asks for the photo, runs the share logic and
 * hands the results to the stateless [SavedContent], which does the drawing and is what the
 * previews show.
 *
 * Share opens Android's share sheet for the photo (see [shareChooserIntent]) and then calls
 * [onShare]. On Android 8.1 and 9 the save gives a `file://` Uri, which must never leave the
 * app, so the photo's `content://` Uri is looked up first (see [shareableUri]); that lookup runs
 * in the background, and a second tap on Share while it runs does nothing. If no content Uri can
 * be found, the share sheet does not open and [onShare] is not called. The share stays here rather
 * than in the ViewModel because opening the share sheet needs the Activity's context.
 *
 * [onEditAnother] is the wide button: after a save the navigation entry opens the gallery of the
 * tool the photo was saved from; in the viewer it opens this photo in the Edit tool. [onClose] is
 * the round close button at the top left: the navigation entry goes back to Home. System Back is
 * left to the navigation: it goes Home after a save, and back to the grid from the viewer.
 */
@Composable
fun SavedScreen(
    viewModel: SavedViewModel,
    onShare: () -> Unit = {},
    onEditAnother: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
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
    // keys (the ViewModel or the size) changes. The ViewModel ignores a repeated request for the
    // same size, so the photo is decoded once.
    LaunchedEffect(viewModel, maxEdgePx) {
        viewModel.load(maxEdgePx)
    }

    // collectAsState turns the ViewModel's StateFlow into Compose state: when the photo is
    // decoded, the screen is drawn again with it.
    val state by viewModel.state.collectAsState()
    val photoUri = state.photoUri

    // Share: look up a shareable Uri off the main thread, then open the share sheet.
    val scope = rememberCoroutineScope()
    val chooserTitle = stringResource(R.string.saved_share_chooser)
    val currentOnShare by rememberUpdatedState(onShare)
    var sharing by remember { mutableStateOf(false) }
    val share: () -> Unit = {
        if (!sharing) {
            sharing = true
            scope.launch {
                try {
                    val uri = shareableUri(context, photoUri)
                    if (uri != null) {
                        val opened = try {
                            context.startActivity(shareChooserIntent(uri, chooserTitle, shareMimeType(context, uri)))
                            true
                        } catch (_: ActivityNotFoundException) {
                            // Every phone has the share sheet; this only guards against a broken one.
                            false
                        }
                        if (opened) currentOnShare()
                    }
                } finally {
                    sharing = false
                }
            }
        }
    }

    SavedContent(
        state = state,
        sharing = sharing,
        onShare = share,
        onEditAnother = onEditAnother,
        onClose = onClose,
        modifier = modifier,
    )
}

/**
 * The stateless part of the Saved screen (see [SavedScreen] for why there are two): draws [state]
 * in layers, back to front, and reports every tap through a callback.
 *
 * 1. The window's background colour, which shows only while the photo is still being decoded (with
 *    the loading indicator, [SavedLoadingIndicator], in the middle) or if it cannot be read.
 * 2. Once decoded: a [BlurredBackdrop] of the photo filling the whole window, and on it the photo
 *    itself ([PhotoView]), whole, centred and as large as fits, so it touches two opposite edges of
 *    the window and the soft copy fills the space left at the other two.
 * 3. The shade at the top ([TopShade]) and, over it, the round close button and the title
 *    ([TopRow]).
 * 4. After a save only: the success mark ([SavedMark]) over the top of the photo.
 * 5. The bottom row ([BottomActions]): the "saved" line after a save, then Share and the wide
 *    button, lifted clear of the phone's gesture zone.
 *
 * `BoxWithConstraints` is a Box that also tells its content how much room it has (`maxWidth`,
 * `maxHeight`); the mark uses it to work out where the top of the photo is (see [markCentreY]).
 *
 * [state] holds the photo, its Uri (only carried onto the photo's semantics so tests can tell which
 * photo is shown) and whether this is the viewer. [sharing] is true while a share is being
 * prepared; Share then ignores taps. [onShare], [onEditAnother] and [onClose] are the three
 * buttons. Nothing here loads a picture or opens the share sheet, so a preview can draw it from
 * sample data.
 */
@Composable
internal fun SavedContent(
    state: SavedUiState,
    sharing: Boolean,
    onShare: () -> Unit,
    onEditAnother: () -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(
        modifier = modifier
            .testTag(SAVED_SCREEN_TAG)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        val photo = state.photo
        when (photo) {
            // Decoding takes a moment; meanwhile an indicator moves in the middle of the window.
            SavedPhoto.Loading -> SavedLoadingIndicator()
            // A photo that cannot be read leaves the window plain: it is saved all the same.
            SavedPhoto.Unreadable -> Unit
            is SavedPhoto.Ready -> {
                // Compose draws an ImageBitmap; wrapping the Android Bitmap is cheap, and remember
                // keeps the wrapper between redraws, so the backdrop's tiny copy is made once.
                val image = remember(photo.bitmap) { photo.bitmap.asImageBitmap() }
                BlurredBackdrop(photo = image, modifier = Modifier.testTag(SAVED_BACKDROP_TAG))
                PhotoView(image = image, photoUri = state.photoUri)
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

        // The viewer shows a photo saved some time ago: there is nothing to celebrate.
        if (!state.viewer) {
            val centreY = markCentreY(
                photo = photo,
                boxWidth = maxWidth,
                boxHeight = maxHeight,
                statusBarTop = statusBarTop,
            )
            SavedMark(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // offset moves the mark down from the top edge; its node moves with it.
                    .offset(y = centreY - MARK_SIZE / 2),
            )
        }

        BottomActions(
            viewer = state.viewer,
            sharing = sharing,
            onShare = onShare,
            onEditAnother = onEditAnother,
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
 * And it never rises into the top row: its top stays 8 dp below the close button. While the photo
 * is not decoded its top is unknown, so only the two limits apply.
 */
private fun markCentreY(photo: SavedPhoto, boxWidth: Dp, boxHeight: Dp, statusBarTop: Dp): Dp {
    val lowest = boxHeight * MARK_MAX_CENTRE_FRACTION
    val highest = statusBarTop + TOP_ROW_VERTICAL_PADDING * 2 + TOP_ROW_BUTTON_SIZE + 8.dp + MARK_SIZE / 2
    val overPhoto = if (photo is SavedPhoto.Ready && boxWidth > 0.dp && boxHeight > 0.dp) {
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
 * The soft shade across the top of the screen, behind the close button and the title, so they and
 * the status bar read on any photo, light or dark. It is [SCRIM_HEIGHT] tall plus the status bar
 * ([statusBarTop]), full width; [modifier] places it.
 *
 * What a `Brush` is: whatever Compose paints an area with. The simplest brush is one plain colour;
 * a gradient brush paints a colour that changes smoothly across the area. A "gradient" is that
 * smooth change: here `Brush.verticalGradient` goes from top to bottom through a few "colour
 * stops", each a position (0 is the top edge, 1 the bottom) paired with a colour, and blends
 * between neighbouring stops.
 *
 * The colour is the theme's `surface` role at every stop, only its alpha (its opacity) changes:
 * [SCRIM_TOP_ALPHA] at the top, held over the status bar and [SCRIM_HOLD] below it, then fading
 * to 0 at the bottom. Fading the same colour, rather than fading to `Color.Transparent`, matters:
 * transparent is "black with alpha 0", so a fade towards it would pass through greyish tones
 * halfway. With one role the shade is light in light mode and dark in dark mode, and the title in
 * `onSurface`, the colour made to read on `surface`, always stands out on it.
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
            .testTag(SAVED_SCRIM_TAG)
            .background(brush),
    )
}

/**
 * The top row, over the shade and below the status bar: the round close button at the left (calls
 * [onClose]; the navigation goes Home) and the title beside it, "Saved", or "My edits" when
 * [viewer]. The title is marked as a heading, so a screen reader can jump to it. [modifier] places
 * the row.
 */
@Composable
private fun TopRow(viewer: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(if (viewer) R.string.saved_layout_viewer_title else R.string.saved_title)
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Below the status bar, whose height the system reports as a window inset.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = EDGE_MARGIN, vertical = TOP_ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoundIconButton(
            onClick = onClose,
            iconRes = R.drawable.ic_close,
            contentDescription = stringResource(R.string.saved_layout_close),
            modifier = Modifier.testTag(SAVED_CLOSE_TAG),
        )
        Text(
            text = title,
            // titleLargeEmphasized is one of the "Emphasized" text styles Material 3 Expressive
            // adds: the size of titleLarge, but bolder, so the title stands out over the photo.
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .testTag(SAVED_TITLE_TAG)
                .semantics { heading() },
        )
    }
}

/**
 * The bottom of the screen: after a save (when not [viewer]) the line saying the photo is in
 * Pictures/Kanso, then one row with the round Share button at the left (calls [onShare], ignored
 * while [sharing]) and the wide button filling the rest (calls [onEditAnother]): "Edit another"
 * after a save, "Edit" in the viewer.
 *
 * The row spans the window's width less [EDGE_MARGIN] at each side, so the wide button is always
 * many times wider than Share. Both are centred on the same line: Share is a 48 dp circle, the
 * wide button Material 3 Expressive's 56 dp medium button.
 *
 * Window insets are the strips along the screen's edges that the system draws on or listens on.
 * The one that matters here is at the bottom: the gesture zone, where a swipe up means "go home".
 * It is not always the navigation bar's height (with swipe gestures on and the hint hidden, the
 * navigation bar is 0 dp tall while the gesture zone is still there), so the row pads itself by
 * whichever of the two is taller (`union`), then keeps a small margin above that, as the tool
 * screen's Save button does. [modifier] places the whole block.
 */
@Composable
private fun BottomActions(
    viewer: Boolean,
    sharing: Boolean,
    onShare: () -> Unit,
    onEditAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = ButtonDefaults.MediumContainerHeight
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
        if (!viewer) SavedMessage()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoundIconButton(
                // The stateful screen already ignores a second tap while a share is prepared; the
                // check here keeps the stateless part honest on its own.
                onClick = { if (!sharing) onShare() },
                iconRes = R.drawable.ic_share,
                contentDescription = stringResource(R.string.saved_share),
                modifier = Modifier.testTag(SAVED_SHARE_TAG),
            )
            Button(
                onClick = onEditAnother,
                // A set of shapes rather than one: the button squares its corners a little while
                // pressed, animated with the theme's motion scheme.
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = true),
                modifier = Modifier
                    // weight(1f) gives the button all the width Share leaves.
                    .weight(1f)
                    .heightIn(min = height)
                    .testTag(SAVED_EDIT_ANOTHER_TAG),
            ) {
                // No description of its own: the label next to it already says it.
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier
                        .size(ButtonDefaults.iconSizeFor(height))
                        .testTag(SAVED_EDIT_ANOTHER_ICON_TAG),
                )
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(height)))
                Text(
                    text = stringResource(if (viewer) R.string.saved_layout_viewer_edit else R.string.saved_edit_another),
                    style = ButtonDefaults.textStyleFor(height),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The line "Your photo is saved in Pictures/Kanso." above the bottom row, shown only right after a
 * save. It sits on a small rounded card in the theme's `surfaceContainerHigh` colour, with text in
 * `onSurface`, so it reads over any photo in light and in dark.
 */
@Composable
private fun SavedMessage() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.testTag(SAVED_MESSAGE_TAG),
    ) {
        Text(
            text = stringResource(R.string.saved_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/**
 * The success mark over the top of the photo, with its animation: it plays once when the screen
 * appears and then rests. The drawing itself is [SavedMarkBadge]; this function only moves the two
 * numbers that the drawing reads.
 *
 * The mark is told in two steps, one after the other:
 *
 * 1. It grows from nothing to its full size as a filled circle with a check in the middle. Its
 *    growth goes from 0 to 1 on the motion scheme's fast spatial spring (in KansoTheme's
 *    expressive scheme: stiffness 800, damping ratio 0.6). A damping ratio below 1 lets a spring
 *    overshoot, so the circle grows almost 10 % too far and springs back: a small, physical "pop".
 * 2. The circle then turns into a cookie with nine scallops, the outline of a seal or a badge. This
 *    shape change goes from 0 to 1 on the slow spatial spring (stiffness 200, damping ratio 0.8),
 *    a calmer movement, so the eye can follow one shape flowing into the other.
 *
 * `Animatable` is the Compose animation API for a value that a coroutine drives: it holds a number
 * (here it starts at 0), and `animateTo` moves it to a target frame by frame, along the spring it
 * is given, suspending until it gets there. `LaunchedEffect(Unit)` starts that coroutine once,
 * when the mark first appears, so the two `animateTo` calls inside it run one after the other.
 * Reading `.value` of an `Animatable` here subscribes this function to it, so every frame draws
 * the mark again with the new numbers.
 *
 * Why it rests: a spring has no fixed duration, but Compose ends it once it is within 0.01 of its
 * target and has almost stopped, and then sets the value to exactly the target. That happens after
 * about 0.3 s for the growth and about 0.45 s for the shape change, so well under a second after
 * the screen appears the mark is still, and nothing in it moves again: unlike the loading
 * indicator, it is never an endless animation. With Android's "Remove animations" setting on,
 * Compose skips the springs and both numbers jump straight to 1.
 *
 * Two cases start the mark already at rest. In Android Studio's preview pane
 * (`LocalInspectionMode` is true there) only one still picture is drawn, which would otherwise be
 * the empty first frame. And once the mark has played, `rememberSaveable` (`remember` that also
 * survives the screen being rebuilt, as on a rotation of the phone) keeps that fact, so turning
 * the phone does not play it a second time. [modifier] is applied to the mark.
 */
@Composable
private fun SavedMark(modifier: Modifier = Modifier) {
    var played by rememberSaveable { mutableStateOf(false) }
    val startAtRest = played || LocalInspectionMode.current
    // remember keeps the same two Animatables across the frames of the animation; each starts at
    // 0 (nothing drawn, still a circle) or, in the two cases above, at 1 (the mark at rest).
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

    SavedMarkBadge(
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
 * What `MaterialShapes` is: Material 3 Expressive's ready-made set of shapes (circles, pills,
 * cookies, clovers, bursts and more). Each one is a `RoundedPolygon`, a polygon with rounded
 * corners, described inside a 1 x 1 square.
 *
 * What `Morph` is: a smooth in-between of two such shapes. `Morph(start, end)` matches the outline
 * of the first shape to the outline of the second, once; `morph.toPath(progress)` then gives the
 * outline at any point between them: progress 0 is exactly the first shape, 1 exactly the second,
 * 0.5 halfway. Moving the progress from 0 to 1 over a few frames makes one shape flow into the
 * other. Matching two outlines takes some work, so the morph is built once and kept with
 * `remember` instead of being built again on every frame.
 *
 * The outline comes back inside that 1 x 1 square, so a scale matrix stretches it to the mark's
 * size before it is drawn, and [growth] then draws it smaller or larger around the mark's centre.
 * The progress handed to the morph is kept between 0 and 1 (`coerceIn`): a spring overshoots its
 * target, and a morph pushed past its last shape would bend the cookie beyond its design. The size
 * is allowed to overshoot, because a mark that grows a little too far and springs back reads well.
 *
 * The shape is painted in `drawBehind`, which draws under the node's content (the check) in the
 * node's own space. The node itself keeps its full size and place whatever the growth, so a test
 * finds it, and crops it, the same way on every frame. The check is a Material Symbols drawable
 * drawn with `Icon` and tinted `onPrimary`; it grows with the shape through `graphicsLayer`, which
 * scales what is drawn without changing the layout.
 *
 * The node carries the content description "Photo saved" and the role of an image, so a screen
 * reader announces the mark; its test tag is [SAVED_MARK_TAG].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SavedMarkBadge(growth: Float, morphProgress: Float, modifier: Modifier = Modifier) {
    // From a plain circle to the nine-scalloped cookie of the Expressive shape set.
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    // A path and a matrix reused on every frame, so drawing creates no new objects.
    val path = remember { Path() }
    val scaleMatrix = remember { Matrix() }
    val badgeColour = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.saved_mark_description)

    Box(
        modifier = modifier
            // The tag comes before the size, so the tagged node covers the whole mark.
            .testTag(SAVED_MARK_TAG)
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
 * The loading indicator in the middle of the window while the photo is being decoded, so the
 * person sees that the photo is on its way instead of an empty screen.
 *
 * `LoadingIndicator` is Material 3 Expressive's way of saying "please wait": a small rounded shape
 * that keeps morphing into the next one while it slowly turns (the tool screen's photo card shows
 * the same one). Called without a progress value, it does not know how long the wait will last,
 * so it animates for as long as it is shown; it goes away by itself as soon as the photo is ready
 * or turns out to be unreadable, because the screen then draws that instead. It is still marked
 * experimental in Material3 1.5.0-beta01, hence the opt-in. It keeps its standard 48 dp box and
 * its default colour, the theme's primary.
 *
 * It shows no text; its content description, set on its own modifier, is what a screen reader
 * says. Because it never stops by itself, no preview shows it, and a test that shows it pauses the
 * test clock first.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SavedLoadingIndicator() {
    val description = stringResource(R.string.saved_loading)
    LoadingIndicator(
        modifier = Modifier
            .testTag(SAVED_LOADING_TAG)
            // Set on the indicator's own modifier, the description becomes its spoken name.
            .semantics { contentDescription = description },
    )
}

/**
 * Draws [image] whole, as large as fits in the window, keeping its shape, and marks the image node
 * with [photoUri] so tests can tell which photo is shown.
 */
@Composable
private fun PhotoView(image: ImageBitmap, photoUri: String) {
    Image(
        bitmap = image,
        contentDescription = stringResource(R.string.saved_photo_description),
        // Fit: never cropped, never stretched.
        contentScale = ContentScale.Fit,
        // aspectRatio sizes the image node itself to the photo's shape, the largest that fits the
        // window, so the node's bounds are exactly where the photo is drawn: it touches the window
        // in one direction, and the blurred backdrop shows in the space left in the other.
        modifier = Modifier
            .aspectRatio(image.width.toFloat() / image.height.toFloat())
            .testTag(SAVED_PHOTO_TAG)
            .semantics { this[SAVED_PHOTO_URI_KEY] = photoUri },
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

/** The sample photo the Saved preview shows, wrapped the way a decoded photo is in the screen's state. */
private fun samplePhoto(): SavedPhoto = SavedPhoto.Ready(sampleBitmap())

/**
 * The Saved screen right after a save, with the photo shown and every button ready, drawn once in
 * light and once in dark, from a [SavedUiState] built out of sample data. [SavedContent] draws the
 * backdrop and the photo ([PhotoView]), the shade ([TopShade]) and the top row ([TopRow]), the
 * success mark ([SavedMark], at rest in the preview pane, see there), and the bottom
 * ([BottomActions] with [SavedMessage]), so this one picture covers them all. No preview shows the
 * screen while the photo is still being decoded: its indicator never stops moving, so a still
 * picture of it would mean nothing. `ThemeMode.System` makes KansoTheme follow the preview's own
 * light or dark picture; the taps do nothing.
 */
@PreviewLightDark
@Composable
private fun SavedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SavedContent(
            state = SavedUiState(
                photoUri = "content://media/external/images/media/42",
                photo = samplePhoto(),
            ),
            sharing = false,
            onShare = {},
            onEditAnother = {},
            onClose = {},
        )
    }
}

/**
 * The photo viewer, a photo opened from My edits, drawn once in light and once in dark: a wide
 * sample photo over its blurred copy, the title "My edits", no success mark and no "saved" line,
 * and the wide button reading "Edit". The taps do nothing.
 */
@PreviewLightDark
@Composable
private fun PhotoViewerPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SavedContent(
            state = SavedUiState(
                photoUri = "content://media/external/images/media/43",
                photo = SavedPhoto.Ready(sampleWideBitmap()),
                viewer = true,
            ),
            sharing = false,
            onShare = {},
            onEditAnother = {},
            onClose = {},
        )
    }
}

/**
 * The success mark at rest, as it stays once its animation has played: the cookie with nine
 * scallops in the theme's primary colour with the check on it, drawn once in light and once in
 * dark on the screen's background colour. [SavedMarkBadge] is given fixed numbers (full size, the
 * shape change complete), so nothing moves here.
 */
@PreviewLightDark
@Composable
private fun SavedMarkPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SavedMarkBadge(
                growth = 1f,
                morphProgress = 1f,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
