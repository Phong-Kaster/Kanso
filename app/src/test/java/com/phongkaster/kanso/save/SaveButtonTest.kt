package com.phongkaster.kanso.save

import android.app.Application
import android.graphics.Bitmap
import android.os.Environment
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.RenderedPhotoStore
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.photo.LoadedPhoto
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.TOOL_BODY_TAG
import com.phongkaster.kanso.ui.tool.TOOL_DONE_PROGRESS_TAG
import com.phongkaster.kanso.ui.tool.TOOL_DONE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_LOADING_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolViewModel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowEnvironment

/**
 * Formerly the Save button test; now the Done button test (Run 9, T-006). The file and the class
 * keep their old name so the test history stays in one place.
 *
 * Save was replaced by Done at the top right of every tool screen. Done never writes a file and
 * never asks for a storage permission: it puts the finished picture into the hand-off store
 * (`RenderedPhotoStore`) and calls `onDone`, once, and the caller opens the Result screen, which
 * saves. This checks, on the real tool screen:
 *
 * - DoD 9: a tap on Edit's Done renders the current settings, hands that picture over, calls
 *   `onDone` once, and writes nothing to the gallery (a fake MediaStore stays empty) on every
 *   Android version the app supports, with no permission asked (one test per version).
 * - While Edit's Done renders, the button is off and shows a small spinner; a second tap is
 *   ignored, so Result opens once.
 * - Done is off until the photo is shown; meanwhile the photo area shows its loading indicator.
 *
 * The old tests of Save's saving state, its messages and the storage permission on Android 8.1
 * and 9 are gone with Save: Run 9 (T-006) moved saving to the Result screen.
 *
 * The screen shows a loading indicator in the photo area until the photo is decoded, and a spinner
 * in Done while Edit renders. Both keep moving for as long as they are shown, so the screen is
 * shown with the test's clock paused, which is moved by hand (see [host]).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SaveButtonTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** A fresh folder for the fake MediaStore's files. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Records permission requests instead of showing the system dialog; Done must make none. */
    private val registry = SinglePermissionRegistry()

    /** How many times the screen called onDone. */
    private val doneCalls = AtomicInteger(0)

    /** Mounts shared storage and empties the Kanso album used on Android 8.1 and 9. */
    @Before
    fun cleanAlbum() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        legacyKansoFolder().deleteRecursively()
    }

    /** Stops Koin, which KansoApplication starts for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * A tap on Done on a tool other than Edit hands over the photo shown (here the unedited one)
     * at once and calls onDone once; Done is usable again afterwards.
     */
    @Test
    fun done_hands_the_photo_over_and_calls_on_done_once() {
        val photo = ToolFixtures.gradient(600, 400)
        val store = RenderedPhotoStore()
        host(ToolViewModel(Tool.BEAUTIFY, PHOTO_URI, FakePhotoLoader(app, photo), ViewModelClearedProbe(), store = store))

        compose.onNodeWithTag(TOOL_DONE_TAG).assertIsDisplayed().assertIsEnabled()
            .assert(hasText(app.getString(R.string.tool_done)))
            .performClick()
        waitUntil { doneCalls.get() > 0 }

        assertEquals(1, doneCalls.get())
        assertSame(photo, store.peek())
        assertTrue("A permission was requested: ${registry.requests}", registry.requests.isEmpty())
        compose.onNodeWithTag(TOOL_DONE_TAG).assertIsEnabled()
    }

    /**
     * While Edit's Done renders the settings, Done is off and shows its spinner; a second tap does
     * nothing. When the render ends the spinner goes, the render is in the store, and onDone has
     * been called exactly once.
     *
     * The spinner keeps moving while the render is held open, so the test's clock stays paused
     * for the whole test and moves only by hand.
     */
    @Test
    fun done_is_disabled_and_shows_progress_while_edit_renders() {
        val gate = CompletableDeferred<Unit>()
        val recipe = Recipe(gate)
        val store = RenderedPhotoStore()
        val viewModel = editViewModel(store)
        host(viewModel, keepClockPaused = true)
        compose.runOnUiThread { viewModel.changeSettings(EditSettings(quarterTurns = 1), recipe.processorFor) }

        compose.onNodeWithTag(TOOL_DONE_TAG).performClick()
        waitUntil { hasNode(TOOL_DONE_PROGRESS_TAG) }
        // Lets the spinner finish growing in, so it is fully on screen when checked.
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag(TOOL_DONE_PROGRESS_TAG).assertIsDisplayed()
        compose.onNodeWithTag(TOOL_DONE_TAG).assertIsNotEnabled().performClick()
        waitUntil { recipe.renders.get() == 1 }
        assertNull("A picture was handed over before the render ended", store.peek())
        assertEquals(0, doneCalls.get())

        gate.complete(Unit)
        waitUntil { doneCalls.get() > 0 }
        // With the clock paused the screen catches up only as the clock moves: once the spinner
        // has gone, Done has been drawn again for the finished render.
        waitUntil { !hasNode(TOOL_DONE_PROGRESS_TAG) }
        compose.onNodeWithTag(TOOL_DONE_TAG).assertIsEnabled()
        assertEquals("Two taps opened Result twice", 1, doneCalls.get())
        assertEquals("A second tap rendered again", 1, recipe.renders.get())
        assertEquals(EditSettings(quarterTurns = 1), recipe.settingsOf(store.peek()))
    }

    /**
     * While the photo is still being decoded, the photo area shows a loading indicator in its
     * middle, named for screen readers, and no photo, and Done is off; once the photo is ready the
     * indicator has gone, the photo is shown in its place and Done is on.
     *
     * The loader holds the photo back until the test lets it go. The indicator keeps moving
     * meanwhile, so the clock is paused before the screen appears and moves only by hand.
     */
    @Test
    fun the_photo_area_shows_a_loading_indicator_until_the_photo_is_ready() {
        val gate = CompletableDeferred<Unit>()
        val photo = ToolFixtures.gradient(600, 400)
        // A loader that answers only once the gate is opened, so the screen can be seen mid-load.
        // An object expression, not a class of its own: it lives only inside this test.
        val loader = object : PhotoLoader(app) {
            /** Waits until the test opens the gate, then hands back the gradient photo. */
            override suspend fun load(uri: String, maxEdgePx: Int): LoadedPhoto? {
                gate.await()
                return LoadedPhoto(photo)
            }
        }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = ToolViewModel(Tool.EDIT, PHOTO_URI, loader, ViewModelClearedProbe())
        compose.mainClock.autoAdvance = false
        setScreen(viewModel)

        waitUntil { hasNode(TOOL_LOADING_TAG) }
        compose.onNodeWithTag(TOOL_LOADING_TAG)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(app.getString(R.string.tool_loading))
        // In the middle of the photo area, give or take a pixel of rounding.
        val indicator = compose.onNodeWithTag(TOOL_LOADING_TAG).fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag(TOOL_BODY_TAG).fetchSemanticsNode().boundsInRoot
        assertEquals(body.center.x, indicator.center.x, 1f)
        assertEquals(body.center.y, indicator.center.y, 1f)
        assertFalse("The photo is shown before it was decoded", hasNode(TOOL_IMAGE_TAG))
        compose.onNodeWithTag(TOOL_DONE_TAG).assertIsNotEnabled()

        gate.complete(Unit)
        waitUntil { hasNode(TOOL_IMAGE_TAG) }
        assertFalse("The loading indicator stayed after the photo arrived", hasNode(TOOL_LOADING_TAG))
        compose.onNodeWithTag(TOOL_IMAGE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(TOOL_DONE_TAG).assertIsEnabled()
    }

    /** Done cannot be tapped while the photo is not shown (here: it cannot be opened). */
    @Test
    fun done_is_disabled_without_a_photo() {
        val viewModel = ToolViewModel(Tool.EDIT, PHOTO_URI, FakePhotoLoader(app, null), ViewModelClearedProbe())
        host(viewModel)

        compose.onNodeWithTag(TOOL_DONE_TAG).assertIsDisplayed().assertIsNotEnabled()
    }

    // --- Done writes nothing, on every Android version --------------------------------------------

    /** Android 15: Edit's Done asks for nothing and writes no row into the (fake) MediaStore. */
    @Test
    fun android_15_done_writes_nothing() {
        assertDoneWritesNothingToMediaStore()
    }

    /** Android 10: Edit's Done asks for nothing and writes no row into the (fake) MediaStore. */
    @Test
    @Config(sdk = [29])
    fun android_10_done_writes_nothing() {
        assertDoneWritesNothingToMediaStore()
    }

    /**
     * Android 9: Edit's Done asks for no storage permission (Save asked for one here; Run 9,
     * T-006 moved saving to Result) and writes no file into the Kanso album.
     */
    @Test
    @Config(sdk = [28])
    fun android_9_done_asks_nothing_and_writes_nothing() {
        assertDoneWritesNothingToTheAlbum()
    }

    /** Android 8.1: the same. */
    @Test
    @Config(sdk = [27])
    fun android_8_1_done_asks_nothing_and_writes_nothing() {
        assertDoneWritesNothingToTheAlbum()
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Stands in for the Edit panel's recipe, with no engine: each processor it builds counts the
     * render, waits for [gate] if there is one (ignoring cancellation, as a real processor might)
     * and answers with a new picture of its input's size, remembered with its settings.
     */
    private class Recipe(private val gate: CompletableDeferred<Unit>? = null) {

        /** How many renders have started. */
        val renders = AtomicInteger(0)

        /** Each picture rendered, with the settings it came from. */
        private val results = CopyOnWriteArrayList<Pair<Bitmap, EditSettings>>()

        /** Builds the processor for one set of settings, as the ViewModel asks for each render. */
        val processorFor: (EditSettings) -> ToolProcessor = { settings ->
            object : ToolProcessor {
                /** Counts the render, waits for the gate, and answers with a new picture. */
                override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult {
                    renders.incrementAndGet()
                    gate?.let { withContext(NonCancellable) { it.await() } }
                    val picture = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
                    results += picture to settings
                    return EditResult.Edited(picture)
                }
            }
        }

        /** The settings [bitmap] was rendered from, or null for a picture this did not render. */
        fun settingsOf(bitmap: Bitmap?): EditSettings? = results.firstOrNull { it.first === bitmap }?.second
    }

    /**
     * Taps Edit's Done after a change, on Android 10 or later, with a fake MediaStore installed,
     * and checks the render was handed over, onDone was called once, nothing was asked and no
     * row was written.
     */
    private fun assertDoneWritesNothingToMediaStore() {
        val media = FakeMediaProvider.install(temp.newFolder("media"))
        val store = assertEditDoneHandsOver()
        assertTrue("Done wrote into the MediaStore: ${media.rows.keys}", media.rows.isEmpty())
        assertTrue(store.peek() != null)
    }

    /**
     * Taps Edit's Done after a change, on Android 8.1 or 9, and checks the render was handed
     * over, onDone was called once, no permission was asked and no file was written to the album.
     */
    private fun assertDoneWritesNothingToTheAlbum() {
        assertEditDoneHandsOver()
        assertTrue("Done wrote a file: ${legacyKansoFiles()}", legacyKansoFiles().isEmpty())
    }

    /**
     * Shows Edit on the gradient photo, makes one change (a quarter turn) with a [Recipe], taps
     * Done and waits for onDone; checks the store holds that render, onDone ran once and no
     * permission was requested. Returns the store.
     */
    private fun assertEditDoneHandsOver(): RenderedPhotoStore {
        val recipe = Recipe()
        val store = RenderedPhotoStore()
        val viewModel = editViewModel(store)
        // The clock stays paused: Done's spinner turns while the render runs.
        host(viewModel, keepClockPaused = true)
        compose.runOnUiThread { viewModel.changeSettings(EditSettings(quarterTurns = 1), recipe.processorFor) }
        compose.waitForIdle()

        compose.onNodeWithTag(TOOL_DONE_TAG).performClick()
        waitUntil { doneCalls.get() > 0 }
        compose.waitForIdle()

        assertEquals(1, doneCalls.get())
        assertEquals(EditSettings(quarterTurns = 1), recipe.settingsOf(store.peek()))
        assertTrue("A permission was requested: ${registry.requests}", registry.requests.isEmpty())
        return store
    }

    /** Builds the Edit screen's ViewModel on the gradient photo, handing over into [store]. */
    private fun editViewModel(store: RenderedPhotoStore): ToolViewModel =
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        ToolViewModel(
            Tool.EDIT,
            PHOTO_URI,
            FakePhotoLoader(app, ToolFixtures.gradient(600, 400)),
            ViewModelClearedProbe(),
            store = store,
        )

    /**
     * Shows the tool screen for [viewModel] and waits until the photo area has finished loading
     * (the photo or the "cannot be opened" message has replaced the loading indicator).
     *
     * The test's clock is paused before the screen appears and moved by hand meanwhile, because
     * the loading indicator keeps moving for as long as it is shown (see the class description);
     * the fake loaders answer at once, so it is gone after a frame or two. Afterwards the clock
     * runs on its own again, unless [keepClockPaused] is true.
     */
    private fun host(viewModel: ToolViewModel, keepClockPaused: Boolean = false) {
        compose.mainClock.autoAdvance = false
        setScreen(viewModel)
        waitUntil { hasNode(TOOL_BODY_TAG) && !hasNode(TOOL_LOADING_TAG) }
        compose.mainClock.autoAdvance = !keepClockPaused
    }

    /**
     * Sets the tool screen for [viewModel] as the content, with the recording registry, so a
     * permission request would be seen. The caller pauses the test's clock first, since the
     * screen opens on a loading indicator.
     */
    private fun setScreen(viewModel: ToolViewModel) {
        compose.setContent {
            val owner = object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry = registry
            }
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                MaterialTheme {
                    ToolScreen(viewModel = viewModel, onBack = {}, onDone = { doneCalls.incrementAndGet() })
                }
            }
        }
        compose.waitForIdle()
    }

    /**
     * Waits until [condition] holds. Edit's render finishes on a background thread and comes back
     * to the main thread, so the main looper is run on each check. While the test's clock is
     * paused it is also moved on by one frame before each check: a paused clock never moves by
     * itself, and the screen only catches up with a change as the clock moves.
     */
    private fun waitUntil(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeByFrame()
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Tells whether a node tagged [tag] is on the screen now. */
    private fun hasNode(tag: String): Boolean =
        compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    /** Values the tests share. */
    private companion object {
        /** The photo the tests show; the fake loader reads nothing from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"
    }
}
