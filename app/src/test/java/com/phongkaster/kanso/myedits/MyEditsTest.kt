package com.phongkaster.kanso.myedits

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.ui.myedits.MY_EDITS_EMPTY_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_GRID_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SCREEN_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SKELETON_TAG_PREFIX
import com.phongkaster.kanso.ui.myedits.MY_EDITS_TILE_TAG_PREFIX
import com.phongkaster.kanso.ui.myedits.MyEditsContent
import com.phongkaster.kanso.ui.myedits.MyEditsUiState
import com.phongkaster.kanso.ui.myedits.MyEditsViewModel
import com.phongkaster.kanso.ui.myedits.myEditsTileTag
import com.phongkaster.kanso.ui.myedits.savedAtText
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.abs
import kotlin.math.ceil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the My edits screen's stateless part, hosted on its own (DoD 8 and 9): forty saved
 * photos as rounded tiles in reading order, newest first, each with its spoken name, a scroll that
 * reaches the fortieth above the caller's bottom padding, a tap that reports the photo, the "no
 * edits yet" message, and skeleton tiles (never a spinner) while the photos are still being read.
 *
 * The tiles' pictures are drawn by a stand-in (a solid colour per photo), because a JVM test has
 * no photos to load; the tile around it (size, rounded shape, tap, spoken name, tag) is the
 * screen's own. The skeletons keep their still frame: `MyEditsContent` is drawn without the
 * stateful screen's pulse, so nothing here animates forever.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class MyEditsTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * Forty photos: the tiles on screen are in reading order (left to right, row by row), tile 0
     * is the newest photo, and each tile's spoken name is "Edited photo" and that photo's date.
     */
    @Test
    fun forty_photos_show_as_tiles_in_reading_order_newest_first_with_their_names() {
        val photos = photos(40)
        show(MyEditsUiState(photos = photos, loading = false))

        val placed = placedTilesInReadingOrder()
        assertTrue("At least two rows of tiles should be on screen, found ${placed.size}", placed.size >= 6)
        assertEquals(placed.indices.map { myEditsTileTag(it) }, placed.map { it.config[SemanticsProperties.TestTag] })
        placed.forEachIndexed { index, node ->
            assertEquals(
                "Tile $index",
                listOf(expectedName(photos[index])),
                node.config.getOrNull(SemanticsProperties.ContentDescription),
            )
        }
        assertTrue(expectedName(photos[0]).startsWith("Edited photo, "))
    }

    /**
     * Scrolling the grid reaches the fortieth (oldest) tile, and at the end of the list the last
     * row rests clear of the bottom padding the caller gave (the floating bar's clearance).
     */
    @Test
    fun scrolling_reaches_the_fortieth_tile_above_the_bottom_padding() {
        val photos = photos(40)
        show(MyEditsUiState(photos = photos, loading = false), contentPadding = PaddingValues(bottom = BOTTOM_PADDING_DP.dp))

        compose.onNodeWithTag(MY_EDITS_GRID_TAG).performScrollToIndex(39)
        compose.waitForIdle()

        val last = compose.onNodeWithTag(myEditsTileTag(39))
        last.assertIsDisplayed()
        last.assertContentDescriptionEquals(expectedName(photos[39]))
        val tile = last.fetchSemanticsNode()
        val grid = compose.onNodeWithTag(MY_EDITS_GRID_TAG).fetchSemanticsNode()
        val gap = grid.positionInRoot.y + grid.size.height - (tile.positionInRoot.y + tile.size.height)
        val padding = with(compose.density) { BOTTOM_PADDING_DP.dp.toPx() }
        assertTrue("The last row should rest at least $padding px above the grid's bottom, was $gap", gap >= padding - 1f)
    }

    /** A tap on a tile reports that tile's photo, as its Uri text. */
    @Test
    fun tapping_a_tile_reports_its_photo_uri() {
        val photos = photos(6)
        val tapped = mutableListOf<String>()
        show(MyEditsUiState(photos = photos, loading = false), onPhotoClick = { tapped += it })

        compose.onNodeWithTag(myEditsTileTag(1)).performClick()
        compose.waitForIdle()

        assertEquals(listOf(photos[1].uri), tapped)
    }

    /**
     * The tiles are rounded: the centre of tile 0 shows its picture, while the pixel 1 px inside
     * its top-left corner is outside the rounded shape and shows the background seen in the gap
     * between tiles 0 and 1. A square tile would show the picture there too.
     *
     * The screen is drawn by drawing the activity's content view into a bitmap (Compose's
     * captureToImage times out under Robolectric here), which needs Robolectric's native graphics.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun tiles_are_rounded() {
        show(MyEditsUiState(photos = photos(6), loading = false))

        val tileZero = compose.onNodeWithTag(myEditsTileTag(0)).fetchSemanticsNode().boundsInRoot
        val tileOne = compose.onNodeWithTag(myEditsTileTag(1)).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Tile 1 should sit to the right of tile 0 with a gap: $tileZero, $tileOne",
            tileOne.left - tileZero.right >= 2f && abs(tileOne.top - tileZero.top) < 1f,
        )
        val cornerX = ceil(tileZero.left).toInt() + 1
        val cornerY = ceil(tileZero.top).toInt() + 1
        val centreX = tileZero.center.x.toInt()
        val centreY = tileZero.center.y.toInt()
        val gapX = ((tileZero.right + tileOne.left) / 2f).toInt()
        val (corner, centre, background) = drawScreen { bitmap ->
            Triple(bitmap.getPixel(cornerX, cornerY), bitmap.getPixel(centreX, centreY), bitmap.getPixel(gapX, centreY))
        }

        assertTrue("The centre of tile 0 should show its picture, was ${hex(centre)}", near(centre, TILE_ZERO_COLOUR))
        assertTrue("The corner of tile 0 should not show its picture, was ${hex(corner)}", !near(corner, TILE_ZERO_COLOUR))
        assertTrue("The corner of tile 0 should show the background ${hex(background)}, was ${hex(corner)}", near(corner, background))
    }

    /** With no saved photo and the read finished, the short message shows, and no grid. */
    @Test
    fun no_saved_photo_shows_the_empty_message() {
        show(MyEditsUiState(photos = emptyList(), loading = false))

        compose.onNodeWithTag(MY_EDITS_SCREEN_TAG).assertIsDisplayed()
        compose.onNodeWithTag(MY_EDITS_EMPTY_TAG).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.my_edits_empty)).assertIsDisplayed()
        compose.onAllNodesWithTag(MY_EDITS_GRID_TAG).assertCountEquals(0)
        compose.onAllNodes(isTile).assertCountEquals(0)
        compose.onAllNodes(isSkeleton).assertCountEquals(0)
    }

    /**
     * While the source holds its reads, the grid shows at least six skeleton tiles, the grid alone
     * says the edits are loading, and there is no spinner or progress bar; once the reads are let
     * go, the skeletons give way to the photos.
     */
    @Test
    fun skeletons_show_while_the_photos_load_and_leave_once_they_arrive() {
        val source = FakeSavedPhotoSource(photos(3)).apply { holdReads() }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = MyEditsViewModel(savedPhotos = source, probe = ViewModelClearedProbe())
        viewModel.refresh()
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                MyEditsContent(
                    state = viewModel.state.collectAsState().value,
                    onPhotoClick = {},
                    thumbnail = standIn,
                )
            }
        }
        compose.waitForIdle()

        val skeletons = compose.onAllNodes(isSkeleton).fetchSemanticsNodes().size
        assertTrue("At least six skeleton tiles should show, found $skeletons", skeletons >= 6)
        compose.onNodeWithTag(MY_EDITS_GRID_TAG).assertContentDescriptionEquals(app.getString(R.string.my_edits_loading))
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)).assertCountEquals(1)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(0)
        compose.onAllNodes(isTile).assertCountEquals(0)

        source.releaseReads()
        compose.waitUntil(LOAD_TIMEOUT_MS) {
            // Lets the main thread run the end of the read the fake just let go.
            shadowOf(Looper.getMainLooper()).idle()
            !viewModel.state.value.loading
        }
        compose.waitForIdle()

        compose.onAllNodes(isSkeleton).assertCountEquals(0)
        compose.onAllNodes(isTile).assertCountEquals(3)
        compose.onNodeWithTag(myEditsTileTag(0)).assertIsDisplayed()
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Shows [MyEditsContent] for [state] in Kanso's light theme, with the solid-colour stand-in
     * for each picture, [contentPadding] and [onPhotoClick], and waits until it is drawn.
     */
    private fun show(
        state: MyEditsUiState,
        contentPadding: PaddingValues = PaddingValues(),
        onPhotoClick: (String) -> Unit = {},
    ) {
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                MyEditsContent(
                    state = state,
                    onPhotoClick = onPhotoClick,
                    contentPadding = contentPadding,
                    thumbnail = standIn,
                )
            }
        }
        compose.waitForIdle()
    }

    /**
     * Returns the photo tiles laid out on screen, in reading order: top row first, left to right
     * in each row. Tiles built ahead of scrolling but not placed are left out.
     */
    private fun placedTilesInReadingOrder(): List<SemanticsNode> =
        compose.onAllNodes(isTile).fetchSemanticsNodes()
            .filter { it.layoutInfo.isPlaced }
            .sortedWith(compareBy<SemanticsNode>({ it.positionInRoot.y }, { it.positionInRoot.x }))

    /** Returns the spoken name the screen should give [photo]'s tile. */
    private fun expectedName(photo: Photo): String =
        app.getString(R.string.my_edits_photo_description, savedAtText(photo.modifiedSeconds))

    /**
     * Draws the activity's content view into a fresh bitmap, hands it to [read], then frees it.
     * The Compose root fills the content view from its top-left corner, so a node's
     * `boundsInRoot` are the bitmap's coordinates.
     */
    private fun <T> drawScreen(read: (Bitmap) -> T): T {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return try {
            view.draw(Canvas(bitmap))
            read(bitmap)
        } finally {
            // A bitmap holds native memory; free it now rather than waiting for the collector.
            bitmap.recycle()
        }
    }

    /** Tells whether the red, green and blue of [a] and [b] each differ by at most [COLOUR_TOLERANCE]. */
    private fun near(a: Int, b: Int): Boolean =
        abs(android.graphics.Color.red(a) - android.graphics.Color.red(b)) <= COLOUR_TOLERANCE &&
            abs(android.graphics.Color.green(a) - android.graphics.Color.green(b)) <= COLOUR_TOLERANCE &&
            abs(android.graphics.Color.blue(a) - android.graphics.Color.blue(b)) <= COLOUR_TOLERANCE

    /** Writes [colour] as `#AARRGGBB`, for failure messages. */
    private fun hex(colour: Int): String = "#%08X".format(colour)

    /** Values, photos and matchers the tests share. */
    private companion object {
        /** How long to wait for the fake source's read to end, in milliseconds. */
        const val LOAD_TIMEOUT_MS = 5_000L

        /** The bottom padding the scroll test gives, in dp: about the floating bar's clearance. */
        const val BOTTOM_PADDING_DP = 120

        /** The largest difference per colour channel still counted as the same colour. */
        const val COLOUR_TOLERANCE = 8

        /** The solid colour of tile 0's stand-in picture: a soft red, far from any theme surface. */
        const val TILE_ZERO_COLOUR = 0xFFD97777.toInt()

        /** The time of the newest sample photo, in seconds since 1970 (October 2026). */
        const val NEWEST_SECONDS = 1_791_800_000L

        /** Matches every photo tile, whatever its place. */
        val isTile = SemanticsMatcher("is a My edits photo tile") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(MY_EDITS_TILE_TAG_PREFIX) == true
        }

        /** Matches every skeleton tile, whatever its place. */
        val isSkeleton = SemanticsMatcher("is a My edits skeleton tile") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(MY_EDITS_SKELETON_TAG_PREFIX) == true
        }

        /**
         * The stand-in for a tile's picture: a solid colour drawn with the tile's modifier. Photo
         * 1 (the newest, tile 0) is [TILE_ZERO_COLOUR]; the others are a soft indigo.
         */
        val standIn: @Composable (Photo, Modifier) -> Unit = { photo, tileModifier ->
            val colour = if (photo.id == 1L) TILE_ZERO_COLOUR else 0xFF5C6BC0.toInt()
            Spacer(modifier = tileModifier.background(Color(colour)))
        }

        /** Returns [count] saved photos, newest first (photo 1 is the newest), one hour apart. */
        fun photos(count: Int): List<Photo> = (1..count).map { i ->
            Photo(
                id = i.toLong(),
                uri = "content://media/external/images/media/$i",
                name = "Kanso_$i.jpg",
                albumId = 1L,
                modifiedSeconds = NEWEST_SECONDS - i * 3_600L,
            )
        }
    }
}
