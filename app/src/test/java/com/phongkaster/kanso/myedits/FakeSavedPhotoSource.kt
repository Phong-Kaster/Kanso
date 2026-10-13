package com.phongkaster.kanso.myedits

import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.SavedPhotoSource
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred

/**
 * Kanso's saved photos for tests: it answers with [photos] exactly as given (in that order, so a
 * test can check who sorts them), counts its reads in [reads], and throws [failure] instead of
 * answering when one is set.
 *
 * A test can also hold its answers back with [holdReads] and let them go with [releaseReads], to
 * look at a screen while its photos are still loading, as `FakePhotoSource` does for the gallery.
 * Until [holdReads] is called every read answers at once.
 *
 * Its fields are @Volatile because a screen may read them on another thread while the test sets
 * them on the main thread.
 */
class FakeSavedPhotoSource(photos: List<Photo> = emptyList()) : SavedPhotoSource {

    /** The photos the fake answers with, in the order it answers. */
    @Volatile
    var photos: List<Photo> = photos

    /** When set, every read throws it instead of answering. */
    @Volatile
    var failure: Exception? = null

    /** How many reads have started. */
    val reads = AtomicInteger()

    /**
     * The gate the reads wait on while they are held: `await()` suspends until someone calls
     * `complete`. Null means the reads are not held and answer at once.
     */
    @Volatile
    private var gate: CompletableDeferred<Unit>? = null

    /** Returns [photos] (or throws [failure]), after waiting while reads are held. */
    override suspend fun savedPhotos(): List<Photo> {
        reads.incrementAndGet()
        gate?.await()
        failure?.let { throw it }
        return photos
    }

    /** Holds every read from now on, until [releaseReads] is called. */
    fun holdReads() {
        gate = CompletableDeferred()
    }

    /** Lets every waiting read answer, and every later read answer at once. */
    fun releaseReads() {
        val held = gate
        gate = null
        held?.complete(Unit)
    }
}
