package com.phongkaster.kanso.navigation

import android.os.Looper
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Drives the back gesture (predictive back) through Kanso's screen transitions and checks what
 * the screens do while the finger moves, when the gesture is called off and when the person lets
 * go. It extends the machine part of DoD 14: [NavTransitionTest] checks the push, the pop and a
 * plain Back press; this test checks the preview the back gesture shows, which is the spec that
 * kansoPredictivePopTransition in KansoTransitions.kt builds.
 *
 * How a test makes the gesture: on a phone, Android turns a swipe in from the edge into a series
 * of back events. Each event is a `BackEventCompat`: where the finger is (x and y), the gesture's
 * progress (a number that grows from 0, the finger has just landed, towards 1, the finger has
 * crossed the screen) and the edge the swipe started from. The activity's back dispatcher
 * (`OnBackPressedDispatcher`) passes such events on to whatever registered for Back, here
 * NavDisplay. The test sends the events into that dispatcher, in the very activity that hosts
 * NavDisplay, through the dispatcher's public functions made for tests, which stand in for the
 * system: `dispatchOnBackStarted` (the finger lands and starts to move),
 * `dispatchOnBackProgressed` (the finger has moved; sent again at each new position),
 * `dispatchOnBackCancelled` (the person slid back and let go, so Back is called off) and, for
 * letting go after a far enough swipe, the dispatcher's ordinary `onBackPressed`, the same call
 * that completes a press of the Back button. From the dispatcher on, the events reach NavDisplay
 * as a gesture's events do. On a phone, though, the system's events enter the dispatcher through
 * another input: the platform's back callback, which the dispatcher registers with the window and
 * through which the system hands it a gesture's progress (on the owner's Android 14, only to an
 * app whose manifest switches predictive back on). That handoff, from the system to the
 * dispatcher, is not driven here, so this test does not check it: the manifest flag is proved by
 * the `aapt2` check of DoD 14, and a person checks the real swipe on the phone (DoD 21, step 8).
 *
 * The host is the same as NavTransitionTest's: NavDisplay with Kanso's three transition specs,
 * two plain screens (Home under Settings, each a tagged box with its name) and a back stack the
 * test owns, with no MainActivity, no Koin and no ViewModel, so the test depends only on the specs.
 *
 * Why every wait steps one frame at a time: the test pauses Compose's clock before anything is
 * shown, so time moves only when the test moves it. When the gesture was first driven, in a
 * throwaway probe written before this test, moving the clock by three seconds in one step after a
 * cancel left the screen underneath composed, while stepping one frame (16 ms) at a time, and
 * running the main thread's queue after each frame, finished the cancel in about 0.85 s. So every
 * wait here is a loop of single frames with a generous limit, and fails naming what it waited for.
 *
 * What a machine cannot say: whether the preview feels right under a real finger. Android shows
 * it only from Android 14 on, with gesture navigation; how a real swipe feels (the pace, the
 * moment the finger lifts) is judged by a person on the phone (DoD 21, step 8). This test proves
 * where the screens are; the person judges how it feels.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PredictiveBackGestureTest {

    /**
     * Starts an empty activity to host NavDisplay. Unlike the plain compose rule, this one hands
     * the test that activity, whose back dispatcher the test sends the gesture's events through.
     */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Stops Koin after each test: the test does not use Koin, but KansoApplication starts it for
     * every Robolectric test, and its state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTheTest() {
        stopKoin()
    }

    /**
     * While the finger moves, the top screen follows it and the screen underneath shows. Once the
     * gesture starts, Home is composed behind Settings within a few frames while Settings stays
     * composed. Then, at progress 0.1, 0.2, 0.5 and 0.8, the top screen's left edge sits at that
     * share of its width (the straight-line slide of kansoPredictivePopTransition: progress 0.2 is
     * a fifth of the width), it never moves back left from one step to the next, Home is composed
     * at every step, and from progress 0.5 on Home rests at the left edge. That is what keeps the
     * preview at the finger's pace instead of running ahead of it. At the end the gesture is
     * called off, so the test leaves no gesture in progress.
     */
    @Test
    fun the_back_gesture_moves_the_top_screen_with_the_finger_and_shows_the_screen_underneath() {
        val backStack = mutableStateListOf<NavKey>(Home, Settings)
        show(backStack)
        assertOnlyScreen(shown = SETTINGS_TAG, gone = HOME_TAG)
        val windowWidth = width(SETTINGS_TAG)

        val frames = startGesture()
        println("PredictiveBackGestureTest: the screen underneath was composed $frames frame(s) after the gesture started")
        compose.onNodeWithTag(SETTINGS_TAG).assertExists()

        var previousLeft = left(SETTINGS_TAG)
        for (progress in PROGRESS_STEPS) {
            moveFingerTo(progress, windowWidth)
            val topLeft = left(SETTINGS_TAG)
            val topWidth = width(SETTINGS_TAG)
            println("PredictiveBackGestureTest: progress $progress: top screen at x $topLeft of $topWidth")

            assertTrue("At progress $progress the screen underneath is not composed", exists(HOME_TAG))
            assertEquals(
                "At progress $progress the top screen should be that share of its width ($topWidth) along",
                progress * topWidth,
                topLeft,
                POSITION_TOLERANCE_PX,
            )
            assertTrue(
                "The top screen moved left between steps, from $previousLeft to $topLeft at progress $progress",
                topLeft >= previousLeft,
            )
            if (progress >= UNDERNEATH_RESTS_FROM_PROGRESS) {
                assertEquals(
                    "At progress $progress the screen underneath should rest at the left edge",
                    0f,
                    left(HOME_TAG),
                    POSITION_TOLERANCE_PX,
                )
            }
            previousLeft = topLeft
        }

        // Call the gesture off and let the cancel finish, so the activity closes with no gesture
        // in progress. The cancel itself is checked by the next test.
        onDispatcher { it.dispatchOnBackCancelled() }
        stepUntil("the screen underneath to be gone after the cancel", FINISH_FRAMES) { !exists(HOME_TAG) }
    }

    /**
     * Calling the gesture off undoes the preview: after the top screen was dragged 0.6 of its
     * width along and the gesture is cancelled, the top screen slides back towards the left edge,
     * never moving right by more than a pixel from one frame to the next, until Home is gone.
     * Then Settings rests at the left edge, Home is no longer composed and the back stack still
     * holds Home and Settings: a person who peeks at the previous screen and changes their mind
     * loses nothing.
     */
    @Test
    fun cancelling_the_back_gesture_brings_the_top_screen_back_and_leaves_only_it() {
        val backStack = mutableStateListOf<NavKey>(Home, Settings)
        show(backStack)
        val windowWidth = width(SETTINGS_TAG)
        startGesture()
        moveFingerTo(CANCEL_PROGRESS, windowWidth)
        val leftWhenCancelled = left(SETTINGS_TAG)
        // The premise: the gesture really moved the top screen before it is called off.
        assertEquals(
            "Before the cancel the top screen should be $CANCEL_PROGRESS of its width along",
            CANCEL_PROGRESS * windowWidth,
            leftWhenCancelled,
            POSITION_TOLERANCE_PX,
        )

        onDispatcher { it.dispatchOnBackCancelled() }

        // After every frame of the return: the top screen is still there and has not moved right.
        var previousLeft = leftWhenCancelled
        val frames = stepUntil(
            what = "the screen underneath to be gone after the cancel",
            maxFrames = FINISH_FRAMES,
            afterEachFrame = {
                assertTrue("The top screen vanished while it came back", exists(SETTINGS_TAG))
                val topLeft = left(SETTINGS_TAG)
                assertTrue(
                    "While it came back the top screen moved right, from $previousLeft to $topLeft in one frame",
                    topLeft <= previousLeft + MAX_RIGHTWARD_STEP_WHILE_RETURNING_PX,
                )
                previousLeft = topLeft
            },
        ) { !exists(HOME_TAG) }
        println(
            "PredictiveBackGestureTest: the cancel from x $leftWhenCancelled left only the top screen " +
                "$frames frame(s) after it was sent",
        )

        compose.onNodeWithTag(SETTINGS_TAG).assertExists()
        compose.onNodeWithTag(HOME_TAG).assertDoesNotExist()
        assertEquals(
            "After the cancel the top screen did not come to rest at the left edge",
            0f,
            left(SETTINGS_TAG),
            REST_TOLERANCE_PX,
        )
        assertEquals("The cancel changed the back stack", listOf<NavKey>(Home, Settings), backStack.toList())
    }

    /**
     * Letting go after a far enough gesture completes Back: after the top screen was dragged 0.3
     * of its width along, the dispatcher's onBackPressed (standing in for the system's signal that
     * the finger lifted after a far enough swipe) pops Settings off the back stack. Then, frame by
     * frame: within a few frames the top screen is still composed and to the right of where the
     * finger left it; while it is composed it only ever moves right; the last place it is seen is
     * within [LAST_SEEN_FRAME_STEPS] frame steps of the slide from the right edge, so it reached
     * the edge instead of being taken away on the way; and the whole finish, from letting go until
     * it is gone, takes about as long as the rest of the one-second straight-line slide (0.7 s,
     * about 44 frames, from progress 0.3), so a change that snapped it away or cut the slide short
     * would fail. In the end only Home is left, resting at the left edge.
     */
    @Test
    fun letting_go_after_a_far_enough_gesture_pops_the_screen_and_carries_it_out_of_the_right_edge() {
        val backStack = mutableStateListOf<NavKey>(Home, Settings)
        show(backStack)
        val windowWidth = width(SETTINGS_TAG)
        startGesture()
        moveFingerTo(COMMIT_PROGRESS, windowWidth)
        val leftWhenLetGo = left(SETTINGS_TAG)
        // The premise: the gesture really moved the top screen before the finger lifts.
        assertEquals(
            "Before letting go the top screen should be $COMMIT_PROGRESS of its width along",
            COMMIT_PROGRESS * windowWidth,
            leftWhenLetGo,
            POSITION_TOLERANCE_PX,
        )

        // Letting go: the dispatcher's ordinary Back stands in for the system's, and completes the
        // gesture.
        onDispatcher { it.onBackPressed() }
        val popFrames = stepUntil("letting go to pop Settings off the back stack", START_FRAMES) {
            backStack.toList() == listOf<NavKey>(Home)
        }

        // From here on, checked after every frame: the top screen, while it is there, only ever
        // moves right, starting from where the finger left it.
        var previousLeft = leftWhenLetGo
        val onlyMovesRight: () -> Unit = {
            if (exists(SETTINGS_TAG)) {
                val topLeft = left(SETTINGS_TAG)
                assertTrue(
                    "After letting go the top screen moved left, from $previousLeft to $topLeft in one frame",
                    topLeft >= previousLeft,
                )
                previousLeft = topLeft
            }
        }

        // Right after letting go: still composed, and carrying on past where the finger left it.
        val moveOnFrames = stepUntil(
            what = "the top screen to carry on to the right of where the finger left it (x $leftWhenLetGo)",
            maxFrames = START_FRAMES,
            afterEachFrame = {
                assertTrue("The top screen vanished right after letting go instead of sliding on", exists(SETTINGS_TAG))
                onlyMovesRight()
            },
        ) { exists(SETTINGS_TAG) && left(SETTINGS_TAG) > leftWhenLetGo }
        val leftJustAfter = left(SETTINGS_TAG)

        // Then on until it has left by the right edge.
        val leaveFrames = stepUntil(
            what = "the popped screen to leave by the right edge",
            maxFrames = FINISH_FRAMES,
            afterEachFrame = onlyMovesRight,
        ) { !exists(SETTINGS_TAG) }
        // onlyMovesRight updated previousLeft on every frame the top screen was composed, so it now
        // holds the last place the top screen was seen.
        val lastLeftSeen = previousLeft
        // Every frame from letting go until the top screen was gone.
        val finishFrames = popFrames + moveOnFrames + leaveFrames
        println(
            "PredictiveBackGestureTest: letting go at x $leftWhenLetGo popped the back stack after $popFrames " +
                "frame(s); the top screen was at x $leftJustAfter $moveOnFrames frame(s) later and gone " +
                "$leaveFrames frame(s) after that, last seen at x $lastLeftSeen, $finishFrames frame(s) in all",
        )

        // The top screen reached the right edge: the straight-line slide moves it this far in one
        // frame, and it was last seen within a couple of such steps of the edge.
        val slideStepPx = windowWidth * FRAME_MILLIS / SLIDE_MILLIS
        assertTrue(
            "The top screen was last seen at x $lastLeftSeen, more than $LAST_SEEN_FRAME_STEPS frame steps of " +
                "the slide ($slideStepPx px each) short of the right edge at $windowWidth: it was taken away " +
                "before it reached the edge",
            lastLeftSeen >= windowWidth - LAST_SEEN_FRAME_STEPS * slideStepPx,
        )
        // The finish took about as long as the rest of the straight-line slide, not a snap.
        val slideFramesLeft = (1f - COMMIT_PROGRESS) * SLIDE_MILLIS / FRAME_MILLIS
        assertTrue(
            "Letting go took $finishFrames frame(s) to carry the top screen away; the rest of the slide from " +
                "progress $COMMIT_PROGRESS is about $slideFramesLeft frames, so between $MIN_FINISH_FRAMES and " +
                "$MAX_FINISH_FRAMES were expected",
            finishFrames in MIN_FINISH_FRAMES..MAX_FINISH_FRAMES,
        )

        compose.onNodeWithTag(HOME_TAG).assertExists()
        compose.onNodeWithTag(SETTINGS_TAG).assertDoesNotExist()
        assertEquals(
            "After letting go the screen underneath did not come to rest at the left edge",
            0f,
            left(HOME_TAG),
            REST_TOLERANCE_PX,
        )
        assertEquals("Letting go did not leave Home alone on the back stack", listOf<NavKey>(Home), backStack.toList())
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Pauses the clock, shows NavDisplay over [backStack] inside Kanso's theme, and steps the
     * clock frame by frame for about a second, so each test begins from a resting screen.
     */
    private fun show(backStack: SnapshotStateList<NavKey>) {
        // Pause the clock before anything is shown: from here on, time moves only by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                TwoScreenNavDisplay(backStack)
            }
        }
        repeat(SETTLE_FRAMES) { nextFrame() }
    }

    /**
     * Starts the back gesture at the left edge, as Android does when a finger lands there and
     * starts to move: sends `dispatchOnBackStarted` with progress 0, steps the clock until the
     * screen underneath is composed behind the top one (failing after [START_FRAMES] frames) and
     * returns how many frames that took. It then waits [FRAMES_PER_STEP] more frames, as after
     * every move of the finger, so the gesture goes on from a settled screen.
     */
    private fun startGesture(): Int {
        // The finger's position (x, y), the progress, and the edge the swipe starts from.
        onDispatcher { it.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT)) }
        val frames = stepUntil("the screen underneath to be composed after the gesture started", START_FRAMES) {
            exists(HOME_TAG)
        }
        repeat(FRAMES_PER_STEP) { nextFrame() }
        return frames
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
     * Hands [send] the back dispatcher of the activity that hosts NavDisplay, on the main thread,
     * which is where Android delivers a gesture's events too.
     */
    private fun onDispatcher(send: (OnBackPressedDispatcher) -> Unit) {
        compose.activityRule.scenario.onActivity { activity -> send(activity.onBackPressedDispatcher) }
    }

    /**
     * Moves the paused clock one frame at a time until [condition] holds, checking it before the
     * first step too, and returns the number of frames that took. After every frame, before
     * [condition] is checked again, it runs [afterEachFrame], where a test checks what must hold
     * on every frame of a move. Fails after [maxFrames] frames, naming [what] it was waiting for.
     */
    private fun stepUntil(
        what: String,
        maxFrames: Int,
        afterEachFrame: () -> Unit = {},
        condition: () -> Boolean,
    ): Int {
        var frames = 0
        while (!condition()) {
            if (frames == maxFrames) {
                throw AssertionError("Waited $maxFrames frames for $what")
            }
            nextFrame()
            frames++
            afterEachFrame()
        }
        return frames
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

    /** Fails unless the screen tagged [shown] is composed and the one tagged [gone] is not. */
    private fun assertOnlyScreen(shown: String, gone: String) {
        compose.onNodeWithTag(shown).assertExists()
        compose.onNodeWithTag(gone).assertDoesNotExist()
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
     * NavDisplay over [backStack] with Kanso's three transition specs, built the way the app
     * builds them: the theme's motion scheme is read here, while Compose draws, and handed to the
     * spec functions, whose lambdas NavDisplay calls later. Back removes the top key, so letting
     * go of the gesture pops it. Home and Settings are each drawn as a [PlainScreen].
     */
    @Composable
    private fun TwoScreenNavDisplay(backStack: SnapshotStateList<NavKey>) {
        val motionScheme = MaterialTheme.motionScheme
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = { backStack.removeLastOrNull() },
            transitionSpec = kansoPushTransition(motionScheme),
            popTransitionSpec = kansoPopTransition(motionScheme),
            predictivePopTransitionSpec = kansoPredictivePopTransition(motionScheme),
            entryProvider = entryProvider<NavKey> {
                entry<Home> { PlainScreen(tag = HOME_TAG, label = "Home") }
                entry<Settings> { PlainScreen(tag = SETTINGS_TAG, label = "Settings") }
            },
        )
    }

    /** A screen that fills the window, tagged [tag] so the test can find it, showing [label]. */
    @Composable
    private fun PlainScreen(tag: String, label: String) {
        Box(modifier = Modifier.fillMaxSize().testTag(tag)) {
            Text(text = label)
        }
    }

    /** Tags, gesture positions, frame counts and tolerances the tests share. */
    private companion object {
        /** Marks the Home screen, the one underneath. */
        const val HOME_TAG = "back-gesture-test-home"

        /** Marks the Settings screen, the top one that the gesture moves. */
        const val SETTINGS_TAG = "back-gesture-test-settings"

        /** How many frames a freshly shown screen is left to settle: 60 frames, about a second. */
        const val SETTLE_FRAMES = 60

        /**
         * The most frames a short wait takes, for the screen underneath to appear once the gesture
         * starts, for the back stack to pop and for the top screen to slide on once the finger
         * lifts: 10 frames, 160 ms (the probe written before this test saw each happen within
         * about four).
         */
        const val START_FRAMES = 10

        /** How many frames after each move of the finger the test looks: 4 frames, 64 ms. */
        const val FRAMES_PER_STEP = 4

        /**
         * The most frames a move that ends the gesture takes: 120 frames, about 1.9 s. A cancel
         * from far along took about 0.85 s and a commit about 0.7 s in the probe written before
         * this test.
         */
        const val FINISH_FRAMES = 120

        /** The progress the top screen is dragged to before the gesture is called off. */
        const val CANCEL_PROGRESS = 0.6f

        /** The progress the top screen is dragged to before the finger lifts and Back completes. */
        const val COMMIT_PROGRESS = 0.3f

        /** From this progress on, the screen underneath has finished coming in and rests at 0. */
        const val UNDERNEATH_RESTS_FROM_PROGRESS = 0.5f

        /**
         * How far, in pixels, a screen may be from where the gesture should have put it: screens
         * are placed on whole pixels, and the top screen was exactly on its mark in the probe
         * written before this test.
         */
        const val POSITION_TOLERANCE_PX = 2f

        /** How far, in pixels, a screen may be from the left edge once a move has ended. */
        const val REST_TOLERANCE_PX = 0.5f

        /**
         * The most, in pixels, the top screen may move right in one frame while a cancel brings
         * it back: rounding to whole pixels may wobble by one, a real step the wrong way is more.
         */
        const val MAX_RIGHTWARD_STEP_WHILE_RETURNING_PX = 1f

        /** The progress values the finger moves through, from early in the swipe to far along. */
        val PROGRESS_STEPS = listOf(0.1f, 0.2f, 0.5f, 0.8f)

        /**
         * How long one frame of the paused test clock is, in milliseconds: what each
         * `advanceTimeByFrame` moves it by.
         */
        const val FRAME_MILLIS = 16f

        /**
         * How long the top screen's straight-line slide during the gesture lasts, in milliseconds:
         * the very constant the transition uses (PREDICTIVE_SLIDE_MILLIS in KansoTransitions.kt,
         * `internal` for this reason), so the test cannot keep a stale copy. The screen crosses the
         * whole width at a constant speed in that time, so one frame moves it the width times
         * [FRAME_MILLIS] over this (about 5 px at 320 px wide).
         */
        val SLIDE_MILLIS = PREDICTIVE_SLIDE_MILLIS.toFloat()

        /**
         * How many frame steps of the slide short of the right edge the top screen may be when it
         * is last seen after letting go: 2. The check is a lower bound and these two steps are
         * slack: the run that added this test saw the last frame at the edge itself (x 320 at 320
         * px wide), and a screen taken away early is far further off.
         */
        const val LAST_SEEN_FRAME_STEPS = 2

        /**
         * The fewest frames the finish may take after letting go at [COMMIT_PROGRESS]: 30. The rest
         * of the one-second slide from progress 0.3 is 0.7 s, about 44 frames (the run that added
         * this test saw 47 in all, counting the few frames before the slide moves on); a screen
         * snapped away, or carried off on a much quicker animation, ends well before 30.
         */
        const val MIN_FINISH_FRAMES = 30

        /**
         * The most frames the finish may take after letting go at [COMMIT_PROGRESS]: 60, about
         * 0.96 s, a generous margin over the 44 or so frames the rest of the slide takes; a finish
         * that restarted the slide or ran on a slower animation would take longer.
         */
        const val MAX_FINISH_FRAMES = 60
    }
}
