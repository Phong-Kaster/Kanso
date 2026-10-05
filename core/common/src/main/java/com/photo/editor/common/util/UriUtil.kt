package com.photo.editor.common.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri

fun getUriForDrawableResource(context: Context, drawableResId: Int): Uri {
    return "android.resource://${context.packageName}/$drawableResId".toUri()
}

fun getUriForDrawableResourceByName(context: Context, drawableName: String): Uri {
    return "android.resource://${context.packageName}/drawable/$drawableName".toUri()
}
