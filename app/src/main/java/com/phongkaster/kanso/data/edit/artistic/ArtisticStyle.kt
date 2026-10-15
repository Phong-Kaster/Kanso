package com.phongkaster.kanso.data.edit.artistic

import androidx.annotation.StringRes
import com.phongkaster.kanso.R

/**
 * The painterly styles the Artistic tool offers.
 *
 * A style is described to the engine by a picture, not by words: the style-transfer model looks
 * at a small "style image" (its colours, its brush marks, its patterns) and repaints the photo
 * with them. Each style therefore has a style image, kept in the app's assets at [assetPath]
 * and drawn in code by [StyleImages] when the asset is missing.
 *
 * [id] is a stable short name used in file names and test tags; [nameRes] is the name the
 * person reads on the style's chip.
 */
enum class ArtisticStyle(val id: String, @StringRes val nameRes: Int) {

    /** Soft washes of orange, red and gold, like a warm watercolour. */
    WARM_WASH("warm_wash", R.string.artistic_style_warm_wash),

    /** Soft washes of blue, teal and violet, like a cool watercolour. */
    COOL_WASH("cool_wash", R.string.artistic_style_cool_wash),

    /** Small bright tiles with dark grout between them, like a mosaic. */
    MOSAIC("mosaic", R.string.artistic_style_mosaic),

    /** Dark brush strokes on pale paper, like an ink drawing. */
    INK("ink", R.string.artistic_style_ink);

    /**
     * Where this style's image lives inside the app's assets (`app/src/main/assets/`), for
     * example `styles/ink.webp`. Assets are files packed into the APK as they are, read with
     * `context.assets.open(path)`.
     */
    val assetPath: String
        get() = "styles/$id.webp"
}
