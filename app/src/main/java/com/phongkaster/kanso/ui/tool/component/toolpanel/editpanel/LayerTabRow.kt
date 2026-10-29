package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.model.TextTab

/** Test tag of a layer sheet's row of tabs. */
const val LAYER_TABS_TAG = "layer-tabs"

/** Returns the test tag of [tab]'s chip in a layer sheet's row of tabs, such as `layer-tab-COLOUR`. */
fun layerTabTag(tab: Enum<*>): String = "layer-tab-${tab.name}"

/**
 * The row of tabs of a layer sheet (Stickers, Text, Date), under its pinned top bar, scrolling
 * sideways: one Material filter chip per entry of [tabs], named by [label], the [chosen] one
 * marked. A tap calls [onChoose]; the sheet then shows that tab's one control under the row, so
 * the sheet's settings never crowd its small height (the owner's decision, 2026-10-28).
 *
 * A tab for which [usable] is false (it styles a layer and none is selected) is greyed out, and so
 * is every tab while [enabled] is false (Compare is on, or Done is rendering). Each chip keeps
 * Material's 48 dp touch target.
 *
 * It is generic: `T` is the sheet's own enum of tabs (`StickersTab`, `TextTab`, `DateTab`), so one
 * row serves all three sheets and each sheet keeps its own list.
 */
@Composable
internal fun <T : Enum<T>> LayerTabRow(
    tabs: List<T>,
    chosen: T,
    label: (T) -> Int,
    usable: (T) -> Boolean,
    enabled: Boolean,
    onChoose: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(LAYER_TABS_TAG),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            tabs.forEach(
                action = { tab ->
                    FilterChip(
                        selected = tab == chosen,
                        onClick = { onChoose(tab) },
                        enabled = enabled && usable(tab),
                        label = { Text(text = stringResource(label(tab))) },
                        modifier = Modifier.testTag(layerTabTag(tab)),
                    )
                },
            )
        },
    )
}

/** The Text sheet's tabs with Colour chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun LayerTabRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                LayerTabRow(
                    tabs = TextTab.entries,
                    chosen = TextTab.COLOUR,
                    label = { it.labelRes },
                    usable = { true },
                    enabled = true,
                    onChoose = {},
                )
            },
        )
    }
}

/** The Text sheet's tabs with nothing selected: only Text is usable, the rest greyed out. */
@PreviewLightDark
@Composable
private fun LayerTabRowNothingSelectedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                LayerTabRow(
                    tabs = TextTab.entries,
                    chosen = TextTab.TEXT,
                    label = { it.labelRes },
                    usable = { !it.needsSelection },
                    enabled = true,
                    onChoose = {},
                )
            },
        )
    }
}
