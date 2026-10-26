package com.apero.pinity.ui.screen.edit_image.decal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.apero.pinity.R
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalImageOverlay
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalNumericClockOverlay
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalOverlay
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalStickerOverlay
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalTextOverlay
import com.apero.pinity.util.uriToBitmap
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

/** DECAL OVERLAY STATE*/
@Stable
interface DecalOverlayState {
    var overlay: DecalOverlay

    // Below properties are used only for visual overlays
    val bitmap: ImageBitmap
    var centerOffset: IntOffset
    var size: IntSize
    var rotationDegrees: Float
    var canvasSize: IntSize
}

/** DECAL DEFAULT OVERLAY STATE*/
class DecalDefaultOverlayState(
    context: Context,
    overlay: DecalOverlay,
    initialSize: IntSize = IntSize(0, 0),
    inspectionMode: Boolean = false,
): DecalOverlayState {
    private var _overlay: DecalOverlay by mutableStateOf(overlay)
    override var overlay: DecalOverlay
        get() = _overlay
        set(value) {
            _overlay = value
        }

    override var bitmap by mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap())

    private var _centerOffset by mutableStateOf(IntOffset.Zero)
    override var centerOffset: IntOffset
        get() = _centerOffset
        set(value) {
            _centerOffset = value
        }

    private var _size by mutableStateOf(IntSize.Zero)
    override var size: IntSize
        get() = _size
        set(value) {
            // Constraint overlay size
            _size = IntSize(
                width = value.width.coerceIn(bitmap.width / 20, bitmap.width * 5),
                height = value.height.coerceIn(bitmap.height / 20, bitmap.height * 5),
            )
        }

    private var _rotationDegrees by mutableFloatStateOf(0f)
    override var rotationDegrees: Float
        get() = _rotationDegrees
        set(value) {
            _rotationDegrees = value
        }

    override var canvasSize: IntSize = IntSize.Zero
        set(value) {
            val oldValue = field
            field = value

            // Update overlay position if size changed from non-zero
            if (oldValue.width != 0 && oldValue.height != 0) {

                val scaleX = value.width.toFloat() / oldValue.width
                val scaleY = value.height.toFloat() / oldValue.height
                val scale = if (scaleX == 1f) scaleY else scaleX



                val newCenterOffset = IntOffset(
                    x = (centerOffset.x * scale).roundToInt(),
                    y = (centerOffset.y * scale).roundToInt(),
                )



                centerOffset = newCenterOffset

                val newSize = IntSize(
                    width = (size.width * scale).roundToInt(),
                    height = (size.height * scale).roundToInt(),
                )
                size = newSize
            }
        }

    init {
        if (inspectionMode) {
            val randomDrawable = listOf(
                R.drawable.filter_sx7000,
                R.drawable.img_peaches,
            ).random()
            bitmap = BitmapFactory.decodeResource(context.resources, randomDrawable).asImageBitmap()
        } else {
            if (overlay is DecalImageOverlay) {
                bitmap = uriToBitmap(context.contentResolver, overlay.uri).asImageBitmap()
            } else if (overlay is DecalStickerOverlay) {
                bitmap = ImageBitmap.imageResource(context.resources, overlay.drawableId)
                Log.d("TAG", "DecalStickerOverlay - bitmap = ${bitmap.width}x${bitmap.height}")
            }

//            else if (overlay is AudioOverlay) {
//                bitmap = getBitmapFromVectorDrawable(context.resources, R.drawable.ic_music).asImageBitmap()
//            }
        }

        val width = if (initialSize.width != 0) initialSize.width else bitmap.width
        val height = width * bitmap.height / bitmap.width

        size = IntSize(width, height)

        val top = bitmap.height / 2 - bitmap.height / 2
        val left = bitmap.width / 2 - bitmap.width / 2
        centerOffset = IntOffset(left, top)

        Log.d("TAG", "DecalDefaultOverlayState - width x height = ${width}x${height}")
        Log.d("TAG", "DecalDefaultOverlayState - bitmap = ${bitmap.width}x${bitmap.height}")
        Log.d("TAG", "DecalDefaultOverlayState - centerOffset = $centerOffset")
    }
}

