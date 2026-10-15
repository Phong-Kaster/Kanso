package com.phongkaster.kanso.data.edit.enhance

import android.graphics.Bitmap
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.WorkingSize
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * The Enhance tool's edit: a larger and better-lit copy of the photo.
 *
 * The steps, in order:
 * 1. Scale the photo down so its longest edge is at most [maxEdgePx] (512 pixels by default,
 *    [WorkingSize.ENHANCE]). The upscaler makes the photo four times wider and taller, so a
 *    4000 x 3000 photo would ask for about 768 MB; 512 pixels become a 2048-pixel result.
 * 2. Upscale it with [upscaler] (the engine's Real-ESRGAN model in the app). Its progress fills
 *    the first nine tenths of the bar.
 * 3. Brighten it with [AutoLighting] when it is dark.
 * 4. Mark it opaque (`setHasAlpha(false)`), so Save writes a JPEG rather than a PNG.
 *
 * Like every [ToolProcessor] it never throws for a failed edit: running out of memory becomes
 * [EditFailure.OutOfMemory], and any other engine error (a model that will not load, an engine
 * error message) becomes [EditFailure.ModelUnavailable]. A cancellation (the person left the
 * screen) is passed on unchanged. The work runs on [dispatcher], a background one by default.
 */
class EnhanceProcessor(
    private val upscaler: Upscaler,
    private val maxEdgePx: Int = WorkingSize.ENHANCE,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ToolProcessor {

    /**
     * Runs the four steps above on [input] and returns the result; [input] is not changed.
     * [onProgress] only ever moves forward, from 0 to 1, even if the engine's own numbers jump
     * back.
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
                report(0f)
                val small = WorkingSize.fit(input, maxEdgePx)
                val upscaled = upscaler.upscale(small) { progress ->
                    report(progress.coerceIn(0f, 1f) * UPSCALE_SHARE)
                }
                report(UPSCALE_SHARE)
                // Stop here if the person left while the engine ran.
                ensureActive()
                val lit = AutoLighting.apply(upscaled)
                // The upscaled copy is no longer needed once the lighting made a new one; freeing
                // it at once matters on a phone, where it can be a 2048-pixel picture (16 MB).
                if (lit !== upscaled && upscaled !== small && upscaled !== input) upscaled.recycle()
                lit.setHasAlpha(false)
                report(1f)
                EditResult.Edited(lit)
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

    /** The processor's fixed numbers. */
    private companion object {
        /** The part of the progress bar the upscaling step fills; the lighting step fills the rest. */
        const val UPSCALE_SHARE = 0.9f
    }
}
