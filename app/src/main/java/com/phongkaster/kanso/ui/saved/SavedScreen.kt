package com.phongkaster.kanso.ui.saved

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.photo.displayMaxEdgePx
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.launch

/** The test tag on the root of the Saved screen. */
const val SAVED_SCREEN_TAG = "saved-screen"

/** The test tag on the saved photo. */
const val SAVED_PHOTO_TAG = "saved-photo"

/**
 * The test tag on the success mark, the small badge above the photo that grows and changes shape
 * once when the screen appears (see [SavedMark]). The same node carries the mark's content
 * description, so a screen reader can say that the photo was saved.
 */
const val SAVED_MARK_TAG = "saved-mark"

/**
 * The test tag on the loading indicator shown in the middle of the photo area while the saved
 * photo is being decoded. It is gone as soon as the photo is shown, or as soon as the photo turns
 * out to be unreadable.
 */
const val SAVED_LOADING_TAG = "saved-loading"

/**
 * The test tag on the button group that holds the three actions. The Share, Edit another and Home
 * buttons are its descendants in the semantics tree (the tree tests and screen readers read).
 */
const val SAVED_ACTIONS_TAG = "saved-actions"

/** The test tag on the Share button. */
const val SAVED_SHARE_TAG = "saved-share"

/** The test tag on the Edit another button. */
const val SAVED_EDIT_ANOTHER_TAG = "saved-edit-another"

/** The test tag on the Home button. */
const val SAVED_HOME_TAG = "saved-home"

/**
 * The test tag on the icon inside the Share button. The icon has no content description (the
 * label beside it names the action), so without a tag of its own a test could not find it; the
 * tag is read in the unmerged semantics tree, because a button merges what is inside it into one
 * node (see [ActionLabel]).
 */
const val SAVED_SHARE_ICON_TAG = "saved-share-icon"

/** The test tag on the icon inside the Edit another button (see [SAVED_SHARE_ICON_TAG]). */
const val SAVED_EDIT_ANOTHER_ICON_TAG = "saved-edit-another-icon"

/** The test tag on the icon inside the Home button (see [SAVED_SHARE_ICON_TAG]). */
const val SAVED_HOME_ICON_TAG = "saved-home-icon"

/**
 * A semantics property carrying the Uri (as text) of the photo the Saved screen shows, so a test
 * can check which photo is on screen without comparing pixels. People and screen readers never
 * see it.
 */
val SAVED_PHOTO_URI_KEY = SemanticsPropertyKey<String>("SavedPhotoUriKey")

/**
 * The smallest height of an action button: Material 3's medium button height, 56 dp, which clears
 * Android's smallest touch target (48 dp). It is also exactly what a label of two lines needs
 * (2 x 20 dp of text plus 2 x 8 dp of padding, see [ACTION_PADDING]), so a button whose label
 * wraps is no taller than its neighbours at the normal font size.
 */
private val ACTION_MIN_HEIGHT = ButtonDefaults.MediumContainerHeight

/**
 * The space inside an action button, around its icon and label: 12 dp on each side and 8 dp above
 * and below. The Material default is wider (16 dp on each side) and taller (10 dp); three buttons
 * share one row of a phone, so every dp given to the padding is taken from the label.
 */
private val ACTION_PADDING = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/**
 * How far a pressed action button may squeeze its neighbours, and so how far it grows. Unless told
 * otherwise the group allows as much as a Material button's end padding, 24 dp, which would squeeze
 * a neighbour's label onto an extra line while a finger is down; a few dp still shows the press
 * without moving any text.
 */
private val PRESS_COMPRESSION = 4.dp

/**
 * The width and height of the success mark: large enough to read at a glance, small enough to
 * leave nearly all of the space between the top bar and the actions to the photo, even on a small
 * phone.
 */
private val MARK_SIZE = 64.dp

/**
 * The size of the check drawn in the middle of the success mark: half the mark, so it stays well
 * inside the cookie's scallops.
 */
private val MARK_CHECK_SIZE = 32.dp

