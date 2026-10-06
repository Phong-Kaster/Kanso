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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.common.BackArrowIcon
import com.phongkaster.kanso.ui.permission.AccessRefusedPanel
import com.phongkaster.kanso.ui.permission.PermissionGate
import com.phongkaster.kanso.ui.permission.fullPhotoPermissionFor
import com.phongkaster.kanso.ui.permission.openAppDetailsSettings
import com.phongkaster.kanso.ui.permission.partialPhotoPermissionsFor
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
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

/** The test tag on the area that shows the photos once access is granted. */
const val GALLERY_BODY_TAG = "gallery-body"

/** The test tag on the scrolling grid of photos. */
const val GALLERY_GRID_TAG = "gallery-grid"

/** The test tag on the message shown when there are no photos to show. */
const val GALLERY_EMPTY_TAG = "gallery-empty"

/** The start of every photo cell's test tag; the photo's id follows it. */
const val GALLERY_PHOTO_TAG_PREFIX = "gallery-photo-"

/** Returns the test tag of the grid cell showing the photo with this [id]. */
fun galleryPhotoTag(id: Long): String = "$GALLERY_PHOTO_TAG_PREFIX$id"

/** Returns the test tag of the album chip for album [id], or of "All photos" when it is null. */
fun galleryAlbumTag(id: Long?): String = "gallery-album-${id ?: "all"}"

/** The smallest width of a grid cell; the grid fits as many columns of at least this as it can. */
private val CELL_MIN_SIZE = 110.dp

/** The gap between two grid cells, and around the grid. */
private val CELL_GAP = 2.dp

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
 * [onBack] is called when the user taps the back arrow; the caller pops the back stack.
 * [onPhotoChosen] is called with the tool and the photo's content URI (as text) when the user
 * taps a photo; the caller opens the tool's screen. The [viewModel] is passed in by the
 * navigation entry, which owns it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onPhotoChosen: (Tool, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toolName = stringResource(viewModel.tool.labelRes)
    val context = LocalContext.current
    val sdkInt = Build.VERSION.SDK_INT

    Scaffold(
        modifier = modifier.testTag(GALLERY_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gallery_title, toolName)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = BackArrowIcon,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar (and system bars) take; the content avoids it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PermissionGate(
                permissions = photoPermissionsFor(sdkInt),
                fullAccess = listOf(fullPhotoPermissionFor(sdkInt)),
                partialAccess = partialPhotoPermissionsFor(sdkInt),
                granted = {
                    GalleryBody(
                        viewModel = viewModel,
                        onPhotoChosen = onPhotoChosen,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                partial = { shareMore ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        PartialAccessNotice(onShareMore = shareMore)
                        GalleryBody(
                            viewModel = viewModel,
                            onPhotoChosen = onPhotoChosen,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                },
                refused = {
                    AccessRefusedPanel(
                        title = stringResource(R.string.gallery_refused_title),
                        message = stringResource(R.string.gallery_refused_message),
                        buttonLabel = stringResource(R.string.gallery_open_settings),
                        onOpenSettings = { context.openAppDetailsSettings() },
                        modifier = Modifier.testTag(GALLERY_REFUSED_TAG),
                        buttonModifier = Modifier.testTag(GALLERY_SETTINGS_BUTTON_TAG),
                    )
                },
            )
        }
    }
}

/**
 * The area that shows the photos: the album switcher on top and the photo grid below it, or a
 * short message when the chosen album has no photos.
 *
 * Tapping a photo calls [onPhotoChosen] with the gallery's tool and the photo's content URI.
 */
@Composable
private fun GalleryBody(
    viewModel: GalleryViewModel,
    onPhotoChosen: (Tool, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // LifecycleResumeEffect runs its block when the screen is resumed (in front and active):
    // right away if it already is, and again each time the person comes back to it. This body
    // only appears once photo access is granted, so this reads the library as soon as access
    // arrives, and again after a trip to the system settings, the camera or another app.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    // collectAsState turns the ViewModel's StateFlow into Compose state: when it changes, this
    // body is drawn again with the new value.
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.testTag(GALLERY_BODY_TAG)) {
        if (state.albums.isNotEmpty()) {
            AlbumSwitcher(
                albums = state.albums,
                selectedAlbumId = state.selectedAlbumId,
                onSelect = viewModel::selectAlbum,
            )
        }
        val rest = Modifier
            .weight(1f)
            .fillMaxWidth()
        when {
            state.photos.isNotEmpty() -> PhotoGrid(
                photos = state.photos,
                onPhotoClick = { photo -> onPhotoChosen(viewModel.tool, photo.uri) },
                modifier = rest,
            )
            // While the first read is running nothing is shown yet, so the empty message never
            // flashes up before the photos arrive.
            state.loading -> Spacer(rest)
            else -> EmptyGallery(rest)
        }
    }
}

/**
 * A row of chips to choose which photos to show: "All photos" first, then one chip per album
 * in [albums]. The chip of [selectedAlbumId] (null means "All photos") is drawn as selected;
 * tapping a chip calls [onSelect] with its album id. The row scrolls sideways when the chips
 * do not fit.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedAlbumId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.gallery_all_photos)) },
            modifier = Modifier.testTag(galleryAlbumTag(null)),
        )
        albums.forEach { album ->
            FilterChip(
                selected = selectedAlbumId == album.id,
                onClick = { onSelect(album.id) },
                label = { Text(album.name) },
                modifier = Modifier.testTag(galleryAlbumTag(album.id)),
            )
        }
    }
}

/**
 * The photos in a grid of square cells, newest first, as many columns as fit (each at least
 * [CELL_MIN_SIZE] wide). Tapping a cell calls [onPhotoClick] with its photo.
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
 */
@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    onPhotoClick: (Photo) -> Unit,
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
                .testTag(GALLERY_GRID_TAG),
            contentPadding = PaddingValues(CELL_GAP),
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
                )
            }
        }
    }
}

/**
 * One square cell of the grid: the thumbnail of [photo], decoded at [sizePx] pixels and cropped
 * to fill the square. Tapping it calls [onClick].
 *
 * The thumbnail is loaded by Coil's AsyncImage: it reads and decodes the photo on a background
 * thread, keeps recent thumbnails in memory so scrolling back is instant, and draws the cell's
 * background colour until the picture is ready. Coil reads the photo through its content URI.
 */
@Composable
private fun PhotoCell(
    photo: Photo,
    sizePx: Int,
    onClick: () -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .testTag(galleryPhotoTag(photo.id)),
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
    // notice stays readable in light, dark and custom colours.
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
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
            TextButton(
                onClick = onShareMore,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag(GALLERY_SHARE_MORE_TAG),
            ) {
                Text(stringResource(R.string.gallery_share_more))
            }
        }
    }
}
