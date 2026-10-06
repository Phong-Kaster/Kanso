package com.phongkaster.kanso.data.media

import android.content.Context
import com.photo.gallery.data.repository.MediaRepository
import com.photo.gallery.model.GalleryBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the phone's photos through Android's MediaStore, using the engine's [MediaRepository].
 *
 * MediaStore is the phone's shared database of photos, videos and music. Apps do not walk the
 * folders themselves: they ask MediaStore, and it answers with one row per photo. Since Android
 * 10 ("scoped storage") the way to open a photo is its content URI, not its file path: the path
 * may be unreadable to the app, or missing entirely. So this class only ever passes on
 * `GalleryImage.uri`, never the engine's `path` field.
 *
 * Asking MediaStore reads a database on disk, which can take a while with thousands of photos,
 * so every call moves to `Dispatchers.IO`, the pool of background threads meant for slow reads
 * and writes. The screen's main thread stays free to draw.
 */
class MediaStorePhotoSource(context: Context) : PhotoSource {

    /** The application context: it lives as long as the app, so holding it leaks no screen. */
    private val appContext = context.applicationContext

    /** The engine's MediaStore reader. */
    private val repository = MediaRepository()

    /** Returns the photo albums, in the engine's order (by name). */
    override suspend fun albums(): List<Album> = withContext(Dispatchers.IO) {
        repository.getImageBuckets(appContext).map { bucket -> Album(bucket.id, bucket.name) }
    }

    /**
     * Returns the photos of album [albumId] (or of every album when it is null) in the engine's
     * order: newest change first.
     */
    override suspend fun photos(albumId: Long?): List<Photo> = withContext(Dispatchers.IO) {
        // The engine filters by the bucket's id only; the name is not used for the query.
        val bucket = albumId?.let { GalleryBucket(id = it, name = "") }
        repository.getBucketImages(appContext, bucket).map { image ->
            Photo(
                id = image.id,
                uri = image.uri.toString(),
                name = image.name,
                albumId = image.bucketId,
                modifiedSeconds = image.modifiedTime,
            )
        }
    }
}
