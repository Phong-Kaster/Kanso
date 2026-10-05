package com.phongkaster.kanso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

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
                KansoPlaceholder()
            }
        }
    }
}

/**
 * A temporary screen that shows the app's name in the middle of the window.
 *
 * It stands in for the real Home screen until navigation is added.
 */
@Composable
private fun KansoPlaceholder() {
    // Surface paints the theme's background colour and makes text on it use the matching
    // "on background" colour, so the text stays readable in light and dark mode.
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "Kanso", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
