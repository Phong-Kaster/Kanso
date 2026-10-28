package com.phongkaster.kanso.ui.tool.component.photocard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.edit.edit.AppLayerPictures
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.LayerMapping
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.component.BlurredBackdrop
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.RETOUCH_NO_FACE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_BODY_TAG
import com.phongkaster.kanso.ui.tool.TOOL_CARD_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_LOADING_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_UNREADABLE_TAG
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.component.Checkerboard
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LayerOverlay

/**
 * The large rounded card holding the photo for [state], full width, with the "Replace" chip
 * floating at its bottom (calls [onReplace]).
 *
 * A `Card` is Material's container for one piece of content: a rounded surface in the theme's
 * container colour. Its shape is the theme's `extraLarge` corner, the biggest rounding of the
 * theme's shape scale, and it clips what it holds to that shape, so the photo and its backdrop
 * get the same rounded corners.
 *
 * Inside it ([TOOL_BODY_TAG]): a loading indicator while the photo is decoded; the photo when it
 * is ready, as large as fits, over a [BlurredBackdrop] of itself that fills the rest of the card;
 * a plain message when it cannot be opened; a progress ring ([EditProgress]) while an edit runs;
 * and, at the top, the message about the last failed edit, if any (a tap hides it, calling
 * [onDismissFailure]). The "Replace" chip floats 16 dp above the card's bottom edge over all of it.
 *
 * Once an edit has been made, the card shows `state.edited` instead of the decoded original; a
 * failed edit leaves `edited` as it was, so the photo shown never changes on a failure. While
 * `state.comparing` is on (the Edit screen's Compare switch), the card shows the decoded original
 * instead, untouched; `edited` is kept, so turning Compare off brings the edited photo back.
 *
 * Replace is switched off while an edit runs, so the screen cannot be left under work that has
 * not finished. With [showReplace] false (the Edit screen) no Replace chip is drawn at
 * all, so nothing sits at the photo's bottom right.
 *
 * **Layers (Edit screen only).** The live picture carries a [LayerOverlay] with the layers of
 * `state.settings` (stickers and texts). It is touchable with any sheet open but Crop, or none,
 * while Done is not running (touching a layer opens its sheet);
 * with Compare on the live picture is not drawn at all, so the original shows with no layers.
 * The `onLayer...` callbacks pass the overlay's events up (select, a gesture's new list, the
 * gesture's end, and the frame buttons: delete, duplicate, a sticker's flip, a text's edit); they
 * default to doing nothing.
 */
