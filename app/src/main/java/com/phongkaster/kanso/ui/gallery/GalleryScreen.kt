package com.phongkaster.kanso.ui.gallery

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.permission.AccessRefusedPanel
import com.phongkaster.kanso.ui.permission.PermissionAccess
import com.phongkaster.kanso.ui.permission.PermissionGate
import com.phongkaster.kanso.ui.permission.PermissionGateContent
import com.phongkaster.kanso.ui.permission.fullPhotoPermissionFor
import com.phongkaster.kanso.ui.permission.openAppDetailsSettings
import com.phongkaster.kanso.ui.permission.partialPhotoPermissionsFor
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.max

/** The test tag on the root of the gallery screen, present in every permission state. */
const val GALLERY_SCREEN_TAG = "gallery-screen"

/** The test tag on the "photo access is off" explanation. */
const val GALLERY_REFUSED_TAG = "gallery-refused"

/** The test tag on the notice that only some photos are shared (Android 14+). */
const val GALLERY_PARTIAL_NOTICE_TAG = "gallery-partial-notice"

/** The test tag on the "share more photos" button of the partial-access notice. */
const val GALLERY_SHARE_MORE_TAG = "gallery-share-more"

/** The test tag on the button that opens the app's page in the system settings. */
const val GALLERY_SETTINGS_BUTTON_TAG = "gallery-settings-button"

/** The test tag on the top bar's camera button, which takes a new photo for the tool. */
const val GALLERY_CAMERA_TAG = "gallery-camera"

/** The test tag on the area that shows the photos once access is granted. */
const val GALLERY_BODY_TAG = "gallery-body"

/** The test tag on the scrolling grid of photos. */
const val GALLERY_GRID_TAG = "gallery-grid"

/** The test tag on the message shown when there are no photos to show. */
const val GALLERY_EMPTY_TAG = "gallery-empty"

/**
 * The test tag on the loading indicator shown while the photos are read for the first time (see
 * [GalleryLoading]). The indicator has no text to find it by, so tests look for this tag.
 */
const val GALLERY_LOADING_TAG = "gallery-loading"

/** The start of every photo cell's test tag; the photo's id follows it. */
const val GALLERY_PHOTO_TAG_PREFIX = "gallery-photo-"

/** Returns the test tag of the grid cell showing the photo with this [id]. */
fun galleryPhotoTag(id: Long): String = "$GALLERY_PHOTO_TAG_PREFIX$id"

/** Returns the test tag of the album chip for album [id], or of "All photos" when it is null. */
fun galleryAlbumTag(id: Long?): String = "gallery-album-${id ?: "all"}"

/** The smallest width of a grid cell; the grid fits as many columns of at least this as it can. */
private val CELL_MIN_SIZE = 110.dp

/**
 * The gap between two grid cells. It is wide enough that the rounded corners of neighbouring
 * cells never touch, so each photo reads as a tile of its own.
 */
private val CELL_GAP = 4.dp

/**
 * The space around the whole grid, and on each side of the row of album chips, so the outer
 * cells and the first chip do not press against the edges of the screen.
 */
private val GRID_PADDING = 8.dp

/** The size of the top bar's back button: Android's smallest comfortable touch target. */
private val TOP_BAR_BUTTON_SIZE = 48.dp

/** The content type shared by every photo cell (see [PhotoGrid]). */
private const val PHOTO_CELL_CONTENT_TYPE = "photo"

