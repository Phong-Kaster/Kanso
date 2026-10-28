package com.phongkaster.kanso.ui.camera

import android.Manifest
import androidx.annotation.DrawableRes
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.phongkaster.kanso.ui.component.RoundBackButton
import com.phongkaster.kanso.ui.component.permission.AccessRefusedPanel
import com.phongkaster.kanso.ui.component.permission.PermissionGate
import com.phongkaster.kanso.ui.component.permission.openAppDetailsSettings
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.awaitCancellation
import java.io.File

/** The test tag on the root of the camera screen, present in every permission state. */
const val CAMERA_SCREEN_TAG = "camera-screen"

/** The test tag on the live camera picture. */
const val CAMERA_PREVIEW_TAG = "camera-preview"

/** The test tag on the shutter button, shown while the live picture is on screen. */
const val CAMERA_SHUTTER_TAG = "camera-shutter"

/**
 * The test tag on the ring inside the shutter button: the part that shrinks while a finger is on
 * the shutter and springs back when it lifts (see [ShutterButton]). It is merged into the
 * shutter's own node for screen readers, so a test finds it in the unmerged tree.
 */
const val CAMERA_SHUTTER_INNER_TAG = "camera-shutter-inner"

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

/**
 * The test tag on the loading indicator shown in the picture area while the camera is being
 * opened (see [CameraLoading]).
 */
const val CAMERA_LOADING_TAG = "camera-loading"

/** The size of the round shutter button; well above the 48 dp smallest touch target. */
private val SHUTTER_SIZE = 72.dp

/** The size of the ring drawn inside the shutter while no finger is on it. */
private val SHUTTER_RING_SIZE = 56.dp

/**
 * How big the shutter's ring is while a finger is on the shutter, as a share of its resting size:
 * it shrinks to four fifths, enough to see under a thumb.
 */
private const val SHUTTER_PRESSED_RING_SCALE = 0.8f

/** The test tag on the top bar's round Back button. */
const val CAMERA_BACK_TAG = "camera-back"

/**
 * The height of the Keep and Retake buttons: Material's medium button height (56 dp), above the
 * 48 dp smallest touch target and large enough to hit without looking.
 */
