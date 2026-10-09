package com.phongkaster.kanso.navigation

import android.content.Context
import android.os.Looper
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
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

/**
 * Checks Kanso's screen transitions (DoD 14, and the machine part of DoD 13) by hosting
 * Navigation 3's NavDisplay directly, with the three specs of KansoTransitions.kt, two plain
 * screens (the [Home] and [Settings] keys, each drawn as a tagged box with its name) and a back
 * stack the test changes by hand. There is no MainActivity, no Koin and no ViewModel, so the test
 * depends only on the specs, not on how the app's navigation host is wired.
 *
 * The test pauses Compose's clock before anything is shown (`mainClock.autoAdvance = false`), so
 * time only moves when the test moves it, one frame (16 ms) at a time or in larger steps. That
 * lets it look at a transition while it runs:
 * - a push: a frame or two after the key is added, both screens are composed at once (a change
 *   with no transition would swap them in a single frame), the new screen is still to the right of
 *   its place and the old one has moved to the left of its own; 2 000 ms later only the new screen
 *   is left, resting at the left edge;
 * - a pop: the same in reverse, the leaving screen moving right and the one underneath coming back
 *   from the left;
 * - a Back press, sent through the activity's back dispatcher as the system sends it, pops the top
 *   screen with the pop transition;
 * - each spec takes its springs from the motion scheme it is given;
 * - with the phone's "Remove animations" setting (the animator duration scale at 0), a push ends
 *   at once, if the test's host passes that setting on to Compose; when it does not, the test says
 *   so and checks that the push still runs as usual.
 *
 * The specs' springs settle in under a second, so 2 000 ms is ample. The preview the back gesture
 * shows while the finger moves (predictive back) is driven in [PredictiveBackGestureTest].
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NavTransitionTest {

    /**
     * Starts an empty activity to host NavDisplay. Unlike the plain compose rule, this one hands
     * the test that activity, whose back dispatcher the Back test presses.
     */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The app context, used to change and read the system's animation setting. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Puts the system's animation scale back to normal, so no later test starts with animations
     * removed, and stops Koin: the test does not use Koin, but KansoApplication starts it for every
     * Robolectric test, and its state outlives the test inside the same JVM.
     */
    @After
    fun restoreAnimationsAndStopKoin() {
        setAnimatorDurationScale(1f)
        stopKoin()
    }

    /**
     * A push animates: shortly after Settings is added above Home both screens are composed, the
     * arriving Settings screen is still to the right of its place while Home has slid to the left,
     * and 2 000 ms later only Settings is left, at the left edge.
     */
    @Test
    fun a_push_composes_both_screens_slides_the_new_one_in_from_the_right_and_then_leaves_only_it() {
        val backStack = mutableStateListOf<NavKey>(Home)
        show(backStack)
        assertOnlyScreen(shown = HOME_TAG, gone = SETTINGS_TAG)

        backStack.add(Settings)

        assertPushSlidesInFromTheRight()
    }

    /**
     * A pop animates: removing Settings from `[Home, Settings]` (a pop by NavDisplay's rule: the
     * new back stack is the start of the old one) composes both screens, Settings slides away to
     * the right while Home comes back from the left, and 2 000 ms later only Home is left.
     */
    @Test
    fun a_pop_composes_both_screens_brings_the_old_one_back_from_the_left_and_then_leaves_only_it() {
        val backStack = mutableStateListOf<NavKey>(Home, Settings)
        show(backStack)
        assertOnlyScreen(shown = SETTINGS_TAG, gone = HOME_TAG)

        backStack.removeLastOrNull()

        assertPopSlidesOutToTheRight()
    }

    /**
     * A normal Back press completes the pop: with `[Home, Settings]` on the back stack, Back sent
     * through the activity's back dispatcher (as the system's Back button or a finished gesture
     * sends it) reaches NavDisplay, whose onBack removes Settings; the pop transition runs, and
     * 2 000 ms later only Home is left and the back stack holds Home alone.
     */
    @Test
    fun a_back_press_pops_the_top_screen_with_the_pop_transition() {
        val backStack = mutableStateListOf<NavKey>(Home, Settings)
        show(backStack)
        assertOnlyScreen(shown = SETTINGS_TAG, gone = HOME_TAG)

        compose.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        assertPopSlidesOutToTheRight()
        assertEquals("Back did not leave Home alone on the back stack", listOf<NavKey>(Home), backStack.toList())
    }

    /**
     * Each of the three spec functions takes its springs from the motion scheme it is given, and
     * exactly two of them: the default spatial spring for the slides and the default effects
     * spring for the fades. (The back gesture's spec asks for both too, but moves its top screen
     * on a straight-line slide, so this test says nothing about that slide: the gesture tests
     * do.) A scheme that records what is asked of it stands in for the theme's, so this needs
     * neither a screen nor a running transition.
     */
    @Test
    fun each_spec_takes_a_spatial_and_an_effects_spring_from_the_scheme_it_is_given() {
        val asked = mutableListOf<String>()
        val scheme = recordingScheme(asked)

        kansoPushTransition(scheme)
        assertEquals("The push asked the motion scheme for other springs", SPRINGS_USED, asked.toSet())

        asked.clear()
        kansoPopTransition(scheme)
        assertEquals("The pop asked the motion scheme for other springs", SPRINGS_USED, asked.toSet())

        asked.clear()
        kansoPredictivePopTransition(scheme)
        assertEquals(
            "The back-gesture pop asked the motion scheme for other springs",
            SPRINGS_USED,
            asked.toSet(),
        )
    }

    /**
     * The "Remove animations" pass (DoD 13, machine part). The phone's setting is the animator
     * duration scale, set here to 0 before anything is shown. Compose honours it only if the host
     * hands it to Compose's coroutines as a `MotionDurationScale`; a Compose test builds its own
     * host, which may not. So the test first runs a control next to the navigation: a plain
     * one-second animation of a number, started together with a push. If the control reaches its
     * end within a few frames, the host carries the setting, and the push must then have finished
     * as quickly: only the new screen is left. If the control is still on its way, the host does
     * not carry the setting; the test prints that this check is "not driven by machine" (which
     * DoD 13 does not count as a failure) and asserts what that implies, that the push is still
     * running like any other. Either way the outcome is asserted, so a host that starts carrying
     * the setting changes which branch runs, and what the printed line says.
     */
    @Test
    fun with_animations_removed_a_push_finishes_at_once_if_the_host_carries_the_setting() {
        setAnimatorDurationScale(0f)
        assertEquals(
            "The test could not set the system's animation scale to 0",
            0f,
            SystemSettings.Global.getFloat(context.contentResolver, SystemSettings.Global.ANIMATOR_DURATION_SCALE, 1f),
            0f,
        )

        val backStack = mutableStateListOf<NavKey>(Home)
        val controlTarget = mutableFloatStateOf(0f)
        lateinit var control: State<Float>
        var effectRan = false
        var effectScale: Float? = null

        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                // The control: a one-second animation of a plain number, in the same composition
                // and on the same clock as NavDisplay.
                control = animateFloatAsState(
                    targetValue = controlTarget.floatValue,
                    animationSpec = tween(durationMillis = CONTROL_MS, easing = LinearEasing),
                    label = "control",
                )
                // Reads the scale Compose's animations see: they run in coroutines like this one,
                // and read it from their context (none at all means a scale of 1).
                LaunchedEffect(Unit) {
                    effectScale = coroutineContext[MotionDurationScale]?.scaleFactor
                    effectRan = true
                }
                TwoScreenNavDisplay(backStack)
            }
        }
        advance(SETTLE_MS)
        assertTrue("The effect that reads the animation scale never ran", effectRan)

        // Start the control and the push together.
        controlTarget.floatValue = 1f
        backStack.add(Settings)
        stepUntil("the pushed screen to be composed") { exists(SETTINGS_TAG) }
        repeat(REMOVED_ANIMATION_FRAMES) { nextFrame() }

        val controlFinishedAtOnce = control.value == 1f
        val oldScreenStillComposed = exists(HOME_TAG)
        assertEquals(
            "The scale in Compose's coroutines ($effectScale) does not match what the control did",
            effectScale == 0f,
            controlFinishedAtOnce,
        )
        if (controlFinishedAtOnce) {
            println(
                "NavTransitionTest: the host carries ANIMATOR_DURATION_SCALE into Compose " +
                    "(MotionDurationScale $effectScale): the push was checked with animations removed",
            )
            assertFalse(
                "With animations removed the push should have finished at once, yet the old " +
                    "screen is still composed $REMOVED_ANIMATION_FRAMES frames after the new one appeared",
                oldScreenStillComposed,
            )
        } else {
            println(
                "NavTransitionTest: not driven by machine. This Robolectric host does not carry " +
                    "ANIMATOR_DURATION_SCALE into Compose (MotionDurationScale ${effectScale ?: "absent"}, " +
                    "the control animation was at ${control.value} after the push), so the " +
                    "Remove-animations pass of DoD 13 is left to the person on the phone",
            )
            assertTrue(
                "The host does not carry the animation setting, so the push should still be " +
                    "running like any other, yet the old screen is already gone",
                oldScreenStillComposed,
            )
        }

        // Whatever the host, both moves end: the control at its target, the push with only the
        // new screen left.
        advance(FINISH_MS)
        assertEquals("The control animation never reached its target", 1f, control.value, 0.001f)
        assertOnlyScreen(shown = SETTINGS_TAG, gone = HOME_TAG)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Pauses the clock, shows NavDisplay over [backStack] inside Kanso's theme, and lets
     * everything that starts with the screen finish, so each test begins from a resting screen.
     */
    private fun show(backStack: SnapshotStateList<NavKey>) {
        // Pause the clock before anything is shown: from here on, time moves only by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                TwoScreenNavDisplay(backStack)
            }
        }
        advance(SETTLE_MS)
    }

    /**
     * After a key was added above Home: steps the clock until Settings is composed and checks
     * that Home still is (both screens at once), then, once Settings has started to move, that it
     * is still to the right of its place and that Home has moved to the left of its own, which is
     * the push spec at work, not NavDisplay's default fade. Then lets the move end and checks that
     * only Settings is left, at the left edge.
     */
    private fun assertPushSlidesInFromTheRight() {
        val frames = stepUntil("the pushed screen to be composed") { exists(SETTINGS_TAG) }
        println("NavTransitionTest: both screens composed $frames frame(s) after the push")
        compose.onNodeWithTag(HOME_TAG).assertExists()
        compose.onNodeWithTag(SETTINGS_TAG).assertExists()

        // A tenth of the way in, long before the spring's small overshoot (about 0.2 s in).
        stepUntil("the pushed screen to start moving") { left(SETTINGS_TAG) < width(SETTINGS_TAG) * 0.9f }
        assertTrue(
            "The pushed screen should arrive from the right, but its left edge is at ${left(SETTINGS_TAG)}",
            left(SETTINGS_TAG) > 0f,
        )
        assertTrue(
            "The covered screen should move left, but its left edge is at ${left(HOME_TAG)}",
            left(HOME_TAG) < 0f,
        )

        advance(FINISH_MS)
        assertOnlyScreen(shown = SETTINGS_TAG, gone = HOME_TAG)
        assertEquals("The pushed screen did not come to rest at the left edge", 0f, left(SETTINGS_TAG), 0.5f)
    }

    /**
     * After Settings was removed from `[Home, Settings]`: steps the clock until Home is composed
     * again and checks that Settings still is (both screens at once), then, once Settings has
     * started to move right, that Home is still to the left of its place, which is the pop spec at
     * work. Then lets the move end and checks that only Home is left, at the left edge.
     */
    private fun assertPopSlidesOutToTheRight() {
        val frames = stepUntil("the screen underneath to be composed") { exists(HOME_TAG) }
        println("NavTransitionTest: both screens composed $frames frame(s) after the pop")
        compose.onNodeWithTag(HOME_TAG).assertExists()
        compose.onNodeWithTag(SETTINGS_TAG).assertExists()

        // A tenth of the way out, long before the spring's small overshoot (about 0.2 s in).
        stepUntil("the leaving screen to start moving") { left(SETTINGS_TAG) > width(SETTINGS_TAG) * 0.1f }
        assertTrue(
            "The screen underneath should come back from the left, but its left edge is at ${left(HOME_TAG)}",
            left(HOME_TAG) < 0f,
        )

        advance(FINISH_MS)
        assertOnlyScreen(shown = HOME_TAG, gone = SETTINGS_TAG)
        assertEquals("The screen underneath did not come to rest at the left edge", 0f, left(HOME_TAG), 0.5f)
    }

    /**
     * Moves the paused clock one frame at a time until [condition] holds, checking it before the
     * first step too, and returns the number of frames that took. Fails after [MAX_FRAMES] frames
     * (160 ms), naming [what] it was waiting for.
     */
    private fun stepUntil(what: String, condition: () -> Boolean): Int {
        var frames = 0
        while (!condition()) {
            if (frames == MAX_FRAMES) {
                throw AssertionError("Waited $MAX_FRAMES frames for $what")
            }
            nextFrame()
            frames++
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

    /** Moves the paused clock forward by [millis], frame after frame, and runs the main thread's queue. */
    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
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
     * slide moves where a screen is placed, so this follows the transition as it runs.
     */
    private fun left(tag: String): Float = compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.x

    /** Returns the width of the screen tagged [tag], in pixels: the window's width. */
    private fun width(tag: String): Float = compose.onNodeWithTag(tag).fetchSemanticsNode().size.width.toFloat()

    /** Sets the system's animator duration scale, the value the "Remove animations" setting turns to 0. */
    private fun setAnimatorDurationScale(scale: Float) {
        SystemSettings.Global.putFloat(context.contentResolver, SystemSettings.Global.ANIMATOR_DURATION_SCALE, scale)
    }

    /**
     * Returns a motion scheme that writes the name of every spring asked of it into [asked], and
     * hands back the expressive scheme's own spring, so the specs built from it work as usual.
     */
    private fun recordingScheme(asked: MutableList<String>): MotionScheme {
        val real = MotionScheme.expressive()
        return object : MotionScheme {
            /** Records the request, then returns the expressive default spatial spring. */
            override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> {
                asked += "defaultSpatialSpec"
                return real.defaultSpatialSpec()
            }

            /** Records the request, then returns the expressive fast spatial spring. */
            override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> {
                asked += "fastSpatialSpec"
                return real.fastSpatialSpec()
            }

            /** Records the request, then returns the expressive slow spatial spring. */
            override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> {
                asked += "slowSpatialSpec"
                return real.slowSpatialSpec()
            }

            /** Records the request, then returns the expressive default effects spring. */
            override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> {
                asked += "defaultEffectsSpec"
                return real.defaultEffectsSpec()
            }

            /** Records the request, then returns the expressive fast effects spring. */
            override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> {
                asked += "fastEffectsSpec"
                return real.fastEffectsSpec()
            }

            /** Records the request, then returns the expressive slow effects spring. */
            override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> {
                asked += "slowEffectsSpec"
                return real.slowEffectsSpec()
            }
        }
    }

    /**
     * NavDisplay over [backStack] with Kanso's three transition specs, built the way the app
     * builds them: the theme's motion scheme is read here, while Compose draws, and handed to the
     * spec functions, whose lambdas NavDisplay calls later. Back removes the top key. Home and
     * Settings are each drawn as a [PlainScreen].
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

    /** Tags, timings and expectations the tests share. */
    private companion object {
        /** Marks the Home screen. */
        const val HOME_TAG = "nav-test-home"

        /** Marks the Settings screen. */
        const val SETTINGS_TAG = "nav-test-settings"

        /** The most frames [stepUntil] waits: 160 ms, a fraction of a transition. */
        const val MAX_FRAMES = 10

        /** How long a freshly shown screen is left to settle before a test changes anything. */
        const val SETTLE_MS = 1_000L

        /** How long after a change every transition has ended: the springs settle in under a second. */
        const val FINISH_MS = 2_000L

        /** How long the control animation of the Remove-animations pass takes at normal speed. */
        const val CONTROL_MS = 1_000

        /**
         * How many frames after the pushed screen appears the Remove-animations pass looks:
         * 48 ms, enough for a move with animations removed to end and be cleared away, and far
         * too short for one at normal speed.
         */
        const val REMOVED_ANIMATION_FRAMES = 3

        /** The springs every spec takes from its motion scheme: one to move, one to fade. */
        val SPRINGS_USED = setOf("defaultSpatialSpec", "defaultEffectsSpec")
    }
}
