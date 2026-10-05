package com.photo.editor.ui.activity

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.photo.editor.common.util.tryWithoutCatch


fun Activity?.setLightStatusBar(isLight: Boolean) = tryWithoutCatch {
    if (this == null) return
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = isLight
    }
}

fun Fragment.setLightStatusBar(isLight: Boolean) = tryWithoutCatch {
    activity.setLightStatusBar(isLight)
}

/**
 * A utility function that recursively searches for and returns the [Activity] instance associated with the current [Context].
 *
 * @return The [Activity] instance associated with the current [Context].
 * @throws IllegalStateException if the current [Context] is not an [Activity] context.
 */
tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> this.baseContext.findActivity()
    else -> throw IllegalStateException(
        "Context is not an Activity context, but a ${javaClass.simpleName} context. "
    )
}