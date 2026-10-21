package com.phongkaster.kanso.ui.tool

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.phongkaster.kanso.ui.common.ClassicSlider
import com.phongkaster.kanso.data.edit.artistic.ArtisticStyle
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Artistic panel's strength slider. */
const val ARTISTIC_STRENGTH_TAG = "artistic-strength"

/** Test tag of the Artistic panel's button that paints the photo in the chosen style. */
const val ARTISTIC_RUN_TAG = "artistic-run"

/** Returns the test tag of [style]'s chip in the Artistic panel, for example `artistic-style-ink`. */
fun artisticStyleTag(style: ArtisticStyle): String = "artistic-style-" + style.id

/**
 * The Artistic tool's controls, shown in the options strip under the photo: a row of style
 * chips, a strength slider and an Apply button.
 *
 * It is stateless: it shows the [selected] style and the [strength] it is given and reports each
 * change ([onSelect], [onStrengthChange]) and the Apply tap ([onApply]); the caller keeps the
 * values. That way a preview or a test can hand it any state. [enabled] turns every control off
 * at once, for example while a style is being painted.
 */
@Composable
fun ArtisticPanel(
    selected: ArtisticStyle,
    strength: Float,
    onSelect: (ArtisticStyle) -> Unit,
    onStrengthChange: (Float) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val strengthLabel = stringResource(R.string.artistic_strength)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ARTISTIC_PANEL_TAG)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The chips scroll sideways when the screen is too narrow to show them all.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtisticStyle.entries.forEach { style ->
                FilterChip(
                    selected = style == selected,
                    onClick = { onSelect(style) },
                    label = { Text(text = stringResource(style.nameRes), maxLines = 1) },
                    enabled = enabled,
                    modifier = Modifier.testTag(artisticStyleTag(style)),
                )
            }
        }
        Text(
            text = strengthLabel,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        ClassicSlider(
            value = strength,
            onValueChange = onStrengthChange,
            valueRange = 0f..1f,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ARTISTIC_STRENGTH_TAG)
                // A screen reader names the slider by this, as the label above it is a separate text.
                .semantics { contentDescription = strengthLabel },
        )
        Button(
            onClick = onApply,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                // 48 dp is the smallest touch target Android's accessibility guidance allows.
                .heightIn(min = 48.dp)
                .testTag(ARTISTIC_RUN_TAG),
        ) {
            Text(stringResource(R.string.artistic_apply))
        }
    }
}

/**
 * The Artistic panel with its own memory of the chosen style and strength: the version the tool
 * screen uses. `rememberSaveable` keeps both through a screen rotation and even when Android
 * stops the app in the background, so the person finds their choice as they left it. Apply
 * calls [onApply] with the style and the strength chosen at that moment.
 */
@Composable
fun ArtisticPanelHost(
    onApply: (ArtisticStyle, Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var selected by rememberSaveable { mutableStateOf(ArtisticStyle.entries.first()) }
    var strength by rememberSaveable { mutableFloatStateOf(1f) }
    ArtisticPanel(
        selected = selected,
        strength = strength,
        onSelect = { selected = it },
        onStrengthChange = { strength = it },
        onApply = { onApply(selected, strength) },
        modifier = modifier,
        enabled = enabled,
    )
}

/**
 * The Artistic panel with the mosaic style chosen at 60 % strength, in light and in dark. The
 * outer Box fills the preview first, because KansoTheme would otherwise stretch its single child
 * to the whole screen.
 */
@PreviewLightDark
@Composable
private fun ArtisticPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                ArtisticPanel(
                    selected = ArtisticStyle.MOSAIC,
                    strength = 0.6f,
                    onSelect = {},
                    onStrengthChange = {},
                    onApply = {},
                )
            }
        }
    }
}

/** The Artistic panel with its controls turned off, as while a style is painted, in light and in dark. */
@PreviewLightDark
@Composable
private fun ArtisticPanelDisabledPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                ArtisticPanel(
                    selected = ArtisticStyle.WARM_WASH,
                    strength = 1f,
                    onSelect = {},
                    onStrengthChange = {},
                    onApply = {},
                    enabled = false,
                )
            }
        }
    }
}

/** The stateful Artistic panel as the tool screen first shows it, in light and in dark. */
@PreviewLightDark
@Composable
private fun ArtisticPanelHostPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                ArtisticPanelHost(onApply = { _, _ -> })
            }
        }
    }
}