/**
 * The photo gallery of a tool: a top bar titled "<tool>: choose a photo" with a back arrow, and
 * below it the photos, once the person has let Kanso read them.
 *
 * The first time it is shown it asks for the photo permission that fits the phone's Android
 * version (see [photoPermissionsFor]) through [PermissionGate]; the app never asks at launch.
 * With full access it shows the photos; with partial access (Android 14+ "Select photos") it
 * shows them under a notice with a "share more" button; after a refusal it explains why access
 * is needed and offers a button to the app's system settings.
 *
 * Why the gallery is two composables. Like Home, it is split in two (state hoisting; see
 * `HomeScreen` for the full story). This function is the *stateful* part: it runs the permission
 * gate, reads the gallery's state (one [GalleryUiState]) from [viewModel], asks it to read the
 * library again each time the screen comes back to the front, and hands all of that, as that
 * state, the gate's answer and callbacks, to the stateless [GalleryContent], which does all the
 * drawing. A preview cannot show the system's permission dialog, keep a ViewModel alive or read
 * the phone's photos, but it can call [GalleryContent] with a [GalleryUiState] built from
 * made-up albums and photos, and that is what the previews at the bottom of this file do.
 *
 * What is specific here: the gate itself stays in this part, because it needs the system's
 * permission dialog. For each answer the gate gives (full, partial, refused, still waiting) this
 * part draws [GalleryContent] with that answer, so a preview can pick any answer without asking.
 *
 * [onBack] is called when the user taps the back arrow; the caller pops the back stack.
 * [onPhotoChosen] is called with the tool and the photo's content URI (as text) when the user
 * taps a photo; the caller opens the tool's screen. [onOpenCamera] is called with the tool when
 * the user taps the camera button in the top bar, to take a new photo for the tool instead of
 * picking one; the caller opens the camera. The [viewModel] is passed in by the
 * navigation entry, which owns it.
 */
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onPhotoChosen: (Tool, String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenCamera: (Tool) -> Unit = {},
) {
    val context = LocalContext.current
    val sdkInt = Build.VERSION.SDK_INT
    // collectAsState turns the ViewModel's StateFlow into Compose state: when it changes, the
    // gallery is drawn again with the new value.
    val state by viewModel.state.collectAsState()

    // The whole screen for one answer of the gate: [access] and [answered] say which answer,
    // [shareMore] asks for photo access again (only the partial answer has one). It is a
    // composable lambda, a piece of UI kept in a value, so the four answers below share it.
    val gallery: @Composable (PermissionAccess, Boolean, () -> Unit) -> Unit =
        { access, answered, shareMore ->
            if (access != PermissionAccess.None) {
                // LifecycleResumeEffect runs its block when the screen is resumed (in front and
                // active): right away if it already is, and again each time the person comes back
                // to it. It is only set up once photo access is granted, so this reads the
                // library as soon as access arrives, and again after a trip to the system
                // settings, the camera or another app.
                LifecycleResumeEffect(viewModel) {
                    viewModel.refresh()
                    onPauseOrDispose { }
                }
            }
            GalleryContent(
                state = state,
                access = access,
                answered = answered,
                onBack = onBack,
                onOpenCamera = { onOpenCamera(state.tool) },
                onSelectAlbum = viewModel::selectAlbum,
                onPhotoClick = { photo -> onPhotoChosen(state.tool, photo.uri) },
                onShareMore = shareMore,
                onOpenSettings = { context.openAppDetailsSettings() },
                modifier = modifier,
            )
        }

    PermissionGate(
        permissions = photoPermissionsFor(sdkInt),
        fullAccess = listOf(fullPhotoPermissionFor(sdkInt)),
        partialAccess = partialPhotoPermissionsFor(sdkInt),
        granted = { gallery(PermissionAccess.Full, true) {} },
        partial = { shareMore -> gallery(PermissionAccess.Partial, true, shareMore) },
        refused = { gallery(PermissionAccess.None, true) {} },
        waiting = { gallery(PermissionAccess.None, false) {} },
    )
}

