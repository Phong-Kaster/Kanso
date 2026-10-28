package com.phongkaster.kanso.ui.myedits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.component.ProvideSkeletonPulse
import com.phongkaster.kanso.ui.component.SkeletonBlock
import com.phongkaster.kanso.ui.gallery.PhotoThumbnail
import com.phongkaster.kanso.ui.theme.KansoTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** The test tag on the root of the My edits screen, present in every state. */
const val MY_EDITS_SCREEN_TAG = "my-edits-screen"

/** The test tag on the grid: the grid of photos, or the grid of skeleton tiles while loading. */
const val MY_EDITS_GRID_TAG = "my-edits-grid"

/** The test tag on the message shown when Kanso has saved no photo yet. */
const val MY_EDITS_EMPTY_TAG = "my-edits-empty"

/** The start of every photo tile's test tag; the tile's place in reading order follows it. */
const val MY_EDITS_TILE_TAG_PREFIX = "my-edits-tile-"

/** The start of every skeleton tile's test tag; the tile's place follows it. */
const val MY_EDITS_SKELETON_TAG_PREFIX = "my-edits-skeleton-"

/** Returns the test tag of the photo tile at [index] in reading order (0 is the newest photo). */
fun myEditsTileTag(index: Int): String = "$MY_EDITS_TILE_TAG_PREFIX$index"

/** Returns the test tag of the skeleton tile at [index]. */
fun myEditsSkeletonTag(index: Int): String = "$MY_EDITS_SKELETON_TAG_PREFIX$index"

/** The smallest width of a tile; the grid fits as many columns of at least this as it can. */
private val TILE_MIN_SIZE = 110.dp

/** The gap between two tiles, wide enough that neighbouring rounded corners never touch. */
private val TILE_GAP = 6.dp

/** The space around the whole grid, so the outer tiles do not press against the screen's edges. */
private val GRID_PADDING = 8.dp

/**
 * The size each thumbnail is decoded at: a little more than the widest tile on a phone, so a
 * picture is sharp without decoding the photo at its full size, which can be fifty times larger.
 */
private val TILE_DECODE_SIZE = 160.dp

/** How many skeleton tiles stand in for the photos while they load: about one phone screen. */
private const val SKELETON_COUNT = 18

/** The content type shared by every photo tile (see [MyEditsGrid]). */
private const val TILE_CONTENT_TYPE = "photo"

/** The content type shared by every skeleton tile. */
private const val SKELETON_CONTENT_TYPE = "skeleton"

/** Milliseconds in a second, to turn a photo's time in seconds into a Date. */
private const val MILLIS_PER_SECOND = 1_000L

/**
 * Writes when a photo was saved, [modifiedSeconds] seconds after the start of 1970, as a medium
 * date and a short time in [locale] (for example "Oct 12, 2026, 2:30 PM"): the part of a tile's
 * spoken name that tells one edit from another.
 */
internal fun savedAtText(modifiedSeconds: Long, locale: Locale = Locale.getDefault()): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        .format(Date(modifiedSeconds * MILLIS_PER_SECOND))

/**
 * The My edits tab: every photo Kanso has saved, newest first, as a grid of rounded tiles. A tap
 * on a tile calls [onPhotoClick] with that photo's content Uri as text; the caller decides what
 * opens. While the photos are read the grid shows skeleton tiles, and when there are none a short
 * message says so. There is no title: the floating tab bar already names the tab.
 *
 * Like every screen of the app it is two composables (see `HomeScreen` for why): this stateful
 * part reads the tab's state, one [MyEditsUiState], from [viewModel], and the stateless
 * [MyEditsContent] draws it. This part also does two things a preview must never do:
 *
 * - It asks for a fresh read each time it enters the composition. `LaunchedEffect` runs its block
 *   once when the composable first appears (and again only if its key, here [viewModel], changes);
 *   a tab's screen is built anew each time the person comes back to the tab, so a photo saved a
 *   moment ago is always listed.
 * - It wraps the content in `ProvideSkeletonPulse`, which makes the skeleton tiles' band move.
 *   [MyEditsContent] on its own keeps them still, which is what previews and tests draw.
 *
 * [contentPadding] is the space to keep free at the edges of the grid, given by the caller: the
 * navigation host passes the height the floating tab bar covers at the bottom, so the last row
 * can scroll up above the bar. [modifier] is applied to the whole screen.
 */
