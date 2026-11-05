package com.phongkaster.kanso.data.edit.edit

import android.graphics.Bitmap
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Builds the filter part of the Edit screen's live preview: a small copy of the original photo
 * with only the chosen filter applied.
 *
 * **How the live preview is put together.** The preview splits an edit into two kinds of work:
 *
 * - The crop, the turns, the flips and the four colour sliders are cheap for the GPU, so Compose
 *   draws them itself every frame (the sliders as a `ColorFilter.colorMatrix` built by
 *   [EditColourMatrix.of], the same numbers Done uses). That is why the photo changes while a
 *   finger is still dragging a slider: no pixel is recomputed on the CPU.
 * - A filter is a look-up per pixel that Compose cannot draw, so this class computes it on the
 *   CPU, off the main thread, once per filter or intensity change, by running the real Done
 *   processor with only the filter set. Its result is the picture Compose then crops, turns, flips
 *   and colours.
 *
 * **Always the original with every current setting.** [source] is the ORIGINAL photo (only made
 * smaller), never a picture an earlier preview produced, and each call to [render] receives the
 * session's complete [EditSettings]. So nothing is stacked or baked in as the person edits:
 * choosing a second filter replaces the first instead of filtering an already-filtered picture,
 * an intensity of 0.4 after 0.8 means 0.4 of the original, and the preview is exactly what Done
 * would render from the same settings. Building on the last preview instead would compound every
 * change and drift further from Done with each one.
 *
 * [source] is the original already shrunk so its longest edge is at most [PREVIEW_EDGE] pixels
 * (the ViewModel uses `WorkingSize.fit(original, PREVIEW_EDGE)`), which keeps a filter change
 * fast on the phone. [processorFor] makes the processor for a set of settings: the app passes
 * one that builds an [EditProcessor] with the real filter look-ups, a test a fake.
 *
 * [source] is never modified, so the same original serves every render.
 */
class FilterPreviewRenderer(
    private val source: Bitmap,
    private val processorFor: (EditSettings) -> ToolProcessor,
) {

    /**
     * Returns [source] with the layers of [settings] drawn on it, then the filter of
     * [settings] at its intensity applied, then its HSL values, as a new bitmap the same size as
     * [source], or null when [settings] has no filter, no HSL change and no sticker to draw (the
     * preview then shows [source] itself) or when the work failed (a look-up image that cannot be read, or no memory left).
     *
     * The sticker whose key is [touchedLayerKey] (the one under a finger) is not drawn: the
     * screen draws it live, so it joins this picture only when the finger lifts.
     *
     * Everything else in [settings] (crop, turns, flips, colour sliders, RGB) is deliberately left out:
     * Compose draws those on top, so they must not be applied twice.
     *
     * The work runs on a background thread. A cancelled call (a newer setting replaced it) stops
     * with a CancellationException, as coroutines expect, rather than returning null.
     */
    suspend fun render(settings: EditSettings, touchedLayerKey: Long? = null): Bitmap? {
        // The sticker under a finger is left out: the screen draws it live on top instead.
        val layers = settings.layers.filter { it.key != touchedLayerKey }
        // Nothing to bake in: no filter and no sticker, so the preview shows [source] itself.
        if (!settings.hasFilter && !settings.hasHslChange && layers.isEmpty()) return null
        // Only the filter, its strength, the HSL values and the layers: every other setting stays
        // at its neutral default. The processor draws layers BEFORE the filter (as Done does), so a
        // sticker under a filter comes out filtered, and the whole photo is kept (no crop), so
        // the layers keep their place as fractions of the original. HSL runs after the filter
        // inside the processor, so the copy holds filter then HSL, and Compose puts the colour
        // matrix on top: the same order as Done.
        val filterOnly = EditSettings(
            filter = settings.filter,
            filterIntensity = settings.filterIntensity,
            layers = layers,
            hsl = settings.hsl,
        )
        return withContext(Dispatchers.Default) {
            try {
                val result = processorFor(filterOnly).process(source) {}
                if (result is EditResult.Edited) matchSourceSize(result.bitmap) else null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (outOfMemory: OutOfMemoryError) {
                null
            } catch (other: Exception) {
                null
            }
        }
    }

    /**
     * Returns [bitmap] at exactly the size of [source]. With no crop and no turn, [EditProcessor]
     * keeps the size of any photo that fits within `WorkingSize.EDIT` (3000 px), which a preview
     * source of at most [PREVIEW_EDGE] always does, so this normally returns [bitmap] untouched.
     * Should a larger source ever be passed in, the processor would shrink it; scaling the result
     * back keeps the preview the size Compose expects.
     */
    private fun matchSourceSize(bitmap: Bitmap): Bitmap {
        if (bitmap.width == source.width && bitmap.height == source.height) return bitmap
        // filter = true smooths the scaling instead of picking the nearest pixel.
        return Bitmap.createScaledBitmap(bitmap, source.width, source.height, true)
    }

    /** The size limit of the preview's source photo. */
    companion object {

        /**
         * The longest edge, in pixels, of the original photo the preview works on: large enough
         * to look sharp on a phone screen, small enough that a filter is applied in a moment even
         * on a 32-bit phone.
         */
        const val PREVIEW_EDGE = 640
    }
}
