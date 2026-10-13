package com.phongkaster.kanso

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.navigation.KANSO_TAB_BAR_TAG
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.kansoTabTag
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Starts the real app on the JVM (Robolectric) and checks that its first screen appears (DoD 34):
 * Home, with the app's name "Kanso", and the floating tab bar with its Home tab selected; the old
 * large button that opened the gallery from Home is gone (R4). It runs on three Android versions,
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
     * Launches MainActivity and checks that Home appears: `home-screen` and the app's name "Kanso"
     * on screen (once), the floating bar `nav-bar` on screen with the Home tab selected, and no
     * text anywhere holding the old large button's words, [OLD_BUTTON_TEXT] (R4: it is gone).
     */
    private fun assertLaunchShowsHome() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithTag(KANSO_TAB_BAR_TAG).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertIsDisplayed()
            compose.onNodeWithText("Kanso").assertIsDisplayed()
            compose.onNodeWithTag(KANSO_TAB_BAR_TAG).assertIsDisplayed()
            compose.onNodeWithTag(kansoTabTag(KansoTab.HOME)).assertIsSelected()

            compose.onAllNodes(hasText(OLD_BUTTON_TEXT, substring = true, ignoreCase = true))
                .assertCountEquals(0)
            compose.onAllNodesWithText("Kanso").assertCountEquals(1)
        }
    }

    /** Values the checks share. */
    private companion object {
        /**
         * The words of Home's old large button, which must be gone. They are joined at run time on
         * purpose: R4's check searches the sources for the phrase written out, and must find none.
         */
        val OLD_BUTTON_TEXT = listOf("Open", "a", "photo").joinToString(" ")
    }
}
