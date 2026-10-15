package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.camera.FakeCameraController
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.save.FakePhotoSaver
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CAMERA_KEEP_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.gallery.GALLERY_CAMERA_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_TAG
import com.phongkaster.kanso.ui.saved.SAVED_CLOSE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
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
 * DoD 6 (gallery path) and DoD 15 (back stack), through the app's real navigation: from Home,
 * Retouch opens its gallery; the gallery's camera button opens the camera; Keep opens the
 * Retouch tool screen; Save replaces everything above Home with the Saved screen; Edit another
 * leaves the Retouch gallery above Home; the round close button and system Back both leave only
 * Home.
 *
 * Run 7, DoD 12: each tool has a screen of its own, so the tool saved from is the screen's own
 * tool, and "Edit another" opens the gallery titled "Retouch: choose a photo". Home's tool
 * galleries are reached through `openToolGallery` (HomeTaps.kt) and Home is recognised by its
 * tag, never by a word on it. Like every walk, the back stack is read from what is on screen and
 * from where Back leads.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SavedNavigationTest {

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

    /** Gallery camera button, Keep: the Retouch tool screen opens, not the Edit one. */
    @Test
    fun gallery_camera_keep_opens_the_galleries_own_tool() {
        launchToSaved(finish = false) {
            compose.onNode(
                hasText(app.getString(R.string.tool_retouch)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
            ).assertExists()
        }
    }

    /**
     * Edit another leaves [Home, Gallery(Retouch)]: the gallery titled "Retouch: choose a photo"
     * shows, and Back reaches Home.
     */
    @Test
    fun edit_another_leaves_home_and_the_tools_gallery() {
        launchToSaved { scenario ->
            compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG).performClick()
            waitFor { count(GALLERY_SCREEN_TAG) > 0 && count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            val title = app.getString(R.string.gallery_title, app.getString(R.string.tool_retouch))
            waitFor { compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(GALLERY_SCREEN_TAG) == 0 }
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        }
    }

    /**
     * Run 7, DoD 12 and R7: the round close button at the top left leaves [Home] alone. No Saved,
     * tool or gallery screen is left below it: Home is shown, and one further Back finishes the
     * activity, which it only does when Home is the last key.
     */
    @Test
    fun close_leaves_only_home() {
        launchToSaved { scenario ->
            compose.onNodeWithTag(SAVED_CLOSE_TAG).performClick()
            waitFor { count(SAVED_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertEquals(0, count(GALLERY_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            shadowOf(Looper.getMainLooper()).idle()
            var finishing = false
            scenario.onActivity { finishing = it.isFinishing }
            assertTrue("One Back after close did not finish the activity: something is left above Home", finishing)
        }
    }

    /** System Back from Saved goes to Home, not to the tool screen. */
    @Test
    fun back_from_saved_goes_home() {
        launchToSaved { scenario ->
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        }
    }

    /**
     * Runs the walk Home, Retouch's gallery, the gallery's camera button, shutter, Keep, then
     * (when [finish]) Save, and calls [body] with the scenario at the end of it.
     *
     * The fakes are loaded before the app starts: a screen's ViewModel keeps the sources it was
     * built with, so a later swap might never reach it.
     */
    private fun launchToSaved(finish: Boolean = true, body: (ActivityScenario<MainActivity>) -> Unit) {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA, *photoPermissionsFor(35).toTypedArray())
        loadKoinModules(
            module {
                factory<CameraController> { FakeCameraController() }
                single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                single<PhotoSource> { FakePhotoSource(albums = emptyList(), allPhotos = emptyList()) }
                single<PhotoSaver> { FakePhotoSaver() }
            },
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openToolGallery(compose, Tool.RETOUCH)
            waitFor { count(GALLERY_CAMERA_TAG) > 0 }
            compose.onNodeWithTag(GALLERY_CAMERA_TAG).performClick()
            waitFor {
                compose.onAllNodes(hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(CAMERA_SHUTTER_TAG).performClick()
            waitFor { count(CAMERA_KEEP_TAG) > 0 }
            compose.onNodeWithTag(CAMERA_KEEP_TAG).performClick()
            waitFor { count(TOOL_SCREEN_TAG) > 0 }
            if (finish) {
                waitFor {
                    compose.onAllNodes(hasTestTag(TOOL_SAVE_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
                waitFor { count(SAVED_SCREEN_TAG) > 0 }
                assertEquals(0, count(TOOL_SCREEN_TAG))
            }
            body(scenario)
        }
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
}
