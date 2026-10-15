package com.phongkaster.kanso.data.edit.artistic

import android.graphics.Bitmap
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
 * The Artistic tool's edit: the photo painted in one of the [ArtisticStyle]s, at a chosen
 * [strength].
 *
 * The steps, in order:
 * 1. Scale the photo down so its longest edge is at most [maxEdgePx] (1024 pixels by default,
 *    [WorkingSize.ARTISTIC]).
 * 2. Paint that copy fully in [style] with [stylizer] (the engine's style-transfer models in the
 *    app). The painted picture is remembered: a second run on the same input bitmap with the
 *    same style (the person only moved the strength slider) skips this slow step entirely.
 * 3. Mix the original and the painted picture pixel by pixel in Kotlin:
 *    `out = original + strength x (painted - original)` for red, green and blue. Strength 0
 *    gives the original, 1 the painted picture, 0.5 halfway between.
 * 4. Mark the result opaque (`setHasAlpha(false)`), so Save writes a JPEG rather than a PNG.
 *
 * Like every [ToolProcessor] it never throws for a failed edit: running out of memory becomes
 * [EditFailure.OutOfMemory], any other engine error [EditFailure.ModelUnavailable]. A cancellation
 * is passed on unchanged. The work runs on [dispatcher], a background one by default.
 */
class ArtisticProcessor(
    private val stylizer: Stylizer,
    private val maxEdgePx: Int = WorkingSize.ARTISTIC,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ToolProcessor {

    /**
     * The style the next run paints in. `@Volatile` makes a change made on the main thread
     * visible at once to the background thread that runs [process].
     */
    @Volatile
    var style: ArtisticStyle = ArtisticStyle.entries.first()

    /** How strongly the next run paints, from 0 (the original) to 1 (fully painted); clamped. */
    @Volatile
    var strength: Float = 1f

    /** The last painted picture and what it was made from, so the strength slider is cheap. */
    private var cache: Painted? = null

    /** Guards [cache], which two runs on two threads could otherwise read and write together. */
    private val cacheLock = Any()

    /**
     * One remembered result of step 2: [painted] is [source] (the working copy of [input])
     * painted in [style], at exactly [source]'s size.
     */
    private class Painted(
        val input: Bitmap,
        val style: ArtisticStyle,
        val source: Bitmap,
        val painted: Bitmap,
    )

    /**
     * Runs the four steps above on [input] and returns a new bitmap; [input] is not changed.
     * [onProgress] only ever moves forward, from 0 to 1.
     */
    override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult =
        withContext(dispatcher) {
            try {
                // Read the settings once, so a change in the middle of a run cannot mix two styles.
                val chosenStyle = style
                val amount = strength.coerceIn(0f, 1f)
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
                val remembered = synchronized(cacheLock) {
                    // `===` compares identity: the very same bitmap object, not equal pixels.
                    cache?.takeIf { it.input === input && it.style == chosenStyle }
                }
                val painted = remembered ?: paint(input, chosenStyle).also { fresh ->
                    synchronized(cacheLock) { cache = fresh }
                }
                report(STYLIZE_SHARE)
                // Stop here if the person left while the engine ran.
                ensureActive()
                val mixed = mix(painted.source, painted.painted, amount)
                report(1f)
                EditResult.Edited(mixed)
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
     * Steps 1 and 2: scales [input] to the working size and has [stylizer] paint it in
     * [chosenStyle]. If the engine answers at another size, the painted picture is scaled to the
     * working copy's size, so the two can be mixed pixel for pixel.
     */
    private suspend fun paint(input: Bitmap, chosenStyle: ArtisticStyle): Painted {
        val source = WorkingSize.fit(input, maxEdgePx)
        val styled = stylizer.stylize(source, chosenStyle)
        val sized = if (styled.width == source.width && styled.height == source.height) {
            styled
        } else {
            Bitmap.createScaledBitmap(styled, source.width, source.height, true)
        }
        return Painted(input, chosenStyle, source, sized)
    }

    /**
     * Step 3 and 4: returns a new opaque bitmap, each pixel `original + amount x (painted -
     * original)` per colour channel, rounded. [original] and [painted] have the same size and
     * neither is changed.
     */
    private fun mix(original: Bitmap, painted: Bitmap, amount: Float): Bitmap {
        val width = original.width
        val height = original.height
        val from = IntArray(width * height)
        val to = IntArray(width * height)
        original.getPixels(from, 0, width, 0, 0, width, height)
        painted.getPixels(to, 0, width, 0, 0, width, height)
        for (index in from.indices) {
            val a = from[index]
            val b = to[index]
            // A pixel is packed as 0xAARRGGBB: shift and mask pick out one 0..255 channel.
            val red = blend((a shr 16) and 0xFF, (b shr 16) and 0xFF, amount)
            val green = blend((a shr 8) and 0xFF, (b shr 8) and 0xFF, amount)
            val blue = blend(a and 0xFF, b and 0xFF, amount)
            // Alpha is always full (0xFF): the result is opaque.
            from[index] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(from, 0, width, 0, 0, width, height)
        result.setHasAlpha(false)
        return result
    }

    /** Returns [from] moved [amount] of the way toward [to], rounded and kept in 0..255. */
    private fun blend(from: Int, to: Int, amount: Float): Int =
        (from + amount * (to - from)).roundToInt().coerceIn(0, 255)

    /** The processor's fixed numbers. */
    private companion object {
        /** The part of the progress bar the painting step fills; the mixing fills the rest. */
        const val STYLIZE_SHARE = 0.9f
    }
}
