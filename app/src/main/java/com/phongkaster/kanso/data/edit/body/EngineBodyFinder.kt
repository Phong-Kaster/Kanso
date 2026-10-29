package com.phongkaster.kanso.data.edit.body

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.nio.ByteOrder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The real [BodyFinder]: MediaPipe's pose landmarker, run on the phone.
 *
 * Its model, `pose_landmarker_full.task` (about 9 MB), ships in the app's own assets
 * (`app/src/main/assets/`), so nothing is downloaded. It is Google's "full" pose model: more
 * accurate than "lite" and much smaller than "heavy". It runs on the same MediaPipe library as the
 * face landmarker ([com.phongkaster.kanso.data.edit.face.EngineLandmarkFinder]).
 *
 * For each person it gives 33 points as fractions of the photo (this class turns them into
 * pixels), each with a visibility, and, because [build] asks for it, a **segmentation mask**: one
 * value per pixel, how sure the model is that the pixel shows that person. The mask comes back as
 * an `MPImage` of 32-bit floats, read here into a [BodyMask].
 *
 * The landmarker is built on the first photo, not when the app starts, and then kept; a [Mutex]
 * makes sure two runs never build two or use one at the same time. This file is the only one that
 * names MediaPipe's pose classes, and it never runs in a JVM test.
 */
class EngineBodyFinder(private val context: Context) : BodyFinder {

    /** The landmarker once built; null before the first photo. */
    private var landmarker: PoseLandmarker? = null

    /** Lets only one call at a time build or use the landmarker. */
    private val lock = Mutex()

    /**
     * Runs the landmarker on [photo] and returns each person's 33 points in pixels of [photo],
     * with their mask. Throws when the model cannot load, which the processor reports as the
     * model being unavailable.
     */
    override suspend fun findBodies(photo: Bitmap): List<BodyLandmarks> = lock.withLock(
        action = {
            val ready = landmarker ?: build().also(block = { built -> landmarker = built })
            // MediaPipe reads ARGB_8888 pixels only; anything else gets a copy in that format.
            val argb = if (photo.config == Bitmap.Config.ARGB_8888) photo else photo.copy(Bitmap.Config.ARGB_8888, false)
            try {
                val result = ready.detect(BitmapImageBuilder(argb).build())
                val width = photo.width.toFloat()
                val height = photo.height.toFloat()
                // Optional<List<MPImage>>: present because build() asked for masks, one per person.
                val masks = if (result.segmentationMasks().isPresent) result.segmentationMasks().get() else emptyList()
                result.landmarks().mapIndexed(
                    transform = { index, person ->
                        BodyLandmarks(
                            points = person.map(
                                transform = { point ->
                                    // Visibility is optional in MediaPipe's API; no value means "unknown", read as sure.
                                    val visibility = if (point.visibility().isPresent) point.visibility().get() else 1f
                                    BodyPoint(point.x() * width, point.y() * height, visibility)
                                },
                            ),
                            mask = masks.getOrNull(index)?.let(block = { maskOf(it, photo.width, photo.height) }),
                        )
                    },
                )
            } finally {
                if (argb !== photo) argb.recycle()
            }
        },
    )

    /**
     * Reads one segmentation [mask] (an `MPImage` of 32-bit floats, one per pixel) into a
     * [BodyMask] for a [photoWidth] x [photoHeight] photo. `ByteBufferExtractor` hands over the
     * image's raw bytes; they were written by native code, so they are read in the phone's own
     * byte order.
     */
    private fun maskOf(mask: MPImage, photoWidth: Int, photoHeight: Int): BodyMask {
        val values = FloatArray(mask.width * mask.height)
        ByteBufferExtractor.extract(mask).order(ByteOrder.nativeOrder()).asFloatBuffer().get(values)
        return BodyMask(mask.width, mask.height, values, photoWidth, photoHeight)
    }

    /**
     * Builds a landmarker for still photos (RunningMode.IMAGE: one picture at a time, answered
     * straight away) that looks for up to [MAX_PEOPLE] people and paints each one's mask.
     */
    private fun build(): PoseLandmarker {
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
            .setRunningMode(RunningMode.IMAGE)
            .setNumPoses(MAX_PEOPLE)
            .setMinPoseDetectionConfidence(MIN_CONFIDENCE)
            .setOutputSegmentationMasks(true)
            .build()
        return PoseLandmarker.createFromOptions(context.applicationContext, options)
    }

    /** The landmarker's fixed settings. */
    private companion object {
        /** The model bundle's name in the app's assets. */
        const val MODEL_ASSET = "pose_landmarker_full.task"

        /** The most people reshaped in one photo. */
        const val MAX_PEOPLE = 3

        /** How sure the detector must be (0..1) before a box counts as a person. */
        const val MIN_CONFIDENCE = 0.5f
    }
}
