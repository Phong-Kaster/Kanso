package com.phongkaster.kanso.ui.permission

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build

/*
 * Which permission lets an app read the phone's photos depends on the Android version:
 *
 * - Android 12 and older (API 32 and below): READ_EXTERNAL_STORAGE, which covers all shared files.
 * - Android 13 (API 33): READ_MEDIA_IMAGES, which covers photos only.
 * - Android 14 and later (API 34+): READ_MEDIA_IMAGES plus READ_MEDIA_VISUAL_USER_SELECTED. The
 *   second one is the "partial access" model: the system dialog offers "Allow all", "Select
 *   photos" and "Don't allow". If the person picks "Select photos", only
 *   READ_MEDIA_VISUAL_USER_SELECTED is granted and the app sees just the photos they chose. An
 *   app that asks for both can tell full from partial access by checking READ_MEDIA_IMAGES.
 *
 * The functions take the SDK level as a plain number, so a test can check every version without
 * a phone; the app passes Build.VERSION.SDK_INT.
 */

/**
 * Returns the permissions to ask for so the app can read photos on Android [sdkInt].
 *
 * On Android 14+ both permissions are requested together so the system can offer the
 * "Select photos" choice. The constants are plain strings, so naming a newer one on an older
 * phone is harmless; the version check decides which ones are actually asked for.
 */
@SuppressLint("InlinedApi")
fun photoPermissionsFor(sdkInt: Int): List<String> = when {
    sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> listOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    sdkInt >= Build.VERSION_CODES.TIRAMISU -> listOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

/**
 * Returns the permission that means "every photo can be read" on Android [sdkInt]: when it is
 * granted the gallery has full access.
 */
@SuppressLint("InlinedApi")
fun fullPhotoPermissionFor(sdkInt: Int): String =
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/**
 * Returns the permissions that mean "only some photos were shared" on Android [sdkInt]: the
 * Android 14+ READ_MEDIA_VISUAL_USER_SELECTED, or nothing on older versions, which have no
 * partial access.
 */
@SuppressLint("InlinedApi")
fun partialPhotoPermissionsFor(sdkInt: Int): List<String> =
    if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        listOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else {
        emptyList()
    }
