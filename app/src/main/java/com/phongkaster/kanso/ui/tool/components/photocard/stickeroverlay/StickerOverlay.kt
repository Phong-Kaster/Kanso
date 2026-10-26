package com.phongkaster.kanso.ui.tool.components.photocard.stickeroverlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.phongkaster.kanso.data.edit.edit.Sticker
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerMapping
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/** Test tag of the sticker overlay's root: tests drive their touches through this node. */
const val STICKER_OVERLAY_TAG = "sticker-overlay"

/** The test tag of the node that draws the sticker with this [key]; one node per sticker. */
fun stickerNodeTag(key: Long): String = "sticker-$key"

/**
 * The layer that draws the stickers on the shown picture and lets the person move them. It
 * fills the box of the shown picture it is given (PhotoCard puts it inside the box that carries
 * `TOOL_IMAGE_TAG`), clips everything to it, and works in that box's own coordinates: a sticker's
 * place is stored as fractions of the ORIGINAL photo and [mapping] (the one translation between
 * the original and the shown picture, which knows the crop, the turns and the flips) turns it
 * into pixels here, and back.
 *
 * The overlay draws NO sticker picture: the pictures are part of the live picture beneath it (the
 * filtered preview holds the stickers at rest, the live preview itself draws the one under a
 * finger), so nothing is drawn twice. Each sticker is one invisible accessibility node tagged
 * [stickerNodeTag], oldest first, at the sticker's place. The selected one also gets a frame with
 * four corner buttons ([StickerFrame]).
 *
 * **Touch.** While [enabled] is true (the Stickers sheet is open, Compare is off and Done is not
 * running) ONE handler on this whole layer ([stickerGestures]) reads the raw touches and decides
 * which sticker is under a finger; while it is false no pointer input is composed at all, the
 * frame and its buttons are gone, and the stickers are only drawn.
 *
 * The overlay keeps no state of its own about the stickers: everything comes in as a parameter
 * and every change goes out as a callback, so the screen's ViewModel stays the one owner.
 *
 * @param stickers the stickers on the photo, oldest first (as in `EditSettings.stickers`).
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
 */
@Composable
fun StickerOverlay(
    stickers: List<Sticker>,
    selectedKey: Long?,
    mapping: StickerMapping,
    enabled: Boolean,
    onSelect: (Long?) -> Unit,
    onPreview: (List<Sticker>) -> Unit,
    onCommit: () -> Unit,
    onDelete: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onFlip: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A picture per sticker, in the same order (null for an id the catalogue lacks: not drawn).
    val painters = stickers.map { rememberStickerPainter(it.pictureId) }
    val aspects = stickers.indices.associate { stickers[it].key to (painters[it]?.aspectRatio() ?: 1f) }

    // The handlers outlive a recomposition, so they read the latest values through these.
    val latestStickers by rememberUpdatedState(stickers)
    val latestSelected by rememberUpdatedState(selectedKey)
    val latestMapping by rememberUpdatedState(mapping)
    val latestAspects by rememberUpdatedState(aspects)
    // The keys of the stickers that are really drawn: only those can be touched.
    val drawnKeys = stickers.indices.filter { painters[it] != null }.map { stickers[it].key }.toSet()
    val latestDrawn by rememberUpdatedState(drawnKeys)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestOnPreview by rememberUpdatedState(onPreview)
    val latestOnCommit by rememberUpdatedState(onCommit)

    var origin by remember { mutableStateOf(Offset.Zero) }
    // The overlay's size in pixels, which the touch handler reads when a gesture starts.
    var sizePx by remember { mutableStateOf(Size.Zero) }
    val buttonRadiusPx = with(LocalDensity.current) { (STICKER_BUTTON_TARGET / 2).toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag(STICKER_OVERLAY_TAG)
            .onSizeChanged { sizePx = Size(it.width.toFloat(), it.height.toFloat()) }
            .onGloballyPositioned { origin = it.positionInRoot() }
            .then(
                if (enabled) {
                    Modifier.stickerGestures(
                        stickers = { latestStickers },
                        selectedKey = { latestSelected },
                        mapping = { latestMapping },
                        size = { sizePx },
                        aspectOf = { latestAspects[it.key] ?: 1f },
                        buttonRadiusPx = buttonRadiusPx,
                        onSelect = { latestOnSelect(it) },
                        onPreview = { latestOnPreview(it) },
                        onCommit = { latestOnCommit() },
                        isDrawn = { it.key in latestDrawn },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        val area = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

        // Oldest first: the Box draws its children in order, so the last is on top.
        stickers.forEachIndexed { index, sticker ->
            val painter = painters[index] ?: return@forEachIndexed
            key(sticker.key) {
                val centre = sticker.shownCentre(mapping, area)
                val width = sticker.shownWidth(mapping, area)
                StickerPlace(
                    centreX = centre.x,
                    centreY = centre.y,
                    widthPx = width,
                    heightPx = width * painter.aspectRatio(),
                    degrees = sticker.shownDegrees(mapping),
                    modifier = Modifier
                        .testTag(stickerNodeTag(sticker.key))
                        .semantics { selected = sticker.key == selectedKey },
                )
            }
        }

        val selectedIndex = stickers.indexOfFirst { it.key == selectedKey }
        if (enabled && selectedIndex >= 0 && painters[selectedIndex] != null) {
            val sticker = stickers[selectedIndex]
            val centre = sticker.shownCentre(mapping, area)
            val width = sticker.shownWidth(mapping, area)
            StickerFrame(
                centreX = centre.x,
                centreY = centre.y,
                widthPx = width,
                heightPx = width * (aspects[sticker.key] ?: 1f),
                degrees = sticker.shownDegrees(mapping),
                overlayOrigin = { origin },
                minWidthPx = MIN_STICKER_WIDTH_FRACTION * area.width,
                maxWidthPx = MAX_STICKER_WIDTH_FRACTION * area.width,
                onDelete = { onDelete(sticker.key) },
                onDuplicate = { onDuplicate(sticker.key) },
                onFlip = { onFlip(sticker.key) },
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
 * Two overlapping sample stickers on a 300 dp square, for the previews. The overlay draws only
 * the frame and the buttons now (the pictures belong to the live picture beneath), so the
 * preview shows the selected sticker's frame over an empty square.
 */
@Composable
private fun SampleStickerOverlay(selectedKey: Long?, enabled: Boolean) {
    val first = StickerCatalog.all[0].pictureId
    val second = StickerCatalog.all[1].pictureId
    val stickers = listOf(
        Sticker(key = 1, pictureId = first, centreX = 0.35f, centreY = 0.4f, width = 0.4f, degrees = -15f, mirrored = false),
        Sticker(key = 2, pictureId = second, centreX = 0.6f, centreY = 0.6f, width = 0.35f, degrees = 20f, mirrored = true),
    )
    KansoTheme(themeMode = ThemeMode.System) {
        // The theme stretches its single child to the whole screen, so a Box goes first.
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .size(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                StickerOverlay(
                    stickers = stickers,
                    selectedKey = selectedKey,
                    mapping = StickerMapping(1000, 1000, EditSettings()),
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

/** Two stickers, the newer one selected with its frame and four buttons, in light and in dark. */
@PreviewLightDark
@Composable
private fun StickerOverlayPreview() {
    SampleStickerOverlay(selectedKey = 2L, enabled = true)
}

/** The same stickers with the overlay not enabled: drawn, but no frame and no buttons. */
@PreviewLightDark
@Composable
private fun StickerOverlayDisabledPreview() {
    SampleStickerOverlay(selectedKey = 2L, enabled = false)
}
