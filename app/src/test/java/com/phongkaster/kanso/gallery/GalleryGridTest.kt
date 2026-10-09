package com.phongkaster.kanso.gallery

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.screenshots.FAKE_SCHEME
import com.phongkaster.kanso.screenshots.SolidColourFetcherFactory
import com.phongkaster.kanso.ui.gallery.GALLERY_BODY_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_EMPTY_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_GRID_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_LOADING_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_PHOTO_TAG_PREFIX
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryUiState
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.gallery.galleryAlbumTag
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import kotlin.math.abs
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
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
 * Checks the gallery's photo grid once photo access is granted: newest photos first, the album
 * switcher and its selected chip, tapping a photo, staying light with thousands of photos,
 * keeping the scroll position across a reload, the empty state, the loading indicator shown
 * while the photos are read, and the rounded corners of the photo cells.
 *
 * The photos come from a [FakePhotoSource], because a JVM test has no real photo library.
 * Photo access is granted with Robolectric's grantPermissions before the screen is shown, and a
 * [RecordingRegistry] stands in for the system dialog in case anything asks. The screen is the
 * size of a typical phone, so the grid has three columns.
 *
 * The loading tests hold the fake's reads back (see [FakePhotoSource.holdReads]) so the gallery
 * stays in its loading state as long as they need. The loading indicator animates for as long as
 * it is shown, so those tests pause the test clock (`mainClock.autoAdvance = false`) before
 * anything is drawn and move time on by hand: with the clock running by itself, every wait for
 * the screen to settle would wait for an animation that never ends.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class GalleryGridTest {

    /** Starts an empty activity to host the gallery. */
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

    /** The grid lists every photo, newest first, across the three albums. */
    @Test
    fun photos_are_listed_newest_first() {
        val source = threeAlbumSource()
        showGallery(source)

        val expected = source.allPhotos.sortedByDescending { it.modifiedSeconds }
        assertEquals(expected.map { galleryPhotoTag(it.id) }, placedPhotoTagsInOrder())
    }

    /** Choosing an album shows exactly its photos; "All photos" shows them all again. */
    @Test
    fun album_filter_shows_only_that_album_and_all_photos_shows_the_union() {
        val source = threeAlbumSource()
        val viewModel = showGallery(source)
        val albumB = source.albums[1]

        compose.onNodeWithTag(galleryAlbumTag(albumB.id)).performScrollTo().performClick()
        waitForLoad(viewModel) { it.selectedAlbumId == albumB.id }

        val inB = source.allPhotos
            .filter { it.albumId == albumB.id }
            .sortedByDescending { it.modifiedSeconds }
        assertEquals(inB.map { galleryPhotoTag(it.id) }, placedPhotoTagsInOrder())

        compose.onNodeWithTag(galleryAlbumTag(null)).performScrollTo().performClick()
        waitForLoad(viewModel) { it.selectedAlbumId == null }

        val all = source.allPhotos.sortedByDescending { it.modifiedSeconds }
        assertEquals(all.map { galleryPhotoTag(it.id) }, placedPhotoTagsInOrder())
    }

    /**
     * The chip of the album on show is the one marked selected, the state screen readers announce
     * and tests read, and every other chip is marked not selected: "All photos" at first, then
     * album B once it is chosen, then "All photos" again.
     */
    @Test
    fun the_chosen_album_chip_is_the_only_one_selected() {
        val source = threeAlbumSource()
        val viewModel = showGallery(source)
        assertOnlyChipSelected(selectedId = null, albums = source.albums)

        val albumB = source.albums[1]
        compose.onNodeWithTag(galleryAlbumTag(albumB.id)).performScrollTo().performClick()
        waitForLoad(viewModel) { it.selectedAlbumId == albumB.id }
        assertOnlyChipSelected(selectedId = albumB.id, albums = source.albums)

        compose.onNodeWithTag(galleryAlbumTag(null)).performScrollTo().performClick()
        waitForLoad(viewModel) { it.selectedAlbumId == null }
        assertOnlyChipSelected(selectedId = null, albums = source.albums)
    }

    /** Tapping a photo hands the gallery's tool and that photo's URI to onPhotoChosen. */
    @Test
    fun tapping_a_photo_reports_the_tool_and_its_uri() {
        val source = threeAlbumSource()
        val chosen = mutableListOf<Pair<Tool, String>>()
        showGallery(source, tool = Tool.RETOUCH) { tool, uri -> chosen += tool to uri }

        val photos = source.allPhotos.sortedByDescending { it.modifiedSeconds }
        val k = 4
        compose.onNodeWithTag(galleryPhotoTag(photos[k].id)).performClick()
        compose.waitForIdle()

        assertEquals(listOf(Tool.RETOUCH to photos[k].uri), chosen)
    }

    /**
     * With 5,000 photos only the cells near the screen are built, the last photo can be scrolled
     * to, and the photo library is read off the main thread.
     */
    @Test
    fun five_thousand_photos_stay_light_and_load_off_the_main_thread() {
        val photos = manyPhotos(5_000)
        val source = FakePhotoSource(albums = listOf(Album(1L, "Camera")), allPhotos = photos)
        showGallery(source)

        val cells = compose.onAllNodes(isPhotoCell).fetchSemanticsNodes().size
        assertTrue("Expected fewer than 100 photo cells, found $cells", cells in 1 until 100)

        compose.onNodeWithTag(GALLERY_GRID_TAG).performScrollToIndex(4_999)
        compose.waitForIdle()
        compose.onNodeWithTag(galleryPhotoTag(photos.last().id)).assertIsDisplayed()

        val mainThread = Looper.getMainLooper().thread
        assertTrue("The fake was never called", source.callingThreads.isNotEmpty())
        assertTrue(
            "The photo library was read on the main thread",
            source.callingThreads.none { it == mainThread },
        )
    }

    /** After a reload that adds one new photo at the top, the same photo stays in view. */
    @Test
    fun reload_with_a_new_photo_on_top_keeps_the_same_photo_in_view() {
        val photos = manyPhotos(300)
        val source = FakePhotoSource(albums = listOf(Album(1L, "Camera")), allPhotos = photos)
        val viewModel = showGallery(source)

        compose.onNodeWithTag(GALLERY_GRID_TAG).performScrollToIndex(100)
        compose.waitForIdle()
        val watched = galleryPhotoTag(photos[100].id)
        compose.onNodeWithTag(watched).assertIsDisplayed()

        val newest = Photo(
            id = 99_999L,
            uri = "content://com.phongkaster.kanso.test/photo/99999",
            name = "new.jpg",
            albumId = 1L,
            modifiedSeconds = photos.first().modifiedSeconds + 1_000,
        )
        source.allPhotos = listOf(newest) + photos
        compose.runOnIdle { viewModel.refresh() }
        waitForLoad(viewModel) { it.photos.size == photos.size + 1 }

        compose.onNodeWithTag(watched).assertIsDisplayed()
        // The grid did not jump back to the top: the new photo is far above the screen.
        assertEquals(0, compose.onAllNodesWithTag(galleryPhotoTag(newest.id)).fetchSemanticsNodes().size)
    }

    /** With no photos at all, the empty message shows inside the photo area. */
    @Test
    fun no_photos_shows_the_empty_state() {
        showGallery(FakePhotoSource(albums = emptyList(), allPhotos = emptyList()))

        compose.onNodeWithTag(GALLERY_BODY_TAG).assertIsDisplayed()
        compose.onNodeWithTag(GALLERY_EMPTY_TAG).assertIsDisplayed()
        assertEquals(0, compose.onAllNodes(isPhotoCell).fetchSemanticsNodes().size)
    }

    /** A photo library that fails to read shows the empty state instead of crashing. */
    @Test
    fun a_failing_source_shows_the_empty_state() {
        val source = threeAlbumSource().apply { failure = SecurityException("access taken away") }
        showGallery(source)

        compose.onNodeWithTag(GALLERY_EMPTY_TAG).assertIsDisplayed()
    }

    /**
     * While the first read of the library is still running, the gallery shows the loading
     * indicator, with a spoken name, and neither the empty message nor any photo cell; once the
     * read finishes the indicator is gone and the grid shows every photo, newest first.
     */
    @Test
    fun the_loading_indicator_shows_until_the_photos_arrive() {
        val source = threeAlbumSource().apply { holdReads() }
        // Paused before anything is drawn: the indicator animates for as long as it is shown.
        compose.mainClock.autoAdvance = false
        val viewModel = setGallery(source)
        compose.mainClock.advanceTimeBy(FRAME_STEP_MS)

        val indicator = compose.onNodeWithTag(GALLERY_LOADING_TAG)
        indicator.assertIsDisplayed()
        val description = indicator.fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.ContentDescription)
        assertTrue(
            "The loading indicator has no content description: $description",
            !description.isNullOrEmpty() && description.all { it.isNotBlank() },
        )
        assertEquals(listOf(app.getString(R.string.gallery_loading)), description)
        assertEquals(0, compose.onAllNodesWithTag(GALLERY_EMPTY_TAG).fetchSemanticsNodes().size)
        assertEquals(
            0,
            compose.onAllNodesWithText(app.getString(R.string.gallery_empty)).fetchSemanticsNodes().size,
        )
        assertEquals(0, compose.onAllNodes(isPhotoCell).fetchSemanticsNodes().size)

        source.releaseReads()
        compose.waitUntil(LOAD_TIMEOUT_MS) {
            // Lets the main thread run what the background read posted back to it.
            shadowOf(Looper.getMainLooper()).idle()
            // The clock is paused, so each check moves it on by one frame: that frame is when the
            // screen is drawn again with the new state.
            compose.mainClock.advanceTimeByFrame()
            !viewModel.state.value.loading &&
                compose.onAllNodesWithTag(GALLERY_LOADING_TAG).fetchSemanticsNodes().isEmpty()
        }
        // The indicator has left the screen, so nothing animates forever any more: the clock may
        // run by itself again.
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()

        compose.onNodeWithTag(GALLERY_GRID_TAG).assertIsDisplayed()
        val expected = source.allPhotos.sortedByDescending { it.modifiedSeconds }
        assertEquals(expected.map { galleryPhotoTag(it.id) }, placedPhotoTagsInOrder())
        assertEquals(0, compose.onAllNodesWithTag(GALLERY_EMPTY_TAG).fetchSemanticsNodes().size)
    }

    /**
     * A read that starts while photos are already on screen (the gallery reads again each time it
     * comes back to the front) keeps those photos and shows no loading indicator: the indicator
     * is only for a gallery with nothing to show yet.
     */
    @Test
    fun a_reload_keeps_the_photos_and_shows_no_loading_indicator() {
        val source = threeAlbumSource()
        // Paused for the whole test: if the indicator wrongly showed, the test must fail on the
        // assertions below rather than wait forever for its animation.
        compose.mainClock.autoAdvance = false
        val viewModel = setGallery(source)
        compose.waitUntil(LOAD_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.mainClock.advanceTimeByFrame()
            !viewModel.state.value.loading &&
                compose.onAllNodes(isPhotoCell).fetchSemanticsNodes().size == source.allPhotos.size
        }

        source.holdReads()
        compose.runOnIdle { viewModel.refresh() }
        compose.mainClock.advanceTimeBy(FRAME_STEP_MS)

        assertTrue("The new read should still be held", viewModel.state.value.loading)
        assertEquals(0, compose.onAllNodesWithTag(GALLERY_LOADING_TAG).fetchSemanticsNodes().size)
        compose.onNodeWithTag(GALLERY_GRID_TAG).assertIsDisplayed()
        assertEquals(source.allPhotos.size, compose.onAllNodes(isPhotoCell).fetchSemanticsNodes().size)

        source.releaseReads()
        compose.waitUntil(LOAD_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            !viewModel.state.value.loading
        }
    }

    /**
     * The photo cells are rounded tiles, and neighbouring cells do not touch.
     *
     * Each thumbnail is a solid colour ([SolidColourFetcherFactory] makes it from the photo's
     * fake URI), so the screen can be read pixel by pixel: the centre of cell 1 (the newest photo,
     * top left) is the thumbnail's colour, while the pixel 1 px inside the cell's top-left corner
     * is outside the rounded shape, so it shows the background the cell sits on (sampled in the
     * gap between cell 1 and cell 2) and is far from the thumbnail's colour on every channel. A
     * square cell would show the thumbnail there too.
     *
     * The screen is drawn by drawing the activity's content view into a bitmap, as the settings
     * tests do (`settings/ScreenPixels.kt`): Compose's captureToImage times out under Robolectric
     * here. Drawing real pixels needs Robolectric's native graphics, switched on for this test.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun photo_cells_have_rounded_corners() {
        val fetchers = SolidColourFetcherFactory()
        // Coil's image loader is a JVM-wide singleton: this one knows the fake scheme and runs
        // every step on the calling thread, so the thumbnails are ready once the screen is idle.
        Coil.setImageLoader(
            ImageLoader.Builder(app)
                .components { add(fetchers) }
                .dispatcher(Dispatchers.Unconfined)
                .build(),
        )
        try {
            val source = solidColourSource()
            showGallery(source)
            compose.waitUntil(LOAD_TIMEOUT_MS) { fetchers.fetched.get() >= source.allPhotos.size }
            compose.waitForIdle()

            val cellOne = compose.onNodeWithTag(galleryPhotoTag(1L)).fetchSemanticsNode().boundsInRoot
            val cellTwo = compose.onNodeWithTag(galleryPhotoTag(2L)).fetchSemanticsNode().boundsInRoot
            assertTrue(
                "Cell 2 should sit to the right of cell 1 with a gap: $cellOne, $cellTwo",
                cellTwo.left - cellOne.right >= 2f && abs(cellTwo.top - cellOne.top) < 1f,
            )

            val cornerX = ceil(cellOne.left).toInt() + 1
            val cornerY = ceil(cellOne.top).toInt() + 1
            val centreX = cellOne.center.x.toInt()
            val centreY = cellOne.center.y.toInt()
            val gapX = ((cellOne.right + cellTwo.left) / 2f).toInt()
            val (corner, centre, background) = drawScreen { bitmap ->
                Triple(
                    bitmap.getPixel(cornerX, cornerY),
                    bitmap.getPixel(centreX, centreY),
                    bitmap.getPixel(gapX, centreY),
                )
            }

            assertTrue(
                "The centre of cell 1 should show its thumbnail ${hex(CELL_ONE_COLOUR)}, was ${hex(centre)}",
                everyChannelWithin(centre, CELL_ONE_COLOUR, COLOUR_TOLERANCE),
            )
            assertTrue(
                "The corner of cell 1 should not show the thumbnail ${hex(CELL_ONE_COLOUR)} " +
                    "(more than $COLOUR_TOLERANCE apart on every channel), was ${hex(corner)}",
                everyChannelApart(corner, CELL_ONE_COLOUR, COLOUR_TOLERANCE),
            )
            assertTrue(
                "The corner of cell 1 should show the background ${hex(background)}, was ${hex(corner)}",
                everyChannelWithin(corner, background, COLOUR_TOLERANCE),
            )
        } finally {
            Coil.setImageLoader(ImageLoader.Builder(app).build())
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Grants photo access, shows the gallery of [tool] reading from [source], waits until the
     * first read is done, and returns the gallery's ViewModel.
     */
    private fun showGallery(
        source: FakePhotoSource,
        tool: Tool = Tool.EDIT,
        onPhotoChosen: (Tool, String) -> Unit = { _, _ -> },
    ): GalleryViewModel {
        val viewModel = setGallery(source, tool, onPhotoChosen)
        waitForLoad(viewModel) { true }
        return viewModel
    }

    /**
     * Grants photo access and shows the gallery of [tool] reading from [source], without waiting
     * for the read, and returns the gallery's ViewModel. The loading tests call it with the test
     * clock paused.
     */
    private fun setGallery(
        source: FakePhotoSource,
        tool: Tool = Tool.EDIT,
        onPhotoChosen: (Tool, String) -> Unit = { _, _ -> },
    ): GalleryViewModel {
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = RecordingRegistry()
        }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = GalleryViewModel(tool, ViewModelClearedProbe(), source)
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                MaterialTheme {
                    GalleryScreen(
                        viewModel = viewModel,
                        onBack = {},
                        onPhotoChosen = onPhotoChosen,
                    )
                }
            }
        }
        return viewModel
    }

    /**
     * Waits until the gallery has finished reading and its state matches [condition], then until
     * the screen has been drawn with it. The read runs on a background thread, which the Compose
     * test rule does not track by itself, hence the explicit wait.
     */
    private fun waitForLoad(
        viewModel: GalleryViewModel,
        condition: (GalleryUiState) -> Boolean,
    ) {
        compose.waitUntil(LOAD_TIMEOUT_MS) {
            // Lets the main thread run what the background read posted back to it.
            shadowOf(android.os.Looper.getMainLooper()).idle()
            val state = viewModel.state.value
            !state.loading && condition(state)
        }
        compose.waitForIdle()
    }

    /**
     * Returns the test tags of the photo cells laid out on screen, in reading order: top row
     * first, left to right in each row. Cells built ahead of scrolling but not placed are left
     * out.
     */
    private fun placedPhotoTagsInOrder(): List<String> =
        compose.onAllNodes(isPhotoCell).fetchSemanticsNodes()
            .filter { it.layoutInfo.isPlaced }
            .sortedWith(compareBy<SemanticsNode>({ it.boundsInRoot.top }, { it.boundsInRoot.left }))
            .map { it.config[SemanticsProperties.TestTag] }

    /**
     * Checks the album chips: the chip of [selectedId] ("All photos" when null) is marked
     * selected, and the chip of every other album in [albums], and "All photos" when an album is
     * chosen, is marked not selected.
     */
    private fun assertOnlyChipSelected(selectedId: Long?, albums: List<Album>) {
        (listOf<Long?>(null) + albums.map { it.id }).forEach { id ->
            val chip = compose.onNodeWithTag(galleryAlbumTag(id))
            if (id == selectedId) chip.assertIsSelected() else chip.assertIsNotSelected()
        }
    }

    /**
     * Three photos in one album whose fake URIs ask [SolidColourFetcherFactory] for a solid
     * colour each. Photo 1 is the newest, so it fills cell 1 (top left), in [CELL_ONE_COLOUR];
     * photo 2 fills the cell to its right.
     */
    private fun solidColourSource(): FakePhotoSource {
        val colours = listOf(CELL_ONE_COLOUR, CELL_TWO_COLOUR, CELL_THREE_COLOUR)
        val photos = colours.mapIndexed { i, colour ->
            Photo(
                id = (i + 1).toLong(),
                uri = "$FAKE_SCHEME://color/${"%08X".format(colour)}",
                name = "solid_${i + 1}.jpg",
                albumId = 1L,
                modifiedSeconds = 10_000L - i,
            )
        }
        return FakePhotoSource(albums = listOf(Album(1L, "Camera")), allPhotos = photos)
    }

    /**
     * Draws the activity's content view into a fresh bitmap, hands it to [read], then frees it.
     * The Compose root fills the content view from its top-left corner, so a node's
     * `boundsInRoot` are the bitmap's coordinates. Under Robolectric the test thread is the main
     * thread, so drawing here is allowed.
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

    /** Tells whether the red, green and blue of [a] and [b] each differ by at most [tolerance]. */
    private fun everyChannelWithin(a: Int, b: Int, tolerance: Int): Boolean =
        channelDistances(a, b).all { it <= tolerance }

    /** Tells whether the red, green and blue of [a] and [b] each differ by more than [tolerance]. */
    private fun everyChannelApart(a: Int, b: Int, tolerance: Int): Boolean =
        channelDistances(a, b).all { it > tolerance }

    /** Returns how far apart the red, green and blue of [a] and [b] are, channel by channel. */
    private fun channelDistances(a: Int, b: Int): List<Int> = listOf(
        abs(Color.red(a) - Color.red(b)),
        abs(Color.green(a) - Color.green(b)),
        abs(Color.blue(a) - Color.blue(b)),
    )

    /** Writes [colour] as `#AARRGGBB`, for failure messages. */
    private fun hex(colour: Int): String = "#%08X".format(colour)

    /** Values the tests share. */
    companion object {
        /** How long to wait for a read of the fake library, in milliseconds. */
        const val LOAD_TIMEOUT_MS = 5_000L

        /** How far the paused clock is moved on so the screen is drawn again, in milliseconds. */
        private const val FRAME_STEP_MS = 500L

        /** The largest difference per colour channel still counted as the same colour. */
        private const val COLOUR_TOLERANCE = 8

        /** The solid colour of photo 1's thumbnail: a soft red, far from any theme background. */
        private const val CELL_ONE_COLOUR = 0xFFD97777.toInt()

        /** The solid colour of photo 2's thumbnail: a soft indigo. */
        private const val CELL_TWO_COLOUR = 0xFF5C6BC0.toInt()

        /** The solid colour of photo 3's thumbnail: a soft teal. */
        private const val CELL_THREE_COLOUR = 0xFF26A69A.toInt()

        /** Matches every photo cell of the grid, whatever its photo. */
        val isPhotoCell = SemanticsMatcher("is a gallery photo cell") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)
                ?.startsWith(GALLERY_PHOTO_TAG_PREFIX) == true
        }

        /**
         * Three albums of three photos each. Every photo has its own date, and the dates are
         * interleaved across albums so that newest first is not the same as album order.
         */
        fun threeAlbumSource(): FakePhotoSource {
            val albums = listOf(Album(10L, "A"), Album(20L, "B"), Album(30L, "C"))
            val photos = (0 until 9).map { i ->
                val album = albums[i % 3]
                Photo(
                    id = (i + 1).toLong(),
                    uri = "content://com.phongkaster.kanso.test/photo/${i + 1}",
                    name = "photo_${i + 1}.jpg",
                    albumId = album.id,
                    // Dates out of id order: 1, 8, 6, 4, 2, 9, 7, 5, 3 (times 1,000 seconds).
                    modifiedSeconds = 1_000L * ((i * 7) % 9 + 1),
                )
            }
            return FakePhotoSource(albums = albums, allPhotos = photos.shuffled(java.util.Random(4)))
        }

        /** [count] photos in one album, newest first, with ids 1 to [count]. */
        fun manyPhotos(count: Int): List<Photo> = (1..count).map { i ->
            Photo(
                id = i.toLong(),
                uri = "content://com.phongkaster.kanso.test/photo/$i",
                name = "photo_$i.jpg",
                albumId = 1L,
                modifiedSeconds = 10_000_000L - i,
            )
        }
    }
}
