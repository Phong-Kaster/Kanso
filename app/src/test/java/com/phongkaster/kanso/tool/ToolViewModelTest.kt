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
 * Checks the editor's ViewModel on its own, with no screen drawn: `selectTool` changes the
 * selected tool in the state and nothing else. The photo already decoded stays the very same
 * picture, it is never asked for again, and where saving stands is kept. While a save runs,
 * `selectTool` is ignored, so the tool saved with is the one the Saved screen gets.
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

    /** Before anything is loaded, selecting a tool changes the tool and leaves the rest as it was. */
    @Test
    fun select_tool_changes_only_the_tool() {
        val viewModel = toolViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)))
        assertEquals(ToolUiState(tool = Tool.EDIT, photoUri = PHOTO_URI), viewModel.state.value)

        viewModel.selectTool(Tool.RETOUCH)

        assertEquals(
            ToolUiState(
                tool = Tool.RETOUCH,
                photoUri = PHOTO_URI,
                photo = ToolUiState.PhotoState.Loading,
                save = ToolUiState.SaveState.Idle,
            ),
            viewModel.state.value,
        )
    }

    /**
     * After the photo is decoded, switching through every editing tool keeps that very picture
     * and the photo's Uri, and the loader is never asked again.
     */
    @Test
    fun select_tool_keeps_the_decoded_photo_and_does_not_load_again() {
        val bitmap = ToolFixtures.gradient(600, 400)
        val loader = FakePhotoLoader(app, bitmap)
        val viewModel = toolViewModel(loader)
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        val loaded = viewModel.state.value.photo
        assertTrue("The photo is not ready: $loaded", loaded is ToolUiState.PhotoState.Ready)

        listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC, Tool.EDIT).forEach { tool ->
            viewModel.selectTool(tool)
            runMainThread()

            val state = viewModel.state.value
            assertEquals(tool, state.tool)
            assertEquals(PHOTO_URI, state.photoUri)
            assertSame(loaded, state.photo)
            assertSame(bitmap, (state.photo as ToolUiState.PhotoState.Ready).bitmap)
            assertEquals(ToolUiState.SaveState.Idle, state.save)
        }
        assertEquals(listOf(MAX_EDGE_PX), loader.requestedEdges.toList())
    }

    /** Selecting a tool after a failed save keeps the failure, so its message stays on screen. */
    @Test
    fun select_tool_keeps_where_saving_stands() {
        val viewModel = toolViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)))
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        // NoSavePhotoSaver fails every save.
        viewModel.save()
        runMainThread()
        assertEquals(ToolUiState.SaveState.Failed, viewModel.state.value.save)

        viewModel.selectTool(Tool.ENHANCE)
        runMainThread()

        assertEquals(Tool.ENHANCE, viewModel.state.value.tool)
        assertEquals(ToolUiState.SaveState.Failed, viewModel.state.value.save)
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
    }

    /** Selecting the tool that is already selected leaves the state exactly as it was. */
    @Test
    fun selecting_the_selected_tool_changes_nothing() {
        val viewModel = toolViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)))
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        val before = viewModel.state.value

        viewModel.selectTool(Tool.EDIT)
        runMainThread()

        assertEquals(before, viewModel.state.value)
    }

    /**
     * DoD 7: while a save runs, selecting another tool is ignored, because the app reads the
     * selected tool when the save ends to decide which gallery "Edit another" opens. Saved on
     * Retouch, a switch to Enhance during the save leaves Retouch, also once the save is done;
     * after the save a switch works again.
     */
    @Test
    fun select_tool_is_ignored_while_a_save_runs() {
        // The gate holds the save open until the test completes it.
        val gate = CompletableDeferred<Unit>()
        val saver = FakePhotoSaver(gate = gate)
        val viewModel = toolViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)), saver)
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        viewModel.selectTool(Tool.RETOUCH)
        viewModel.save()
        runMainThread()
        val saving = viewModel.state.value
        assertEquals(ToolUiState.SaveState.Saving, saving.save)
        assertEquals(Tool.RETOUCH, saving.tool)

        viewModel.selectTool(Tool.ENHANCE)
        runMainThread()

        // Nothing changed: the tool, the photo and the running save are all as they were.
        assertEquals(saving, viewModel.state.value)

        gate.complete(Unit)
        runMainThread()

        assertEquals(ToolUiState.SaveState.Saved(keptOriginalOrientation = false), viewModel.state.value.save)
        assertEquals(Tool.RETOUCH, viewModel.state.value.tool)
        assertEquals(listOf(PHOTO_URI), saver.calls.toList())

        // The save is over: switching tools works again.
        viewModel.selectTool(Tool.ENHANCE)
        runMainThread()
        assertEquals(Tool.ENHANCE, viewModel.state.value.tool)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the ViewModel on Edit for [PHOTO_URI] with [loader], the way the app's Koin recipe
     * does but by hand, with a probe nobody reads and [saver] (by default one that writes
     * nothing and fails every save).
     */
    private fun toolViewModel(loader: FakePhotoLoader, saver: PhotoSaver = NoSavePhotoSaver()): ToolViewModel =
        ToolViewModel(
            tool = Tool.EDIT,
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
