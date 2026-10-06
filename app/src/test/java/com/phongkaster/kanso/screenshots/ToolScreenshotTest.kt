package com.phongkaster.kanso.screenshots

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolViewModel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Takes the reference pictures of the Edit tool showing a photo, with its Save action visible,
 * in light and in dark (pictures 16 and 17, DoD 25).
 *
 * The photo is a 600 x 400 gradient drawn in code and handed over by a [FakePhotoLoader], so the
 * pictures never depend on a file or on the decoder. The screen is hosted directly in KansoTheme
 * with the mode forced and the baseline colours, on Android 11 and the same phone screen as
 * [ScreenshotTest].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = TOOL_PHONE)
class ToolScreenshotTest {

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

    /** Picture 16: the Edit tool with a photo, in light mode. */
    @Test
    fun tool_photo_light() {
        showToolWithPhoto(ThemeMode.Light)
        capture("16_tool_photo_light.png")
    }

    /** Picture 17: the Edit tool with a photo, in dark mode. */
    @Test
    fun tool_photo_dark() {
        showToolWithPhoto(ThemeMode.Dark)
        capture("17_tool_photo_dark.png")
    }

    /** Shows the Edit tool on the gradient photo in [mode] and waits until the photo is drawn. */
    private fun showToolWithPhoto(mode: ThemeMode) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = ToolViewModel(
            tool = Tool.EDIT,
            photoUri = "content://media/external/images/media/42",
            loader = loader,
            probe = ViewModelClearedProbe(),
        )
        compose.setContent {
            KansoTheme(themeMode = mode) {
                ToolScreen(viewModel = viewModel, onBack = {})
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(TOOL_IMAGE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed().assertIsNotEnabled()
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
private const val TOOL_PHONE = "w411dp-h891dp-xxhdpi"
