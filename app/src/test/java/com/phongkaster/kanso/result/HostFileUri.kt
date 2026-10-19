package com.phongkaster.kanso.result

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Run 9: names a cached file the way the app's FileProvider does,
 * `content://<package>.fileprovider/shared/<file name>`, without asking FileProvider.
 *
 * FileProvider compares file paths with a forward slash, so on a Windows desktop JVM it cannot
 * find the cache folder and throws. Tests hand this to `shareableBitmapUri` and `ResultScreen`
 * instead; the real provider's declaration is checked in `SharePhotoTest` by reading the manifest.
 */
val HOST_URI: (Context, File) -> Uri = { context, file ->
    Uri.parse("content://${context.packageName}.fileprovider/shared/${file.name}")
}
