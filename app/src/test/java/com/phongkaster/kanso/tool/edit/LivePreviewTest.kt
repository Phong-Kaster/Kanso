package com.phongkaster.kanso.tool.edit

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.EditColourMatrix
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.FilterPreviewRenderer
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.component.photocard.LIVE_PREVIEW_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.LivePreviewGeometry
import com.phongkaster.kanso.ui.tool.component.photocard.LivePreviewImage
import com.phongkaster.kanso.ui.tool.component.photocard.LivePreviewMatrix
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks on Android 15 the Edit screen's live preview (DoD 4, "the photo changes while a slider is
 * still being dragged, before the finger lifts", and DoD 6, "the preview is always the original
 * photo with every current setting applied; nothing is stacked or baked in as I go").
 *
 * **The drawn part.** `LivePreviewImage` draws the photo with the settings applied by Compose
 * itself, every frame. Its node says in its semantics (the screen's description that tests and
 * screen readers read) which colour matrix ([LivePreviewMatrix]) and which crop, turns and flips
 * ([LivePreviewGeometry]) it applies, so the tests read them instead of pixels (the pixels
 * themselves are compared with Done in `ColourMatrixParityTest`). The matrix is the identity with
 * neutral settings and changes with each of the four sliders; the geometry changes with a quarter
 * turn, each flip and a crop rectangle; and going back to a value gives that value's matrix
 * again, not one stacked on the last.
 *
 * **The computed part.** A filter is computed by the ViewModel on a small copy of the original
 * ([FilterPreviewRenderer.PREVIEW_EDGE] pixels long). With a fake processor that records every
 * input, the tests check that a filter-intensity drag (no release) publishes a new
 * `filterPreview` for each step before any commit, that `edited` stays null all along (nothing
 * full-size is rendered while editing), that every render's input is the same original-derived
 * 640-pixel copy (never an earlier result), and that a burst of steps ends with the last one
 * shown (a newer request replaces one still waiting: latest wins).
 *
 * The filter work runs on a real background thread (`Dispatchers.Default` inside the renderer), so
 * the ViewModel tests wait in real time, letting the main thread's queue run, rather than on a
 * virtual clock.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LivePreviewTest {

    /** Starts an empty activity to host the preview. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test, for the fake loader. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The settings the hosted preview draws, changed by the tests as the screen's state would be. */
    private var shownSettings by mutableStateOf(EditSettings())

    /**
     * Stops Koin after each test: the application starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * Neutral settings draw with the identity matrix; each of brightness, contrast, saturation and
     * warmth, up or down, changes it to exactly [EditColourMatrix.of] of the settings, the numbers
     * Done uses; and none of them touches the geometry.
     */
    @Test
    fun the_matrix_is_the_identity_when_neutral_and_follows_each_colour_slider() {
        showPreview()
        assertEquals(IDENTITY, shownMatrix())
        val neutralGeometry = shownGeometry()
        listOf(
            EditSettings(brightness = 0.5f),
            EditSettings(brightness = -0.5f),
            EditSettings(contrast = 0.5f),
            EditSettings(contrast = -0.5f),
            EditSettings(saturation = 0.5f),
            EditSettings(saturation = -0.5f),
            EditSettings(warmth = 0.5f),
            EditSettings(warmth = -0.5f),
        ).forEach { settings ->
            show(settings)
            val matrix = shownMatrix()
            assertNotEquals("$settings drew the identity matrix", IDENTITY, matrix)
            assertEquals(EditColourMatrix.of(settings).toList(), matrix)
            assertEquals("$settings changed the geometry", neutralGeometry, shownGeometry())
        }
        show(EditSettings())
        assertEquals("back to neutral did not give the identity", IDENTITY, shownMatrix())
    }

    /**
     * DoD 6: the matrix always comes from the current settings alone. Brightness 60 then 30 draws
     * the matrix of 30 (not 30 stacked on 60), and the four sliders together draw their one
     * combined matrix.
     */
    @Test
    fun the_matrix_is_never_stacked_on_the_last_one() {
        showPreview()
        show(EditSettings(brightness = 0.6f))
        show(EditSettings(brightness = 0.3f))
        assertEquals(EditColourMatrix.of(EditSettings(brightness = 0.3f)).toList(), shownMatrix())
        val all = EditSettings(brightness = 0.2f, contrast = -0.3f, saturation = 0.4f, warmth = -0.1f)
        show(all)
        assertEquals(EditColourMatrix.of(all).toList(), shownMatrix())
    }

    /**
     * The geometry follows a quarter turn, each flip and a crop rectangle, and four quarter turns
     * are no turn at all; none of them changes the colour matrix.
     */
    @Test
    fun the_geometry_follows_turns_flips_and_the_crop() {
        showPreview()
        val neutral = shownGeometry()
        assertTrue("neutral geometry: $neutral", neutral.startsWith("turns=0;flipH=false;flipV=false;crop="))

        show(EditSettings(quarterTurns = 1))
        assertNotEquals(neutral, shownGeometry())
        assertTrue(shownGeometry(), shownGeometry().startsWith("turns=1;"))
        assertEquals("a turn changed the colours", IDENTITY, shownMatrix())

        show(EditSettings(quarterTurns = -1))
        assertTrue(shownGeometry(), shownGeometry().startsWith("turns=3;"))

        show(EditSettings(quarterTurns = 4))
        assertEquals("four turns are not the neutral geometry", neutral, shownGeometry())

        show(EditSettings(flipHorizontal = true))
        assertNotEquals(neutral, shownGeometry())
        assertTrue(shownGeometry(), shownGeometry().contains("flipH=true"))

        show(EditSettings(flipVertical = true))
        assertNotEquals(neutral, shownGeometry())
        assertTrue(shownGeometry(), shownGeometry().contains("flipV=true"))

        val crop = EditSettings.CropRect(0.25f, 0.1f, 0.75f, 0.9f)
        show(EditSettings(crop = crop))
        assertNotEquals(neutral, shownGeometry())
        assertTrue(
            shownGeometry(),
            shownGeometry().endsWith("crop=${crop.left},${crop.top},${crop.right},${crop.bottom}"),
        )
        assertEquals("a crop changed the colours", IDENTITY, shownMatrix())
    }

    /**
     * DoD 4 through the ViewModel: a colour slider being dragged (`previewSettings`, no release)
     * reaches the drawn preview at once, step after step, while nothing is committed (no undo
     * entry yet) and nothing full-size is rendered (`edited` stays null).
     */
    @Test
    fun a_colour_slider_drag_changes_the_preview_before_release() {
        val photo = ToolFixtures.gradient(600, 400)
        val viewModel = loadedEditViewModel(photo)
        val recorder = FilterRenderRecorder()
        compose.setContent {
            val state by viewModel.state.collectAsState()
            Box(Modifier.size(PREVIEW_BOX_DP.dp)) {
                LivePreviewImage(state.filterPreview ?: photo, state.settings)
            }
        }
        compose.waitForIdle()
        assertEquals(IDENTITY, shownMatrix())

        listOf(0.2f, 0.5f, 0.8f).forEach { brightness ->
            val dragged = EditSettings(brightness = brightness)
            viewModel.previewSettings(dragged, recorder.processorFor)
            compose.waitForIdle()
            assertEquals("the preview did not follow the drag", EditColourMatrix.of(dragged).toList(), shownMatrix())
            assertFalse("the drag was committed before release", viewModel.state.value.canUndo)
            assertNull("a full-size picture was rendered while dragging", viewModel.state.value.edited)
        }
        assertTrue("a colour slider ran a CPU render", recorder.inputs.isEmpty())
    }

    /**
     * DoD 4 and 6 for a filter: a chip, then an intensity drag (no release). Each step publishes a
     * new `filterPreview` rendered at that step's intensity, before `commitSettings` is ever
     * called; `edited` stays null throughout; and every render's input is the very same
     * 640-pixel copy of the original, never the original itself nor an earlier render's result.
     */
    @Test
    fun a_filter_intensity_drag_renders_from_the_small_original_before_release() {
        val photo = ToolFixtures.gradient(1280, 960)
        val viewModel = loadedEditViewModel(photo)
        val recorder = FilterRenderRecorder()
        val chip = EditSettings(filter = FilterPreset.KELVIN)

        viewModel.changeSettings(chip, recorder.processorFor)
        waitUntil("the chip's preview") { recorder.settingsOf(viewModel.state.value.filterPreview) == chip }
        assertNull(viewModel.state.value.edited)
        val afterChip = checkNotNull(viewModel.state.value.filterPreview)

        val firstStep = chip.copy(filterIntensity = 0.7f)
        viewModel.previewSettings(firstStep, recorder.processorFor)
        waitUntil("the first drag step's preview") {
            recorder.settingsOf(viewModel.state.value.filterPreview) == firstStep
        }
        val afterFirst = checkNotNull(viewModel.state.value.filterPreview)
        assertNotSame(afterChip, afterFirst)
        assertNull("a full-size picture was rendered while dragging", viewModel.state.value.edited)

        val secondStep = chip.copy(filterIntensity = 0.3f)
        viewModel.previewSettings(secondStep, recorder.processorFor)
        waitUntil("the second drag step's preview") {
            recorder.settingsOf(viewModel.state.value.filterPreview) == secondStep
        }
        val afterSecond = checkNotNull(viewModel.state.value.filterPreview)
        assertNotSame(afterFirst, afterSecond)
        assertNotEquals(
            "the two intensities gave the same picture",
            afterFirst.getPixel(0, 0),
            afterSecond.getPixel(0, 0),
        )
        assertNull("a full-size picture was rendered while dragging", viewModel.state.value.edited)
        assertEquals(secondStep, viewModel.state.value.settings)
        // Only the chip is an undo entry so far: the drag has not been released.
        assertTrue(viewModel.state.value.canUndo)

        assertSmallOriginalWasEveryInput(recorder, photo)
        assertEquals(afterSecond.width, recorder.inputs.first().width)
        assertEquals(afterSecond.height, recorder.inputs.first().height)
    }

    /**
     * A burst of 20 quick intensity steps while a slow render runs ends with the LAST step shown:
     * the steps that arrived during the render replaced one another, so only the last is rendered
     * after it (the waiting request is conflated: latest wins), and none is shown out of order.
     */
    @Test
    fun a_burst_of_drag_steps_ends_with_the_last_one_shown() {
        val photo = ToolFixtures.gradient(1280, 960)
        val viewModel = loadedEditViewModel(photo)
        val slow = CompletableDeferred<Unit>()
        val recorder = FilterRenderRecorder(firstGate = slow)
        val chip = EditSettings(filter = FilterPreset.KELVIN)

        viewModel.changeSettings(chip, recorder.processorFor)
        waitUntil("the first render to start") { recorder.inputs.size == 1 }

        val steps = (1..BURST_STEPS).map { step -> chip.copy(filterIntensity = 1f - step * 0.04f) }
        steps.forEach { viewModel.previewSettings(it, recorder.processorFor) }
        runMainThread()
        assertEquals(steps.last(), viewModel.state.value.settings)

        slow.complete(Unit)
        waitUntil("the last step's preview") {
            recorder.settingsOf(viewModel.state.value.filterPreview) == steps.last()
        }
        // The slow render (the chip) finished out of date and was not shown; of the burst, only the
        // last step was rendered.
        assertEquals(listOf(chip, steps.last()), recorder.rendered)
        assertNull(viewModel.state.value.edited)
        assertSmallOriginalWasEveryInput(recorder, photo)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Stands in for the Edit panel's recipe `{ settings -> EditProcessor(settings, filters) }`:
     * each processor it builds records its input and its settings, and answers with a new
     * picture of the input's size filled with a red that tells the intensity apart. With
     * [firstGate] set, the first render waits for it, as a slow filter would.
     */
    private class FilterRenderRecorder(private val firstGate: CompletableDeferred<Unit>? = null) {

        /** Every input bitmap a render was given, oldest first. */
        val inputs = CopyOnWriteArrayList<Bitmap>()

        /** Each finished picture with the settings it was rendered from, oldest first. */
        private val results = CopyOnWriteArrayList<Pair<Bitmap, EditSettings>>()

        /** The settings of every finished render, oldest first. */
        val rendered: List<EditSettings>
            get() = results.map { it.second }

        /** The pictures every finished render answered with. */
        val outputs: List<Bitmap>
            get() = results.map { it.first }

        /** Builds the processor for one set of settings, as the renderer asks for each render. */
        val processorFor: (EditSettings) -> ToolProcessor = { settings ->
            object : ToolProcessor {
                /** Records [input], waits for the gate on the first render, and answers a picture. */
                override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult {
                    val index = synchronized(inputs) {
                        inputs += input
                        inputs.size - 1
                    }
                    if (index == 0) firstGate?.await()
                    val picture = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
                    picture.eraseColor(Color.rgb((settings.filterIntensity * 255).toInt(), 40, 40))
                    results += picture to settings
                    return EditResult.Edited(picture)
                }
            }
        }

        /** The settings [bitmap] was rendered from, or null for a picture this did not render. */
        fun settingsOf(bitmap: Bitmap?): EditSettings? = results.firstOrNull { it.first === bitmap }?.second
    }

    /**
     * Asserts every render of [recorder] was given one and the same picture: the original [photo]
     * shrunk to [FilterPreviewRenderer.PREVIEW_EDGE] on its long edge, never [photo] itself and
     * never a picture an earlier render answered with.
     */
    private fun assertSmallOriginalWasEveryInput(recorder: FilterRenderRecorder, photo: Bitmap) {
        assertTrue("nothing was rendered", recorder.inputs.isNotEmpty())
        val source = recorder.inputs.first()
        recorder.inputs.forEach { assertSame("a render worked on another picture", source, it) }
        assertNotSame("a render worked on the full-size original", photo, source)
        assertEquals(FilterPreviewRenderer.PREVIEW_EDGE, maxOf(source.width, source.height))
        assertEquals(photo.width * source.height, photo.height * source.width)
        recorder.outputs.forEach { output ->
            assertTrue("a render worked on an earlier result", recorder.inputs.none { it === output })
        }
    }

    /** Hosts the preview of the 120 x 80 sample photo drawing [shownSettings]. */
    private fun showPreview() {
        val photo = ToolFixtures.gradient(120, 80)
        compose.setContent {
            Box(Modifier.size(PREVIEW_BOX_DP.dp)) {
                LivePreviewImage(photo, shownSettings)
            }
        }
        compose.waitForIdle()
    }

    /** Makes the hosted preview draw [settings], as the screen's state changing would. */
    private fun show(settings: EditSettings) {
        shownSettings = settings
        compose.waitForIdle()
    }

    /** The colour matrix the preview's node says it applies. */
    private fun shownMatrix(): List<Float> =
        compose.onNodeWithTag(LIVE_PREVIEW_TAG).fetchSemanticsNode().config[LivePreviewMatrix]

    /** The geometry the preview's node says it applies. */
    private fun shownGeometry(): String =
        compose.onNodeWithTag(LIVE_PREVIEW_TAG).fetchSemanticsNode().config[LivePreviewGeometry]

    /**
     * Builds the Edit ViewModel for [photo] through a [FakePhotoLoader], the way the app's Koin
     * recipe does but by hand, loads the photo and checks it is shown. Its own background work
     * runs in place (`Dispatchers.Unconfined`); the filter renderer still uses its own thread.
     */
    private fun loadedEditViewModel(photo: Bitmap): ToolViewModel {
        val viewModel = ToolViewModel(
            tool = Tool.EDIT,
            photoUri = PHOTO_URI,
            loader = FakePhotoLoader(app, photo),
            probe = ViewModelClearedProbe(),
            workDispatcher = Dispatchers.Unconfined,
        )
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        assertNotNull((viewModel.state.value.photo as ToolUiState.PhotoState.Ready).bitmap)
        return viewModel
    }

    /**
     * Waits, in real time and at most [WAIT_TIMEOUT_MS], until [condition] holds, running the main
     * thread's queue between checks (where the ViewModel's results land); fails naming [what].
     */
    private fun waitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS
        while (true) {
            runMainThread()
            if (condition()) return
            if (System.currentTimeMillis() > deadline) fail("Timed out waiting for $what")
            Thread.sleep(POLL_MS)
        }
    }

    /** Runs everything waiting in the main thread's queue. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Values shared by the tests. */
    private companion object {
        /** The identity colour matrix's 20 numbers: ones at places 0, 6, 12 and 18. */
        val IDENTITY = List(20) { index -> if (index % 6 == 0) 1f else 0f }

        /** The side, in dp, of the box the preview is drawn in. */
        const val PREVIEW_BOX_DP = 200

        /** The photo the ViewModel works on; the fake loader reads nothing from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** The size the screen asks for on the test phone: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3

        /** How many quick drag steps the burst test sends. */
        const val BURST_STEPS = 20

        /** How long, in real milliseconds, a wait may take before the test fails. */
        const val WAIT_TIMEOUT_MS = 5_000L

        /** How long, in milliseconds, to sleep between two checks of a wait. */
        const val POLL_MS = 5L
    }
}
