package com.phongkaster.kanso.result

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.RenderedPhotoStore
import com.phongkaster.kanso.data.photo.SaveResult
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.save.FakePhotoSaver
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.result.ResultPhoto
import com.phongkaster.kanso.ui.result.ResultUiState
import com.phongkaster.kanso.ui.result.ResultViewModel
import kotlinx.coroutines.CompletableDeferred
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
 * Checks the Result screen's ViewModel on its own, with no screen drawn, in its two uses.
 *
 * Right after Done it shows the picture Done left in the hand-off store ([RenderedPhotoStore]),
 * or "Nothing to show" ([ResultPhoto.Empty]) when the store is empty, and decodes nothing. Save
 * writes that picture through the saver exactly once: the state goes Idle, Saving, Saved; a tap
 * while saving or once saved is ignored; a failure gives Failed and can be retried; a refused
 * storage permission gives PermissionDenied.
 *
 * As the photo viewer (a photo opened from My edits) it starts as loading, becomes the picture
 * when the loader returns one or unreadable when it returns nothing, decodes once per size, and
 * never saves.
 *
 * Run 9: rewritten from the old SavedViewModelTest, because the Result screen replaced the Saved
 * screen: the picture now comes from the store, and Save moved here from the tool screen.
 *
 * It runs under Robolectric because the fakes need an Android Context and a Bitmap. Robolectric
 * runs the test on Android's main thread, the thread the ViewModel's coroutines run on, so work
 * that does not wait has finished when the call returns; the main thread's queue is still run
 * afterwards (see [runMainThread]), so a coroutine resumed later is not missed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ResultViewModelTest {

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

    // --- Right after Done: the picture --------------------------------------------------------

    /**
     * The picture Done put in the store is ready at once, the very same bitmap, with no Uri, not
     * the viewer, and nothing saved yet.
     */
    @Test
    fun the_rendered_picture_from_the_store_is_ready_at_once() {
        val rendered = ToolFixtures.gradient(600, 400)
        val viewModel = resultViewModel(store = storeWith(rendered))

        val state = viewModel.state.value
        assertEquals(null, state.photoUri)
        assertFalse("The state says viewer", state.viewer)
        assertEquals(ResultUiState.SaveState.Idle, state.save)
        assertTrue("The photo is not ready: ${state.photo}", state.photo is ResultPhoto.Ready)
        assertSame(rendered, (state.photo as ResultPhoto.Ready).bitmap)
    }

    /** An empty store (the app was restarted) gives "Nothing to show". */
    @Test
    fun an_empty_store_gives_nothing_to_show() {
        val viewModel = resultViewModel(store = RenderedPhotoStore())

        assertEquals(ResultUiState(photo = ResultPhoto.Empty), viewModel.state.value)
    }

    /** Right after Done the picture is already in memory, so asking to load decodes nothing. */
    @Test
    fun right_after_done_load_decodes_nothing() {
        val loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400))
        val rendered = ToolFixtures.gradient(300, 200)
        val viewModel = resultViewModel(store = storeWith(rendered), loader = loader)

        viewModel.load(MAX_EDGE_PX)
        runMainThread()

        assertTrue("The loader was asked for ${loader.requestedEdges}", loader.requestedEdges.isEmpty())
        assertSame(rendered, (viewModel.state.value.photo as ResultPhoto.Ready).bitmap)
    }

    // --- Right after Done: Save ---------------------------------------------------------------

    /**
     * Save writes the rendered picture through the saver: the state is Saving while the saver
     * works, and Saved once it answers; the saver was given that very bitmap, once.
     */
    @Test
    fun save_goes_from_saving_to_saved_and_writes_the_rendered_picture_once() {
        val rendered = ToolFixtures.gradient(600, 400)
        val gate = CompletableDeferred<Unit>()
        val saver = FakePhotoSaver(gate = gate)
        val viewModel = resultViewModel(store = storeWith(rendered), saver = saver)

        viewModel.save()
        runMainThread()
        assertEquals(ResultUiState.SaveState.Saving, viewModel.state.value.save)

        gate.complete(Unit)
        runMainThread()

        assertEquals(ResultUiState.SaveState.Saved(keptOriginalOrientation = false), viewModel.state.value.save)
        assertEquals(1, saver.bitmapCalls.size)
        assertSame(rendered, saver.bitmapCalls.single())
        assertTrue("The Uri save was called: ${saver.calls}", saver.calls.isEmpty())
    }

    /**
     * Run 9 (review): an untouched photo is saved by copying its own file, as Save always did, not
     * by re-encoding the screen-sized picture Done handed over.
     */
    @Test
    fun an_untouched_photo_is_saved_from_its_own_file() {
        val store = RenderedPhotoStore().apply { put(ToolFixtures.gradient(600, 400), "content://photos/9") }
        val saver = FakePhotoSaver()
        val viewModel = resultViewModel(store = store, saver = saver)

        viewModel.save()
        runMainThread()

        assertEquals(listOf("content://photos/9"), saver.calls.toList())
        assertTrue(saver.bitmapCalls.isEmpty())
    }

    /** Run 9 (review): leaving Result frees the full-size picture the store was holding for it. */
    @Test
    fun clearing_the_view_model_empties_the_store() {
        val store = storeWith(ToolFixtures.gradient(600, 400))
        val viewModel = resultViewModel(store = store)

        // onCleared is protected; ViewModelStore.clear() is what the navigation entry calls.
        val owner = androidx.lifecycle.ViewModelStore().also { it.put("k", viewModel) }
        owner.clear()

        assertEquals(null, store.peek())
    }

    /** A second tap while the first save is still running is ignored: one tap, one file. */
    @Test
    fun a_second_save_while_saving_is_ignored() {
        val gate = CompletableDeferred<Unit>()
        val saver = FakePhotoSaver(gate = gate)
        val viewModel = resultViewModel(store = storeWith(ToolFixtures.gradient(600, 400)), saver = saver)

        viewModel.save()
        runMainThread()
        viewModel.save()
        runMainThread()
        gate.complete(Unit)
        runMainThread()

        assertEquals(1, saver.bitmapCalls.size)
        assertTrue(viewModel.state.value.save is ResultUiState.SaveState.Saved)
    }

    /** Once the picture is saved, Save does nothing more: it is never written twice. */
    @Test
    fun save_after_saved_is_ignored() {
        val saver = FakePhotoSaver()
        val viewModel = resultViewModel(store = storeWith(ToolFixtures.gradient(600, 400)), saver = saver)

        viewModel.save()
        runMainThread()
        viewModel.save()
        runMainThread()

        assertEquals(1, saver.bitmapCalls.size)
        assertTrue(viewModel.state.value.save is ResultUiState.SaveState.Saved)
    }

    /** A saver that had to keep the photo's rotation tag says so, and the state carries it. */
    @Test
    fun a_save_that_kept_the_original_orientation_says_so() {
        val saver = FakePhotoSaver(result = SaveResult.Saved(SAVED_URI, keptOriginalOrientation = true))
        val viewModel = resultViewModel(store = storeWith(ToolFixtures.gradient(600, 400)), saver = saver)

        viewModel.save()
        runMainThread()

        assertEquals(ResultUiState.SaveState.Saved(keptOriginalOrientation = true), viewModel.state.value.save)
    }

    /** A failed save gives Failed, and Save can be tapped again, which this time succeeds. */
    @Test
    fun a_failed_save_gives_failed_and_can_be_retried() {
        val saver = FakePhotoSaver(result = SaveResult.Failed)
        val viewModel = resultViewModel(store = storeWith(ToolFixtures.gradient(600, 400)), saver = saver)

        viewModel.save()
        runMainThread()
        assertEquals(ResultUiState.SaveState.Failed, viewModel.state.value.save)

        saver.result = SaveResult.Saved(SAVED_URI)
        viewModel.save()
        runMainThread()

        assertEquals(2, saver.bitmapCalls.size)
        assertEquals(ResultUiState.SaveState.Saved(keptOriginalOrientation = false), viewModel.state.value.save)
    }

    /** With nothing to show there is nothing to save: the saver is never called. */
    @Test
    fun save_with_an_empty_store_writes_nothing() {
        val saver = FakePhotoSaver()
        val viewModel = resultViewModel(store = RenderedPhotoStore(), saver = saver)

        viewModel.save()
        runMainThread()

        assertTrue(saver.bitmapCalls.isEmpty())
        assertEquals(ResultUiState.SaveState.Idle, viewModel.state.value.save)
    }

    /** The ViewModel passes on the saver's answer to "must Save ask for the storage permission?". */
    @Test
    fun needs_write_permission_follows_the_saver() {
        val saver = FakePhotoSaver(needsPermission = true)
        val viewModel = resultViewModel(store = storeWith(ToolFixtures.gradient(600, 400)), saver = saver)

        assertTrue(viewModel.needsWritePermission())
        saver.needsPermission = false
        assertFalse(viewModel.needsWritePermission())
    }

    /**
     * A refused storage permission gives PermissionDenied and writes nothing; Save can then be
     * tapped again, and once granted the picture is saved.
     */
    @Test
    fun a_refused_permission_gives_permission_denied_and_save_can_be_tried_again() {
        val saver = FakePhotoSaver(needsPermission = true)
        val viewModel = resultViewModel(store = storeWith(ToolFixtures.gradient(600, 400)), saver = saver)

        viewModel.onWritePermissionDenied()
        assertEquals(ResultUiState.SaveState.PermissionDenied, viewModel.state.value.save)
        assertTrue(saver.bitmapCalls.isEmpty())

        viewModel.save()
        runMainThread()

        assertEquals(1, saver.bitmapCalls.size)
        assertTrue(viewModel.state.value.save is ResultUiState.SaveState.Saved)
    }

    /** A refusal arriving after the picture was saved leaves it saved. */
    @Test
    fun a_refused_permission_after_a_save_leaves_it_saved() {
        val viewModel = resultViewModel(store = storeWith(ToolFixtures.gradient(600, 400)))

        viewModel.save()
        runMainThread()
        viewModel.onWritePermissionDenied()

        assertTrue(viewModel.state.value.save is ResultUiState.SaveState.Saved)
    }

    // --- The photo viewer ---------------------------------------------------------------------

    /**
     * Run 7, DoD 9: a ViewModel built as the viewer (a photo opened from My edits) names the photo,
     * says viewer and is still loading before the screen asks for the photo.
     */
    @Test
    fun the_viewer_starts_as_loading() {
        val viewModel = viewerViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)))

        assertEquals(
            ResultUiState(photoUri = PHOTO_URI, photo = ResultPhoto.Loading, viewer = true),
            viewModel.state.value,
        )
    }

    /** In the viewer, when the loader returns a picture, the state holds that very picture and stays the viewer. */
    @Test
    fun the_viewer_becomes_ready_with_the_loaded_picture() {
        val bitmap = ToolFixtures.gradient(600, 400)
        val viewModel = viewerViewModel(FakePhotoLoader(app, bitmap))

        viewModel.load(MAX_EDGE_PX)
        runMainThread()

        val state = viewModel.state.value
        assertEquals(PHOTO_URI, state.photoUri)
        assertTrue("The viewer flag was lost by the load", state.viewer)
        assertTrue("The photo is not ready: ${state.photo}", state.photo is ResultPhoto.Ready)
        assertSame(bitmap, (state.photo as ResultPhoto.Ready).bitmap)
    }

    /** In the viewer, when the loader returns nothing (a missing or broken photo), the state says unreadable. */
    @Test
    fun the_viewer_becomes_unreadable_when_the_loader_returns_nothing() {
        val viewModel = viewerViewModel(FakePhotoLoader(app, null))

        viewModel.load(MAX_EDGE_PX)
        runMainThread()

        assertEquals(
            ResultUiState(photoUri = PHOTO_URI, photo = ResultPhoto.Unreadable, viewer = true),
            viewModel.state.value,
        )
    }

    /** Asking twice for the same size, as the screen does after a rotation, loads only once. */
    @Test
    fun a_second_request_for_the_same_size_does_not_load_again() {
        val loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400))
        val viewModel = viewerViewModel(loader)

        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        viewModel.load(MAX_EDGE_PX)
        runMainThread()

        assertEquals(listOf(MAX_EDGE_PX), loader.requestedEdges.toList())
        assertTrue(viewModel.state.value.photo is ResultPhoto.Ready)
    }

    /** Asking for a different size does load again: the guard is per size, not "only once ever". */
    @Test
    fun a_request_for_a_new_size_loads_again() {
        val loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400))
        val viewModel = viewerViewModel(loader)

        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        viewModel.load(2 * MAX_EDGE_PX)
        runMainThread()

        assertEquals(listOf(MAX_EDGE_PX, 2 * MAX_EDGE_PX), loader.requestedEdges.toList())
    }

    /**
     * The viewer's photo is already in the gallery, so Save does nothing there, and the viewer
     * ignores the hand-off store even when it holds a picture.
     */
    @Test
    fun the_viewer_never_saves_and_ignores_the_store() {
        val saver = FakePhotoSaver()
        val bitmap = ToolFixtures.gradient(600, 400)
        val viewModel = viewerViewModel(
            FakePhotoLoader(app, bitmap),
            store = storeWith(ToolFixtures.gradient(300, 200)),
            saver = saver,
        )

        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        viewModel.save()
        runMainThread()

        assertTrue(saver.bitmapCalls.isEmpty())
        assertTrue(saver.calls.isEmpty())
        assertEquals(ResultUiState.SaveState.Idle, viewModel.state.value.save)
        assertSame(bitmap, (viewModel.state.value.photo as ResultPhoto.Ready).bitmap)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** A hand-off store holding [bitmap], as Done leaves it. */
    private fun storeWith(bitmap: Bitmap): RenderedPhotoStore = RenderedPhotoStore().apply { put(bitmap) }

    /**
     * Builds the ViewModel right after Done (no Uri, not the viewer), the way the app's Koin recipe
     * does but by hand, with a probe nobody reads.
     */
    private fun resultViewModel(
        store: RenderedPhotoStore,
        loader: FakePhotoLoader = FakePhotoLoader(app, null),
        saver: FakePhotoSaver = FakePhotoSaver(),
    ): ResultViewModel = ResultViewModel(
        photoUri = null,
        viewer = false,
        store = store,
        loader = loader,
        saver = saver,
        probe = ViewModelClearedProbe(),
    )

    /** Builds the ViewModel as the photo viewer for [PHOTO_URI], decoding with [loader]. */
    private fun viewerViewModel(
        loader: FakePhotoLoader,
        store: RenderedPhotoStore = RenderedPhotoStore(),
        saver: FakePhotoSaver = FakePhotoSaver(),
    ): ResultViewModel = ResultViewModel(
        photoUri = PHOTO_URI,
        viewer = true,
        store = store,
        loader = loader,
        saver = saver,
        probe = ViewModelClearedProbe(),
    )

    /** Runs everything waiting in the main thread's queue, where the ViewModel's coroutines go on. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Values shared by the tests. */
    private companion object {
        /** A saved photo as Android 10 and later return it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** The Uri the fake saver reports for a saved picture. */
        const val SAVED_URI = "content://media/external_primary/images/media/1"

        /** The size the screen asks for on the test phone: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3
    }
}
