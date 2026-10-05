package com.photo.editor.ui.screen.retouch.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.photo.editor.R
import com.photo.editor.common.ui.transformableimage.ImageState
import com.photo.editor.common.util.scaleDownBitmap
import com.photo.editor.common.util.size
import com.photo.editor.common.util.uriToBitmap
import com.photo.editor.domain.datastructure.Memento
import com.photo.editor.domain.usecase.ExportImageUC
import com.photo.editor.localml.removeobject.InteractiveSegmentationUC
import com.photo.editor.localml.removeobject.RemoveObjectUC
import com.photo.editor.ui.base.BaseViewModel
import com.photo.editor.ui.screen.retouch.edit.component.drawer.RetouchDrawingState
import com.photo.editor.ui.screen.retouch.edit.component.drawer.RetouchPathProperties
import com.photo.editor.ui.screen.retouch.edit.component.toucher.RetouchTouchState
import com.photo.editor.ui.screen.retouch.model.RetouchMode
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.annotation.KoinViewModel
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@KoinViewModel
class RetouchViewModel @Inject constructor(
    private val context: Context,
    private val removeObjectUC: RemoveObjectUC,
    private val exportImageUC: ExportImageUC,
    private val interactiveSegmentationUC: InteractiveSegmentationUC,
) : BaseViewModel() {

    var uiState = RetouchUiState()

    var imageState = ImageState()

    val drawingState = RetouchDrawingState().apply {
        reset()
        currentPathProperty = RetouchPathProperties(strokeWidth = uiState.brushSize)
    }

    val touchState = RetouchTouchState()

    private val processedImagesMemento = Memento<Bitmap>()

    init {
        Log.d("RetouchViewModel", "init")
        removeObjectUC.initModel()
        interactiveSegmentationUC.initModel()

        launchInIO {
            processedImagesMemento.canUndoFlow.collectLatest { uiState.canUndo = it }
        }

        launchInIO {
            processedImagesMemento.canRedoFlow.collectLatest { uiState.canRedo = it }
        }
    }

    fun setData(uri: Uri) {
        val bitmap = uriToBitmap(context.contentResolver, uri)
        setData(bitmap)
    }
    fun setData(resId: Int) {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        setData(bitmap)
    }

    private fun setData(bitmap: Bitmap) {
        Log.d("RetouchViewModel", "setData")
        uiState.loading = true

        launchInIO {
            try {
                // Scale down bitmap if it's bigger than 2000 pixels
                val scaledBitmap = scaleDownBitmap(bitmap, 2000)

                uiState.originalImage = scaledBitmap
                imageState.image = scaledBitmap.asImageBitmap()

                imageState.image = bitmap.asImageBitmap()
                processedImagesMemento.saveState(bitmap)

                uiState.originalImage = bitmap
                uiState.currentImage = bitmap
                uiState.loading = false
            } catch (ex: Exception) {
                ex.printStackTrace()
                Firebase.crashlytics.recordException(ex)
                uiState.failure = false
            }
        }

    }

    fun exportImage(
        onSuccess: (Uri) -> Unit = {},
        onFailure: (String) -> Unit = {},
    ) {
        launchInDefault {
            try {
                val image = processedImagesMemento.currentState ?: uiState.originalImage
                uiState.saving = true
                uiState.exportedImage = Uri.EMPTY

                exportImageUC(
                    image = image,
                    format = Bitmap.CompressFormat.PNG,
                    onSuccess = { uri ->
                        uiState.saving = false
                        uiState.exportedImage = uri
                        launchInMain { onSuccess(uri) }
                    },
                    onFailure = {
                        uiState.saving = false
                        launchInMain { onFailure(context.getString(R.string.unexpected_error)) }
                    }
                )
            } catch (ex: Exception) {
                uiState.saving = false
                ex.printStackTrace()
                Firebase.crashlytics.recordException(ex)
                launchInMain {
                    onFailure(ex.localizedMessage ?: context.getString(R.string.unexpected_error))
                }
            }
        }
    }

    fun updateUiState(mode: RetouchMode) {
        Log.d("RetouchViewModel", "updateUiState")
        uiState.retouchMode = mode
        drawingState.currentPathProperty = RetouchPathProperties(strokeWidth = uiState.brushSize)
    }

    fun updateBrushSize(size: Float){
        Log.d("RetouchViewModel", "updateBrushSize")
        uiState.brushSize = size
        drawingState.currentPathProperty = RetouchPathProperties(strokeWidth = uiState.brushSize)
    }

    fun removeObjectDraw() {
        Log.d("RetouchViewModel", "removeObjectDraw")
        launchInDefault {
            uiState.loading = true
            val image = uiState.currentImage
            val drawingStateTemp = drawingState

            val bitmapMask = drawingStateTemp.createBitmap(image.size())
            val output = removeObjectUC(image, bitmapMask)

            processedImagesMemento.saveState(output)
            uiState.currentImage = output
            uiState.loading = false
            imageState.image = output.asImageBitmap()
            drawingState.apply {
                reset()
                currentPathProperty = RetouchPathProperties(strokeWidth = uiState.brushSize)
            }
        }
    }

    fun removeObjectFromPoint(offset: Offset) {
        Log.d("RetouchViewModel", "removeObjectFromPoint: $offset")
        /** x86 and x64 architectures are supported */
        val isSupportedArchitecture = Build.SUPPORTED_ABIS.firstOrNull() !in listOf("x86_64", "x86")

        /** Only support Android P and above */
        val isSupportedAndroidVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        if (!isSupportedAndroidVersion || !isSupportedArchitecture) return

        launchInDefault {
            uiState.loading = true
            val image = uiState.currentImage

            val point = offset.let { it.x to it.y }
            val matMask = interactiveSegmentationUC(image, listOf(point))
            val output = removeObjectUC(image, matMask)

            processedImagesMemento.saveState(output)
            uiState.currentImage = output
            uiState.loading = false
            imageState.image = output.asImageBitmap()
        }
    }


    fun undo() {
        val bitmap = processedImagesMemento.undo()
        bitmap?.let {
            uiState.currentImage = bitmap
            imageState.image = bitmap.asImageBitmap()
        }
    }

    fun redo() {
        val bitmap = processedImagesMemento.redo()
        bitmap?.let {
            uiState.currentImage = bitmap
            imageState.image = bitmap.asImageBitmap()
        }
    }

    fun reset() {
        Log.d("RetouchViewModel", "reset")
        uiState.reset()
        processedImagesMemento.reset()
        drawingState.reset()
        touchState.reset()
        imageState = ImageState()
    }

    override fun onCleared() {
        Log.d("RetouchViewModel", "onCleared")
        super.onCleared()

        removeObjectUC.release()
    }
}
