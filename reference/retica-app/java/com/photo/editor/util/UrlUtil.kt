package com.photo.editor.util

import android.content.Context
import android.content.Intent
import com.photo.editor.R
import com.photo.editor.common.Constants
import androidx.core.net.toUri
import com.photo.editor.common.util.tryWithoutCatch

object UrlUtil {
    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = url.toUri()
        try {
            context.startActivity(intent, null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    fun shareApp(context: Context) = tryWithoutCatch {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, Constants.STORE_LISTING_URL)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_with)))
    }
}