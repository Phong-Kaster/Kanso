package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
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
import com.phongkaster.kanso.ui.home.HOME_CAROUSEL_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.homeBannerTag
import com.phongkaster.kanso.ui.home.homeCarouselDotTag
import com.phongkaster.kanso.ui.home.homeTryTag
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
 * Each of Home's five shortcuts, and each of the four AI tools' banner and its "Try now" button,
 * leads through that tool's gallery, with no screen in between, to that tool's own screen (Run 7:
 * one screen per tool, named by its tool alone).
 *
 * The app is started as a person would (Home, then a shortcut, a banner or a "Try now"). Only the
 * photo library and the photo decoder are swapped for fakes, through Koin, before the app starts.
 * Shortcuts are reached through `openToolGallery` (HomeTaps.kt); a banner is brought to the front
 * by swiping the carousel, which composes only the banners in view. Home and everything on it are
 * found by their tags, never by their text.
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
     * DoD 5 and 10: each of the five shortcuts on Home (`home-shortcut-<TOOL>`) opens the gallery
     * titled "<tool>: choose a photo"; photo 1 there opens that tool's own screen on that photo,
     * named by that tool alone. Back leads to the same gallery, then to Home, so the back stack
     * was [Home, Gallery(tool), ToolPhoto(tool, photo 1)] for every tool: no screen in between. A
     * test cannot read the back stack, so it is read from where Back leads.
     */
    @Test
    fun each_shortcut_on_home_opens_its_gallery_then_its_own_tool_screen() {
        val photo = PHOTOS.first()
        launchWithFakes { scenario ->
            Tool.entries.forEach { tool ->
                openToolGallery(compose, tool)
                walkGalleryToEditor(tool, photo.id, photo.uri, scenario)
            }
        }
    }

    /**
     * The same walk for each of the four AI tools through a tap on its banner in Home's carousel
     * (`home-banner-<TOOL>`), the carousel first swiped until that banner is in front: the tool's
     * gallery, then its own screen on photo 1, then Back to the gallery and to Home.
     */
    @Test
    fun each_banner_opens_its_gallery_then_its_own_tool_screen() {
        val photo = PHOTOS.first()
        launchWithFakes { scenario ->
            AI_TOOLS.forEach { tool ->
                bringBannerToFront(tool)
                compose.onNodeWithTag(homeBannerTag(tool)).performSemanticsAction(SemanticsActions.OnClick)
                walkGalleryToEditor(tool, photo.id, photo.uri, scenario)
            }
        }
    }

    /**
     * The same walk for each of the four AI tools through its banner's "Try now" button
     * (`home-try-<TOOL>`), the carousel first swiped until that banner is in front.
     */
    @Test
    fun each_try_now_opens_its_gallery_then_its_own_tool_screen() {
        val photo = PHOTOS.first()
        launchWithFakes { scenario ->
            AI_TOOLS.forEach { tool ->
                bringBannerToFront(tool)
                compose.onNodeWithTag(homeTryTag(tool)).performSemanticsAction(SemanticsActions.OnClick)
                walkGalleryToEditor(tool, photo.id, photo.uri, scenario)
            }
        }
    }

    /**
     * Swipes Home's carousel, as a finger would, until [tool]'s banner is in front: its "Try now"
     * wholly inside the carousel. The carousel composes only the banners in view, so a banner
     * further along does not exist until it is swiped to. The selected page dot says which banner
     * is in front, and so which way to swipe; Home keeps the carousel's position when the person
     * comes back from a tool, so the walk may start from any banner. The carousel is first
     * scrolled into view in Home's column.
     */
    private fun bringBannerToFront(tool: Tool) {
        waitForHome(compose)
        compose.onNodeWithTag(HOME_CAROUSEL_TAG).performScrollTo()
        val target = AI_TOOLS.indexOf(tool)
        repeat(AI_TOOLS.size * 2) {
            if (tryNowWhollyInCarousel(tool)) return
            val current = currentBanner()
            compose.onNodeWithTag(HOME_CAROUSEL_TAG).performTouchInput {
                if (current > target) swipeRight() else swipeLeft()
            }
            compose.waitForIdle()
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertTrue("${tool.name}'s banner should be in front after swiping", tryNowWhollyInCarousel(tool))
    }

    /** Returns the index of the banner the page dots mark as in front (0 if none is marked). */
    private fun currentBanner(): Int = AI_TOOLS.indices.firstOrNull { index ->
        compose.onNodeWithTag(homeCarouselDotTag(index)).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Selected) == true
    } ?: 0

    /**
     * Tells whether [tool]'s "Try now" is composed and lies wholly inside the carousel, from its
     * laid-out position and size (not clipped to what is visible).
     */
    private fun tryNowWhollyInCarousel(tool: Tool): Boolean {
        val tryNow = compose.onAllNodesWithTag(homeTryTag(tool)).fetchSemanticsNodes().singleOrNull() ?: return false
        val carousel = compose.onNodeWithTag(HOME_CAROUSEL_TAG).fetchSemanticsNode()
        val left = tryNow.positionInRoot.x
        val right = left + tryNow.size.width
        return left >= carousel.positionInRoot.x - 1f && right <= carousel.positionInRoot.x + carousel.size.width + 1f
    }

    /**
     * Starting right after a tap on Home that opens [tool]'s gallery: waits for the gallery titled
     * "<tool>: choose a photo", taps the photo [photoId], checks that [tool]'s screen shows the
     * photo [photoUri] with [tool]'s name as its only tool name, then presses Back twice and
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
        Tool.entries.forEach { other ->
            compose.onAllNodes(
                hasText(app.getString(other.labelRes)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
            ).assertCountEquals(if (other == tool) 1 else 0)
        }

        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { count(TOOL_SCREEN_TAG) == 0 && titleShown(title) }
        assertEquals(1, count(GALLERY_SCREEN_TAG))

        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { count(GALLERY_SCREEN_TAG) == 0 }
        assertEquals(0, count(TOOL_SCREEN_TAG))
        compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        scenario.onActivity { assertFalse(it.isFinishing) }
    }

    /**
     * Starts the app with photo access granted, a library of three photos in one album
     * ([PHOTOS], photo 1 the newest, so the first cell) and the fake decoder, waits for Home, and
     * runs [body] with the scenario. The fakes are loaded before the app starts: a screen's
     * ViewModel keeps the sources it was built with, so a later swap might never reach it.
     */
    private fun launchWithFakes(body: (ActivityScenario<MainActivity>) -> Unit) {
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        val source = FakePhotoSource(albums = listOf(Album(1L, "A")), allPhotos = PHOTOS)
        // Koin lets a later module replace an earlier recipe with the same type.
        loadKoinModules(
            module {
                single<PhotoSource> { source }
                single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
            },
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome(compose)
            body(scenario)
        }
    }

    /** Grants photo access, loads the fakes, starts the app and opens Edit's gallery, then runs [body]. */
    private fun openGalleryWith(source: FakePhotoSource, body: () -> Unit) {
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        // Koin lets a later module replace an earlier recipe with the same type.
        loadKoinModules(
            module {
                single<PhotoSource> { source }
                single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
            },
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            openToolGallery(compose, Tool.EDIT)
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

        /** The four AI tools, in the order Home's carousel shows their banners. */
        val AI_TOOLS = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC)
    }
}