/**
 * The screen shown after a photo is saved: a top bar titled "Saved", a small success mark that
 * grows and changes shape once to say the save worked (see [SavedMark]), the saved photo upright,
 * the message that it is in Pictures/Kanso, and three actions at the bottom: Share, Edit another
 * and Home.
 *
 * [viewModel] holds what the screen shows: the saved photo's Uri (the text of the Uri the save
 * returned) and that photo, decoded as the tool screen decodes one, in the background, upright,
 * and no larger than twice the screen's longest edge. This function works out that size and asks
 * the ViewModel for the photo. The navigation entry builds the ViewModel from the screen's key;
 * tests build it with a loader that hands back a fixed picture.
 *
 * Like Home, this screen is split in two (state hoisting; see `HomeScreen`). This function is the
 * stateful part: it reads the ViewModel's state, asks for the photo, runs the share logic and
 * hands the results to the stateless [SavedContent], which does the drawing and is what the
 * preview shows.
 *
 * Share opens Android's share sheet for the photo (see [shareChooserIntent]) and then calls
 * [onShare]. On Android 8.1 and 9 the save gives a `file://` Uri, which must never leave the
 * app, so the photo's `content://` Uri is looked up first (see [shareableUri]); that lookup runs
 * in the background, and Share cannot be tapped again while it runs. If no content Uri can be
 * found, the share sheet does not open and [onShare] is not called. The share stays here rather
 * than in the ViewModel because opening the share sheet needs the Activity's context.
 *
 * [onEditAnother] goes back to the gallery of the tool the photo was saved from (the navigation
 * entry reads that tool from the screen's key); [onHome] goes back to Home. There is no back
 * arrow: system Back already goes Home.
 */
@Composable
fun SavedScreen(
    viewModel: SavedViewModel,
    onShare: () -> Unit,
    onEditAnother: () -> Unit,
    onHome: () -> Unit,
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
                            context.startActivity(shareChooserIntent(uri, chooserTitle))
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
        onHome = onHome,
        modifier = modifier,
    )
}

/**
 * The stateless part of the Saved screen (see [SavedScreen] for why there are two): draws the top
 * bar, the success mark, the photo area and the bottom actions from [state], and reports every
 * tap through a callback.
 *
 * Between the top bar and the actions the screen is one column: the success mark ([SavedMark])
 * at the top, centred, and under it the photo area, which takes all the height the mark leaves.
 * The photo area shows the loading indicator ([SavedLoadingIndicator]) while the photo is being
 * decoded, then the photo as large as fits; a photo that cannot be read leaves it empty (the
 * photo is saved all the same, and the message at the bottom says so).
 *
 * [state] holds the photo, shown once it is decoded, and its Uri, which is only carried onto the
 * photo's semantics so tests can tell which photo is shown. [sharing] is true while a share is
 * being prepared, which turns the Share button off. Nothing here loads a picture or opens the
 * share sheet, so a preview can draw it from sample data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SavedContent(
    state: SavedUiState,
    sharing: Boolean,
    onShare: () -> Unit,
    onEditAnother: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(SAVED_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.saved_title),
                        // headlineSmallEmphasized is one of the "Emphasized" text styles that Material 3
                        // Expressive adds: the same size family as headlineSmall, but bolder, so the
                        // title stands out from the body text.
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                    )
                },
            )
        },
        bottomBar = {
            SavedActions(
                shareEnabled = !sharing,
                onShare = onShare,
                onEditAnother = onEditAnother,
                onHome = onHome,
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar and the actions take; the mark and the photo
        // stay between.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SavedMark()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // weight(1f) gives the photo area all the height the mark leaves, so the photo
                    // stays as large as fits.
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when (val current = state.photo) {
                    // Decoding takes a moment; meanwhile an indicator moves in the middle of the area.
                    SavedPhoto.Loading -> SavedLoadingIndicator()
                    // A photo that cannot be read leaves the area empty: it is saved all the same,
                    // and the message below says so.
                    SavedPhoto.Unreadable -> Unit
                    is SavedPhoto.Ready -> PhotoView(bitmap = current.bitmap, photoUri = state.photoUri)
                }
            }
        }
    }
}

/**
 * The success mark above the photo, with its animation: it plays once when the screen appears and
 * then rests. The drawing itself is [SavedMarkBadge]; this function only moves the two numbers
 * that the drawing reads.
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
 * (Material's own loading indicator does the same: it keeps its morph progress in range and lets
 * the size carry the bounce.)
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
 * The loading indicator in the middle of the photo area while the saved photo is being decoded,
 * so the person sees that the photo is on its way instead of an empty space.
 *
 * `LoadingIndicator` is Material 3 Expressive's way of saying "please wait": a small rounded shape
 * that keeps morphing into the next one while it slowly turns (the editor's photo area shows the
 * same one, see `ToolScreen`). Called without a progress value, it does not know how long the wait
 * will last, so it animates for as long as it is shown; it goes away by itself as soon as the photo
 * is ready or turns out to be unreadable, because the photo area then draws that instead. It is
 * still marked experimental in Material3 1.5.0-beta01, hence the opt-in. It keeps its standard
 * 48 dp box and its default colour, the theme's primary.
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
 * Draws [bitmap] as large as fits, keeping its shape, and marks the image node with [photoUri]
 * so tests can tell which photo is shown.
 */
