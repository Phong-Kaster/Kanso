package com.photo.editor.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photo.editor.R
import org.koin.core.annotation.Factory

@Factory
class ExportImageUC(
    private val context: Context,
) {
    suspend operator fun invoke(
        image: Bitmap,
        outputUri: Uri,
        format: Bitmap.CompressFormat,
        onSuccess: (Uri) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        context.contentResolver
            .openOutputStream(outputUri)
            ?.use {
                image.compress(format, 100, it)
                onSuccess(outputUri)
            } ?: run {
            onFailure(context.getString(R.string.unexpected_error))
        }
    }

    suspend operator fun invoke(
        image: Bitmap,
        format: Bitmap.CompressFormat,
        onSuccess: (Uri) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        try {
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val extension = if (format == Bitmap.CompressFormat.JPEG) ".jpg" else ".png"
            val newImageDetails = ContentValues().apply {
                val systemTimeMillis = System.currentTimeMillis()
                val fileName = context
                    .getString(R.string.app_name)
                    .replace(" ", "") + "_" + systemTimeMillis + extension
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.DATE_MODIFIED, systemTimeMillis)
            }

            // Keep a handle to the new image's URI in case you need to modify it later.
            val newImageUri = context.contentResolver.insert(imageCollection, newImageDetails)
            if (newImageUri == null) {
                onFailure(context.getString(R.string.unexpected_error))
                return
            }

            invoke(image, newImageUri, format, onSuccess, onFailure)

            context.contentResolver.update(newImageUri, newImageDetails, null, null)
        } catch (ex: Exception) {
            ex.printStackTrace()
            onFailure(context.getString(R.string.unexpected_error))
            return
        }
    }
}