/**
 * The stateless part of the gallery (see [GalleryScreen] for why there are two): the top bar
 * titled with the name of [state]'s tool, with its back arrow and camera button, and below it
 * what fits the photo access the person gave.
 *
 * The top bar stays on screen whatever the access. Below it, [access] and [answered] choose the
 * rest, the same way the permission gate does (see `PermissionGateContent`): with full access
 * the album switcher and the photos; with partial access the same under a notice whose button
 * calls [onShareMore]; after a refusal ([PermissionAccess.None] and [answered]) the explanation
 * with a button that calls [onOpenSettings]; and nothing while the question is still open.
 * [access] and [answered] are not part of [state]: they are the permission gate's answer, which
 * only the drawing side can get from the system.
 *
 * [state] is what the gallery shows (see [GalleryUiState]): the tool, the albums, the chosen
 * album (null means "All photos"), its photos, and whether the library is still being read.
 * Taps are reported, never acted on here: [onBack] for the back arrow, [onOpenCamera] for the
 * camera button, [onSelectAlbum] for an album chip and [onPhotoClick] for a photo.
 *
 * [thumbnail] draws one photo cell's picture, given the photo, the cell's size in pixels and the
 * cell's modifier (size, rounded shape, background, tap and test tag, which it must use). The app
 * keeps the default, which loads the picture with Coil; a preview cannot read the phone's photos,
 * so it passes a coloured square instead and the grid still looks like a grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryContent(
    state: GalleryUiState,
    access: PermissionAccess,
    answered: Boolean,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onSelectAlbum: (Long?) -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onShareMore: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: @Composable (photo: Photo, sizePx: Int, modifier: Modifier) -> Unit =
        { photo, sizePx, cellModifier -> PhotoThumbnail(photo, sizePx, cellModifier) },
) {
    // The state holds the tool itself; its label is looked up in the strings here, so the
    // title follows the phone's language.
    val toolName = stringResource(state.tool.labelRes)

    Scaffold(
        modifier = modifier.testTag(GALLERY_SCREEN_TAG),
        topBar = {
            GalleryTopBar(
                title = stringResource(R.string.gallery_title, toolName),
                onBack = onBack,
                onOpenCamera = onOpenCamera,
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar and the system bars take, the navigation bar at
        // the bottom included. consumeWindowInsets tells the content below that those insets are
        // already dealt with, so the grid's own inset padding (see PhotoGrid) only adds what is
        // still missing: the part of the bottom gesture zone taller than the navigation bar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            PermissionGateContent(
                access = access,
                answered = answered,
                granted = {
                    GalleryBody(
                        albums = state.albums,
                        selectedAlbumId = state.selectedAlbumId,
                        photos = state.photos,
                        loading = state.loading,
                        onSelectAlbum = onSelectAlbum,
                        onPhotoClick = onPhotoClick,
                        thumbnail = thumbnail,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                partial = { shareMore ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        PartialAccessNotice(onShareMore = shareMore)
                        GalleryBody(
                            albums = state.albums,
                            selectedAlbumId = state.selectedAlbumId,
                            photos = state.photos,
                            loading = state.loading,
                            onSelectAlbum = onSelectAlbum,
                            onPhotoClick = onPhotoClick,
                            thumbnail = thumbnail,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                },
                refused = {
                    AccessRefusedPanel(
                        title = stringResource(R.string.gallery_refused_title),
                        message = stringResource(R.string.gallery_refused_message),
                        buttonLabel = stringResource(R.string.gallery_open_settings),
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.testTag(GALLERY_REFUSED_TAG),
                        buttonModifier = Modifier.testTag(GALLERY_SETTINGS_BUTTON_TAG),
                    )
                },
                // While the system's question is still open the area below the top bar is empty.
                waiting = {},
                requestAgain = onShareMore,
            )
        }
    }
}

/**
 * The gallery's top bar: the back arrow (calls [onBack]), the [title] and, at the right end, the
 * camera button (calls [onOpenCamera]).
 *
 * Both buttons are Material 3 Expressive icon buttons. Given `shapes` (a set of shapes) instead
 * of one shape, an icon button changes its shape while a finger is on it: the round button
 * squares its corners a little, and Material animates between the two with the theme's motion
 * scheme, so a press feels physical. `IconButtonDefaults.shapes()` is the standard pair.
 *
 * - Back is a plain icon button: an arrow with no container, as the start of a top bar usually
 *   has. It is given 48 dp, the smallest touch target Android recommends, for its drawn shape as
 *   well as its touch area (the shape would otherwise be 40 dp).
 * - The camera button is a *filled tonal* icon button: it sits on a soft circle of the theme's
 *   secondary container colour, which marks it as the screen's one extra action without the
 *   weight of a filled primary button. Material keeps its touch area at least 48 dp by itself.
 *
 * Both icons are Material Symbols drawables drawn with `Icon(painterResource(...))`. The files
 * paint white; `Icon` tints them with the content colour the bar or the button provides, so they
 * show in light and in dark. Their content descriptions are the buttons' spoken names: "Back",
 * and "Take a photo" for the camera, which a test reads.
 *
 * The title uses `titleLargeEmphasized`, one of the "Emphasized" text styles Material 3
 * Expressive adds: the size of `titleLarge`, but bolder, so the title stands out. The title may
 * take two lines, so even "Remove background: choose a photo" shows its tool name on a narrow
 * phone; only a longer text than that ends with an ellipsis, rather than pushing the camera
 * button off the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(title: String, onBack: () -> Unit, onOpenCamera: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(TOP_BAR_BUTTON_SIZE),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        // actions are the buttons at the right end of the top bar.
        actions = {
            FilledTonalIconButton(
                onClick = onOpenCamera,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag(GALLERY_CAMERA_TAG),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = stringResource(R.string.gallery_open_camera),
                )
            }
        },
    )
}

/**
 * The area that shows the photos: the album switcher on top (when there are [albums]) and the
 * grid of [photos] below it, or a short message when the chosen album has no photos. While the
 * library is [loading] and no photo is known yet, the Expressive `LoadingIndicator` shows in the
 * middle instead (see [GalleryLoading]); once photos are on screen a new read keeps them there
 * and shows no indicator, so the grid never blinks away.
 *
 * Tapping an album chip calls [onSelectAlbum] with its id ([selectedAlbumId] is the chosen one,
 * null for "All photos"); tapping a photo calls [onPhotoClick] with it. [thumbnail] draws each
 * cell's picture (see [GalleryContent]).
 */
