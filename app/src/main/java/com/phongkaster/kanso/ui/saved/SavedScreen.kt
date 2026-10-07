package com.phongkaster.kanso.ui.saved

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.displayMaxEdgePx
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** The test tag on the root of the Saved screen. */
const val SAVED_SCREEN_TAG = "saved-screen"

/** The test tag on the saved photo. */
const val SAVED_PHOTO_TAG = "saved-photo"

/** The test tag on the Share button. */
const val SAVED_SHARE_TAG = "saved-share"

/** The test tag on the Edit another button. */
const val SAVED_EDIT_ANOTHER_TAG = "saved-edit-another"

/** The test tag on the Home button. */
const val SAVED_HOME_TAG = "saved-home"

/**
 * A semantics property carrying the Uri (as text) of the photo the Saved screen shows, so a test
 * can check which photo is on screen without comparing pixels. People and screen readers never
 * see it.
 */
val SAVED_PHOTO_URI_KEY = SemanticsPropertyKey<String>("SavedPhotoUriKey")

/** The smallest height of the action buttons: Android's smallest touch target. */
private val MIN_TOUCH = 48.dp

/**
 * The screen shown after a photo is saved: a top bar titled "Saved", the saved photo upright,
 * the message that it is in Pictures/Kanso, and three actions at the bottom: Share, Edit another
 * and Home.
 *
 * The photo at [photoUri] (the text of the Uri the save returned) is decoded by the app's
 * [PhotoLoader], taken from Koin, the same way the tool screen shows a photo: in the background,
 * upright, and no larger than twice the screen's longest edge. Tests swap the loader in Koin for
 * one that hands back a fixed picture.
 *
 * Like Home, this screen is split in two (state hoisting; see `HomeScreen`). This function is the
 * stateful part: it loads the photo, runs the share logic and hands the results to the stateless
 * [SavedContent], which does the drawing and is what the preview shows.
 *
 * Share opens Android's share sheet for the photo (see [shareChooserIntent]) and then calls
 * [onShare]. On Android 8.1 and 9 the save gives a `file://` Uri, which must never leave the
 * app, so the photo's `content://` Uri is looked up first (see [shareableUri]); that lookup runs
 * in the background, and Share cannot be tapped again while it runs. If no content Uri can be
 * found, the share sheet does not open and [onShare] is not called.
 *
 * [onEditAnother] goes back to the gallery of [tool]; [onHome] goes back to Home. There is no
 * back arrow: system Back already goes Home. [tool] is the tool the photo was saved from; the
 * navigation entry reads it from the same key to open that tool's gallery, so the screen itself
 * only receives it to keep the key's facts together.
 */
@Composable
fun SavedScreen(
    tool: Tool,
    photoUri: String,
    onShare: () -> Unit,
    onEditAnother: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // koinInject asks Koin for the shared PhotoLoader once and keeps it across redraws.
    val loader = koinInject<PhotoLoader>()
    val context = LocalContext.current

    // The screen size in pixels, worked out as on the tool screen.
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val maxEdgePx = with(density) {
        displayMaxEdgePx(
            screenWidthPx = configuration.screenWidthDp.dp.roundToPx(),
            screenHeightPx = configuration.screenHeightDp.dp.roundToPx(),
        )
    }

    // produceState starts with Loading and runs its block in the background of the screen; the
    // value it sets is drawn as soon as it arrives. It runs again only if a key changes.
    val photo by produceState<SavedPhoto>(SavedPhoto.Loading, loader, photoUri, maxEdgePx) {
        value = loader.load(photoUri, maxEdgePx)?.let { SavedPhoto.Ready(it.bitmap) } ?: SavedPhoto.Unreadable
    }

    // Share: look up a shareable Uri off the main thread, then open the share sheet.
    val scope = rememberCoroutineScope()
    val chooserTitle = stringResource(R.string.saved_share_chooser)
    val currentOnShare by rememberUpdatedState(onShare)
    var sharing by remember { mutableStateOf(false) }
    val share: () -> Unit = {
        if (!sharing) {
            sharing = true
            scope.launch {
                try {
                    val uri = shareableUri(context, photoUri)
                    if (uri != null) {
                        val opened = try {
                            context.startActivity(shareChooserIntent(uri, chooserTitle))
                            true
                        } catch (_: ActivityNotFoundException) {
                            // Every phone has the share sheet; this only guards against a broken one.
                            false
                        }
                        if (opened) currentOnShare()
                    }
                } finally {
                    sharing = false
                }
            }
        }
    }

    SavedContent(
        photo = photo,
        photoUri = photoUri,
        sharing = sharing,
        onShare = share,
        onEditAnother = onEditAnother,
        onHome = onHome,
        modifier = modifier,
    )
}

