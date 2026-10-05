package com.photo.editor.ui.screen.removebg.common

import android.net.Uri
import com.photo.editor.ui.screen.removebg.model.RemoveBackgroundImage
import com.photo.editor.ui.screen.removebg.model.RemoveBackgroundScene

sealed class RemoveBGBackground {
    data class SolidColor(val color: Long) : RemoveBGBackground()
    data class Image(val uri: Uri) : RemoveBGBackground()
    data class Scene(val scene: RemoveBackgroundScene) : RemoveBGBackground()
    data class Background(val background: RemoveBackgroundImage) : RemoveBGBackground()
}