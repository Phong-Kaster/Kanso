package com.phongkaster.kanso.tool

import android.graphics.Bitmap
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult

/**
 * A saver for tests that are not about saving: it needs no permission and writes nothing, and
 * every save fails. Tests of the tool screen's photo, tags and back arrow pass it in.
 */
class NoSavePhotoSaver : PhotoSaver {

    /** Never asks for a permission. */
    override fun needsWritePermission(): Boolean = false

    /** Writes nothing and reports failure. */
    override suspend fun save(sourceUri: String): SaveResult = SaveResult.Failed

    /** Writes nothing for an edited picture either, and reports failure. */
    override suspend fun save(bitmap: Bitmap): SaveResult = SaveResult.Failed
}