@Composable
private fun PhotoView(bitmap: Bitmap, photoUri: String) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = stringResource(R.string.saved_photo_description),
        // Fit: never cropped, never stretched.
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .testTag(SAVED_PHOTO_TAG)
            .semantics { this[SAVED_PHOTO_URI_KEY] = photoUri },
    )
}

/**
 * The bottom of the screen: the "saved" message, and under it the three actions, Share, Edit
 * another and Home, side by side in one row.
 *
 * The row is a [ButtonGroup], a layout that Material 3 Expressive adds for a few related buttons.
 * Next to a plain Row it does two extra things: the button under a finger grows a little and
 * squeezes its neighbours (see [PRESS_COMPRESSION]), which makes a press feel physical; and
 * buttons that do not fit the row move into a menu behind a "more" button instead of being cut
 * off. Three details are worth knowing, because each one is a trap the first time:
 *
 * - The braces after `ButtonGroup(...)` are not composable code. They only list the items, and
 *   the group draws them later. So the three labels are read with `stringResource` here, above the
 *   group, where a composable call is allowed.
 * - Every item has a weight (see [ActionButton]). An item without one is as wide as its label,
 *   and when the labels together are wider than the row (a big font on a small phone) the last
 *   buttons slide into the overflow menu. With a weight on every item, the three buttons always
 *   split the whole row between them, so all three are always on screen, and a label that is too
 *   long wraps onto a second line instead of making a button disappear.
 * - `clickableItem`, the short way to add an item, builds its own button, so there is no place to
 *   attach a test tag. `customItem` takes the button as a composable, so each tag goes on a button
 *   built in this file, and the group carries its own tag, [SAVED_ACTIONS_TAG]. The buttons are
 *   then children of the group in the semantics tree (the tree of nodes that tests and screen
 *   readers read instead of looking at pixels).
 *
 * `verticalAlignment = Alignment.CenterVertically` keeps the buttons centred on one line even if
 * one label wraps and its button gets taller than the others. The bar sits on the theme's surface
 * colour with the theme's text and button colours, so it reads well in light and dark.
 *
 * The bar is at the very bottom of the screen, where the system keeps two strips for itself, and
 * the buttons are lifted clear of both (the comment on the Column below shows how):
 *
 * - the navigation bar, which holds the back, home and recents buttons or the thin gesture hint,
 *   and which the system draws on top of the app;
 * - the bottom gesture zone, where a swipe up means "go home". The system reads a touch there as
 *   the start of a gesture, so a button inside it can bring up the phone's own navigation instead
 *   of being tapped. It is not always as tall as the navigation bar: with the navigation bar
 *   hidden, or with swipe gestures on and the gesture hint turned off (as One UI allows on the
 *   owner's Galaxy A13), the navigation bar is 0 dp tall but the gesture zone is still there.
 *   Padding by the navigation bar alone would then leave the buttons inside the gesture zone.
 *
 * Every button is at least 56 dp tall. [shareEnabled] is false while a share is being prepared.
 */
