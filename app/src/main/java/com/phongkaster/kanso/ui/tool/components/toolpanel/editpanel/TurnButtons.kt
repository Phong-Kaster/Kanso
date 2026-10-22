package com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Edit panel's "Turn left" button (a quarter turn anticlockwise). */
const val EDIT_ROTATE_LEFT_TAG = "edit-rotate-left"

/** Test tag of the Edit panel's "Turn right" button (a quarter turn clockwise). */
const val EDIT_ROTATE_RIGHT_TAG = "edit-rotate-right"

/** Test tag of the Edit panel's button that mirrors the photo left to right. */
const val EDIT_FLIP_HORIZONTAL_TAG = "edit-flip-horizontal"

/** Test tag of the Edit panel's button that mirrors the photo top to bottom. */
const val EDIT_FLIP_VERTICAL_TAG = "edit-flip-vertical"

/**
 * The turn and flip buttons in one row, scrolling sideways if the screen is narrow: "Turn left"
 * and "Turn right" call [onTurn] with -1 and +1 quarter turns, "Flip across" and "Flip upside
 * down" call [onFlip] (true for left to right, false for top to bottom; a second tap flips back).
 * While [enabled] is false all four are disabled.
 */
@Composable
internal fun TurnButtons(enabled: Boolean, onTurn: (Int) -> Unit, onFlip: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditFeatureTile(
            icon = R.drawable.ic_rotate_left,
            title = R.string.edit_tool_rotate_left,
            onClick = { onTurn(-1) },
            enabled = enabled,
            modifier = Modifier.testTag(EDIT_ROTATE_LEFT_TAG),
        )
        EditFeatureTile(
            icon = R.drawable.ic_rotate_right,
            title = R.string.edit_tool_rotate_right,
            onClick = { onTurn(1) },
            enabled = enabled,
            modifier = Modifier.testTag(EDIT_ROTATE_RIGHT_TAG),
        )
        EditFeatureTile(
            icon = R.drawable.ic_flip_horizontal,
            title = R.string.edit_tool_flip_horizontal,
            onClick = { onFlip(true) },
            enabled = enabled,
            modifier = Modifier.testTag(EDIT_FLIP_HORIZONTAL_TAG),
        )
        EditFeatureTile(
            icon = R.drawable.ic_flip_vertical,
            title = R.string.edit_tool_flip_vertical,
            onClick = { onFlip(false) },
            enabled = enabled,
            modifier = Modifier.testTag(EDIT_FLIP_VERTICAL_TAG),
        )
    }
}

/** The Rotate and Flip buttons, in light and in dark. */
@PreviewLightDark
@Composable
private fun TurnButtonsPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        TurnButtons(enabled = true, onTurn = {}, onFlip = {})
    }
}
