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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.edit.ProcessorRegistry
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.BrightenedProcessor
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.ENHANCE_COMPARE_TAG
import com.phongkaster.kanso.ui.tool.ENHANCE_RUN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.editRatioTag
import com.phongkaster.kanso.ui.tool.editToolTag
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.ui.tool.TOOL_DONE_TAG
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

    /** Picture 45: Remove background before it runs, light. */
    @Test
    fun tool_cutout_light() {
        showTool(Tool.REMOVE_BACKGROUND, ThemeMode.Light)
        capture("45_tool_cutout_light.png")
    }

    /** Picture 46: Remove background before it runs, dark. */
    @Test
    fun tool_cutout_dark() {
        showTool(Tool.REMOVE_BACKGROUND, ThemeMode.Dark)
        capture("46_tool_cutout_dark.png")
    }

    /** Picture 47: Retouch with its strength slider, light. */
    @Test
    fun tool_retouch_light() {
        showTool(Tool.RETOUCH, ThemeMode.Light)
        capture("47_tool_retouch_light.png")
    }

    /** Picture 48: Retouch with its strength slider, dark. */
    @Test
    fun tool_retouch_dark() {
        showTool(Tool.RETOUCH, ThemeMode.Dark)
        capture("48_tool_retouch_dark.png")
    }

    /** Picture 49: Artistic with its styles and strength slider, light. */
    @Test
    fun tool_artistic_light() {
        showTool(Tool.ARTISTIC, ThemeMode.Light)
        capture("49_tool_artistic_light.png")
    }

    /** Picture 50: Artistic with its styles and strength slider, dark. */
    @Test
    fun tool_artistic_dark() {
        showTool(Tool.ARTISTIC, ThemeMode.Dark)
        capture("50_tool_artistic_dark.png")
    }

    /** Picture 51: Edit with the Free crop controls open, light. */
    @Test
    fun tool_edit_free_crop_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        openFreeCrop()
        capture("51_tool_edit_free_crop_light.png")
    }

    /** Picture 52: Edit with the Free crop controls open, dark. */
    @Test
    fun tool_edit_free_crop_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark)
        openFreeCrop()
        capture("52_tool_edit_free_crop_dark.png")
    }

    /** Picture 53: Enhance after it ran, with the compare control, light. */
    @Test
    fun tool_enhance_done_light() {
        showTool(Tool.ENHANCE, ThemeMode.Light, registry = enhancedRegistry())
        runEnhance()
        capture("53_tool_enhance_done_light.png")
    }

    /** Picture 54: Enhance after it ran, with the compare control, dark. */
    @Test
    fun tool_enhance_done_dark() {
        showTool(Tool.ENHANCE, ThemeMode.Dark, registry = enhancedRegistry())
        runEnhance()
        capture("54_tool_enhance_done_dark.png")
    }

    /**
     * Opens the Crop sheet from the row of tools, then taps the Free crop chip, which opens the
     * rectangle sliders. Run 9: Edit's controls now live in sheets, so the sheet is opened first.
     */
    private fun openFreeCrop() {
        compose.onNodeWithTag(editToolTag(EditTool.CROP)).performClick()
        compose.onNodeWithTag(editRatioTag(CropRatio.FREE)).performScrollTo().performClick()
    }

    /** A registry whose Enhance is the instant [BrightenedProcessor]. */
    private fun enhancedRegistry() = ProcessorRegistry(mapOf(Tool.ENHANCE to BrightenedProcessor()))

    /** Taps Enhance's run button and waits for the compare control to appear. */
    private fun runEnhance() {
        // Enhance's panel does not scroll (unlike Edit's), so the button is tapped where it is.
        compose.onNodeWithTag(ENHANCE_RUN_TAG).performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(ENHANCE_COMPARE_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Shows [tool]'s screen on [photo] (by default the 600 x 400 gradient) in [mode], and waits
     * until the photo is drawn and Save can be tapped.
     */
    private fun showTool(
        tool: Tool,
        mode: ThemeMode,
        photo: Bitmap = ToolFixtures.gradient(600, 400),
        registry: ProcessorRegistry = ProcessorRegistry(),
    ) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val loader = FakePhotoLoader(context, photo)
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = ToolViewModel(
            tool = tool,
            photoUri = "content://media/external/images/media/42",
            loader = loader,
            probe = ViewModelClearedProbe(),
            registry = registry,
        )
        compose.setContent {
            KansoTheme(themeMode = mode) {
                ToolScreen(viewModel = viewModel, onBack = {}, onDone = {})
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(TOOL_IMAGE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(TOOL_DONE_TAG).assertIsDisplayed().assertIsEnabled()
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
