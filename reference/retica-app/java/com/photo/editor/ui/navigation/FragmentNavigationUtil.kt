package com.photo.editor.ui.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController

fun Fragment.safePopBackstack(): Boolean {
    return try {
        findNavController().popBackStack()
    } catch (ex: Exception) {
        ex.printStackTrace()
        false
    }
}

fun Fragment.safePopBackstack(@IdRes destinationId: Int, inclusive: Boolean) : Boolean {
    return try {
        findNavController().popBackStack(destinationId, inclusive)
    } catch (ex: Exception) {
        ex.printStackTrace()
        false
    }
}

fun Fragment.safeNavigate(directions: NavDirections) {
    safeNavigate(directions) { findNavController() }
}

fun Fragment.safeNavigate(directions: NavDirections, navOptions: NavOptions?) {
    safeNavigate(directions, navOptions) { findNavController() }
}