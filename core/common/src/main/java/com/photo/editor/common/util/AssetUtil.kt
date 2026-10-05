package com.photo.editor.common.util

import android.content.Context
import java.io.IOException


fun getAssetAbsoluteFilePath(assetPath: String): String {
    return "file:///android_asset/$assetPath"
}

fun listAssetFiles(context: Context, path: String): List<String> {
    val result = mutableListOf<String>()

    try {
        val assetList = context.assets.list(path)?.toList() ?: listOf()
        if (assetList.isNotEmpty()) {
            // This is a folder
            for (file in assetList) {
                // Add all sub files in the folder if it's a folder
                // Otherwise add the file
                val subFiles = listAssetFiles(context,"$path/$file")
                if (subFiles.isNotEmpty()) result.addAll(subFiles)
                else result.add("$path/$file")
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return result
}