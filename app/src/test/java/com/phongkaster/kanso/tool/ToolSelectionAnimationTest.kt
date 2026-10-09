package com.phongkaster.kanso.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.editorToolTag
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks that selecting a tool in the editor's toolbar is animated, and that the animation ends
 * (DoD 12): the selected toggle changes its colours and its shape over a few frames, then stays
 * still.
 *
 * The test pauses Compose's test clock before showing anything, so time only moves when the test
 * moves it, frame by frame. It draws the screen into a bitmap at chosen moments and compares the
 * pixels inside the newly selected toggle: before the switch, 50 ms after it, 1 s after it and
 * 2 s after it. An animated change looks different at 50 ms from both its start and its end; a
 * finished one looks the same at 1 s and at 2 s. The switch is made twice: once by a tap on
 * Retouch, as a person would, and once on Artistic with no touch at all, so that a tap's own
 * ripple and pressed shape cannot pass for the selection's animation.
 *
 * It draws the activity's content view itself, as the settings tests do, because Compose's
 * captureToImage waits for the window to redraw and that wait times out under Robolectric here.
 * The screen runs in KansoTheme, so the toggle moves with the app's own expressive motion scheme.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ToolSelectionAnimationTest {

    /** Starts an empty activity to host the screen, and hands the test that activity. */
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

    /**
     * With Edit selected, a tap on Retouch turns its toggle on with a picture that is still
     * changing 50 ms later (different from before the tap and from the end), and that has
     * settled by 1 s (the same at 1 s and at 2 s, and different from before the tap).
     *
     * A tap alone also draws a ripple and squeezes the pressed button's shape, so this case on
     * its own would pass even if the selection itself never animated: the next test checks the
     * selection without any touch.
     */
    @Test
    fun selecting_a_tool_animates_its_toggle_and_then_settles() {
        showEditorOnEdit()

        assertSwitchAnimatesThenSettles(Tool.RETOUCH) {
            compose.onNodeWithTag(editorToolTag(Tool.RETOUCH)).performClick()
        }
    }

    /**
     * The same check with no touch at all: Artistic is selected straight through the ViewModel,
     * as the toolbar's tap would, so there is no ripple and no pressed shape. Only the selection
     * itself can make the Artistic toggle's picture differ, at 50 ms, from before the switch and
     * from 1 s, and it must be still between 1 s and 2 s.
     */
    @Test
    fun selecting_a_tool_without_a_touch_animates_its_toggle_and_then_settles() {
        val viewModel = showEditorOnEdit()

        assertSwitchAnimatesThenSettles(Tool.ARTISTIC) {
            compose.runOnUiThread { viewModel.selectTool(Tool.ARTISTIC) }
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the editor's ViewModel on Edit (outside any composable: lint forbids constructing a
     * ViewModel while drawing), pauses the clock, shows the editor in KansoTheme, waits for the
     * photo and lets everything that started with the screen finish. Returns the ViewModel.
     */
    private fun showEditorOnEdit(): ToolViewModel {
        val context: Context = ApplicationProvider.getApplicationContext()
        val viewModel = ToolViewModel(
            tool = Tool.EDIT,
            photoUri = PHOTO_URI,
            loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400)),
            probe = ViewModelClearedProbe(),
            saver = NoSavePhotoSaver(),
        )

        // Pause the clock before anything is shown: from here on, time moves only by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                ToolScreen(viewModel = viewModel, onBack = {}, onSaved = {})
            }
        }
        waitForPhoto()
        // Let everything that started with the screen finish before the first picture.
        advance(SETTLE_MS)
        return viewModel
    }

    /**
     * With Edit selected, draws [tool]'s toggle before [switchTo] runs and 50 ms, 1 s and 2 s
     * after it, then checks that [tool] is on and Edit off, and that the toggle's picture moved:
     * at 50 ms it differs from before and from 1 s, at 1 s it differs from before, and it is the
     * same at 1 s and at 2 s.
     */
    private fun assertSwitchAnimatesThenSettles(tool: Tool, switchTo: () -> Unit) {
        val toggle = compose.onNodeWithTag(editorToolTag(tool))
        toggle.assertIsOff()
        compose.onNodeWithTag(editorToolTag(Tool.EDIT)).assertIsOn()
        val before = pictureOf(tool)

        switchTo()
        advance(50)
        val early = pictureOf(tool)
        advance(950)
        val atOneSecond = pictureOf(tool)
        advance(1000)
        val atTwoSeconds = pictureOf(tool)

        toggle.assertIsOn()
        compose.onNodeWithTag(editorToolTag(Tool.EDIT)).assertIsOff()
        assertFalse("Nothing changed in the first 50 ms after the switch", before.contentEquals(early))
        assertFalse("The toggle had already finished changing 50 ms after the switch", early.contentEquals(atOneSecond))
        assertFalse("The selected toggle looks as it did before the switch", before.contentEquals(atOneSecond))
        assertTrue("The toggle was still changing after 1 s", atOneSecond.contentEquals(atTwoSeconds))
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
     * Moves the paused clock forward until the photo is on screen, so the picture of the toggle
     * is never taken while the photo area is still changing. Gives up after about ten seconds of
     * the test's clock.
     */
    private fun waitForPhoto() {
        repeat(100) {
            if (compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty()) return
            advance(100)
        }
        error("The photo never appeared")
    }

    /**
     * Draws the activity's content into a fresh bitmap and returns the pixels inside [tool]'s
     * toggle, row by row. The Compose root fills the content view from its top-left corner, so
     * the toggle's bounds in the root are its place in the bitmap.
     */
    private fun pictureOf(tool: Tool): IntArray {
        val bounds = compose.onNodeWithTag(editorToolTag(tool)).fetchSemanticsNode().boundsInRoot
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            // Keep the bounds inside the bitmap, so getPixels never reads outside it.
            val left = bounds.left.toInt().coerceIn(0, bitmap.width - 1)
            val top = bounds.top.toInt().coerceIn(0, bitmap.height - 1)
            val right = bounds.right.toInt().coerceIn(left + 1, bitmap.width)
            val bottom = bounds.bottom.toInt().coerceIn(top + 1, bitmap.height)
            val width = right - left
            val height = bottom - top
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, left, top, width, height)
            return pixels
        } finally {
            // A bitmap holds native memory; free it now rather than waiting for the collector.
            bitmap.recycle()
        }
    }

    /** Values the test uses. */
    private companion object {
        /** The photo the fake loader stands in for; nothing is read from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** How long the screen is left to settle before the first picture, in milliseconds. */
        const val SETTLE_MS = 2_000L
    }
}
