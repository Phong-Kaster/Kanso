package com.phongkaster.kanso.ui.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phongkaster.kanso.di.ViewModelClearedProbe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Returns the folder shots are written to: `shots/` inside the app's private files folder.
 *
 * That folder belongs to Kanso alone. Other apps cannot read it, the phone's gallery does not
 * list it, and Android deletes it with the app. A shot only reaches the phone's gallery later,
 * if the person saves the finished photo.
 */
fun shotsDirectory(context: Context): File = File(context.filesDir, "shots")

/**
 * Holds the state of the camera screen: the camera itself ([controller]) and the photo just
 * taken, which the person then keeps or retakes.
 *
 * A ViewModel outlives a rotation, so a photo taken just before the phone is turned is still
 * waiting for Keep or Retake afterwards. The camera connection itself does not survive (it is
 * tied to the screen), and the screen simply connects again.
 *
 * [probe] is told when this ViewModel is cleared, so tests can check that leaving the screen
 * frees it. Clearing also lets go of the camera and deletes a photo that was taken but neither
 * kept nor retaken.
 */
class CameraViewModel(
    private val controller: CameraController,
    private val probe: ViewModelClearedProbe,
) : ViewModel() {

    /**
     * The current state behind [state]; only this ViewModel changes it. It starts with the camera
     * as the controller reports it right now, so even the first snapshot is complete.
     */
    private val mutableState = MutableStateFlow(CameraUiState(camera = controller.state.value))

    /**
     * Everything the camera screen shows, as one snapshot: what the camera is doing (opening,
     * ready, unavailable, with its lens, flash and zoom), the photo waiting for a decision, and
     * whether a photo is being taken.
     */
    val state: StateFlow<CameraUiState> = mutableState.asStateFlow()

    init {
        // An init block runs once, while this ViewModel is being built, in the order it sits among
        // the property declarations: it must stay below mutableState, which it writes to.
        // The controller keeps its own state (a StateFlow of CameraState). collect runs the block
        // once with the current value and again after every change, copying each one into the
        // screen's one snapshot, so the screen reads a single state. update { } swaps only the
        // camera part and keeps the rest of the snapshot as it is at that instant, so a photo
        // taken in between is never undone. viewModelScope ends this copying when the ViewModel
        // is cleared.
        viewModelScope.launch {
            controller.state.collect { camera -> mutableState.update { it.copy(camera = camera) } }
        }
    }

    /** Whether the waiting photo was kept, so clearing this ViewModel does not delete it. */
    private var kept = false

    /**
     * Connects the camera to the screen's preview, tied to [lifecycleOwner]; see
     * [CameraController.bind]. The screen calls this from a coroutine of its own, so leaving the
     * screen while the camera is still opening cancels the connection.
     */
    suspend fun bind(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        controller.bind(lifecycleOwner, surfaceProvider)
    }

    /** Disconnects the camera from the screen, when the live picture leaves the screen. */
    fun unbind() {
        controller.unbind()
    }

    /**
     * Swaps between the back and front lens. Does nothing while a photo is being taken, since
     * swapping disconnects the camera the photo is coming from.
     */
    fun switchLens() {
        if (mutableState.value.taking) return
        controller.switchLens()
    }

    /**
     * Moves the flash to its next mode: auto, then on, then off, then auto again.
     *
     * It reads the mode from the controller itself, not from the copy in [state]: the copy is
     * updated a moment later, and two quick taps must move the flash two steps.
     */
    fun cycleFlash() {
        controller.setFlashMode(controller.state.value.flashMode.next())
    }

    /**
     * Zooms by [factor] from the current zoom: 2 doubles it, 0.5 halves it. A pinch reports how
     * much the fingers spread or closed since its last report as such a factor, so each report
     * multiplies the zoom a little more. The controller keeps the result within the lens's limits.
     *
     * Like [cycleFlash], it starts from the controller's own zoom rather than the copy in
     * [state], because a pinch sends many reports in a row and each must build on the last.
     */
    fun zoomBy(factor: Float) {
        if (factor <= 0f || factor.isNaN()) return
        val current = controller.state.value
        val target = (current.zoomRatio * factor).coerceIn(current.minZoomRatio, current.maxZoomRatio)
        if (target != current.zoomRatio) controller.setZoomRatio(target)
    }

    /**
     * Takes a photo into a new file in [folder] (see [shotsDirectory]). Does nothing while a
     * photo is already being taken or one is waiting for Keep or Retake.
     *
     * The file is created on a background thread (Dispatchers.IO, meant for files). On failure
     * the half-written file is deleted and [CameraUiState.shotFailed] is set, so the screen can
     * say so and the person can try again.
     */
    fun takePicture(folder: File) {
        val current = mutableState.value
        if (current.taking || current.shot != null) return
        mutableState.update { it.copy(taking = true, shotFailed = false) }
        // viewModelScope is cancelled when this ViewModel is cleared, so a photo still being
        // taken when the person leaves is abandoned.
        viewModelScope.launch {
            var file: File? = null
            try {
                val target = withContext(Dispatchers.IO) {
                    folder.mkdirs()
                    // Kept shots are never deleted by their screen (the tool screen still shows them),
                    // so clear out shots older than a day each time a new one is taken.
                    val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
                    folder.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
                    // createTempFile picks a name no other file in the folder has.
                    File.createTempFile("shot_", ".jpg", folder)
                }
                file = target
                controller.takePicture(target)
                kept = false
                mutableState.update { it.copy(shot = target, taking = false) }
            } catch (e: CancellationException) {
                file?.delete()
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.IO) { file?.delete() }
                mutableState.update { it.copy(taking = false, shotFailed = true) }
            }
        }
    }

    /**
     * Throws the waiting photo away and goes back to the live picture.
     *
     * The photo is dropped from the state at once, so the screen stops showing it and the live
     * picture comes back. The file is then deleted on a background thread, trying again a few
     * times (about two seconds at most): the picture loader may still have the file open for a
     * moment, and Windows (where the tests run) refuses to delete an open file. Until the file is
     * gone [CameraUiState.taking] stays true, so the shutter cannot be pressed while the old
     * photo is still on disk.
     */
    fun retake() {
        val shot = mutableState.value.shot ?: return
        mutableState.update { it.copy(shot = null, taking = true, shotFailed = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    var tries = 0
                    while (shot.exists() && !shot.delete() && tries++ < 40) delay(50)
                }
            } finally {
                // Leaving the screen mid-delete cancels this coroutine; make one last plain attempt.
                if (shot.exists()) shot.delete()
                mutableState.update { it.copy(taking = false) }
            }
        }
    }

    /**
     * Keeps the waiting photo and returns it as the text of a `file://` Uri, ready to hand to
     * the tool screen, or null when no photo is waiting. A kept photo is not deleted when this
     * ViewModel is cleared.
     */
    fun keep(): String? {
        val shot = mutableState.value.shot ?: return null
        kept = true
        return Uri.fromFile(shot).toString()
    }

    /**
     * Reports to the probe that this ViewModel has been cleared, lets go of the camera, and
     * deletes a photo that was taken but not kept.
     */
    override fun onCleared() {
        probe.recordCleared(this)
        controller.release()
        if (!kept) mutableState.value.shot?.delete()
        super.onCleared()
    }
}
