package com.photo.editor.ui.screen.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.snapshotFlow
import com.photo.editor.domain.usecase.ExportImageUC
import com.photo.editor.editimage.model.filter.FilterData
import com.photo.editor.editimage.util.cropToAspectRatio
import com.photo.editor.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.annotation.KoinViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SuppressLint("StaticFieldLeak")
@KoinViewModel
class CameraViewModel(
    private val context: Context,
    private val exportImageUC: ExportImageUC,
) : BaseViewModel() {
    val uiState = CameraUiState()
    private val imageProcessor = CameraImageProcessor()

    var screenAspectRatio = 1f
    var deviceRotationDegrees = 0

    init {
        imageProcessor.init(context)
        collectEditData()
    }

    fun onNewAnalysisFrame(imageProxy: ImageProxy) {
        if (uiState.imageCaptured) {
            imageProxy.close()
            return
        }

        launchInDefault {
            val bitmap = preprocessFrame(imageProxy)
            imageProcessor.setImage(bitmap)
            imageProcessor.processNewFrame()
            imageProxy.close()
        }
    }

    fun onNewCaptureFrame(imageProxy: ImageProxy) {
        uiState.imageCaptured = true

        launchInDefault {
            val bitmap = preprocessFrame(imageProxy)
            imageProcessor.setImage(bitmap)
            imageProcessor.processNewFrame()
            imageProxy.close()
        }
    }

    fun setPreviewSurface(surface: Surface?, width: Int, height: Int) {
        imageProcessor.setSurface(surface, width, height)
        if (width != 0 && height != 0) screenAspectRatio = width.toFloat() / height
    }

    private fun collectEditData() {
        launchInIO {
            snapshotFlow { uiState.selectedFilter to uiState.filterIntensity }.collectLatest { (filter, intensity) ->
                imageProcessor.setFilterData(FilterData(filter, intensity))
            }
        }
    }

    private fun preprocessFrame(imageProxy: ImageProxy): Bitmap {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val aspectRatio = if (rotationDegrees == 0 || rotationDegrees == 180) screenAspectRatio else 1 / screenAspectRatio
        val bitmap = imageProxy.toBitmap().cropToAspectRatio(aspectRatio)
        val transformMatrix = Matrix()
        if (uiState.cameraLensFacing == CameraSelector.LENS_FACING_FRONT) {
            if (rotationDegrees == 0 || rotationDegrees == 180) transformMatrix.postScale(-1f, 1f)
            else transformMatrix.postScale(1f, -1f)
        }
        transformMatrix.postRotate(rotationDegrees.toFloat())
        val transformedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transformMatrix, true)
        return transformedBitmap
    }

    fun saveCapturedImage(
        onSuccess: (Uri) -> Unit,
        onFailure: (String) -> Unit
    ) {
        launchInIO {
            val capturedBitmap = suspendCoroutine { continuation ->
                imageProcessor.extractFrame(onResult = { continuation.resume(it) })
            }

            exportImageUC(
                image = capturedBitmap,
                format = Bitmap.CompressFormat.JPEG,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        imageProcessor.release()
    }
}