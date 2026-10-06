package com.phongkaster.kanso.ui.tool

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.photo.displayMaxEdgePx
import com.phongkaster.kanso.ui.common.BackArrowIcon

/** The test tag on the root of the tool screen. */
const val TOOL_SCREEN_TAG = "tool-screen"

/** The test tag on the area below the top bar where the photo is shown. */
const val TOOL_BODY_TAG = "tool-body"

/** The test tag on the photo itself. */
const val TOOL_IMAGE_TAG = "tool-image"

/** The test tag on the message shown when the photo cannot be opened. */
const val TOOL_UNREADABLE_TAG = "tool-unreadable"

/** The test tag on the Save action in the top bar. */
const val TOOL_SAVE_TAG = "tool-save"

/**
 * A semantics property carrying the Uri (as text) of the photo the image node shows.
 *
 * Semantics are the facts about each element that accessibility services and tests read, such
 * as its text or its role. This extra, Kanso-only fact lets a test check which photo is on screen
 * without comparing pixels; it is invisible to people and to screen readers.
 */
val TOOL_PHOTO_URI_KEY = SemanticsPropertyKey<String>("PhotoUriKey")

/**
 * The screen where a tool works on one photo: a top bar with a back arrow, the tool's name and a
 * Save action, and below it the photo, upright and fitted to the space.
 *
 * Passing data in a Navigation 3 key: the screen needs two facts, which tool and which photo.
 * They are not handed over in memory from the gallery; they travel inside the navigation key,
 * `ToolPhoto(tool, photoUri)`, the value pushed onto the back stack to open this screen. The
 * back stack is saved (every key is @Serializable), so when the phone rotates, or when Android
 * kills the app in the background and later restores it, the same key comes back and this screen
 * reopens on the same tool and photo with nothing else to restore. The navigation entry passes
 * the key's values to the [viewModel] it builds, which exposes them as `tool` and `photoUri`.
 * Only small values belong in a key: the photo goes in as the text of its Uri, never as pixels.
 *
 * The photo is decoded to about twice the screen's longest edge (see [displayMaxEdgePx]), in the
 * background, and never at full size. If it cannot be opened, a plain message says so.
 *
 * Save is shown but not enabled yet: saving is added to this screen separately.
 * [onBack] is called when the user taps the back arrow; the caller pops the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScreen(
    viewModel: ToolViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The screen size in pixels: LocalConfiguration gives it in dp, LocalDensity turns dp into
    // pixels (3 pixels per dp on an xxhdpi phone).
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val maxEdgePx = with(density) {
        displayMaxEdgePx(
            screenWidthPx = configuration.screenWidthDp.dp.roundToPx(),
            screenHeightPx = configuration.screenHeightDp.dp.roundToPx(),
        )
    }

    // LaunchedEffect runs its block once when the screen appears (and again only if maxEdgePx
    // changes). The ViewModel ignores a repeated request for the same size.
    LaunchedEffect(viewModel, maxEdgePx) {
        viewModel.load(maxEdgePx)
    }

    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier.testTag(TOOL_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(viewModel.tool.labelRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = BackArrowIcon,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    // Visible but disabled until saving exists, so the layout does not jump later.
                    TextButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.testTag(TOOL_SAVE_TAG),
                    ) {
                        Text(stringResource(R.string.tool_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar (and system bars) take; the photo avoids it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(TOOL_BODY_TAG),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = state) {
                // Decoding takes a moment; the area simply stays empty meanwhile.
                ToolUiState.Loading -> Unit
                is ToolUiState.Ready -> PhotoView(
                    current = current,
                    photoUri = viewModel.photoUri,
                )
                ToolUiState.Unreadable -> Text(
                    text = stringResource(R.string.tool_photo_unreadable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .testTag(TOOL_UNREADABLE_TAG),
                )
            }
        }
    }
}

/**
 * Draws the decoded photo of [current] as large as fits, keeping its shape, and marks the image
 * node with [photoUri] so tests can tell which photo is shown.
 */
@Composable
private fun PhotoView(current: ToolUiState.Ready, photoUri: String) {
    val bitmap = current.bitmap
    // Compose draws an ImageBitmap; wrapping the Android Bitmap is cheap, and remember keeps the
    // wrapper between redraws.
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val description = stringResource(R.string.tool_photo_description)
    Image(
        bitmap = image,
        contentDescription = description,
        // Fit scales the photo down (or up) until it fits whole: never cropped, never stretched.
        contentScale = ContentScale.Fit,
        // aspectRatio sizes the image node itself to the photo's shape, the largest that fits
        // the area, so the node's bounds are exactly where the photo is drawn.
        modifier = Modifier
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .testTag(TOOL_IMAGE_TAG)
            .semantics { this[TOOL_PHOTO_URI_KEY] = photoUri },
    )
}
