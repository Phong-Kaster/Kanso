package com.phongkaster.kanso.ui.tool.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.delay

/** The test tag on the "20 stickers at most" message shown on the photo. */
const val STICKER_LIMIT_MESSAGE_TAG = "sticker-limit-message"

/** How long the limit message stays on the photo before it asks to be hidden, in milliseconds. */
internal const val STICKER_LIMIT_MESSAGE_MILLIS = 2500L

/**
 * A short-lived card on the photo saying a photo holds at most 20 stickers.
 *
 * While [visible] is true it shows the message in the theme's error-container colours, like
 * the photo card's `FailureMessage`, and after [STICKER_LIMIT_MESSAGE_MILLIS] calls [onShown] so the owner of the
 * state can set [visible] back to false. `LaunchedEffect(visible)` is Compose's way to run a
 * suspending block (here, a delay) when a value changes: it starts when [visible] turns true and is
 * cancelled if the card leaves the screen first. Screen readers announce the message when it appears.
 */
@Composable
internal fun StickerLimitMessage(
    visible: Boolean,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The latest callback, so a new lambda passed while waiting does not restart the timer.
    val currentOnShown by rememberUpdatedState(onShown)
    LaunchedEffect(visible) {
        if (visible) {
            delay(STICKER_LIMIT_MESSAGE_MILLIS)
            currentOnShown()
        }
    }
    if (!visible) return
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .padding(16.dp)
            .testTag(STICKER_LIMIT_MESSAGE_TAG)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = stringResource(R.string.stickers_limit_message),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/**
 * The limit message in light and dark. The sample sits in a full-size Box because `KansoTheme`
 * stretches its direct child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun StickerLimitMessagePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            StickerLimitMessage(visible = true, onShown = {})
        }
    }
}
