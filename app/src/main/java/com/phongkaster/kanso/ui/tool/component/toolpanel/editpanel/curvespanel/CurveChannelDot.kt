package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.CurveChannel
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/**
 * The small coloured dot beside a channel's label: the channel's own tint for Red, Green and Blue,
 * and for the master RGB curve a sweep through the three tints, so it reads as "all of them".
 *
 * The tints are the same ones the RGB sheet uses (through [curveLineColorFor]), chosen for the
 * graph's own background so they stay visible in light and dark. The dot is decoration: the entry that holds it
 * carries the spoken name.
 */
@Composable
internal fun CurveChannelDot(channel: CurveChannel, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val red = curveLineColorFor(scheme, CurveChannel.RED)
    val green = curveLineColorFor(scheme, CurveChannel.GREEN)
    val blue = curveLineColorFor(scheme, CurveChannel.BLUE)
    val brush = when (channel) {
        // A sweep goes round the circle; repeating the first colour at the end closes the loop.
        CurveChannel.RGB -> Brush.sweepGradient(listOf(red, green, blue, red))
        CurveChannel.RED -> Brush.linearGradient(listOf(red, red))
        CurveChannel.GREEN -> Brush.linearGradient(listOf(green, green))
        CurveChannel.BLUE -> Brush.linearGradient(listOf(blue, blue))
    }
    Box(modifier = modifier.size(12.dp).clip(CircleShape).background(brush))
}

/** All four dots side by side, in light and in dark. */
@PreviewLightDark
@Composable
private fun CurveChannelDotPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CurveChannel.entries.forEach { CurveChannelDot(it) }
            }
        }
    }
}
