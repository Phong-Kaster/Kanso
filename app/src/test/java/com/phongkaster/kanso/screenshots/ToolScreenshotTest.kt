package com.phongkaster.kanso.screenshots

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
import com.phongkaster.kanso.tool.NoSavePhotoSaver
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
 * Takes the reference pictures of a tool screen showing a photo, with its Save button visible, in
 * light and in dark: the Edit screen (pictures 16 and 17, DoD 25), the Enhance screen opened from
 * its own key (pictures 22 and 23), where the title and the options strip are Enhance's, and the
 * Edit screen with a tall, narrow photo whose blurred copy fills the card beside it (pictures 40
 * and 41 on Android 15, 42 on Android 11, DoD 27).
 *
 * The photos are drawn in code and handed over by a [FakePhotoLoader], so the pictures never
 * depend on a file or on the decoder. The screen is hosted directly in KansoTheme with the mode
 * forced and the default colours (Kanso blue), on Android 11 unless a test says otherwise, and
 * the same phone screen as [ScreenshotTest].
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

    /** Picture 16: the Edit screen with a photo, in light mode. */
    @Test
    fun tool_photo_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        capture("16_tool_photo_light.png")
    }

    /** Picture 17: the Edit screen with a photo, in dark mode. */
    @Test
    fun tool_photo_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark)
        capture("17_tool_photo_dark.png")
    }

    /** Picture 22: the Enhance screen, opened from its own key, on the same photo, in light mode. */
    @Test
    fun editor_enhance_light() {
        showTool(Tool.ENHANCE, ThemeMode.Light)
        capture("22_editor_enhance_light.png")
    }

    /** Picture 23: the Enhance screen, opened from its own key, on the same photo, in dark mode. */
    @Test
    fun editor_enhance_dark() {
        showTool(Tool.ENHANCE, ThemeMode.Dark)
        capture("23_editor_enhance_dark.png")
    }

    /** Picture 40: the Edit screen with a narrow photo over its blurred copy, Android 15, light. */
    @Test
    @Config(sdk = [35])
    fun tool_narrow_photo_light() {
        showTool(Tool.EDIT, ThemeMode.Light, ToolFixtures.narrowStripes())
        capture("40_tool_narrow_photo_light.png")
    }

    /** Picture 41: the Edit screen with a narrow photo over its blurred copy, Android 15, dark. */
    @Test
    @Config(sdk = [35])
    fun tool_narrow_photo_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark, ToolFixtures.narrowStripes())
        capture("41_tool_narrow_photo_dark.png")
    }

    /**
     * Picture 42: the same on Android 11, where Compose's blur draws nothing and the stretched
     * tiny copy alone softens the space beside the photo, light.
     */
    @Test
    fun tool_narrow_photo_sdk30_light() {
        showTool(Tool.EDIT, ThemeMode.Light, ToolFixtures.narrowStripes())
        capture("42_tool_narrow_photo_sdk30_light.png")
    }

    /**
     * Shows [tool]'s screen on [photo] (by default the 600 x 400 gradient) in [mode], and waits
     * until the photo is drawn and Save can be tapped.
     */
    private fun showTool(tool: Tool, mode: ThemeMode, photo: Bitmap = ToolFixtures.gradient(600, 400)) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val loader = FakePhotoLoader(context, photo)
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = ToolViewModel(
            tool = tool,
            photoUri = "content://media/external/images/media/42",
            loader = loader,
            probe = ViewModelClearedProbe(),
            saver = NoSavePhotoSaver(),
        )
        compose.setContent {
            KansoTheme(themeMode = mode) {
                ToolScreen(viewModel = viewModel, onBack = {}, onSaved = {})
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(TOOL_IMAGE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed().assertIsEnabled()
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
