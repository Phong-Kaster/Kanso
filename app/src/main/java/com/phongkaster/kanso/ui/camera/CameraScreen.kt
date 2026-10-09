package com.phongkaster.kanso.ui.camera

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.common.BackArrowIcon
import com.phongkaster.kanso.ui.permission.AccessRefusedPanel
import com.phongkaster.kanso.ui.permission.PermissionGate
import com.phongkaster.kanso.ui.permission.openAppDetailsSettings
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.awaitCancellation
import java.io.File

/** The test tag on the root of the camera screen, present in every permission state. */
const val CAMERA_SCREEN_TAG = "camera-screen"

/** The test tag on the live camera picture. */
const val CAMERA_PREVIEW_TAG = "camera-preview"

/** The test tag on the shutter button, shown while the live picture is on screen. */
const val CAMERA_SHUTTER_TAG = "camera-shutter"

/** The test tag on the picture of the photo just taken, shown while Keep and Retake are. */
const val CAMERA_SHOT_TAG = "camera-shot"

/** The test tag on the Keep button. */
const val CAMERA_KEEP_TAG = "camera-keep"

/** The test tag on the Retake button. */
const val CAMERA_RETAKE_TAG = "camera-retake"

/** The test tag on the "camera access is off" explanation. */
const val CAMERA_REFUSED_TAG = "camera-refused"

/** The test tag on the button that opens the app's page in the system settings. */
const val CAMERA_SETTINGS_BUTTON_TAG = "camera-settings-button"

/** The test tag on the message shown when the camera cannot be started or a shot failed. */
const val CAMERA_MESSAGE_TAG = "camera-message"

/** The test tag on the button that swaps between the back and front lens. */
const val CAMERA_SWITCH_LENS_TAG = "camera-switch-lens"

/** The test tag on the flash button, shown only when the lens in use has a flash. */
const val CAMERA_FLASH_TAG = "camera-flash"

/** The test tag on the area over the live picture that reacts to a two-finger pinch (zoom). */
const val CAMERA_ZOOM_AREA_TAG = "camera-zoom-area"

/** The size of the round shutter button; well above the 48 dp smallest touch target. */
private val SHUTTER_SIZE = 72.dp

/** The smallest height of the Keep and Retake buttons: Android's smallest touch target. */
private val MIN_TOUCH = 48.dp

