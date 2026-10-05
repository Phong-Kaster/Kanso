package com.photo.editor.common.util

import android.content.Context
import android.widget.Toast

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.toast(message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}