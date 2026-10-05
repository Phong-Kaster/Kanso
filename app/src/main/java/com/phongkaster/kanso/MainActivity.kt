package com.phongkaster.kanso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.phongkaster.kanso.navigation.KansoNavHost

/**
 * The app's one and only activity: the window every Kanso screen is drawn in.
 *
 * Kanso is "single activity, all Compose": instead of one activity per screen, this activity hands
 * its whole window to Jetpack Compose, and Compose code decides what to show.
 */
class MainActivity : ComponentActivity() {

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
        // setContent hands the window to Compose: everything inside the braces is a composable,
        // a function that describes the UI and is re-run whenever the state it reads changes.
        setContent {
            // MaterialTheme gives every Material 3 component below it its colours, type and shapes.
            MaterialTheme {
                KansoNavHost()
            }
        }
    }
}
