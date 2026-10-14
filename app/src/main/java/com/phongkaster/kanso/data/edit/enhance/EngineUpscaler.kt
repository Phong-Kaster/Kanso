package com.phongkaster.kanso.data.edit.enhance

import android.content.Context
import android.graphics.Bitmap
import com.photo.editor.localml.enhance.EnhanceImageUC
import com.photo.editor.localml.enhance.ProgressStatus
import kotlinx.coroutines.flow.first

/**
 * The real [Upscaler]: the engine's Real-ESRGAN model (`EnhanceImageUC` in `core/localml`), which
 * makes a photo four times wider and taller. The model file is bundled in the app
 * (`assets/realesrgan_64x64_float16.tflite`), so nothing is downloaded. This is the only file of
 * the app that names the engine's classes, so the rest of Enhance can be tested without them.
 *
 * [createEngine] makes a fresh engine object for each run. The engine keeps a TensorFlow Lite
 * interpreter of its own, and making one per run means nothing is left loaded between runs.
 */
class EngineUpscaler(private val createEngine: () -> EnhanceImageUC) : Upscaler {

    /**
     * Builds the upscaler on [context]. The application context is used, so the engine never
     * holds on to a screen that has closed.
     */
    constructor(context: Context) : this({ EnhanceImageUC(context.applicationContext) })

    /**
     * Runs the engine on [photo] and returns its four-times-larger result.
     *
     * The engine answers with a Flow, a stream of values arriving over time: first several
     * `Processing(progress)` values, then one `Success(result)` or one `Error(message)`. This
     * collects the stream only until the first final answer (`first { }` stops collecting as
     * soon as its test is true), passing each progress on to [onProgress].
     */
    override suspend fun upscale(photo: Bitmap, onProgress: (Float) -> Unit): Bitmap {
        val engine = createEngine()
        // initModel() loads the model in the background and returns its Job; waiting for it
        // (join) means the model is ready before the first window of the photo is processed.
        // Retica calls it once before invoking the engine too.
        engine.initModel()?.join()
        var result: Bitmap? = null
        engine.invoke(photo).first { status ->
            when (status) {
                is ProgressStatus.Processing -> {
                    onProgress(status.progress.coerceIn(0f, 1f))
                    false
                }
                is ProgressStatus.Success -> {
                    result = status.result
                    true
                }
                is ProgressStatus.Error -> throw IllegalStateException("Enhance engine: ${status.message}")
                // Any other status the engine may add later is not a final answer: keep waiting.
                else -> false
            }
        }
        return result ?: throw IllegalStateException("Enhance engine finished without a picture")
    }
}
