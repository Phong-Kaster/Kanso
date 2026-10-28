package com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.phongkaster.kanso.data.edit.edit.TextLayerLayout
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.LayerMapping
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/** Test tag of the sticker overlay's root: tests drive their touches through this node. */
const val LAYER_OVERLAY_TAG = "layer-overlay"

/** The test tag of the node that draws the sticker with this [key]; one node per sticker. */
fun layerNodeTag(key: Long): String = "layer-$key"

/**
 * The layer that draws the layers on the shown picture and lets the person move them. It
 * fills the box of the shown picture it is given (PhotoCard puts it inside the box that carries
 * `TOOL_IMAGE_TAG`), clips everything to it, and works in that box's own coordinates: a sticker's
 * place is stored as fractions of the ORIGINAL photo and [mapping] (the one translation between
 * the original and the shown picture, which knows the crop, the turns and the flips) turns it
 * into pixels here, and back.
 *
 * The overlay draws NO sticker picture: the pictures are part of the live picture beneath it (the
 * filtered preview holds the layers at rest, the live preview itself draws the one under a
 * finger), so nothing is drawn twice. Each sticker is one invisible accessibility node tagged
 * [layerNodeTag], oldest first, at the sticker's place. The selected one also gets a frame with
 * four corner buttons ([LayerFrame]).
 *
 * **Touch.** While [enabled] is true (the Stickers sheet is open, Compare is off and Done is not
 * running) ONE handler on this whole layer ([layerGestures]) reads the raw touches and decides
 * which sticker is under a finger; while it is false no pointer input is composed at all, the
 * frame and its buttons are gone, and the layers are only drawn.
 *
 * The overlay keeps no state of its own about the layers: everything comes in as a parameter
 * and every change goes out as a callback, so the screen's ViewModel stays the one owner.
 *
 * @param layers the layers on the photo, oldest first (as in `EditSettings.layers`).
 * @param selectedKey the key of the selected sticker, or null for none.
 * @param mapping the translation between the shown picture and the original photo.
 * @param enabled whether touches work and the frame shows.
 * @param onSelect a sticker was touched (its key), or empty photo was (null).
 * @param onPreview a move, pinch, turn or resize is under way: the complete new list. It makes no
 * undo entry; the screen shows it at once.
 * @param onCommit the gesture is over: make the previewed list one undo step. It is called once
 * per gesture, after at least one [onPreview].
 * @param onDelete the delete button of the sticker with this key was tapped.
 * @param onDuplicate the duplicate button of the sticker with this key was tapped.
 * @param onFlip the flip button of the sticker with this key was tapped.
 * @param onEdit the edit button of the text with this key was tapped (a text's top-right button).
 * @param onTap the layer with this key was tapped while it was already selected (a date then
 * moves to its next format).
 */
