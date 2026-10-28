package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.component.CustomColourPicker
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Text sheet's colour picker pane. */
const val TEXT_COLOUR_PANE_TAG = "text-colour-pane"

/** Test tag of the colour picker pane's back arrow. */
const val TEXT_COLOUR_BACK_TAG = "text-colour-back"

/**
 * The Text sheet's colour picker, which takes the place of the sheet's rows while it is open, so
 * the photo stays in sight and the text changes colour live as the person drags. At the top, a
 * back arrow ([onBack]) and the title "Colour"; below, the same HSV picker as Settings → Theme
 * colour ([CustomColourPicker]), starting at [startArgb], scrolling inside the sheet when the
 * sheet is shorter than the picker. Every colour picked calls [onColour] with it, opaque.
 */
@Composable
internal fun TextColourPane(
    startArgb: Int,
    onColour: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().testTag(TEXT_COLOUR_PANE_TAG),
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).testTag(TEXT_COLOUR_BACK_TAG),
                        content = {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.text_colour_back),
                            )
                        },
                    )
                    Text(
                        text = stringResource(R.string.text_colour_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                content = {
                    CustomColourPicker(seedArgb = startArgb, onColorChanged = { onColour(it.toArgb()) })
                },
            )
        },
    )
}

/** The pane starting at red, in a sheet-sized box, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextColourPanePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = { TextColourPane(startArgb = 0xFFE53935.toInt(), onColour = {}, onBack = {}) },
        )
    }
}
