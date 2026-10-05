@file:Suppress("FunctionName")

package com.photo.editor.common.ui.transformableimage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap

@Stable
interface ImageState {
    var image: ImageBitmap
    var contentScale: Float
    val transformState: TransformState
}

fun ImageState(
    initialImage: ImageBitmap = createBitmap(1, 1).asImageBitmap(),
): ImageState = object : ImageState {
    private var _image by mutableStateOf(initialImage)
    override var image: ImageBitmap
        get() = _image
        set(value) {
            _image = value
            updateBounds()
        }

    private var _contentScale: Float by mutableFloatStateOf(1f)
    override var contentScale: Float
        get() = _contentScale
        set(value) {
            _contentScale = value
        }

    override val transformState = TransformState()

    init {
        updateBounds()
    }

    private fun updateBounds() {
        transformState.surfaceSize = Size(image.width.toFloat(), image.height.toFloat())
        transformState.scaleAndTranslate(1f, 0f, 0f)
    }

}

@Composable
fun rememberTransformableState(
    image: ImageBitmap,
): ImageState = remember {
    ImageState(image)
}