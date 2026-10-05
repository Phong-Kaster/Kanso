package com.photo.editor.common.util

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import androidx.core.content.ContextCompat

fun getStoragePermissions(): List<String> {
    val permissions = mutableListOf<String>()
    if (SDK_INT < Build.VERSION_CODES.R) permissions.add(WRITE_EXTERNAL_STORAGE)
    if (SDK_INT < TIRAMISU) permissions.add(READ_EXTERNAL_STORAGE)

    if (SDK_INT >= TIRAMISU) {
        permissions.add(READ_MEDIA_IMAGES)
        if (SDK_INT >= UPSIDE_DOWN_CAKE) {
            permissions.add(READ_MEDIA_VISUAL_USER_SELECTED)
        }
    }

    return permissions
}

fun Context.checkPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Context.checkPermissions(permissions: List<String>): Boolean {
    return permissions.all { checkPermission(it) }
}

fun checkCameraPermission(context: Context): Boolean = context.checkPermission(Manifest.permission.CAMERA)

fun checkStoragePermissions(context: Context, allowPartial: Boolean = true): Boolean {
    if (SDK_INT >= TIRAMISU && (context.checkPermission(READ_MEDIA_IMAGES))) {
        // Full access on Android 13 (API level 33) or higher
        return true
    } else if (SDK_INT >= UPSIDE_DOWN_CAKE && context.checkPermission(READ_MEDIA_VISUAL_USER_SELECTED)) {
        // Partial access on Android 14 (API level 34) or higher
        return allowPartial
    } else if (context.checkPermission(READ_EXTERNAL_STORAGE)) {
        // Full access up to Android 12 (API level 32)
        return true
    } else {
        // Access denied
        return false
    }
}

fun isPartialStoragePermissionGranted(context: Context): Boolean {
    return SDK_INT >= UPSIDE_DOWN_CAKE &&
            !context.checkPermission(READ_MEDIA_IMAGES) &&
            context.checkPermission(READ_MEDIA_VISUAL_USER_SELECTED)
}

fun checkNotificationPermission(context: Context): Boolean {
    return if (SDK_INT >= TIRAMISU) {
        context.checkPermission(Manifest.permission.POST_NOTIFICATIONS)
    } else true
}

fun checkLocationPermissions(context: Context): Boolean {
    return context.checkPermissions(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
}