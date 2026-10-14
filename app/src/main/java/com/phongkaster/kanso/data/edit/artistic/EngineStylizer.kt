package com.phongkaster.kanso.data.edit.artistic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.photo.editor.localml.styletransfer.StylePredictInceptionV3UC
import com.photo.editor.localml.styletransfer.StyleTransferInceptionV3BlendUC
import java.io.IOException
import kotlinx.coroutines.coroutineScope

/**
 * The real [Stylizer]: the engine's Inception v3 style-transfer models (`core/localml`).
 *
 * Style transfer runs in two steps, one model each, both bundled in the app's assets so nothing
 * is downloaded (`image_stylization_inceptionv3_dr_predict.tflite` and
 * `image_stylization_inceptionv3_dr_transfer.tflite`):
 * 1. the "predict" model (`StylePredictInceptionV3UC`) looks at the style image and sums up its
 *    style as a short list of numbers;
 * 2. the "transfer" model (`StyleTransferInceptionV3BlendUC`) repaints the photo with that style.
 *
 * This is the only file of the app that names those engine classes, so the rest of Artistic can
 * be tested without them.
 *
 * [createEngine] makes a fresh engine object for each run, so no model stays loaded between
 * runs. [loadStyleImage] returns the picture of a style (the app's one, [readStyleImage], reads
 * it from the assets, or draws it with [StyleImages]).
 */
class EngineStylizer(
    private val createEngine: () -> StyleTransferInceptionV3BlendUC,
    private val loadStyleImage: (ArtisticStyle) -> Bitmap,
) : Stylizer {

    /**
     * Builds the stylizer on [context]. The application context is used, so the engine never
     * holds on to a screen that has closed.
     */
    constructor(context: Context) : this(
        createEngine = {
            val app = context.applicationContext
            StyleTransferInceptionV3BlendUC(app, StylePredictInceptionV3UC(app))
        },
        loadStyleImage = { style -> readStyleImage(context.applicationContext, style) },
    )

    /**
     * Loads the models, then paints [photo] fully in [style] and returns the new picture.
     */
    override suspend fun stylize(photo: Bitmap, style: ArtisticStyle): Bitmap {
        val engine = createEngine()
        // initModel() starts loading the models in the coroutine scope it is given and returns
        // the Job doing it; waiting for it (join) means the models are ready before invoke.
        // coroutineScope { } gives it a scope that lives exactly as long as this call.
        coroutineScope { engine.initModel(this)?.join() }
        val styleImage = loadStyleImage(style)
        // The engine gets its own copy: it is not known whether it changes its input, and `photo`
        // may be the very bitmap the screen shows.
        return engine.invoke(photo.copy(Bitmap.Config.ARGB_8888, false), styleImage, FULL_STYLE_RATIO)
    }

    /** The stylizer's fixed number and the style-image loader. */
    companion object {

        /**
         * The blend ratio handed to the engine, meant as "the photo painted fully in the style";
         * the Artistic processor makes every weaker strength itself by mixing in Kotlin.
         *
         * Unverified: the engine's class files do not say which end of the ratio is full style
         * (1 might instead mean "all original"). It is named once, here, so the device run
         * (DoD 13) can confirm it or flip it in one place.
         */
        const val FULL_STYLE_RATIO = 1f

        /**
         * Returns [style]'s picture from the app's assets ([ArtisticStyle.assetPath]), or, when
         * that file is missing or cannot be decoded, the same picture drawn by [StyleImages].
         */
        fun readStyleImage(context: Context, style: ArtisticStyle): Bitmap {
            val decoded = try {
                context.assets.open(style.assetPath).use { stream -> BitmapFactory.decodeStream(stream) }
            } catch (missing: IOException) {
                null
            }
            return decoded ?: StyleImages.draw(style)
        }
    }
}
