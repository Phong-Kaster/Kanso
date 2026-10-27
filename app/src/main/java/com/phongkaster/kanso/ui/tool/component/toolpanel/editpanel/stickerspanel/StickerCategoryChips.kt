package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the row of sticker category chips. */
const val STICKER_CHIPS_TAG = "sticker-chips"

/** Returns the test tag of [category]'s chip, such as `sticker-chip-FOOD`. */
fun stickerChipTag(category: StickerCategory): String = "sticker-chip-${category.name}"

/**
 * The row of category chips above the sticker grid: one pill per [StickerCategory], in the
 * enum's order, scrolling sideways when they do not fit.
 *
 * Like Pinity's catalogue the chips are fully rounded pills with a thin outline, and the chosen one
 * is filled. The colours come from the theme's colour roles (the default [FilterChip] colours),
 * so light, dark and the user's theme colour all work. [FilterChip] also reports its `selected`
 * state to accessibility services and tests.
 *
 * @param selected the category whose layers are shown.
 * @param onSelect called with the category the user taps.
 */
@Composable
internal fun StickerCategoryChips(
    selected: StickerCategory,
    onSelect: (StickerCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    // LazyRow draws only the chips on screen and scrolls sideways on its own.
    LazyRow(
        modifier = modifier.testTag(STICKER_CHIPS_TAG),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(StickerCategory.entries, key = { it.name }) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category.label)) },
                shape = CircleShape,
                modifier = Modifier.testTag(stickerChipTag(category)),
            )
        }
    }
}

/**
 * The category chips with the first category chosen, in light and in dark. The sample sits in a
 * full-size Box because `KansoTheme` stretches its direct child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun StickerCategoryChipsPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            StickerCategoryChips(selected = StickerCategory.entries.first(), onSelect = {})
        }
    }
}
