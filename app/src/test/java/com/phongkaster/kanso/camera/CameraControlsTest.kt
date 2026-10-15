package com.phongkaster.kanso.camera

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.RecordingRegistry
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.camera.CAMERA_FLASH_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_LOADING_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_INNER_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SWITCH_LENS_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_ZOOM_AREA_TAG
import com.phongkaster.kanso.ui.camera.CameraContent
import com.phongkaster.kanso.ui.camera.CameraLens
import com.phongkaster.kanso.ui.camera.CameraScreen
import com.phongkaster.kanso.ui.camera.CameraState
import com.phongkaster.kanso.ui.camera.CameraStatus
import com.phongkaster.kanso.ui.camera.CameraUiState
import com.phongkaster.kanso.ui.camera.CameraViewModel
import com.phongkaster.kanso.ui.camera.FlashMode
import com.phongkaster.kanso.ui.camera.shotsDirectory
import com.phongkaster.kanso.ui.gallery.GALLERY_CAMERA_TAG
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.theme.KansoTheme
import java.time.Duration
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
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the camera's controls with a [FakeCameraController] in place of the real camera: the
 * lens switch, the flash button's auto, on, off cycle (and its absence on a lens with no flash),
 * pinch and programmatic zoom, and the gallery's top-bar camera button.
 *
 * It also checks the controls' Material 3 Expressive form (DoD 8): Back, flash and the lens
 * switch are icon buttons with a spoken name, no visible text and at least 48 dp each way; the
 * shutter's inner ring visibly shrinks while a finger is on the shutter, one tap takes exactly one
 * photo, and the shutter is drawn as before once the finger has lifted; the bottom controls stay
 * out of the bottom gesture zone when the navigation bar is 0 dp tall; and the picture area shows
 * the Expressive loading indicator while the camera is opening (DoD 9).
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
     * Back (in the top bar), the flash button and the lens switch are icon buttons: each can be
     * tapped, has a spoken name and no visible text, and is at least 48 dp wide and tall, the
     * smallest touch target Android recommends. The flash button's name is exactly the label of
     * its mode, as [the_flash_cycles_auto_on_off] reads it.
     *
     * The screen runs with the test clock paused, as DoD 8 asks, in KansoTheme.
     */
    @Test
    fun back_flash_and_lens_switch_are_named_icon_buttons_without_text_of_at_least_48_dp() {
        showCameraWithPausedClock(FakeCameraController(hasFlash = true, canSwitchLens = true))

        assertIconButton(
            name = "Back",
            node = compose.onNodeWithContentDescription(app.getString(R.string.back)),
            description = app.getString(R.string.back),
        )
        assertIconButton(
            name = "The flash button",
            node = compose.onNodeWithTag(CAMERA_FLASH_TAG),
            description = app.getString(R.string.camera_flash_auto),
        )
        assertIconButton(
            name = "The lens switch",
            node = compose.onNodeWithTag(CAMERA_SWITCH_LENS_TAG),
            description = app.getString(R.string.camera_switch_lens),
        )
    }

    /**
     * The shutter answers the press: 150 ms after a finger touches it, its inner ring is drawn
     * differently from how it looked at rest (it shrinks, and the button squares its corners);
     * lifting the finger takes exactly one photo; and 1 s after the release the shutter is drawn
     * exactly as before the press.
     *
     * The test pauses Compose's clock before showing anything, so time moves only when the test
     * moves it, and draws the screen into a bitmap at chosen moments, as `GalleryGridTest`
     * does for the photo cells (Compose's captureToImage times out under Robolectric here). Each time the test clock
     * moves, Robolectric's own clock moves by as much, so Android's ripple behind the press (an
     * Android drawable that runs on that clock, not on Compose's) plays out and fades like on a
     * phone, instead of being left frozen mid-way.
     *
     * The screen is the stateless camera content showing a ready camera, so it stays on the live
     * picture whatever happens to the photo; the shutter calls a real [CameraViewModel] over a
     * [FakeCameraController], which records every photo it is asked to take.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun the_shutter_ring_shrinks_while_pressed_takes_one_photo_and_springs_back() {
        val fake = FakeCameraController(hasFlash = true, canSwitchLens = true)
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = CameraViewModel(controller = fake, probe = ViewModelClearedProbe())
        val folder = shotsDirectory(app)
        var shutterCalls = 0

        // Pause the clock before anything is shown: from here on, time moves only by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                CameraContent(
                    state = readyState,
                    onShutter = {
                        shutterCalls++
                        viewModel.takePicture(folder)
                    },
                    onKeep = {},
                    onRetake = {},
                    onFlash = {},
                    onSwitchLens = {},
                    onZoom = {},
                    // A plain stand-in: the live picture plays no part in this test.
                    livePicture = { pictureModifier -> Box(pictureModifier) },
                    shotPicture = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        advanceBothClocks(SETTLE_MS)

        val shutter = compose.onNodeWithTag(CAMERA_SHUTTER_TAG).assertIsEnabled()
        // The ring is merged into the shutter's node for screen readers, so it is found in the
        // unmerged tree. Its resting bounds are where every picture below is read.
        val ring = compose.onNodeWithTag(CAMERA_SHUTTER_INNER_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val resting = pixelsInside(ring)
        // One pixel in the middle of the ring's 3 dp stroke, at its left-most point. The whole
        // region above also changes when the button squares its corners or its ripple starts, so
        // this pixel is what proves the RING itself shrank: once it shrinks, that spot is no
        // longer on the stroke.
        val strokeSpot = Rect(
            left = ring.left + 1.5f * app.resources.displayMetrics.density,
            top = ring.center.y,
            right = ring.left + 1.5f * app.resources.displayMetrics.density + 1f,
            bottom = ring.center.y + 1f,
        )
        val restingStroke = pixelsInside(strokeSpot).single()

        shutter.performTouchInput { down(center) }
        advanceBothClocks(PRESS_MS)
        val pressed = pixelsInside(ring)
        val pressedStroke = pixelsInside(strokeSpot).single()
        assertEquals("Touching the shutter must not take the photo yet", 0, shutterCalls)

        shutter.performTouchInput { up() }
        advanceBothClocks(AFTER_RELEASE_MS)
        val released = pixelsInside(ring)

        // The photo is written on a background thread; wait for the fake to record it.
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            fake.takenFiles.isNotEmpty()
        }
        assertEquals("One tap must call the shutter exactly once", 1, shutterCalls)
        assertEquals("One tap must take exactly one photo", 1, fake.takenFiles.size)
        assertFalse(
            "The shutter's ring looked the same $PRESS_MS ms into the press as at rest",
            resting.contentEquals(pressed),
        )
        assertFalse(
            "The ring's stroke did not move $PRESS_MS ms into the press: the ring did not shrink",
            restingStroke == pressedStroke,
        )
        assertTrue(
            "$AFTER_RELEASE_MS ms after the release the shutter was not drawn as before the press",
            resting.contentEquals(released),
        )
    }

    /**
     * The flash button, the shutter and the lens switch stay above the bottom gesture zone even
     * when the navigation bar is 0 dp tall (POLICIES.md, the bottom-edge rule).
     *
     * The bottom gesture zone is the strip where a swipe up means "go home"; a tap inside it can
     * bring up the phone's own navigation instead of taking a photo. With swipe gestures on and
     * the gesture hint off (as on the owner's Galaxy A13), the navigation bar's inset is 0 while
     * the zone is still there, so a bar padded by the navigation bar alone would keep only its own
     * 16 dp margin and end inside a zone [GESTURE_ZONE] tall.
     *
     * Robolectric draws no system bars, so the test hands the view the screen is drawn in the
     * window insets such a phone would: the gesture zone at the bottom, and every other inset, the
     * navigation bar's included, 0. It also reads both insets back as the screen sees them: if
     * they never arrived (a change in Robolectric or Compose), the test fails, because without
     * the zone it would prove nothing.
     */
    @Test
    fun the_bottom_controls_clear_the_gesture_zone_when_the_navigation_bar_is_zero_tall() {
        var composeView: View? = null
        var seenZonePx = -1
        var seenNavigationBarPx = -1
        showCamera(FakeCameraController(hasFlash = true, canSwitchLens = true)) {
            // LocalView is the Android view the screen is drawn in, the one that receives the
            // window insets; both insets are read here just as the screen reads them.
            val view = LocalView.current
            val localDensity = LocalDensity.current
            val zone = WindowInsets.systemGestures.getBottom(localDensity)
            val navigationBar = WindowInsets.navigationBars.getBottom(localDensity)
            // SideEffect hands the values out after each composition that succeeds.
            SideEffect {
                composeView = view
                seenZonePx = zone
                seenNavigationBarPx = navigationBar
            }
        }

        val zonePx = with(compose.density) { GESTURE_ZONE.roundToPx() }
        // Only the gesture zone is set, so every other inset, the navigation bar's included, is 0.
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(0, 0, 0, zonePx))
            .build()
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(checkNotNull(composeView), insets) }
        compose.waitForIdle()
        assertEquals("The screen did not see the gesture zone the test sent", zonePx, seenZonePx)
        assertEquals("The screen saw a navigation bar the test did not send", 0, seenNavigationBarPx)

        // The zone's top edge, counted from the top of the screen's root.
        val zoneTop = (compose.onRoot().fetchSemanticsNode().size.height - zonePx).toFloat()
        listOf(CAMERA_FLASH_TAG, CAMERA_SHUTTER_TAG, CAMERA_SWITCH_LENS_TAG).forEach { tag ->
            val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
            // positionInRoot plus the size is where the node is laid out, never clipped to the
            // screen as boundsInRoot is, so a control pushed off the bottom cannot look inside.
            val top = node.positionInRoot.y
            val bottom = top + node.size.height
            assertTrue("$tag starts at $top px, above the top of the screen", top >= 0f)
            assertTrue(
                "$tag ends at $bottom px, inside the gesture zone that starts at $zoneTop px",
                bottom <= zoneTop,
            )
        }
    }

    /**
     * While the camera is opening, the picture area shows the Expressive loading indicator, with
     * a spoken name, and the shutter cannot be pressed; once the camera is ready the indicator is
     * gone and the shutter works (DoD 9, the camera's part).
     *
     * [FakeCameraController] reports ready at once, so the opening state is handed to the
     * stateless camera content directly. The indicator animates for as long as it is shown, so
     * the test clock is paused before anything is drawn and moved on by hand: with the clock
     * running by itself, every wait for the screen to settle would wait forever.
     */
    @Test
    fun a_loading_indicator_shows_while_the_camera_is_opening_and_leaves_once_it_is_ready() {
        var state by mutableStateOf(readyState.copy(camera = readyState.camera.copy(status = CameraStatus.Starting)))

        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                CameraContent(
                    state = state,
                    onShutter = {},
                    onKeep = {},
                    onRetake = {},
                    onFlash = {},
                    onSwitchLens = {},
                    onZoom = {},
                    livePicture = { pictureModifier -> Box(pictureModifier) },
                    shotPicture = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        compose.mainClock.advanceTimeBy(INDICATOR_MS)

        compose.onNodeWithTag(CAMERA_LOADING_TAG)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(app.getString(R.string.camera_loading))
        compose.onNodeWithTag(CAMERA_SHUTTER_TAG).assertIsNotEnabled()

        // The camera reports that it is ready. sendApplyNotifications tells Compose about the
        // change at once, so the next frame of the paused clock draws it.
        compose.runOnIdle { state = readyState }
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(INDICATOR_MS)

        assertEquals("The loading indicator stayed after the camera was ready", 0, count(CAMERA_LOADING_TAG))
        compose.onNodeWithTag(CAMERA_SHUTTER_TAG).assertIsEnabled()
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Shows the camera screen with [fake] as its camera and waits until the shutter can be used,
     * which means the fake camera is connected. Returns the screen's ViewModel. [probe] is
     * composed just before the screen, for a test that needs to read something from inside the
     * composition (it draws nothing).
     */
    private fun showCamera(
        fake: FakeCameraController,
        probe: @Composable () -> Unit = {},
    ): CameraViewModel {
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = CameraViewModel(controller = fake, probe = ViewModelClearedProbe())
        compose.setContent {
            probe()
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

    /**
     * Like [showCamera], but pauses the test clock before the screen is shown and then moves it
     * on by hand, a tenth of a second at a time, until the shutter can be used. The screen runs
     * in KansoTheme, as in the app. Gives up after about ten seconds of the test's clock.
     */
    private fun showCameraWithPausedClock(fake: FakeCameraController): CameraViewModel {
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = CameraViewModel(controller = fake, probe = ViewModelClearedProbe())
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                CameraScreen(viewModel = viewModel, onBack = {}, onKept = {})
            }
        }
        repeat(100) {
            if (compose.onAllNodes(enabledShutter).fetchSemanticsNodes().isNotEmpty()) return viewModel
            compose.mainClock.advanceTimeBy(100)
            shadowOf(Looper.getMainLooper()).idle()
        }
        error("The shutter never became usable: the fake camera did not connect")
    }

    /**
     * Fails unless [node], called [name] in the messages, is an icon button as DoD 8 describes
     * it: it can be tapped, its content description is exactly [description], it shows no text,
     * and it is at least [MIN_TOUCH] wide and tall.
     *
     * The node is the one tests and screen readers see for the whole button: it gathers the
     * meaning of everything inside the button, so a visible text anywhere in the button would
     * appear on it as its Text property.
     */
    private fun assertIconButton(name: String, node: SemanticsNodeInteraction, description: String) {
        node.assertHasClickAction()
        node.assertContentDescriptionEquals(description)
        node.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val minimumPx = with(compose.density) { MIN_TOUCH.toPx() }
        // Half a pixel of slack for rounding between dp and pixels.
        assertTrue(
            "$name is ${bounds.width} x ${bounds.height} px, smaller than $MIN_TOUCH ($minimumPx px)",
            bounds.width >= minimumPx - 0.5f && bounds.height >= minimumPx - 0.5f,
        )
    }

    /**
     * Moves the paused test clock forward by [millis], one frame after another, and Robolectric's
     * own clock by as much, running whatever the main thread was asked to do meanwhile. Compose's
     * animations follow the first clock; Android's own drawables (the press ripple) the second.
     */
    private fun advanceBothClocks(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
    }

    /**
     * Draws the activity's content into a fresh bitmap and returns the pixels inside [bounds] (a
     * node's bounds in the Compose root, in pixels), row by row. The Compose root fills the
     * content view from its top-left corner, so the bounds are a place in the bitmap.
     */
    private fun pixelsInside(bounds: Rect): IntArray {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            // Keep the bounds inside the bitmap, so getPixels never reads outside it.
            val left = bounds.left.toInt().coerceIn(0, bitmap.width - 1)
            val top = bounds.top.toInt().coerceIn(0, bitmap.height - 1)
            val right = bounds.right.toInt().coerceIn(left + 1, bitmap.width)
            val bottom = bounds.bottom.toInt().coerceIn(top + 1, bitmap.height)
            val width = right - left
            val height = bottom - top
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, left, top, width, height)
            return pixels
        } finally {
            // A bitmap holds native memory; free it now rather than waiting for the collector.
            bitmap.recycle()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Values the tests share. */
    private companion object {
        /** Matches the shutter button while it can be pressed. */
        val enabledShutter: SemanticsMatcher = hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()

        /** A camera that is open and ready, with a flash and a second lens. */
        val readyState = CameraUiState(
            camera = CameraState(
                status = CameraStatus.Ready,
                canSwitchLens = true,
                flashMode = FlashMode.Auto,
                hasFlash = true,
            ),
        )

        /** Android's smallest recommended touch target. */
        val MIN_TOUCH = 48.dp

        /**
         * The height of the bottom gesture zone the test sends: 32 dp, the size Android uses for
         * gesture navigation, taller than the bottom bar's own 16 dp margin.
         */
        val GESTURE_ZONE = 32.dp

        /** How long the screen is left to settle before the first picture, in milliseconds. */
        const val SETTLE_MS = 2_000L

        /** How long after the touch-down the pressed shutter is drawn, in milliseconds. */
        const val PRESS_MS = 150L

        /** How long after the release the shutter is drawn again, in milliseconds. */
        const val AFTER_RELEASE_MS = 1_000L

        /**
         * How far the paused clock is moved on after each change of the camera's state in the
         * loading indicator test, in milliseconds: a few frames, enough to draw the change.
         */
        const val INDICATOR_MS = 500L
    }
}