/**
 * The stateless part of the Saved screen (see [SavedScreen] for why there are two): draws the top
 * bar, the [photo] area and the bottom actions, and reports every tap through a callback.
 *
 * [photoUri] is only carried onto the photo's semantics so tests can tell which photo is shown.
 * [sharing] is true while a share is being prepared, which turns the Share button off. Nothing
 * here loads a picture or opens the share sheet, so a preview can draw it from sample data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SavedContent(
    photo: SavedPhoto,
    photoUri: String,
    sharing: Boolean,
    onShare: () -> Unit,
    onEditAnother: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(SAVED_SCREEN_TAG),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.saved_title)) })
        },
        bottomBar = {
            SavedActions(
                shareEnabled = !sharing,
                onShare = onShare,
                onEditAnother = onEditAnother,
                onHome = onHome,
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar and the actions take; the photo stays between.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = photo) {
                // Decoding takes a moment, and a photo that cannot be read leaves the area empty:
                // it is saved all the same, and the message below says so.
                SavedPhoto.Loading, SavedPhoto.Unreadable -> Unit
                is SavedPhoto.Ready -> PhotoView(bitmap = current.bitmap, photoUri = photoUri)
            }
        }
    }
}

/** What the photo area shows: nothing yet, the decoded photo, or nothing because it cannot be read. */
internal sealed interface SavedPhoto {

    /** The photo is still being decoded. */
    data object Loading : SavedPhoto

    /** The photo is decoded, upright, as [bitmap]. */
    data class Ready(val bitmap: Bitmap) : SavedPhoto

    /** The photo could not be opened or decoded. */
    data object Unreadable : SavedPhoto
}

/**
 * Draws [bitmap] as large as fits, keeping its shape, and marks the image node with [photoUri]
 * so tests can tell which photo is shown.
 */
@Composable
private fun PhotoView(bitmap: Bitmap, photoUri: String) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = stringResource(R.string.saved_photo_description),
        // Fit: never cropped, never stretched.
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .testTag(SAVED_PHOTO_TAG)
            .semantics { this[SAVED_PHOTO_URI_KEY] = photoUri },
    )
}

/**
 * The bottom of the screen: the "saved" message, then Share across the full width, and Edit
 * another and Home side by side below it.
 *
 * It sits on the theme's surface colour with the theme's text and button colours, so it reads
 * well in light and dark. navigationBarsPadding keeps the buttons clear of the gesture bar, and
 * every button is at least 48 dp tall. [shareEnabled] is false while a share is being prepared.
 */
@Composable
private fun SavedActions(
    shareEnabled: Boolean,
    onShare: () -> Unit,
    onEditAnother: () -> Unit,
    onHome: () -> Unit,
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
            Text(
                text = stringResource(R.string.saved_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onShare,
                enabled = shareEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_TOUCH)
                    .testTag(SAVED_SHARE_TAG),
            ) {
                Text(stringResource(R.string.saved_share))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onEditAnother,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MIN_TOUCH)
                        .testTag(SAVED_EDIT_ANOTHER_TAG),
                ) {
                    Text(stringResource(R.string.saved_edit_another), textAlign = TextAlign.Center)
                }
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MIN_TOUCH)
                        .testTag(SAVED_HOME_TAG),
                ) {
                    Text(stringResource(R.string.saved_home), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/**
 * A made-up photo for the preview: a warm sky-to-sand gradient with a sun, 600 by 800 pixels
 * (portrait), drawn with Android's Canvas so no file, camera or MediaStore is needed.
 */
private fun sampleBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, 800f,
        0xFF5B8DEF.toInt(), 0xFFF2D6A2.toInt(),
        android.graphics.Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, 600f, 800f, paint)
    paint.shader = null
    paint.color = 0xFFFFE08A.toInt()
    canvas.drawCircle(420f, 260f, 90f, paint)
    return bitmap
}

/** The sample photo the preview shows, wrapped the way the loader's result is. */
private fun samplePhoto(): SavedPhoto = SavedPhoto.Ready(sampleBitmap())

/**
 * The Saved screen with a photo on show and every button ready, drawn once in light and once in
 * dark. [SavedContent] draws the photo ([PhotoView]) and the bottom buttons ([SavedActions]), so
 * this one picture covers them all. `ThemeMode.System` makes KansoTheme follow the preview's own
 * light or dark picture; the taps do nothing.
 */
@PreviewLightDark
@Composable
private fun SavedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SavedContent(
            photo = samplePhoto(),
            photoUri = "content://media/external/images/media/42",
            sharing = false,
            onShare = {},
            onEditAnother = {},
            onHome = {},
        )
    }
}
