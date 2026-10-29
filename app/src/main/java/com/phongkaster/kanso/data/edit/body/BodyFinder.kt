package com.phongkaster.kanso.data.edit.body

import android.graphics.Bitmap

/**
 * Finds every person in a photo and places their body landmarks, for the Beautify tool's Body
 * reshapes (Waist).
 *
 * MediaPipe's pose landmarker does it in one call: `pose_landmarker_full.task` is a zip that holds
 * `pose_detector.tflite` (finds a box around each person) and `pose_landmarks_detector.tflite`
 * (places 33 points in each box and paints the person's mask).
 *
 * It is an interface, a seam: the real finder ([EngineBodyFinder]) runs native code that cannot
 * load in a JVM test, so tests hand the processor a fake that returns the bodies they want.
 */
interface BodyFinder {

    /**
     * Returns every person in [photo], landmarks in pixels of [photo]; an empty list means no
     * person was found. [photo] is only read, never changed.
     *
     * It is a suspend function because the real model loads itself on the first call, which takes
     * a moment.
     */
    suspend fun findBodies(photo: Bitmap): List<BodyLandmarks>
}
