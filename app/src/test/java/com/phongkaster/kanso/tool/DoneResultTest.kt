package com.phongkaster.kanso.tool

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.RenderedPhotoStore
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks what Done does in the tool screen's ViewModel (Run 9, T-006), with no screen drawn.
 *
 * Save was replaced by Done on all five tools. Done never writes a file: it puts the finished
 * picture into the hand-off store (`RenderedPhotoStore`) and sends one done event, which makes the
 * screen open Result; the Result screen saves or shares. These tests cover:
 *
 * - DoD 9: on Edit, Done renders the CURRENT settings at full working size from the original
 *   photo (not the small preview copy), hands that very picture over, and writes nothing (the
 *   ViewModel holds no saver at all).
 * - DoD 19: on the four other tools, Done hands over the edit the person sees.
 * - Done is ignored while it already runs, so two taps open Result once.
 * - DoD 11: Done does not end the session. After it, the settings, the undo steps and the edits
 *   are exactly as they were, so Back from Result finds the screen as it was left.
 *
 * The Edit tests run the render on `Dispatchers.Default`, as the app does, so they wait for it in
 * real time ([waitUntil]). The ViewModel's own coroutines run on Robolectric's main thread, which
 * is the test's thread; [runMainThread] runs what waits there.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DoneResultTest {

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
     * DoD 9: Edit's Done renders the settings as they are when Done is tapped, from the original
     * shrunk only to the Edit working size (3000 pixels), never the 640-pixel preview copy, and
     * the picture in the store is exactly that render, at the size the settings give it (a
     * quarter turn swaps width and height). One done event is sent and Done is usable again.
     */
    @Test
    fun edit_done_renders_the_settings_at_full_working_size_and_hands_that_picture_over() {
        val original = Bitmap.createBitmap(4500, 3000, Bitmap.Config.RGB_565)
        val store = RenderedPhotoStore()
        val viewModel = loadedViewModel(original, store, Tool.EDIT)
        val renderer = SizedRenderer()
        val turned = EditSettings(quarterTurns = 1, contrast = 0.2f)
        viewModel.changeSettings(turned, renderer.processorFor)
        runMainThread()
        assertTrue("A full render ran before Done", renderer.inputs.isEmpty())

        viewModel.done()
        waitUntil("Done's render to be handed over") { store.peek() != null && !viewModel.state.value.finishing }

        val fitted = WorkingSize.fit(original, WorkingSize.maxEdgeFor(Tool.EDIT))
        val input = renderer.inputs.single()
        assertEquals("Done did not render at the Edit working size", fitted.width, input.width)
        assertEquals(fitted.height, input.height)
        assertTrue(
            "Done rendered the small preview copy",
            maxOf(input.width, input.height) > FilterPreviewRenderer.PREVIEW_EDGE,
        )
        val handed = checkNotNull(store.peek())
        assertEquals("Done did not render the current settings", turned, renderer.settingsOf(handed))
        // A quarter turn: the picture handed over is the render, turned, at the render's size.
        assertEquals(input.height, handed.width)
        assertEquals(input.width, handed.height)
        assertEquals("Done did not open Result once", 1, doneEventCount(viewModel))
        assertNull(viewModel.state.value.failure)
    }

    /**
     * DoD 9: Done writes nothing. The ViewModel is given no saver and holds none: no constructor
     * parameter and no field is a [PhotoSaver], the only thing in the app that writes to the
     * gallery. So the picture can only go to the hand-off store.
     */
    @Test
    fun the_view_model_holds_no_saver_so_done_writes_nothing() {
        val saverType = PhotoSaver::class.java
        ToolViewModel::class.java.constructors.forEach { constructor ->
            assertTrue(
                "A ToolViewModel constructor takes a saver: $constructor",
                constructor.parameterTypes.none { saverType.isAssignableFrom(it) },
            )
        }
        val saverFields = ToolViewModel::class.java.declaredFields.filter { saverType.isAssignableFrom(it.type) }
        assertTrue("ToolViewModel holds a saver: $saverFields", saverFields.isEmpty())
    }

    /**
     * DoD 19: on each of the four other tools, Done hands over the edit the person sees (the last
     * edit, the very same bitmap), at once: nothing is rendered again, `finishing` never turns on,
     * and one done event is sent.
     */
    @Test
    fun each_other_tool_s_done_hands_over_its_edited_result() {
        OTHER_TOOLS.forEach { tool ->
            val store = RenderedPhotoStore()
            val result = ToolFixtures.gradient(320, 240)
            val viewModel = loadedViewModel(ToolFixtures.gradient(600, 400), store, tool)
            viewModel.apply(FakeProcessor { _, _ -> EditResult.Edited(result) })
            waitUntil("$tool's edit") { viewModel.state.value.edited != null }

            viewModel.done()
            assertFalse("$tool: Done showed a waiting state", viewModel.state.value.finishing)
            runMainThread()
            assertSame("$tool: Done did not hand over the edit", result, store.peek())
            assertEquals("$tool: Done did not open Result once", 1, doneEventCount(viewModel))
        }
    }

    /**
     * While Edit's Done renders, `finishing` is on; a second tap then is ignored, and so is a tap
     * on any tool while an edit runs. However many taps, the render runs once and Result opens
     * once.
     */
    @Test
    fun a_second_tap_while_done_runs_is_ignored_and_result_opens_once() {
        val store = RenderedPhotoStore()
        val viewModel = loadedViewModel(ToolFixtures.gradient(600, 400), store, Tool.EDIT)
        val gate = CompletableDeferred<Unit>()
        val renderer = SizedRenderer(gate)
        viewModel.changeSettings(EditSettings(flipHorizontal = true), renderer.processorFor)
        runMainThread()

        viewModel.done()
        assertTrue("Done did not show it is working", viewModel.state.value.finishing)
        waitUntil("the render to start") { renderer.inputs.size == 1 }
        viewModel.done()
        viewModel.done()
        runMainThread()
        assertEquals("A second tap started another render", 1, renderer.inputs.size)
        assertNull("A picture was handed over before the render ended", store.peek())

        gate.complete(Unit)
        waitUntil("Done to finish") { !viewModel.state.value.finishing && store.peek() != null }
        assertEquals("Two taps opened Result twice", 1, doneEventCount(viewModel))
        assertEquals(1, renderer.inputs.size)

        // A tap while another tool's edit runs is ignored too.
        val beautifyStore = RenderedPhotoStore()
        val beautify = loadedViewModel(ToolFixtures.gradient(600, 400), beautifyStore, Tool.BEAUTIFY)
        val never = CompletableDeferred<Unit>()
        beautify.apply(FakeProcessor { input, _ -> never.await(); EditResult.Edited(input) })
        waitUntil("the edit to run") { beautify.state.value.processing is ToolUiState.Processing.Running }
        beautify.done()
        beautify.done()
        runMainThread()
        assertNull(beautifyStore.peek())
        assertEquals(0, doneEventCount(beautify))
        never.complete(Unit)
    }

    /**
     * DoD 11: Done leaves Edit's session intact. After Done, the settings are the ones Done
     * rendered, Undo still steps back through each earlier action, and Back still asks to discard
     * the changes, so coming back from Result finds the screen as it was.
     */
    @Test
    fun after_edit_done_the_settings_and_undo_steps_are_intact() {
        val store = RenderedPhotoStore()
        val viewModel = loadedViewModel(ToolFixtures.gradient(600, 400), store, Tool.EDIT)
        val renderer = SizedRenderer()
        val first = EditSettings(quarterTurns = 1)
        val second = first.copy(contrast = 0.4f)
        viewModel.changeSettings(first, renderer.processorFor)
        viewModel.changeSettings(second, renderer.processorFor)
        runMainThread()
        val before = viewModel.state.value

        viewModel.done()
        waitUntil("Done to finish") { !viewModel.state.value.finishing && store.peek() != null }
        assertEquals(1, doneEventCount(viewModel))

        val after = viewModel.state.value
        assertEquals("Done changed the settings", second, after.settings)
        assertTrue("Done lost the undo steps", after.canUndo)
        assertEquals("Done changed the photo", before.photo, after.photo)
        assertNull("Done left a full-size picture in the Edit state", after.edited)
        assertNull(after.failure)
        assertFalse(after.discardPrompt)

        viewModel.undo()
        runMainThread()
        assertEquals("Undo after Done did not step back one action", first, viewModel.state.value.settings)
        viewModel.undo()
        runMainThread()
        assertEquals(EditSettings(), viewModel.state.value.settings)
        assertFalse(viewModel.state.value.canUndo)
    }

    /**
     * DoD 11: Done leaves the other tools' edits intact: the shown edit is the same bitmap, Undo
     * still steps back to the edit before it and then to the original.
     */
    @Test
    fun after_another_tool_s_done_the_edits_and_undo_steps_are_intact() {
        val original = ToolFixtures.gradient(600, 400)
        val store = RenderedPhotoStore()
        val viewModel = loadedViewModel(original, store, Tool.REMOVE_BACKGROUND)
        val first = ToolFixtures.gradient(300, 200)
        val second = ToolFixtures.twoHalves(300, 200)
        viewModel.apply(FakeProcessor { _, _ -> EditResult.Edited(first) })
        waitUntil("the first edit") { viewModel.state.value.edited === first }
        viewModel.apply(FakeProcessor { _, _ -> EditResult.Edited(second) })
        waitUntil("the second edit") { viewModel.state.value.edited === second }

        viewModel.done()
        runMainThread()
        assertSame(second, store.peek())
        assertEquals(1, doneEventCount(viewModel))
        assertSame("Done changed the shown edit", second, viewModel.state.value.edited)
        assertTrue("Done lost the undo steps", viewModel.state.value.canUndo)

        viewModel.undo()
        assertSame(first, viewModel.state.value.edited)
        viewModel.undo()
        assertNull(viewModel.state.value.edited)
        assertSame(original, (viewModel.state.value.photo as ToolUiState.PhotoState.Ready).bitmap)
    }

    /**
     * Done before the photo is shown does nothing: there is nothing to hand over yet.
     */
    @Test
    fun done_before_the_photo_is_shown_does_nothing() {
        Tool.entries.forEach { tool ->
            val store = RenderedPhotoStore()
            val viewModel = toolViewModel(FakePhotoLoader(app, ToolFixtures.gradient(600, 400)), store, tool)
            viewModel.done()
            runMainThread()
            assertNull("$tool: Done handed something over before the photo", store.peek())
            assertEquals(0, doneEventCount(viewModel))
            assertFalse(viewModel.state.value.finishing)
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Stands in for the Edit panel's recipe, with no engine: each processor it builds records its
     * input and answers with a new picture of the input's size, its width and height swapped when
     * the settings hold an odd number of quarter turns (as a real turn would), remembered with its
     * settings. With a [gate], every render waits for it first, ignoring cancellation as a real
     * processor might.
     */
    private class SizedRenderer(private val gate: CompletableDeferred<Unit>? = null) {

        /** Every input a render was given, oldest first. */
        val inputs = CopyOnWriteArrayList<Bitmap>()

        /** Each picture rendered, with the settings it came from. */
        private val results = CopyOnWriteArrayList<Pair<Bitmap, EditSettings>>()

        /** Builds the processor for one set of settings, as the ViewModel asks for each render. */
        val processorFor: (EditSettings) -> ToolProcessor = { settings ->
            FakeProcessor { input, _ ->
                inputs += input
                gate?.let { withContext(NonCancellable) { it.await() } }
                val turned = settings.quarterTurns % 2 != 0
                val picture = Bitmap.createBitmap(
                    if (turned) input.height else input.width,
                    if (turned) input.width else input.height,
                    Bitmap.Config.RGB_565,
                )
                results += picture to settings
                EditResult.Edited(picture)
            }
        }

        /** The settings [bitmap] was rendered from, or null for a picture this did not render. */
        fun settingsOf(bitmap: Bitmap?): EditSettings? = results.firstOrNull { it.first === bitmap }?.second
    }

    /** A [ToolProcessor] for tests: answers with [block], which may wait, return or throw. */
    private class FakeProcessor(
        private val block: suspend (input: Bitmap, onProgress: (Float) -> Unit) -> EditResult,
    ) : ToolProcessor {

        /** Runs [block] on [input]. */
        override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult =
            block(input, onProgress)
    }

    /** Builds the ViewModel for [tool] on [photo], loads it and checks it is shown. */
    private fun loadedViewModel(photo: Bitmap, store: RenderedPhotoStore, tool: Tool): ToolViewModel {
        val viewModel = toolViewModel(FakePhotoLoader(app, photo), store, tool)
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        return viewModel
    }

    /**
     * Builds the ViewModel by hand, as the app's Koin recipe does, with [store] as the hand-off
     * Done fills and the background work on `Dispatchers.Default`, the app's own choice.
     */
    private fun toolViewModel(loader: FakePhotoLoader, store: RenderedPhotoStore, tool: Tool): ToolViewModel =
        ToolViewModel(
            tool = tool,
            photoUri = PHOTO_URI,
            loader = loader,
            probe = ViewModelClearedProbe(),
            registry = ProcessorRegistry(),
            store = store,
            workDispatcher = Dispatchers.Default,
        )

    /**
     * Counts the done events [viewModel] has sent and nobody has read yet, reading them all; the
     * short timeout ends the count once the queue (a Channel) is empty.
     */
    private fun doneEventCount(viewModel: ToolViewModel): Int = runBlocking {
        var count = 0
        withTimeoutOrNull(EVENT_WAIT_MS) { viewModel.doneEvents.collect { count++ } }
        count
    }

    /** Runs everything waiting in the main thread's queue, where the ViewModel's jobs end. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Waits, in real time and at most 5 seconds, until [condition] holds, running the main
     * thread's queue between checks; fails naming [what]. The work runs on `Dispatchers.Default`,
     * so its result reaches the main thread only after real time has passed.
     */
    private fun waitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000L
        while (true) {
            runMainThread()
            if (condition()) return
            if (System.currentTimeMillis() > deadline) fail("Timed out waiting for $what")
            Thread.sleep(5L)
        }
    }

    /** Values shared by the tests. */
    private companion object {
        /** The photo the ViewModel works on; the fake loader reads nothing from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** The size the screen asks for on the test phone: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3

        /** How long [doneEventCount] waits for one more event before it stops counting. */
        const val EVENT_WAIT_MS = 100L

        /** The four tools other than Edit, whose Done hands over their last edit. */
        val OTHER_TOOLS = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.BEAUTIFY, Tool.ARTISTIC)
    }
}
