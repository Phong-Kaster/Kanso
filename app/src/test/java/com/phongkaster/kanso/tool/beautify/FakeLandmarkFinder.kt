package com.phongkaster.kanso.tool.beautify

import android.graphics.Bitmap
import com.phongkaster.kanso.data.edit.face.FaceLandmarks
import com.phongkaster.kanso.data.edit.face.LandmarkFinder
import com.phongkaster.kanso.tool.face.SampleFace
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A stand-in for MediaPipe's face landmarker (which cannot load on the JVM): it records the size
 * of every photo it is handed, then returns the faces [facesFor] makes for that photo; by default
 * one oval face filling the middle half of the photo, like [FakeFaceFinder.centre].
 */
class FakeLandmarkFinder(
    private val facesFor: (Bitmap) -> List<FaceLandmarks> = { listOf(centre(it)) },
) : LandmarkFinder {

    /** Width and height of each photo handed to [findLandmarks], oldest first. */
    val received = CopyOnWriteArrayList<Pair<Int, Int>>()

    /** Records [photo]'s size and returns the fixed faces. */
    override suspend fun findLandmarks(photo: Bitmap): List<FaceLandmarks> {
        received += photo.width to photo.height
        return facesFor(photo)
    }

    /** Ready-made answers for [facesFor]. */
    companion object {

        /** An oval face in the middle half of [photo] on each axis. */
        fun centre(photo: Bitmap): FaceLandmarks =
            SampleFace.oval(photo.width / 2f, photo.height / 2f, photo.width / 4f, photo.height / 4f)

        /** A finder that never finds a face. */
        fun none(): FakeLandmarkFinder = FakeLandmarkFinder(facesFor = { emptyList() })
    }
}
