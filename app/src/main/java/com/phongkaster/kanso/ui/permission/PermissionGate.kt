package com.phongkaster.kanso.ui.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** How much a set of runtime permissions currently lets the app do. */
enum class PermissionAccess {
    /** Every permission needed for full access is granted. */
    Full,

    /** Only a partial-access permission is granted (Android 14+ "Select photos"). */
    Partial,

    /** Nothing useful is granted. */
    None,
}

/**
 * Reads, right now, what the phone has granted: [PermissionAccess.Full] when every one of
 * [fullAccess] is granted, [PermissionAccess.Partial] when at least one of [partialAccess] is,
 * otherwise [PermissionAccess.None].
 *
 * ContextCompat.checkSelfPermission asks the system directly, so the answer is always the real
 * one, including changes the person made in the system settings while the app was away.
 */
fun readPermissionAccess(
    context: Context,
    fullAccess: List<String>,
    partialAccess: List<String>,
): PermissionAccess {
    /** Tells whether the system has granted [permission] to this app. */
    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    return when {
        fullAccess.isNotEmpty() && fullAccess.all(::granted) -> PermissionAccess.Full
        partialAccess.any(::granted) -> PermissionAccess.Partial
        else -> PermissionAccess.None
    }
}

/**
 * Asks for [permissions] the first time it is shown, then shows the content that fits the
 * answer: [granted] for full access, [partial] for partial access, [refused] after a refusal and
 * [waiting] while the question is still open. Reuse it for any runtime permission; the camera
 * passes `listOf(Manifest.permission.CAMERA)`.
 *
 * How runtime permissions work in Compose:
 *
 * - Declaring a permission in the manifest is not enough for "dangerous" ones such as photos or
 *   the camera: the app must ask while it runs, and the system shows its own dialog. The answer
 *   comes back to the activity later, like the result of another screen.
 * - `rememberLauncherForActivityResult` registers for that answer and hands back a launcher. It
 *   must be registered on every composition (not only when asking), so that an answer which
 *   arrives after the activity was re-created (for example the phone was rotated while the
 *   dialog was open) still finds its way back here.
 * - The "already asked" flag is kept with `rememberSaveable`, not plain `remember`, because a
 *   rotation re-creates the activity and plain remembered values start over. Without it the
 *   screen would ask again after every rotation and, after a refusal, would loop. Pausing and
 *   resuming does not reset it either, so the request fires once per visit to the screen.
 * - After a refusal the gate does not ask again by itself. The [refused] content should explain
 *   why access is needed and offer the way to the app's system settings (see
 *   [AccessRefusedPanel]); Android itself stops showing the dialog after repeated refusals.
 * - The state is read with ContextCompat.checkSelfPermission after each answer and every time
 *   the screen resumes, rather than trusting only the answer's map, so a change made in the
 *   system settings shows up as soon as the person comes back.
 *
 * [fullAccess] are the permissions that together mean full access (by default all of
 * [permissions]); [partialAccess] are those that mean partial access (none by default). The
 * [partial] content receives a function that asks for [permissions] again, for a "share more"
 * button; when [partial] is null, partial access shows [granted].
 */
@Composable
fun PermissionGate(
    permissions: List<String>,
    refused: @Composable () -> Unit,
    granted: @Composable () -> Unit,
    fullAccess: List<String> = permissions,
    partialAccess: List<String> = emptyList(),
    waiting: @Composable () -> Unit = {},
    partial: (@Composable (requestAgain: () -> Unit) -> Unit)? = null,
) {
    val context = LocalContext.current

    // What the phone has granted right now. Plain `remember` is enough: it is read again from the
    // system after a re-creation anyway.
    var access by remember {
        mutableStateOf(readPermissionAccess(context, fullAccess, partialAccess))
    }

    // Whether the request has been launched on this visit, and whether its answer came back.
    // Both survive rotation and process death through rememberSaveable (see the KDoc above).
    var asked by rememberSaveable { mutableStateOf(false) }
    var answered by rememberSaveable { mutableStateOf(false) }

    // RequestMultiplePermissions shows the system dialog for every permission in the array and
    // answers with a map of permission -> granted. The map is not used: the real state is read
    // from the system instead.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        answered = true
        access = readPermissionAccess(context, fullAccess, partialAccess)
    }

    /** Launches the system permission dialog for [permissions] and remembers that it was asked. */
    fun request() {
        asked = true
        launcher.launch(permissions.toTypedArray())
    }

    // LaunchedEffect(Unit) runs once when the gate first appears, never at app launch. It asks
    // only if nothing has been asked on this visit yet and nothing useful is granted; someone who
    // already allowed access, or shared some photos, is not asked again.
    LaunchedEffect(Unit) {
        if (!asked && access == PermissionAccess.None) request()
    }

    // Read the state again each time the screen resumes: the person may have gone to the
    // system settings (through the refused panel's button) and changed it there.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                access = readPermissionAccess(context, fullAccess, partialAccess)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        access == PermissionAccess.Full -> granted()
        access == PermissionAccess.Partial ->
            if (partial != null) partial { request() } else granted()
        answered -> refused()
        else -> waiting()
    }
}
