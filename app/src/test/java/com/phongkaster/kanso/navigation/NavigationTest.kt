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
import com.phongkaster.kanso.ui.placeholder.PLACEHOLDER_SCREEN_TAG
import com.phongkaster.kanso.ui.placeholder.PlaceholderViewModel
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Home lists the tools, each tool opens its placeholder, Back returns to Home, Back on Home
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

    /** DoD 5: every tool opens a placeholder naming it, and Back returns to Home. */
    @Test
    fun each_tool_opens_its_placeholder_and_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            Tool.entries.forEach { tool ->
                val name = label(tool)
                compose.onNodeWithText(name).performScrollTo().performClick()
                waitForPlaceholderTitle(name)
                compose.onNode(placeholderTitle(name)).assertIsDisplayed()

                Espresso.pressBack()
                waitForHomeOnly()
                assertEquals(Lifecycle.State.RESUMED, scenario.state)
                scenario.onActivity { activity -> assertFalse(activity.isFinishing) }
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

    /** DoD 8: rotating (recreating the activity) on the Edit placeholder keeps it on screen. */
    @Test
    fun placeholder_survives_recreation_and_one_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            val name = label(Tool.EDIT)
            compose.onNodeWithText(name).performScrollTo().performClick()
            waitForPlaceholderTitle(name)

            scenario.recreate()
            waitForPlaceholderTitle(name)
            compose.onNode(placeholderTitle(name)).assertIsDisplayed()

            Espresso.pressBack()
            waitForHomeOnly()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    /**
     * DoD 10: a placeholder's ViewModel is cleared when the placeholder is popped, while Home's
     * ViewModel, whose entry stays on the back stack, is kept.
     */
    @Test
    fun popping_a_placeholder_clears_its_view_model_but_not_homes() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            val probe = KoinPlatform.getKoin().get<ViewModelClearedProbe>()

            val name = label(Tool.EDIT)
            compose.onNodeWithText(name).performScrollTo().performClick()
            waitForPlaceholderTitle(name)
            assertFalse(probe.wasCleared(PlaceholderViewModel::class))

            Espresso.pressBack()
            waitForHomeOnly()
            compose.waitUntil(timeoutMillis = 10_000) {
                probe.wasCleared(PlaceholderViewModel::class)
            }
            assertTrue(probe.wasCleared(PlaceholderViewModel::class))
            assertFalse(probe.wasCleared(HomeViewModel::class))
        }
    }

    /** Returns the label Home and the placeholder show for [tool]. */
    private fun label(tool: Tool): String = context.getString(tool.labelRes)

    /** Matches a node with exactly the text [name] inside the placeholder screen. */
    private fun placeholderTitle(name: String) =
        hasText(name) and hasAnyAncestor(hasTestTag(PLACEHOLDER_SCREEN_TAG))

    /** Waits until the placeholder screen shows [name]. */
    private fun waitForPlaceholderTitle(name: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(placeholderTitle(name)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home's title is on screen. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home is shown and no placeholder or Settings screen is left on screen. */
    private fun waitForHomeOnly() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(PLACEHOLDER_SCREEN_TAG).fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Kanso").assertIsDisplayed()
    }
}
