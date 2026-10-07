package com.phongkaster.kanso.camera

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.RecordingRegistry
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.camera.CAMERA_FLASH_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SWITCH_LENS_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_ZOOM_AREA_TAG
import com.phongkaster.kanso.ui.camera.CameraLens
import com.phongkaster.kanso.ui.camera.CameraScreen
import com.phongkaster.kanso.ui.camera.CameraViewModel
import com.phongkaster.kanso.ui.camera.FlashMode
import com.phongkaster.kanso.ui.gallery.GALLERY_CAMERA_TAG
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks the camera's controls with a [FakeCameraController] in place of the real camera: the
 * lens switch, the flash button's auto, on, off cycle (and its absence on a lens with no flash),
 * pinch and programmatic zoom, and the gallery's top-bar camera button.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CameraControlsTest {

    /** Starts an empty activity to host the screen under test. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Grants camera access, so the camera screen shows the camera straight away. */
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

    /** Each tap on the lens switch swaps between the back and the front lens. */
    @Test
    fun the_lens_switch_toggles_the_lens() {
        val fake = FakeCameraController()
        showCamera(fake)
        assertEquals(CameraLens.Back, fake.state.value.lens)

        compose.onNodeWithTag(CAMERA_SWITCH_LENS_TAG).performClick()
        compose.waitForIdle()
        assertEquals(CameraLens.Front, fake.state.value.lens)

        compose.onNodeWithTag(CAMERA_SWITCH_LENS_TAG).performClick()
        compose.waitForIdle()
        assertEquals(CameraLens.Back, fake.state.value.lens)
    }

    /** A phone with one camera shows no lens switch. */
    @Test
    fun the_lens_switch_is_absent_with_one_camera() {
        showCamera(FakeCameraController(canSwitchLens = false))
        assertEquals(0, count(CAMERA_SWITCH_LENS_TAG))
    }

    /** The flash button moves auto, on, off, auto, and its description names the mode. */
    @Test
    fun the_flash_cycles_auto_on_off() {
        val fake = FakeCameraController()
        showCamera(fake)

        val expected = listOf(
            FlashMode.Auto to R.string.camera_flash_auto,
            FlashMode.On to R.string.camera_flash_on,
            FlashMode.Off to R.string.camera_flash_off,
            FlashMode.Auto to R.string.camera_flash_auto,
        )
        expected.forEachIndexed { index, (mode, label) ->
            if (index > 0) {
                compose.onNodeWithTag(CAMERA_FLASH_TAG).performClick()
                compose.waitForIdle()
            }
            assertEquals(mode, fake.state.value.flashMode)
            compose.onNodeWithTag(CAMERA_FLASH_TAG)
                .assertContentDescriptionEquals(app.getString(label))
        }
    }

    /** A lens with no flash shows no flash button, while the shutter and lens switch remain. */
    @Test
    fun the_flash_button_is_absent_without_a_flash() {
        showCamera(FakeCameraController(hasFlash = false))
        assertEquals(0, count(CAMERA_FLASH_TAG))
        assertEquals(1, count(CAMERA_SWITCH_LENS_TAG))
        assertEquals(1, count(CAMERA_SHUTTER_TAG))
    }

    /** Zooming by a factor changes the zoom, held within the lens's limits. */
    @Test
    fun zoom_changes_the_state_within_limits() {
        val fake = FakeCameraController(maxZoom = 4f)
        val viewModel = showCamera(fake)
        assertEquals(1f, fake.state.value.zoomRatio, 0.001f)

        viewModel.zoomBy(2f)
        assertEquals(2f, fake.state.value.zoomRatio, 0.001f)

        viewModel.zoomBy(10f)
        assertEquals(4f, fake.state.value.zoomRatio, 0.001f)

        viewModel.zoomBy(0.01f)
        assertEquals(1f, fake.state.value.zoomRatio, 0.001f)
    }

    /** Spreading two fingers over the live picture zooms in. */
    @Test
    fun a_pinch_over_the_picture_zooms_in() {
        val fake = FakeCameraController(maxZoom = 4f)
        showCamera(fake)

        compose.onNodeWithTag(CAMERA_ZOOM_AREA_TAG).performTouchInput {
            val spread = width / 3f
            pinch(
                start0 = center - Offset(10f, 0f),
                end0 = center - Offset(spread, 0f),
                start1 = center + Offset(10f, 0f),
                end1 = center + Offset(spread, 0f),
            )
        }
        compose.waitForIdle()

        assertTrue(
            "A pinch out did not zoom in: ${fake.state.value.zoomRatio}",
            fake.state.value.zoomRatio > 1.05f,
        )
    }

    /** The gallery's camera button calls onOpenCamera once with the gallery's tool. */
    @Test
    fun the_gallery_camera_button_opens_the_camera_for_its_tool() {
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        val opened = mutableListOf<Tool>()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = RecordingRegistry()
        }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = GalleryViewModel(
            Tool.RETOUCH,
            ViewModelClearedProbe(),
            FakePhotoSource(emptyList(), emptyList()),
        )
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                MaterialTheme {
                    GalleryScreen(
                        viewModel = viewModel,
                        onBack = {},
                        onPhotoChosen = { _, _ -> },
                        onOpenCamera = { opened += it },
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag(GALLERY_CAMERA_TAG)
            .assertContentDescriptionEquals(app.getString(R.string.gallery_open_camera))
            .performClick()
        compose.waitForIdle()

        assertEquals(listOf(Tool.RETOUCH), opened)
    }

    /**
     * Shows the camera screen with [fake] as its camera and waits until the shutter can be used,
     * which means the fake camera is connected. Returns the screen's ViewModel.
     */
    private fun showCamera(fake: FakeCameraController): CameraViewModel {
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = CameraViewModel(controller = fake, probe = ViewModelClearedProbe())
        compose.setContent {
            MaterialTheme {
                CameraScreen(viewModel = viewModel, onBack = {}, onKept = {})
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(enabledShutter).fetchSemanticsNodes().isNotEmpty()
        }
        return viewModel
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
