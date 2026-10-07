package com.phongkaster.kanso.camera

import android.Manifest
import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.ui.camera.CAMERA_KEEP_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_REFUSED_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_RETAKE_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHOT_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraScreen
import com.phongkaster.kanso.ui.camera.CameraViewModel
import com.phongkaster.kanso.ui.camera.shotsDirectory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks the camera screen on its own, with camera access granted and a [FakeCameraController]
 * in place of the real camera: the shutter takes a shot into the app's private `shots/` folder,
 * Keep hands that shot's `file://` Uri to the caller and keeps the file, and Retake deletes the
 * file and brings the shutter back.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CameraScreenTest {

    /** Starts an empty activity to host the camera screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The camera the screen is given. */
    private val fake = FakeCameraController()

    /** Every Uri the screen handed to onKept, oldest first. */
    private val keptUris = mutableListOf<String>()

    /** Grants camera access, so the screen shows the camera straight away. */
    @Before
    fun grantCamera() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
    }

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** The shutter writes a shot into `shots/`, and the shot is shown with Keep and Retake. */
    @Test
    fun the_shutter_takes_a_shot_into_private_storage() {
        showCamera()
        takeShot()

        val file = fake.takenFiles.single()
        assertTrue("The shot was not written", file.exists() && file.length() > 0)
        assertEquals(shotsDirectory(app).canonicalFile, file.parentFile!!.canonicalFile)
        compose.onNodeWithTag(CAMERA_SHOT_TAG).assertExists()
        compose.onNodeWithTag(CAMERA_RETAKE_TAG).assertExists()
        assertEquals(0, count(CAMERA_SHUTTER_TAG))
        assertEquals(0, count(CAMERA_REFUSED_TAG))
    }

    /** Keep calls onKept once with the shot's file:// Uri, and the file stays. */
    @Test
    fun keep_hands_over_the_shot_uri_and_keeps_the_file() {
        showCamera()
        takeShot()
        val file = fake.takenFiles.single()

        compose.onNodeWithTag(CAMERA_KEEP_TAG).performClick()
        compose.waitForIdle()

        assertEquals(listOf(Uri.fromFile(file).toString()), keptUris)
        assertTrue(keptUris.single().startsWith("file://"))
        assertTrue("A kept shot must not be deleted", file.exists())
    }

    /** Retake deletes the shot's file and shows the shutter again, ready for another shot. */
    @Test
    fun retake_deletes_the_shot_and_shows_the_shutter_again() {
        showCamera()
        takeShot()
        val file = fake.takenFiles.single()
        assertTrue(file.exists())

        compose.onNodeWithTag(CAMERA_RETAKE_TAG).performClick()
        waitForEnabledShutter()

        assertFalse("Retake left the shot's file behind", file.exists())
        assertEquals(0, count(CAMERA_SHOT_TAG))
        assertEquals(0, count(CAMERA_KEEP_TAG))
        assertTrue(keptUris.isEmpty())
        // The live picture came back, so the camera was connected a second time.
        assertEquals(2, fake.bindCount)

        // A second shot works after a retake.
        takeShot()
        assertEquals(2, fake.takenFiles.size)
    }

    /** Shows the camera screen, with the fake camera, and waits until the shutter can be used. */
    private fun showCamera() {
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = CameraViewModel(controller = fake, probe = ViewModelClearedProbe())
        compose.setContent {
            MaterialTheme {
                CameraScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onKept = { keptUris += it },
                )
            }
        }
        waitForEnabledShutter()
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
            compose.onAllNodes(enabledShutter).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Values the tests share. */
    private companion object {
        /** Matches the shutter button while it can be pressed. */
        val enabledShutter: SemanticsMatcher = hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()
    }
}
