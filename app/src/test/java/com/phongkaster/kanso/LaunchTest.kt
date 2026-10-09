package com.phongkaster.kanso

import android.content.Context
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.ui.home.HOME_OPEN_PHOTO_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Starts the real app on the JVM (Robolectric) and checks that its first screen appears: Home,
 * with the app's name and the "Open a photo" button (DoD 24). It runs on three Android versions,
 * one test each, so every Robolectric run counts as one test: Android 15 (API 35), Android 11
 * (API 30) and Android 8.1 (API 27, the oldest version the app supports).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LaunchTest {

    /** Lets the test find and check Compose nodes in whatever activity the test launches. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /**
     * Stops Koin after each test. KansoApplication starts Koin for every test's fresh
     * Application, but Koin's global state outlives it inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Launching MainActivity from a fresh start on Android 15 shows Home without crashing. */
    @Test
    fun launching_the_app_shows_its_first_screen() {
        assertLaunchShowsHome()
    }

    /** Launching MainActivity from a fresh start on Android 11 shows Home without crashing. */
    @Test
    @Config(sdk = [30])
    fun launching_the_app_on_android_11_shows_home() {
        assertLaunchShowsHome()
    }

    /** Launching MainActivity from a fresh start on Android 8.1 shows Home without crashing. */
    @Test
    @Config(sdk = [27])
    fun launching_the_app_on_android_8_1_shows_home() {
        assertLaunchShowsHome()
    }

    /**
     * Launches MainActivity and checks that Home appears: the app's name "Kanso" on screen, and
     * the "Open a photo" button on screen with its words.
     */
    private fun assertLaunchShowsHome() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Kanso").assertIsDisplayed()
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertIsDisplayed()

            val openPhoto = ApplicationProvider.getApplicationContext<Context>().getString(R.string.home_open_photo)
            compose.onNodeWithTag(HOME_OPEN_PHOTO_TAG)
                .assertIsDisplayed()
                .assert(hasText(openPhoto))
        }
    }
}
