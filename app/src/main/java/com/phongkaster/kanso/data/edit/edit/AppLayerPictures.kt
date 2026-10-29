package com.phongkaster.kanso.data.edit.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * The app's [LayerPictures]: for a [LayerContent.Picture] it finds the sticker in [StickerCatalog]
 * and decodes its drawable file; for a [LayerContent.Text] it draws the words with the bundled
 * fonts through [TextLayerLayout], at the width asked for, so the letters stay sharp at any size;
 * a [LayerContent.Date] is drawn the same way, as the text of its words.
 *
 * Decoding with `inSampleSize` makes the bitmap smaller while it is read (the size is divided by
 * a power of two), which is much cheaper than decoding it whole and scaling it down; the
 * processor then scales to the exact size. Nothing is cached, so every call returns a fresh
 * bitmap that nobody else holds, which satisfies [LayerPictures]'s ownership rule.
 */
class AppLayerPictures(private val context: Context) : LayerPictures {

    /** The text layout with the app's fonts, made on first use. */
    private val texts by lazy { TextLayerLayout.forApp(context) }

    /**
     * Returns the picture of [content] at [widthPx] wide or a little wider. A sticker is decoded
     * from its drawable (never narrower unless the file itself is narrower), as ARGB_8888 so
     * transparent edges stay transparent; null for an id the catalog does not know or a file that
     * cannot be decoded. A text is drawn fresh at [widthPx]; null when it has no letters.
     */
    override fun picture(content: LayerContent, widthPx: Int): Bitmap? = when (content) {
        is LayerContent.Picture -> sticker(content.pictureId, widthPx)
        is LayerContent.Text -> texts.render(content, widthPx)
        // A date is drawn as the one-line text of its words (ADR-006).
        is LayerContent.Date -> texts.render(content.asText(), widthPx)
    }

    /** Decodes the catalogue sticker [pictureId] at [widthPx] wide or a little wider. */
    private fun sticker(pictureId: String, widthPx: Int): Bitmap? {
        val sticker = StickerCatalog.byId(pictureId) ?: return null
        val resources = context.resources

        // First pass: read only the picture's size, no pixels.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, sticker.res, bounds)
        if (bounds.outWidth <= 0) return null

        // The largest power of two that still leaves the decoded width at or above widthPx.
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= widthPx) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return try {
            BitmapFactory.decodeResource(resources, sticker.res, options)
        } catch (e: OutOfMemoryError) {
            null
        }
    }
}