@Composable
fun LayerOverlay(
    layers: List<Layer>,
    selectedKey: Long?,
    mapping: LayerMapping,
    enabled: Boolean,
    onSelect: (Long?) -> Unit,
    onPreview: (List<Layer>) -> Unit,
    onCommit: () -> Unit,
    onDelete: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onFlip: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (Long) -> Unit = {},
    onTap: (Long) -> Unit = {},
) {
    // A shape per layer, in the same order (null for a sticker the catalogue lacks: not drawn).
    val textLayout = rememberTextLayerLayout()
    val shapes = layers.map { rememberLayerAspect(it.content, textLayout) }
    val aspects = layers.indices.associate { layers[it].key to (shapes[it] ?: 1f) }

    // The handlers outlive a recomposition, so they read the latest values through these.
    val latestStickers by rememberUpdatedState(layers)
    val latestSelected by rememberUpdatedState(selectedKey)
    val latestMapping by rememberUpdatedState(mapping)
    val latestAspects by rememberUpdatedState(aspects)
    // The keys of the layers that are really drawn: only those can be touched.
    val drawnKeys = layers.indices.filter { shapes[it] != null }.map { layers[it].key }.toSet()
    val latestDrawn by rememberUpdatedState(drawnKeys)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestOnPreview by rememberUpdatedState(onPreview)
    val latestOnCommit by rememberUpdatedState(onCommit)
    val latestOnTap by rememberUpdatedState(onTap)

    var origin by remember { mutableStateOf(Offset.Zero) }
    // The overlay's size in pixels, which the touch handler reads when a gesture starts.
    var sizePx by remember { mutableStateOf(Size.Zero) }
    val buttonRadiusPx = with(LocalDensity.current) { (LAYER_BUTTON_TARGET / 2).toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag(LAYER_OVERLAY_TAG)
            .onSizeChanged { sizePx = Size(it.width.toFloat(), it.height.toFloat()) }
            .onGloballyPositioned { origin = it.positionInRoot() }
            .then(
                if (enabled) {
                    Modifier.layerGestures(
                        layers = { latestStickers },
                        selectedKey = { latestSelected },
                        mapping = { latestMapping },
                        size = { sizePx },
                        aspectOf = { latestAspects[it.key] ?: 1f },
                        buttonRadiusPx = buttonRadiusPx,
                        onSelect = { latestOnSelect(it) },
                        onPreview = { latestOnPreview(it) },
                        onCommit = { latestOnCommit() },
                        isDrawn = { it.key in latestDrawn },
                        hasTopRightButton = { it.content !is LayerContent.Date },
                        // Only a date answers a tap (it moves to its next format, ADR-006).
                        takesTaps = { it.content is LayerContent.Date },
                        onTap = { latestOnTap(it) },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        val area = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

        // Oldest first: the Box draws its children in order, so the last is on top.
        layers.forEachIndexed { index, sticker ->
            val aspect = shapes[index] ?: return@forEachIndexed
            key(sticker.key) {
                val centre = sticker.shownCentre(mapping, area)
                val width = sticker.shownWidth(mapping, area)
                val content = sticker.content
                LayerPlace(
                    centreX = centre.x,
                    centreY = centre.y,
                    widthPx = width,
                    heightPx = width * aspect,
                    degrees = sticker.shownDegrees(mapping),
                    modifier = Modifier
                        .testTag(layerNodeTag(sticker.key))
                        .semantics { selected = sticker.key == selectedKey },
                    hint = if (content is LayerContent.Text && content.isBlank) {
                        { TextPlaceholder(content = content, widthPx = width, layout = textLayout) }
                    } else {
                        null
                    },
                )
            }
        }

        val selectedIndex = layers.indexOfFirst { it.key == selectedKey }
        if (enabled && selectedIndex >= 0 && shapes[selectedIndex] != null) {
            val sticker = layers[selectedIndex]
            val centre = sticker.shownCentre(mapping, area)
            val width = sticker.shownWidth(mapping, area)
            LayerFrame(
                centreX = centre.x,
                centreY = centre.y,
                widthPx = width,
                heightPx = width * (aspects[sticker.key] ?: 1f),
                degrees = sticker.shownDegrees(mapping),
                overlayOrigin = { origin },
                minWidthPx = MIN_LAYER_WIDTH_FRACTION * area.width,
                maxWidthPx = MAX_LAYER_WIDTH_FRACTION * area.width,
                onDelete = { onDelete(sticker.key) },
                onDuplicate = { onDuplicate(sticker.key) },
                // A date has no flip: mirrored digits cannot be read (ADR-006).
                onFlip = if (sticker.content is LayerContent.Date) null else { { onFlip(sticker.key) } },
                // A text's top-right button edits its words instead of flipping it (ADR-005).
                onEdit = if (sticker.content is LayerContent.Text) { { onEdit(sticker.key) } } else null,
                onResize = { newWidth, newDegrees ->
                    // Same centre, new width and turn, carried back to the original's fractions.
                    // The centre is kept inside the picture, like in a drag.
                    val resized = sticker.placedAt(mapping, area, clampCentre(centre, area), newWidth, newDegrees)
                    latestOnPreview(latestStickers.map { if (it.key == sticker.key) resized else it })
                },
                onResizeEnd = { latestOnCommit() },
            )
        }
    }
}

/**
 * Two overlapping sample layers on a 300 dp square, for the previews. The overlay draws only
 * the frame and the buttons now (the pictures belong to the live picture beneath), so the
 * preview shows the selected sticker's frame over an empty square.
 */
@Composable
private fun SampleStickerOverlay(selectedKey: Long?, enabled: Boolean) {
    val first = StickerCatalog.all[0].pictureId
    val second = StickerCatalog.all[1].pictureId
    val layers = listOf(
        Layer(key = 1, content = LayerContent.Picture(first), centreX = 0.35f, centreY = 0.4f, width = 0.4f, degrees = -15f, mirrored = false),
        Layer(key = 2, content = LayerContent.Picture(second), centreX = 0.6f, centreY = 0.6f, width = 0.35f, degrees = 20f, mirrored = true),
    )
    KansoTheme(themeMode = ThemeMode.System) {
        // The theme stretches its single child to the whole screen, so a Box goes first.
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .size(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                LayerOverlay(
                    layers = layers,
                    selectedKey = selectedKey,
                    mapping = LayerMapping(1000, 1000, EditSettings()),
                    enabled = enabled,
                    onSelect = {},
                    onPreview = {},
                    onCommit = {},
                    onDelete = {},
                    onDuplicate = {},
                    onFlip = {},
                )
            }
        }
    }
}

/** Two layers, the newer one selected with its frame and four buttons, in light and in dark. */
@PreviewLightDark
@Composable
private fun StickerOverlayPreview() {
    SampleStickerOverlay(selectedKey = 2L, enabled = true)
}

/** The same layers with the overlay not enabled: drawn, but no frame and no buttons. */
@PreviewLightDark
@Composable
private fun StickerOverlayDisabledPreview() {
    SampleStickerOverlay(selectedKey = 2L, enabled = false)
}

/**
 * The faint "Your text" shown in the frame of a text that has no letters yet, [widthPx] wide, so
 * the person sees where the text will appear before typing. It is drawn by the overlay only:
 * the live picture and the saved photo never show it.
 */
@Composable
private fun TextPlaceholder(content: LayerContent.Text, widthPx: Float, layout: TextLayerLayout) {
    val w = widthPx.roundToInt().coerceAtLeast(1)
    val hint = remember(content, w, layout) {
        layout.render(content, w, LayerContent.Text.PLACEHOLDER, alpha = PLACEHOLDER_ALPHA)?.asImageBitmap()
    }
    if (hint != null) {
        Image(bitmap = hint, contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

/** How solid the placeholder of an empty text is drawn: half, so it reads as a hint. */
private const val PLACEHOLDER_ALPHA = 0.5f
