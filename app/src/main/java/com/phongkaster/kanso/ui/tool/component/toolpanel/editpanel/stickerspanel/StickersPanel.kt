package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.CatalogSticker
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the whole Stickers panel (top bar, category chips and grid). */
const val STICKERS_PANEL_TAG = "stickers-panel"

/** Test tag of the grid of sticker tiles. */
const val STICKERS_GRID_TAG = "stickers-grid"

/** How many tiles sit side by side in the grid. */
private const val GRID_COLUMNS = 3

/**
 * The Stickers sheet's contents: a top bar with Cancel and Confirm, a row of category chips, and
 * a grid of the stickers of [category], three to a row, scrolling vertically.
 *
 * It has no state of its own: [category] is the chosen chip, a chip tap calls [onCategoryChosen],
 * a tile tap calls [onStickerChosen] with that [CatalogSticker], and the top bar buttons call
 * [onCancel] and [onConfirm]. While [enabled] is false every tile ignores taps.
 *
 * The panel fills the height its sheet gives it and never grows it: the grid takes what the top
 * bar and chips leave and scrolls the rest. `LazyVerticalGrid` composes only the tiles on screen
 * plus a few more as it scrolls, so a long category costs little.
 */
@Composable
internal fun StickersPanel(
    category: StickerCategory,
    enabled: Boolean,
    onCategoryChosen: (StickerCategory) -> Unit,
    onStickerChosen: (CatalogSticker) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().testTag(STICKERS_PANEL_TAG)) {
        StickersSheetTopBar(onCancel = onCancel, onConfirm = onConfirm)
        StickerCategoryChips(selected = category, onSelect = onCategoryChosen)
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.fillMaxWidth().weight(1f).testTag(STICKERS_GRID_TAG),
            horizontalArrangement = Arrangement.Center,
        ) {
            items(items = StickerCatalog.inCategory(category), key = { it.pictureId }) { sticker ->
                StickerTile(
                    sticker = sticker,
                    enabled = enabled,
                    onClick = { onStickerChosen(sticker) },
                    modifier = Modifier.height(56.dp),
                )
            }
        }
    }
}

/** The panel in a sheet-sized box, in light and in dark, showing the Food category. */
@PreviewLightDark
@Composable
private fun StickersPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                StickersPanel(
                    category = StickerCategory.FOOD,
                    enabled = true,
                    onCategoryChosen = {},
                    onStickerChosen = {},
                    onCancel = {},
                    onConfirm = {},
                )
            }
        }
    }
}
