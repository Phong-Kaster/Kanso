package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Text sheet's row of font chips. */
const val TEXT_FONTS_TAG = "text-fonts"

/** Returns the test tag of the font chip for [font], such as `text-font-SPACE_MONO`. */
fun textFontTag(font: TextFont): String = "text-font-${font.name}"

/**
 * The Text sheet's row of font families, scrolling sideways: "None" (the phone's own font) and
 * then one chip per bundled font, each showing "Aa" drawn in that very font, so the person sees
 * the style before choosing it. The chip of [chosen] is marked; a tap calls [onChoose]. Every chip
 * keeps Material's 48 dp touch target and carries the font's name for screen readers.
 */
@Composable
internal fun TextFontRow(
    chosen: TextFont?,
    enabled: Boolean,
    onChoose: (TextFont) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(TEXT_FONTS_TAG),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            TextFont.entries.forEach(
                action = { font ->
                    val name = stringResource(font.label())
                    FilterChip(
                        selected = font == chosen,
                        onClick = { onChoose(font) },
                        enabled = enabled,
                        label = {
                            Text(
                                text = stringResource(if (font == TextFont.NONE) R.string.text_font_none else R.string.text_font_sample),
                                fontFamily = font.res?.let { FontFamily(Font(it)) },
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        modifier = Modifier
                            .testTag(textFontTag(font))
                            .semantics { contentDescription = name },
                    )
                },
            )
        },
    )
}

/** The spoken name of this font family. */
@StringRes
private fun TextFont.label(): Int = when (this) {
    TextFont.NONE -> R.string.text_font_none_description
    TextFont.BE_VIETNAM_PRO -> R.string.text_font_be_vietnam_pro
    TextFont.PLAYFAIR_DISPLAY -> R.string.text_font_playfair_display
    TextFont.DANCING_SCRIPT -> R.string.text_font_dancing_script
    TextFont.PATRICK_HAND -> R.string.text_font_patrick_hand
    TextFont.SPACE_MONO -> R.string.text_font_space_mono
}

/** The font row with Dancing Script chosen, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextFontRowPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = { TextFontRow(chosen = TextFont.DANCING_SCRIPT, enabled = true, onChoose = {}) },
        )
    }
}
