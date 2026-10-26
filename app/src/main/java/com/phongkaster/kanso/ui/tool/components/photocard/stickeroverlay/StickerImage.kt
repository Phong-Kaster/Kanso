package com.phongkaster.kanso.ui.tool.components.photocard.stickeroverlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.data.settings.ThemeMode
import kotlin.math.roundToInt

/**
 * The picture of the catalogue sticker [pictureId], ready to draw, or null for an id the catalogue
 * does not know. `painterResource` decodes the drawable once and remembers it, so a sticker that
 * is moved on every frame does not decode its file again.
 */
@Composable
fun rememberStickerPainter(pictureId: String): Painter? {
    val res = StickerCatalog.byId(pictureId)?.res ?: return null
    return painterResource(res)
}

/** A picture's height divided by its width, or 1 (square) when the painter does not know its size. */
fun Painter.aspectRatio(): Float {
    val w = intrinsicSize.width
    val h = intrinsicSize.height
    return if (w.isFinite() && h.isFinite() && w > 0f && h > 0f) h / w else 1f
}

/**
 * Draws one sticker on the overlay: [painter] at [widthPx] by [heightPx] pixels, its centre at
 * ([centreX], [centreY]) of the overlay, turned clockwise by [degrees] about that centre and
 * mirrored left to right when [mirrored] is true. The overlay clips it to the shown picture, so a
 * sticker pushed past the edge is cut there.
 *
 * Three small steps do the work, in this order. `offset` puts the node's top-left corner where
 * the sticker's corner would be unturned. `layout` gives the node its exact size even when that
 * is larger than the overlay (a plain size would be squeezed to fit). `graphicsLayer` then
 * mirrors (scaleX -1) and turns (rotationZ) the drawn node about its own middle, without
 * changing where the layout thinks it is; the mirror is applied first, so the sticker is
 * mirrored and then turned, as a sticker's stored flag means.
 *
 * It draws the picture unfiltered: the live filter does not reach it.
 */
@Composable
fun StickerImage(
    painter: Painter,
    centreX: Float,
    centreY: Float,
    widthPx: Float,
    heightPx: Float,
    degrees: Float,
    mirrored: Boolean,
    modifier: Modifier = Modifier,
) {
    val w = widthPx.roundToInt().coerceAtLeast(1)
    val h = heightPx.roundToInt().coerceAtLeast(1)
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        // The caller's modifier (its test tag and semantics) goes AFTER the offset and the exact
        // size, so the node's reported bounds are the sticker's own place and size.
        modifier = Modifier
            .offset { IntOffset((centreX - w / 2f).roundToInt(), (centreY - h / 2f).roundToInt()) }
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(w, h) { placeable.place(0, 0) }
            }
            .then(modifier)
            .graphicsLayer {
                rotationZ = degrees
                scaleX = if (mirrored) -1f else 1f
            },
    )
}

/**
 * One sample sticker (the first of the catalogue) turned 20 degrees, in a 240 dp square that
 * clips it, mirrored or not. The outer Box fills the preview first, because the app theme would
 * otherwise stretch its single child to the whole screen.
 */
@Composable
private fun SampleStickerImage(mirrored: Boolean) {
    val painter = rememberStickerPainter(StickerCatalog.all.first().pictureId) ?: return
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .size(240.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clipToBounds(),
            ) {
                StickerImage(
                    painter = painter,
                    centreX = 300f,
                    centreY = 300f,
                    widthPx = 300f,
                    heightPx = 300f * painter.aspectRatio(),
                    degrees = 20f,
                    mirrored = mirrored,
                )
            }
        }
    }
}

/** A sticker drawn turned 20 degrees, in light and in dark. */
@PreviewLightDark
@Composable
private fun StickerImagePreview() {
    SampleStickerImage(mirrored = false)
}

/** The same sticker mirrored left to right, in light and in dark. */
@PreviewLightDark
@Composable
private fun StickerImageMirroredPreview() {
    SampleStickerImage(mirrored = true)
}
