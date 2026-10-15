package com.phongkaster.kanso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.launcher.LauncherIconSwitcher
import com.phongkaster.kanso.navigation.KansoNavHost
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/**
 * The app's one and only activity: the window every Kanso screen is drawn in.
 *
 * Kanso is "single activity, all Compose": instead of one activity per screen, this activity hands
 * its whole window to Jetpack Compose, and Compose code decides what to show.
 */
class MainActivity : ComponentActivity() {

    /** The saved settings, handed out by Koin the first time they are used. */
    private val settings: SettingsRepository by inject()

    /** Keeps the launcher icon's background in the theme colour, handed out by Koin. */
    private val launcherIcons: LauncherIconSwitcher by inject()

    /**
     * Sets up the window when Android creates the activity: system splash first, then the
     * content drawn edge to edge by Compose.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash screen (the launch theme in themes.xml). It must be installed before
        // super.onCreate, and it switches the window to Theme.Kanso as soon as the app is ready.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars, as Android 15 and later do anyway.
        enableEdgeToEdge()
        // Read the saved theme mode once, before the first frame, so a person who chose Dark never
        // sees one light frame. It is a tiny local file, so blocking for it is brief.
        val savedMode = runBlocking { settings.themeMode.first() }
        // The same for the theme colour, so the first frame already has the chosen colours.
        val savedColour = runBlocking { settings.themeColour.first() }
        // setContent hands the window to Compose: everything inside the braces is a composable,
        // a function that describes the UI and is re-run whenever the state it reads changes.
        setContent {
            // A Flow is a stream of values over time; collectAsState turns it into Compose state,
            // so picking a new mode in Settings re-draws the whole app at once.
            val themeMode by settings.themeMode.collectAsState(initial = savedMode)
            val themeColour by settings.themeColour.collectAsState(initial = savedColour)
            // KansoTheme gives every Material 3 component below it its colours, type and shapes.
            KansoTheme(themeMode, themeColour) {
                KansoNavHost()
            }
        }
        // Each time the theme colour changes (and once at start, which also catches a wallpaper
        // that changed while Kanso was closed), show the launcher icon in the closest colour.
        // lifecycleScope stops collecting when the activity is destroyed.
        lifecycleScope.launch {
            settings.themeColour.collect { colour -> launcherIcons.show(launcherIcons.iconFor(colour)) }
        }
    }
}
