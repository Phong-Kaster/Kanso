package com.phongkaster.kanso.home

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.myedits.FakeSavedPhotoSource
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.home.HomeUiState
import com.phongkaster.kanso.ui.home.HomeViewModel
import java.io.IOException
import java.lang.reflect.Modifier
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks Home's state (DoD 4 and 7): the one snapshot HomeViewModel hands the screen holds the
 * tools of the hero carousel (the four AI tools) and the tools of the row of round shortcuts (all
 * five), each in the order Home draws them, nothing for the camera, and the newest saved photos
 * for the "Recent edits" strip, newest first and at most ten. A new read keeps the photos already
 * loaded on screen until it ends, so the strip never flashes its placeholders on a return to Home.
 *
 * It runs under Robolectric, on Android's main thread, the thread the ViewModel's coroutines run
 * on: a read whose fake source answers at once has finished once the main thread's queue has run.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class HomeViewModelTest {

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * DoD 4: the carousel's banners are Remove background, Enhance, Beautify and Artistic, in that
     * order, and the shortcuts are Edit, Remove background, Enhance, Beautify and Artistic, in that
     * order, which is the order of the `Tool` enum itself.
     */
    @Test
    fun home_state_holds_the_carousel_tools_and_the_five_shortcuts_in_order() {
        val state = homeViewModel(FakeSavedPhotoSource()).state.value

        assertEquals(
            listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.BEAUTIFY, Tool.ARTISTIC),
            state.carouselTools,
        )
        assertEquals(
            listOf(Tool.EDIT, Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.BEAUTIFY, Tool.ARTISTIC),
            state.shortcutTools,
        )
        assertEquals(Tool.entries.toList(), state.shortcutTools)
    }

    /**
     * Nothing in Home's state is for the camera: the shortcuts hold each of the five editing tools
     * exactly once (so no camera entry), the carousel holds only tools that also have a shortcut,
     * and the state has no field other than the two lists of tools and the recent photos with
     * their loading flag (so no hidden camera flag either).
     */
    @Test
    fun home_state_has_nothing_for_the_camera() {
        val state = homeViewModel(FakeSavedPhotoSource()).state.value
        val shortcuts = state.shortcutTools

        assertEquals("A tool has two shortcuts: $shortcuts", shortcuts.toSet().size, shortcuts.size)
        assertEquals(Tool.entries.toSet(), shortcuts.toSet())
        assertEquals(5, shortcuts.size)
        assertTrue(
            "Every banner's tool must also have a shortcut: ${state.carouselTools}",
            shortcuts.containsAll(state.carouselTools),
        )

        // The fields the compiled class declares, read with Java reflection. Static fields are
        // left out: the Compose compiler adds a static `$stable` marker to classes it checks.
        val fields = HomeUiState::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()
        assertEquals(setOf("carouselTools", "shortcutTools", "recentPhotos", "recentLoading"), fields)
        assertTrue(
            "HomeUiState has a field for the camera: $fields",
            fields.none { it.contains("camera", ignoreCase = true) },
        )
    }

    /**
     * Camera is not a tool any more (R3, assumption A-010): the `Tool` enum holds exactly the five
     * editing tools, in the order Home's shortcuts show them, and no entry is named
     * after the camera. The `Camera` navigation key is what stands for it.
     */
    @Test
    fun camera_is_not_a_tool() {
        assertEquals(5, Tool.entries.size)
        assertEquals(
            listOf(Tool.EDIT, Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.BEAUTIFY, Tool.ARTISTIC),
            Tool.entries,
        )
        assertTrue(
            "A tool is named after the camera: ${Tool.entries}",
            Tool.entries.none { it.name.contains("camera", ignoreCase = true) },
        )
    }

    /** Before the first read the strip is loading, with no photos, and nothing has been read. */
    @Test
    fun the_strip_starts_as_loading_with_no_photos() {
        val source = FakeSavedPhotoSource(photos(3))
        val state = homeViewModel(source).state.value

        assertEquals(true, state.recentLoading)
        assertEquals(emptyList<Photo>(), state.recentPhotos)
        assertEquals(0, source.reads.get())
    }

    /**
     * DoD 7 (a): with twelve photos given out of order, a read lists the newest ten, newest first,
     * and the strip is no longer loading.
     */
    @Test
    fun a_read_lists_the_newest_ten_newest_first() {
        val photos = photos(12)
        val viewModel = homeViewModel(FakeSavedPhotoSource(photos.shuffled(java.util.Random(7))))

        viewModel.refresh()
        runMainThread()

        val state = viewModel.state.value
        assertEquals(false, state.recentLoading)
        assertEquals(HomeViewModel.RECENT_LIMIT, state.recentPhotos.size)
        assertEquals(10, HomeViewModel.RECENT_LIMIT)
        assertEquals(photos.take(10).map { it.id }, state.recentPhotos.map { it.id })
    }

    /**
     * While a second read runs, the photos already loaded stay in the state (only the loading flag
     * is set), so the strip keeps its cards; once the read ends, a photo saved in between is first.
     */
    @Test
    fun refresh_keeps_the_loaded_photos_while_it_reads_again() {
        val source = FakeSavedPhotoSource(photos(3))
        val viewModel = homeViewModel(source)
        viewModel.refresh()
        runMainThread()
        val loaded = viewModel.state.value.recentPhotos
        assertEquals(3, loaded.size)

        val saved = photo(id = 99L, modifiedSeconds = NEWEST_SECONDS + 600L)
        source.photos = source.photos + saved
        source.holdReads()
        viewModel.refresh()
        runMainThread()

        val during = viewModel.state.value
        assertEquals(true, during.recentLoading)
        assertEquals("The loaded photos must stay while the new read runs", loaded, during.recentPhotos)

        source.releaseReads()
        runMainThread()

        val after = viewModel.state.value
        assertEquals(false, after.recentLoading)
        assertEquals(saved, after.recentPhotos.first())
        assertEquals(4, after.recentPhotos.size)
        assertEquals(2, source.reads.get())
    }

    /** A source with nothing, or one that fails, gives the empty strip: no photos, not loading. */
    @Test
    fun an_empty_or_failing_source_gives_the_empty_strip() {
        val empty = homeViewModel(FakeSavedPhotoSource())
        empty.refresh()
        runMainThread()
        assertEquals(emptyList<Photo>(), empty.state.value.recentPhotos)
        assertEquals(false, empty.state.value.recentLoading)

        val failing = homeViewModel(FakeSavedPhotoSource(photos(2)).apply { failure = IOException("unreadable") })
        failing.refresh()
        runMainThread()
        assertEquals(emptyList<Photo>(), failing.state.value.recentPhotos)
        assertEquals(false, failing.state.value.recentLoading)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Builds Home's ViewModel over [source], in the constructor's order: probe, then the source. */
    private fun homeViewModel(source: FakeSavedPhotoSource): HomeViewModel =
        HomeViewModel(ViewModelClearedProbe(), source)

    /** Runs everything waiting on the main thread's queue, where the ViewModel's reads finish. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Values and photos the tests share. */
    private companion object {
        /** The time of the newest sample photo, in seconds since 1970 (October 2026). */
        const val NEWEST_SECONDS = 1_791_800_000L

        /** Returns [count] saved photos, newest first (photo 1 is the newest), one hour apart. */
        fun photos(count: Int): List<Photo> = (1..count).map { i ->
            photo(id = i.toLong(), modifiedSeconds = NEWEST_SECONDS - i * 3_600L)
        }

        /** Returns one saved photo with [id], saved at [modifiedSeconds]. */
        fun photo(id: Long, modifiedSeconds: Long): Photo = Photo(
            id = id,
            uri = "content://media/external/images/media/$id",
            name = "Kanso_$id.jpg",
            albumId = 1L,
            modifiedSeconds = modifiedSeconds,
        )
    }
}
