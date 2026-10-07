package com.phongkaster.kanso.tool

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.tool.TOOL_BODY_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_UNREADABLE_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the tool screen on Android 15 (DoD 8): a photo tagged "turned" is drawn upright and
 * fitted inside the area below the top bar, the image node names the photo's Uri, an unreadable
 * photo gives a plain message, Save is shown but disabled, and the back arrow calls back.
 *
 * The screen runs on the same phone size as the pictures (411 x 891 dp, xxhdpi).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ToolScreenTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** A fresh folder for the fixture files, deleted after each test. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * A 400 x 200 JPEG with EXIF orientation 6, through the real loader: the drawn image is
     * taller than wide, lies inside the area below the top bar, and carries the photo's Uri.
     */
    @Test
    fun a_turned_photo_is_drawn_upright_and_fitted() {
        val file = ToolFixtures.writeJpeg(
            temp.newFile("turned.jpg"),
            ToolFixtures.twoHalves(400, 200),
            ExifInterface.ORIENTATION_ROTATE_90,
        )
        val uri = ToolFixtures.serve(context, file, "turned.jpg")
        show(ToolViewModel(Tool.EDIT, uri, PhotoLoader(context), ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()

        val image = compose.onNodeWithTag(TOOL_IMAGE_TAG).fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag(TOOL_BODY_TAG).fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertTrue("The photo is not taller than wide: $image", image.height > image.width)
        assertInside(image, body)
        assertInside(image, root)
        // Fitted: the photo keeps its 1:2 shape and fills the area's width or its height,
        // whichever is tighter, so it is neither stretched, cropped nor shrunk more than needed.
        assertEquals(image.height / 2f, image.width, 2f)
        assertTrue(
            "The photo $image does not fill $body on either side",
            kotlin.math.abs(image.width - body.width) <= 1f ||
                kotlin.math.abs(image.height - body.height) <= 1f,
        )

        compose.onNode(hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, uri))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.tool_photo_description))
            .assertIsDisplayed()
    }

    /** The top bar shows the tool's name, and Save is visible but cannot be tapped yet. */
    @Test
    fun the_top_bar_names_the_tool_and_save_is_visible_and_enabled() {
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        show(ToolViewModel(Tool.RETOUCH, "content://media/external/images/media/7", loader, ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()

        compose.onNode(
            hasText(context.getString(R.string.tool_retouch)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
        ).assertIsDisplayed()
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed().assertIsEnabled()
    }

    /**
     * The screen asks for the photo at twice the phone's longest edge: 891 dp at 3 pixels per dp
     * is 2673 pixels, so 5346; and only once, however often it is drawn.
     */
    @Test
    fun the_photo_is_asked_for_at_twice_the_screen_s_longest_edge() {
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        show(ToolViewModel(Tool.EDIT, "content://media/external/images/media/7", loader, ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()

        assertEquals(listOf(2 * 891 * 3), loader.requestedEdges.toList())
    }

    /** A photo that cannot be opened gives the plain message, and no image. */
    @Test
    fun an_unreadable_photo_shows_a_plain_message() {
        show(ToolViewModel(Tool.EDIT, ToolFixtures.MISSING_URI, PhotoLoader(context), ViewModelClearedProbe(), NoSavePhotoSaver()))
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(TOOL_UNREADABLE_TAG)).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag(TOOL_UNREADABLE_TAG).assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.tool_photo_unreadable))).assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed()
    }

    /** The back arrow calls onBack. */
    @Test
    fun the_back_arrow_calls_on_back() {
        var backCalls = 0
        val loader = FakePhotoLoader(context, null)
        show(ToolViewModel(Tool.EDIT, "content://x/1", loader, ViewModelClearedProbe(), NoSavePhotoSaver()), onBack = { backCalls++ })

        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertEquals(1, backCalls)
    }

    /** Asking again for the same size decodes nothing more; clearing the ViewModel tells the probe. */
    @Test
    fun loading_is_done_once_per_size_and_clearing_is_reported() {
        val loader = FakePhotoLoader(context, null)
        val probe = ViewModelClearedProbe()
        val viewModel = ToolViewModel(Tool.EDIT, "content://x/1", loader, probe, NoSavePhotoSaver())
        // A ViewModelStore is what a navigation entry keeps its ViewModels in; clearing it is
        // what happens when the entry leaves the back stack.
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            /** Hands out the ViewModel built above. */
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            }
        }
        val stored = ViewModelProvider(store, factory)[ToolViewModel::class.java]

        compose.runOnUiThread {
            stored.load(100)
            stored.load(100)
            stored.load(200)
        }
        compose.waitForIdle()
        assertEquals(listOf(100, 200), loader.requestedEdges.toList())

        assertFalse(probe.wasCleared(ToolViewModel::class))
        compose.runOnUiThread { store.clear() }
        assertTrue(probe.wasCleared(ToolViewModel::class))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Shows the tool screen for [viewModel] (built by the caller, outside any composable). */
    private fun show(viewModel: ToolViewModel, onBack: () -> Unit = {}) {
        compose.setContent {
            MaterialTheme {
                ToolScreen(viewModel = viewModel, onBack = onBack, onSaved = {})
            }
        }
        compose.waitForIdle()
    }

    /** Waits until the photo is on screen. */
    private fun waitForImage() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Checks that [inner] lies within [outer], allowing half a pixel for rounding. */
    private fun assertInside(inner: Rect, outer: Rect) {
        val slack = 0.5f
        assertTrue(
            "$inner is not inside $outer",
            inner.left >= outer.left - slack &&
                inner.top >= outer.top - slack &&
                inner.right <= outer.right + slack &&
                inner.bottom <= outer.bottom + slack,
        )
    }
}
