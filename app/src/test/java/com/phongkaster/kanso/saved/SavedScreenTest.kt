package com.phongkaster.kanso.saved

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_TAG
import com.phongkaster.kanso.ui.saved.SAVED_HOME_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.saved.SAVED_SCREEN_TAG
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
     * back a 600 x 400 gradient for any Uri.
     */
    private fun show(
        photoUri: String,
        onShare: () -> Unit = {},
        onEditAnother: () -> Unit = {},
        onHome: () -> Unit = {},
    ) {
        val viewModel = SavedViewModel(
            photoUri = photoUri,
            loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400)),
            probe = ViewModelClearedProbe(),
        )
        compose.setContent {
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
    }
}
