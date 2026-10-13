package com.phongkaster.kanso.camera

import android.Manifest
import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.navigation.tapCameraButton
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CAMERA_KEEP_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_RETAKE_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SCREEN_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHOT_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * DoD 6, through the app's real navigation: the round Camera button beside the floating tab bar
 * on Home opens the camera; Keep replaces the camera with the Edit tool on the shot; Retake
 * leaves the camera on top with the shutter back and the shot's file deleted.
 *
 * The app is started as a person would. Camera access is granted beforehand, and the camera and
 * the photo decoder are swapped for fakes through Koin before the camera is opened (the fake
 * camera writes a fixture JPEG).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CameraFlowTest {

    /** Finds Compose nodes in the activity the test starts. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The camera the app is given in place of CameraX. */
    private val fake = FakeCameraController()

    /** Stops Koin: its global state outlives the test inside the same JVM. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Home, Camera, shutter, Keep: the Edit tool shows the shot, and the camera is gone. */
    @Test
    fun keep_opens_the_edit_tool_on_the_shot() {
        openCamera {
            takeShot()
            val file = fake.takenFiles.single()

            compose.onNodeWithTag(CAMERA_KEEP_TAG).performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                shadowOf(Looper.getMainLooper()).idle()
                count(TOOL_SCREEN_TAG) > 0 && count(CAMERA_SCREEN_TAG) == 0
            }

            compose.onNodeWithTag(TOOL_SCREEN_TAG).assertExists()
            compose.onNode(
                hasTestTag(TOOL_IMAGE_TAG) and
                    SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, Uri.fromFile(file).toString()),
            ).assertExists()
            // The Edit tool's title is shown on the tool screen.
            compose.onNode(
                hasText(app.getString(R.string.tool_edit)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
            ).assertExists()
            assertEquals(0, count(CAMERA_SCREEN_TAG))
            assertTrue("A kept shot must not be deleted", file.exists())
        }
    }

    /** Home, Camera, shutter, Retake: still on the camera, shutter back, shot file deleted. */
    @Test
    fun retake_stays_on_the_camera_and_deletes_the_shot() {
        openCamera {
            takeShot()
            val file = fake.takenFiles.single()
            assertTrue(file.exists())

            compose.onNodeWithTag(CAMERA_RETAKE_TAG).performClick()
            waitForEnabledShutter()

            compose.onNodeWithTag(CAMERA_SCREEN_TAG).assertExists()
            compose.onNodeWithTag(CAMERA_SHUTTER_TAG).assertExists()
            assertEquals(0, count(CAMERA_SHOT_TAG))
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertFalse("Retake left the shot's file behind", file.exists())
        }
    }

    /**
     * Grants camera access, starts the app, swaps in the fakes, taps the Camera button on Home's
     * tab bar (by its tag, `nav-camera`) and waits until the shutter can be used, then runs [body].
     */
    private fun openCamera(body: () -> Unit) {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        ActivityScenario.launch(MainActivity::class.java).use {
            // Koin lets a later module replace an earlier recipe with the same type.
            loadKoinModules(
                module {
                    factory<CameraController> { fake }
                    single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                },
            )
            tapCameraButton(compose)
            waitForEnabledShutter()
            body()
        }
    }

    /** Taps the shutter and waits until the shot is shown with its Keep button. */
    private fun takeShot() {
        compose.onNodeWithTag(CAMERA_SHUTTER_TAG).performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            // Lets the main thread run what the background write posted back to it.
            shadowOf(Looper.getMainLooper()).idle()
            count(CAMERA_KEEP_TAG) > 0
        }
    }

    /** Waits until the shutter is on screen and enabled (the fake camera is connected). */
    private fun waitForEnabledShutter() {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size
}