@Composable
fun MyEditsScreen(
    viewModel: MyEditsViewModel,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    // collectAsState turns the ViewModel's StateFlow into Compose state: each new snapshot draws
    // the screen again.
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    ProvideSkeletonPulse {
        MyEditsContent(
            state = state,
            onPhotoClick = onPhotoClick,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
}

/**
 * The stateless part of My edits (see [MyEditsScreen] for why there are two). It draws one of
 * three faces from [state] (see [MyEditsUiState]): the grid of photos, the grid of skeleton tiles
 * while the first read runs, or the "no edits yet" message.
 *
 * There is no top bar, so the screen keeps clear of the status bar itself. A Scaffold pads its
 * content by its `contentWindowInsets`, the strips along the screen's edges the system draws on;
 * here they are only the top and the sides (`safeDrawing.only(Top + Horizontal)`), because the
 * bottom is handled by [contentPadding]: the caller's clearance for the floating bar already
 * includes the navigation bar, and counting it twice would leave a gap.
 *
 * [onPhotoClick] gets the tapped photo's Uri as text. [contentPadding] is added to the grid's own
 * padding inside the scrolling area (see [MyEditsGrid]), and to the message's.
 *
 * [thumbnail] draws one tile's picture, given the photo and the tile's modifier (size, rounded
 * shape, background, tap, spoken name and test tag, which it must use). The app keeps the
 * default, the gallery's `PhotoThumbnail`, which loads the picture with Coil; a preview cannot
 * read the phone's photos, so it passes a coloured square instead and the grid still looks like
 * a grid.
 */
@Composable
fun MyEditsContent(
    state: MyEditsUiState,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    thumbnail: @Composable (photo: Photo, modifier: Modifier) -> Unit = { photo, tileModifier ->
        PhotoThumbnail(
            photo = photo,
            sizePx = with(LocalDensity.current) { TILE_DECODE_SIZE.roundToPx() },
            modifier = tileModifier,
            // The tile carries its own spoken name ("Edited photo" and the date).
            contentDescription = null,
        )
    },
) {
    Scaffold(
        modifier = modifier.testTag(MY_EDITS_SCREEN_TAG),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        // consumeWindowInsets tells everything inside that these insets are already dealt with.
        val area = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        when {
            // Photos already on screen stay there while a new read runs, so the grid never blinks.
            state.photos.isNotEmpty() -> MyEditsGrid(
                photos = state.photos,
                onPhotoClick = onPhotoClick,
                contentPadding = contentPadding,
                thumbnail = thumbnail,
                modifier = area,
            )
            state.loading -> MyEditsLoading(contentPadding = contentPadding, modifier = area)
            else -> MyEditsEmpty(contentPadding = contentPadding, modifier = area)
        }
    }
}

/**
 * The saved [photos] as a grid of square, rounded tiles, newest first in reading order (left to
 * right, then row by row), as many columns as fit (each at least [TILE_MIN_SIZE] wide). A tap on
 * a tile calls [onPhotoClick] with the photo's Uri as text.
 *
 * What `LazyVerticalGrid` is. A grid that scrolls up and down and builds only the tiles on screen
 * (plus a few just outside, ready for scrolling): a tile that scrolls away is dropped and its slot
 * reused for the next one, so a hundred edits cost no more than ten. `GridCells.Adaptive` picks
 * the number of columns from the width, so a small phone shows two or three and a tablet more.
 * Each tile is keyed by its photo's id, so when a new edit appears at the top the grid keeps every
 * other tile's picture instead of rebuilding them all.
 *
 * Its `contentPadding` is space inside the scrolling area: the tiles can scroll through it, but
 * the first and last rows come to rest clear of it. That is why the caller's [contentPadding] (the
 * floating bar's clearance) goes there, added to the grid's own [GRID_PADDING]: at the end of the
 * list the last row sits above the bar, while further up the tiles still pass behind it.
 *
 * [thumbnail] draws each tile's picture (see [MyEditsContent]).
 */
@Composable
private fun MyEditsGrid(
    photos: List<Photo>,
    onPhotoClick: (String) -> Unit,
    contentPadding: PaddingValues,
    thumbnail: @Composable (photo: Photo, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = TILE_MIN_SIZE),
        modifier = modifier.testTag(MY_EDITS_GRID_TAG),
        contentPadding = gridPadding(contentPadding, LocalLayoutDirection.current),
        horizontalArrangement = Arrangement.spacedBy(TILE_GAP),
        verticalArrangement = Arrangement.spacedBy(TILE_GAP),
    ) {
        itemsIndexed(
            items = photos,
            key = { _, photo -> photo.id },
            contentType = { _, _ -> TILE_CONTENT_TYPE },
        ) { index, photo ->
            MyEditsTile(
                index = index,
                photo = photo,
                onClick = { onPhotoClick(photo.uri) },
                thumbnail = thumbnail,
            )
        }
    }
}

/**
 * One tile of the grid: [photo]'s picture, square and cut to the theme's large rounded shape, at
 * place [index] in reading order. A tap calls [onClick].
 *
 * The tile shows no text. Its spoken name, "Edited photo" and the date and time it was saved,
 * is set on the tile with `semantics`, so a screen reader can tell the edits apart. The order of
 * the modifiers matters, because each wraps the ones after it: `clip` comes before the background,
 * the tap and the picture, so all three are cut to the rounded shape, the tap's ripple included.
 * The background, `surfaceContainerHigh`, shows until the picture has loaded.
 */
@Composable
private fun MyEditsTile(
    index: Int,
    photo: Photo,
    onClick: () -> Unit,
    thumbnail: @Composable (photo: Photo, modifier: Modifier) -> Unit,
) {
    // Formatting a date takes a moment; remember keeps the text until the photo's time changes.
    val savedAt = remember(photo.modifiedSeconds) { savedAtText(photo.modifiedSeconds) }
    val description = stringResource(R.string.my_edits_photo_description, savedAt)
    thumbnail(
        photo,
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(myEditsTileTag(index)),
    )
}

/**
 * What the grid shows while the saved photos are read for the first time: [SKELETON_COUNT]
 * skeleton tiles, each the size and shape of a photo tile, laid out exactly like [MyEditsGrid]
 * (same columns, gaps and [contentPadding]), so nothing jumps when the photos arrive.
 *
 * The skeletons have no text, so the grid itself carries one spoken name saying the edits are
 * loading; the tiles add nothing for screen readers. It does not scroll: there is nothing to
 * scroll to yet. There is no spinner here; the skeletons' moving band (see [SkeletonBlock]) is
 * the only sign of waiting.
 */
@Composable
private fun MyEditsLoading(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    // Read in composition, so the spoken name follows the phone's language.
    val description = stringResource(R.string.my_edits_loading)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = TILE_MIN_SIZE),
        modifier = modifier
            .testTag(MY_EDITS_GRID_TAG)
            .semantics { contentDescription = description },
        contentPadding = gridPadding(contentPadding, LocalLayoutDirection.current),
        horizontalArrangement = Arrangement.spacedBy(TILE_GAP),
        verticalArrangement = Arrangement.spacedBy(TILE_GAP),
        userScrollEnabled = false,
    ) {
        items(count = SKELETON_COUNT, contentType = { SKELETON_CONTENT_TYPE }) { index ->
            SkeletonBlock(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag(myEditsSkeletonTag(index)),
            )
        }
    }
}

