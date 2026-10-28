package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.model.TextSwatch

/** Test tag of the Text sheet's row of colours. */
const val TEXT_COLOURS_TAG = "text-colours"

/** Test tag of the colour row's last circle, which opens the colour picker. */
const val TEXT_CUSTOM_COLOUR_TAG = "text-colour-custom"

/** Returns the test tag of the circle of [swatch], such as `text-colour-RED`. */
fun textSwatchTag(swatch: TextSwatch): String = "text-colour-${swatch.name}"

/** The size of one colour circle. */
private val SWATCH_SIZE = 32.dp

/** The touch target around one circle: Material's comfortable minimum. */
private val SWATCH_TARGET = 48.dp

/** The hues of the custom circle's rainbow, round the colour wheel and back to red. */
private val RAINBOW = listOf(
    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000),
)

/**
 * The Text sheet's row of colours, scrolling sideways: the twelve ready colours ([TextSwatch]) and
 * a last, rainbow circle for any other colour ([onCustom] opens the picker). The circle of
 * [chosenArgb] gets a ring in the theme's primary colour; the rainbow circle gets it when the
 * colour is none of the twelve. A tap on a ready colour calls [onChoose] with it. Each circle is
 * a 48 dp target with its colour's name for screen readers.
 */
@Composable
internal fun TextColourRow(
    chosenArgb: Int?,
    enabled: Boolean,
    onChoose: (Int) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val custom = chosenArgb != null && TextSwatch.entries.none { it.argb == chosenArgb }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(TEXT_COLOURS_TAG),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            TextSwatch.entries.forEach(
                action = { swatch ->
                    ColourCircle(
                        fill = Brush.linearGradient(listOf(Color(swatch.argb), Color(swatch.argb))),
                        chosen = swatch.argb == chosenArgb,
                        enabled = enabled,
                        description = stringResource(swatch.label),
                        onClick = { onChoose(swatch.argb) },
                        modifier = Modifier.testTag(textSwatchTag(swatch)),
                    )
                },
            )
            ColourCircle(
                fill = Brush.sweepGradient(RAINBOW),
                chosen = custom,
                enabled = enabled,
                description = stringResource(R.string.text_colour_custom),
                onClick = onCustom,
                modifier = Modifier.testTag(TEXT_CUSTOM_COLOUR_TAG),
            )
        },
    )
}

/**
 * One colour circle of the row, [fill] inside a thin outline (so white shows on a light sheet and
 * black on a dark one), with a ring around it while it is [chosen].
 */
@Composable
private fun ColourCircle(
    fill: Brush,
    chosen: Boolean,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ring = if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = modifier
            .size(SWATCH_TARGET)
            .selectable(selected = chosen, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
        content = {
            Box(
                modifier = Modifier
                    .size(SWATCH_SIZE + 8.dp)
                    .border(width = 2.dp, color = ring, shape = CircleShape),
                contentAlignment = Alignment.Center,
                content = {
                    Box(
                        modifier = Modifier
                            .size(SWATCH_SIZE)
                            .clip(CircleShape)
                            .background(fill)
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = CircleShape),
                    )
                },
            )
        },
    )
}

/** The colour row with red chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextColourRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                TextColourRow(chosenArgb = TextSwatch.RED.argb, enabled = true, onChoose = {}, onCustom = {})
            },
        )
    }
}

/** The colour row with a colour none of the twelve has, so the rainbow circle is marked. */
@PreviewLightDark
@Composable
private fun TextColourRowCustomPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                TextColourRow(chosenArgb = 0xFF123456.toInt(), enabled = true, onChoose = {}, onCustom = {})
            },
        )
    }
}
