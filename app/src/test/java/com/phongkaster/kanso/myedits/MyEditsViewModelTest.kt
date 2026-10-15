package com.phongkaster.kanso.myedits

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.ui.myedits.MyEditsUiState
import com.phongkaster.kanso.ui.myedits.MyEditsViewModel
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks the My edits ViewModel on its own, with no screen drawn (DoD 9): it starts as loading,
 * lists the saved photos newest first, picks up a photo saved since the last read, and shows the
 * empty state (never an error) when the source has nothing or fails.
 *
 * It runs under Robolectric, on Android's main thread, the thread the ViewModel's coroutines run
 * on: a read whose fake source answers at once has finished when `refresh` returns, and the main
 * thread's queue is still run afterwards so nothing waiting there is missed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MyEditsViewModelTest {

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Before the first read the state is loading, with no photos. */
    @Test
    fun the_state_starts_as_loading_with_no_photos() {
        val viewModel = myEditsViewModel(FakeSavedPhotoSource(photos(3)))

        assertEquals(MyEditsUiState(photos = emptyList(), loading = true), viewModel.state.value)
    }

    /** The photos are listed newest first, whatever order the source gives them in. */
    @Test
    fun photos_are_listed_newest_first() {
        val photos = photos(5)
        // Out of order on purpose: the oldest first, the newest in the middle.
        val given = listOf(photos[4], photos[2], photos[0], photos[3], photos[1])
        val viewModel = myEditsViewModel(FakeSavedPhotoSource(given))

        viewModel.refresh()
        runMainThread()

        val state = viewModel.state.value
        assertEquals(false, state.loading)
        assertEquals(photos.sortedByDescending { it.modifiedSeconds }.map { it.id }, state.photos.map { it.id })
    }

    /** A photo saved after the first read is listed, first, by the next refresh. */
    @Test
    fun refresh_picks_up_a_newly_saved_photo() {
        val source = FakeSavedPhotoSource(photos(3))
        val viewModel = myEditsViewModel(source)
        viewModel.refresh()
        runMainThread()
        assertEquals(3, viewModel.state.value.photos.size)

        val saved = photo(id = 99L, modifiedSeconds = NEWEST_SECONDS + 600L)
        source.photos = source.photos + saved
        viewModel.refresh()
        runMainThread()

        val state = viewModel.state.value
        assertEquals(4, state.photos.size)
        assertEquals(saved, state.photos.first())
        assertEquals(2, source.reads.get())
    }

    /** While a second read runs, the photos already shown stay, marked as loading. */
    @Test
    fun photos_stay_on_screen_while_a_new_read_runs() {
        val source = FakeSavedPhotoSource(photos(3))
        val viewModel = myEditsViewModel(source)
        viewModel.refresh()
        runMainThread()

        source.holdReads()
        viewModel.refresh()
        runMainThread()
        val during = viewModel.state.value
        assertTrue("The second read should still be running", during.loading)
        assertEquals(3, during.photos.size)

        source.releaseReads()
        runMainThread()
        assertEquals(false, viewModel.state.value.loading)
    }

    /** A source with no saved photo gives the empty state: no photos, not loading. */
    @Test
    fun an_empty_source_gives_the_empty_state() {
        val viewModel = myEditsViewModel(FakeSavedPhotoSource(emptyList()))

        viewModel.refresh()
        runMainThread()

        assertEquals(MyEditsUiState(photos = emptyList(), loading = false), viewModel.state.value)
    }

    /** A source that throws gives the empty state too, never a crash. */
    @Test
    fun a_source_that_throws_gives_the_empty_state() {
        val source = FakeSavedPhotoSource(photos(3)).apply { failure = IOException("no library") }
        val viewModel = myEditsViewModel(source)

        viewModel.refresh()
        runMainThread()

        assertEquals(MyEditsUiState(photos = emptyList(), loading = false), viewModel.state.value)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Builds the ViewModel with [source], the way the app's Koin recipe does but by hand. */
    private fun myEditsViewModel(source: FakeSavedPhotoSource): MyEditsViewModel =
        MyEditsViewModel(savedPhotos = source, probe = ViewModelClearedProbe())

    /** Runs everything waiting in the main thread's queue, where the ViewModel's read ends. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Values and photos shared by the tests. */
    private companion object {
        /** The time of the newest sample photo, in seconds since 1970 (October 2026). */
        const val NEWEST_SECONDS = 1_791_800_000L

        /** Returns [count] saved photos, newest first, one hour apart. */
        fun photos(count: Int): List<Photo> =
            (1..count).map { i -> photo(id = i.toLong(), modifiedSeconds = NEWEST_SECONDS - i * 3_600L) }

        /** Returns one saved photo with [id], changed at [modifiedSeconds]. */
        fun photo(id: Long, modifiedSeconds: Long): Photo = Photo(
            id = id,
            uri = "content://media/external/images/media/$id",
            name = "Kanso_$id.jpg",
            albumId = 1L,
            modifiedSeconds = modifiedSeconds,
        )
    }
}
