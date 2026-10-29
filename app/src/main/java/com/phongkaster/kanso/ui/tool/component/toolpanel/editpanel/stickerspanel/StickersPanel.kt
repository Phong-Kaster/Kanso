package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.CatalogSticker
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.LayerSize
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerSheetTopBar
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerOpacitySlider
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerSizeSlider
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerTabRow
import com.phongkaster.kanso.ui.tool.model.StickersTab

/** Test tag of the whole Stickers panel (top bar, sliders, category chips and grid). */
const val STICKERS_PANEL_TAG = "stickers-panel"

/** Test tag of the grid of sticker tiles. */
const val STICKERS_GRID_TAG = "stickers-grid"

/** How many tiles sit side by side in the grid. */
private const val GRID_COLUMNS = 3

/** The height of one tile of the grid. */
private val TILE_HEIGHT = 56.dp

/**
 * The Stickers sheet's contents, in three parts (the owner's decision, 2026-10-28):
 *
 * 1. the top bar with Cancel ([onCancel]) and Confirm ([onConfirm]), pinned: it never scrolls;
 * 2. the row of tabs ([LayerTabRow], [StickersTab]): Stickers, Size, Opacity;
 * 3. the chosen tab's control: for Stickers, a row of category chips and a grid of the stickers of
 *    [category], three to a row, filling the rest of the sheet and scrolling on its own; for Size
 *    and Opacity, one slider for the selected sticker ([LayerSizeSlider], [LayerOpacitySlider]).
 *
 * The sheet opens on the Stickers tab, and the chosen tab stays while it is open. Size and Opacity
 * need a selected sticker: with none they are greyed out and the Stickers tab shows.
 *
 * It has no state of its own about the stickers: [category] is the chosen chip, a chip tap calls
 * [onCategoryChosen], a tile tap calls [onStickerChosen] with that [CatalogSticker]. [selected] is
 * the sticker selected on the photo, or null; the sliders show its size (its width in percent of
 * the photo, see [LayerSize]) and opacity. A slider moving calls [onSelectedChange] with the whole
 * changed sticker (no undo step yet) and letting go calls [onSelectedChangeFinished] (one undo
 * step). While [enabled] is false every tab, tile and slider ignores touches. The one thing it
 * remembers is the chosen tab.
 *
 * `LazyVerticalGrid` composes only the tiles on screen plus a few more as it scrolls, so a long
 * category costs little.
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
    selected: Layer? = null,
    onSelectedChange: (Layer) -> Unit = {},
    onSelectedChangeFinished: () -> Unit = {},
) {
    var chosenTab by rememberSaveable { mutableStateOf(StickersTab.STICKERS) }
    // Size and Opacity need a selected sticker; without one the sheet shows its first tab.
    val tab = if (selected == null && chosenTab.needsSelection) StickersTab.STICKERS else chosenTab
    Column(
        modifier = modifier.fillMaxSize().testTag(STICKERS_PANEL_TAG),
        content = {
            // Pinned: above everything that changes or scrolls, so it never moves.
            LayerSheetTopBar(
                title = stringResource(R.string.stickers_tool_label),
                cancelDescription = stringResource(R.string.stickers_sheet_cancel),
                confirmDescription = stringResource(R.string.stickers_sheet_confirm),
                onCancel = onCancel,
                onConfirm = onConfirm,
            )
            LayerTabRow(
                tabs = StickersTab.entries,
                chosen = tab,
                label = { it.labelRes },
                usable = { !it.needsSelection || selected != null },
                enabled = enabled,
                onChoose = { chosenTab = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            when (tab) {
                StickersTab.STICKERS -> {
                    StickerCategoryChips(selected = category, onSelect = onCategoryChosen)
                    // weight(1f): the grid takes all the height left and scrolls the rest.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(GRID_COLUMNS),
                        modifier = Modifier.fillMaxWidth().weight(1f).testTag(STICKERS_GRID_TAG),
                        horizontalArrangement = Arrangement.Center,
                        content = {
                            items(
                                items = StickerCatalog.inCategory(category),
                                key = { it.pictureId },
                                itemContent = { sticker ->
                                    StickerTile(
                                        sticker = sticker,
                                        enabled = enabled,
                                        onClick = { onStickerChosen(sticker) },
                                        modifier = Modifier.height(TILE_HEIGHT),
                                    )
                                },
                            )
                        },
                    )
                }
                StickersTab.SIZE -> LayerSizeSlider(
                    size = selected?.let { LayerSize.ofPicture(it.width) } ?: LayerSize.MIN,
                    enabled = enabled && selected != null,
                    onSizeChange = { size -> selected?.let { onSelectedChange(it.copy(width = LayerSize.pictureWidth(size))) } },
                    onChangeFinished = onSelectedChangeFinished,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                StickersTab.OPACITY -> LayerOpacitySlider(
                    opacity = selected?.opacity ?: 1f,
                    enabled = enabled && selected != null,
                    onOpacityChange = { opacity -> selected?.let { onSelectedChange(it.copy(opacity = opacity)) } },
                    onChangeFinished = onSelectedChangeFinished,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        },
    )
}

/** The panel in a sheet-sized box, in light and in dark, showing the Food category and a selected sticker. */
@PreviewLightDark
@Composable
private fun StickersPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(208.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = {
                        StickersPanel(
                            category = StickerCategory.FOOD,
                            enabled = true,
                            onCategoryChosen = {},
                            onStickerChosen = {},
                            onCancel = {},
                            onConfirm = {},
                            selected = Layer(
                                key = 1,
                                content = LayerContent.Picture(StickerCatalog.all.first().pictureId),
                                centreX = 0.5f,
                                centreY = 0.5f,
                                width = 0.3f,
                                degrees = 0f,
                                mirrored = false,
                                opacity = 0.8f,
                            ),
                        )
                    },
                )
            },
        )
    }
}
