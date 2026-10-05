package com.photo.editor.domain.usecase

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.content.FileProvider
import com.photo.editor.BuildConfig
import com.photo.editor.R
import com.photo.editor.domain.model.ShareTarget
import org.koin.core.annotation.Factory
import java.io.File

@Factory
class ShareImageUC {

    operator fun invoke(
        context: Context,
        image: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        shareTarget: ShareTarget? = null,
        onFailure: (Exception) -> Unit = {}
    ) {
        try {
            // Save QR Code image into cache file
            val directory = File(context.cacheDir, "cache")
            if (!directory.exists()) directory.mkdirs()
            val file = File.createTempFile("${context.getString(R.string.app_name)}_", ".jpg", directory)
            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)

            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                image.compress(format, 100, outStream)
            }

            val shareIntent = IntentBuilder(context)
                .setStream(uri)
                .getIntent()
                .setAction(Intent.ACTION_SEND)
                .setDataAndType(uri, "image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (shareTarget?.packageId != null) {
                if (context.packageManager.getLaunchIntentForPackage(shareTarget.packageId) == null) {
                    onFailure(Exception(context.getString(R.string.error_app_not_installed)))
                    return
                } else {
                    shareIntent.setPackage(shareTarget.packageId)
                }
            }

            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_with)))
        } catch (ex: Exception) {
            onFailure(ex)
        }
    }

    operator fun invoke(
        context: Context,
        imageUri: Uri,
        shareTarget: ShareTarget? = null,
        onFailure: (Exception) -> Unit = {},
    ) {
        try {
            val shareIntent = IntentBuilder(context)
                .setStream(imageUri)
                .getIntent()
                .setAction(Intent.ACTION_SEND)
                .setDataAndType(imageUri, "image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (shareTarget?.packageId != null) {
                if (context.packageManager.getLaunchIntentForPackage(shareTarget.packageId) == null) {
                    onFailure(Exception(context.getString(R.string.error_app_not_installed)))
                    return
                } else {
                    shareIntent.setPackage(shareTarget.packageId)
                }
            }

            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_with)))
        } catch (ex: Exception) {
            onFailure(ex)
        }
    }
}