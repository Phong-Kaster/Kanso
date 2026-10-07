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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_TAG
import com.phongkaster.kanso.ui.saved.SAVED_HOME_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
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
 * leaves the Retouch gallery above Home; Home and system Back both leave only Home.
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

    /** Edit another leaves [Home, Gallery(Retouch)]: the gallery shows and Back reaches Home. */
    @Test
    fun edit_another_leaves_home_and_the_tools_gallery() {
        launchToSaved { scenario ->
            compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG).performClick()
            waitFor { count(GALLERY_SCREEN_TAG) > 0 && count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(GALLERY_SCREEN_TAG) == 0 }
            compose.onNodeWithText(app.getString(R.string.tool_camera)).assertExists()
        }
    }

    /** Home leaves [Home]: no Saved, tool or gallery screen below it. */
    @Test
    fun home_leaves_only_home() {
        launchToSaved {
            compose.onNodeWithTag(SAVED_HOME_TAG).performClick()
            waitFor { count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertEquals(0, count(GALLERY_SCREEN_TAG))
            compose.onNodeWithText(app.getString(R.string.tool_camera)).assertExists()
        }
    }

    /** System Back from Saved goes to Home, not to the tool screen. */
    @Test
    fun back_from_saved_goes_home() {
        launchToSaved { scenario ->
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            compose.onNodeWithText(app.getString(R.string.tool_camera)).assertExists()
        }
    }

    /**
     * Runs the walk Home, Retouch, gallery camera button, shutter, Keep, then (when [finish]) Save,
     * and calls [body] with the scenario at the end of it.
     */
    private fun launchToSaved(finish: Boolean = true, body: (ActivityScenario<MainActivity>) -> Unit) {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA, *photoPermissionsFor(35).toTypedArray())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadKoinModules(
                module {
                    factory<CameraController> { FakeCameraController() }
                    single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                    single<PhotoSource> { FakePhotoSource(albums = emptyList(), allPhotos = emptyList()) }
                    single<PhotoSaver> { FakePhotoSaver() }
                },
            )
            val label = app.getString(R.string.tool_retouch)
            waitFor { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(label).performScrollTo().performClick()
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
