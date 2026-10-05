package com.photo.editor.ui.screen.edit.common

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.createBitmap
import com.photo.editor.domain.datastructure.Memento
import com.photo.editor.editimage.model.FaceLandmark
import com.photo.editor.editimage.model.ImageFrame
import com.photo.editor.editimage.model.filter.FilterData
import com.photo.editor.editimage.model.filter.FilterPreset
import com.photo.editor.overlaybrush.brush.BrushDrawingState
import com.photo.editor.editimage.model.EditImageData
import com.photo.editor.overlaybrush.overlay.ImageOverlaySaveState
import com.photo.editor.overlaybrush.overlay.ImageOverlayState
import com.photo.editor.overlaybrush.overlay.textoverlay.common.TextOverlayData
import com.photo.editor.overlaybrush.overlay.textoverlay.common.TextOverlaySaveState
import com.photo.editor.overlaybrush.overlay.textoverlay.common.TextOverlayState
import com.photo.editor.common.ui.transformableimage.TransformState
import com.photo.editor.ui.screen.edit.enums.EditImageBeautyOption
import com.photo.editor.ui.screen.edit.enums.EditImageEditOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
class EditImageUiState(
    val context: Context? = null,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    var originalBitmap: Bitmap by mutableStateOf(createBitmap(1, 1))

    var isSelectedEditOption by mutableStateOf(true)
    var selectedEditOption: EditImageEditOption? by mutableStateOf(null)
    var selectedBeautyOption: EditImageBeautyOption? by mutableStateOf(null)

    val transformState = TransformState()

    val showOriginalImageFlow = MutableStateFlow(false)
    private var saveStateJob: Job? = null

    // -------------------- Filter -------------------- //

    private val _filterDataFlow = MutableStateFlow(FilterData())
    val filterDataFlow = _filterDataFlow.asStateFlow()

    fun setSelectedFilter(filter: FilterPreset?) = _filterDataFlow.update { it.copy(filter = filter) }
    fun setFilterIntensity(intensity: Float) = _filterDataFlow.update { it.copy(intensity = intensity) }
    fun setFilterData(filter: FilterPreset?, intensity: Float) = _filterDataFlow.update { it.copy(filter = filter, intensity = intensity) }

    // -------------------- Adjust -------------------- //

    val adjustState = EditImageAdjustState()

    // -------------------- Crop and transform -------------------- //

    val cropState = EditImageCropState()

    // -------------------- Color -------------------- //

    val colorState = EditImageColorState()

    // -------------------- Frame -------------------- //

    private val _frameFlow = MutableStateFlow<ImageFrame?>(null)
    val frameFlow = _frameFlow.asStateFlow()

    fun setFrame(frame: ImageFrame?) = _frameFlow.update { frame }

    // -------------------- Beauty and makeup -------------------- //

    val beautyState = EditImageBeautyState()

    // -------------------- Brush -------------------- //

    val brushDrawingState = BrushDrawingState(transformState)

    // -------------------- Sticker and text overlays -------------------- //

    val overlayState = EditImageOverlayState(context, transformState)

    /** Face detection starts running when user clicks on Beauty on bottom bar. */
    var isDetectingFaceLandmark: Boolean by mutableStateOf(false)
    var faceLandmarkDetected: Boolean by mutableStateOf(false)
    var faceLandmarks: List<FaceLandmark> by mutableStateOf(emptyList())

    // -------------------- Undo and redo -------------------- //

    private val editDataMemento = Memento<EditImageSaveState>()

    var canUndo: Boolean by mutableStateOf(false)
        private set
    var canRedo: Boolean by mutableStateOf(false)
        private set

    init {
        saveState()
    }

    fun undo() {
        val saveState = editDataMemento.undo() ?: return
        restoreState(saveState)

        canUndo = editDataMemento.canUndo
        canRedo = editDataMemento.canRedo
    }

    fun redo() {
        val saveState = editDataMemento.redo() ?: return
        restoreState(saveState)

        canUndo = editDataMemento.canUndo
        canRedo = editDataMemento.canRedo
    }

    fun restoreLastState() {
        val saveState = editDataMemento.currentState ?: return
        restoreState(saveState)
    }

    fun saveStateDelayed() {
        saveStateJob?.cancel()
        saveStateJob = coroutineScope.launch {
            delay(300)
            saveState()
        }
    }

    fun saveState() {
        coroutineScope.launch {
            editDataMemento.saveState(
                EditImageSaveState(
                    editData = EditImageData(
                        filter = _filterDataFlow.value,
                        adjust = adjustState.adjustDataFlow.value,
                        crop = cropState.cropDataFlow.value,
                        color = colorState.colorDataFlow.value,
                        frame = _frameFlow.value,
                        beauty = beautyState.beautyDataFlow.value,
                        makeup = beautyState.makeupDataFlow.value,
                    ),
                    brushSaveState = brushDrawingState.saveState(),
                    overlaySaveStates = overlayState.overlayStates.value.mapNotNull {
                        when (it) {
                            is TextOverlayState -> it.saveState()
                            is ImageOverlayState -> it.saveState()
                            else -> null
                        }
                    },
                )
            )

            canUndo = editDataMemento.canUndo
            canRedo = editDataMemento.canRedo
        }
    }

    private fun restoreState(saveState: EditImageSaveState) {
        saveState.editData.let { editData ->
            _filterDataFlow.value = editData.filter
            adjustState.setAdjustData(editData.adjust)
            cropState.setCropData(editData.crop)
            colorState.setColorData(editData.color)
            _frameFlow.value = editData.frame
            beautyState.setBeautyData(editData.beauty)
            beautyState.setMakeupData(editData.makeup)
        }
        brushDrawingState.restoreState(saveState.brushSaveState)
        val newOverlays = saveState.overlaySaveStates.mapNotNull { overlaySaveState ->
            when (overlaySaveState) {
                is TextOverlaySaveState -> {
                    TextOverlayState(context!!, transformState, TextOverlayData()).apply { restoreState(overlaySaveState) }
                }

                is ImageOverlaySaveState -> {
                    ImageOverlayState(transformState, overlaySaveState.overlayImage).apply { restoreState(overlaySaveState) }
                }

                else -> null
            }
        }
        overlayState.setOverlays(newOverlays)
    }

    // -------------------- Export -------------------- //

    var exportingImage: Boolean by mutableStateOf(false)
    var exportedImage: Uri by mutableStateOf(Uri.EMPTY)
}