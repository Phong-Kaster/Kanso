package com.phongkaster.kanso.ui.tool

import android.content.Context
import com.phongkaster.kanso.data.edit.edit.DrawableStickerPictures
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut

/**
 * Builds THE recipe that turns Edit settings into the processor that draws them.
 *
 * Several places need to draw the same settings: the cropper, the live filter preview and Done's
 * final render. If each built its processor differently (one without the sticker pictures, say),
 * a sticker added in one place would silently vanish in another. So there is exactly one recipe,
 * here: the filter look-up tables are read from the app's assets and the sticker pictures from
 * its drawables, both through the application context so no screen is held on to.
 *
 * Call it once (remember the result in the screen function) and hand the same function to every
 * user, because the ViewModel recognises a recipe by identity and reuses its caches for it.
 */
fun editProcessorFor(context: Context): (EditSettings) -> EditProcessor {
    val appContext = context.applicationContext
    val filters = FilterLut.fromAssets { path -> appContext.assets.open(path) }
    val stickers = DrawableStickerPictures(appContext)
    return { wanted -> EditProcessor(wanted, filters, stickers) }
}
