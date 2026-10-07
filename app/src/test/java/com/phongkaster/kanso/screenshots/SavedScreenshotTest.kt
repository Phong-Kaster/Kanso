package com.phongkaster.kanso.screenshots

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.saved.SAVED_HOME_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SHARE_TAG
import com.phongkaster.kanso.ui.saved.SavedScreen
import com.phongkaster.kanso.ui.theme.KansoTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Takes the reference pictures of the Saved screen showing a saved photo with its Share, Edit
 * another and Home actions, in light and in dark (pictures 18 and 19, DoD 26).
 *
 * The photo is a 600 x 400 gradient drawn in code and handed over by a [FakePhotoLoader], swapped
 * into Koin, so the pictures never depend on a file or on the decoder. The screen is hosted
 * directly in KansoTheme with the mode forced and the baseline colours, on Android 11 and the
 * same phone screen as [ScreenshotTest].
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

    /** Shows the Saved screen on the gradient photo in [mode] and waits until the photo is drawn. */
    private fun showSaved(mode: ThemeMode) {
        val context: Context = ApplicationProvider.getApplicationContext()
        // Koin lets a later module replace an earlier recipe with the same type.
        loadKoinModules(module { single<PhotoLoader> { FakePhotoLoader(context, ToolFixtures.gradient(600, 400)) } })
        compose.setContent {
            KansoTheme(themeMode = mode) {
                SavedScreen(
                    tool = Tool.EDIT,
                    photoUri = "content://media/external/images/media/42",
                    onShare = {},
                    onEditAnother = {},
                    onHome = {},
                )
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(SAVED_PHOTO_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(SAVED_PHOTO_TAG).assertIsDisplayed()
        compose.onNodeWithTag(SAVED_SHARE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(SAVED_HOME_TAG).assertIsDisplayed()
    }

    /** Saves a picture of the whole window as `src/test/screenshots/[name]`. */
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
