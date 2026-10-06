package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.GalleryGridTest
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * DoD 3, through the app's real navigation: tapping a photo in a tool's gallery opens that
 * tool's screen with that photo's URI on its image, and a fast double tap opens it only once.
 *
 * The app is started as a person would (Home, then the Edit tile). Only the photo library and
 * the photo decoder are swapped for fakes, through Koin, before the gallery is opened.
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
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("Edit").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Edit").performScrollTo().performClick()
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
}
