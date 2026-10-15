package com.phongkaster.kanso.data.edit.retouch

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Finds the faces in a photo, for the Retouch tool.
 *
 * It is an interface, a seam: the app's real finder ([EngineFaceFinder]) runs the engine's
 * face-landmark model, which is native code and cannot load in a JVM test, so tests hand
 * [RetouchProcessor] a fake finder that simply returns the rectangles they want.
 */
interface FaceFinder {

    /**
     * Returns one rectangle per face found in [photo], in pixels of [photo] (left, top, right,
     * bottom, with right and bottom exclusive, as `android.graphics.Rect` counts them). An empty
     * list means no face was found. [photo] is only read, never changed.
     *
     * It is a suspend function because the real model may load itself first, which takes time.
     */
    suspend fun findFaces(photo: Bitmap): List<Rect>
}
