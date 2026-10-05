package com.photo.editor.ui.screen.removebg.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.photo.editor.R
import com.photo.editor.common.ui.transformableimage.ImageState
import com.photo.editor.common.util.resourceToBitmap
import com.photo.editor.common.util.scaleDownBitmap
import com.photo.editor.common.util.uriToBitmap
import com.photo.editor.domain.usecase.ExportImageUC
import com.photo.editor.localml.removebackground.ForegroundToMaskUC
import com.photo.editor.localml.removebackground.RemoveBackgroundUC
import com.photo.editor.ui.base.BaseViewModel
import com.photo.editor.ui.screen.removebg.edit.component.refine.RefineDrawingState
import com.photo.editor.util.computeFillMaxDimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.opencv.android.Utils
import org.opencv.core.Mat

@SuppressLint("StaticFieldLeak")
@KoinViewModel
class RemoveBgViewModel(
    private val context: Context,
    private val exportImageUC: ExportImageUC,
    private val removeBackgroundUC: RemoveBackgroundUC,
    private val foregroundToMaskUC: ForegroundToMaskUC,
) : BaseViewModel() {
    val uiState = RemoveBgUiState()
    val imageState = ImageState()
    val drawingState: RefineDrawingState? = null

    private var originalImageMat: Mat? = null
    private var currentForegroundMask: Mat? = null
    private var _processedImage = MutableStateFlow<ImageBitmap?>(null)

    fun setData(uri: Uri) {
        val bitmap = uriToBitmap(context.contentResolver, uri)
        setData(bitmap)
    }

    fun setData(imageId: Int) {
        val bitmap = resourceToBitmap(context, imageId)
        setData(bitmap)
    }

    private fun setData(bitmap: Bitmap) {

        uiState.loading = true

        launchInIO {
            try {
                // Scale down bitmap if it's bigger than 2000 pixels
                val scaledBitmap = scaleDownBitmap(bitmap, 2000)

                uiState.originalImage = scaledBitmap

                imageState.image = scaledBitmap.asImageBitmap()

                val foreground = withContext(Dispatchers.Default) {
                    removeBackgroundUC.invoke(scaledBitmap) ?: throw RuntimeException("Failed to remove background")
                }

                foreground.let {
                    uiState.currentImage = it
                    imageState.image = it.asImageBitmap()
                }

                originalImageMat = Mat().apply { Utils.bitmapToMat(scaledBitmap, this) }

                currentForegroundMask = foregroundToMaskUC.invoke(foreground)

                uiState.loading = false
            } catch (ex: Exception) {
                ex.printStackTrace()
                Firebase.crashlytics.recordException(ex)
                uiState.loading = false
            }
        }
    }

    fun exportImage(
        onSuccess: (Uri) -> Unit = {},
        onFailure: (String) -> Unit = {},
    ) {
        launchInDefault {
            try {
                uiState.saving = true
                uiState.exportedImage = Uri.EMPTY

                createImage()

                val image = _processedImage.value ?: uiState.originalImage.asImageBitmap()

                exportImageUC(
                    image = image.asAndroidBitmap(),
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

    private fun createImage() {

        val foreground = uiState.currentImage

        // Build the image
        _processedImage.value = uiState.background
            .let {
                val baseBackground = createBitmap(foreground.width, foreground.height)
                val canvas = Canvas(baseBackground)

                // Draw background
                when (val currentBackground = uiState.background) {
                    is RemoveBGBackground.SolidColor -> {
                        canvas.drawColor(currentBackground.color.toInt())
                    }

                    is RemoveBGBackground.Image,
                    is RemoveBGBackground.Scene,
                    is RemoveBGBackground.Background,
                        -> {

                        val backgroundImage = when (val currentBg = uiState.background) {
                            is RemoveBGBackground.Image -> {
                                uriToBitmap(
                                    context.contentResolver,
                                    currentBg.uri
                                ).copy(Bitmap.Config.ARGB_8888, true)
                            }

                            is RemoveBGBackground.Scene -> {
                                resourceToBitmap(context, currentBg.scene.imageId)
                            }

                            is RemoveBGBackground.Background -> {
                                resourceToBitmap(context, currentBg.background.imageId)
                            }

                            else -> throw Exception("Unexpected error")
                        }

                        val scaleFactor = computeFillMaxDimension(
                            backgroundImage.width.toDouble(),
                            backgroundImage.height.toDouble(),
                            foreground.width.toDouble(),
                            foreground.height.toDouble(),
                        )

                        // Calculate the upper left coordinates if the scaled bitmap
                        val left = (backgroundImage.width - foreground.width / scaleFactor) / 2
                        val top = (backgroundImage.height - foreground.height / scaleFactor) / 2
                        val scaledWidth = foreground.width / scaleFactor
                        val scaledHeight = foreground.height / scaleFactor

                        val srcRect =
                            Rect(left.toInt(), top.toInt(), (left + scaledWidth).toInt(), (top + scaledHeight).toInt())
                        val dstRect = Rect(0, 0, foreground.width, foreground.height)
                        canvas.drawBitmap(backgroundImage, srcRect, dstRect, null)
                    }
                }

                // Draw foreground
                canvas.drawBitmap(foreground, 0f, 0f, null)
                baseBackground
            }
            .asImageBitmap()
    }
}