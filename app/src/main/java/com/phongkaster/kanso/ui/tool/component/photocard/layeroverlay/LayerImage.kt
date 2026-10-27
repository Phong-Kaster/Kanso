package com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay

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
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/**
 * The picture of a layer's [content], ready to draw, or null when there is none (an id the
 * catalogue does not know). For a sticker, `painterResource` decodes the drawable once and
 * remembers it, so a layer that is moved on every frame does not decode its file again. The
 * overlay only reads the painter's shape ([aspectRatio]); the pixels on screen come from the
 * live picture beneath it.
 */
@Composable
fun rememberLayerPainter(content: LayerContent): Painter? = when (content) {
    is LayerContent.Picture -> {
        val res = StickerCatalog.byId(content.pictureId)?.res
        if (res == null) null else painterResource(res)
    }
}

/** A picture's height divided by its width, or 1 (square) when the painter does not know its size. */
fun Painter.aspectRatio(): Float {
    val w = intrinsicSize.width
    val h = intrinsicSize.height
    return if (w.isFinite() && h.isFinite() && w > 0f && h > 0f) h / w else 1f
}

/** The first catalogue sticker drawn from [rememberLayerPainter], in light and in dark. */
@PreviewLightDark
@Composable
private fun LayerPainterPreview() {
    val painter = rememberLayerPainter(LayerContent.Picture(StickerCatalog.all.first().pictureId)) ?: return
    KansoTheme(themeMode = ThemeMode.System) {
        // The theme stretches its single child to the whole screen, so a Box goes first.
        Box(Modifier.fillMaxSize()) {
            Image(painter = painter, contentDescription = null, modifier = Modifier.size(160.dp))
        }
    }
}
