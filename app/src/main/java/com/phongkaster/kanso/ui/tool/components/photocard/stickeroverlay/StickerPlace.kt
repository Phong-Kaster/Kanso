package com.phongkaster.kanso.ui.tool.components.photocard.stickeroverlay

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/**
 * The invisible node that marks where one sticker is on the shown picture: [widthPx] by [heightPx]
 * pixels, its centre at ([centreX], [centreY]) of the overlay, turned clockwise by [degrees] about
 * that centre. It draws nothing: the sticker's picture is part of the live picture beneath (the
 * filtered preview for the stickers at rest, the live preview's own drawing for the one under a
 * finger), so drawing it here as well would show it twice. The node still carries the caller's
 * test tag and semantics, and its bounds are the sticker's own place and size, as the
 * accessibility tree and the tests expect.
 */
@Composable
internal fun StickerPlace(
    centreX: Float,
    centreY: Float,
    widthPx: Float,
    heightPx: Float,
    degrees: Float,
    modifier: Modifier = Modifier,
) {
    val w = widthPx.roundToInt().coerceAtLeast(1)
    val h = heightPx.roundToInt().coerceAtLeast(1)
    Box(
        // Three steps: put the corner where it belongs, give the exact size even when it is
        // larger than the overlay, then turn about the middle.
        modifier = Modifier
            .offset { IntOffset((centreX - w / 2f).roundToInt(), (centreY - h / 2f).roundToInt()) }
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(w, h) { placeable.place(0, 0) }
            }
            .then(modifier)
            .graphicsLayer { rotationZ = degrees },
    )
}

/**
 * A sample place turned 20 degrees, made visible with an outline (the real node draws nothing) so
 * the preview shows where the node lies. The outer Box fills the preview first, because the app
 * theme would otherwise stretch its single child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun StickerPlacePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.size(240.dp).clipToBounds()) {
                StickerPlace(
                    centreX = 300f,
                    centreY = 300f,
                    widthPx = 300f,
                    heightPx = 240f,
                    degrees = 20f,
                    modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