/**
 * The message shown when Kanso has saved no photo yet, or none can be read: a picture-off icon
 * above one short line, centred in the space left above [contentPadding]. The icon only
 * decorates, so it has no spoken name; the text says it all.
 */
@Composable
private fun MyEditsEmpty(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(24.dp)
            .testTag(MY_EDITS_EMPTY_TAG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A Material Symbols drawable: Icon tints it, here with the theme's softer text colour.
        Icon(
            painter = painterResource(R.drawable.ic_hide_image),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.my_edits_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Returns the grid's padding: [GRID_PADDING] on every side plus the caller's [extra] padding,
 * with start and end read for [layoutDirection] (start is the left in English, the right in
 * Arabic or Hebrew).
 */
private fun gridPadding(extra: PaddingValues, layoutDirection: LayoutDirection): PaddingValues =
    PaddingValues(
        start = GRID_PADDING + extra.calculateStartPadding(layoutDirection),
        top = GRID_PADDING + extra.calculateTopPadding(),
        end = GRID_PADDING + extra.calculateEndPadding(layoutDirection),
        bottom = GRID_PADDING + extra.calculateBottomPadding(),
    )

/**
 * Twelve saved photos for the previews, newest first, one hour apart, named like Kanso's own
 * files. Their Uris look like the phone's real ones, but nothing reads them: the previews draw
 * [SampleTile] instead of loading the pictures.
 */
private val samplePhotos by lazy {
    (1..12).map { i ->
        Photo(
            id = i.toLong(),
            uri = "content://media/external/images/media/${2_000 + i}",
            name = "Kanso_20261012_%02d0000.jpg".format(24 - i),
            albumId = 1L,
            modifiedSeconds = 1_791_800_000L - i * 3_600L,
        )
    }
}

/**
 * The colour of a photo's tile in the previews: the hue moves round the colour wheel with the
 * photo's id, so neighbouring tiles are easy to tell apart, at a soft saturation and brightness
 * that reads well on light and dark backgrounds alike.
 */
private fun sampleTileColour(photo: Photo): Color =
    Color.hsv(hue = (photo.id * 37 % 360).toFloat(), saturation = 0.45f, value = 0.85f)

/**
 * The stand-in for a photo's picture in the previews: a square of [photo]'s sample colour drawn
 * with the tile's [modifier], which already cuts it to the rounded tile.
 */
@Composable
private fun SampleTile(photo: Photo, modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.background(sampleTileColour(photo)))
}

/**
 * My edits for the previews below: [MyEditsContent] drawing [state], with [SampleTile] in every
 * tile and taps that do nothing. Nothing here animates: the skeletons keep their still frame.
 */
@Composable
private fun SampleMyEdits(state: MyEditsUiState) {
    MyEditsContent(
        state = state,
        onPhotoClick = {},
        thumbnail = { photo, tileModifier -> SampleTile(photo, tileModifier) },
    )
}

/**
 * My edits with twelve saved photos: rounded coloured squares standing in for the photos, newest
 * first. Drawn in light and dark; `ThemeMode.System` makes KansoTheme follow the preview's mode.
 */
@PreviewLightDark
@Composable
private fun MyEditsPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleMyEdits(MyEditsUiState(photos = samplePhotos, loading = false))
    }
}

/** My edits when Kanso has saved nothing yet: the icon and the short message. */
@PreviewLightDark
@Composable
private fun MyEditsEmptyPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleMyEdits(MyEditsUiState(photos = emptyList(), loading = false))
    }
}

/** My edits while the saved photos are read: the skeleton tiles, standing still. */
@PreviewLightDark
@Composable
private fun MyEditsLoadingPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleMyEdits(MyEditsUiState(loading = true))
    }
}
