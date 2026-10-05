package com.photo.editor.ui.screen.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.photo.editor.AppGraphDirections
import com.photo.editor.R
import com.photo.editor.common.util.logEvent
import com.photo.editor.common.util.tryWithoutCatch
import com.photo.editor.domain.model.FlashMode
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.ui.base.BaseComposeFragment
import com.photo.editor.ui.base.BaseScreen
import com.photo.editor.ui.navigation.safeNavigate
import com.photo.editor.ui.navigation.safePopBackstack
import com.photo.editor.ui.screen.camera.component.CameraBottomBar
import com.photo.editor.ui.screen.camera.component.CameraCapturePreviewBottomBar
import com.photo.editor.ui.screen.camera.component.CameraTopBar
import com.photo.editor.ui.screen.camera.util.drawCameraOverlay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class CameraFragment : BaseComposeFragment(), SensorEventListener {
    private val viewModel: CameraViewModel by viewModel()
    private val uiState: CameraUiState
        get() = viewModel.uiState

    private lateinit var cameraExecutor: ExecutorService
    private var previewSurface: SurfaceView? by mutableStateOf(null)
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var zoomListenerJob: Job? = null

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Set up sensor to calculate device rotation degrees
        sensorManager = context.getSystemService(SensorManager::class.java)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCamera()
        initPreviewSurface()
    }

    override fun onScreenView() {
        super.onScreenView()
        logEvent("camera_scr")
    }

    @SuppressLint("RestrictedApi")
    private fun initCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            // Camera provider is now guaranteed to be available
            cameraProvider = cameraProviderFuture.get()

            // Set up the capture use case to allow users to take photos.
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1920, 1080),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                        )
                        .build()
                )
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        viewModel.onNewAnalysisFrame(imageProxy)
                    }
                }

            // Choose the camera by requiring a lens facing
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(uiState.cameraLensFacing)
                .build()

            // Must unbind the use-cases before rebinding them
            cameraProvider?.unbindAll()

            tryWithoutCatch {
                camera = cameraProvider?.bindToLifecycle(this, cameraSelector, imageCapture, imageAnalyzer)
            }

            zoomListenerJob?.cancel()
            zoomListenerJob = lifecycleScope.launch {
                camera?.cameraInfo?.zoomState?.asFlow()?.collectLatest { zoomState ->
//                    linearZoom = zoomState.linearZoom
                    uiState.zoomRatio = zoomState.zoomRatio
                    uiState.maximumZoomRatio = zoomState.maxZoomRatio
                    uiState.minZoomRatio = zoomState.minZoomRatio
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun initPreviewSurface() {
        previewSurface = SurfaceView(requireContext()).apply {
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    viewModel.setPreviewSurface(holder.surface, width, height)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    viewModel.setPreviewSurface(null, 0, 0)
                }
            })
        }
    }

    @SuppressLint("RestrictedApi")
    private fun captureImage() {
        imageCapture?.flashMode = when (uiState.flashMode) {
            FlashMode.On -> ImageCapture.FLASH_MODE_ON
            FlashMode.Off -> ImageCapture.FLASH_MODE_OFF
            FlashMode.Auto -> ImageCapture.FLASH_MODE_AUTO
        }
        imageCapture?.takePicture(cameraExecutor, object : OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                viewModel.onNewCaptureFrame(image)
            }
        })
    }

    private fun switchCamera() {
        uiState.cameraLensFacing = if (uiState.cameraLensFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        initCamera()
    }

    @Composable
    override fun ComposeView() {
        CameraScreen(
            uiState = uiState,
            interaction = CameraInteraction(
                onBackClick = {
                    logEvent("camera_scr_back_click")
                    if (uiState.imageCaptured) uiState.imageCaptured = false
                    else safePopBackstack()
                },
                onCaptureClick = {
                    logEvent("camera_scr_capture_click")
                    captureImage()
                },
                onLibraryClick = {
                    logEvent("camera_scr_library_click")
                    safeNavigate(AppGraphDirections.actionGallery())
                },
                onCameraZoomChange = { zoomRatio ->
                    camera?.cameraControl?.setZoomRatio(zoomRatio)
                },
                onSwitchCameraClick = ::switchCamera,
                onConfirmCapturedImage = {
                    logEvent("camera_scr_confirm_click")
                    viewModel.saveCapturedImage(
                        onSuccess = { uri ->
                            runOnUiThread { safeNavigate(CameraFragmentDirections.actionCameraToEdit(uri)) }
                        },
                        onFailure = {
                            runOnUiThread { toast(R.string.unexpected_error) }
                        }
                    )
                },
                onDismissCapturedImage = {
                    logEvent("camera_scr_cancel_click")
                    uiState.imageCaptured = false
                },
            ),
            surfaceView = previewSurface,
        )
    }

    override fun onResume() {
        super.onResume()

        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()

        sensorManager.unregisterListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        cameraExecutor.shutdown()
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Calculate device rotation degrees based on sensor data
        if (event.sensor == accelerometer) {
            if (abs(event.values[1]) > abs(event.values[0])) {
                // Mainly portrait
                if (event.values[1] > 1) {
                    // Portrait
                    viewModel.deviceRotationDegrees = 0
                } else if (event.values[1] < -1) {
                    // Inverse Portrait
                    viewModel.deviceRotationDegrees = 180
                }
            } else {
                // Mainly landscape
                if (event.values[0] > 1) {
                    // Landscape - right side up
                    viewModel.deviceRotationDegrees = 270
                } else if (event.values[0] < -1) {
                    // Landscape - left side up
                    viewModel.deviceRotationDegrees = 90
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }
}

@Composable
private fun CameraScreen(
    uiState: CameraUiState = CameraUiState(),
    interaction: CameraInteraction = CameraInteraction(),
    surfaceView: SurfaceView? = null,
) {
    BackHandler { interaction.onBackClick() }

    BaseScreen(
        backgroundColor = Color.Black,
        bottomBar = {
            BaseBannerAdContent(AdsProvider.bannerAll)
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { surfaceView ?: View(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                val nextZoom = (uiState.zoomRatio * zoom).coerceIn(uiState.minZoomRatio, uiState.maximumZoomRatio)
                                interaction.onCameraZoomChange(nextZoom)
                            }
                        }
                    },
            )

            if (LocalInspectionMode.current) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFB6B2AF))
                )
            }

            // Aspect ratio overlay
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawCameraOverlay(
                        aspectRatio = uiState.aspectRatio,
                        overlayColor = if (uiState.imageCaptured) Color.Black else Color.Black.copy(0.55f)
                    )
            )

            if (uiState.imageCaptured) {
                CameraCapturePreviewBottomBar(
                    onConfirmCapturedImage = interaction.onConfirmCapturedImage,
                    onDismissCapturedImage = interaction.onDismissCapturedImage,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                CameraBottomBar(
                    uiState = uiState,
                    onCaptureClick = interaction.onCaptureClick,
                    onLibraryClick = interaction.onLibraryClick,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                CameraTopBar(
                    uiState = uiState,
                    interaction = interaction,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    CameraScreen()
}