package com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.stickerspanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Stickers sheet's cancel (X) button. */
const val STICKERS_CANCEL_TAG = "stickers-cancel"

/** Test tag of the Stickers sheet's confirm (check) button. */
const val STICKERS_CONFIRM_TAG = "stickers-confirm"

/**
 * The top row of the Stickers sheet: a cross at the start that throws the sticker changes away
 * ([onCancel]) and a check at the end that keeps them ([onConfirm]).
 *
 * Each button is an [IconButton], whose 48 dp square is the minimum comfortable touch target. The
 * icons are Material Symbols drawables drawn with [Icon], which tints them with the content
 * colour (the files paint white, so they must not be drawn as a plain image).
 */
@Composable
internal fun StickersSheetTopBar(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // Space between pushes the cross to the start and the check to the end.
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel, modifier = Modifier.size(48.dp).testTag(STICKERS_CANCEL_TAG)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.stickers_sheet_cancel),
            )
        }
        IconButton(onClick = onConfirm, modifier = Modifier.size(48.dp).testTag(STICKERS_CONFIRM_TAG)) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.stickers_sheet_confirm),
            )
        }
    }
}

/**
 * The Stickers sheet's top bar, in light and in dark. The sample sits in a full-size Box because
 * `KansoTheme` stretches its direct child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun StickersSheetTopBarPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            StickersSheetTopBar(onCancel = {}, onConfirm = {})
        }
    }
}