@Composable
private fun GalleryBody(
    albums: List<Album>,
    selectedAlbumId: Long?,
    photos: List<Photo>,
    loading: Boolean,
    onSelectAlbum: (Long?) -> Unit,
    onPhotoClick: (Photo) -> Unit,
    thumbnail: @Composable (photo: Photo, sizePx: Int, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(GALLERY_BODY_TAG)) {
        if (albums.isNotEmpty()) {
            AlbumSwitcher(
                albums = albums,
                selectedAlbumId = selectedAlbumId,
                onSelect = onSelectAlbum,
            )
        }
        val rest = Modifier
            .weight(1f)
            .fillMaxWidth()
        when {
            photos.isNotEmpty() -> PhotoGrid(
                photos = photos,
                onPhotoClick = onPhotoClick,
                thumbnail = thumbnail,
                modifier = rest,
            )
            // While a read with no photo to show yet is running, the indicator says so; the
            // empty message waits for the read to finish, so it never flashes up before the
            // photos arrive.
            loading -> GalleryLoading(rest)
            else -> EmptyGallery(rest)
        }
    }
}

/**
 * What the photo area shows while the library is being read and no photo is known yet: Material 3
 * Expressive's loading indicator, centred in the space [modifier] gives it.
 *
 * `LoadingIndicator` is the Expressive replacement for the old round spinner. Instead of a
 * turning arc it draws one small shape that keeps morphing into the next (a soft burst, a cookie,
 * a pentagon, a pill and a few more) while it slowly rotates, in the theme's primary colour. This
 * is its *indeterminate* form: it is given no progress, because nobody knows how long reading the
 * photo library will take, so it simply animates for as long as it is on screen. It never
 * finishes by itself; it stops when it leaves the composition, here as soon as the photos (or
 * the empty message) take its place.
 *
 * Three details are worth knowing:
 *
 * - It is still marked experimental in this version of Material 3, so calling it needs an
 *   explicit opt-in, `OptIn(ExperimentalMaterial3ExpressiveApi::class)`. The opt-in sits on this
 *   one small function rather than on the whole screen, so the rest of the file uses only stable
 *   APIs, and it is plain to see what would need a change if the API moved.
 * - It has no text, so a screen reader would have nothing to say about it. The content
 *   description set on its modifier ("Loading photos") becomes its spoken name; the indicator
 *   itself adds that it is a progress indicator that is still running.
 * - It carries the test tag [GALLERY_LOADING_TAG] so tests can find it. Because it animates
 *   forever, a Compose test that waited for the screen to settle while it shows would wait
 *   forever too, so the tests pause the test clock before showing it and move time on by hand.
 *   For the same reason no preview in this file shows the loading state: the previews always
 *   pass a state whose read has finished.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GalleryLoading(modifier: Modifier = Modifier) {
    // Read in composition, so the description follows the phone's language.
    val description = stringResource(R.string.gallery_loading)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LoadingIndicator(
            modifier = Modifier
                .testTag(GALLERY_LOADING_TAG)
                .semantics { contentDescription = description },
        )
    }
}

/**
 * A row of chips to choose which photos to show: "All photos" first, then one chip per album
 * in [albums]. The chip of [selectedAlbumId] (null means "All photos") is drawn as selected;
 * tapping a chip calls [onSelect] with its album id. The row scrolls sideways when the chips
 * do not fit; its scroll position is remembered, so it stays put when the photos are read again.
 */
