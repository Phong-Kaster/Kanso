package com.photo.editor.common.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Size
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.core.graphics.scale

fun Bitmap.size(): Size = Size(this.width, this.height)
fun ImageBitmap.size(): Size = Size(this.width, this.height)
fun Bitmap.composeSize(): ComposeSize = ComposeSize(this.width.toFloat(), this.height.toFloat())
fun Bitmap.center(): IntOffset = IntOffset(this.width / 2, this.height / 2)

fun resourceToBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
    return BitmapFactory.decodeResource(context.resources, resId)
}

fun assetToBitmap(context: Context, assetName: String): Bitmap? {
    return tryOrNull { context.assets.open(assetName).use { BitmapFactory.decodeStream(it) } }
}

fun uriToBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap {
    var bitmap = contentResolver.openInputStream(uri).use { inStream ->
        BitmapFactory.decodeStream(inStream)
    }

    // Get orientation from exif data and rotate image if orientation is not 0
    bitmap = contentResolver.openInputStream(uri)?.use { inStream ->
        val rotationDegrees = ExifInterface(inStream).rotationDegrees
        if (rotationDegrees == 0) bitmap
        else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }, false)
    } ?: bitmap

    return bitmap
}

fun scaleDownBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    return if (width > maxSize || height > maxSize) {
        val ratio = width.toFloat() / height
        // Scale down so that both dimensions are less than 2000
        val newWidth = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
        val newHeight = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
        bitmap.scale(newWidth, newHeight)
    } else bitmap
}

/** Get the center bounds of the bitmap based on the aspect ratio */
fun getCenterBounds(bitmap: Bitmap, aspectRatio: Float): IntRect {
    val currentAspect = bitmap.width.toFloat() / bitmap.height

    // If current AR > crop AR, cut the width, otherwise cut the height
    val roi = if (currentAspect > aspectRatio) {
        val newWidth = (aspectRatio * bitmap.height).roundToInt()
        val offSet = (bitmap.width - newWidth) / 2
        IntRect(offSet, 0, offSet + newWidth, bitmap.height)
    } else {
        val newHeight = (bitmap.width / aspectRatio).roundToInt()
        val offset = (bitmap.height - newHeight) / 2
        IntRect(0, offset, bitmap.width, offset + newHeight)
    }

    return roi
}

fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()
    val file = File(cachePath, "shared_image.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}