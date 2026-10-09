package com.phongkaster.kanso.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
// The system settings class goes by another name here, as in NavTransitionTest, where the plain
// name would hide Kanso's own Settings screen key.
import android.provider.Settings as SystemSettings
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.MotionDurationScale
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
import org.junit.Assert.assertEquals
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
 * A third test is the "Remove animations" pass of DoD 13 (machine part), built like the one in
 * NavTransitionTest. The phone's "Remove animations" setting turns the system's animator duration
 * scale to 0; the test sets it so before anything is shown, then switches to Artistic without a
 * touch. Compose only honours the setting if the test's host hands it to Compose's coroutines as a
 * `MotionDurationScale`, which a Robolectric host may not do. So a control runs next to the
 * editor: a plain one-second animation of a number, started together with the switch. The control
 * is what tells the test whether the host carries the setting. If it reaches its end within a few
 * frames, the host does, and the toggle must have settled just as quickly. If it is still on its
 * way, the host does not; the test prints that this check is "not driven by machine" and asserts
 * that the toggle still animates like any other. Both branches assert something.
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
     * Puts the system's animator duration scale back to 1, its normal value, so no later test
     * starts with animations removed. Only the Remove-animations test changes it; for the two
     * others this writes the normal value they already read, so it changes nothing for them.
     */
    @After
    fun restoreAnimatorDurationScale() {
        setAnimatorDurationScale(1f)
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

    /**
     * The "Remove animations" pass (DoD 13, machine part), with the same switch as the test
     * above: Artistic selected through the ViewModel, with no touch. The phone's setting is the
     * animator duration scale, set here to 0 before anything is shown. Compose honours it only if
     * the host hands it to Compose's coroutines as a `MotionDurationScale`; a Compose test builds
     * its own host, which may not.
     *
     * So a control runs inside the same composition as the editor: a plain one-second animation
     * of a number, started together with the switch. The control is what tells the test whether
     * the host carries the setting. If it has reached its end a few frames after the switch, the
     * host does, and the Artistic toggle must have settled as quickly: its picture then is the
     * same as 1 s later, and different from before the switch. If the control is still on its
     * way, the host does not; the test prints that this check is "not driven by machine" (which
     * DoD 13 does not count as a failure) and asserts what that implies, that the selection is
     * still animating like any other: a few frames in, the toggle has started to change and has
     * not yet reached its picture of 1 s later. Either way the outcome is asserted, so a host that
     * starts carrying the setting changes which branch runs, and what the printed line says.
     */
    @Test
    fun with_animations_removed_a_tool_switch_settles_at_once_if_the_host_carries_the_setting() {
        // Remove animations before anything is shown, as the phone's setting would have, and
        // check the system really holds the new value.
        setAnimatorDurationScale(0f)
        assertEquals(
            "The test could not set the system's animation scale to 0",
            0f,
            SystemSettings.Global.getFloat(
                ApplicationProvider.getApplicationContext<Context>().contentResolver,
                SystemSettings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ),
            0f,
        )

        // A state the test writes to start the control: its animation runs towards this value.
        val controlTarget = mutableFloatStateOf(0f)
        // Set while Compose draws: the control's current value, readable by the test afterwards.
        lateinit var control: State<Float>
        var effectRan = false
        var effectScale: Float? = null

        val viewModel = showEditorOnEdit {
            // The control: a one-second animation of a plain number, in the same composition and
            // on the same paused clock as the editor. It draws nothing, so it adds nothing on
            // screen and changes no picture of the toggle.
            control = animateFloatAsState(
                targetValue = controlTarget.floatValue,
                animationSpec = tween(durationMillis = CONTROL_MS, easing = LinearEasing),
                label = "control",
            )
            // Reads the scale Compose's animations see: they run in coroutines like this one, and
            // read it from their context (none at all means a scale of 1).
            LaunchedEffect(Unit) {
                effectScale = coroutineContext[MotionDurationScale]?.scaleFactor
                effectRan = true
            }
        }
        assertTrue("The effect that reads the animation scale never ran", effectRan)

        val toggle = compose.onNodeWithTag(editorToolTag(Tool.ARTISTIC))
        toggle.assertIsOff()
        compose.onNodeWithTag(editorToolTag(Tool.EDIT)).assertIsOn()
        val before = pictureOf(Tool.ARTISTIC)

        // Start the control and the switch together, in one go on the UI thread, as the test
        // without a touch selects the tool.
        compose.runOnUiThread {
            controlTarget.floatValue = 1f
            viewModel.selectTool(Tool.ARTISTIC)
        }
        repeat(REMOVED_ANIMATION_FRAMES) { nextFrame() }
        // Read the control and draw the toggle at the same moment, before any more time passes.
        val controlAfterFrames = control.value
        val afterFrames = pictureOf(Tool.ARTISTIC)
        advance(SETTLED_MS)
        val settled = pictureOf(Tool.ARTISTIC)

        val controlFinishedAtOnce = controlAfterFrames == 1f
        assertEquals(
            "The scale in Compose's coroutines ($effectScale) does not match what the control did",
            effectScale == 0f,
            controlFinishedAtOnce,
        )
        if (controlFinishedAtOnce) {
            println(
                "ToolSelectionAnimationTest: the host carries ANIMATOR_DURATION_SCALE into Compose " +
                    "(MotionDurationScale $effectScale): the tool switch was checked with animations removed",
            )
            assertTrue(
                "With animations removed the toggle should have settled at once, yet its picture " +
                    "$REMOVED_ANIMATION_FRAMES frames after the switch differs from its picture 1 s later",
                afterFrames.contentEquals(settled),
            )
            assertFalse(
                "With animations removed the toggle should look selected at once, yet " +
                    "$REMOVED_ANIMATION_FRAMES frames after the switch it looks as it did before",
                before.contentEquals(afterFrames),
            )
        } else {
            println(
                "ToolSelectionAnimationTest: not driven by machine. This Robolectric host does not carry " +
                    "ANIMATOR_DURATION_SCALE into Compose (MotionDurationScale ${effectScale ?: "absent"}, " +
                    "the control animation was at $controlAfterFrames after the switch), so the " +
                    "Remove-animations pass of DoD 13 is left to the person on the phone",
            )
            assertFalse(
                "The host does not carry the animation setting, so the toggle should be animating " +
                    "like any other, yet nothing changed in the first $REMOVED_ANIMATION_FRAMES frames",
                before.contentEquals(afterFrames),
            )
            assertFalse(
                "The host does not carry the animation setting, so the toggle should still be " +
                    "animating like any other, yet $REMOVED_ANIMATION_FRAMES frames after the switch " +
                    "it already looks as it does 1 s later",
                afterFrames.contentEquals(settled),
            )
        }

        // Whatever the host, both moves end: the control at its target, Artistic on and Edit
        // off, and the Artistic toggle looking selected.
        assertEquals("The control animation never reached its target", 1f, control.value, 0.001f)
        toggle.assertIsOn()
        compose.onNodeWithTag(editorToolTag(Tool.EDIT)).assertIsOff()
        assertFalse("The selected toggle looks as it did before the switch", before.contentEquals(settled))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the editor's ViewModel on Edit (outside any composable: lint forbids constructing a
     * ViewModel while drawing), pauses the clock, shows the editor in KansoTheme, waits for the
     * photo and lets everything that started with the screen finish. Returns the ViewModel.
     *
     * [alongside] is composed next to the editor, inside the same theme and composition, so it
     * runs on the same clock: the Remove-animations test puts its control there. It must draw
     * nothing, so that the screen and the toggle's pictures stay exactly the editor's. By default
     * it is empty, and the editor is shown alone.
     */
    private fun showEditorOnEdit(alongside: @Composable () -> Unit = {}): ToolViewModel {
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
                // What the test asked to run next to the editor; nothing unless it asked.
                alongside()
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
     * Moves the paused clock forward by exactly one frame (16 ms) and runs whatever the main
     * thread was asked to do meanwhile (under Robolectric the test runs on the main thread, so its
     * queue only moves when the test runs it).
     */
    private fun nextFrame() {
        compose.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Sets the system's animator duration scale, the value the phone's "Remove animations"
     * setting turns to 0, through the app context's settings store.
     */
    private fun setAnimatorDurationScale(scale: Float) {
        val context: Context = ApplicationProvider.getApplicationContext()
        SystemSettings.Global.putFloat(context.contentResolver, SystemSettings.Global.ANIMATOR_DURATION_SCALE, scale)
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

        /**
         * How long the control animation of the Remove-animations test takes at normal speed, in
         * milliseconds. The control is what tells the test whether the host carries the setting:
         * at a scale of 0 it ends in its first frame; at normal speed it is far from its end a
         * few frames in.
         */
        const val CONTROL_MS = 1_000

        /**
         * How many frames after the switch the Remove-animations test looks: 6 frames, about
         * 100 ms. That is enough for the switch to reach the screen and, with animations removed,
         * for the toggle to settle; at normal speed the selection, about 0.3 s on the Expressive
         * springs, is still on its way.
         */
        const val REMOVED_ANIMATION_FRAMES = 6

        /**
         * How long after the switch the Remove-animations test takes the toggle's settled
         * picture, in milliseconds: the toggle's springs end well within a second.
         */
        const val SETTLED_MS = 1_000L
    }
}
