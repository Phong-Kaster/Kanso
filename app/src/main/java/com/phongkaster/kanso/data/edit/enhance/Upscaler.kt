package com.phongkaster.kanso.data.edit.enhance

import android.graphics.Bitmap

/**
 * Makes a photo larger while keeping it sharp: the "super-resolution" step of the Enhance tool.
 *
 * It is an interface, not the engine itself, because the engine (a Real-ESRGAN model run by
 * TensorFlow Lite) is native code that cannot load in a JVM test. The app uses [EngineUpscaler];
 * a test hands [EnhanceProcessor] a small fake that, for example, simply doubles the size.
 */
interface Upscaler {

    /**
     * Returns a new, larger copy of [photo]; [photo] itself is not changed. [onProgress] is called
     * with values from 0 to 1 as the work goes on. It throws when the engine fails (a model that
     * will not load, an engine error, or an `OutOfMemoryError`); the caller turns that into a
     * failure the screen can show.
     */
    suspend fun upscale(photo: Bitmap, onProgress: (Float) -> Unit): Bitmap
}
