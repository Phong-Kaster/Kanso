package com.phongkaster.kanso.data.edit.artistic

import android.graphics.Bitmap

/**
 * Repaints a photo in one of the [ArtisticStyle]s: the style-transfer step of the Artistic tool.
 *
 * It is an interface, not the engine itself, because the engine (two TensorFlow Lite models run
 * natively) cannot load in a JVM test. The app uses [EngineStylizer]; a test hands
 * [ArtisticProcessor] a small fake that recolours the photo in a fixed way per style.
 */
interface Stylizer {

    /**
     * Returns a new copy of [photo] painted fully in [style] (full strength: the Artistic
     * processor mixes it with the original itself). [photo] is not changed. The result is
     * usually the same size as [photo]; the caller scales it if it is not. It throws when the
     * engine fails (a model that will not load, an engine error, or an `OutOfMemoryError`).
     */
    suspend fun stylize(photo: Bitmap, style: ArtisticStyle): Bitmap
}
