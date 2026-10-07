package com.phongkaster.kanso

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Starts the real app on the JVM (Robolectric) and checks that its first screen appears.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LaunchTest {

    /** Lets the test find and check Compose nodes in whatever activity the test launches. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** Launching MainActivity from a fresh start shows the app's name without crashing. */
    @Test
    fun launching_the_app_shows_its_first_screen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Kanso").assertIsDisplayed()
        }
    }
}
