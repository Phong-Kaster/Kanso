package com.phongkaster.kanso.saved

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.saved.SAVED_ACTIONS_TAG
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_ICON_TAG
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_TAG
import com.phongkaster.kanso.ui.saved.SAVED_HOME_ICON_TAG
import com.phongkaster.kanso.ui.saved.SAVED_HOME_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.saved.SAVED_SCREEN_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SHARE_ICON_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SHARE_TAG
import com.phongkaster.kanso.ui.saved.SavedScreen
import com.phongkaster.kanso.ui.saved.SavedViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the Saved screen (DoD 15): the saved photo is shown, Share opens the system share sheet
 * for it with a read grant and no chosen app, and Edit another and Home call back.
 *
 * The screen's [SavedViewModel] is built with a [FakePhotoLoader] holding a picture drawn in
 * code, so nothing depends on a file. The screen runs on the same phone size as the pictures.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SavedScreenTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** The photo, named by its Uri and described for screen readers, the title and the message show. */
    @Test
    fun the_saved_photo_title_and_message_are_shown() {
        show(CONTENT_URI)
        waitForPhoto()

        compose.onNode(hasTestTag(SAVED_PHOTO_TAG) and SemanticsMatcher.expectValue(SAVED_PHOTO_URI_KEY, CONTENT_URI))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.saved_photo_description)).assertIsDisplayed()
        compose.onNode(hasText(app.getString(R.string.saved_title))).assertIsDisplayed()
        compose.onNode(hasText(app.getString(R.string.saved_message))).assertIsDisplayed()
    }

    /** The three actions are shown and are each at least 48 dp tall. */
    @Test
    fun the_three_actions_are_shown_and_large_enough_to_tap() {
        show(CONTENT_URI)
        listOf(SAVED_SHARE_TAG, SAVED_EDIT_ANOTHER_TAG, SAVED_HOME_TAG).forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
    }

    /**
     * The three actions are one Material 3 Expressive button group, laid out in a single row.
     *
     * Each button is found by its own tag, by its label and as a descendant of the node tagged
     * [SAVED_ACTIONS_TAG], the group. They are at least 48 dp tall, their vertical centres are
     * within 1 dp of each other (so they share a row, even if one label wrapped and made its
     * button taller), and they run left to right in the order Share, Edit another, Home without
     * overlapping.
     */
    @Test
    fun the_three_actions_are_one_button_group_in_a_single_row() {
        show(CONTENT_URI)
        compose.onNodeWithTag(SAVED_ACTIONS_TAG).assertIsDisplayed()

        val actions = listOf(
            SAVED_SHARE_TAG to R.string.saved_share,
            SAVED_EDIT_ANOTHER_TAG to R.string.saved_edit_another,
            SAVED_HOME_TAG to R.string.saved_home,
        )
        val bounds = actions.map { (tag, label) ->
            // hasText reads the label the button's own text shows; hasAnyAncestor puts the button
            // inside the group's node, so a button drawn outside the group would not be found.
            val button = compose.onNode(
                hasTestTag(tag) and
                    hasText(app.getString(label)) and
                    hasAnyAncestor(hasTestTag(SAVED_ACTIONS_TAG)),
            )
            button.assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            button.fetchSemanticsNode().boundsInRoot
        }

        // One row: the middle of every button is at the same height, to within 1 dp.
        val oneDp = with(compose.density) { 1.dp.toPx() }
        val centres = bounds.map { (it.top + it.bottom) / 2f }
        val spread = centres.max() - centres.min()
        assertTrue("The buttons' centres are $spread px apart, more than 1 dp ($oneDp px)", spread <= oneDp)

        // Left to right, each one ends before the next one starts.
        bounds.zipWithNext().forEach { (left, right) ->
            assertTrue("A button overlaps the next one: $left and $right", left.right <= right.left)
        }
    }

    /**
     * Each of the three actions shows an icon as well as its label (DoD 7: "with an icon and
     * text"), the icon inside the button and to the left of the label.
     *
     * The icons have no content description (the label beside each one names the action), and a
     * button merges everything inside it into one node of the semantics tree, so an icon leaves no
     * trace in the merged tree that the tests above read: they would stay green with an icon
     * deleted. Each icon therefore carries a test tag of its own, and this test reads the unmerged
     * tree (`useUnmergedTree = true`), where every tagged node inside a button is still a node of
     * its own. For each button it checks that the icon's node exists, is displayed, sits under the
     * button, is not the label's text node, lies inside the button's bounds and ends before the
     * label's text starts. A button drawn without its icon has no node with that tag, so the first
     * check fails.
     */
    @Test
    fun each_action_shows_an_icon_inside_its_button_left_of_its_label() {
        show(CONTENT_URI)

        val actions = listOf(
            Triple(SAVED_SHARE_TAG, SAVED_SHARE_ICON_TAG, R.string.saved_share),
            Triple(SAVED_EDIT_ANOTHER_TAG, SAVED_EDIT_ANOTHER_ICON_TAG, R.string.saved_edit_another),
            Triple(SAVED_HOME_TAG, SAVED_HOME_ICON_TAG, R.string.saved_home),
        )
        actions.forEach { (buttonTag, iconTag, label) ->
            // The button as the merged tree sees it: one node holding its tag and its label.
            val button = compose.onNodeWithTag(buttonTag)
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot

            // The icon, found by its own tag in the unmerged tree. hasAnyAncestor puts it under
            // this button; keyNotDefined(Text) makes sure the tag is not on the label instead.
            val icon = compose.onNodeWithTag(iconTag, useUnmergedTree = true)
                .assertIsDisplayed()
                .assert(hasAnyAncestor(hasTestTag(buttonTag)))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
                .fetchSemanticsNode()
                .boundsInRoot

            // The label's own text node inside the same button, also from the unmerged tree.
            val text = compose.onNode(
                hasText(app.getString(label)) and hasAnyAncestor(hasTestTag(buttonTag)),
                useUnmergedTree = true,
            )
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot

            assertTrue(
                "The icon $iconTag at $icon is not inside its button $buttonTag at $button",
                icon.left >= button.left && icon.right <= button.right &&
                    icon.top >= button.top && icon.bottom <= button.bottom,
            )
            assertTrue(
                "The icon $iconTag ends at ${icon.right} px, after its label starts at ${text.left} px",
                icon.right <= text.left,
            )
        }
    }

    /**
     * The three actions stay above the bottom gesture zone even when the navigation bar is 0 dp
     * tall (POLICIES.md, the bottom-edge rule).
     *
     * The bottom gesture zone is the strip where a swipe up means "go home"; a tap inside it can
     * bring up the phone's own navigation instead of pressing the button. With the navigation bar
     * hidden, or with swipe gestures on and the gesture hint off (as on the owner's Galaxy A13),
     * the navigation bar's inset is 0 while the gesture zone is still there, so a bar padded by
     * the navigation bar alone keeps only its own 16 dp margin and sits inside the zone.
     *
     * Robolectric draws no system bars, so the test hands the view the screen is drawn in the
     * window insets such a phone would: a gesture zone [GESTURE_ZONE] tall at the bottom and every
     * other inset, the navigation bar's included, 0. Each button's bottom edge must then be at or
     * above the zone's top edge.
     *
     * The screen's own reading of the gesture zone is taken as well. If the insets never reached
     * it (a change in Robolectric or Compose), the test fails: without the zone it would prove
     * nothing, and a skipped test would only look like a pass.
     */
    @Test
    fun the_actions_clear_the_bottom_gesture_zone_when_the_navigation_bar_is_zero_tall() {
        var composeView: View? = null
        var seenZonePx = -1
        show(
            CONTENT_URI,
            probe = {
                // LocalView is the Android view the screen is drawn in, the one that receives the
                // window insets; the gesture zone is read here just as the screen reads it.
                val view = LocalView.current
                val zone = WindowInsets.systemGestures.getBottom(LocalDensity.current)
                // SideEffect hands the values out after each composition that succeeds.
                SideEffect {
                    composeView = view
                    seenZonePx = zone
                }
            },
        )

        val zonePx = with(compose.density) { GESTURE_ZONE.roundToPx() }
        // Only the gesture zone is set, so every other inset, the navigation bar's included, is 0.
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(0, 0, 0, zonePx))
            .build()
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(checkNotNull(composeView), insets) }
        compose.waitForIdle()
        assertTrue(
            "The screen saw a gesture zone of $seenZonePx px, not the $zonePx px the test sent",
            seenZonePx == zonePx,
        )

        val screenBottom = compose.onNodeWithTag(SAVED_SCREEN_TAG).fetchSemanticsNode().boundsInRoot.bottom
        val zoneTop = screenBottom - zonePx
        listOf(SAVED_SHARE_TAG, SAVED_EDIT_ANOTHER_TAG, SAVED_HOME_TAG).forEach { tag ->
            val buttonBottom = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.bottom
            assertTrue(
                "The button $tag ends at $buttonBottom px, inside the gesture zone that starts at $zoneTop px",
                buttonBottom <= zoneTop,
            )
        }
    }

    /**
     * Share starts the system chooser wrapping an image/jpeg ACTION_SEND that carries the photo
     * with a read grant, targets no app, and then calls onShare once.
     */
    @Test
    fun share_opens_the_chooser_for_the_photo() {
        var shares = 0
        show(CONTENT_URI, onShare = { shares++ })

        compose.onNodeWithTag(SAVED_SHARE_TAG).performClick()
        val chooser = waitForStartedActivity()

        assertChooserFor(chooser, Uri.parse(CONTENT_URI))
        compose.waitForIdle()
        assertEquals(1, shares)
    }

    /** On Android 9 the saved file's Uri never leaves the app: the share carries its content Uri. */
    @Test
    @Config(sdk = [28])
    fun share_on_android_9_sends_the_content_uri_of_the_saved_file() {
        val provider = FakeImagesProvider.install()
        provider.files[SAVED_PATH] = 42L
        show("file://$SAVED_PATH")

        compose.onNodeWithTag(SAVED_SHARE_TAG).performClick()
        val chooser = waitForStartedActivity()

        assertChooserFor(chooser, Uri.parse("content://media/external/images/media/42"))
    }

    /** Edit another calls onEditAnother, and nothing else. */
    @Test
    fun edit_another_calls_back() {
        var edits = 0
        var homes = 0
        show(CONTENT_URI, onEditAnother = { edits++ }, onHome = { homes++ })

        compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG).performClick()
        compose.waitForIdle()

        assertEquals(1, edits)
        assertEquals(0, homes)
        assertTrue(startedIntents().none { it.action == Intent.ACTION_CHOOSER })
    }

    /** Home calls onHome, and nothing else. */
    @Test
    fun home_calls_back() {
        var edits = 0
        var homes = 0
        show(CONTENT_URI, onEditAnother = { edits++ }, onHome = { homes++ })

        compose.onNodeWithTag(SAVED_HOME_TAG).performClick()
        compose.waitForIdle()

        assertEquals(0, edits)
        assertEquals(1, homes)
        assertTrue(startedIntents().none { it.action == Intent.ACTION_CHOOSER })
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Shows the Saved screen for [photoUri]. Its ViewModel is built here, before the screen is
     * drawn (lint forbids building a ViewModel while drawing), with a fake loader that hands
     * back a 600 x 400 gradient for any Uri. [probe] is composed next to the screen, for a test
     * that needs to read something only a composable can (it draws nothing).
     */
    private fun show(
        photoUri: String,
        onShare: () -> Unit = {},
        onEditAnother: () -> Unit = {},
        onHome: () -> Unit = {},
        probe: @Composable () -> Unit = {},
    ) {
        val viewModel = SavedViewModel(
            photoUri = photoUri,
            loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400)),
            probe = ViewModelClearedProbe(),
        )
        compose.setContent {
            probe()
            MaterialTheme {
                SavedScreen(
                    viewModel = viewModel,
                    onShare = onShare,
                    onEditAnother = onEditAnother,
                    onHome = onHome,
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(SAVED_SCREEN_TAG).assertExists()
    }

    /**
     * Waits until the photo is on screen. The ViewModel's load ends on the main thread, so the
     * main thread's queue is run while waiting.
     */
    private fun waitForPhoto() {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(hasTestTag(SAVED_PHOTO_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Waits until the screen starts the chooser, and returns its intent. The share Uri is looked
     * up on a background thread, so the main thread's queue is run while waiting. Any other
     * started activity (such as the host activity itself) is skipped.
     */
    private fun waitForStartedActivity(): Intent {
        var chooser: Intent? = null
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            chooser = startedIntents().firstOrNull { it.action == Intent.ACTION_CHOOSER }
            chooser != null
        }
        return chooser!!
    }

    /** Takes every activity start recorded so far, oldest first. */
    private fun startedIntents(): List<Intent> =
        generateSequence { shadowOf(app).nextStartedActivity }.toList()

    /**
     * Checks that [chooser] is the system chooser wrapping an image/jpeg ACTION_SEND of [uri],
     * with the read grant on both intents and the Uri in their ClipData, and no app chosen.
     */
    private fun assertChooserFor(chooser: Intent, uri: Uri) {
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertNull(chooser.`package`)
        assertNull(chooser.component)
        assertTrue(chooser.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(app.getString(R.string.saved_share_chooser), chooser.getCharSequenceExtra(Intent.EXTRA_TITLE)?.toString())

        val send = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull("The chooser wraps no intent", send)
        send!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/jpeg", send.type)
        assertEquals(uri, IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertNull(send.`package`)
        assertNull(send.component)
        assertEquals(uri, send.clipData?.getItemAt(0)?.uri)
        assertEquals(uri, chooser.clipData?.getItemAt(0)?.uri)
    }

    /** Values shared by the tests. */
    private companion object {
        /** A saved photo as Android 10 and later return it. */
        const val CONTENT_URI = "content://media/external/images/media/7"

        /** Where Android 9's Save puts a photo. */
        const val SAVED_PATH = "/storage/emulated/0/Pictures/Kanso/Kanso_20261007_120000.jpg"

        /**
         * The height of the bottom gesture zone the gesture test hands the screen: more than the
         * action bar's own 16 dp bottom margin, so a bar padded by the navigation bar alone would
         * end inside it.
         */
        val GESTURE_ZONE = 40.dp
    }
}
