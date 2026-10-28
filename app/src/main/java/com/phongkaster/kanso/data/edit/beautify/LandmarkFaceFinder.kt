package com.phongkaster.kanso.data.edit.beautify

import android.graphics.Bitmap
import android.graphics.Rect
import com.phongkaster.kanso.data.edit.face.LandmarkFinder
import com.phongkaster.kanso.data.edit.face.paddedFaceBox

/**
 * The [FaceFinder] the app uses for smoothing: one padded box per face ([paddedFaceBox]), made
 * from the landmarks of [landmarks], the same face model Face width uses.
 *
 * **Why not the engine's own finder.** Loading the engine's `DetectFaceLandmarkUC` beside Kanso's
 * MediaPipe landmarker started a second MediaPipe graph in the app, and that crashed the Galaxy
 * A13 with a native SIGBUS inside `Graph.startRunningGraph` (seen 2026-10-28). One model for both
 * tools avoids it, and saves loading the model twice.
 */
class LandmarkFaceFinder(private val landmarks: LandmarkFinder) : FaceFinder {

    /** Finds the faces' landmarks in [photo] and returns one padded box per face, in pixels of [photo]. */
    override suspend fun findFaces(photo: Bitmap): List<Rect> =
        landmarks.findLandmarks(photo).mapNotNull(transform = { face -> paddedFaceBox(face, photo.width, photo.height) })
}
