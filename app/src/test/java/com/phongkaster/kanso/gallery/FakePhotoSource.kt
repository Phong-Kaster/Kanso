package com.phongkaster.kanso.gallery

import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A photo library for tests: it holds [albums] and [allPhotos], answers like the real source
 * (an album's photos, newest first), records the thread of every call in [callingThreads], and
 * throws [failure] instead of answering when one is set.
 *
 * Its fields are @Volatile because the gallery reads them on a background thread while the test
 * sets them on the main thread.
 */
class FakePhotoSource(
    albums: List<Album>,
    allPhotos: List<Photo>,
) : PhotoSource {

    /** The albums the fake answers with. */
    @Volatile
    var albums: List<Album> = albums

    /** Every photo of every album, in any order; the fake sorts them when asked. */
    @Volatile
    var allPhotos: List<Photo> = allPhotos

    /** When set, every call throws it instead of answering. */
    @Volatile
    var failure: Exception? = null

    /** The thread each call ran on, in call order. */
    val callingThreads: MutableList<Thread> = CopyOnWriteArrayList()

    /** Returns [albums], after recording the calling thread. */
    override suspend fun albums(): List<Album> {
        callingThreads += Thread.currentThread()
        failure?.let { throw it }
        return albums
    }

    /** Returns the photos of [albumId] (all when null), newest first, after recording the thread. */
    override suspend fun photos(albumId: Long?): List<Photo> {
        callingThreads += Thread.currentThread()
        failure?.let { throw it }
        return allPhotos
            .filter { albumId == null || it.albumId == albumId }
            .sortedByDescending { it.modifiedSeconds }
    }
}
