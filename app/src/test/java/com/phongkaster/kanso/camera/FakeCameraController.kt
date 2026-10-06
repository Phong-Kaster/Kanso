package com.phongkaster.kanso.camera

import android.graphics.Bitmap
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.camera.CameraState
import com.phongkaster.kanso.ui.camera.CameraStatus
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * A [CameraController] with no camera behind it, for JVM tests (Robolectric cannot run CameraX).
 *
 * Binding reports [CameraStatus.Ready] at once and ignores the surface provider, so the screen's
 * PreviewView is never drawn into. Taking a picture writes [fixture] as a JPEG into the file it
 * is given, as a real camera would, and records the file in [takenFiles]. The JPEG gets the EXIF
 * [orientation] if one is given, like a photo taken with the phone held sideways.
 */
class FakeCameraController(
    private val fixture: Bitmap = ToolFixtures.gradient(60, 40),
    private val orientation: Int? = null,
) : CameraController {

    /** The current state behind [state]. */
    private val mutableState = MutableStateFlow(CameraState())

    /** What the fake camera is doing. */
    override val state: StateFlow<CameraState> = mutableState.asStateFlow()

    /** Every file a picture was written into, oldest first. */
    val takenFiles = CopyOnWriteArrayList<File>()

    /** How many times the screen connected the camera. */
    @Volatile
    var bindCount = 0
        private set

    /** Whether [release] has been called. */
    @Volatile
    var released = false
        private set

    /** Reports the camera ready; there is no hardware to open. */
    override suspend fun bind(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        bindCount++
        mutableState.value = CameraState(status = CameraStatus.Ready)
    }

    /** Writes the fixture JPEG into [file] on a background thread, like CameraX does. */
    override suspend fun takePicture(file: File) {
        withContext(Dispatchers.IO) { ToolFixtures.writeJpeg(file, fixture, orientation) }
        takenFiles += file
    }

    /** Reports the camera idle again. */
    override fun unbind() {
        mutableState.value = CameraState(status = CameraStatus.Idle)
    }

    /** Records that the camera was let go of. */
    override fun release() {
        released = true
        unbind()
    }
}