private val SHOT_ACTION_HEIGHT = ButtonDefaults.MediumContainerHeight

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
 * It holds no state, so the real screen (with the permission gate as its [content]), the previews
 * (with a sample picture, or the refused panel) and the screenshot test all use it; it is
 * `internal` rather than private for that last one.
 *
 * The back arrow is the app's shared `RoundBackButton`: a 48 dp round Material 3 Expressive icon
 * button that squares its corners a little while pressed, as on every other screen. Its content
 * description, "Back", is its spoken name, and it carries the tag [CAMERA_BACK_TAG].
 *
 * The title uses `titleLargeEmphasized`, one of the "Emphasized" text styles Material 3
 * Expressive adds: the size of `titleLarge`, but bolder.
 *
 * The bottom edge. The Scaffold's padding normally includes the navigation bar at the bottom.
 * Here it is limited to the top and the sides (`only(Horizontal + Top)`), so the content reaches
 * the bottom of the screen and clears the system's strips there itself: the bottom bar of
 * [CameraContent] pads its buttons inside its own surface (see [CameraBottomBar]), so its colour
 * runs on behind the navigation bar, and the refused panel pads itself by the navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CameraFrame(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.testTag(CAMERA_SCREEN_TAG),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tool_camera),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                navigationIcon = {
                    // The shared round Back button (see ui/common), 48 dp, with a little room
                    // from the screen's edge.
                    RoundBackButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag(CAMERA_BACK_TAG),
                    )
                },
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar and the side insets take (the bottom is left to
        // the content, see above). consumeWindowInsets tells the content below that those insets
        // are already dealt with, so an inset padding further down does not add them twice.
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
 * The stateless part of the screen once camera access is granted: the picture area above the
 * [CameraBottomBar], drawn from [state], the camera screen's one snapshot.
 *
 * While [shotPicture] is null the area shows [livePicture] with the pinch-to-zoom layer on top
 * (a pinch calls [onZoom]); once a photo waits, [shotPicture] is shown instead and the bar
 * offers Keep ([onKeep]) and Retake ([onRetake]). Both pictures are slots: each is called with
 * the modifier that sizes it, and draws whatever its caller chose. The real screen passes a
 * [shotPicture] exactly when [state] holds a shot, so the slot is how this part knows a photo
 * waits. While the camera is opening ([CameraStatus.Starting]) and no photo waits, a loading
 * indicator turns in the middle of the area ([CameraLoading]).
 *
 * The shutter and the lens switch work only when the camera in [state] is ready and no photo is
 * being taken. The flash button shows the flash mode, and only when the lens has a flash; the lens
 * switch shows only when the phone can switch lenses. A message is shown in the bar when the last
 * shot failed, or when the camera is unavailable and no photo waits. [onShutter], [onFlash] and
 * [onSwitchLens] are called by their buttons.
 *
 * Every colour comes from the theme. The picture area's background, which shows wherever the
 * picture does not cover it (before the camera sends its first frame, when it cannot be opened,
 * and around a photo whose shape differs from the area's), is `surfaceContainer`: one step away
 * from the `surface` colour of the bars above and below, so the area reads as a frame of its own
 * in light and in dark. Nothing readable is drawn on it; every label and message sits in the
 * bottom bar.
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
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            // While a photo waits for Keep or Retake the live picture leaves the screen, which
            // also disconnects the camera (see LivePreview); Retake brings it back.
            if (shotPicture == null) {
                livePicture(Modifier.fillMaxSize())
                PinchToZoomArea(onZoom = onZoom, modifier = Modifier.matchParentSize())
                // Drawn last, so it sits on top; it takes no touches, so a pinch still zooms.
                if (camera.status == CameraStatus.Starting) {
                    CameraLoading()
                }
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
 * What the picture area shows while the camera is being opened (and again after a lens switch,
 * which reopens it): Material 3 Expressive's loading indicator, centred in the area.
 *
 * `LoadingIndicator` is explained where it first appears, on the gallery's `GalleryLoading`: a
 * small shape that keeps morphing into the next while it slowly turns, in the theme's primary
 * colour. Here it is the indeterminate form too, because nobody knows how long the camera takes
 * to open; it leaves as soon as the camera reports that it is ready or unavailable. It is still
 * marked experimental, hence the opt-in on this one small function.
 *
 * It has no text, so its content description ("Opening the camera") is its spoken name. Because
 * it animates for as long as it is shown, no preview shows the opening state (a preview needs a
 * picture that settles), and a test that wants to see it pauses the test clock first.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CameraLoading(modifier: Modifier = Modifier) {
    // Read in composition, so the description follows the phone's language.
    val description = stringResource(R.string.camera_loading)
    LoadingIndicator(
        modifier = modifier
            .testTag(CAMERA_LOADING_TAG)
            .semantics { contentDescription = description },
    )
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
 * PreviewView paints a background of its own until the first frame arrives; it is made
 * see-through here, so the theme's picture-area colour (see [CameraContent]) shows instead, with
 * the loading indicator on it. Once the camera streams, its frames cover the whole area.
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
            // See-through until the first frame, so the theme's colour shows (see above).
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
 * well in light and dark.
 *
 * The bar is at the very bottom of the screen, where the system keeps two strips for itself, and
 * the buttons are lifted clear of both (the comment on the Column below shows how):
 *
 * - the navigation bar, which holds the back, home and recents buttons or the thin gesture hint,
 *   and which the system draws on top of the app;
 * - the bottom gesture zone, where a swipe up means "go home". The system reads a touch there as
 *   the start of a gesture, so a button inside it can bring up the phone's own navigation instead
 *   of being tapped. It is not always as tall as the navigation bar: with swipe gestures on and
 *   the gesture hint turned off (as One UI allows on the owner's Galaxy A13), the navigation bar
 *   is 0 dp tall but the gesture zone is still there. Padding by the navigation bar alone would
 *   then leave the shutter inside the gesture zone.
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
                // Window insets are the strips along the screen's edges that the system draws on
                // (the navigation bar) or listens on (the gesture zones). `only(Bottom)` keeps just
                // the bottom gesture zone: the left and right ones are where the back swipe
                // starts, and padding them would only narrow the row. `union` then takes, on each
                // side, the larger of the two insets, so the bottom padding is the navigation
                // bar's height or the gesture zone's, whichever is taller, never the two added
                // together. The padding sits inside the Surface, so the Surface's colour still
                // runs down behind the navigation bar while the buttons stay above both strips.
                // (The frame's Scaffold leaves the bottom inset to this bar; see CameraFrame.)
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
                )
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
                    RetakeButton(onClick = onRetake)
                    KeepButton(onClick = onKeep)
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
                        Spacer(Modifier.size(IconButtonDefaults.mediumContainerSize()))
                    }
                    ShutterButton(enabled = shutterEnabled, onClick = onShutter)
                    if (canSwitchLens) {
                        SwitchLensButton(enabled = switchEnabled, onClick = onSwitchLens)
                    } else {
                        Spacer(Modifier.size(IconButtonDefaults.mediumContainerSize()))
                    }
                }
            }
        }
    }
}

