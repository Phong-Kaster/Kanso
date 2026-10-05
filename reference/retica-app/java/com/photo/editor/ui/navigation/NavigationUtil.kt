package com.photo.editor.ui.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

fun safePopBackstack(navControllerProvider: () -> NavController): Boolean {
    return try {
        navControllerProvider().popBackStack()
    } catch (ex: Exception) {
        ex.printStackTrace()
        false
    }
}

fun safePopBackstack(@IdRes destinationId: Int, inclusive: Boolean, navControllerProvider: () -> NavController): Boolean {
    return try {
        navControllerProvider().popBackStack(destinationId, inclusive)
    } catch (ex: Exception) {
        ex.printStackTrace()
        false
    }
}

fun NavController?.safePopBackStack(): Boolean {
    if (this == null) return false
    return try {
        popBackStack()
    } catch (ex: Exception) {
        ex.printStackTrace()
        false
    }
}

fun NavController?.safePopBackstack(@IdRes destinationId: Int, inclusive: Boolean): Boolean {
    if (this == null) return false
    return try {
        popBackStack(destinationId, inclusive)
    } catch (ex: Exception) {
        ex.printStackTrace()
        false
    }
}

fun NavController?.safeNavigate(directions: NavDirections) {
    if (this == null) return
    try {
        currentDestination?.getAction(directions.actionId)?.let { navigate(directions) }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun NavController?.safeNavigate(directions: NavDirections, navOptions: NavOptions?) {
    if (this == null) return
    try {
        currentDestination?.getAction(directions.actionId)?.let { navigate(directions, navOptions) }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun safeNavigate(directions: NavDirections, navOptions: NavOptions?, navControllerProvider: () -> NavController) {
    try {
        with(navControllerProvider()) {
            currentDestination?.getAction(directions.actionId)?.let { navigate(directions, navOptions) }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun safeNavigate(directions: NavDirections, navControllerProvider: () -> NavController) {
    try {
        with(navControllerProvider()) {
            currentDestination?.getAction(directions.actionId)?.let { navigate(directions) }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun safeNavigate(@IdRes resId: Int, navControllerProvider: () -> NavController) {
    safeNavigate(resId, null, navControllerProvider)
}

fun safeNavigate(@IdRes resId: Int, args: Bundle?, navControllerProvider: () -> NavController) {
    safeNavigate(resId, args, null, navControllerProvider)
}

fun safeNavigate(@IdRes resId: Int, args: Bundle?, navOptions: NavOptions?, navControllerProvider: () -> NavController) {
    safeNavigate(resId, args, navOptions, null, navControllerProvider)
}

fun safeNavigate(
    @IdRes resId: Int,
    args: Bundle?,
    navOptions: NavOptions?,
    navigatorExtras: Navigator.Extras?,
    navControllerProvider: () -> NavController,
) {
    try {
        navControllerProvider().navigate(resId, args, navOptions, navigatorExtras)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}