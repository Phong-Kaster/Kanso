package com.phongkaster.kanso.data.edit.face

import android.graphics.Bitmap

/**
 * Finds every face in a photo and places its landmarks: the first three steps of a face edit.
 *
 * A face editor such as Meitu works in four steps:
 * 1. **Detect**: a small, fast model finds a box around each face.
 * 2. **Align**: a second model looks only inside that box (cut out and turned upright using the
 *    eyes) and places hundreds of landmarks on it.
 * 3. **Refine**: the eyes, lips and irises get a more precise second look.
 * 4. **Parse**: a third model labels each pixel (skin, hair, lips...). Face width does not need it.
 *
 * MediaPipe's face landmarker does steps 1 to 3 in one call: `face_landmarker.task` is a zip that
 * holds `face_detector.tflite` (step 1) and `face_landmarks_detector.tflite` (steps 2 and 3).
 *
 * It is an interface, a seam: the real finder ([EngineLandmarkFinder]) runs native code that
 * cannot load in a JVM test, so tests hand the processor a fake that returns the points they want.
 */
interface LandmarkFinder {

    /**
     * Returns the landmarks of every face in [photo], in pixels of [photo]; an empty list means no
     * face was found. [photo] is only read, never changed.
     *
     * It is a suspend function because the real model loads itself on the first call, which takes
     * a moment.
     */
    suspend fun findLandmarks(photo: Bitmap): List<FaceLandmarks>
}
