package com.phongkaster.kanso.tool.artistic

import android.graphics.Bitmap
import com.phongkaster.kanso.data.edit.artistic.ArtisticStyle
import com.phongkaster.kanso.data.edit.artistic.Stylizer

/** A stand-in for an engine that fails: it throws [failure] as soon as it is called. */
class ThrowingStylizer(private val failure: Throwable) : Stylizer {

    /** Throws [failure]. */
    override suspend fun stylize(photo: Bitmap, style: ArtisticStyle): Bitmap = throw failure
}
