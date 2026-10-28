package com.phongkaster.kanso.tool.beautify

import android.graphics.Bitmap
import android.graphics.Rect
import com.phongkaster.kanso.data.edit.beautify.FaceFinder
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A stand-in for the engine's face finder (which cannot load on the JVM): it records the size of
 * every photo it is handed, then throws [failure] if one is given, and otherwise returns the
 * rectangles [facesFor] makes for that photo.
 */
class FakeFaceFinder(
    private val failure: Throwable? = null,
    private val facesFor: (Bitmap) -> List<Rect> = { listOf(centre(it)) },
) : FaceFinder {

    /** Width and height of each photo handed to [findFaces], oldest first. */
    val received = CopyOnWriteArrayList<Pair<Int, Int>>()

    /** Records [photo]'s size, then throws [failure] or returns the fixed faces. */
    override suspend fun findFaces(photo: Bitmap): List<Rect> {
        received += photo.width to photo.height
        failure?.let { throw it }
        return facesFor(photo)
    }

    /** Ready-made answers for [facesFor]. */
    companion object {

        /** The middle half of [photo] on each axis: a 200 x 200 photo gives (50, 50)-(150, 150). */
        fun centre(photo: Bitmap): Rect =
            Rect(photo.width / 4, photo.height / 4, photo.width * 3 / 4, photo.height * 3 / 4)

        /** A finder that never finds a face. */
        fun none(): FakeFaceFinder = FakeFaceFinder(facesFor = { emptyList() })
    }
}
