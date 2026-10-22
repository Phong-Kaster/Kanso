package com.phongkaster.kanso.ui.tool.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.ceil

/** Test tag of the checkerboard drawn behind a cut-out photo. */
const val CUTOUT_CHECKERBOARD_TAG = "cutout-checkerboard"

/** The side of one checkerboard tile. */
private val TILE_SIZE = 12.dp

/**
 * A semantics property carrying the checkerboard's two tile colours, the lighter first.
 *
 * Semantics are what accessibility services and tests read about a node. Compose lets an app add
 * its own property with a `SemanticsPropertyKey`, so a test can ask the checkerboard which two
 * colours it paints and check that they differ, without reading pixels.
 */
val CheckerboardTileColours = SemanticsPropertyKey<List<Color>>("CheckerboardTileColours")

/**
 * The grey-and-white squares photo editors draw behind a picture with transparent parts, so
 * "see-through" is visible: wherever the cut-out photo has no pixels, the checks show.
 *
 * It is drawn with a `Canvas`, a composable that hands its size to a drawing lambda: first the
 * whole area in the lighter colour, then every other tile in the darker one. Both colours are
 * roles of the theme (`surfaceContainerLowest` and `surfaceContainerHighest`), so the checks follow
 * light and dark mode and the chosen theme colour. The node carries [CUTOUT_CHECKERBOARD_TAG] and
 * its two colours in [CheckerboardTileColours].
 *
 * [tileSize] is the side of one square.
 */
@Composable
fun Checkerboard(modifier: Modifier = Modifier, tileSize: Dp = TILE_SIZE) {
    val light = MaterialTheme.colorScheme.surfaceContainerLowest
    val dark = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(
        modifier = modifier
            .testTag(CUTOUT_CHECKERBOARD_TAG)
            .semantics { set(CheckerboardTileColours, listOf(light, dark)) },
    ) {
        // toPx turns density-independent dp into this screen's pixels; at least 1 px per tile.
        val tile = tileSize.toPx().coerceAtLeast(1f)
        drawRect(color = light)
        val columns = ceil(size.width / tile).toInt()
        val rows = ceil(size.height / tile).toInt()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                // A tile is dark when its row and column add up to an odd number: a chess pattern.
                if ((row + column) % 2 == 1) {
                    drawRect(
                        color = dark,
                        topLeft = Offset(column * tile, row * tile),
                        size = Size(tile, tile),
                    )
                }
            }
        }
    }
}

/**
 * The checkerboard on a 160 dp square, drawn once in light and once in dark. The outer Box fills
 * the preview first, because KansoTheme would otherwise stretch its single child to the screen.
 */
@PreviewLightDark
@Composable
private fun CheckerboardPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Checkerboard(Modifier.padding(16.dp).size(160.dp))
        }
    }
}
