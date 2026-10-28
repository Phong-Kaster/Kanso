package com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.data.edit.edit.TextAlignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.border
import com.phongkaster.kanso.data.edit.edit.TextLayerLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
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
 * The picture of a sticker [content], ready to draw, or null for an id the catalogue does not
 * know. `painterResource` decodes the drawable once and remembers it, so a layer that is moved on
 * every frame does not decode its file again.
 */
@Composable
fun rememberLayerPainter(content: LayerContent.Picture): Painter? {
    val res = StickerCatalog.byId(content.pictureId)?.res ?: return null
    return painterResource(res)
}

/**
 * A layer's shape (its height divided by its width), or null when it cannot be drawn (a sticker
 * the catalogue does not know). The overlay needs only this: the pixels on screen come from the
 * live picture beneath it. A sticker's shape is its picture's; a text's is its block's, measured
 * with the bundled fonts ([layout]), an empty text being measured as its placeholder so it still
 * has a frame.
 */
@Composable
fun rememberLayerAspect(content: LayerContent, layout: TextLayerLayout): Float? = when (content) {
    is LayerContent.Picture -> rememberLayerPainter(content)?.aspectRatio()
    is LayerContent.Text -> remember(content, layout) {
        layout.measure(content, content.text.ifBlank { LayerContent.Text.PLACEHOLDER }).aspect
    }
}

/**
 * The app's text layout (the bundled fonts), made once for the application and remembered, for
 * composables that measure or draw a text layer.
 */
@Composable
fun rememberTextLayerLayout(): TextLayerLayout {
    val context = LocalContext.current.applicationContext
    return remember(context) { TextLayerLayout.forApp(context) }
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

/** A sample text for the shape preview: two lines in Dancing Script. */
private val sampleTextContent = LayerContent.Text(
    text = "Happy birthday\nMinh Anh!",
    colorArgb = 0xFFE53935.toInt(),
    font = TextFont.DANCING_SCRIPT,
    alignment = TextAlignment.CENTRE,
    wrapEms = 8f,
)

/**
 * The frame a sample text gets: an outlined box with the shape [rememberLayerAspect] measures
 * through [rememberTextLayerLayout], 200 dp wide, in light and in dark.
 */
@PreviewLightDark
@Composable
private fun LayerAspectPreview() {
    val aspect = rememberLayerAspect(sampleTextContent, rememberTextLayerLayout()) ?: 1f
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                Box(
                    modifier = Modifier
                        .size(width = 200.dp, height = 200.dp * aspect)
                        .border(2.dp, MaterialTheme.colorScheme.primary),
                )
            },
        )
    }
}

