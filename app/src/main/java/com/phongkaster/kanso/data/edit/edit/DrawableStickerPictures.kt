package com.phongkaster.kanso.data.edit.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * The app's [StickerPictures]: finds a sticker in [StickerCatalog] and decodes its drawable file.
 *
 * Decoding with `inSampleSize` makes the bitmap smaller while it is read (the size is divided by
 * a power of two), which is much cheaper than decoding it whole and scaling it down; the
 * processor then scales to the exact size. Nothing is cached, so every call returns a fresh
 * bitmap that nobody else holds, which satisfies [StickerPictures]'s ownership rule.
 */
class DrawableStickerPictures(private val context: Context) : StickerPictures {

    /**
     * Returns the sticker [pictureId] decoded at [widthPx] wide or a little wider (never narrower
     * unless the file itself is narrower), as ARGB_8888 so transparent edges stay transparent.
     * Returns null for an id the catalog does not know or a file that cannot be decoded.
     */
    override fun picture(pictureId: String, widthPx: Int): Bitmap? {
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