/**
 * The Retake button, shown once a photo waits: an outlined button with a "redo" arrow and the
 * word "Retake". A tap calls [onClick], which throws the photo away.
 *
 * Like every button on this screen it is given `shapes`, so it squares its corners while pressed.
 * `ButtonDefaults.shapesFor(height)` picks the pair Material recommends for a button of that
 * height, here Material's medium height ([SHOT_ACTION_HEIGHT]).
 */
@Composable
private fun RetakeButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(SHOT_ACTION_HEIGHT),
        contentPadding = ButtonDefaults.contentPaddingFor(SHOT_ACTION_HEIGHT, hasStartIcon = true),
        modifier = Modifier
            .heightIn(min = SHOT_ACTION_HEIGHT)
            .widthIn(min = 120.dp)
            .testTag(CAMERA_RETAKE_TAG),
    ) {
        ShotActionLabel(icon = R.drawable.ic_refresh, label = stringResource(R.string.camera_retake))
    }
}

/**
 * The Keep button, shown once a photo waits: a filled button in the theme's primary colour with a
 * tick and the word "Keep", the screen's main action at that point. A tap calls [onClick], which
 * opens the tool on the photo. Its shapes work as on [RetakeButton].
 */
@Composable
private fun KeepButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(SHOT_ACTION_HEIGHT),
        contentPadding = ButtonDefaults.contentPaddingFor(SHOT_ACTION_HEIGHT, hasStartIcon = true),
        modifier = Modifier
            .heightIn(min = SHOT_ACTION_HEIGHT)
            .widthIn(min = 120.dp)
            .testTag(CAMERA_KEEP_TAG),
    ) {
        ShotActionLabel(icon = R.drawable.ic_check, label = stringResource(R.string.camera_keep))
    }
}

/**
 * The inside of the Keep and Retake buttons: the Material Symbols drawable [icon] followed by
 * the visible [label], sized and spaced as Material recommends for [SHOT_ACTION_HEIGHT].
 *
 * The icon has no content description of its own: the label next to it already names the action,
 * and a screen reader would otherwise say it twice. `Icon` tints the white drawable with the
 * button's content colour, so it matches the label in every theme and when disabled.
 */
@Composable
private fun ShotActionLabel(@DrawableRes icon: Int, label: String) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(SHOT_ACTION_HEIGHT)),
    )
    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(SHOT_ACTION_HEIGHT)))
    Text(text = label, style = ButtonDefaults.textStyleFor(SHOT_ACTION_HEIGHT))
}

/**
 * The flash button: its icon shows the current [mode] (a bolt with an "A" for auto, a bolt for
 * on, a crossed-out bolt for off), and a tap calls [onClick] to move to the next mode.
 *
 * It is a *filled tonal* icon button of Material's medium size (56 dp): a soft circle in the
 * theme's secondary container colour, which marks it as a button beside the shutter without
 * competing with it. Given the medium round and pressed shapes, it squares its corners while
 * pressed, like the back arrow. It has no visible text: the screen reader hears the current mode
 * ("Flash: auto" and so on) as its content description, which tests read as the mode's label.
 */
@Composable
private fun FlashButton(mode: FlashMode, onClick: () -> Unit) {
    val (icon, label) = when (mode) {
        FlashMode.Auto -> R.drawable.ic_flash_auto to stringResource(R.string.camera_flash_auto)
        FlashMode.On -> R.drawable.ic_flash_on to stringResource(R.string.camera_flash_on)
        FlashMode.Off -> R.drawable.ic_flash_off to stringResource(R.string.camera_flash_off)
    }
    FilledTonalIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(
            shape = IconButtonDefaults.mediumRoundShape,
            pressedShape = IconButtonDefaults.mediumPressedShape,
        ),
        modifier = Modifier
            .size(IconButtonDefaults.mediumContainerSize())
            .testTag(CAMERA_FLASH_TAG),
    ) {
        Icon(painter = painterResource(icon), contentDescription = label)
    }
}