/** DECAL TEXT OVERLAY STATE*/
class DecalTextOverlayState(
    val context: Context,
    overlay: DecalTextOverlay,
) : DecalOverlayState {
    private var _overlay: DecalTextOverlay by mutableStateOf(overlay)
    override var overlay: DecalOverlay
        get() = _overlay
        set(value) {
            _overlay = value as DecalTextOverlay
            updateBitmapFromOverlay()
        }

    override var bitmap by mutableStateOf(createBitmap(1, 1).asImageBitmap())
        private set

    private var _centerOffset by mutableStateOf(IntOffset.Zero)
    override var centerOffset: IntOffset
        get() = _centerOffset
        set(value) {
            _centerOffset = value
        }

    private var _size by mutableStateOf(IntSize.Zero)
    override var size: IntSize
        get() = _size
        set(value) {
            // Constraint overlay size
            _size = IntSize(
                width = value.width.coerceIn(bitmap.width / 20, bitmap.width * 5),
                height = value.height.coerceIn(bitmap.height / 20, bitmap.height * 5),
            )
        }

    private var _rotationDegrees by mutableFloatStateOf(0f)
    override var rotationDegrees: Float
        get() = _rotationDegrees
        set(value) {
            _rotationDegrees = value
        }

    override var canvasSize: IntSize = IntSize.Zero
        set(value) {
            val oldValue = field
            field = value

            // Update overlay position if size changed from non-zero
            if (oldValue != IntSize.Zero) {
                val scaleX = value.width.toFloat() / oldValue.width
                val scaleY = value.height.toFloat() / oldValue.height
                val scale = if (scaleX == 1f) scaleY else scaleX

                val newCenterOffset = IntOffset(
                    x = (centerOffset.x * scale).roundToInt(),
                    y = (centerOffset.y * scale).roundToInt(),
                )
                centerOffset = newCenterOffset
                val newSize = IntSize(
                    width = (size.width * scale).roundToInt(),
                    height = (size.height * scale).roundToInt(),
                )
                size = newSize
            }
        }

    init {
        this.bitmap = _overlay.getOverlayBitmap(context).asImageBitmap()
        this._size = IntSize(bitmap.width, bitmap.height)

        val top = size.height / 2 - size.height / 2
        val left = size.width / 2 - size.width / 2

        centerOffset = IntOffset(left, top)
    }

    private fun updateBitmapFromOverlay() {
        // Get old image scale
        val scaleX = size.width.toFloat() / bitmap.width
        val scaleY = size.height.toFloat() / bitmap.height

        this.bitmap = _overlay.getOverlayBitmap(context).asImageBitmap()
        this._size = IntSize((bitmap.width * scaleX).toInt(), (bitmap.height * scaleY).toInt())
    }
}

/** DECAL Numeric Clock STATE*/
class DecalNumericClockOverlayState(
    val context: Context,
    overlay: DecalNumericClockOverlay,
) : DecalOverlayState {
    private var _overlay: DecalNumericClockOverlay by mutableStateOf(overlay)
    override var overlay: DecalOverlay
        get() = _overlay
        set(value) {
            _overlay = value as DecalNumericClockOverlay
            updateBitmapFromOverlay()
        }

    override var bitmap by mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap())
        private set

    private var _centerOffset by mutableStateOf(IntOffset.Zero)
    override var centerOffset: IntOffset
        get() = _centerOffset
        set(value) {
            _centerOffset = value
        }

    private var _size by mutableStateOf(IntSize.Zero)
    override var size: IntSize
        get() = _size
        set(value) {
            // Constraint overlay size
            _size = IntSize(
                width = value.width.coerceIn(bitmap.width / 20, bitmap.width * 5),
                height = value.height.coerceIn(bitmap.height / 20, bitmap.height * 5),
            )
        }

    private var _rotationDegrees by mutableFloatStateOf(0f)
    override var rotationDegrees: Float
        get() = _rotationDegrees
        set(value) {
            _rotationDegrees = value
        }

    override var canvasSize: IntSize = IntSize.Zero
        set(value) {
            val oldValue = field
            field = value

            // Update overlay position if size changed from non-zero
            if (oldValue != IntSize.Zero) {
                val scaleX = value.width.toFloat() / oldValue.width
                val scaleY = value.height.toFloat() / oldValue.height
                val scale = if (scaleX == 1f) scaleY else scaleX

                val newCenterOffset = IntOffset(
                    x = (centerOffset.x * scale).roundToInt(),
                    y = (centerOffset.y * scale).roundToInt(),
                )
                centerOffset = newCenterOffset
                val newSize = IntSize(
                    width = (size.width * scale).roundToInt(),
                    height = (size.height * scale).roundToInt(),
                )
                size = newSize
            }
        }

    init {
        this.bitmap = _overlay.getOverlayBitmap(context).asImageBitmap()
        this._size = IntSize(bitmap.width, bitmap.height)

        val top = size.height / 2 - size.height / 2
        val left = size.width / 2 - size.width / 2

        centerOffset = IntOffset(left, top)
    }

    private fun updateBitmapFromOverlay() {
        // Get old image scale
        val scaleX = size.width.toFloat() / bitmap.width
        val scaleY = size.height.toFloat() / bitmap.height

        this.bitmap = _overlay.getOverlayBitmap(context).asImageBitmap()
        this._size = IntSize((bitmap.width * scaleX).toInt(), (bitmap.height * scaleY).toInt())
    }
}