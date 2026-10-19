package com.phongkaster.kanso.screenshots

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.edit.RenderedPhotoStore
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.save.FakePhotoSaver
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.result.RESULT_EDIT_TAG
import com.phongkaster.kanso.ui.result.RESULT_HOME_TAG
import com.phongkaster.kanso.ui.result.RESULT_SAVE_TAG
import com.phongkaster.kanso.ui.result.RESULT_SHARE_TAG
import com.phongkaster.kanso.ui.result.ResultScreen
import com.phongkaster.kanso.ui.result.ResultUiState
import com.phongkaster.kanso.ui.result.ResultViewModel
import com.phongkaster.kanso.ui.result.RESULT_BACKDROP_TAG
import com.phongkaster.kanso.ui.result.RESULT_CLOSE_TAG
import com.phongkaster.kanso.ui.result.RESULT_MARK_TAG
import com.phongkaster.kanso.ui.result.RESULT_PHOTO_TAG
import com.phongkaster.kanso.ui.result.RESULT_SCRIM_TAG
import com.phongkaster.kanso.ui.theme.KansoTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Takes the reference pictures of the Result screen, in light and in dark: right after Done once
 * Save has succeeded (pictures 18 and 19, with the success mark over the top of the photo at rest)
 * and as the photo viewer, a photo opened from My edits (pictures 43 and 44: the close button, the
 * title "My edits", Share and "Edit", no mark). Both show the whole photo over its blurred copy and
 * the shade at the top behind the title.
 *
 * Run 9: rewritten from the old SavedScreenshotTest, because the Result screen replaced the Saved
 * screen. The viewer keeps its form and its pictures' names (43, 44). Pictures 18 and 19 keep their
 * names but now show the Result screen after a save ("Your edit", Share, a switched-off "Saved"
 * button and Go home, no close button), so their reference files must be recorded again.
 *
 * The photo is a 600 x 400 gradient drawn in code: right after Done it waits in the hand-off store
 * and a [FakePhotoSaver] "saves" it without writing anything; in the viewer a [FakePhotoLoader]
 * hands it over. So the pictures never depend on a file or on the decoder. The screen is hosted in
 * KansoTheme with the mode forced, on Android 11 and the same phone screen as [ScreenshotTest]. On
 * Android 11 Compose's blur draws nothing, so the backdrop is the stretched tiny copy alone, the
 * same on every run.
 *
 * The test clock is paused before anything is drawn (`mainClock.autoAdvance = false`) and moved
 * only by hand, for two reasons. While the viewer's photo is decoded the screen shows a loading
 * indicator that never stops by itself, and with the clock running by itself a wait for the screen
 * to settle would wait for it forever. And the success mark animates once when the screen appears:
 * the clock is moved on by [MARK_AT_REST_MS], well past the end of that animation, so every
 * recording shows the mark in the same, final state.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = RESULT_PHONE)
class ResultScreenshotTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Picture 18: the Result screen once the edit is saved, in light mode. */
    @Test
    fun saved_light() {
        showResult(ThemeMode.Light, viewer = false)
        capture("18_saved_light.png")
    }

    /** Picture 19: the Result screen once the edit is saved, in dark mode. */
    @Test
    fun saved_dark() {
        showResult(ThemeMode.Dark, viewer = false)
        capture("19_saved_dark.png")
    }

    /** Picture 43: the photo viewer (a photo opened from My edits), in light mode. */
    @Test
    fun photo_viewer_light() {
        showResult(ThemeMode.Light, viewer = true)
        capture("43_photo_viewer_light.png")
    }

    /** Picture 44: the photo viewer (a photo opened from My edits), in dark mode. */
    @Test
    fun photo_viewer_dark() {
        showResult(ThemeMode.Dark, viewer = true)
        capture("44_photo_viewer_dark.png")
    }

    /**
     * Shows the Result screen on the gradient photo in [mode]: as the photo viewer when [viewer],
     * else right after Done with the picture already saved. The clock is paused, then moved on
     * until the photo is drawn, then on by [MARK_AT_REST_MS] so the mark (after a save) is at rest.
     * Checks the parts each picture must hold, so a picture of a half-built screen is never
     * recorded.
     */
    private fun showResult(mode: ThemeMode, viewer: Boolean) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val gradient = ToolFixtures.gradient(600, 400)
        // Right after Done the picture waits in the hand-off store; the viewer ignores the store.
        val store = RenderedPhotoStore().apply { if (!viewer) put(gradient) }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = ResultViewModel(
            photoUri = if (viewer) "content://media/external/images/media/42" else null,
            viewer = viewer,
            store = store,
            loader = FakePhotoLoader(context, gradient),
            saver = FakePhotoSaver(),
            probe = ViewModelClearedProbe(),
        )
        if (!viewer) {
            // The fake saver answers at once, so the state is Saved before the first frame.
            viewModel.save()
            shadowOf(Looper.getMainLooper()).idle()
            check(viewModel.state.value.save is ResultUiState.SaveState.Saved) { "The picture was not saved" }
        }
        // Paused before setContent, so the first frame is already under the test's control (see
        // the class comment).
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = mode) {
                ResultScreen(
                    viewModel = viewModel,
                    onClose = {},
                    onEdit = {},
                )
            }
        }
        waitForPhoto()
        advance(MARK_AT_REST_MS)

        compose.onNodeWithTag(RESULT_PHOTO_TAG).assertIsDisplayed()
        compose.onNodeWithTag(RESULT_BACKDROP_TAG).assertIsDisplayed()
        compose.onNodeWithTag(RESULT_SCRIM_TAG).assertIsDisplayed()
        compose.onNodeWithTag(RESULT_SHARE_TAG).assertIsDisplayed()
        if (viewer) {
            compose.onNodeWithTag(RESULT_CLOSE_TAG).assertIsDisplayed()
            compose.onNodeWithTag(RESULT_EDIT_TAG).assertIsDisplayed()
            compose.onAllNodesWithTag(RESULT_MARK_TAG).assertCountEquals(0)
        } else {
            compose.onNodeWithTag(RESULT_SAVE_TAG).assertIsDisplayed()
            compose.onNodeWithTag(RESULT_HOME_TAG).assertIsDisplayed()
            compose.onNodeWithTag(RESULT_MARK_TAG).assertIsDisplayed()
        }
    }

    /**
     * Moves the paused clock forward until the photo is on screen. The viewer's load is started
     * by the screen and ends on the main thread, so the clock is moved and the main thread's queue
     * is run in turn. Gives up after about ten seconds of the test's clock.
     */
    private fun waitForPhoto() {
        repeat(100) {
            if (compose.onAllNodes(hasTestTag(RESULT_PHOTO_TAG)).fetchSemanticsNodes().isNotEmpty()) return
            advance(100)
        }
        error("The photo never appeared")
    }

    /**
     * Moves the paused clock forward by [millis], one frame after another, and runs whatever the
     * main thread was asked to do meanwhile.
     */
    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Saves a picture of the whole window as `src/test/screenshots/[name]`. The clock stays
     * paused, so nothing moves between the checks above and the picture.
     */
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name")
    }
}

/**
 * The Robolectric screen the pictures are drawn on, the same as [ScreenshotTest]'s: 411 dp wide,
 * 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val RESULT_PHONE = "w411dp-h891dp-xxhdpi"

/**
 * How far the paused clock is moved on once the photo is shown, in milliseconds: three times the
 * longest the success mark can take to settle (it takes under one second), so the mark is
 * pictured at rest on every run.
 */
private const val MARK_AT_REST_MS = 3_000L
