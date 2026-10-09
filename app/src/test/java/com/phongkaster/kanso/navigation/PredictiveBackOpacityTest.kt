package com.phongkaster.kanso.navigation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks, by reading drawn pixels, that during the back gesture (predictive back) the top screen
 * is drawn fully opaque and above the screen underneath, and that the screen underneath is drawn
 * behind it wherever the top screen has moved aside.
 *
 * Why pixels: keeping the top screen opaque is the reason kansoPredictivePopTransition (in
 * KansoTransitions.kt) gives it no fade-out. PredictiveBackGestureTest checks where the screens
 * are, but a screen that fades is still composed and still placed where it was, so a fade-out put
 * back on the top screen would pass every one of its checks. Only the drawn picture shows it.
 *
 * How the pixels are read: the host is PredictiveBackGestureTest's (NavDisplay with Kanso's three
 * transition specs, a back stack the test owns, the gesture's events sent into the activity's back
 * dispatcher), with two screens that are each one solid colour: Home, the screen underneath, in
 * red, and Settings, the top screen, in blue. The finger is moved to progress 0.3 and then to
 * 0.8, and each time the activity's content view is drawn into a bitmap and two pixels are read
 * on the top screen's vertical middle, each [SAMPLE_INSET_PX] pixels away from its moving left
 * edge: one inside the top screen, one left of it, over the screen underneath.
 * `GraphicsMode.Mode.NATIVE` switches on Android's real drawing code, so the bitmap holds real
 * pixels. Compose's captureToImage is not used: its wait for the window to redraw times out under
 * Robolectric here, so the test draws the content view itself, as the settings tests do.
 *
 * Why the pixel inside the top screen proves both opacity and drawing order: by progress 0.3 the
 * screen underneath has slid back to within a pixel of the left edge and fills the window, so that
 * point lies inside both screens (the test checks it does). Its colour is the top screen's blue
 * only if the top screen is drawn above the other one and lets none of it through: drawn below,
 * the point would be red; drawn above but partly see-through, a blend of the two. Both screens are
 * plain opaque fills placed on whole pixels, and every point read is far from every edge, where
 * nothing is smoothed, so the pixel must equal the colour exactly, with no tolerance. The pixel
 * left of the edge, red, shows that the screen underneath is really drawn where the top screen
 * has moved aside.
 *
 * What a fade-out would do: the plain pop fades its leaving screen out on the motion scheme's
 * effects spring, which is about nine tenths done after a tenth of a second. During the gesture
 * NavDisplay seeks the transition over its one-second running time, so at progress 0.3 that fade
 * would be 0.3 s in and long finished: the top screen would be fully see-through, the pixel
 * inside it would be the red of the screen underneath, and this test would fail.
 *
 * What a machine cannot say: how the preview feels under a real finger. This test proves what is
 * drawn where while the gesture is held; a person judges how it feels on the phone (DoD 21,
 * step 8).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PredictiveBackOpacityTest {

    /**
     * Starts an empty activity to host NavDisplay, and hands the test that activity: its back
     * dispatcher receives the gesture's events and its content view is what gets drawn.
     */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Stops Koin after the test: the test does not use Koin, but KansoApplication starts it for
     * every Robolectric test, and its state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTheTest() {
        stopKoin()
    }

    /**
     * At progress 0.3 and at progress 0.8 of the back gesture, the pixel inside the top screen is
     * exactly Settings' blue and the pixel left of its edge is exactly Home's red. Before reading,
     * the test checks its premises: Home is composed, the top screen sits at the finger's share of
     * the width, the top screen's remaining strip is wide enough to read a pixel [SAMPLE_INSET_PX]
     * pixels away from both its edges (at 0.8 a fifth of the width is left), and both points lie
     * inside Home. At the end the gesture is called off and the cancel is let finish, so no
     * gesture is left in progress.
     */
    @Test
    fun during_the_back_gesture_the_top_screen_is_drawn_opaque_over_the_screen_underneath() {
        val backStack = mutableStateListOf<NavKey>(Home, Settings)
        show(backStack)
        val windowWidth = width(SETTINGS_TAG)
        startGesture()

        for (progress in SAMPLED_PROGRESS) {
            moveFingerTo(progress, windowWidth)
            assertTrue("At progress $progress the screen underneath is not composed", exists(HOME_TAG))
            val topLeft = left(SETTINGS_TAG)
            // The premise: the gesture really moved the top screen to the finger's share of the width.
            assertEquals(
                "At progress $progress the top screen should be that share of the width ($windowWidth) along",
                progress * windowWidth,
                topLeft,
                POSITION_TOLERANCE_PX,
            )

            // Two points on the top screen's vertical middle, SAMPLE_INSET_PX either side of its
            // moving left edge: one inside the top screen, one over the screen underneath.
            val y = middleY(SETTINGS_TAG)
            val insideTopX = topLeft.roundToInt() + SAMPLE_INSET_PX
            val underneathX = topLeft.roundToInt() - SAMPLE_INSET_PX
            // The premise: what is left of the top screen on the window is wide enough to read a
            // pixel away from both of its edges. A test that cannot set its premise fails.
            assertTrue(
                "At progress $progress the top screen's strip, from x $topLeft to the window's edge at " +
                    "$windowWidth, is too narrow to read a pixel $SAMPLE_INSET_PX px away from both its edges",
                insideTopX + SAMPLE_INSET_PX <= windowWidth,
            )
            // The premise: both points lie inside the screen underneath, so the inner one shows the
            // top screen's colour only if the top screen is drawn over it and lets none of it through.
            val homeLeft = left(HOME_TAG)
            val homeRight = homeLeft + width(HOME_TAG)
            assertTrue(
                "At progress $progress the screen underneath spans x $homeLeft to $homeRight, which does " +
                    "not hold both points (x $underneathX and x $insideTopX)",
                underneathX >= homeLeft && insideTopX < homeRight,
            )

            val (insideTop, underneath) = drawnPixelsOnRow(y, listOf(insideTopX, underneathX))
            println(
                "PredictiveBackOpacityTest: progress $progress: top screen at x $topLeft; " +
                    "x $insideTopX drawn ${hex(insideTop)}, x $underneathX drawn ${hex(underneath)} (row $y)",
            )
            assertDrawnColour(
                "At progress $progress the pixel inside the top screen (x $insideTopX, y $y) is not the top " +
                    "screen's own colour: the top screen is drawn see-through, or below the screen underneath",
                SETTINGS_COLOUR,
                insideTop,
            )
            assertDrawnColour(
                "At progress $progress the pixel left of the top screen (x $underneathX, y $y) is not the colour " +
                    "of the screen underneath: that screen is not drawn where the top one has moved aside",
                HOME_COLOUR,
                underneath,
            )
        }

        // Call the gesture off and let the cancel finish, so the activity closes with no gesture
        // in progress.
        onDispatcher { it.dispatchOnBackCancelled() }
        stepUntil("the screen underneath to be gone after the cancel", FINISH_FRAMES) { !exists(HOME_TAG) }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Pauses the clock, shows NavDisplay over [backStack] inside Kanso's theme, and steps the
     * clock frame by frame for about a second, so the test begins from a resting screen.
     */
    private fun show(backStack: SnapshotStateList<NavKey>) {
        // Pause the clock before anything is shown: from here on, time moves only by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                TwoSolidScreensNavDisplay(backStack)
            }
        }
        repeat(SETTLE_FRAMES) { nextFrame() }
    }

    /**
     * Starts the back gesture at the left edge: sends `dispatchOnBackStarted` with progress 0,
     * steps the clock until the screen underneath is composed behind the top one (failing after
     * [START_FRAMES] frames), then waits [FRAMES_PER_STEP] more frames, as after every move of the
     * finger, so the gesture goes on from a settled screen.
     */
    private fun startGesture() {
        // The finger's position (x, y), the progress, and the edge the swipe starts from.
        onDispatcher { it.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT)) }
        stepUntil("the screen underneath to be composed after the gesture started", START_FRAMES) {
            exists(HOME_TAG)
        }
        repeat(FRAMES_PER_STEP) { nextFrame() }
    }

    /**
     * Moves the finger to [progress] of the gesture: sends `dispatchOnBackProgressed` with that
     * progress, and a finger at the same share of [windowWidth] as in a swipe from the left edge,
     * then steps [FRAMES_PER_STEP] frames, by which time NavDisplay has moved the screens to it.
     */
    private fun moveFingerTo(progress: Float, windowWidth: Float) {
        onDispatcher {
            it.dispatchOnBackProgressed(
                BackEventCompat(progress * windowWidth, 0f, progress, BackEventCompat.EDGE_LEFT),
            )
        }
        repeat(FRAMES_PER_STEP) { nextFrame() }
    }

    /**
     * Hands [send] the back dispatcher of the activity that hosts NavDisplay, on the main thread.
     * The dispatcher's test functions the test calls through it stand in for the system's events.
     */
    private fun onDispatcher(send: (OnBackPressedDispatcher) -> Unit) {
        compose.activityRule.scenario.onActivity { activity -> send(activity.onBackPressedDispatcher) }
    }

    /**
     * Moves the paused clock one frame at a time until [condition] holds, checking it before the
     * first step too. Fails after [maxFrames] frames, naming [what] it was waiting for.
     */
    private fun stepUntil(what: String, maxFrames: Int, condition: () -> Boolean) {
        var frames = 0
        while (!condition()) {
            if (frames == maxFrames) {
                throw AssertionError("Waited $maxFrames frames for $what")
            }
            nextFrame()
            frames++
        }
    }

    /**
     * Moves the paused clock forward by one frame and runs whatever the main thread was asked to
     * do meanwhile (under Robolectric the test runs on the main thread, so its queue only moves
     * when the test runs it).
     */
    private fun nextFrame() {
        compose.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Tells whether a screen tagged [tag] is composed, wherever it is drawn. */
    private fun exists(tag: String): Boolean = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /**
     * Returns the left edge of the screen tagged [tag], in pixels from the left of the window. A
     * slide moves where a screen is placed, so this follows the gesture as it goes.
     */
    private fun left(tag: String): Float = compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.x

    /** Returns the width of the screen tagged [tag], in pixels: the window's width. */
    private fun width(tag: String): Float = compose.onNodeWithTag(tag).fetchSemanticsNode().size.width.toFloat()

    /**
     * Returns the row of pixels through the vertical middle of the screen tagged [tag], counted
     * from the top of the window: far from its top and bottom edges.
     */
    private fun middleY(tag: String): Int {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        return (node.positionInRoot.y + node.size.height / 2f).roundToInt()
    }

    /**
     * Draws the activity's content view into a fresh bitmap and returns the colour, as an ARGB
     * number, of the pixel at each of [xs] on row [y], in order. The Compose root fills the content
     * view from its top-left corner, so a position in the root is a position in the bitmap. Fails
     * if a point lies outside the drawn picture, rather than reading somewhere else.
     */
    private fun drawnPixelsOnRow(y: Int, xs: List<Int>): List<Int> {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            // Draw the content view into the bitmap, as Android would draw it on the screen.
            view.draw(Canvas(bitmap))
            return xs.map { x ->
                check(x in 0 until bitmap.width && y in 0 until bitmap.height) {
                    "($x, $y) lies outside the drawn picture of ${bitmap.width} x ${bitmap.height}"
                }
                bitmap.getPixel(x, y)
            }
        } finally {
            // A bitmap holds native memory; free it now rather than waiting for the collector.
            bitmap.recycle()
        }
    }

    /**
     * Fails with [message] unless the drawn pixel [drawnArgb] is exactly [expected]. Both are
     * compared written as `#AARRGGBB`, so a failure names the two colours.
     */
    private fun assertDrawnColour(message: String, expected: Color, drawnArgb: Int) {
        assertEquals(message, hex(expected.toArgb()), hex(drawnArgb))
    }

    /** Writes an ARGB colour number as `#AARRGGBB`, for messages. */
    private fun hex(argb: Int): String = "#%08X".format(argb)

    /**
     * NavDisplay over [backStack] with Kanso's three transition specs, built the way the app
     * builds them: the theme's motion scheme is read here, while Compose draws, and handed to the
     * spec functions, whose lambdas NavDisplay calls later. Back removes the top key. Home is
     * drawn in [HOME_COLOUR] and Settings in [SETTINGS_COLOUR], each as a [SolidScreen].
     */
    @Composable
    private fun TwoSolidScreensNavDisplay(backStack: SnapshotStateList<NavKey>) {
        val motionScheme = MaterialTheme.motionScheme
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = { backStack.removeLastOrNull() },
            transitionSpec = kansoPushTransition(motionScheme),
            popTransitionSpec = kansoPopTransition(motionScheme),
            predictivePopTransitionSpec = kansoPredictivePopTransition(motionScheme),
            entryProvider = entryProvider<NavKey> {
                entry<Home> { SolidScreen(tag = HOME_TAG, colour = HOME_COLOUR) }
                entry<Settings> { SolidScreen(tag = SETTINGS_TAG, colour = SETTINGS_COLOUR) }
            },
        )
    }

    /**
     * A screen that fills the window with one opaque [colour] and nothing else (no text, whose
     * smoothed letters could land on a point the test reads), tagged [tag] so the test can find it.
     */
    @Composable
    private fun SolidScreen(tag: String, colour: Color) {
        Box(modifier = Modifier.fillMaxSize().background(colour).testTag(tag))
    }

    /** Tags, colours, gesture positions, frame counts and distances the test uses. */
    private companion object {
        /** Marks the Home screen, the one underneath. */
        const val HOME_TAG = "back-opacity-test-home"

        /** Marks the Settings screen, the top one that the gesture moves. */
        const val SETTINGS_TAG = "back-opacity-test-settings"

        /** Home's colour, an opaque red: what the screen underneath draws everywhere. */
        val HOME_COLOUR = Color(0xFFC62828)

        /**
         * Settings' colour, an opaque blue: what the top screen draws everywhere. It is far from
         * Home's red in every colour channel, so any blend of the two is told apart from either.
         */
        val SETTINGS_COLOUR = Color(0xFF1565C0)

        /** How many frames the freshly shown screen is left to settle: 60 frames, about a second. */
        const val SETTLE_FRAMES = 60

        /**
         * The most frames the screen underneath may take to appear once the gesture starts: 10
         * frames, 160 ms (PredictiveBackGestureTest saw it take two).
         */
        const val START_FRAMES = 10

        /** How many frames after each move of the finger the test looks: 4 frames, 64 ms. */
        const val FRAMES_PER_STEP = 4

        /**
         * The most frames the closing cancel may take: 120 frames, about 1.9 s. A cancel from far
         * along took about 0.85 s in the probe written before PredictiveBackGestureTest.
         */
        const val FINISH_FRAMES = 120

        /**
         * The progress values at which the pixels are read: 0.3, where a fade-out on the effects
         * spring would long be finished (0.3 s into a fade that is nine tenths done after 0.1 s),
         * and 0.8, where only a fifth of the top screen is still on the window.
         */
        val SAMPLED_PROGRESS = listOf(0.3f, 0.8f)

        /**
         * How far, in pixels, each point read lies from the top screen's moving left edge, on
         * either side. Edges are where drawing may smooth one colour into the next; 24 pixels away
         * there is only the solid fill.
         */
        const val SAMPLE_INSET_PX = 24

        /**
         * How far, in pixels, the top screen may be from where the gesture should have put it:
         * screens are placed on whole pixels, and PredictiveBackGestureTest finds it exactly on
         * its mark.
         */
        const val POSITION_TOLERANCE_PX = 2f
    }
}
