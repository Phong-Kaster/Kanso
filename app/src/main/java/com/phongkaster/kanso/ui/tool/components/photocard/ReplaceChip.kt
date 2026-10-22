package com.phongkaster.kanso.ui.tool.components.photocard

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_REPLACE_TAG

/**
 * The "Replace" chip at the bottom of the photo card: a pill with the swap icon and the word
 * "Replace", calling [onClick] when tapped (and doing nothing while [enabled] is false).
 *
 * A chip is Material's small, rounded control for one quick action next to some content. The
 * elevated assist chip has a solid container in the theme's `surfaceContainerLow` colour and a
 * small shadow, so its word stays readable over any photo (a flat chip is see-through). Its
 * default corners are small; `CircleShape` rounds them fully into a pill. Its touch target is at
 * least 48 dp tall although the drawn chip is 32 dp.
 */
@Composable
internal fun ReplaceChip(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedAssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(stringResource(R.string.tool_screen_replace)) },
        leadingIcon = {
            // No description of its own: the word "Replace" next to it already says it.
            Icon(
                painter = painterResource(R.drawable.ic_swap_horiz),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        shape = CircleShape,
        modifier = modifier.testTag(TOOL_REPLACE_TAG),
    )
}

/** The Replace chip in light and dark, usable. */
@PreviewLightDark
@Composable
private fun ReplaceChipPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ReplaceChip(enabled = true, onClick = {})
    }
}
