package com.phongkaster.kanso.camera

import android.graphics.Bitmap
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.camera.CameraState
import com.phongkaster.kanso.ui.camera.CameraStatus
import com.phongkaster.kanso.ui.camera.FlashMode
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * A [CameraController] with no camera behind it, for JVM tests (Robolectric cannot run CameraX).
 *
 * Binding reports [CameraStatus.Ready] at once and ignores the surface provider, so the screen's
 * PreviewView is never drawn into. Taking a picture writes [fixture] as a JPEG into the file it
 * is given, as a real camera would, and records the file in [takenFiles]. The JPEG gets the EXIF
 * [orientation] if one is given, like a photo taken with the phone held sideways.
 *
 * The settings behave like a phone with both lenses ([canSwitchLens]), a flash on every lens
 * when [hasFlash] is true, and a zoom from 1 to [maxZoom]. Each lens switch resets the zoom to 1,
 * as CameraX does.
 */
class FakeCameraController(
    private val fixture: Bitmap = ToolFixtures.gradient(60, 40),
    private val orientation: Int? = null,
    private val hasFlash: Boolean = true,
    private val canSwitchLens: Boolean = true,
    private val maxZoom: Float = 4f,
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

    /** Reports the camera ready with this fake's lens, flash and zoom; there is no hardware. */
    override suspend fun bind(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        bindCount++
        mutableState.update {
            it.copy(
                status = CameraStatus.Ready,
                canSwitchLens = canSwitchLens,
                hasFlash = hasFlash,
                minZoomRatio = 1f,
                maxZoomRatio = maxZoom,
            )
        }
    }

    /** Writes the fixture JPEG into [file] on a background thread, like CameraX does. */
    override suspend fun takePicture(file: File) {
        withContext(Dispatchers.IO) { ToolFixtures.writeJpeg(file, fixture, orientation) }
        takenFiles += file
    }

    /** Swaps the lens in the state and resets the zoom, when switching is possible. */
    override fun switchLens() {
        if (!canSwitchLens) return
        mutableState.update { it.copy(lens = it.lens.other(), zoomRatio = 1f) }
    }

    /** Records the flash mode in the state. */
    override fun setFlashMode(mode: FlashMode) {
        mutableState.update { it.copy(flashMode = mode) }
    }

    /** Records the zoom in the state, held between 1 and [maxZoom], while connected. */
    override fun setZoomRatio(ratio: Float) {
        if (mutableState.value.status != CameraStatus.Ready) return
        mutableState.update { it.copy(zoomRatio = ratio.coerceIn(1f, maxZoom)) }
    }

    /** Reports the camera idle again; the lens and flash choices are kept, as CameraX does. */
    override fun unbind() {
        mutableState.update { it.copy(status = CameraStatus.Idle) }
    }

    /** Records that the camera was let go of. */
    override fun release() {
        released = true
        unbind()
    }
}
