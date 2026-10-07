package com.photo.editor.common.util

import android.os.Bundle
import android.util.Log

/**
 * Records that the event [name] happened, by writing it to the debug log.
 *
 * Kanso has no analytics: this only logs locally, and nothing leaves the device.
 */
fun logEvent(name: String) {
    Log.d("EventTracking", "logEvent: $name")
}

/**
 * Records that the event [name] happened, with the parameters [block] adds, by writing it to the
 * debug log.
 *
 * Kanso has no analytics: this only logs locally, and nothing leaves the device.
 */
inline fun logEvent(name: String, crossinline block: ParametersBuilder.() -> Unit) {
    val params = ParametersBuilder().also { it.block() }
    Log.d("EventTracking", "logEvent: $name: ${params.bundle}")
}

/**
 * Collects the parameters of one logged event into a [Bundle].
 *
 * It keeps the same `param(key, value)` calls the engine was written against, so callers did not
 * have to change when the analytics library was removed.
 */
class ParametersBuilder {

    /** The parameters added so far. */
    val bundle: Bundle = Bundle()

    /** Adds the text parameter [key] with the value [value]. */
    fun param(key: String, value: String) {
        bundle.putString(key, value)
    }

    /** Adds the whole-number parameter [key] with the value [value]. */
    fun param(key: String, value: Long) {
        bundle.putLong(key, value)
    }

    /** Adds the decimal parameter [key] with the value [value]. */
    fun param(key: String, value: Double) {
        bundle.putDouble(key, value)
    }

    /** Adds the nested parameter group [key] with the value [value]. */
    fun param(key: String, value: Bundle) {
        bundle.putBundle(key, value)
    }

    /** Adds the list of parameter groups [key] with the value [value]. */
    fun param(key: String, value: Array<Bundle>) {
        bundle.putParcelableArray(key, value)
    }
}