@Composable
private fun AlbumSwitcher(
    albums: List<Album>,
    selectedAlbumId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = GRID_PADDING, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AlbumChip(
            label = stringResource(R.string.gallery_all_photos),
            selected = selectedAlbumId == null,
            onClick = { onSelect(null) },
            tag = galleryAlbumTag(null),
        )
        albums.forEach { album ->
            AlbumChip(
                label = album.name,
                selected = selectedAlbumId == album.id,
                onClick = { onSelect(album.id) },
                tag = galleryAlbumTag(album.id),
            )
        }
    }
}

/**
 * One album chip: a Material filter chip showing [label], marked [selected] when its album is
 * the one on show, calling [onClick] when tapped, and carrying the test tag [tag].
 *
 * A filter chip is a small selectable label; `selected` is part of what it reports to screen
 * readers and tests (it is announced as selected), not only a colour. This chip is given
 * `shapes = FilterChipDefaults.shapes()`, the Material 3 Expressive form: instead of one fixed
 * shape it holds three, taken from the theme's shapes, a rounded rectangle at rest, a full pill
 * when selected and a squarer one while pressed, and it morphs between them with the theme's
 * motion scheme. So the chosen album stands out by its shape as well as by its tonal colour, and
 * a tick in front of its label says it once more without relying on colour. The label keeps the
 * chip's own `labelLarge` text style.
 */
@Composable
private fun AlbumChip(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, maxLines = 1) },
        shapes = FilterChipDefaults.shapes(),
        modifier = Modifier.testTag(tag),
        // The chip animates the tick in and out by itself when this switches between an icon
        // and null. The tick has no description: the chip already says it is selected.
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

/**
 * The photos in a grid of square cells with rounded corners, newest first, as many columns as fit
 * (each at least [CELL_MIN_SIZE] wide). Tapping a cell calls [onPhotoClick] with its photo.
 *
 * How a lazy grid works: a phone may hold thousands of photos, and building a cell for each one
 * would use a lot of memory and time. `LazyVerticalGrid` builds only the cells that are on
 * screen (plus a few just outside it, ready for scrolling). When a cell scrolls away it is
 * dropped and its slot is reused for the next one coming in, so the cost stays the same with
 * fifty photos or fifty thousand.
 *
 * Why each cell has a key: by default the grid knows its cells only by their position (0, 1, 2,
 * ...). When the list is read again and a new photo appears at the top, every position shifts
 * by one, and the grid would keep the same position, so the person would suddenly see different
 * photos. With `key = photo.id` the grid knows each cell by its photo instead: it keeps the same
 * photo in view at the same place, and only the cells whose photo really changed are rebuilt.
 * `contentType` tells the grid that all cells are built the same way, so any dropped cell can be
 * reused for any photo.
 *
 * The bottom edge. The screen's last row must not sit in the bottom gesture zone, the strip where
 * a swipe up means "go home": a tap there may start the system gesture instead. That zone is not
 * always as tall as the navigation bar (with swipe gestures on and the gesture hint hidden, the
 * navigation bar is 0 dp but the zone is still there), so the grid is padded by
 * `navigationBars.union(systemGestures.only(Bottom))`, the taller of the two. `only(Bottom)`
 * leaves out the side gesture zones, where the back swipe starts, which would only narrow the
 * grid. The navigation bar itself is already covered by the Scaffold's padding and marked as
 * consumed (see [GalleryContent]), so here only the part of the gesture zone taller than the
 * navigation bar is added, never the two counted twice.
 *
 * [thumbnail] draws each cell's picture (see [GalleryContent]).
 */
