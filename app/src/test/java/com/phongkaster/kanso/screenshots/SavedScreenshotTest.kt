package com.phongkaster.kanso.screenshots

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.saved.SAVED_HOME_TAG
import com.phongkaster.kanso.ui.saved.SAVED_MARK_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SHARE_TAG
import com.phongkaster.kanso.ui.saved.SavedScreen
import com.phongkaster.kanso.ui.saved.SavedViewModel
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
 * Takes the reference pictures of the Saved screen showing a saved photo with its Share, Edit
 * another and Home actions, in light and in dark (pictures 18 and 19, DoD 26), with the success
 * mark above the photo at rest (DoD 7).
 *
 * The photo is a 600 x 400 gradient drawn in code and handed over by a [FakePhotoLoader], given
 * to the screen's [SavedViewModel], so the pictures never depend on a file or on the decoder. The
 * screen is hosted directly in KansoTheme with the mode forced and the baseline colours, on
 * Android 11 and the same phone screen as [ScreenshotTest].
 *
 * The test clock is paused before anything is drawn (`mainClock.autoAdvance = false`) and moved
 * only by hand, for two reasons. While the photo is decoded the screen shows a loading indicator
 * that never stops by itself, and with the clock running by itself a wait for the screen to settle
 * would wait for it forever. And the success mark animates once when the screen appears: the
 * clock is moved on by [MARK_AT_REST_MS], well past the end of that animation, so every recording
 * shows the mark in the same, final state.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = SAVED_PHONE)
class SavedScreenshotTest {

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

    /** Picture 18: the Saved screen, in light mode. */
    @Test
    fun saved_light() {
        showSaved(ThemeMode.Light)
        capture("18_saved_light.png")
    }

    /** Picture 19: the Saved screen, in dark mode. */
    @Test
    fun saved_dark() {
        showSaved(ThemeMode.Dark)
        capture("19_saved_dark.png")
    }

    /**
     * Shows the Saved screen on the gradient photo in [mode] with the clock paused, moves the
     * clock on until the photo is drawn, then on by [MARK_AT_REST_MS] so the mark is at rest.
     */
    private fun showSaved(mode: ThemeMode) {
        val context: Context = ApplicationProvider.getApplicationContext()
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = SavedViewModel(
            photoUri = "content://media/external/images/media/42",
            loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400)),
            probe = ViewModelClearedProbe(),
        )
        // Paused before setContent, so the first frame is already under the test's control (see
        // the class comment).
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = mode) {
                SavedScreen(
                    viewModel = viewModel,
                    onShare = {},
                    onEditAnother = {},
                    onHome = {},
                )
            }
        }
        waitForPhoto()
        advance(MARK_AT_REST_MS)

        compose.onNodeWithTag(SAVED_PHOTO_TAG).assertIsDisplayed()
        compose.onNodeWithTag(SAVED_MARK_TAG).assertIsDisplayed()
        compose.onNodeWithTag(SAVED_SHARE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(SAVED_HOME_TAG).assertIsDisplayed()
    }

    /**
     * Moves the paused clock forward until the photo is on screen. The ViewModel's load is started
     * by the screen and ends on the main thread, so the clock is moved and the main thread's queue
     * is run in turn. Gives up after about ten seconds of the test's clock.
     */
    private fun waitForPhoto() {
        repeat(100) {
            if (compose.onAllNodes(hasTestTag(SAVED_PHOTO_TAG)).fetchSemanticsNodes().isNotEmpty()) return
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
private const val SAVED_PHONE = "w411dp-h891dp-xxhdpi"

/**
 * How far the paused clock is moved on once the photo is shown, in milliseconds: three times the
 * longest the success mark can take to settle (it takes under one second), so the mark is
 * pictured at rest on every run.
 */
private const val MARK_AT_REST_MS = 3_000L
