package com.phongkaster.kanso.tool

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.save.FakePhotoSaver
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks the tool screen's ViewModel on its own, with no screen drawn (Run 7, DoD 10): each tool
 * has a screen of its own, so the state's tool is the navigation key's tool from the first
 * snapshot to the last, through the photo's decode and a whole save. Loading and saving each
 * change only their own part of the snapshot: the decoded photo stays the very same picture
 * through a save, and it is never asked for again.
 *
 * It runs under Robolectric because the [FakePhotoLoader] it is given needs an Android Context and
 * a Bitmap. Robolectric runs the test on Android's main thread, the thread the ViewModel's
 * coroutines run on, so a load or a save whose fake answers at once has finished when the call
 * returns; the main thread's queue is still run afterwards, so nothing waiting there is missed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ToolViewModelTest {

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
     * DoD 10: for each of the five tools, a ViewModel built for that tool starts with it and the
     * key's photo, nothing decoded and nothing saved, and keeps that tool in every snapshot it
     * hands out: before the decode, after it, while a save runs and after the save is done.
     */
    @Test
    fun the_state_keeps_the_key_s_tool_for_the_view_model_s_whole_life() {
        Tool.entries.forEach { tool ->
            // The gate holds the save open until the test completes it.
            val gate = CompletableDeferred<Unit>()
            val saver = FakePhotoSaver(gate = gate)
            val viewModel = toolViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)), saver, tool)
            assertEquals(ToolUiState(tool = tool, photoUri = PHOTO_URI), viewModel.state.value)

            viewModel.load(MAX_EDGE_PX)
            runMainThread()
            assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
            assertEquals(tool, viewModel.state.value.tool)

            viewModel.save()
            runMainThread()
            assertEquals(ToolUiState.SaveState.Saving, viewModel.state.value.save)
            assertEquals(tool, viewModel.state.value.tool)

            gate.complete(Unit)
            runMainThread()
            assertEquals(ToolUiState.SaveState.Saved(keptOriginalOrientation = false), viewModel.state.value.save)
            assertEquals(tool, viewModel.state.value.tool)
            assertEquals(PHOTO_URI, viewModel.state.value.photoUri)
            assertEquals(listOf(PHOTO_URI), saver.calls.toList())
        }
    }

    /**
     * A save, from start to end, leaves the decoded photo as the very same picture, and the
     * loader is asked for it only once.
     */
    @Test
    fun a_save_keeps_the_decoded_photo_and_does_not_load_again() {
        val bitmap = ToolFixtures.gradient(600, 400)
        val loader = FakePhotoLoader(app, bitmap)
        val viewModel = toolViewModel(loader, FakePhotoSaver())
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        val loaded = viewModel.state.value.photo
        assertTrue("The photo is not ready: $loaded", loaded is ToolUiState.PhotoState.Ready)

        viewModel.save()
        runMainThread()

        val state = viewModel.state.value
        assertEquals(ToolUiState.SaveState.Saved(keptOriginalOrientation = false), state.save)
        assertSame(loaded, state.photo)
        assertSame(bitmap, (state.photo as ToolUiState.PhotoState.Ready).bitmap)
        assertEquals(listOf(MAX_EDGE_PX), loader.requestedEdges.toList())
    }

    /** A failed save keeps the decoded photo and the tool, so the failure is shown over the photo. */
    @Test
    fun a_failed_save_keeps_the_photo_and_the_tool() {
        val viewModel = toolViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)), tool = Tool.ENHANCE)
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        // NoSavePhotoSaver fails every save.
        viewModel.save()
        runMainThread()

        assertEquals(ToolUiState.SaveState.Failed, viewModel.state.value.save)
        assertEquals(Tool.ENHANCE, viewModel.state.value.tool)
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the ViewModel on [tool] (Edit by default) for [PHOTO_URI] with [loader], the way the
     * app's Koin recipe does but by hand, with a probe nobody reads and [saver] (by default one
     * that writes nothing and fails every save).
     */
    private fun toolViewModel(
        loader: FakePhotoLoader,
        saver: PhotoSaver = NoSavePhotoSaver(),
        tool: Tool = Tool.EDIT,
    ): ToolViewModel =
        ToolViewModel(
            tool = tool,
            photoUri = PHOTO_URI,
            loader = loader,
            probe = ViewModelClearedProbe(),
            saver = saver,
        )

    /** Runs everything waiting in the main thread's queue, where the ViewModel's jobs end. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Values shared by the tests. */
    private companion object {
        /** The photo the ViewModel works on; the fake loader reads nothing from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** The size the screen asks for on the test phone: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3
    }
}
