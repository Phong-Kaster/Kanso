package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag of the Reset button. */
internal const val CURVE_RESET_TAG = "curve-reset"

/**
 * The Reset button of the Curves sheet: puts the curve back to the straight diagonal when pressed.
 *
 * It is a [TextButton] at least 48 dp by 48 dp, so a finger can always hit it. Its spoken name is
 * its text, which a screen reader reads on its own. While [enabled] is false (the curve is already
 * straight, or the sheet is locked) it is dimmed and does nothing.
 */
@Composable
internal fun CurveResetButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).testTag(CURVE_RESET_TAG),
    ) {
        Text(text = stringResource(R.string.edit_curves_reset))
    }
}

/** The button enabled, in light and in dark. */
@PreviewLightDark
@Composable
private fun CurveResetButtonPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box(modifier = Modifier.fillMaxSize()) {
                CurveResetButton(enabled = true, onClick = {})
            }
        }
    }
}
