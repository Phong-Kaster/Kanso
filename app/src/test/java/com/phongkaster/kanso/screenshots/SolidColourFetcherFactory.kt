package com.phongkaster.kanso.screenshots

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.util.concurrent.atomic.AtomicInteger

/**
 * Teaches Coil the test-only `fake` scheme: for `fake://color/AARRGGBB` it hands back a 64 x 64
 * bitmap filled with that colour. Coil crops and scales it to the cell like a real thumbnail.
 * [fetched] counts the bitmaps made, so a test can wait for them.
 */
class SolidColourFetcherFactory : Fetcher.Factory<Uri> {

    /** How many thumbnails have been made so far. */
    val fetched = AtomicInteger()

    /** Returns a fetcher for `fake` URIs, or null so Coil tries its own fetchers for others. */
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        if (data.scheme != FAKE_SCHEME) return null
        val colour = data.lastPathSegment.orEmpty().toLong(radix = 16).toInt()
        return object : Fetcher {
            /** Makes the solid-colour bitmap. */
            override suspend fun fetch(): FetchResult {
                val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(colour)
                fetched.incrementAndGet()
                return DrawableResult(
                    drawable = BitmapDrawable(options.context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.MEMORY,
                )
            }
        }
    }
}