/**
 * The button that swaps between the back and front lens; a tap calls [onClick] when [enabled].
 * A filled tonal icon button of the same size and shapes as the flash button, so the two frame
 * the shutter evenly; its content description ("Switch camera") is its spoken name.
 */
@Composable
private fun SwitchLensButton(enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(
            shape = IconButtonDefaults.mediumRoundShape,
            pressedShape = IconButtonDefaults.mediumPressedShape,
        ),
        enabled = enabled,
        modifier = Modifier
            .size(IconButtonDefaults.mediumContainerSize())
            .testTag(CAMERA_SWITCH_LENS_TAG),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_cameraswitch),
            contentDescription = stringResource(R.string.camera_switch_lens),
        )
    }
}

/**
 * The round shutter button: a filled circle in the theme's primary colour with a ring inside,
 * the shape people know from camera apps. Tapping it calls [onClick] when [enabled]; one tap
 * takes one photo.
 *
 * It answers the press in two ways at once, both driven by the same finger:
 *
 * - The button itself is a Material 3 Expressive filled icon button given a pair of `shapes`: a
 *   circle at rest and Material's large "pressed" shape (a rounded square) while a finger is on
 *   it. Material animates between the two by itself.
 * - The ring inside shrinks to [SHUTTER_PRESSED_RING_SCALE] of its size while the finger is down
 *   and springs back when it lifts, like a real shutter being pushed in.
 *
 * How the ring knows about the finger. A `MutableInteractionSource` is a stream of the things
 * that happen to a component: pressed, released, focused and so on. The button reports into the
 * one created here (it is handed to the button), and `collectIsPressedAsState` turns that stream
 * into a simple true or false that Compose watches: true while a finger is on the button. No
 * second click handler is involved, so a tap still reaches [onClick] exactly once.
 *
 * How the ring moves. `animateFloatAsState` does not jump to its new target: whenever the target
 * changes (1 at rest, the pressed scale while pressed) it moves the value there over a few frames
 * with the spring it is given. The spring is the theme's `fastSpatialSpec`, the motion scheme's
 * quick spring for things that move or change size; the Expressive scheme lets it overshoot a
 * little, which is what makes the ring feel springy. `graphicsLayer` applies the scale when the
 * ring is drawn, so only the drawing changes: the ring's place in the layout stays the same and
 * nothing around it moves.
 *
 * It has no visible text, so its purpose ("Take photo") is given to screen readers as its
 * content description. The ring takes the button's content colour (onPrimary, or the faint
 * disabled colour), so it always stands out against the button.
 */
@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.camera_shutter)
    // remember keeps the same stream across redraws; a new one each time would lose the press.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val ringScale by animateFloatAsState(
        targetValue = if (pressed) SHUTTER_PRESSED_RING_SCALE else 1f,
        // Read here, while composing: the motion scheme is a theme value.
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>(),
        label = "shutterRingScale",
    )
    FilledIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(
            shape = CircleShape,
            pressedShape = IconButtonDefaults.largePressedShape,
        ),
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(SHUTTER_SIZE)
            .testTag(CAMERA_SHUTTER_TAG)
            .semantics { contentDescription = label },
    ) {
        val ringColour = LocalContentColor.current
        Box(
            modifier = Modifier
                .size(SHUTTER_RING_SIZE)
                .testTag(CAMERA_SHUTTER_INNER_TAG)
                // ringScale is read inside this block, so a new value only redraws the ring;
                // the composables around it are not run again.
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                }
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
 * by [sampleShotBitmap]) fitted into the picture area, on the theme's colour where it does not
 * reach, and the Retake and Keep buttons, each with its icon, below it.
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
 * The camera when it cannot be opened, in light and in dark: the picture area shows only the
 * theme's colour, the bar says the camera is unavailable (CameraContent picks that message from
 * the status), the shutter is greyed out, and the flash and lens buttons are left out (their
 * empty places keep the shutter centred).
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
