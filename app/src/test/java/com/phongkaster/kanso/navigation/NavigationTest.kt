package com.phongkaster.kanso.navigation

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Drives the real app (MainActivity, started on the JVM by Robolectric) through its navigation:
 * Home lists the tools, each tool opens its gallery (the camera its stand-in), Back returns to Home, Back on Home
 * closes the app, rotation keeps the current screen, and each screen's ViewModel lives exactly
 * as long as its back stack entry.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NavigationTest {

    /** Lets the test find and check Compose nodes in whatever activity the test launches. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The app context, used to read the same string resources the screens show. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test. KansoApplication starts Koin for every test's fresh
     * Application, but Koin's global state outlives it inside the same JVM, and starting it
     * twice fails.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** DoD 4: Home lists each of the six tools and Settings exactly once, each one tappable. */
    @Test
    fun home_lists_the_six_tools_and_settings_each_clickable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            compose.onAllNodesWithText("Kanso").assertCountEquals(1)

            val labels = Tool.entries.map { label(it) } + context.getString(R.string.settings)
            assertEquals(7, labels.toSet().size)
            labels.forEach { label ->
                compose.onAllNodesWithText(label).assertCountEquals(1)
                compose.onNodeWithText(label).assertHasClickAction()
            }
        }
    }

    /**
     * DoD 1: each of the five photo tools opens the photo gallery, and Back returns to Home each
     * time. The Camera tile opens the camera instead; that route is tested with a fake camera in
     * the camera tests.
     */
    @Test
    fun each_tool_opens_its_screen_and_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            Tool.entries.filter { it != Tool.CAMERA }.forEach { tool ->
                val name = label(tool)
                compose.onNodeWithText(name).performScrollTo().performClick()
                waitForGallery()
                compose.onNodeWithTag(GALLERY_SCREEN_TAG).assertIsDisplayed()
                // The gallery's title names the tool, so a tile opening another tool's
                // gallery would fail here.
                compose.onNode(
                    hasText(name, substring = true) and hasAnyAncestor(hasTestTag(GALLERY_SCREEN_TAG)),
                ).assertIsDisplayed()

                Espresso.pressBack()
                waitForHomeOnly()
                assertEquals(Lifecycle.State.RESUMED, scenario.state)
                scenario.onActivity { activity -> assertFalse(activity.isFinishing) }
            }
        }
    }

    /**
     * DoD 17: launching the app asks for no permission; the first tap on Edit asks for the two
     * photo permissions Android 15 uses.
     */
    @Test
    fun launch_asks_for_nothing_and_the_gallery_asks_for_photo_access() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            scenario.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }

            compose.onNodeWithText(label(Tool.EDIT)).performScrollTo().performClick()
            waitForGallery()

            scenario.onActivity { activity ->
                val request = shadowOf(activity).lastRequestedPermission
                assertEquals(
                    listOf(
                        android.Manifest.permission.READ_MEDIA_IMAGES,
                        android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                    ),
                    request?.requestedPermissions?.toList(),
                )
            }
        }
    }

    /** DoD 6: the Settings row opens Settings, and Back returns to Home. */
    @Test
    fun settings_opens_and_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            compose.onNodeWithText(context.getString(R.string.settings))
                .performScrollTo()
                .performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(
                hasText(context.getString(R.string.settings)) and
                    hasAnyAncestor(hasTestTag(SETTINGS_SCREEN_TAG)),
            ).assertIsDisplayed()

            Espresso.pressBack()
            waitForHomeOnly()
            scenario.onActivity { assertFalse(it.isFinishing) }
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    /** DoD 7: Back on Home closes the app: the activity is destroyed. */
    @Test
    fun back_on_home_closes_the_app() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            var finishing = false
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            // finish() marks the activity as finishing at once; the teardown follows later.
            scenario.onActivity { finishing = it.isFinishing }
            assertTrue("Back on Home should finish the activity", finishing)
        }
    }

    /** DoD 8: rotating (recreating the activity) on the Edit gallery keeps it on screen. */
    @Test
    fun gallery_survives_recreation_and_one_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            val name = label(Tool.EDIT)
            compose.onNodeWithText(name).performScrollTo().performClick()
            waitForGallery()

            scenario.recreate()
            waitForGallery()
            compose.onNodeWithTag(GALLERY_SCREEN_TAG).assertIsDisplayed()

            Espresso.pressBack()
            waitForHomeOnly()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    /**
     * DoD 10: a gallery's ViewModel is cleared when the gallery is popped, while Home's
     * ViewModel, whose entry stays on the back stack, is kept.
     */
    @Test
    fun popping_a_gallery_clears_its_view_model_but_not_homes() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            val probe = KoinPlatform.getKoin().get<ViewModelClearedProbe>()

            val name = label(Tool.EDIT)
            compose.onNodeWithText(name).performScrollTo().performClick()
            waitForGallery()
            assertFalse(probe.wasCleared(GalleryViewModel::class))

            Espresso.pressBack()
            waitForHomeOnly()
            compose.waitUntil(timeoutMillis = 10_000) {
                probe.wasCleared(GalleryViewModel::class)
            }
            assertTrue(probe.wasCleared(GalleryViewModel::class))
            assertFalse(probe.wasCleared(HomeViewModel::class))
        }
    }

    /** Returns the label Home shows for [tool]. */
    private fun label(tool: Tool): String = context.getString(tool.labelRes)

    /** Waits until the gallery screen is on screen. */
    private fun waitForGallery() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(GALLERY_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home's title is on screen. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home is shown and no gallery or Settings screen is left on screen. */
    private fun waitForHomeOnly() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(GALLERY_SCREEN_TAG).fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Kanso").assertIsDisplayed()
    }
}
