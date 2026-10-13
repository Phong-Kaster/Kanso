package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.GalleryGridTest
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import com.phongkaster.kanso.ui.home.HOME_OPEN_PHOTO_TAG
import com.phongkaster.kanso.ui.home.homeToolTag
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.editorToolTag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * DoD 3 and 5, through the app's real navigation: tapping a photo in a tool's gallery opens that
 * tool's screen with that photo's URI on its image, and a fast double tap opens it only once.
 * Each of Home's five tool cards, and Home's "Open a photo", leads through the gallery to the
 * editor with the right tool selected.
 *
 * The app is started as a person would (Home, then a tool card or "Open a photo"). Only the photo
 * library and the photo decoder are swapped for fakes, through Koin, before the gallery is opened.
 * Home's cards are found by their tag (`homeToolTag`), because the "Edit" header and the Edit card
 * read the same word.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PhotoToToolNavigationTest {

    /** Finds Compose nodes in the activity the test starts. */
    @get:org.junit.Rule
    val compose = createEmptyComposeRule()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin: its global state outlives the test inside the same JVM. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Tapping photo k opens Edit's tool screen on photo k; the back arrow returns to the gallery. */
    @Test
    fun tapping_a_photo_opens_the_tool_screen_on_that_photo() {
        val source = GalleryGridTest.threeAlbumSource()
        val photo = source.allPhotos.sortedByDescending { it.modifiedSeconds }[3]
        openGalleryWith(source) {
            compose.onNodeWithTag(galleryPhotoTag(photo.id)).performClick()
            waitForToolScreen()

            compose.onNodeWithTag(TOOL_SCREEN_TAG).assertExists()
            compose.onNode(
                hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, photo.uri),
            ).assertExists()

            Espresso.pressBack()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(GALLERY_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithTag(TOOL_SCREEN_TAG).assertCountEquals(0)
        }
    }

    /** A double tap on a photo opens one tool screen, so one Back returns to the gallery. */
    @Test
    fun a_double_tap_opens_the_tool_screen_once() {
        val source = GalleryGridTest.threeAlbumSource()
        val photo = source.allPhotos.first()
        openGalleryWith(source) {
            compose.onNodeWithTag(galleryPhotoTag(photo.id)).performClick()
            // The second tap may land after the gallery is already gone; it must not matter.
            runCatching { compose.onNodeWithTag(galleryPhotoTag(photo.id)).performClick() }
            waitForToolScreen()
            Espresso.pressBack()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(GALLERY_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(0, compose.onAllNodesWithTag(TOOL_SCREEN_TAG).fetchSemanticsNodes().size)
        }
    }

    /**
     * DoD 5: "Open a photo" opens the gallery titled "Edit: choose a photo"; photo 1 there opens
     * the editor on that photo with Edit on and the other four tools off. The back stack is
     * [Home, Gallery(EDIT), ToolPhoto(EDIT, photo 1)]: a test cannot read it, so it is read from
     * where Back leads, the Edit gallery first and Home after it.
     */
    @Test
    fun open_a_photo_opens_the_edit_gallery_then_the_editor_on_edit() {
        val photo = PHOTOS.first()
        launchWithFakes { scenario ->
            compose.onNodeWithTag(HOME_OPEN_PHOTO_TAG).tapAboveBar()
            walkGalleryToEditor(Tool.EDIT, photo.id, photo.uri, scenario)
        }
    }

    /**
     * DoD 5: each of the five tool cards opens the gallery titled "<tool>: choose a photo"; photo 1
     * there opens the editor on that photo with that tool on and the other four off. Back leads to
     * the same gallery, then to Home, so the back stack was [Home, Gallery(tool), ToolPhoto(tool,
     * photo 1)] for every card.
     */
    @Test
    fun each_tool_card_opens_its_gallery_then_the_editor_with_that_tool_selected() {
        val photo = PHOTOS.first()
        launchWithFakes { scenario ->
            Tool.editingTools.forEach { tool ->
                compose.onNodeWithTag(homeToolTag(tool)).tapAboveBar()
                walkGalleryToEditor(tool, photo.id, photo.uri, scenario)
            }
        }
    }

    /**
     * Starting right after a tap on Home that opens [tool]'s gallery: waits for the gallery titled
     * "<tool>: choose a photo", taps the photo [photoId], checks that the editor shows the photo
     * [photoUri] with [tool] on and the other editing tools off, then presses Back twice and
     * checks that the first Back shows that same gallery and the second one Home.
     */
    private fun walkGalleryToEditor(
        tool: Tool,
        photoId: Long,
        photoUri: String,
        scenario: ActivityScenario<MainActivity>,
    ) {
        val title = app.getString(R.string.gallery_title, app.getString(tool.labelRes))
        waitFor { titleShown(title) && count(galleryPhotoTag(photoId)) > 0 }
        compose.onNodeWithTag(galleryPhotoTag(photoId)).performClick()

        // The photo marker: the image node names the Uri of the photo it shows.
        val shownPhoto = hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, photoUri)
        waitFor { count(GALLERY_SCREEN_TAG) == 0 && compose.onAllNodes(shownPhoto).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, count(TOOL_SCREEN_TAG))
        Tool.editingTools.forEach { other ->
            val toggle = compose.onNodeWithTag(editorToolTag(other))
            if (other == tool) toggle.assertIsOn() else toggle.assertIsOff()
        }

        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { count(TOOL_SCREEN_TAG) == 0 && titleShown(title) }
        assertEquals(1, count(GALLERY_SCREEN_TAG))

        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { count(GALLERY_SCREEN_TAG) == 0 }
        assertEquals(0, count(TOOL_SCREEN_TAG))
        compose.onNodeWithTag(HOME_OPEN_PHOTO_TAG).assertExists()
        scenario.onActivity { assertFalse(it.isFinishing) }
    }

    /**
     * Starts the app with photo access granted, swaps in a library of three photos in one album
     * ([PHOTOS], photo 1 the newest, so the first cell) and the fake decoder, waits for Home, and
     * runs [body] with the scenario.
     */
    private fun launchWithFakes(body: (ActivityScenario<MainActivity>) -> Unit) {
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        val source = FakePhotoSource(albums = listOf(Album(1L, "A")), allPhotos = PHOTOS)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Koin lets a later module replace an earlier recipe with the same type.
            loadKoinModules(
                module {
                    single<PhotoSource> { source }
                    single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                },
            )
            waitFor { count(HOME_OPEN_PHOTO_TAG) > 0 }
            body(scenario)
        }
    }

    /** Starts the app, swaps in the fakes, grants photo access and opens Edit's gallery, then runs [body]. */
    private fun openGalleryWith(source: FakePhotoSource, body: () -> Unit) {
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            // Koin lets a later module replace an earlier recipe with the same type.
            loadKoinModules(
                module {
                    single<PhotoSource> { source }
                    single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                },
            )
            // The Edit card, by its tag: the "Edit" header above it reads the same word.
            val editCard = homeToolTag(Tool.EDIT)
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(editCard).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(editCard).tapAboveBar()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(galleryPhotoTag(source.allPhotos.first().id)).fetchSemanticsNodes().isNotEmpty()
            }
            body()
        }
    }

    /** Waits until the tool screen is on screen. */
    private fun waitForToolScreen() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(TOOL_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Tells whether the gallery titled [title] is on screen. */
    private fun titleShown(title: String): Boolean =
        compose.onAllNodes(hasText(title) and hasAnyAncestor(hasTestTag(GALLERY_SCREEN_TAG)))
            .fetchSemanticsNodes().isNotEmpty()

    /**
     * Waits up to ten seconds of real time for [condition], letting the main thread run in
     * between: the gallery's read and the editor's decode post their results back to the main
     * thread, and under Robolectric the test runs on that thread itself.
     */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Values the walks share. */
    private companion object {
        /** Three photos in one album, newest first: photo 1 is the first cell of the gallery. */
        val PHOTOS = GalleryGridTest.manyPhotos(3)
    }
}
