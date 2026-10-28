package com.phongkaster.kanso.ui.tool.component.toolpanel.enhancepanel.enhancecompare

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the control the person holds down to see the original photo. */
const val ENHANCE_COMPARE_HOLD_TAG = "enhance-compare-hold"

/**
 * The hold-to-compare control, a pill at least 48 dp tall. [onHeldChange] gets true the moment a
 * finger touches it and false when the finger lifts (or slides off and is cancelled).
 *
 * `pointerInput` reads raw touches; `detectTapGestures(onPress = …)` calls its block on each
 * press, and `tryAwaitRelease()` suspends until that finger lets go. For a screen-reader user,
 * who cannot hold, a double tap (the semantics `onClick`) flips between the two photos instead.
 * [held] colours the pill while the original is shown.
 */
@Composable
internal fun HoldToCompare(held: Boolean, onHeldChange: (Boolean) -> Unit) {
    val label = stringResource(R.string.enhance_hold)
    // pointerInput(Unit) starts once and keeps running, so it reads the newest callback through
    // this State instead of the one it captured at the start.
    val currentOnHeldChange by rememberUpdatedState(onHeldChange)
    val currentHeld by rememberUpdatedState(held)
    val container = if (held) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = if (held) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(container)
            .testTag(ENHANCE_COMPARE_HOLD_TAG)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = label) {
                    currentOnHeldChange(!currentHeld)
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        currentOnHeldChange(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            currentOnHeldChange(false)
                        }
                    },
                )
            },
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

/** The hold-to-compare button, at rest and held, in light and in dark. */
@PreviewLightDark
@Composable
private fun HoldToComparePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Row {
            HoldToCompare(held = false, onHeldChange = {})
            HoldToCompare(held = true, onHeldChange = {})
        }
    }
}
