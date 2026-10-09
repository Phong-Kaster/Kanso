package com.phongkaster.kanso.gallery

import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred

/**
 * A photo library for tests: it holds [albums] and [allPhotos], answers like the real source
 * (an album's photos, newest first), records the thread of every call in [callingThreads], and
 * throws [failure] instead of answering when one is set.
 *
 * A test can also hold its answers back with [holdReads] and let them go with [releaseReads], to
 * look at the gallery while its photos are still loading. Until [holdReads] is called every read
 * answers at once, as it always did.
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

    /**
     * The gate the reads wait on while they are held: a CompletableDeferred is a value that is
     * not there yet, and `await()` suspends until someone calls `complete`. Null means the reads
     * are not held and answer at once.
     */
    @Volatile
    private var gate: CompletableDeferred<Unit>? = null

    /** Returns [albums], after recording the calling thread (and waiting while reads are held). */
    override suspend fun albums(): List<Album> {
        callingThreads += Thread.currentThread()
        waitWhileHeld()
        failure?.let { throw it }
        return albums
    }

    /** Returns the photos of [albumId] (all when null), newest first, after recording the thread. */
    override suspend fun photos(albumId: Long?): List<Photo> {
        callingThreads += Thread.currentThread()
        waitWhileHeld()
        failure?.let { throw it }
        return allPhotos
            .filter { albumId == null || it.albumId == albumId }
            .sortedByDescending { it.modifiedSeconds }
    }

    /**
     * Holds every read from now on: each one waits, without answering, until [releaseReads] is
     * called. The waiting read suspends its coroutine, so it does not block the thread it runs on.
     */
    fun holdReads() {
        gate = CompletableDeferred()
    }

    /** Lets every waiting read answer, and every later read answer at once. */
    fun releaseReads() {
        val held = gate
        gate = null
        held?.complete(Unit)
    }

    /** Suspends while the reads are held (see [holdReads]); returns at once otherwise. */
    private suspend fun waitWhileHeld() {
        gate?.await()
    }
}
