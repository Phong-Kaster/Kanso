package com.phongkaster.kanso.camera

import android.Manifest
import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.ui.camera.CAMERA_BACK_TAG
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
 * file and brings the shutter back. Keep and Retake are labelled buttons of at least 48 dp.
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

    /** How many times the screen called onBack. */
    private var backTaps = 0

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

    /**
     * Once a shot waits, Keep and Retake keep their tags and their visible words ("Keep",
     * "Retake"; each button's icon has no spoken name of its own, so the word is the button's
     * only text), can be tapped, and are at least 48 dp tall and wide (DoD 8).
     */
    @Test
    fun keep_and_retake_are_labelled_buttons_of_at_least_48_dp() {
        showCamera()
        takeShot()

        val minimumPx = with(compose.density) { 48.dp.toPx() }
        listOf(CAMERA_KEEP_TAG to R.string.camera_keep, CAMERA_RETAKE_TAG to R.string.camera_retake)
            .forEach { (tag, label) ->
                val button = compose.onNodeWithTag(tag)
                    .assertHasClickAction()
                    .assertTextEquals(app.getString(label))
                val bounds = button.fetchSemanticsNode().boundsInRoot
                // Half a pixel of slack for rounding between dp and pixels.
                assertTrue(
                    "$tag is ${bounds.width} x ${bounds.height} px, smaller than 48 dp ($minimumPx px)",
                    bounds.width >= minimumPx - 0.5f && bounds.height >= minimumPx - 0.5f,
                )
            }
    }

    /**
     * The top bar's round Back button (tag `camera-back`) is named "Back", is a 48 dp by 48 dp
     * square box drawn as a circle by the shared round button, and a tap calls onBack once.
     */
    @Test
    fun the_round_back_button_is_named_48_dp_and_calls_back() {
        showCamera()

        compose.onNodeWithTag(CAMERA_BACK_TAG)
            .assertContentDescriptionEquals(app.getString(R.string.back))
            .assertHasClickAction()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .performClick()
        compose.waitForIdle()

        assertEquals(1, backTaps)
    }

    /** Shows the camera screen, with the fake camera, and waits until the shutter can be used. */
    private fun showCamera() {
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = CameraViewModel(controller = fake, probe = ViewModelClearedProbe())
        compose.setContent {
            MaterialTheme {
                CameraScreen(
                    viewModel = viewModel,
                    onBack = { backTaps++ },
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
