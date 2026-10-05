package com.photo.editor.ui.screen.artistic.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.photo.editor.common.util.assetToBitmap
import com.photo.editor.common.util.scaleDownBitmap
import com.photo.editor.common.util.uriToBitmap
import com.photo.editor.domain.usecase.ExportImageUC
import com.photo.editor.localml.styletransfer.StyleTransferInceptionV3BlendUC
import com.photo.editor.ui.base.BaseViewModel
import com.photo.editor.ui.screen.artistic.model.ArtisticStyle
import org.koin.android.annotation.KoinViewModel
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
@KoinViewModel
class ArtisticViewModel(
    private val context: Context,
    private val exportImageUC: ExportImageUC,
    private val styleTransferInceptionV3BlendUC: StyleTransferInceptionV3BlendUC,
) : BaseViewModel() {
    val uiState = ArtisticUiState()

    private val processing = AtomicBoolean(false)
    private val queuedProcessing = AtomicBoolean(false)

    init {
        styleTransferInceptionV3BlendUC.initModel(viewModelScope)
    }

    fun setOriginalImage(uri: Uri) {
        setOriginalImage(uriToBitmap(context.contentResolver, uri))
    }

    private fun setOriginalImage(originalBitmap: Bitmap) {
        val scaledDownBitmap = scaleDownBitmap(originalBitmap, 2000)
        uiState.originalBitmap = scaledDownBitmap
        uiState.generatedBitmap = scaledDownBitmap
        processImage()
    }

    fun selectStyle(style: ArtisticStyle?) {
        if (style == null) {
            uiState.selectedStyle = null
            uiState.showOriginalImage = true
            return
        }
        uiState.showOriginalImage = false
        uiState.selectedStyle = style
        processImage()
    }

    fun updateBlendRatio(blendRatio: Float) {
        uiState.blendRatio = blendRatio
        processImage()
    }

    private fun processImage() {
        if (uiState.selectedStyle == null) return
        launchInDefault {
            if (processing.getAndSet(true)) {
                // A process is running, request to reprocess after current process is done
                queuedProcessing.set(true)
                return@launchInDefault
            }
            uiState.processing = true

            val style = uiState.selectedStyle
            val originalBitmap = uiState.originalBitmap
            val ratio = uiState.blendRatio

            val styleImage = style?.let { assetToBitmap(context, it.assetPath) }!!
            val generatedBitmap = styleTransferInceptionV3BlendUC(originalBitmap, styleImage, ratio)
            uiState.generatedBitmap = generatedBitmap

            uiState.processing = false

            processing.set(false)
            if (queuedProcessing.getAndSet(false)) {
                // Start reprocessing if there was a request to reprocess during previous processing
                processImage()
            }
        }
    }

    fun exportImage(onSuccess: (Uri) -> Unit, onFailure: (String) -> Unit) {
        launchInIO {
            val image = uiState.generatedBitmap
            exportImageUC(
                image = image,
                format = Bitmap.CompressFormat.JPEG,
                onSuccess = { uri ->
                    uiState.exportedImage = uri
                    onSuccess(uri)
                },
                onFailure = onFailure,
            )
        }
    }
}