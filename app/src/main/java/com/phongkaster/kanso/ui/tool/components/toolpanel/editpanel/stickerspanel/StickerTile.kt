package com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.stickerspanel

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.CatalogSticker
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Returns the test tag of the grid tile showing the sticker with [pictureId], such as `sticker-tile-apple`. */
fun stickerTileTag(pictureId: String): String = "sticker-tile-$pictureId"

/** How faint a disabled tile is drawn: Material's usual opacity for disabled content. */
private const val TILE_DISABLED_ALPHA = 0.38f

/**
 * One tile of the sticker grid: the picture of [sticker], in a clickable square at least 48 dp
 * on each side so a finger can hit it. A tap calls [onClick]; the screen then adds that sticker
 * to the photo. While [enabled] is false (Compare is on, or Done is rendering) the tile is drawn
 * faint and ignores taps.
 *
 * The picture is a bundled bitmap (a webp drawable), so `painterResource` reads it directly. Its
 * content description is a short generic phrase, because the pictures have no individual names.
 */
@Composable
internal fun StickerTile(
    sticker: CatalogSticker,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .testTag(stickerTileTag(sticker.pictureId))
            .alpha(if (enabled) 1f else TILE_DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(sticker.res),
            contentDescription = stringResource(R.string.stickers_sticker_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Two sample tiles, one enabled and one disabled, in light and in dark. */
@PreviewLightDark
@Composable
private fun StickerTilePreview() {
    val sample = CatalogSticker(
        pictureId = "apple",
        res = R.drawable.img_sticker_apple,
        category = StickerCategory.FOOD,
    )
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                androidx.compose.foundation.layout.Row {
                    StickerTile(sticker = sample, enabled = true, onClick = {}, modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp))
                    StickerTile(sticker = sample, enabled = false, onClick = {}, modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp))
                }
            }
        }
    }
}