@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    onPhotoClick: (Photo) -> Unit,
    thumbnail: @Composable (photo: Photo, sizePx: Int, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The grid's scroll position. It lives here, outside the grid, so it outlasts each reload.
    val gridState = rememberLazyGridState()

    // BoxWithConstraints tells its content how much room it has. It is used to work out the
    // width of a cell in pixels, so each thumbnail is decoded at the size it is drawn at rather
    // than at the photo's full size, which could be fifty times larger.
    BoxWithConstraints(modifier = modifier) {
        val columns = max(1, (maxWidth / CELL_MIN_SIZE).toInt())
        val cellSizePx = if (constraints.hasBoundedWidth) {
            max(1, constraints.maxWidth / columns)
        } else {
            with(LocalDensity.current) { CELL_MIN_SIZE.roundToPx() }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = CELL_MIN_SIZE),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .testTag(GALLERY_GRID_TAG)
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
                ),
            contentPadding = PaddingValues(GRID_PADDING),
            horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
            verticalArrangement = Arrangement.spacedBy(CELL_GAP),
        ) {
            items(
                items = photos,
                key = { photo -> photo.id },
                contentType = { PHOTO_CELL_CONTENT_TYPE },
            ) { photo ->
                PhotoCell(
                    photo = photo,
                    sizePx = cellSizePx,
                    onClick = { onPhotoClick(photo) },
                    thumbnail = thumbnail,
                )
            }
        }
    }
}

/**
 * One square cell of the grid: the thumbnail of [photo], decoded at [sizePx] pixels and cropped
 * to fill the square, inside a rounded tile. Tapping it calls [onClick].
 *
 * The cell decides its size, shape, background, tap and test tag, and hands that modifier to
 * [thumbnail], which draws the picture: [PhotoThumbnail] in the app, a coloured square in a
 * preview. The order of the modifiers matters, because each one wraps the ones after it:
 * `clip` comes before the background, the tap and the picture, so all three are cut to the
 * theme's large rounded shape, the tap's ripple included. Outside the rounded corners the screen's
 * own background shows. The background, `surfaceContainerHigh` from the theme, is what the tile
 * shows until its picture is loaded, so an empty tile is still visible in light and in dark.
 */
@Composable
private fun PhotoCell(
    photo: Photo,
    sizePx: Int,
    onClick: () -> Unit,
    thumbnail: @Composable (photo: Photo, sizePx: Int, modifier: Modifier) -> Unit,
) {
    thumbnail(
        photo,
        sizePx,
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .testTag(galleryPhotoTag(photo.id)),
    )
}

/**
 * The picture of one grid cell in the app: the thumbnail of [photo], decoded at [sizePx] pixels
 * and cropped to fill the cell, drawn with the cell's [modifier].
 *
 * The thumbnail is loaded by Coil's AsyncImage: it reads and decodes the photo on a background
 * thread, keeps recent thumbnails in memory so scrolling back is instant, and draws the cell's
 * background colour until the picture is ready. Coil reads the photo through its content URI.
 * It has no preview of its own: a preview cannot read the phone's photos, so the gallery's
 * previews draw a coloured square in its place (see [GalleryContent]).
 */
@Composable
private fun PhotoThumbnail(
    photo: Photo,
    sizePx: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // The request is remembered so it is not rebuilt each time the cell is drawn again.
    val request = remember(photo.uri, photo.modifiedSeconds, sizePx) {
        // The cache key includes the modified time: a photo edited in place keeps its URI, and
        // would otherwise keep showing its old thumbnail.
        val cacheKey = "${photo.uri}#${photo.modifiedSeconds}"
        ImageRequest.Builder(context)
            .data(photo.uri)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .size(sizePx)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = stringResource(R.string.gallery_photo_description, photo.name),
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/** The message shown when the chosen album (or the whole phone) has no photos. */
@Composable
private fun EmptyGallery(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(GALLERY_EMPTY_TAG)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.gallery_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The banner shown when only some photos are shared (Android 14+): it says so and offers a
 * button that calls [onShareMore], which asks for photo access again so the person can share
 * more photos or all of them.
 */
@Composable
private fun PartialAccessNotice(onShareMore: () -> Unit) {
    // secondaryContainer with onSecondaryContainer text is a Material 3 colour pair, so the
    // notice stays readable in light, dark and custom colours. The theme's large rounded shape
    // and a little space around it make the notice a card that matches the rounded photo tiles.
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GRID_PADDING, vertical = 4.dp)
            .testTag(GALLERY_PARTIAL_NOTICE_TAG),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.gallery_partial_notice),
                style = MaterialTheme.typography.bodyMedium,
            )
            // shapes makes the button change shape while it is pressed, like the top bar's
            // buttons (see GalleryTopBar).
            TextButton(
                onClick = onShareMore,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag(GALLERY_SHARE_MORE_TAG),
            ) {
                Text(stringResource(R.string.gallery_share_more))
            }
        }
    }
}

