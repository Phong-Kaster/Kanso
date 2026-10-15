package com.phongkaster.kanso.data.edit.cutout

import android.graphics.Bitmap
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Remove background's edit: finds the subject with [segmenter] and returns the photo with
 * everything else made fully transparent, ready to be saved as a PNG.
 *
 * The steps, in order, all on [dispatcher] (a background thread pool, so the screen never waits):
 * 1. Shrink the photo, if it is larger, so its longest edge is at most
 *    [WorkingSize.REMOVE_BACKGROUND] pixels. The ViewModel already does this, but the processor
 *    does it again itself so the engine can never be handed a huge photo, whoever calls it.
 * 2. Ask [segmenter] for the per-pixel foreground probability (progress 0.1 before, 0.8 after).
 * 3. Cut the subject out with [MaskCompositor] (progress 1.0 when done).
 *
 * Failures come back as values, never as exceptions (the [ToolProcessor] contract): no mask, or a
 * mask with no subject in it, is [EditFailure.NoSubject]; running out of memory is
 * [EditFailure.OutOfMemory]; any other exception or a native library that will not link is
 * [EditFailure.ModelUnavailable]. A `CancellationException` is passed on, because it means the
 * person left the screen, not that the edit failed.
 */
class CutoutProcessor(
    private val segmenter: Segmenter,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ToolProcessor {

    /**
     * Cuts the subject out of [input] (see the class description) and reports progress through
     * [onProgress]. The result is at most [WorkingSize.REMOVE_BACKGROUND] pixels on its longest
     * edge and has an alpha channel.
     */
    override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult =
        withContext(dispatcher) {
            try {
                onProgress(PROGRESS_STARTED)
                val working = WorkingSize.fit(input, WorkingSize.REMOVE_BACKGROUND)
                val mask = segmenter.segment(working)
                    ?: return@withContext EditResult.Failed(EditFailure.NoSubject)
                // A mask of the wrong size is a broken engine result, not an empty photo.
                check(mask.size == working.width * working.height) {
                    "The segmenter returned ${mask.size} values for ${working.width}x${working.height}"
                }
                if (!MaskCompositor.hasSubject(mask)) {
                    return@withContext EditResult.Failed(EditFailure.NoSubject)
                }
                onProgress(PROGRESS_SEGMENTED)
                val cutout = MaskCompositor.composite(working, mask)
                onProgress(PROGRESS_DONE)
                EditResult.Edited(cutout)
            } catch (cancelled: CancellationException) {
                // Caught first: CancellationException is itself an IllegalStateException, and it
                // must stop the coroutine rather than become a failure.
                throw cancelled
            } catch (outOfMemory: OutOfMemoryError) {
                EditResult.Failed(EditFailure.OutOfMemory)
            } catch (notLinked: LinkageError) {
                // A native library of the engine that is missing or will not load on this phone.
                EditResult.Failed(EditFailure.ModelUnavailable)
            } catch (other: Exception) {
                EditResult.Failed(EditFailure.ModelUnavailable)
            }
        }

    /** The progress values this processor reports, in the order it reports them. */
    private companion object {
        /** Reported before the segmenter runs. */
        const val PROGRESS_STARTED = 0.1f

        /** Reported once the segmenter has returned a mask with a subject in it. */
        const val PROGRESS_SEGMENTED = 0.8f

        /** Reported when the cut-out picture is ready. */
        const val PROGRESS_DONE = 1f
    }
}
