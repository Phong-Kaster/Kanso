package com.phongkaster.kanso.data.edit.face

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The real [LandmarkFinder]: MediaPipe's face landmarker, which runs steps 1 to 3 of a face edit
 * (detect, align, refine) on the phone.
 *
 * Its model, `face_landmarker.task`, is already bundled in the app (it comes with the engine's
 * `localml` library, whose assets are merged into the app's), so nothing is downloaded. Inside
 * that file, `face_detector.tflite` (BlazeFace, about 0.2 MB) finds each face's box, and
 * `face_landmarks_detector.tflite` (about 2.5 MB) places 478 points in each box, with a closer
 * look at the eyes, lips and irises.
 *
 * MediaPipe gives the points as fractions of the photo's width and height (0 to 1); this class
 * turns them into pixels. The landmarker is built on the first photo, not when the app starts,
 * and then kept; a [Mutex] makes sure two runs never build two. This file is the only one that
 * names MediaPipe classes, and it never runs in a JVM test.
 */
class EngineLandmarkFinder(private val context: Context) : LandmarkFinder {

    /** The landmarker once built; null before the first photo. */
    private var landmarker: FaceLandmarker? = null

    /** Lets only one call at a time build or use the landmarker. */
    private val lock = Mutex()

    /**
     * Runs the landmarker on [photo] and returns each face's 478 points in pixels of [photo].
     * Throws when the model cannot load, which the processor reports as the model being
     * unavailable.
     */
    override suspend fun findLandmarks(photo: Bitmap): List<FaceLandmarks> = lock.withLock(
        action = {
            // One call at a time: a MediaPipe landmarker must not be used from two threads at once
            // (the zoom, a live look and a render can each ask), and only one is ever built.
            val ready = landmarker ?: build().also(block = { built -> landmarker = built })
            // MediaPipe reads ARGB_8888 pixels only; anything else gets a copy in that format.
            val argb = if (photo.config == Bitmap.Config.ARGB_8888) photo else photo.copy(Bitmap.Config.ARGB_8888, false)
            try {
                val result = ready.detect(BitmapImageBuilder(argb).build())
                val width = photo.width.toFloat()
                val height = photo.height.toFloat()
                result.faceLandmarks().map(
                    transform = { face ->
                        FaceLandmarks(face.map(transform = { point -> FacePoint(point.x() * width, point.y() * height) }))
                    },
                )
            } finally {
                if (argb !== photo) argb.recycle()
            }
        },
    )

    /**
     * Builds a landmarker for still photos (RunningMode.IMAGE: one picture at a time, answered
     * straight away) that looks for up to [MAX_FACES] faces.
     */
    private fun build(): FaceLandmarker {
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(MAX_FACES)
            .setMinFaceDetectionConfidence(MIN_CONFIDENCE)
            .build()
        return FaceLandmarker.createFromOptions(context.applicationContext, options)
    }

    /** The landmarker's fixed settings. */
    private companion object {
        /** The model bundle's name in the app's assets. */
        const val MODEL_ASSET = "face_landmarker.task"

        /** The most faces reshaped in one photo. */
        const val MAX_FACES = 5

        /** How sure the detector must be (0..1) before a box counts as a face. */
        const val MIN_CONFIDENCE = 0.5f
    }
}
