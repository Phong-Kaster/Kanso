package com.phongkaster.kanso.data.edit.retouch

import android.graphics.Bitmap
import android.graphics.Rect
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * The Retouch tool's edit: smoother skin on every face in the photo, as much as the strength
 * slider asks for.
 *
 * The steps, in order:
 * 1. Scale the photo down so its longest edge is at most [maxEdgePx] (2000 pixels by default,
 *    [WorkingSize.RETOUCH]), so a 24-megapixel photo never runs the phone out of memory.
 * 2. Ask [finder] for the faces (the engine's face-landmark model in the app). No face means
 *    nothing to smooth: the result is [EditFailure.NoFace] and the photo stays as it was.
 * 3. Smooth each face's rectangle with [smoother] (OpenCV's bilateral filter in the app), one
 *    face after another, giving a fully smoothed copy in which everything outside the faces is
 *    untouched.
 * 4. Blend that copy over the original by [strength], pixel by pixel, here in Kotlin:
 *    `out = original + strength x (smoothed - original)` for each colour channel. Strength 0 gives
 *    the original back exactly, strength 1 the fully smoothed copy, and anything between a mix.
 *    Outside the faces smoothed equals original, so those pixels never change.
 * 5. Mark the result opaque (`setHasAlpha(false)`), so Save writes a JPEG.
 *
 * Like every [ToolProcessor] it never throws for a failed edit: running out of memory becomes
 * [EditFailure.OutOfMemory], any other error from the engine [EditFailure.ModelUnavailable], and a
 * cancellation (the person left the screen) is passed on unchanged. The work runs on
 * [dispatcher], a background one by default. The input bitmap is never changed.
 */
class RetouchProcessor(
    private val finder: FaceFinder,
    private val smoother: SkinSmoother,
    private val maxEdgePx: Int = WorkingSize.RETOUCH,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ToolProcessor {

    /**
     * How much of the smoothing to show, from 0 (none: the photo as it was) to 1 (all of it).
     * The panel's slider sets it just before a run; a value outside 0..1 is clamped when used.
     *
     * `@Volatile` makes a value written on the main thread visible at once to the background
     * thread that runs [process].
     */
    @Volatile
    var strength: Float = 1f

    /**
     * Runs the five steps above on [input] and returns the result; [input] is not changed.
     * [onProgress] only ever moves forward, from 0 to 1.
     */
    override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult =
        withContext(dispatcher) {
            try {
                var reported = 0f
                // Passes on a progress value only if it moves the bar forward.
                val report: (Float) -> Unit = { value ->
                    val next = value.coerceIn(0f, 1f)
                    if (next >= reported) {
                        reported = next
                        onProgress(next)
                    }
                }
                // Read once, so a slider moved during the run cannot change it half way.
                val amount = strength.let { if (it.isNaN()) 1f else it.coerceIn(0f, 1f) }
                report(0f)
                val small = WorkingSize.fit(input, maxEdgePx)
                report(FIT_DONE)
                val faces = finder.findFaces(small).mapNotNull { clampTo(it, small.width, small.height) }
                report(FIND_DONE)
                if (faces.isEmpty()) {
                    if (small !== input) small.recycle()
                    return@withContext EditResult.Failed(EditFailure.NoFace)
                }
                ensureActive()
                val smoothed = smoothEveryFace(small, faces) { done ->
                    report(FIND_DONE + (SMOOTH_DONE - FIND_DONE) * done / faces.size)
                }
                ensureActive()
                val result = blend(small, smoothed, amount)
                // The working copies are no longer needed; freeing them at once matters on a phone,
                // where each one can be a 2000-pixel picture (16 MB).
                if (smoothed !== small && smoothed !== input) smoothed.recycle()
                if (small !== input) small.recycle()
                report(1f)
                EditResult.Edited(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (outOfMemory: OutOfMemoryError) {
                EditResult.Failed(EditFailure.OutOfMemory)
            } catch (missing: LinkageError) {
                // A native library or the model's class could not load on this phone.
                EditResult.Failed(EditFailure.ModelUnavailable)
            } catch (failed: Exception) {
                EditResult.Failed(EditFailure.ModelUnavailable)
            }
        }

    /**
     * Smooths each rectangle of [faces] on [photo], one after another, and returns the copy with
     * all of them smoothed. [onFaceDone] is told how many faces are done after each one. [photo]
     * is not changed; the in-between copies are freed as soon as the next one exists.
     */
    private fun smoothEveryFace(photo: Bitmap, faces: List<Rect>, onFaceDone: (Int) -> Unit): Bitmap {
        var current = photo
        faces.forEachIndexed { index, face ->
            val next = smoother.smooth(current, face)
            check(next.width == photo.width && next.height == photo.height) {
                "The smoother returned ${next.width} x ${next.height} for a ${photo.width} x ${photo.height} photo"
            }
            // Free the previous face's copy, but never the photo itself.
            if (current !== photo && current !== next) current.recycle()
            current = next
            onFaceDone(index + 1)
        }
        return current
    }

    /**
     * Returns a new opaque bitmap mixing [original] and [smoothed] (the same size) by [amount]:
     * each colour channel is `original + amount x (smoothed - original)`, rounded. At amount 0
     * every pixel equals [original]'s exactly.
     */
    private fun blend(original: Bitmap, smoothed: Bitmap, amount: Float): Bitmap {
        val width = original.width
        val height = original.height
        val before = IntArray(width * height)
        val after = IntArray(width * height)
        original.getPixels(before, 0, width, 0, 0, width, height)
        smoothed.getPixels(after, 0, width, 0, 0, width, height)
        for (i in before.indices) {
            val o = before[i]
            val s = after[i]
            val r = mix((o shr 16) and CHANNEL, (s shr 16) and CHANNEL, amount)
            val g = mix((o shr 8) and CHANNEL, (s shr 8) and CHANNEL, amount)
            val b = mix(o and CHANNEL, s and CHANNEL, amount)
            // Alpha is forced to fully opaque: a retouched photo has no see-through pixels.
            before[i] = (OPAQUE_ALPHA shl 24) or (r shl 16) or (g shl 8) or b
        }
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(before, 0, width, 0, 0, width, height)
        out.setHasAlpha(false)
        return out
    }

    /** One channel's mix: [from] moved toward [to] by [amount] (0..1), rounded to 0..255. */
    private fun mix(from: Int, to: Int, amount: Float): Int =
        (from + amount * (to - from)).roundToInt().coerceIn(0, CHANNEL)

    /**
     * Returns [face] cut to the [width] x [height] picture, or null when nothing of it lies inside
     * (a finder must not make the processor read outside the photo).
     */
    private fun clampTo(face: Rect, width: Int, height: Int): Rect? {
        val clamped = Rect(
            face.left.coerceIn(0, width),
            face.top.coerceIn(0, height),
            face.right.coerceIn(0, width),
            face.bottom.coerceIn(0, height),
        )
        return if (clamped.right > clamped.left && clamped.bottom > clamped.top) clamped else null
    }

    /** The processor's fixed numbers. */
    private companion object {
        /** The progress once the photo has been scaled to the working size. */
        const val FIT_DONE = 0.05f

        /** The progress once the faces have been found. */
        const val FIND_DONE = 0.4f

        /** The progress once every face has been smoothed; the blend fills the rest. */
        const val SMOOTH_DONE = 0.9f

        /** The largest value of one colour channel, also the mask that reads one from a pixel. */
        const val CHANNEL = 0xFF

        /** The alpha of a fully opaque pixel. */
        const val OPAQUE_ALPHA = 0xFF
    }
}