/**
 * The camera screen: a top bar with a back arrow and the title "Camera", a live picture from the
 * camera (pinch it to zoom), and a bar at the bottom with the flash button, the shutter and the
 * lens switch. After a photo is taken the live picture is replaced by the photo, and the bar
 * offers Keep and Retake.
 *
 * The first time it is shown it asks for camera access through [PermissionGate] (the app never
 * asks at launch). After a refusal it explains why the camera is needed and offers a button to
 * the app's system settings, like the gallery does.
 *
 * Keep calls [onKept] with the photo's `file://` Uri (as text); the caller opens the tool on it.
 * Retake deletes the photo and shows the live picture again. [onBack] is called when the user
 * taps the back arrow; the caller pops the back stack. The [viewModel] is passed in by the
 * navigation entry, which owns it.
 *
 * Like Home, the camera is cut into a stateful part and a stateless part (the "state hoisting"
 * explained on `HomeScreen`). What is specific here: the stateful side is this function, which
 * asks for the permission, plus [CameraBody], which reads the screen's one state from the
 * [viewModel]; the stateless side is [CameraFrame] (the top bar) and [CameraContent] (the picture
 * area and the bottom bar). The live camera picture and the photo just taken are handed to
 * [CameraContent] as small composable "slots" (a piece of UI passed in as a parameter), so a
 * preview can pass a drawn stand-in where the real screen passes the camera.
 */
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onKept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    CameraFrame(onBack = onBack, modifier = modifier) {
        PermissionGate(
            permissions = listOf(Manifest.permission.CAMERA),
            granted = {
                CameraBody(
                    viewModel = viewModel,
                    onKept = onKept,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            refused = {
                CameraRefusedPanel(onOpenSettings = { context.openAppDetailsSettings() })
            },
        )
    }
}

/**
 * The frame around everything the camera screen shows: a top bar with a back arrow (which calls
 * [onBack]) and the title "Camera", and below it the [content], which fills the rest.
 *
 * It holds no state, so both the real screen (with the permission gate as its [content]) and the
 * previews (with a sample picture, or the refused panel) use it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraFrame(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.testTag(CAMERA_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tool_camera)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = BackArrowIcon,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar and the system bars take, the gesture bar at the
        // bottom included. consumeWindowInsets tells the content below that those insets are
        // already dealt with, so a navigationBarsPadding further down does not add them twice.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            content()
        }
    }
}

/**
 * What the camera screen shows once access was refused: why the camera is needed, and a button
 * that calls [onOpenSettings] to open the app's page in the system settings.
 */
@Composable
private fun CameraRefusedPanel(onOpenSettings: () -> Unit) {
    AccessRefusedPanel(
        title = stringResource(R.string.camera_refused_title),
        message = stringResource(R.string.camera_refused_message),
        buttonLabel = stringResource(R.string.camera_open_settings),
        onOpenSettings = onOpenSettings,
        modifier = Modifier
            .navigationBarsPadding()
            .testTag(CAMERA_REFUSED_TAG),
        buttonModifier = Modifier.testTag(CAMERA_SETTINGS_BUTTON_TAG),
    )
}

/**
 * The stateful part of the screen once camera access is granted: it reads the screen's one state
 * (the camera and the photo just taken) from [viewModel] and hands it to [CameraContent].
 *
 * It also decides which picture goes in the picture area: the live camera picture, connected
 * through the [viewModel], while no photo waits; the photo file once one does. Keep calls
 * [onKept] with the photo's `file://` Uri as text.
 */
@Composable
private fun CameraBody(
    viewModel: CameraViewModel,
    onKept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // collectAsState turns the StateFlow into Compose state: when it changes, this body is
    // drawn again with the new value.
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val shot = state.shot

    // A slot only while a photo waits; null means "show the live picture". The declared type
    // tells Kotlin that the lambda below is a composable taking the picture's modifier.
    val shotPicture: (@Composable (Modifier) -> Unit)? = if (shot == null) {
        null
    } else {
        { pictureModifier -> ShotView(shot = shot, modifier = pictureModifier) }
    }

    CameraContent(
        state = state,
        onShutter = { viewModel.takePicture(shotsDirectory(context)) },
        onKeep = { viewModel.keep()?.let(onKept) },
        onRetake = viewModel::retake,
        onFlash = viewModel::cycleFlash,
        onSwitchLens = viewModel::switchLens,
        onZoom = viewModel::zoomBy,
        livePicture = { pictureModifier ->
            LivePreview(viewModel = viewModel, modifier = pictureModifier)
        },
        shotPicture = shotPicture,
        modifier = modifier,
    )
}

/**
 * The stateless part of the screen once camera access is granted: a black picture area above the
 * [CameraBottomBar], drawn from [state], the camera screen's one snapshot.
 *
 * While [shotPicture] is null the area shows [livePicture] with the pinch-to-zoom layer on top
 * (a pinch calls [onZoom]); once a photo waits, [shotPicture] is shown instead and the bar
 * offers Keep ([onKeep]) and Retake ([onRetake]). Both pictures are slots: each is called with
 * the modifier that sizes it, and draws whatever its caller chose. The real screen passes a
 * [shotPicture] exactly when [state] holds a shot, so the slot is how this part knows a photo
 * waits.
 *
 * The shutter and the lens switch work only when the camera in [state] is ready and no photo is
 * being taken. The flash button shows the flash mode, and only when the lens has a flash; the lens
 * switch shows only when the phone can switch lenses. A message is shown in the bar when the last
 * shot failed, or when the camera is unavailable and no photo waits. [onShutter], [onFlash] and
 * [onSwitchLens] are called by their buttons.
 *
 * The picture area is black because a camera picture rarely fills the space exactly, and black
 * edges look like a viewfinder in light and dark themes alike. Nothing readable is drawn on the
 * black; every label and message sits in the bottom bar, on the theme's surface colour.
 */
@Composable
fun CameraContent(
    state: CameraUiState,
    onShutter: () -> Unit,
    onKeep: () -> Unit,
    onRetake: () -> Unit,
    onFlash: () -> Unit,
    onSwitchLens: () -> Unit,
    onZoom: (Float) -> Unit,
    livePicture: @Composable (Modifier) -> Unit,
    shotPicture: (@Composable (Modifier) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val camera = state.camera
    // A failed shot is the most important thing to say; "unavailable" only matters while the
    // live picture would be on screen, not while a photo waits.
    val message = when {
        state.shotFailed -> stringResource(R.string.camera_shot_failed)
        shotPicture == null && camera.status == CameraStatus.Unavailable ->
            stringResource(R.string.camera_unavailable)
        else -> null
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            // While a photo waits for Keep or Retake the live picture leaves the screen, which
            // also disconnects the camera (see LivePreview); Retake brings it back.
            if (shotPicture == null) {
                livePicture(Modifier.fillMaxSize())
                PinchToZoomArea(onZoom = onZoom, modifier = Modifier.matchParentSize())
            } else {
                shotPicture(Modifier.fillMaxSize())
            }
        }

        val ready = camera.status == CameraStatus.Ready && !state.taking
        CameraBottomBar(
            hasShot = shotPicture != null,
            shutterEnabled = ready,
            message = message,
            onShutter = onShutter,
            onKeep = onKeep,
            onRetake = onRetake,
            flashMode = camera.flashMode.takeIf { camera.hasFlash },
            onFlash = onFlash,
            canSwitchLens = camera.canSwitchLens,
            switchEnabled = ready,
            onSwitchLens = onSwitchLens,
        )
    }
}

/**
 * The live camera picture, drawn by CameraX's PreviewView and connected to the camera while it
 * is on screen.
 *
 * PreviewView is a classic Android View, not a composable. AndroidView is the bridge: its
 * factory builds the View once, and Compose places and sizes it like any other element. The
 * view is created with `remember` so that the same one is used for the connection below.
 * COMPATIBLE mode makes PreviewView draw through a TextureView, which moves, clips and animates
 * correctly inside Compose (the default SurfaceView can show through other content).
 *
 * The connection: LaunchedEffect runs while this composable is on screen. It connects the
 * camera to the preview's surface provider and the screen's lifecycle owner, then waits; when
 * the composable leaves the screen the wait is cancelled and `finally` disconnects the camera.
 * If the screen leaves while the camera is still opening, the connection is cancelled before it
 * is made.
 */
@Composable
private fun LivePreview(viewModel: CameraViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            // FILL_CENTER fills the whole area and crops the edges, like the phone's camera app.
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(viewModel, lifecycleOwner, previewView) {
        try {
            viewModel.bind(lifecycleOwner, previewView.surfaceProvider)
            awaitCancellation()
        } finally {
            viewModel.unbind()
        }
    }

    val description = stringResource(R.string.camera_preview_description)
    AndroidView(
        factory = { previewView },
        modifier = modifier
            .testTag(CAMERA_PREVIEW_TAG)
            .semantics { contentDescription = description },
    )
}

/**
 * A see-through layer over the live picture that turns a two-finger pinch into zoom: spreading
 * the fingers zooms in, closing them zooms out. It calls [onZoom] with how much the fingers
 * spread since the last call (1.1 means ten percent wider), many times per pinch.
 *
 * How the gesture is read: `pointerInput` gives raw access to the fingers touching this area,
 * and `detectTransformGestures` watches them and works out, for each small movement, how far
 * they slid, turned and spread. Only the spread (the "zoom" value) is used here; the slide and
 * turn are ignored. The layer sits on top of the camera picture rather than on it, because the
 * picture is a classic Android View that could otherwise take the touches for itself.
 *
 * Nothing is drawn, so the picture shows through. The layer covers only the picture area, not
 * the bottom bar, so the buttons there keep their taps.
 */
@Composable
private fun PinchToZoomArea(onZoom: (Float) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(CAMERA_ZOOM_AREA_TAG)
            .pointerInput(onZoom) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) onZoom(zoom)
                }
            },
    )
}

