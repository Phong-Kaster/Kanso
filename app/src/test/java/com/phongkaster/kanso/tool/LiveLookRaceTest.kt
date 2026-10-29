package com.phongkaster.kanso.tool

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.RenderedPhotoStore
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A live frame that is still being made when the full render finishes must not reach the screen
 * after it: it would be drawn over the finished picture for good (the owner saw it on the Galaxy
 * A13 as a seam around Temple, 2026-10-29). A frame asked for after the render began still shows.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LiveLookRaceTest {

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin after each test, as the other ViewModel tests do. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * Drag (a live frame starts and is held), let go (the render finishes), then let the held
     * frame finish: the finished picture is shown with no live look on top. A drag after that
     * shows its live look as usual.
     */
    @Test
    fun a_frame_older_than_the_finished_render_is_dropped() {
        val held = CompletableDeferred<Unit>()
        val processor = GatedLiveProcessor(gates = listOf(held))
        val viewModel = ToolViewModel(
            tool = Tool.BEAUTIFY,
            photoUri = "content://kanso.test/photo/1",
            loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400)),
            probe = ViewModelClearedProbe(),
            registry = ProcessorRegistry(mapOf(Tool.BEAUTIFY to processor)),
            store = RenderedPhotoStore(),
            workDispatcher = Dispatchers.Unconfined,
        )
        viewModel.load(1000)
        runMainThread()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)

        viewModel.previewRegistered(configure = {})
        runMainThread()
        viewModel.renderRegistered(configure = {})
        runMainThread()
        assertNotNull("the render did not finish", viewModel.state.value.edited)
        held.complete(Unit)
        runMainThread()

        assertNull("an old live frame covers the finished picture", viewModel.state.value.livePreview)

        viewModel.previewRegistered(configure = {})
        runMainThread()
        assertNotNull("a new drag shows no live look", viewModel.state.value.livePreview)
    }

    /** Runs everything waiting in the main thread's queue, where the ViewModel's jobs end. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }
}
