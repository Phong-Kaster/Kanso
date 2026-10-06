package com.phongkaster.kanso.gallery

import android.Manifest
import android.app.Application
import android.os.Looper
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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.gallery.GALLERY_BODY_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_EMPTY_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_GRID_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_PHOTO_TAG_PREFIX
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryUiState
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.gallery.galleryAlbumTag
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks the gallery's photo grid once photo access is granted: newest photos first, the album
 * switcher, tapping a photo, staying light with thousands of photos, keeping the scroll position
 * across a reload, and the empty state.
 *
 * The photos come from a [FakePhotoSource], because a JVM test has no real photo library.
 * Photo access is granted with Robolectric's grantPermissions before the screen is shown, and a
 * [RecordingRegistry] stands in for the system dialog in case anything asks. The screen is the
 * size of a typical phone, so the grid has three columns.
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
        waitForLoad(viewModel) { true }
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

    /** Values the tests share. */
    companion object {
        /** How long to wait for a read of the fake library, in milliseconds. */
        const val LOAD_TIMEOUT_MS = 5_000L

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

/**
 * A photo library for tests: it holds [albums] and [allPhotos], answers like the real source
 * (an album's photos, newest first), records the thread of every call in [callingThreads], and
 * throws [failure] instead of answering when one is set.
 *
 * Its fields are @Volatile because the gallery reads them on a background thread while the test
 * sets them on the main thread.
 */
class FakePhotoSource(
    albums: List<Album>,
    allPhotos: List<Photo>,
) : PhotoSource {

    /** The albums the fake answers with. */
    @Volatile
    var albums: List<Album> = albums

    /** Every photo of every album, in any order; the fake sorts them when asked. */
    @Volatile
    var allPhotos: List<Photo> = allPhotos

    /** When set, every call throws it instead of answering. */
    @Volatile
    var failure: Exception? = null

    /** The thread each call ran on, in call order. */
    val callingThreads: MutableList<Thread> = CopyOnWriteArrayList()

    /** Returns [albums], after recording the calling thread. */
    override suspend fun albums(): List<Album> {
        callingThreads += Thread.currentThread()
        failure?.let { throw it }
        return albums
    }

    /** Returns the photos of [albumId] (all when null), newest first, after recording the thread. */
    override suspend fun photos(albumId: Long?): List<Photo> {
        callingThreads += Thread.currentThread()
        failure?.let { throw it }
        return allPhotos
            .filter { albumId == null || it.albumId == albumId }
            .sortedByDescending { it.modifiedSeconds }
    }
}
