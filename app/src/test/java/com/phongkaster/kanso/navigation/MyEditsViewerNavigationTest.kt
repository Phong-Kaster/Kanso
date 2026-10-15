package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.IntentCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.camera.FakeCameraController
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.myedits.FakeSavedPhotoSource
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SCREEN_TAG
import com.phongkaster.kanso.ui.myedits.myEditsTileTag
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.saved.SAVED_CLOSE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_TAG
import com.phongkaster.kanso.ui.saved.SAVED_MARK_TAG
import com.phongkaster.kanso.ui.saved.SAVED_MESSAGE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.saved.SAVED_SCREEN_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SHARE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_TITLE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * DoD 9 (the photo viewer) and the viewer clause of DoD 1, through the app's real navigation: on
 * the My edits tab, a tap on tile 3 opens that photo full screen, in the Saved screen's viewer
 * form. The viewer shows photo 3, a round Share button with no visible text, a wide "Edit" button,
 * a round close button and the title "My edits"; it has no success mark, no "saved" line and no
 * floating tab bar. Share starts the system chooser with photo 3's content Uri; Edit opens the Edit
 * tool screen on photo 3; close shows Home; system Back returns to My edits with tile 3 still on
 * screen.
 *
 * The fakes are loaded before the app starts (`createEmptyComposeRule`, `loadKoinModules`, then
 * `ActivityScenario.launch`): a screen's ViewModel keeps the source it was built with, so a swap
 * after launch might never reach My edits or Home. The edits source holds [EDIT_COUNT] photos,
 * newest first, so tile 3 is photo 3 of [editedPhotos]. The test runs on a phone-sized screen, so
 * tile 3 is in view without scrolling under the floating bar.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class MyEditsViewerNavigationTest {

    /** Finds Compose nodes in the activity the test starts. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin: its global state outlives the test inside the same JVM. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * A tap on tile 3 opens the viewer on photo 3 with every part the viewer has (round Share with
     * the description "Share" and no text, the wide "Edit", round close, the title "My edits"),
     * and none of the parts it must not have (the success mark, the "saved" line, the tab bar).
     */
    @Test
    fun a_tile_opens_the_viewer_on_its_photo_with_share_edit_close_and_no_bar() {
        launchToViewer {
            compose.onNode(
                hasTestTag(SAVED_PHOTO_TAG) and SemanticsMatcher.expectValue(SAVED_PHOTO_URI_KEY, photo3.uri),
            ).assertIsDisplayed()

            // Share: round, named "Share", no visible text.
            val share = compose.onNodeWithTag(SAVED_SHARE_TAG)
                .assertIsDisplayed()
                .assert(hasContentDescription(app.getString(R.string.saved_share)))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
                .fetchSemanticsNode()
            assertRound("Share", share.size.width, share.size.height)

            // The wide button reads exactly "Edit".
            val edit = compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG).assertIsDisplayed().fetchSemanticsNode()
            val editTexts = edit.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
            assertEquals(listOf(app.getString(R.string.saved_layout_viewer_edit)), editTexts)
            assertEquals("Edit", app.getString(R.string.saved_layout_viewer_edit))
            assertTrue(
                "The Edit button (${edit.size.width} px) is not at least twice Share's width (${share.size.width} px)",
                edit.size.width >= 2 * share.size.width,
            )

            // Close: round, named.
            val close = compose.onNodeWithTag(SAVED_CLOSE_TAG)
                .assertIsDisplayed()
                .assert(hasContentDescription(app.getString(R.string.saved_layout_close)))
                .fetchSemanticsNode()
            assertRound("Close", close.size.width, close.size.height)

            // The title.
            compose.onNode(hasTestTag(SAVED_TITLE_TAG) and hasText("My edits")).assertIsDisplayed()

            // No mark, no "saved" line, no tab bar or Camera button beside it.
            assertEquals(0, count(SAVED_MARK_TAG))
            assertEquals(0, count(SAVED_MESSAGE_TAG))
            assertEquals(0, count(KANSO_TAB_BAR_TAG))
            assertEquals(0, count(KANSO_CAMERA_BUTTON_TAG))
        }
    }

    /** Share starts the system chooser wrapping an ACTION_SEND of photo 3's content Uri. */
    @Test
    fun share_in_the_viewer_starts_the_chooser_for_the_photo() {
        launchToViewer {
            // Activity starts recorded so far (the app's own launch) are not the share.
            startedIntents()
            compose.onNodeWithTag(SAVED_SHARE_TAG).performClick()

            var chooser: Intent? = null
            waitFor {
                chooser = startedIntents().firstOrNull { it.action == Intent.ACTION_CHOOSER }
                chooser != null
            }
            val send = IntentCompat.getParcelableExtra(chooser!!, Intent.EXTRA_INTENT, Intent::class.java)
            assertNotNull("The chooser wraps no intent", send)
            assertEquals(Intent.ACTION_SEND, send!!.action)
            assertEquals("image/jpeg", send.type)
            assertEquals(Uri.parse(photo3.uri), IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
            assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertTrue(chooser!!.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        }
    }

    /** Edit opens the Edit tool screen on photo 3: its title is "Edit" and its image is photo 3. */
    @Test
    fun edit_in_the_viewer_opens_the_edit_tool_on_the_photo() {
        launchToViewer {
            compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG).performClick()
            waitFor { count(TOOL_SCREEN_TAG) > 0 && count(SAVED_SCREEN_TAG) == 0 }

            compose.onNode(
                hasText(app.getString(R.string.tool_edit)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
            ).assertExists()
            waitFor {
                compose.onAllNodes(
                    hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, photo3.uri),
                ).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    /** The round close button shows Home, with neither the viewer nor My edits left on screen. */
    @Test
    fun close_in_the_viewer_shows_home() {
        launchToViewer {
            compose.onNodeWithTag(SAVED_CLOSE_TAG).performClick()
            waitFor { count(SAVED_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
            assertEquals(0, count(MY_EDITS_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertIsDisplayed()
        }
    }

    /** System Back from the viewer shows My edits again, with tile 3 still on screen. */
    @Test
    fun back_from_the_viewer_shows_my_edits_with_the_tile_on_screen() {
        launchToViewer { scenario ->
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(SAVED_SCREEN_TAG) == 0 && count(MY_EDITS_SCREEN_TAG) > 0 }
            waitFor { count(myEditsTileTag(3)) > 0 }
            compose.onNodeWithTag(MY_EDITS_SCREEN_TAG).assertIsDisplayed()
            compose.onNodeWithTag(myEditsTileTag(3)).assertIsDisplayed()
            assertEquals(0, count(HOME_SCREEN_TAG))
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Loads the fakes, starts the app, opens the My edits tab through the floating bar, taps tile
     * 3, waits until the viewer shows its photo and the screen move has settled (the bar is gone),
     * then calls [body] with the scenario.
     */
    private fun launchToViewer(body: (ActivityScenario<MainActivity>) -> Unit) {
        shadowOf(app).grantPermissions(*photoPermissionsFor(35).toTypedArray(), Manifest.permission.CAMERA)
        loadKoinModules(
            module {
                factory<CameraController> { FakeCameraController() }
                single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                single<PhotoSource> { FakePhotoSource(albums = emptyList(), allPhotos = emptyList()) }
                single<SavedPhotoSource> { FakeSavedPhotoSource(editedPhotos) }
            },
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome(compose)
            openTab(compose, KansoTab.MY_EDITS)
            waitFor { count(MY_EDITS_SCREEN_TAG) > 0 && count(myEditsTileTag(3)) > 0 }
            compose.onNodeWithTag(myEditsTileTag(3)).tapAboveBar()

            waitFor { count(SAVED_SCREEN_TAG) > 0 && count(MY_EDITS_SCREEN_TAG) == 0 }
            waitFor { count(KANSO_TAB_BAR_TAG) == 0 && count(SAVED_PHOTO_TAG) > 0 }
            body(scenario)
        }
    }

    /** Fails unless a node [width] by [height] pixels is round: as wide as tall, within 1 dp. */
    private fun assertRound(name: String, width: Int, height: Int) {
        val oneDp = with(compose.density) { 1f * density }
        assertTrue("$name is $width x $height px, not round", abs(width - height) <= oneDp)
    }

    /** Waits up to ten seconds for [condition], letting the main thread run in between. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Takes every activity start recorded so far, oldest first. */
    private fun startedIntents(): List<Intent> =
        generateSequence { shadowOf(app).nextStartedActivity }.toList()

    /** The photo tile 3 shows: the fourth newest of [editedPhotos]. */
    private val photo3: Photo
        get() = editedPhotos[3]

    /** Values shared by the tests. */
    private companion object {
        /** How many photos the edits source holds: enough for tile 3 and a row after it. */
        const val EDIT_COUNT = 6

        /**
         * The photos Kanso has saved, newest first, as My edits lists them: photo `i` has the
         * content Uri ending in `100 + i`, so no two tests' Uris look alike.
         */
        val editedPhotos: List<Photo> = (0 until EDIT_COUNT).map { i ->
            Photo(
                id = 100L + i,
                uri = "content://media/external/images/media/${100 + i}",
                name = "Kanso_$i.jpg",
                albumId = 1L,
                modifiedSeconds = 1_791_000_000L - i * 3_600L,
            )
        }
    }
}
