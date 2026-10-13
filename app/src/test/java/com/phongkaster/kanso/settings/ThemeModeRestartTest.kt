package com.phongkaster.kanso.settings

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.SETTINGS_SCOPE
import com.phongkaster.kanso.di.appModule
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.openTab
import com.phongkaster.kanso.navigation.tapAboveBar
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
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
 * Settings is opened with the floating tab bar's Settings tab (`TabTaps.openTab`), and the Dark row
 * is tapped with `TabTaps.tapAboveBar`, so a row resting under the bar is still reached.
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
            compose.onNodeWithTag(THEME_MODE_DARK_TAG).tapAboveBar()
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
     * of its title, "Kanso" (its own bounds, from the unmerged tree): plain text on the plain
     * background, unlike the Settings tab's label, which sits on the bar's inverse colour. The
     * title is first scrolled into view: the text must be drawn on screen to be sampled.
     */
    private fun sampleHome(scenario: ActivityScenario<MainActivity>): ScreenSample {
        val title = hasText(context.getString(R.string.app_name)) and hasAnyAncestor(hasTestTag(HOME_SCREEN_TAG))
        compose.onNode(title).performScrollTo()
        compose.waitForIdle()
        val bounds = compose
            .onNode(title, useUnmergedTree = true)
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

    /** Taps the Settings tab on the floating bar and waits until the Settings screen is shown. */
    private fun openSettings() {
        openTab(compose, KansoTab.SETTINGS)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home is on screen, found by its own tag. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
