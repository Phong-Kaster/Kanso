package com.photo.editor.common.util

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.ParametersBuilder
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

fun logEvent(name: String) {
    Log.d("EventTracking", "logEvent: $name")
    Firebase.analytics.logEvent(name, null)
}

inline fun logEvent(name: String, crossinline block: ParametersBuilder.() -> Unit) {
    val params = ParametersBuilder().also(block)
    Log.d("EventTracking", "logEvent: $name: ${params.bundle}")
    Firebase.analytics.logEvent(name, params.bundle)
}