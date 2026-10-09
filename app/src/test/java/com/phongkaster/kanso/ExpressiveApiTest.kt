package com.phongkaster.kanso

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Proves that the Material 3 Expressive components of Material3 1.5.0-beta01 compile in this app
 * and work on screen: a button group, a floating toolbar, a loading indicator, a shape from
 * MaterialShapes and the two wavy progress indicators. Nothing in the app uses them yet (the new
 * look comes in the next run), so this test is the evidence that the library is ready for it.
 *
 * What each test proves, and no more: the component is composed and laid out (it has a size and
 * a place inside the window). For the button group and the toolbar that is shown by finding their
 * text on screen. For the loading indicator, the shape and the two wavy indicators the test also
 * draws the screen into a bitmap and checks that the component painted at least one pixel inside
 * its own bounds, so a component that drew nothing at all, or failed while drawing, would fail.
 * No test checks what the drawing looks like.
 *
 * Expressive is the newest style of Material 3: rounder shapes, springier motion and a few new
 * components. Some of its parts are still marked experimental, and the library refuses to compile
 * a call to them unless the caller opts in with ExperimentalMaterial3ExpressiveApi. The OptIn
 * annotation on this class opts in once for everything inside it.
 *
 * Three of these components animate forever: the loading indicator morphs from shape to shape
 * and both wavy indicators keep their wave moving. A Compose test normally waits until every
 * animation has finished before it checks the screen, which here would never happen. So each test
 * pauses the test clock before it shows anything, then moves the clock forward by hand.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ExpressiveApiTest {

    /**
     * Starts an empty activity to host the composable under test. Unlike the plain compose rule,
     * this one hands the test that activity (compose.activity), so the test can draw its screen.
     */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * A button group lays its buttons out in a row. This one holds two buttons, Edit and Share,
     * and both labels are on screen. Its overflow indicator, the button that would open a menu
     * of the buttons that do not fit, is left empty: two short buttons always fit.
     */
    @Test
    fun button_group_shows_both_of_its_buttons() {
        // Pause the clock so the test never waits for an animation to end; time moves by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                // The trailing lambda lists the group's items; ButtonGroup builds a button for each.
                ButtonGroup(overflowIndicator = { }) {
                    clickableItem(onClick = { }, label = EDIT)
                    clickableItem(onClick = { }, label = SHARE)
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)

        compose.onNodeWithText(EDIT).assertIsDisplayed()
        compose.onNodeWithText(SHARE).assertIsDisplayed()
    }

    /**
     * A floating toolbar is a pill of actions that floats above the content. Its main content,
     * here a single word, is always shown, and the word is on screen. The expanded flag does not
     * hide that content: it decides whether the optional leading and trailing slots appear, when
     * they are given (this toolbar gives none).
     */
    @Test
    fun floating_toolbar_shows_its_content() {
        // Pause the clock so the test never waits for an animation to end; time moves by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                HorizontalFloatingToolbar(expanded = true) {
                    Text(TOOLBAR)
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)

        compose.onNodeWithText(TOOLBAR).assertIsDisplayed()
    }

    /**
     * The loading indicator, the Expressive replacement for a plain spinner, is on screen and
     * paints inside its bounds. With no progress given it is indeterminate: it morphs between
     * shapes for as long as it is shown.
     */
    @Test
    fun loading_indicator_is_displayed() {
        // Pause the clock: this indicator animates forever, so waiting for it would never end.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                LoadingIndicator(modifier = Modifier.testTag(LOADING_TAG))
            }
        }
        compose.mainClock.advanceTimeBy(500)

        compose.onNodeWithTag(LOADING_TAG).assertIsDisplayed()
        assertPainted(LOADING_TAG)
    }

    /**
     * A box clipped to one of the MaterialShapes is on screen and painted. MaterialShapes holds
     * the Expressive shape set (cookies, suns, pills and more) as polygons; toShape turns one into
     * a Shape that clip can cut a box with.
     */
    @Test
    fun material_shape_clips_a_box_that_is_displayed() {
        // Pause the clock so the test never waits for an animation to end; time moves by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier
                        // The tag comes first, so the tagged node covers the whole 48 dp box.
                        .testTag(SHAPE_TAG)
                        .size(48.dp)
                        // A cookie with nine bumps: one of the shapes of the Expressive set.
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        compose.mainClock.advanceTimeBy(500)

        compose.onNodeWithTag(SHAPE_TAG).assertIsDisplayed()
        assertPainted(SHAPE_TAG)
    }

    /**
     * The linear wavy progress indicator, a progress bar whose filled part is a wave, is on
     * screen at half progress and paints inside its bounds. Progress is passed as a function, so
     * the bar can read a changing value while it draws without the screen having to be composed
     * again.
     */
    @Test
    fun linear_wavy_progress_indicator_is_displayed() {
        // Pause the clock: the wave keeps moving, so waiting for it to stop would never end.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                LinearWavyProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier.testTag(LINEAR_WAVY_TAG),
                )
            }
        }
        compose.mainClock.advanceTimeBy(500)

        compose.onNodeWithTag(LINEAR_WAVY_TAG).assertIsDisplayed()
        assertPainted(LINEAR_WAVY_TAG)
    }

    /**
     * The circular wavy progress indicator, a ring drawn as a wave, is on screen at half progress
     * and paints inside its bounds.
     */
    @Test
    fun circular_wavy_progress_indicator_is_displayed() {
        // Pause the clock: the wave keeps moving, so waiting for it to stop would never end.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CircularWavyProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier.testTag(CIRCULAR_WAVY_TAG),
                )
            }
        }
        compose.mainClock.advanceTimeBy(500)

        compose.onNodeWithTag(CIRCULAR_WAVY_TAG).assertIsDisplayed()
        assertPainted(CIRCULAR_WAVY_TAG)
    }

    /**
     * All the Expressive components shown together, stacked in one column: the button group,
     * the floating toolbar, the loading indicator, the clipped shape and both wavy indicators are
     * each on screen, and the four without text each paint inside their own bounds. This is the
     * one place where every new name meets in a single screen.
     */
    @Test
    fun all_expressive_components_draw_together() {
        // Pause the clock: three of these (the loading indicator and both wavy indicators)
        // animate forever, so waiting for them would never end.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                // A Column stacks its children from top to bottom.
                Column {
                    ButtonGroup(overflowIndicator = { }) {
                        clickableItem(onClick = { }, label = EDIT)
                        clickableItem(onClick = { }, label = SHARE)
                    }
                    HorizontalFloatingToolbar(expanded = true) {
                        Text(TOOLBAR)
                    }
                    LoadingIndicator(modifier = Modifier.testTag(LOADING_TAG))
                    Box(
                        Modifier
                            .testTag(SHAPE_TAG)
                            .size(48.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    LinearWavyProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier.testTag(LINEAR_WAVY_TAG),
                    )
                    CircularWavyProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier.testTag(CIRCULAR_WAVY_TAG),
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)

        compose.onNodeWithText(EDIT).assertIsDisplayed()
        compose.onNodeWithText(SHARE).assertIsDisplayed()
        compose.onNodeWithText(TOOLBAR).assertIsDisplayed()
        compose.onNodeWithTag(LOADING_TAG).assertIsDisplayed()
        compose.onNodeWithTag(SHAPE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(LINEAR_WAVY_TAG).assertIsDisplayed()
        compose.onNodeWithTag(CIRCULAR_WAVY_TAG).assertIsDisplayed()

        assertPainted(LOADING_TAG)
        assertPainted(SHAPE_TAG)
        assertPainted(LINEAR_WAVY_TAG)
        assertPainted(CIRCULAR_WAVY_TAG)
    }

    /**
     * Fails unless the node tagged [tag] painted at least one pixel inside its own bounds.
     *
     * assertIsDisplayed only proves that a node has a size and a place in the window; it never
     * runs the draw pass. This helper draws the screen into a bitmap and looks inside the node's
     * bounds for any pixel that is not fully transparent. These tests put no background behind
     * the components, so a pixel inside the bounds stays transparent unless something drew there.
     *
     * It draws the activity's content view itself, as the settings tests do, because Compose's
     * captureToImage waits for the window to redraw and that wait times out under Robolectric
     * here. Under Robolectric the test thread is the main thread, so drawing here is allowed.
     */
    private fun assertPainted(tag: String) {
        // The node's bounds in the Compose root, in pixels. The Compose root fills the content
        // view from its top-left corner, so the bitmap drawn below uses the same coordinates.
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

        // The first child of the content frame is the view that holds the Compose root. Drawing
        // it, and not the whole window, leaves out the window background.
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val painted = try {
            view.draw(Canvas(bitmap))
            // Keep the bounds inside the bitmap, so a node that reaches past its edge cannot make
            // getPixel read outside it.
            val left = bounds.left.toInt().coerceIn(0, bitmap.width - 1)
            val top = bounds.top.toInt().coerceIn(0, bitmap.height - 1)
            val right = bounds.right.toInt().coerceIn(left + 1, bitmap.width)
            val bottom = bounds.bottom.toInt().coerceIn(top + 1, bitmap.height)
            var count = 0
            for (y in top until bottom) {
                for (x in left until right) {
                    // A pixel nothing drew on keeps the fresh bitmap's alpha of zero.
                    if (Color(bitmap.getPixel(x, y)).alpha > 0f) count++
                }
            }
            count
        } finally {
            // A bitmap holds native memory; free it now rather than waiting for the collector.
            bitmap.recycle()
        }
        assertTrue(
            "The node tagged " + tag + " was never painted: " +
                "every pixel inside its bounds is transparent",
            painted > 0,
        )
    }

    /** The labels and test tags the tests share. */
    private companion object {
        /** The first button of the button group. */
        const val EDIT = "Edit"

        /** The second button of the button group. */
        const val SHARE = "Share"

        /** The word shown inside the floating toolbar. */
        const val TOOLBAR = "Toolbar"

        /** Marks the loading indicator, which has no text to find it by. */
        const val LOADING_TAG = "loading"

        /** Marks the box clipped to a MaterialShapes shape. */
        const val SHAPE_TAG = "shape"

        /** Marks the linear wavy progress indicator. */
        const val LINEAR_WAVY_TAG = "linearWavy"

        /** Marks the circular wavy progress indicator. */
        const val CIRCULAR_WAVY_TAG = "circularWavy"
    }
}
