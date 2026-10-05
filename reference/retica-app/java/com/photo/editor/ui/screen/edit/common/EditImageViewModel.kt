package com.photo.editor.ui.screen.edit.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.view.Surface
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withRotation
import androidx.core.graphics.withTranslation
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.photo.editor.R
import com.photo.editor.common.util.uriToBitmap
import com.photo.editor.domain.usecase.ExportImageUC
import com.photo.editor.editimage.model.EditImageData
import com.photo.editor.editimage.model.FaceLandmark
import com.photo.editor.localml.facelandmark.DetectFaceLandmarkUC
import com.photo.editor.ui.base.BaseViewModel
import com.photo.editor.ui.screen.edit.EditImageFragmentArgs
import com.photo.editor.ui.screen.edit.enums.EditImageBeautyOption
import com.photo.editor.ui.screen.edit.enums.EditImageEditOption
import com.photoeditor.opengl.util.Util.computeFillMaxDimension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import org.koin.android.annotation.KoinViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SuppressLint("StaticFieldLeak")
@KoinViewModel
class EditImageViewModel(
    savedStateHandle: SavedStateHandle,
    private val context: Context,
    private val exportImageUC: ExportImageUC,
    private val detectFaceLandmarkUC: DetectFaceLandmarkUC,
) : BaseViewModel() {
    companion object {
        var navigationAction: EditImageNavigationAction? = null
    }

    private val args = EditImageFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val uiState = EditImageUiState(context, viewModelScope)

    /** Main image processor that handles image processing pipeline
     * and draw output into surface */
    private val imageProcessor = EditImageProcessor()

    private var originalBitmap: Bitmap = createBitmap(1, 1)

    init {
        imageProcessor.init(context)

        // Handle input image
        launchInIO {
            originalBitmap = uriToBitmap(context.contentResolver, args.imageUri)
            uiState.originalBitmap = originalBitmap
            imageProcessor.setImage(originalBitmap)
            detectFaceLandmark()
            imageProcessor.processNewFrame()
        }

        handleNavigationAction()

        collectTransformState()
        collectEditData()
    }

    fun setSurface(surface: Surface?, width: Int, height: Int) {
        imageProcessor.setSurface(surface, width, height)
        imageProcessor.processNewFrame()
    }

    private fun handleNavigationAction() {
        when (val navigationAction = navigationAction) {
            EditImageNavigationAction.OpenAdjust -> uiState.selectedEditOption = EditImageEditOption.Adjust
            EditImageNavigationAction.OpenBeauty -> uiState.isSelectedEditOption = false
            EditImageNavigationAction.OpenFilter -> uiState.selectedEditOption = EditImageEditOption.Filter
            EditImageNavigationAction.OpenFrame -> uiState.selectedEditOption = EditImageEditOption.Frame
            EditImageNavigationAction.OpenMakeup -> {
                uiState.isSelectedEditOption = false
                uiState.selectedBeautyOption = EditImageBeautyOption.Makeup
            }

            EditImageNavigationAction.OpenSticker -> uiState.selectedEditOption = EditImageEditOption.Sticker
            EditImageNavigationAction.OpenBrush -> uiState.selectedEditOption = EditImageEditOption.Brush

            is EditImageNavigationAction.UseFilter -> {
                uiState.selectedEditOption = EditImageEditOption.Filter
                uiState.setFilterData(navigationAction.filter, 1f)
            }

            is EditImageNavigationAction.UseFrame -> {
                uiState.selectedEditOption = EditImageEditOption.Frame
                uiState.setFrame(navigationAction.frame)
            }

            else -> {}
        }

        navigationAction = null
    }

    private fun collectTransformState() {
        launchInIO {
            snapshotFlow {
                Triple(uiState.transformState.scale, uiState.transformState.translateX, uiState.transformState.translateY)
            }.collectLatest { (scale, translateX, translateY) ->
                imageProcessor.setScaleAndTranslate(scale, translateX, translateY)
            }
        }
    }

    private fun collectEditData() {
        launchInDefault {
            val cropFlow = uiState.cropState.cropDataFlow

            val filterFlow = uiState.filterDataFlow
            val adjustFlow = uiState.adjustState.adjustDataFlow
            val colorFlow = uiState.colorState.colorDataFlow
            val frameFlow = uiState.frameFlow

            val beautyFlow = uiState.beautyState.beautyDataFlow
            val makeupFlow = uiState.beautyState.makeupDataFlow

            combine(
                cropFlow,
                filterFlow,
                adjustFlow,
                colorFlow,
                beautyFlow,
                makeupFlow,
                frameFlow,
            ) { crop, filter, adjust, color, beauty, makeup, frame ->
                EditImageData(
                    crop = crop,
                    filter = filter,
                    adjust = adjust,
                    color = color,
                    frame = frame,
                    beauty = beauty,
                    makeup = makeup,
                )
            }.collectLatest { editData ->
                imageProcessor.setEditData(
                    faceLandmarks = uiState.faceLandmarks,
                    editImageData = editData,
                )
                imageProcessor.processNewFrameBlocking()
            }
        }

        launchInDefault {
            uiState.showOriginalImageFlow.collectLatest {
                if (it) imageProcessor.disableEffects()
                else imageProcessor.enableEffects()
            }
        }
    }

    private fun detectFaceLandmark() {
        launchInDefault {
            uiState.isDetectingFaceLandmark = true

            uiState.faceLandmarks = detectFaceLandmarkUC(originalBitmap).map { FaceLandmark(it) }

            uiState.faceLandmarkDetected = true
            uiState.isDetectingFaceLandmark = false
        }
    }

    fun exportImage(
        onSuccess: (Uri) -> Unit = {},
        onFailure: (String) -> Unit = {},
    ) {
        launchInIO {
            uiState.exportingImage = true

            val combinedImage = getCombinedImage()

            exportImageUC(
                image = combinedImage,
                format = Bitmap.CompressFormat.JPEG, // TODO: Determine if image has alpha and save as PNG if it does
                onSuccess = { uri ->
                    uiState.exportedImage = uri
                    onSuccess(uri)
                },
                onFailure = { onFailure(context.getString(R.string.unexpected_error)) }
            )

            uiState.exportingImage = false
        }
    }

    /** Get edited image with overlays and brush */
    private suspend fun getCombinedImage(): Bitmap {
        val bitmap = suspendCoroutine { continuation ->
            imageProcessor.extractFrame { bitmap -> continuation.resume(bitmap) }
        }.let { it.copy(it.config ?: Bitmap.Config.ARGB_8888, true) }

        drawBrushOnImage(bitmap)
        drawOverlayOnImage(bitmap)

        return bitmap
    }

    private fun drawBrushOnImage(image: Bitmap) {
        val canvas = Canvas(image)

        // Create brush bitmap, scale it to fill the image
        var brushBitmap = uiState.brushDrawingState.getBitmap(context)
        val brushScaleFactor = computeFillMaxDimension(
            srcWidth = brushBitmap.width.toFloat(),
            srcHeight = brushBitmap.height.toFloat(),
            dstWidth = image.width.toFloat(),
            dstHeight = image.height.toFloat()
        )
        brushBitmap = brushBitmap.scale(
            (brushBitmap.width * brushScaleFactor).toInt(),
            (brushBitmap.height * brushScaleFactor).toInt()
        )
        // Draw brush bitmap onto the image
        canvas.drawBitmap(
            brushBitmap,
            image.width / 2f - brushBitmap.width / 2f,
            image.height / 2f - brushBitmap.height / 2f,
            Paint().apply { isFilterBitmap = true }
        )

    }

    private fun drawOverlayOnImage(image: Bitmap) {
        val canvas = Canvas(image)

        // Create overlay bitmap and draw overlays onto it
        val surfaceSize = uiState.transformState.surfaceSize
        var overlayBitmap = createBitmap(surfaceSize.width.toInt(), surfaceSize.height.toInt())
        val overlayBitmapCanvas = Canvas(overlayBitmap)
        uiState.overlayState.overlayStates.value.forEach { overlayState ->
            overlayBitmapCanvas.apply {
                withTranslation(
                    x = overlayState.centerOffset.x.toFloat(),
                    y = overlayState.centerOffset.y.toFloat(),
                ) {
                    withRotation(
                        degrees = overlayState.rotationDegrees,
                        pivotX = overlayState.size.width / 2f,
                        pivotY = overlayState.size.height / 2f,
                    ) {
                        drawBitmap(
                            overlayState.overlayImage.asAndroidBitmap(),
                            null,
                            android.graphics.Rect(0, 0, overlayState.size.width, overlayState.size.height),
                            null,
                        )
                    }
                }
            }
        }

        val overlayFactor = computeFillMaxDimension(
            srcWidth = overlayBitmap.width.toFloat(),
            srcHeight = overlayBitmap.height.toFloat(),
            dstWidth = image.width.toFloat(),
            dstHeight = image.height.toFloat()
        )

        overlayBitmap = overlayBitmap.scale((overlayBitmap.width * overlayFactor).toInt(), (overlayBitmap.height * overlayFactor).toInt())
        // Draw overlay bitmap onto the image
        canvas.drawBitmap(
            overlayBitmap,
            image.width / 2f - overlayBitmap.width / 2f,
            image.height / 2f - overlayBitmap.height / 2f,
            Paint().apply { isFilterBitmap = true }
        )
    }

    /**
     * Extension function to combine 7 flows
     * Returns a [Flow] whose values are generated with [transform] function by combining
     * the most recently emitted values by each flow.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
        flow1: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        flow7: Flow<T7>,
        transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R
    ): Flow<R> = combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
            args[6] as T7,
        )
    }
}