package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel.colourrangerow

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag of the row of nine colour-range circles. */
const val HSL_RANGE_ROW_TAG = "hsl-range-row"

/** How far a circle's 48 dp touch area reaches past its 32 dp cell on each side: 8 dp. */
private val TOUCH_OVERFLOW = (TOUCH_SIZE - CELL_SIZE) / 2

/**
 * The first row of the HSL sheet: the nine colour ranges, Red to Pink in wheel order, each a
 * circle in its own colour. The [chosen] circle is marked; a tap calls [onChosen] with that range
 * and changes nothing in the photo. While [enabled] is false every circle is disabled.
 *
 * The row is 32 dp tall in the layout. Each circle's 48 dp touch area overflows its 32 dp cell by
 * 8 dp on every side (see [ColourRangeCircle]), so the scrolled content has 8 dp of padding at each
 * end: a scrolling row cuts off what lies outside it sideways, and the padding keeps the first and
 * last circles' touch areas whole. Nine 32 dp cells with 8 dp gaps (368 dp with the padding) fit a
 * 411 dp phone; on a narrower one the row scrolls sideways (`horizontalScroll`) instead of
 * squeezing the circles below a size that can be touched.
 */
@Composable
internal fun ColourRangeRow(
    chosen: ColourRange,
    onChosen: (ColourRange) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CELL_SIZE)
            .testTag(HSL_RANGE_ROW_TAG)
            .horizontalScroll(rememberScrollState())
            // Inside the scroll, so it scrolls with the circles and keeps the end touch areas whole.
            .padding(horizontal = TOUCH_OVERFLOW),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColourRange.entries.forEach { range ->
            ColourRangeCircle(
                range = range,
                chosen = range == chosen,
                onClick = { onChosen(range) },
                enabled = enabled,
            )
        }
    }
}

/** The row with Light blue chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun ColourRangeRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            ColourRangeRow(chosen = ColourRange.LIGHT_BLUE, onChosen = {})
        }
    }
}
