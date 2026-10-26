package com.phongkaster.kanso.ui.tool.components.photocard.stickeroverlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

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

/** The first catalogue sticker drawn from [rememberStickerPainter], in light and in dark. */
@PreviewLightDark
@Composable
private fun StickerPainterPreview() {
    val painter = rememberStickerPainter(StickerCatalog.all.first().pictureId) ?: return
    KansoTheme(themeMode = ThemeMode.System) {
        // The theme stretches its single child to the whole screen, so a Box goes first.
        Box(Modifier.fillMaxSize()) {
            Image(painter = painter, contentDescription = null, modifier = Modifier.size(160.dp))
        }
    }
}
