package com.phongkaster.kanso.screenshots

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performSemanticsAction
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.ui.tool.EDIT_BRIGHTNESS_TAG
import com.phongkaster.kanso.ui.tool.EDIT_COMPARE_TAG
import com.phongkaster.kanso.ui.tool.EDIT_PILL_TAG
import com.phongkaster.kanso.ui.tool.editFilterTag
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
 * Takes the reference pictures of a tool screen showing a photo, with its Done button visible, in
 * light and in dark: the Edit screen (pictures 16 and 17, DoD 25), the Enhance screen opened from
 * its own key (pictures 22 and 23), where the title and the options strip are Enhance's, and the
 * Edit screen with a tall, narrow photo whose blurred copy fills the card beside it (pictures 40
 * and 41 on Android 15, 42 on Android 11, DoD 27). Run 9 adds pictures 55 to 68: Edit's row of
 * tools, its Crop, Rotate, Adjust and Filters sheets, Compare, and the Undo and Compare pill.
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

    /** Picture 51: Edit with Free crop chosen, the cropper shown over the photo, light. */
    @Test
    fun tool_edit_free_crop_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        openFreeCrop()
        capture("51_tool_edit_free_crop_light.png")
    }

    /** Picture 52: Edit with Free crop chosen, the cropper shown over the photo, dark. */
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

    /** Picture 55: the Edit screen's row of tools with no sheet open, light. Run 9: DoD 22 (1). */
    @Test
    fun edit_tool_row_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        capture("55_edit_tool_row_light.png")
    }

    /** Picture 56: the row of tools with no sheet open, dark. Run 9: dark twin of picture 55. */
    @Test
    fun edit_tool_row_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark)
        capture("56_edit_tool_row_dark.png")
    }

    /** Picture 57: the Crop sheet open, light. Run 9: DoD 22 (2). */
    @Test
    fun edit_crop_sheet_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        openSheet(EditTool.CROP)
        capture("57_edit_crop_sheet_light.png")
    }

    /** Picture 58: the Crop sheet open, dark. Run 9: dark twin of picture 57. */
    @Test
    fun edit_crop_sheet_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark)
        openSheet(EditTool.CROP)
        capture("58_edit_crop_sheet_dark.png")
    }

    /** Picture 59: the Rotate sheet open, light. Run 9: DoD 22 (3). */
    @Test
    fun edit_rotate_sheet_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        openSheet(EditTool.ROTATE)
        capture("59_edit_rotate_sheet_light.png")
    }

    /** Picture 60: the Rotate sheet open, dark. */
    @Test
    fun edit_rotate_sheet_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark)
        openSheet(EditTool.ROTATE)
        capture("60_edit_rotate_sheet_dark.png")
    }

    /** Picture 61: the Adjust sheet with brightness at 70, light. Run 9: DoD 22 (4). */
    @Test
    fun edit_adjust_brightness_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        openSheet(EditTool.ADJUST)
        setBrightness70()
        capture("61_edit_adjust_brightness_light.png")
    }

    /** Picture 62: the Adjust sheet with brightness at 70, dark. */
    @Test
    fun edit_adjust_brightness_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark)
        openSheet(EditTool.ADJUST)
        setBrightness70()
        capture("62_edit_adjust_brightness_dark.png")
    }

    /** Picture 63: the Filters sheet with the 1977 filter chosen, light. Run 9: DoD 22 (5). */
    @Test
    fun edit_filters_1977_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        chooseFilter1977()
        capture("63_edit_filters_1977_light.png")
    }

    /** Picture 64: the Filters sheet with the 1977 filter chosen, dark. */
    @Test
    fun edit_filters_1977_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark)
        chooseFilter1977()
        capture("64_edit_filters_1977_dark.png")
    }

    /** Picture 65: Compare switched on after an edit, light. Run 9: DoD 22 (6). */
    @Test
    fun edit_compare_on_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        openSheet(EditTool.ADJUST)
        setBrightness70()
        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        capture("65_edit_compare_on_light.png")
    }

    /** Picture 66: Compare switched on after an edit, dark. */
    @Test
    fun edit_compare_on_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark)
        openSheet(EditTool.ADJUST)
        setBrightness70()
        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        capture("66_edit_compare_on_dark.png")
    }

    /** Picture 67: the Undo and Compare pill over the photo after an edit, light. Run 9: DoD 22 (8). */
    @Test
    fun edit_pill_light() {
        showTool(Tool.EDIT, ThemeMode.Light)
        openSheet(EditTool.ADJUST)
        setBrightness70()
        compose.onNodeWithTag(EDIT_PILL_TAG).assertIsDisplayed()
        capture("67_edit_pill_light.png")
    }

    /** Picture 68: the Undo and Compare pill over the photo after an edit, dark. */
    @Test
    fun edit_pill_dark() {
        showTool(Tool.EDIT, ThemeMode.Dark)
        openSheet(EditTool.ADJUST)
        setBrightness70()
        compose.onNodeWithTag(EDIT_PILL_TAG).assertIsDisplayed()
        capture("68_edit_pill_dark.png")
    }

    /** Taps [tool]'s entry in the row so its sheet opens. Run 9: the sheets are opened by the real UI. */
    private fun openSheet(tool: EditTool) {
        compose.onNodeWithTag(editToolTag(tool)).performClick()
        compose.waitForIdle()
    }

    /** Sets the Brightness slider to 0.7 (shown as 70) through its semantics, as AdjustSheetTest does. */
    private fun setBrightness70() {
        compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.7f) }
        compose.waitForIdle()
    }

    /**
     * Opens the Filters sheet and picks 1977. The filter preview renders on Dispatchers.Default, off
     * the test clock, and nothing in the semantics says when its bitmap has landed. So this polls in
     * real time (up to 10 s, checking every 50 ms) until the 1977 chip reports itself selected, then
     * gives the background render a short settle time before the picture is taken.
     */
    private fun chooseFilter1977() {
        openSheet(EditTool.FILTERS)
        val chip = compose.onNodeWithTag(editFilterTag(FilterPreset.SEVENTY_SEVEN))
        chip.performScrollTo().performClick()
        compose.waitForIdle()
        val deadline = System.currentTimeMillis() + FILTER_POLL_LIMIT_MILLIS
        while (!isSelected(chip) && System.currentTimeMillis() < deadline) {
            Thread.sleep(FILTER_POLL_STEP_MILLIS)
            compose.waitForIdle()
        }
        Thread.sleep(FILTER_SETTLE_MILLIS)
        compose.waitForIdle()
    }

    /** True when the chip's semantics say it is selected (a FilterChip writes this property). */
    private fun isSelected(chip: androidx.compose.ui.test.SemanticsNodeInteraction): Boolean =
        androidx.compose.ui.semantics.SemanticsProperties.Selected in chip.fetchSemanticsNode().config &&
            chip.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Selected]

    /**
     * Opens the Crop sheet from the row of tools, then taps the Free crop chip, which shows the
     * cropper over the photo (Run 9: the real cropper replaced the rectangle sliders). Edit's
     * controls live in sheets, so the sheet is opened first.
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
     * until the photo is drawn and Done can be tapped.
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

/** The longest real time, in milliseconds, [ToolScreenshotTest] polls for the chosen filter chip to be selected. */
private const val FILTER_POLL_LIMIT_MILLIS = 10_000L

/** Real milliseconds between two polls of the filter chip. */
private const val FILTER_POLL_STEP_MILLIS = 50L

/** Real milliseconds given to the filter's background render once the chip is selected (it runs off the test clock). */
private const val FILTER_SETTLE_MILLIS = 1_500L
