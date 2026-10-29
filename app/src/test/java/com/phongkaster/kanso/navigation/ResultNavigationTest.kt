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
import com.phongkaster.kanso.ui.component.permission.model.photoPermissionsFor
import com.phongkaster.kanso.ui.result.RESULT_HOME_TAG
import com.phongkaster.kanso.ui.result.RESULT_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_DONE_TAG
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
 * Beautify opens its gallery; the gallery's camera button opens the camera; Keep opens the
 * Beautify tool screen; Done opens the Result screen above it; Back returns to the tool screen and Go home leaves only
 * Home.
 *
 * Run 7, DoD 12: each tool has a screen of its own, so the tool Done is tapped on is the screen's own
 * tool (Run 9: "Edit another" no longer exists). Home's tool
 * galleries are reached through `openToolGallery` (HomeTaps.kt) and Home is recognised by its
 * tag, never by a word on it. Like every walk, the back stack is read from what is on screen and
 * from where Back leads.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ResultNavigationTest {

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

    /** Gallery camera button, Keep: the Beautify tool screen opens, not the Edit one. */
    @Test
    fun gallery_camera_keep_opens_the_galleries_own_tool() {
        launchToResult(finish = false) {
            compose.onNode(
                hasText(app.getString(R.string.tool_beautify)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
            ).assertExists()
        }
    }

    /**
     * Run 9, DoD 9 and 11: Done opens Result above the tool screen (the tool screen is still below
     * it), and system Back from Result returns to that same tool screen.
     */
    @Test
    fun back_from_result_returns_to_the_tool_screen() {
        launchToResult { scenario ->
            assertEquals(1, count(RESULT_SCREEN_TAG))
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(RESULT_SCREEN_TAG) == 0 && count(TOOL_SCREEN_TAG) > 0 }
            compose.onNodeWithTag(TOOL_DONE_TAG).assertExists()
        }
    }

    /**
     * Run 9 (was: the close button of Saved), DoD 10: Go home leaves only [Home]. No Result, tool
     * or gallery screen is left below it: Home is shown, and one further Back finishes the
     * activity, which it only does when Home is the last key.
     */
    @Test
    fun go_home_leaves_only_home() {
        launchToResult { scenario ->
            compose.onNodeWithTag(RESULT_HOME_TAG).performClick()
            waitFor { count(RESULT_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertEquals(0, count(GALLERY_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            shadowOf(Looper.getMainLooper()).idle()
            var finishing = false
            scenario.onActivity { finishing = it.isFinishing }
            assertTrue("One Back after Go home did not finish the activity: something is left above Home", finishing)
        }
    }

    /**
     * Runs the walk Home, Beautify's gallery, the gallery's camera button, shutter, Keep, then
     * (when [finish]) Done, and calls [body] with the scenario at the end of it.
     *
     * The fakes are loaded before the app starts: a screen's ViewModel keeps the sources it was
     * built with, so a later swap might never reach it.
     */
    private fun launchToResult(finish: Boolean = true, body: (ActivityScenario<MainActivity>) -> Unit) {
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
            openToolGallery(compose, Tool.BEAUTIFY)
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
                    compose.onAllNodes(hasTestTag(TOOL_DONE_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag(TOOL_DONE_TAG).performClick()
                waitFor { count(RESULT_SCREEN_TAG) > 0 }
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
