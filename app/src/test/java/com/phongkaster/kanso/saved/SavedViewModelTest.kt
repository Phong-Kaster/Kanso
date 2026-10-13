package com.phongkaster.kanso.saved

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.saved.SavedPhoto
import com.phongkaster.kanso.ui.saved.SavedUiState
import com.phongkaster.kanso.ui.saved.SavedViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks the Saved screen's ViewModel on its own, with no screen drawn (DoD 4): its state starts
 * as loading, becomes the picture when the loader returns one, becomes unreadable when the loader
 * returns nothing, and a second request for the same size does not load again. Since Run 7 it
 * also checks the viewer flag, which tells the screen after a save from the photo viewer.
 *
 * It runs under Robolectric because the [FakePhotoLoader] it is given needs an Android Context and
 * a Bitmap. Robolectric runs the test on Android's main thread, the thread the ViewModel's
 * coroutines run on, so a load whose loader answers at once has finished when `load` returns; the
 * main thread's queue is still run afterwards, so nothing waiting there is missed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SavedViewModelTest {

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

    /** Before the screen asks for the photo, the state names the photo and is still loading. */
    @Test
    fun the_state_starts_as_loading() {
        val viewModel = savedViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)))

        assertEquals(SavedUiState(photoUri = PHOTO_URI, photo = SavedPhoto.Loading), viewModel.state.value)
    }

    /** When the loader returns a picture, the state holds that very picture, ready to draw. */
    @Test
    fun the_state_becomes_ready_with_the_loaded_picture() {
        val bitmap = ToolFixtures.gradient(600, 400)
        val viewModel = savedViewModel(FakePhotoLoader(app, bitmap))

        viewModel.load(MAX_EDGE_PX)
        runMainThread()

        val state = viewModel.state.value
        assertEquals(PHOTO_URI, state.photoUri)
        val photo = state.photo
        assertTrue("The photo is not ready: $photo", photo is SavedPhoto.Ready)
        assertSame(bitmap, (photo as SavedPhoto.Ready).bitmap)
    }

    /** When the loader returns nothing (a missing or broken photo), the state says unreadable. */
    @Test
    fun the_state_becomes_unreadable_when_the_loader_returns_nothing() {
        val viewModel = savedViewModel(FakePhotoLoader(app, null))

        viewModel.load(MAX_EDGE_PX)
        runMainThread()

        assertEquals(SavedUiState(photoUri = PHOTO_URI, photo = SavedPhoto.Unreadable), viewModel.state.value)
    }

    /** Asking twice for the same size, as the screen does after a rotation, loads only once. */
    @Test
    fun a_second_request_for_the_same_size_does_not_load_again() {
        val loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400))
        val viewModel = savedViewModel(loader)

        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        viewModel.load(MAX_EDGE_PX)
        runMainThread()

        assertEquals(listOf(MAX_EDGE_PX), loader.requestedEdges.toList())
        assertTrue(viewModel.state.value.photo is SavedPhoto.Ready)
    }

    /** Asking for a different size does load again: the guard is per size, not "only once ever". */
    @Test
    fun a_request_for_a_new_size_loads_again() {
        val loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400))
        val viewModel = savedViewModel(loader)

        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        viewModel.load(2 * MAX_EDGE_PX)
        runMainThread()

        assertEquals(listOf(MAX_EDGE_PX, 2 * MAX_EDGE_PX), loader.requestedEdges.toList())
    }

    /**
     * Run 7, DoD 9 and 14: a ViewModel built without the viewer flag is the screen after a save,
     * and says so in its state from the start to after the photo is decoded.
     */
    @Test
    fun without_the_viewer_flag_the_state_is_the_screen_after_a_save() {
        val viewModel = savedViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)))

        assertFalse("The state says viewer before the load", viewModel.state.value.viewer)
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        assertFalse("The state says viewer after the load", viewModel.state.value.viewer)
        assertTrue(viewModel.state.value.photo is SavedPhoto.Ready)
    }

    /**
     * Run 7, DoD 9: a ViewModel built with the viewer flag (a photo opened from My edits) carries
     * it in its state, keeps it once the photo is decoded, and decodes the photo as usual.
     */
    @Test
    fun the_viewer_flag_reaches_the_state_and_stays_after_the_load() {
        val bitmap = ToolFixtures.gradient(600, 400)
        val viewModel = savedViewModel(FakePhotoLoader(app, bitmap), viewer = true)

        assertEquals(
            SavedUiState(photoUri = PHOTO_URI, photo = SavedPhoto.Loading, viewer = true),
            viewModel.state.value,
        )
        viewModel.load(MAX_EDGE_PX)
        runMainThread()

        val state = viewModel.state.value
        assertTrue("The viewer flag was lost by the load", state.viewer)
        assertSame(bitmap, (state.photo as SavedPhoto.Ready).bitmap)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the ViewModel for [PHOTO_URI] with [loader], the way the app's Koin recipe does but
     * by hand, with a probe nobody reads; [viewer] is passed only when a test asks for it, so the
     * other tests build it exactly as the Saved key does, with the constructor's default.
     */
    private fun savedViewModel(loader: FakePhotoLoader, viewer: Boolean? = null): SavedViewModel =
        if (viewer == null) {
            SavedViewModel(photoUri = PHOTO_URI, loader = loader, probe = ViewModelClearedProbe())
        } else {
            SavedViewModel(photoUri = PHOTO_URI, loader = loader, probe = ViewModelClearedProbe(), viewer = viewer)
        }

    /** Runs everything waiting in the main thread's queue, where the ViewModel's load ends. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Values shared by the tests. */
    private companion object {
        /** A saved photo as Android 10 and later return it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** The size the screen asks for on the test phone: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3
    }
}