@Composable
private fun SavedActions(
    shareEnabled: Boolean,
    onShare: () -> Unit,
    onEditAnother: () -> Unit,
    onHome: () -> Unit,
) {
    val shareLabel = stringResource(R.string.saved_share)
    val editAnotherLabel = stringResource(R.string.saved_edit_another)
    val homeLabel = stringResource(R.string.saved_home)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                // Window insets are the strips along the screen's edges that the system draws on
                // (the navigation bar) or listens on (the gesture zones). `only(Bottom)` keeps just
                // the bottom gesture zone: the left and right ones are where the back swipe
                // starts, and padding them would only narrow the row. `union` then takes, on each
                // side, the larger of the two insets, so the bottom padding is the navigation
                // bar's height or the gesture zone's, whichever is taller, never the two added
                // together. The padding sits inside the Surface, so the Surface's colour still
                // runs down behind the navigation bar while the buttons stay above both strips.
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.saved_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            ButtonGroup(
                // The "more" button the group shows if its items ever did not fit; it opens a menu
                // with the items that did not. With the weights below nothing overflows, but an
                // empty indicator would make an overflowing item impossible to reach.
                overflowIndicator = { menu -> ButtonGroupDefaults.OverflowIndicator(menuState = menu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SAVED_ACTIONS_TAG),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Each customItem is one button in the row (buttonGroupContent) plus the line the
                // same action would have in the overflow menu (menuContent). The relative widths
                // are 1 : 1.3 : 1: "Edit another" is the longest label, so on a phone it gets a
                // little more of the row and still fits on one line, while Share and Home keep
                // theirs on one line too.
                customItem(
                    buttonGroupContent = {
                        ActionButton(
                            label = shareLabel,
                            icon = R.drawable.ic_share,
                            onClick = onShare,
                            tag = SAVED_SHARE_TAG,
                            iconTag = SAVED_SHARE_ICON_TAG,
                            relativeWidth = 1f,
                            primary = true,
                            enabled = shareEnabled,
                        )
                    },
                    menuContent = { menu ->
                        ActionMenuItem(
                            label = shareLabel,
                            icon = R.drawable.ic_share,
                            onClick = onShare,
                            enabled = shareEnabled,
                            menu = menu,
                        )
                    },
                )
                customItem(
                    buttonGroupContent = {
                        ActionButton(
                            label = editAnotherLabel,
                            icon = R.drawable.ic_edit,
                            onClick = onEditAnother,
                            tag = SAVED_EDIT_ANOTHER_TAG,
                            iconTag = SAVED_EDIT_ANOTHER_ICON_TAG,
                            relativeWidth = 1.3f,
                        )
                    },
                    menuContent = { menu ->
                        ActionMenuItem(
                            label = editAnotherLabel,
                            icon = R.drawable.ic_edit,
                            onClick = onEditAnother,
                            menu = menu,
                        )
                    },
                )
                customItem(
                    buttonGroupContent = {
                        ActionButton(
                            label = homeLabel,
                            icon = R.drawable.ic_home,
                            onClick = onHome,
                            tag = SAVED_HOME_TAG,
                            iconTag = SAVED_HOME_ICON_TAG,
                            relativeWidth = 1f,
                        )
                    },
                    menuContent = { menu ->
                        ActionMenuItem(
                            label = homeLabel,
                            icon = R.drawable.ic_home,
                            onClick = onHome,
                            menu = menu,
                        )
                    },
                )
            }
        }
    }
}

/**
 * One button of the action row: an icon and a label, in the group's filled style when [primary]
 * and in its softer tonal style otherwise (Share is the main action of this screen; Edit another
 * and Home are the ways out of it).
 *
 * This is an extension of [ButtonGroupScope], the receiver of the braces after `ButtonGroup(...)`,
 * and that is on purpose: the two modifiers below that only a group understands, `weight` and
 * `animateWidth`, are members of that scope, so they can only be called with it in hand.
 *
 * - `weight(relativeWidth)` gives the button a share of the row proportional to [relativeWidth],
 *   as `weight` does inside a Row, so the buttons always fill the row and none is pushed out.
 * - `animateWidth(interactionSource, ...)` lets the group watch this button's presses, and widen
 *   the pressed button while it narrows the others. The button must be given the same
 *   interaction source, which is what reports the presses.
 * - `heightIn(min = ACTION_MIN_HEIGHT)` and `testTag(tag)` are the usual size floor and test tag.
 *
 * `shapes = ButtonDefaults.shapes()` is what makes the button morph: it holds two shapes, a pill
 * at rest and a squarer one while pressed, and the button animates between them. [onClick] runs
 * when the button is tapped; [enabled] is false while a share is being prepared. The label may
 * take two lines, which is why [ActionLabel] allows them. [iconTag] is handed to [ActionLabel],
 * which puts it on the icon, so a test can check the icon is there.
 */
@Composable
private fun ButtonGroupScope.ActionButton(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    tag: String,
    iconTag: String,
    relativeWidth: Float,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val modifier = Modifier
        .weight(relativeWidth)
        .animateWidth(interactionSource, PRESS_COMPRESSION)
        .heightIn(min = ACTION_MIN_HEIGHT)
        .testTag(tag)
    val shapes = ButtonDefaults.shapes()

    if (primary) {
        Button(
            onClick = onClick,
            shapes = shapes,
            modifier = modifier,
            enabled = enabled,
            contentPadding = ACTION_PADDING,
            interactionSource = interactionSource,
        ) {
            ActionLabel(icon = icon, label = label, iconTag = iconTag)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            shapes = shapes,
            modifier = modifier,
            enabled = enabled,
            contentPadding = ACTION_PADDING,
            interactionSource = interactionSource,
        ) {
            ActionLabel(icon = icon, label = label, iconTag = iconTag)
        }
    }
}

