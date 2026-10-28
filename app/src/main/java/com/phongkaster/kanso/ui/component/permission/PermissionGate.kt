package com.phongkaster.kanso.ui.component.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.component.permission.model.PermissionAccess
import com.phongkaster.kanso.ui.theme.KansoTheme

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

    // Everything above gathers state; choosing what to draw from it is the stateless part's job.
    PermissionGateContent(
        access = access,
        answered = answered,
        refused = refused,
        granted = granted,
        waiting = waiting,
        partial = partial,
        requestAgain = { request() },
    )
}

/**
 * The stateless part of the gate. It is split from [PermissionGate] for the same reason Home is
 * split (see `HomeScreen`'s KDoc on state hoisting): the stateful part needs a permission
 * launcher, the phone's lifecycle and the system's answers, which a preview cannot provide.
 *
 * What is specific here: this part only chooses which content to show. With [access]
 * [PermissionAccess.Full] it shows [granted]; with [PermissionAccess.Partial] it shows [partial]
 * (handing it [requestAgain], for a "share more" button), or [granted] when [partial] is null;
 * with nothing granted it shows [refused] once the question was [answered], and [waiting] while
 * the question is still open.
 */
@Composable
fun PermissionGateContent(
    access: PermissionAccess,
    answered: Boolean,
    refused: @Composable () -> Unit,
    granted: @Composable () -> Unit,
    waiting: @Composable () -> Unit,
    partial: (@Composable (requestAgain: () -> Unit) -> Unit)?,
    requestAgain: () -> Unit,
) {
    when {
        access == PermissionAccess.Full -> granted()
        access == PermissionAccess.Partial ->
            if (partial != null) partial(requestAgain) else granted()
        answered -> refused()
        else -> waiting()
    }
}

/**
 * A stand-in for a screen's real content in the gate's previews: [label] centred on the theme's
 * background, saying which slot the gate chose. The real gallery or camera needs the phone's
 * photos or camera, which a preview cannot reach, so the previews show this instead; [action],
 * when given, adds a button (the partial state's "share more").
 */
@Composable
private fun SampleGateSlot(label: String, action: Pair<String, () -> Unit>? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            TextButton(onClick = action.second, shapes = ButtonDefaults.shapes()) {
                Text(action.first)
            }
        }
    }
}

/**
 * Draws [PermissionGateContent] for one [access] and [answered] pair, with a stand-in for each
 * slot. The refused slot is the real [AccessRefusedPanel] with the gallery's wording, as the
 * gallery shows it; the others are [SampleGateSlot] stand-ins. Every gate preview below calls
 * this with a different state, inside KansoTheme and a Surface.
 */
@Composable
private fun SampleGate(access: PermissionAccess, answered: Boolean) {
    PermissionGateContent(
        access = access,
        answered = answered,
        refused = {
            AccessRefusedPanel(
                title = stringResource(R.string.gallery_refused_title),
                message = stringResource(R.string.gallery_refused_message),
                buttonLabel = stringResource(R.string.gallery_open_settings),
                onOpenSettings = {},
            )
        },
        granted = { SampleGateSlot("All 248 photos are shared: the gallery shows here.") },
        waiting = { SampleGateSlot("Waiting for an answer to the system's question.") },
        partial = { requestAgain ->
            SampleGateSlot(
                label = stringResource(R.string.gallery_partial_notice),
                action = stringResource(R.string.gallery_share_more) to requestAgain,
            )
        },
        requestAgain = {},
    )
}

/**
 * The gate while the system's question is still open: nothing granted and no answer yet, so it
 * shows the waiting slot (the real screens leave it empty; the preview labels it). Drawn in light
 * and dark; `ThemeMode.System` makes KansoTheme follow the preview's mode.
 */
@PreviewLightDark
@Composable
private fun PermissionGateWaitingPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SampleGate(access = PermissionAccess.None, answered = false)
        }
    }
}

/** The gate after full access was granted: it shows the granted slot, the real screen. */
@PreviewLightDark
@Composable
private fun PermissionGateFullPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SampleGate(access = PermissionAccess.Full, answered = true)
        }
    }
}

/**
 * The gate after the person chose "Select photos" (Android 14+): it shows the partial slot with
 * its "Share more photos" button, which asks again.
 */
@PreviewLightDark
@Composable
private fun PermissionGatePartialPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SampleGate(access = PermissionAccess.Partial, answered = true)
        }
    }
}

/** The gate after a refusal: it shows the refused slot, the panel pointing to the settings. */
@PreviewLightDark
@Composable
private fun PermissionGateRefusedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SampleGate(access = PermissionAccess.None, answered = true)
        }
    }
}
