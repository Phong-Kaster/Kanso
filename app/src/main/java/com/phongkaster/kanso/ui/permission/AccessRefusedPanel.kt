package com.phongkaster.kanso.ui.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/**
 * Builds the intent that opens this app's page in the system settings ("App info"), where the
 * person can turn a refused permission on. The `package:` URI tells the settings app which app's
 * page to show.
 */
fun appDetailsSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

/**
 * Opens this app's page in the system settings. Once Android has stopped showing its permission
 * dialog (after refusals), that page is the only place left to grant access, so every refused
 * state offers a button that calls this.
 */
fun Context.openAppDetailsSettings() {
    startActivity(appDetailsSettingsIntent(packageName))
}

/**
 * The plain explanation shown when a permission was refused: a [title], a [message] saying why
 * the app needs the access, and a button labelled [buttonLabel] that calls [onOpenSettings].
 *
 * The text sits in a vertically scrollable column, so a long message (or a large font size)
 * never pushes the button off screen. Colours come from the theme: onSurface text on the screen's
 * surface, and the standard filled button. [buttonModifier] lets the caller tag the button.
 */
@Composable
fun AccessRefusedPanel(
    title: String,
    message: String,
    buttonLabel: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
) {
    // BoxWithConstraints tells its content how much room there is (maxHeight). The column is at
    // least that tall, so short text sits in the middle, and it scrolls once the text is longer.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Inside an unbounded parent (another scrolling list) there is no height to fill.
        val minHeight = if (constraints.hasBoundedHeight) maxHeight else 0.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onOpenSettings, modifier = buttonModifier) {
                Text(buttonLabel)
            }
        }
    }
}

/**
 * The refused panel as the gallery shows it, drawn once in light and once in dark.
 *
 * The panel already takes plain values (text and a callback), so unlike a screen it needs no
 * split: the preview calls it directly with the gallery's real wording. `ThemeMode.System` makes
 * KansoTheme follow the preview's light or dark mode, and the Surface gives it the screen's
 * background. The button does nothing here, because a preview cannot open the system settings.
 */
@PreviewLightDark
@Composable
private fun AccessRefusedPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AccessRefusedPanel(
                title = stringResource(R.string.gallery_refused_title),
                message = stringResource(R.string.gallery_refused_message),
                buttonLabel = stringResource(R.string.gallery_open_settings),
                onOpenSettings = {},
            )
        }
    }
}
