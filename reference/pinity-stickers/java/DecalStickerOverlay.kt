package com.apero.pinity.ui.screen.edit_image.decal.model

import androidx.annotation.DrawableRes
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalOverlay

data class DecalStickerOverlay(
    @DrawableRes val drawableId: Int,
    override val id: Long = 0,
//    override val id: Long = 0,
//    override val startTimeMs: Long = 0,
//    override val endTimeMs: Long = 5000,
) : DecalOverlay {
//    override fun copyWithTimestamp(
//        startTimeMs: Long,
//        endTimeMs: Long,
//    ): Overlay {
//        return copy(
//            startTimeMs = startTimeMs,
//            endTimeMs = endTimeMs,
//        )
//    }
}
