package com.phongkaster.kanso.data.edit.cutout

import android.graphics.Bitmap

/**
 * Finds the subject of a photo (a person, for Kanso's bundled model) and says, for every pixel,
 * how sure it is that the pixel belongs to that subject.
 *
 * It is the seam between Remove background and the machine-learning engine: the engine is native
 * code that cannot load in a JVM test, so [CutoutProcessor] only knows this interface, the app
 * hands it [EngineSegmenter], and a test hands it a fake that returns a mask it drew itself.
 */
interface Segmenter {

    /**
     * Segments [photo] and returns its mask, or null when no subject was found.
     *
     * The mask is one number per pixel of [photo], row by row from the top-left corner (so its
     * size is `photo.width * photo.height`, and the pixel at (x, y) is at index
     * `y * photo.width + x`). Each number is the foreground probability, from 0 (surely
     * background) to 1 (surely the subject). A plain `FloatArray` keeps the mask free of any
     * engine type, so tests can build one by hand.
     *
     * It may throw: an `OutOfMemoryError` on a large photo, or an exception when the model will
     * not load. [CutoutProcessor] turns those into failures the screen can show.
     */
    suspend fun segment(photo: Bitmap): FloatArray?
}
