package com.phongkaster.kanso.tool

import android.content.Context
import android.graphics.Bitmap
import com.phongkaster.kanso.data.photo.LoadedPhoto
import com.phongkaster.kanso.data.photo.PhotoLoader
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A [PhotoLoader] that decodes nothing: it hands back [bitmap] (or null, "unreadable") at once
 * and records every size it was asked for.
 */
class FakePhotoLoader(
    context: Context,
    private val bitmap: Bitmap?,
) : PhotoLoader(context) {

    /** The `maxEdgePx` of every call, oldest first. */
    val requestedEdges = CopyOnWriteArrayList<Int>()

    /** Records the request and returns the fixed bitmap. */
    override suspend fun load(uri: String, maxEdgePx: Int): LoadedPhoto? {
        requestedEdges += maxEdgePx
        return bitmap?.let(::LoadedPhoto)
    }
}