@Composable
internal fun PhotoCard(
    state: ToolUiState,
    onReplace: () -> Unit,
    onDismissFailure: () -> Unit,
    modifier: Modifier = Modifier,
    showReplace: Boolean = true,
    onLayerSelect: (Long?) -> Unit = {},
    onLayerPreview: (List<Layer>) -> Unit = {},
    onLayerCommit: () -> Unit = {},
    onLayerDelete: (Long) -> Unit = {},
    onLayerDuplicate: (Long) -> Unit = {},
    onLayerFlip: (Long) -> Unit = {},
    onLayerEdit: (Long) -> Unit = {},
) {
    val running = state.processing as? ToolUiState.Processing.Running
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.testTag(TOOL_CARD_TAG),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TOOL_BODY_TAG),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = state.photo) {
                // Decoding takes a moment; meanwhile an indicator moves in the middle of the card.
                ToolUiState.PhotoState.Loading -> PhotoLoadingIndicator()
                is ToolUiState.PhotoState.Ready -> {
                    // The edited photo when there is one, else the decoded original; while Compare
                    // is on, always the original, so the person sees what they started from.
                    val shown = if (state.comparing) current.bitmap else state.edited ?: current.bitmap
                    // Compose draws an ImageBitmap; wrapping the Android Bitmap is cheap, and
                    // remember keeps the wrapper between redraws.
                    val image = remember(shown) { shown.asImageBitmap() }
                    if (state.tool == Tool.REMOVE_BACKGROUND && shown.hasAlpha()) {
                        // A cut-out has see-through parts: show a checkerboard behind them
                        // instead of the blurred copy of the photo.
                        Checkerboard(Modifier.fillMaxSize())
                    } else {
                        BlurredBackdrop(photo = image)
                    }
                    if (state.tool == Tool.EDIT && !state.comparing) {
                        // The Edit screen draws the photo live: the settings' crop, turns, flips and
                        // colour sliders are applied by Compose as it draws, and only a filter is
                        // a re-rendered bitmap (state.filterPreview). Nothing is rendered in full
                        // until Done.
                        // The node is sized to the shape of what is drawn (the cropped, turned
                        // photo), so its bounds are exactly where the photo is.
                        val crop = state.settings.crop
                        val cropWidth = (crop.right - crop.left).coerceAtLeast(0.01f) * current.bitmap.width
                        val cropHeight = (crop.bottom - crop.top).coerceAtLeast(0.01f) * current.bitmap.height
                        val turned = state.settings.quarterTurns % 2 != 0
                        val photoDescription = stringResource(R.string.tool_photo_description)
                        Box(
                            modifier = Modifier
                                .aspectRatio(if (turned) cropHeight / cropWidth else cropWidth / cropHeight)
                                .testTag(TOOL_IMAGE_TAG)
                                .semantics {
                                    this[TOOL_PHOTO_URI_KEY] = state.photoUri
                                    contentDescription = photoDescription
                                },
                        ) {
                            // The sticker under a finger is drawn by the live preview itself, every
                            // frame; its picture is decoded once per picture, off the touch path's
                            // hot loop (remember), at a size that suits the preview. The other
                            // layers are already inside the filtered copy.
                            val touched = state.touchedLayerKey?.let { key ->
                                state.settings.layers.firstOrNull { it.key == key }
                            }
                            val context = LocalContext.current
                            val touchedPicture = remember(touched?.content) {
                                touched?.let {
                                    AppLayerPictures(context)
                                        .picture(it.content, TOUCHED_PICTURE_EDGE)
                                        ?.asImageBitmap()
                                }
                            }
                            LivePreviewImage(
                                bitmap = state.filterPreview ?: current.bitmap,
                                settings = state.settings,
                                touchedLayer = touched,
                                touchedPicture = touchedPicture,
                            )
                            // The layers sit on top of the live picture, inside the same box, so
                            // the overlay's coordinates are the shown picture's. They are drawn with
                            // any tool open (they are part of the photo); only the Stickers sheet,
                            // with Compare off and Done idle, gives them touches, a frame and buttons.
                            if (state.settings.layers.isNotEmpty()) {
                                val mapping = remember(current.bitmap, state.settings) {
                                    LayerMapping(current.bitmap.width, current.bitmap.height, state.settings)
                                }
                                LayerOverlay(
                                    layers = state.settings.layers,
                                    selectedKey = state.selectedLayerKey,
                                    mapping = mapping,
                                    // Layers answer touches with any sheet open but Crop (whose
                                    // cropper replaces this card), or none: touching one opens its
                                    // own sheet (ADR-005).
                                    enabled = state.openTool != EditTool.CROP && !state.finishing,
                                    onSelect = onLayerSelect,
                                    onPreview = onLayerPreview,
                                    onCommit = onLayerCommit,
                                    onDelete = onLayerDelete,
                                    onDuplicate = onLayerDuplicate,
                                    onFlip = onLayerFlip,
                                    onEdit = onLayerEdit,
                                )
                            }
                        }
                    } else {
                        PhotoView(image = image, photoUri = state.photoUri)
                    }
                }
                ToolUiState.PhotoState.Unreadable -> Text(
                    text = stringResource(R.string.tool_photo_unreadable),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .testTag(TOOL_UNREADABLE_TAG),
                )
            }
            // AnimatedVisibility fades the ring in when an edit starts and out when it ends; while
            // it fades out the edit is over, so the ring is drawn full.
            androidx.compose.animation.AnimatedVisibility(visible = running != null, enter = fadeIn(), exit = fadeOut()) {
                EditProgress(progress = running?.progress ?: 1f)
            }
            // The failed edit's message sits at the top of the card.
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.failure?.let { failure ->
                    // The no-face message also sits in a node of its own, tagged for Retouch, the
                    // one tool that can fail this way; its card keeps the common failure tag.
                    Box(
                        if (failure == EditFailure.NoFace) Modifier.testTag(RETOUCH_NO_FACE_TAG) else Modifier,
                    ) {
                        FailureMessage(
                            text = stringResource(failureMessageRes(failure)),
                            onDismiss = onDismissFailure,
                        )
                    }
                }
            }
            if (showReplace) {
                ReplaceChip(
                    enabled = running == null,
                    onClick = onReplace,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

/**
 * The loading indicator in the middle of the photo card while the photo is being decoded, so the
 * person sees that the photo is on its way instead of an empty card.
 *
 * `LoadingIndicator` is the Material 3 Expressive way of saying "please wait". Instead of the
 * classic spinning ring, a small rounded shape keeps morphing into the next one (a soft burst, a
 * cookie, a pentagon, a pill and so on) while it slowly turns. Called without a progress value,
 * as here, it does not know how long the wait will last, so it animates for as long as it is
 * shown; that is why no preview shows the loading state (a preview draws one still picture).
 * Here it has its standard 48 dp box and its default colour, the theme's primary blue.
 * It goes away by itself as soon as the photo is ready or turns out to be unreadable, because
 * the card then draws that instead.
 *
 * It shows no text, so the tool's name stays the only text naming the tool on this screen. Its
 * content description, which screen readers say, tells that the photo is being opened.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PhotoLoadingIndicator() {
    val description = stringResource(R.string.tool_loading)
    LoadingIndicator(
        modifier = Modifier
            .testTag(TOOL_LOADING_TAG)
            // Set on the indicator's own modifier, the description becomes its spoken name.
            .semantics { contentDescription = description },
    )
}

/** The photo card in light and dark: the sample photo shown, with the Replace chip at its bottom. */
@PreviewLightDark
@Composable
private fun PhotoCardPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        PhotoCard(
            state = ToolUiState(
                tool = Tool.RETOUCH,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
            ),
            onReplace = {},
            onDismissFailure = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** The Edit screen's photo with one sticker selected and the Stickers sheet open, light and dark. */
@PreviewLightDark
@Composable
private fun PhotoCardStickersPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        PhotoCard(
            state = ToolUiState(
                tool = Tool.EDIT,
                photoUri = SAMPLE_PHOTO_URI,
                photo = ToolUiState.PhotoState.Ready(samplePhoto()),
                settings = EditSettings(
                    layers = listOf(
                        Layer(
                            key = 1,
                            content = LayerContent.Picture(StickerCatalog.all[0].pictureId),
                            centreX = 0.5f,
                            centreY = 0.5f,
                            width = 0.3f,
                            degrees = 10f,
                            mirrored = false,
                        ),
                    ),
                ),
                openTool = EditTool.STICKERS,
                selectedLayerKey = 1L,
            ),
            onReplace = {},
            onDismissFailure = {},
            showReplace = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * The width, in pixels, a touched sticker's picture is decoded at: enough for a sticker covering
 * most of a phone-sized photo, and the decoder never goes below the file's own size.
 */
private const val TOUCHED_PICTURE_EDGE = 1024

/** The Uri text the preview pretends its photo came from. */
private const val SAMPLE_PHOTO_URI = "content://media/external/images/media/42"

/** A made-up 640 x 480 photo for the preview, drawn in code: a sky-to-sea gradient. */
private fun samplePhoto(): Bitmap {
    val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, 480f, 0xFFFFB74D.toInt(), 0xFF1565C0.toInt(), Shader.TileMode.CLAMP)
    }
    Canvas(bitmap).drawRect(0f, 0f, 640f, 480f, paint)
    return bitmap
}
