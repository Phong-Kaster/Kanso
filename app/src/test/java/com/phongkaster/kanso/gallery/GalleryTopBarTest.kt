package com.phongkaster.kanso.gallery

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.gallery.GALLERY_BACK_TAG
import com.phongkaster.kanso.ui.gallery.GalleryContent
import com.phongkaster.kanso.ui.gallery.GalleryUiState
import com.phongkaster.kanso.ui.component.permission.model.PermissionAccess
import kotlin.math.abs
import kotlin.math.ceil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the gallery's top bar: the round Back button (round, 48 dp, named "Back", calls Back)
 * and the title "<tool>: choose a photo", which sits in the middle of the window for every tool.
 *
 * It shows the stateless [GalleryContent] with a finished, empty read, so there is no permission
 * dialog, no ViewModel and no loading indicator (which would animate forever and keep the test
 * from going idle). The screen is the size of a typical phone.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class GalleryTopBarTest {

    /** Starts an empty activity to host the gallery. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The tool the gallery on screen is for; a test changes it to show another tool's title. */
    private var tool by mutableStateOf(Tool.EDIT)

    /** How many times Back was tapped. */
    private var backTaps = 0

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Shows the gallery of [tool] with full access and no photos. */
    private fun showGallery() {
        compose.setContent {
            MaterialTheme {
                GalleryContent(
                    state = GalleryUiState(tool = tool, loading = false),
                    access = PermissionAccess.Full,
                    answered = true,
                    onBack = { backTaps++ },
                    onOpenCamera = {},
                    onSelectAlbum = {},
                    onPhotoClick = {},
                    onShareMore = {},
                    onOpenSettings = {},
                )
            }
        }
        compose.waitForIdle()
    }

    /** Back is named "Back", is 48 dp by 48 dp, and a tap calls the Back callback once. */
    @Test
    fun back_is_named_48_dp_and_calls_back() {
        showGallery()

        compose.onNodeWithTag(GALLERY_BACK_TAG)
            .assertContentDescriptionEquals(app.getString(R.string.back))
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .performClick()
        compose.waitForIdle()

        assertEquals(1, backTaps)
    }

    /**
     * Back is round: the pixel 1 px inside its top-left corner shows the bar's background, while
     * a pixel just inside the circle's left edge shows the button's own colour. A square button
     * would show its colour in the corner too.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun back_is_round() {
        showGallery()

        val bounds = compose.onNodeWithTag(GALLERY_BACK_TAG).fetchSemanticsNode().boundsInRoot
        val cornerX = ceil(bounds.left).toInt() + 1
        val cornerY = ceil(bounds.top).toInt() + 1
        val edgeX = ceil(bounds.left).toInt() + EDGE_INSET_PX
        val edgeY = bounds.center.y.toInt()

        val (corner, edge) = drawScreen { bitmap ->
            bitmap.getPixel(cornerX, cornerY) to bitmap.getPixel(edgeX, edgeY)
        }

        val apart = listOf(
            abs(Color.red(corner) - Color.red(edge)),
            abs(Color.green(corner) - Color.green(edge)),
            abs(Color.blue(corner) - Color.blue(edge)),
        ).max()
        assertTrue(
            "The corner of Back should show the background, not the button: " +
                "corner #%08X, edge #%08X".format(corner, edge),
            apart >= MIN_COLOUR_GAP,
        )
    }

    /** The title "<tool>: choose a photo" is centred in the window, within 1 dp, for each tool. */
    @Test
    fun the_title_is_centred_for_every_tool() {
        showGallery()
        val windowCentre = compose.onRoot().fetchSemanticsNode().boundsInRoot.center.x
        val onePx = app.resources.displayMetrics.density

        Tool.entries.forEach { each ->
            tool = each
            compose.waitForIdle()
            val title = app.getString(R.string.gallery_title, app.getString(each.labelRes))

            val centre = compose.onNodeWithText(title).fetchSemanticsNode().boundsInRoot.center.x
            assertTrue(
                "The title of $each should be centred on $windowCentre px, was at $centre px",
                abs(centre - windowCentre) <= onePx,
            )
        }
    }

    /**
     * Draws the activity's content view into a fresh bitmap, hands it to [read], then frees it
     * (see `GalleryGridTest`, which does the same for the photo cells).
     */
    private fun <T> drawScreen(read: (Bitmap) -> T): T {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return try {
            view.draw(Canvas(bitmap))
            read(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /** Values the tests share. */
    private companion object {
        /** How far inside Back's left edge the "button colour" pixel is read, in pixels. */
        const val EDGE_INSET_PX = 4

        /** The least difference on one colour channel that counts as a different colour. */
        const val MIN_COLOUR_GAP = 8
    }
}