/** Albums for the previews, named like the folders a phone usually has. Built on first use so the app itself never pays for them. */
private val sampleAlbums by lazy {
    listOf(
        Album(id = 1L, name = "Camera"),
        Album(id = 2L, name = "Screenshots"),
        Album(id = 3L, name = "Travel"),
        Album(id = 4L, name = "WhatsApp Images"),
    )
}

/**
 * Twenty-four photos for the previews, newest first, spread over [sampleAlbums], one hour and a
 * half apart. Their URIs look like the phone's real ones, but nothing reads them: the previews
 * draw [SampleThumbnail] instead of loading the pictures.
 */
private val samplePhotos by lazy {
    (1..24).map { i ->
        Photo(
            id = i.toLong(),
            uri = "content://media/external/images/media/${1_000 + i}",
            name = "IMG_20261004_%04d.jpg".format(i),
            albumId = sampleAlbums[i % sampleAlbums.size].id,
            modifiedSeconds = 1_790_000_000L - i * 5_400L,
        )
    }
}

/**
 * The colour of a photo's square in the previews: the hue moves round the colour wheel with the
 * photo's [Photo.id], so neighbouring cells are easy to tell apart, at a soft saturation and
 * brightness that reads well on light and dark backgrounds alike.
 */
private fun sampleThumbnailColour(photo: Photo): Color =
    Color.hsv(hue = (photo.id * 23 % 360).toFloat(), saturation = 0.45f, value = 0.85f)

/**
 * The stand-in for a photo's thumbnail in the previews: a square of [photo]'s sample colour
 * drawn with the cell's [modifier], so the grid looks like a grid of pictures without reading
 * any photo from the phone. The cell's modifier already clips it to the rounded tile.
 */
@Composable
private fun SampleThumbnail(photo: Photo, modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.background(sampleThumbnailColour(photo)))
}

/**
 * The gallery of the Edit tool for the previews below: [GalleryContent] with the given [access]
 * and [answered], and a [GalleryUiState] built by hand from [albums] and [photos] ("All photos"
 * chosen, the read finished), with [SampleThumbnail] in every cell. Every tap does nothing: a
 * preview only draws. The read is always finished here, so no preview ever shows the loading
 * indicator, which animates forever.
 */
@Composable
private fun SampleGallery(
    access: PermissionAccess,
    answered: Boolean = true,
    albums: List<Album> = sampleAlbums,
    photos: List<Photo> = samplePhotos,
) {
    GalleryContent(
        state = GalleryUiState(
            tool = Tool.EDIT,
            albums = albums,
            selectedAlbumId = null,
            photos = photos,
            loading = false,
        ),
        access = access,
        answered = answered,
        onBack = {},
        onOpenCamera = {},
        onSelectAlbum = {},
        onPhotoClick = {},
        onShareMore = {},
        onOpenSettings = {},
        thumbnail = { photo, _, cellModifier -> SampleThumbnail(photo, cellModifier) },
    )
}

/**
 * The gallery with full photo access: the top bar, the album chips ("All photos" chosen) and a
 * grid of rounded coloured squares standing in for the photos. Drawn in light and dark;
 * `ThemeMode.System` makes KansoTheme follow the preview's mode (see `HomePreview`).
 */
@PreviewLightDark
@Composable
private fun GalleryPhotosPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleGallery(access = PermissionAccess.Full)
    }
}

/**
 * The gallery after the person shared only some photos (Android 14+ "Select photos"): the notice
 * with its "Share more photos" button above the few photos that were shared.
 */
@PreviewLightDark
@Composable
private fun GalleryPartialAccessPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleGallery(
            access = PermissionAccess.Partial,
            albums = sampleAlbums.take(1),
            photos = samplePhotos.filter { it.albumId == sampleAlbums.first().id },
        )
    }
}

/**
 * The gallery after the person refused photo access: the top bar stays, and below it the panel
 * explains why access is needed, with its button to the app's system settings.
 */
@PreviewLightDark
@Composable
private fun GalleryRefusedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleGallery(access = PermissionAccess.None, answered = true)
    }
}

/**
 * The gallery with full access on a phone with no photos at all: no album chips, only the
 * message saying there is nothing to show.
 */
@PreviewLightDark
@Composable
private fun GalleryEmptyPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleGallery(
            access = PermissionAccess.Full,
            albums = emptyList(),
            photos = emptyList(),
        )
    }
}
