package com.phongkaster.kanso.gallery

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

/**
 * An ActivityResultRegistry that shows no dialog: it records the permissions of each request
 * and lets the test answer the latest one with [answerLatest].
 */
class RecordingRegistry : ActivityResultRegistry() {

    /** The permissions of every request so far, oldest first. */
    val requests = mutableListOf<List<String>>()

    /** The request code of the latest request, used to deliver its answer. */
    private var latestRequestCode = -1

    /** Records the request instead of showing the system permission dialog. */
    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        @Suppress("UNCHECKED_CAST")
        requests += (input as Array<String>).toList()
        latestRequestCode = requestCode
    }

    /** Delivers [result] as the answer to the latest request. */
    fun answerLatest(result: Map<String, Boolean>) {
        check(latestRequestCode != -1) { "No permission request to answer" }
        dispatchResult(latestRequestCode, result)
    }
}