/**
 * What goes inside an action button: the Material Symbols icon, a gap, and the label.
 *
 * The icon is drawn with `Icon(painterResource(...))`, which tints it with the button's content
 * colour (so it is readable on a filled button and on a tonal one, in light and in dark); it has
 * no content description because the label next to it already says what the button does. Its size
 * and the gap come from [ButtonDefaults] for the small button size, the size whose 14 sp label
 * text fits three to a row. The label may wrap to two lines, centred, and ends with an ellipsis if
 * a very large font needs more: a long label never hides the button.
 *
 * The icon carries the test tag [iconTag]. A button merges everything inside it into one node of
 * the semantics tree (the tree tests and screen readers read), and an icon with no description
 * adds nothing to that node, so the merged tree cannot tell a button with an icon from one
 * without. The unmerged tree keeps every tagged node inside the button as a node of its own, and
 * that is where a test looks for the icon by this tag. A test tag is never read aloud.
 */
@Composable
private fun ActionLabel(@DrawableRes icon: Int, label: String, iconTag: String) {
    val sizeClass = ButtonDefaults.MinHeight
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier
            .size(ButtonDefaults.iconSizeFor(sizeClass))
            .testTag(iconTag),
    )
    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(sizeClass)))
    Text(
        text = label,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

/**
 * The line an action has in the group's overflow menu: the icon and the same label, and the same
 * action as the button. It only ever shows if the group runs out of room. [menu] is the menu's
 * state, used to close the menu once an action is chosen.
 */
@Composable
private fun ActionMenuItem(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    menu: ButtonGroupMenuState,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onClick()
            menu.dismiss()
        },
        leadingIcon = { Icon(painter = painterResource(icon), contentDescription = null) },
        enabled = enabled,
    )
}

/**
 * A made-up photo for the preview: a warm sky-to-sand gradient with a sun, 600 by 800 pixels
 * (portrait), drawn with Android's Canvas so no file, camera or MediaStore is needed.
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

/** The sample photo the preview shows, wrapped the way a decoded photo is in the screen's state. */
private fun samplePhoto(): SavedPhoto = SavedPhoto.Ready(sampleBitmap())

/**
 * The Saved screen with a photo on show and every button ready, drawn once in light and once in
 * dark, from a [SavedUiState] built out of sample data. [SavedContent] draws the success mark
 * ([SavedMark], at rest in the preview pane, see there), the photo ([PhotoView]) and the bottom
 * row of three buttons in their button group ([SavedActions], whose parts are [ActionButton] and
 * [ActionLabel]), so this one picture covers them all. The overflow menu's lines,
 * [ActionMenuItem], never show here (the row always fits), so they have a preview of their own,
 * [ActionMenuItemPreview]. No preview shows the photo area while the photo is still being
 * decoded: its indicator never stops moving, so a still picture of it would mean nothing.
 * `ThemeMode.System` makes KansoTheme follow the preview's own light or dark picture; the taps do
 * nothing.
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
            onHome = {},
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

/**
 * The lines of the button group's overflow menu, drawn once in light and once in dark.
 * [SavedPreview] never shows them: every button in the row has a weight, so the row always fits,
 * the "more" menu stays closed and its [ActionMenuItem]s are never drawn. This preview draws the
 * three lines the menu would hold (Share, Edit another and Home, each with its label and its icon
 * from the app's drawables) one under the other, on the colour a Material drop-down menu sits on.
 * The menu state is a fresh, closed one made here, and the taps do nothing.
 */
@PreviewLightDark
@Composable
private fun ActionMenuItemPreview() {
    // ActionMenuItem only needs a menu state to close the menu after a tap; a closed one will do.
    val menu = remember { ButtonGroupMenuState() }
    KansoTheme(themeMode = ThemeMode.System) {
        // MenuDefaults.containerColor is the background of a real drop-down menu, so the lines are
        // seen on the colour they would have on a phone.
        Surface(color = MenuDefaults.containerColor) {
            Column {
                ActionMenuItem(
                    label = stringResource(R.string.saved_share),
                    icon = R.drawable.ic_share,
                    onClick = {},
                    menu = menu,
                )
                ActionMenuItem(
                    label = stringResource(R.string.saved_edit_another),
                    icon = R.drawable.ic_edit,
                    onClick = {},
                    menu = menu,
                )
                ActionMenuItem(
                    label = stringResource(R.string.saved_home),
                    icon = R.drawable.ic_home,
                    onClick = {},
                    menu = menu,
                )
            }
        }
    }
}
