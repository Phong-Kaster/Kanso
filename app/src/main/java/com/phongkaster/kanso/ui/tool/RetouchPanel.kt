package com.phongkaster.kanso.ui.tool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/** Test tag of the Retouch panel's strength slider. */
const val RETOUCH_STRENGTH_TAG = "retouch-strength"

/** Test tag of the Retouch panel's button that smooths the skin. */
const val RETOUCH_RUN_TAG = "retouch-run"

/**
 * The Retouch tool's controls, shown in the options strip under the photo: a "Smoothing" slider
 * from 0 (none) to 1 (full, the default) with its value as a percentage, and a "Smooth skin"
 * button that calls [onApply] with the slider's strength. The caller runs the Retouch processor
 * through the tool screen's ViewModel, which shows the progress on the photo card.
 *
 * The slider's position is the panel's own state, kept with `rememberSaveable`, so it survives a
 * rotation (the screen is rebuilt then) as well as ordinary redraws. Everything else is stateless:
 * [enabled] false (while an edit runs, for example) switches both controls off.
 */
@Composable
fun RetouchPanel(
    onApply: (strength: Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // `by` reads and writes the state's value directly; a change redraws the panel.
    var strength by rememberSaveable { mutableFloatStateOf(DEFAULT_STRENGTH) }
    val label = stringResource(R.string.retouch_strength_label)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(RETOUCH_PANEL_TAG)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.retouch_strength_value, (strength * PERCENT).roundToInt()),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Slider(
            value = strength,
            onValueChange = { strength = it.coerceIn(0f, 1f) },
            enabled = enabled,
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                // 48 dp is the smallest touch target Android's accessibility guidance allows.
                .heightIn(min = 48.dp)
                .testTag(RETOUCH_STRENGTH_TAG)
                // A screen reader names the slider by its label, then reads its position.
                .semantics { contentDescription = label },
        )
        Button(
            onClick = { onApply(strength) },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(RETOUCH_RUN_TAG),
        ) {
            Text(stringResource(R.string.retouch_run))
        }
    }
}

/** The slider's starting strength: full smoothing. */
private const val DEFAULT_STRENGTH = 1f

/** Turns a 0..1 strength into a 0..100 percentage. */
private const val PERCENT = 100

/**
 * The Retouch panel at its default strength, drawn once in light and once in dark. The outer Box
 * fills the preview first, because KansoTheme would otherwise stretch its single child to the
 * whole screen.
 */
@PreviewLightDark
@Composable
private fun RetouchPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                RetouchPanel(onApply = {})
            }
        }
    }
}

/** The Retouch panel switched off, as while an edit runs, in light and in dark. */
@PreviewLightDark
@Composable
private fun RetouchPanelDisabledPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                RetouchPanel(onApply = {}, enabled = false)
            }
        }
    }
}
