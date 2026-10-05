package com.photo.editor.ui.screen.enhance.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.photo.editor.common.util.assetToBitmap
import com.photo.editor.common.util.scaleDownBitmap
import com.photo.editor.common.util.uriToBitmap
import com.photo.editor.domain.usecase.ExportImageUC
import com.photo.editor.localml.enhance.EnhanceImageUC
import com.photo.editor.localml.enhance.ProgressStatus
import com.photo.editor.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.job
import org.koin.android.annotation.KoinViewModel
import kotlin.coroutines.coroutineContext

@SuppressLint("StaticFieldLeak")
@KoinViewModel
class EnhanceViewModel(
    private val context: Context,
    private val enhanceImageUC: EnhanceImageUC,
    private val exportImageUC: ExportImageUC,
) : BaseViewModel() {
    val uiState = EnhanceUiState()

    init {
        enhanceImageUC.initModel()
    }

    fun setImage(assetPath: String) {
        launchInIO {
            uiState.loading = true
            val bitmap = assetToBitmap(context, assetPath) ?: return@launchInIO
            setImage(bitmap)
        }
    }

    fun setImage(imageUri: Uri) {
        launchInIO {
            uiState.loading = true
            val bitmap = uriToBitmap(context.contentResolver, imageUri)
            setImage(bitmap)
        }
    }

    fun setImage(bitmap: Bitmap) {
        launchInIO {
            uiState.loading = true
//            uiState.originalBitmap = bitmap
            // Scale down image for displaying purpose
            val scaledOriginalBitmap = scaleDownBitmap(bitmap, 2000)
            uiState.scaledOriginalImage = scaledOriginalBitmap
            uiState.scaledEnhancedImage = scaledOriginalBitmap
            startEnhance(bitmap = bitmap)
        }
    }

    private fun startEnhance(bitmap: Bitmap) {
        launchInIO {

            enhanceImageUC(bitmap).collectLatest { status ->
                when (status) {
                    is ProgressStatus.None -> {}

                    is ProgressStatus.Processing -> {
                        uiState.enhanceProgress = status.progress
                    }

                    is ProgressStatus.Success -> {
//                        uiState.scaledEnhancedImage = status.result
                        // Scale down image for displaying purpose
                        val scaledEnhancedBitmap = scaleDownBitmap(status.result, 2000)

                        uiState.loading = false
                        uiState.enhanceProgress = 1f
                        uiState.scaledEnhancedImage = scaledEnhancedBitmap

                        // Cancel this coroutine after result has returned
                        coroutineContext.job.cancel()
                    }

                    is ProgressStatus.Error -> {
                        uiState.loading = false
                        uiState.failure = true
                        // Cancel this coroutine after result has returned
                        coroutineContext.job.cancel()
                    }
                }
            }
        }
    }

    fun exportImage(onSuccess: (Uri) -> Unit, onFailure: (String) -> Unit) {
        launchInIO {
            val image = uiState.scaledEnhancedImage
            exportImageUC.invoke(
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