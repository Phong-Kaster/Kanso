package com.photo.editor.common.ui.dialog

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun getActivityWindow(): Window? = LocalView.current.context.getActivityWindow()

private tailrec fun Context.getActivityWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.getActivityWindow()
    else -> null
}

@Composable
fun BaseFullscreenDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = true,
                decorFitsSystemWindows = false,
            )
        ) {
            // Make the dialog fullscreen and drawn over navigation bar
            val parentView = LocalView.current.parent as View
            val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
            val activityWindow = getActivityWindow()
            SideEffect {
                dialogWindowProvider?.window?.let {
                    it.setDimAmount(0f)
                    // Hide navigation bar
                    val controller = WindowInsetsControllerCompat(it, it.decorView)
                    controller.hide(WindowInsetsCompat.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                    // Make dialog fullscreen
                    val attributes = WindowManager.LayoutParams()
                    attributes.copyFrom(activityWindow!!.attributes)
                    attributes.type = it.attributes.type
                    it.attributes = attributes
                    parentView.layoutParams = FrameLayout.LayoutParams(activityWindow.decorView.width, activityWindow.decorView.height)

                    it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                }
            }
            // -------------------------------------------------------

            content()
        }
    }
}