/**
 * The photo just taken, from [shot], fitted whole into the area.
 *
 * Coil's AsyncImage reads and decodes the file on a background thread. A camera often stores a
 * photo sideways and records in its EXIF data how to turn it; Coil reads that and turns the
 * picture, so the photo appears upright.
 */
@Composable
private fun ShotView(shot: File, modifier: Modifier = Modifier) {
    AsyncImage(
        model = shot,
        contentDescription = stringResource(R.string.camera_shot_description),
        contentScale = ContentScale.Fit,
        modifier = modifier.testTag(CAMERA_SHOT_TAG),
    )
}

/**
 * The bar under the picture: the shutter while the live picture shows ([hasShot] false), or
 * Retake and Keep once a photo is waiting. A [message], when there is one, is shown above the
 * buttons.
 *
 * Beside the shutter sit the flash button (left) and the lens switch (right). The flash button
 * shows the current [flashMode] and is left out when [flashMode] is null (the lens has no
 * flash); the lens switch is left out when [canSwitchLens] is false (the phone has one camera).
 * An empty space of the same size takes a missing button's place, so the shutter stays centred.
 *
 * It sits on the theme's surface colour, with the theme's text and button colours, so it reads
 * well in light and dark. navigationBarsPadding keeps the buttons clear of the gesture bar.
 *
 * [shutterEnabled] is false until the camera is ready and while a photo is being taken;
 * [switchEnabled] likewise for the lens switch.
 */
