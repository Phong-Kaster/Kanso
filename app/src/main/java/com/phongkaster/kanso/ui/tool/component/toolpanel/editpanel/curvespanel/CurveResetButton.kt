package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.curvespanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag of the Reset button. */
internal const val CURVE_RESET_TAG = "curve-reset"

/**
 * The Reset button of the Curves sheet: a 48 dp icon button (the circular-arrow drawable) that puts
 * the open channel's curve back to the straight diagonal when pressed.
 *
 * The icon is drawn with `Icon(painterResource(...))`, which tints it with the content colour; its
 * spoken name is the string `edit_curves_reset`. While [enabled] is false (the curve is already
 * straight, or the sheet is locked) it is dimmed and does nothing.
 */
@Composable
internal fun CurveResetButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(48.dp).testTag(CURVE_RESET_TAG),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = stringResource(R.string.edit_curves_reset),
        )
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
