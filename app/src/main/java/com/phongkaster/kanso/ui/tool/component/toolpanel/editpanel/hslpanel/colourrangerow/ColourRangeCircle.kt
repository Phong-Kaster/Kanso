package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.displayColour
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.nameRes
import com.phongkaster.kanso.R

/** The size of the coloured disc of one circle. */
private val DISC_SIZE = 32.dp

/** The size of one circle's cell in the row: exactly the disc, the ring is drawn inside it. */
internal val CELL_SIZE = 32.dp

/**
 * The size of one circle's touch area: Android's recommended 48 dp touch target. It is larger than
 * the 32 dp cell and overflows it evenly on every side without taking more room in the layout.
 */
internal val TOUCH_SIZE = 48.dp

/** How opaque a disabled circle is drawn (Material dims disabled content to 38 percent). */
private const val DISABLED_ALPHA = 0.38f

/** The thickness of the ring round the chosen circle, drawn just inside the disc's edge. */
private val RING_WIDTH = 2.dp

/** The size of the check mark drawn on the chosen circle. */
private val CHECK_SIZE = 18.dp

/** Returns the test tag of [range]'s circle, such as `hsl-range-LIGHT_GREEN`. */
fun hslRangeTag(range: ColourRange): String = "hsl-range-${range.name}"

/**
 * Returns the colours that mark a chosen circle whose disc is [fill], taken only from [scheme]'s
 * roles, as a pair: the ring drawn round the disc, then the check mark drawn on it.
 *
 * Both parts take whichever of `onSurface` and `surface` contrasts more with the disc. The ring is
 * drawn just inside the disc's edge, so it too lies on the disc's colour, not on the sheet: a
 * fixed `onSurface` ring was only about 1.9 to 1 on navy blue in the light theme. In any theme one
 * of those two roles is dark and the other light, so one of them contrasts with every disc by
 * well over the 3 to 1 that WCAG asks of graphics.
 */
internal fun markColours(scheme: ColorScheme, fill: Color): Pair<Color, Color> {
    /**
     * Returns the WCAG contrast ratio of the colours [a] and [b], from 1 (the same) to 21 (black
     * on white): the lighter colour's relative luminance plus 0.05, divided by the darker one's.
     */
    fun contrast(a: Color, b: Color): Float {
        val high = maxOf(a.luminance(), b.luminance())
        val low = minOf(a.luminance(), b.luminance())
        return (high + 0.05f) / (low + 0.05f)
    }
    val check = if (contrast(scheme.onSurface, fill) >= contrast(scheme.surface, fill)) scheme.onSurface else scheme.surface
    return Pair(check, check)
}

/**
 * One circle of the colour-range row: a 32 dp disc in [range]'s colour filling a 32 dp cell. While
 * [chosen] it is marked twice, with a ring round the disc and a check mark on it (see
 * [markColours]), so the mark does not rely on colour alone. The ring is drawn inside the disc so
 * nothing leaves the cell.
 *
 * The cell is 32 dp in the layout, because the sheet has room for three 32 dp rows only, but the
 * part a finger can hit is 48 dp square ([TOUCH_SIZE]). The tappable box asks for exactly 48 dp
 * with `requiredSize`, which ignores the 32 dp the cell allows: Compose then centres the larger box
 * on the cell, so it overflows by 8 dp on every side while the row still lays the cell out at
 * 32 dp. That box is the circle's node for screen readers and tests, a radio button named after
 * the range, so the size it reports is the real 48 dp touch area.
 *
 * While [enabled] is false the circle ignores touches and is drawn dimmed.
 */
@Composable
internal fun ColourRangeCircle(
    range: ColourRange,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fill = range.displayColour()
    val marks = markColours(MaterialTheme.colorScheme, fill)
    val description = stringResource(range.nameRes())
    // The 32 dp cell: what the row lays out and spaces.
    Box(
        // While disabled the whole circle is dimmed, as Material dims a disabled control.
        modifier = modifier.size(CELL_SIZE).alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                // 48 dp although the cell allows only 32 dp: the box overflows the cell, centred on it.
                .requiredSize(TOUCH_SIZE)
                .testTag(hslRangeTag(range))
                // Round, so the touch area is a circle and not the 48 dp square. There is
                // deliberately no ripple (`indication = null`): the ring and check mark on the
                // chosen circle are the feedback, and a ripple never let Compose's test clock go idle.
                .clip(CircleShape)
                .selectable(
                    selected = chosen,
                    enabled = enabled,
                    interactionSource = null,
                    indication = null,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                // The ring is drawn last, inside the disc's edge, so it never leaves the 32 dp disc.
                modifier = Modifier
                    .size(DISC_SIZE)
                    .clip(CircleShape)
                    .background(fill)
                    .border(width = RING_WIDTH, color = if (chosen) marks.first else Color.Transparent, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (chosen) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = marks.second,
                        modifier = Modifier.size(CHECK_SIZE),
                    )
                }
            }
        }
    }
}

/** Three circles, the middle one chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun ColourRangeCirclePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColourRangeCircle(ColourRange.RED, chosen = false, onClick = {})
                ColourRangeCircle(ColourRange.LIGHT_GREEN, chosen = true, onClick = {})
                ColourRangeCircle(ColourRange.NAVY_BLUE, chosen = true, onClick = {})
                ColourRangeCircle(ColourRange.PINK, chosen = false, onClick = {}, enabled = false)
            }
        }
    }
}