@Composable
private fun CameraBottomBar(
    hasShot: Boolean,
    shutterEnabled: Boolean,
    message: String?,
    onShutter: () -> Unit,
    onKeep: () -> Unit,
    onRetake: () -> Unit,
    flashMode: FlashMode?,
    onFlash: () -> Unit,
    canSwitchLens: Boolean,
    switchEnabled: Boolean,
    onSwitchLens: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(CAMERA_MESSAGE_TAG),
                )
            }
            if (hasShot) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier
                            .heightIn(min = MIN_TOUCH)
                            .widthIn(min = 120.dp)
                            .testTag(CAMERA_RETAKE_TAG),
                    ) {
                        Text(stringResource(R.string.camera_retake))
                    }
                    Button(
                        onClick = onKeep,
                        modifier = Modifier
                            .heightIn(min = MIN_TOUCH)
                            .widthIn(min = 120.dp)
                            .testTag(CAMERA_KEEP_TAG),
                    ) {
                        Text(stringResource(R.string.camera_keep))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (flashMode != null) {
                        FlashButton(mode = flashMode, onClick = onFlash)
                    } else {
                        Spacer(Modifier.size(MIN_TOUCH))
                    }
                    ShutterButton(enabled = shutterEnabled, onClick = onShutter)
                    if (canSwitchLens) {
                        SwitchLensButton(enabled = switchEnabled, onClick = onSwitchLens)
                    } else {
                        Spacer(Modifier.size(MIN_TOUCH))
                    }
                }
            }
        }
    }
}

/**
 * The flash button: its icon shows the current [mode] (a bolt with an "A" for auto, a bolt for
 * on, a crossed-out bolt for off), and a tap calls [onClick] to move to the next mode.
 *
 * The screen reader hears the current mode ("Flash: auto" and so on) as its content description.
 * IconButton draws the icon in the bar's onSurface content colour, which stands out on the
 * surface colour in every theme.
 */
@Composable
private fun FlashButton(mode: FlashMode, onClick: () -> Unit) {
    val (icon, label) = when (mode) {
        FlashMode.Auto -> FlashAutoIcon to stringResource(R.string.camera_flash_auto)
        FlashMode.On -> FlashOnIcon to stringResource(R.string.camera_flash_on)
        FlashMode.Off -> FlashOffIcon to stringResource(R.string.camera_flash_off)
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(MIN_TOUCH)
            .testTag(CAMERA_FLASH_TAG),
    ) {
        Icon(imageVector = icon, contentDescription = label)
    }
}

/**
 * The button that swaps between the back and front lens; a tap calls [onClick] when [enabled].
 * Drawn in the bar's onSurface content colour, like the flash button.
 */
@Composable
private fun SwitchLensButton(enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(MIN_TOUCH)
            .testTag(CAMERA_SWITCH_LENS_TAG),
    ) {
        Icon(
            imageVector = SwitchLensIcon,
            contentDescription = stringResource(R.string.camera_switch_lens),
        )
    }
}

/**
 * The round shutter button: a filled circle in the theme's primary colour with a ring inside,
 * the shape people know from camera apps. Tapping it calls [onClick] when [enabled].
 *
 * It has no visible text, so its purpose ("Take photo") is given to screen readers as its
 * content description.
 */
@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.camera_shutter)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(SHUTTER_SIZE)
            .testTag(CAMERA_SHUTTER_TAG)
            .semantics { contentDescription = label },
    ) {
        // The inner ring takes the button's content colour (onPrimary, or the disabled colour),
        // so it always stands out against the button.
        val ringColour = LocalContentColor.current
        Box(
            modifier = Modifier
                .size(SHUTTER_SIZE - 16.dp)
                .border(width = 3.dp, color = ringColour, shape = CircleShape),
        )
    }
}

/**
 * A drawn stand-in for the live camera picture, used by the previews: a soft evening sky fading
 * into a green field, with a low sun. The previews cannot open a real camera, so this fills the
 * picture area the way a view through the lens would.
 */
@Composable
private fun SampleLivePicture(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(Color(0xFF7FB3D5), Color(0xFFF5CBA7), Color(0xFF58804A)),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        // The sun, a little above the middle.
        Box(
            modifier = Modifier
                .padding(bottom = 80.dp)
                .size(56.dp)
                .background(Color(0xFFFFE08A), CircleShape),
        )
    }
}

