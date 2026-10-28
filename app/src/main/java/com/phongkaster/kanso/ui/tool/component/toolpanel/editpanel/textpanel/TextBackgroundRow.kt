package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.model.TextSwatch

/** Test tag of the Text and Date sheets' row of background colours. */
const val TEXT_BACKGROUNDS_TAG = "text-backgrounds"

/** Test tag of the background row's first circle, "None": no box behind the words. */
const val TEXT_BACKGROUND_NONE_TAG = "text-background-none"

/** Test tag of the background row's last circle, which opens the picker with transparency. */
const val TEXT_CUSTOM_BACKGROUND_TAG = "text-background-custom"

/** Returns the test tag of the background circle of [swatch], such as `text-background-RED`. */
fun textBackgroundTag(swatch: TextSwatch): String = "text-background-${swatch.name}"

/**
 * The background row of the Text and Date sheets (their Background tab), laid out like the Colour
 * row, scrolling sideways: a slashed "None" circle (no box behind the words,
 * the default), the twelve ready colours ([TextSwatch]) as solid boxes, and a rainbow circle
 * ([onCustom]) that opens the colour picker with its transparency bar, for any colour at any
 * see-through strength.
 *
 * [chosenArgb] is the box's colour, or null for none: the matching circle gets the ring; the
 * rainbow gets it for a colour none of the twelve has (a see-through white included). A tap on
 * None or a ready colour calls [onChoose] with null or that colour. The letters keep their own
 * colour from the Colour row.
 */
@Composable
internal fun TextBackgroundRow(
    chosenArgb: Int?,
    enabled: Boolean,
    onChoose: (Int?) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val custom = chosenArgb != null && TextSwatch.entries.none { it.argb == chosenArgb }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(TEXT_BACKGROUNDS_TAG),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            ColourCircle(
                fill = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                chosen = chosenArgb == null,
                enabled = enabled,
                description = stringResource(R.string.text_background_none),
                onClick = { onChoose(null) },
                modifier = Modifier.testTag(TEXT_BACKGROUND_NONE_TAG),
                slashed = true,
            )
            TextSwatch.entries.forEach(
                action = { swatch ->
                    ColourCircle(
                        fill = Brush.linearGradient(listOf(Color(swatch.argb), Color(swatch.argb))),
                        chosen = swatch.argb == chosenArgb,
                        enabled = enabled,
                        description = stringResource(R.string.text_background_swatch, stringResource(swatch.label)),
                        onClick = { onChoose(swatch.argb) },
                        modifier = Modifier.testTag(textBackgroundTag(swatch)),
                    )
                },
            )
            ColourCircle(
                fill = Brush.sweepGradient(RAINBOW),
                chosen = custom,
                enabled = enabled,
                description = stringResource(R.string.text_background_custom),
                onClick = onCustom,
                modifier = Modifier.testTag(TEXT_CUSTOM_BACKGROUND_TAG),
            )
        },
    )
}

/** The background row with no box chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextBackgroundRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = { TextBackgroundRow(chosenArgb = null, enabled = true, onChoose = {}, onCustom = {}) },
        )
    }
}

/** The background row with a yellow box chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextBackgroundRowYellowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = { TextBackgroundRow(chosenArgb = TextSwatch.YELLOW.argb, enabled = true, onChoose = {}, onCustom = {}) },
        )
    }
}
