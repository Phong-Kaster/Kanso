package com.phongkaster.kanso.save

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

/**
 * An ActivityResultRegistry that shows no dialog: it records the permission of each single
 * permission request (`RequestPermission`, whose input is one String) and lets the test answer
 * the latest one with [answerLatest].
 */
class SinglePermissionRegistry : ActivityResultRegistry() {

    /** The permission of every request so far, oldest first. */
    val requests = mutableListOf<String>()

    /** The request code of the latest request, used to deliver its answer. */
    private var latestRequestCode = -1

    /** Records the request instead of showing the system permission dialog. */
    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        requests += input.toString()
        latestRequestCode = requestCode
    }

    /** Delivers [granted] as the answer to the latest request. */
    fun answerLatest(granted: Boolean) {
        check(latestRequestCode != -1) { "No permission request to answer" }
        dispatchResult(latestRequestCode, granted)
    }
}