/**
 * Paints a small landscape photo (400 by 300 pixels) into a new [ImageBitmap], standing in for
 * the photo just taken in the after-shot preview: a blue sky, a sun and a green hill.
 *
 * CanvasDrawScope lets ordinary drawing commands (drawRect, drawCircle) paint into a bitmap
 * outside of any screen, so the picture is made in code and needs no file.
 */
private fun sampleShotBitmap(): ImageBitmap {
    val width = 400
    val height = 300
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = Size(width.toFloat(), height.toFloat()),
    ) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF5DADE2), Color(0xFFD6EAF8))))
        drawCircle(color = Color(0xFFF7DC6F), radius = 36f, center = Offset(300f, 80f))
        drawCircle(color = Color(0xFF52BE80), radius = 260f, center = Offset(140f, 470f))
        drawCircle(color = Color(0xFF27AE60), radius = 220f, center = Offset(380f, 500f))
    }
    return bitmap
}

/**
 * The camera as the person sees it before a shot, in light and in dark: the live picture (a
 * drawn stand-in here) with the pinch-to-zoom layer on top, and below it the flash button set to
 * auto, the shutter ready to press and the lens switch.
 *
 * `@PreviewLightDark` draws it twice, in day and in night mode, and `ThemeMode.System` makes
 * KansoTheme follow that. The state is a snapshot made up here, with no camera behind it. The
 * taps do nothing in a preview.
 */
@PreviewLightDark
@Composable
private fun CameraLivePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        CameraFrame(onBack = {}) {
            CameraContent(
                state = CameraUiState(
                    camera = CameraState(
                        status = CameraStatus.Ready,
                        canSwitchLens = true,
                        flashMode = FlashMode.Auto,
                        hasFlash = true,
                    ),
                ),
                onShutter = {},
                onKeep = {},
                onRetake = {},
                onFlash = {},
                onSwitchLens = {},
                onZoom = {},
                livePicture = { pictureModifier -> SampleLivePicture(pictureModifier) },
                shotPicture = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The camera right after a shot, in light and in dark: the photo just taken (a picture painted
 * by [sampleShotBitmap]) fitted into the black area, and the Retake and Keep buttons below it.
 *
 * `remember` paints the sample photo once, not on every re-draw of the preview. The waiting photo
 * reaches the screen through the shotPicture slot, so the sample state needs no file.
 */
@PreviewLightDark
@Composable
private fun CameraAfterShotPreview() {
    val photo = remember { sampleShotBitmap() }
    KansoTheme(themeMode = ThemeMode.System) {
        CameraFrame(onBack = {}) {
            CameraContent(
                state = CameraUiState(
                    camera = CameraState(
                        status = CameraStatus.Ready,
                        canSwitchLens = true,
                        flashMode = FlashMode.Auto,
                        hasFlash = true,
                    ),
                ),
                onShutter = {},
                onKeep = {},
                onRetake = {},
                onFlash = {},
                onSwitchLens = {},
                onZoom = {},
                livePicture = { pictureModifier -> SampleLivePicture(pictureModifier) },
                shotPicture = { pictureModifier ->
                    Image(
                        bitmap = photo,
                        contentDescription = stringResource(R.string.camera_shot_description),
                        contentScale = ContentScale.Fit,
                        modifier = pictureModifier,
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The camera when it cannot be opened, in light and in dark: the picture area stays black, the
 * bar says the camera is unavailable (CameraContent picks that message from the status), the
 * shutter is greyed out, and the flash and lens buttons are left out (their empty places keep the
 * shutter centred).
 */
@PreviewLightDark
@Composable
private fun CameraUnavailablePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        CameraFrame(onBack = {}) {
            CameraContent(
                state = CameraUiState(
                    camera = CameraState(
                        status = CameraStatus.Unavailable,
                        canSwitchLens = false,
                        flashMode = FlashMode.Off,
                        hasFlash = false,
                    ),
                ),
                onShutter = {},
                onKeep = {},
                onRetake = {},
                onFlash = {},
                onSwitchLens = {},
                onZoom = {},
                // Nothing to show: a camera that cannot be opened sends no picture.
                livePicture = {},
                shotPicture = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The camera after camera access was refused, in light and in dark: the explanation and the
 * button to the app's settings, under the same top bar. The button does nothing in a preview.
 */
@PreviewLightDark
@Composable
private fun CameraRefusedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        CameraFrame(onBack = {}) {
            CameraRefusedPanel(onOpenSettings = {})
        }
    }
}
