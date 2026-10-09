package com.phongkaster.kanso.settings

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.SETTINGS_SCOPE
import com.phongkaster.kanso.di.appModule
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_DARK_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks that a Dark choice survives a restart: the first activity and the settings DataStore
 * are shut down, Koin is started afresh like a new process, and a new activity draws Home dark.
 *
 * It launches activities itself (ActivityScenario) because it needs two of them in one test.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "notnight")
class ThemeModeRestartTest {

    /** Lets the test find and tap Compose nodes in whatever activity the test launches. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The app context, used to read the same string resources the screens show. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after the test. Koin's state outlives the test's Application inside the same
     * JVM, and starting it twice fails.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Dark is picked, the app is "restarted", and the new activity opens Home dark. */
    @Test
    fun dark_choice_survives_a_restart() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            assertDrawnLight(sampleHome(scenario))
            openSettings()
            compose.onNodeWithTag(THEME_MODE_DARK_TAG).performScrollTo().performClick()
            // The screen only turns dark once the DataStore has written the file and re-read it.
            compose.waitUntil(timeoutMillis = 10_000) { isDark(drawnColor(scenario)) }
        }

        // Release the settings file, then rebuild every singleton as a new process would.
        runBlocking {
            KoinPlatform.getKoin().get<CoroutineScope>(SETTINGS_SCOPE)
                .coroutineContext[Job]!!
                .cancelAndJoin()
        }
        stopKoin()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(appModule)
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            assertDrawnDark(sampleHome(scenario))

            openSettings()
            compose.onNodeWithTag(THEME_MODE_DARK_TAG).assertIsSelected()
        }
    }

    /**
     * Lets Compose settle, then draws [scenario]'s Home and samples its background and the text
     * of its "Settings" row (its own bounds, from the unmerged tree). Home is taller than this
     * test's screen (Robolectric's default 320 x 470 dp), so the row is first scrolled into view:
     * the text must be drawn on screen to be sampled.
     */
    private fun sampleHome(scenario: ActivityScenario<MainActivity>): ScreenSample {
        compose.onNodeWithText(context.getString(R.string.settings)).performScrollTo()
        compose.waitForIdle()
        val bounds = compose
            .onNodeWithText(context.getString(R.string.settings), useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        var sample: ScreenSample? = null
        scenario.onActivity { sample = it.sampleScreen(bounds) }
        return sample!!
    }

    /** Returns the drawn colour near the bottom-left corner of [scenario]'s activity. */
    private fun drawnColor(scenario: ActivityScenario<MainActivity>): Color {
        var color = Color.Unspecified
        scenario.onActivity { color = it.drawnColorNearBottomLeft() }
        return color
    }

    /** Taps the Settings row on Home and waits until the Settings screen is shown. */
    private fun openSettings() {
        compose.onNodeWithText(context.getString(R.string.settings))
            .performScrollTo()
            .performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home's title is on screen. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
