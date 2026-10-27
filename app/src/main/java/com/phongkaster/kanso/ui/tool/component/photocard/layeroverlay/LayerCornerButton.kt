package com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the delete button at the selected sticker's top-left corner. */
const val LAYER_DELETE_TAG = "layer-delete"

/** Test tag of the duplicate button at the selected sticker's top-right corner. */
const val LAYER_DUPLICATE_TAG = "layer-duplicate"

/** Test tag of the flip (mirror) button at the selected sticker's bottom-left corner. */
const val LAYER_FLIP_TAG = "layer-flip"

/** Test tag of the resize and turn handle at the selected sticker's bottom-right corner. */
const val LAYER_RESIZE_TAG = "layer-resize"

/** The size of a corner button's touch target: the 48 dp the accessibility guidelines ask for. */
val LAYER_BUTTON_TARGET = 48.dp

/** The size of the coloured disc inside a corner button's touch target. */
private val DISC_SIZE = 28.dp

/** The size of the glyph drawn on the disc. */
private val GLYPH_SIZE = 16.dp

/**
 * One round button on a corner of the selected sticker's frame: a disc in the theme's container
 * colours with a glyph on it, inside a 48 dp touch target (the disc alone would be too small to
 * hit with a thumb).
 *
 * It is a separate component, not part of the frame's drawing, so each button is its own
 * accessibility node and takes its own touches. [modifier] places it (the frame gives it an
 * offset); [tag] lets a test find it; [description] is what a screen reader says. A tap calls
 * [onClick]; the resize handle passes null and a drag handler through [gestures] instead. The
 * glyph is drawn with `Icon`, which tints it with the content colour: the file itself is white.
 */
@Composable
fun LayerCornerButton(
    @DrawableRes iconRes: Int,
    description: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gestures: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .testTag(tag)
            .size(LAYER_BUTTON_TARGET)
            .semantics { contentDescription = description }
            .then(gestures)
            .then(if (onClick != null) Modifier.clip(CircleShape).clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(DISC_SIZE),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(GLYPH_SIZE),
                )
            }
        }
    }
}

/** The four buttons in a row, as the frame draws them, in light and in dark. */
@PreviewLightDark
@Composable
private fun StickerCornerButtonPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        // The theme stretches its single child to the whole screen, so a Box goes first.
        Box(Modifier.fillMaxSize()) {
            Row {
                LayerCornerButton(R.drawable.ic_close_circle, stringResource(R.string.sticker_frame_delete), LAYER_DELETE_TAG, onClick = {})
                LayerCornerButton(R.drawable.ic_duplicate, stringResource(R.string.sticker_frame_duplicate), LAYER_DUPLICATE_TAG, onClick = {})
                LayerCornerButton(R.drawable.ic_flip, stringResource(R.string.sticker_frame_flip), LAYER_FLIP_TAG, onClick = {})
                LayerCornerButton(R.drawable.ic_resize_circle, stringResource(R.string.sticker_frame_resize), LAYER_RESIZE_TAG)
            }
        }
    }
}
