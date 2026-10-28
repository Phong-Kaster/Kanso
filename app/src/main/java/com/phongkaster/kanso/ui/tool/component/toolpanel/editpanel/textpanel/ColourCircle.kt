package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The size of one colour circle. */
private val SWATCH_SIZE = 32.dp

/** The touch target around one circle: Material's comfortable minimum. */
private val SWATCH_TARGET = 48.dp

/** The hues of the custom circle's rainbow, round the colour wheel and back to red. */
internal val RAINBOW = listOf(
    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000),
)

/**
 * One colour circle of the Colour and Background rows ([TextColourRow], [TextBackgroundRow]):
 * [fill] inside a thin outline (so white shows on a light sheet and black on a dark one), with a
 * ring in the theme's primary colour around it while it is [chosen]. A [slashed] circle has no
 * fill and a line across it, the usual sign for "none". Each is a 48 dp target, a radio button
 * named [description] for screen readers.
 */
@Composable
internal fun ColourCircle(
    fill: Brush,
    chosen: Boolean,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    slashed: Boolean = false,
) {
    val ring = if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent
    val outline = MaterialTheme.colorScheme.outlineVariant
    val slash = MaterialTheme.colorScheme.onSurfaceVariant
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
                            .border(width = 1.dp, color = outline, shape = CircleShape)
                            .then(
                                if (slashed) {
                                    // A line from the top-right to the bottom-left, across the circle.
                                    Modifier.drawBehind {
                                        drawLine(
                                            color = slash,
                                            start = Offset(size.width * 0.85f, size.height * 0.15f),
                                            end = Offset(size.width * 0.15f, size.height * 0.85f),
                                            strokeWidth = 2.dp.toPx(),
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                },
            )
        },
    )
}

/** A chosen red circle, a plain white one, the slashed "none" and the rainbow, in light and in dark. */
@PreviewLightDark
@Composable
private fun ColourCirclePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    content = {
                        ColourCircle(fill = Brush.linearGradient(listOf(Color.Red, Color.Red)), chosen = true, enabled = true, description = "Red", onClick = {})
                        ColourCircle(fill = Brush.linearGradient(listOf(Color.White, Color.White)), chosen = false, enabled = true, description = "White", onClick = {})
                        ColourCircle(fill = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)), chosen = false, enabled = true, description = "None", onClick = {}, slashed = true)
                        ColourCircle(fill = Brush.sweepGradient(RAINBOW), chosen = false, enabled = true, description = "Any colour", onClick = {})
                    },
                )
            },
        )
    }
}
