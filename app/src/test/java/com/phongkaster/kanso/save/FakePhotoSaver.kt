package com.phongkaster.kanso.save

import android.graphics.Bitmap
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred

/**
 * A [PhotoSaver] that writes nothing: it records each photo it is asked to save and answers
 * [result]. With [gate] set, each save waits until the test completes it, so the screen can be
 * seen mid-save.
 */
class FakePhotoSaver(
    var needsPermission: Boolean = false,
    var result: SaveResult = SaveResult.Saved("content://media/external_primary/images/media/1"),
    val gate: CompletableDeferred<Unit>? = null,
) : PhotoSaver {

    /** The source Uri of every save of an unedited photo asked for, oldest first. */
    val calls = CopyOnWriteArrayList<String>()

    /** The bitmap of every save of an edited picture asked for, oldest first. */
    val bitmapCalls = CopyOnWriteArrayList<Bitmap>()

    /** Answers [needsPermission]. */
    override fun needsWritePermission(): Boolean = needsPermission

    /** Records the call, waits for [gate] if any, and answers [result]. */
    override suspend fun save(sourceUri: String): SaveResult {
        calls += sourceUri
        gate?.await()
        return result
    }

    /** Records the edited bitmap, waits for [gate] if any, and answers [result]. */
    override suspend fun save(bitmap: Bitmap): SaveResult {
        bitmapCalls += bitmap
        gate?.await()
        return result
    }
}
