package com.phongkaster.kanso.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.camera.CAMERA_FLASH_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SWITCH_LENS_TAG
import com.phongkaster.kanso.ui.camera.CameraContent
import com.phongkaster.kanso.ui.camera.CameraFrame
import com.phongkaster.kanso.ui.camera.CameraState
import com.phongkaster.kanso.ui.camera.CameraStatus
import com.phongkaster.kanso.ui.camera.CameraUiState
import com.phongkaster.kanso.ui.camera.FlashMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Takes the reference pictures of the camera before a shot, in light and in dark (pictures 20
 * and 21): the top bar with the Back icon button and the title, the live picture, and below it
 * the flash button set to auto, the round shutter ready to press and the lens switch.
 *
 * The camera's stateless parts are hosted directly in KansoTheme with the mode forced, on Android
 * 15 and the same phone screen as [ScreenshotTest]: the frame (the top bar) around the camera
 * content, given a ready camera with a flash and a second lens. Robolectric cannot run a real
 * camera, so the live picture is a drawn stand-in (a sky-to-field gradient, the same colours as
 * the screen's own preview). Nothing on this screen keeps moving, so the picture is the same on
 * every run.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = CAMERA_CONTROLS_PHONE)
class CameraControlsScreenshotTest {

    /** Starts an empty activity to host the camera content. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Picture 20: the camera before a shot, in light mode. */
    @Test
    fun camera_live_light() {
        showLiveCamera(ThemeMode.Light)
        capture("20_camera_live_light.png")
    }

    /** Picture 21: the camera before a shot, in dark mode. */
    @Test
    fun camera_live_dark() {
        showLiveCamera(ThemeMode.Dark)
        capture("21_camera_live_dark.png")
    }

    /**
     * Shows the camera frame and content in [mode] with a ready camera and the drawn stand-in
     * picture, and checks that the three bottom controls are on screen and the shutter can be
     * pressed.
     */
    private fun showLiveCamera(mode: ThemeMode) {
        compose.setContent {
            KansoTheme(themeMode = mode) {
                CameraFrame(onBack = {}) {
                    CameraContent(
                        state = liveState,
                        onShutter = {},
                        onKeep = {},
                        onRetake = {},
                        onFlash = {},
                        onSwitchLens = {},
                        onZoom = {},
                        livePicture = { pictureModifier ->
                            Box(pictureModifier.background(Brush.verticalGradient(standInColours)))
                        },
                        shotPicture = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(CAMERA_SHUTTER_TAG).assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag(CAMERA_FLASH_TAG).assertIsDisplayed()
        compose.onNodeWithTag(CAMERA_SWITCH_LENS_TAG).assertIsDisplayed()
    }

    /** Saves a picture of the whole window as `src/test/screenshots/[name]`. */
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name")
    }

    /** Values the tests share. */
    private companion object {
        /** A camera that is open and ready, with its flash on auto and a second lens. */
        val liveState = CameraUiState(
            camera = CameraState(
                status = CameraStatus.Ready,
                canSwitchLens = true,
                flashMode = FlashMode.Auto,
                hasFlash = true,
            ),
        )

        /** The stand-in picture's colours, top to bottom: an evening sky fading into a field. */
        val standInColours = listOf(Color(0xFF7FB3D5), Color(0xFFF5CBA7), Color(0xFF58804A))
    }
}

/**
 * The Robolectric screen the pictures are drawn on, the same as [ScreenshotTest]'s: 411 dp wide,
 * 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val CAMERA_CONTROLS_PHONE = "w411dp-h891dp-xxhdpi"
