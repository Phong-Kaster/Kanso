package com.phongkaster.kanso.ui.tool.component.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_DONE_PROGRESS_TAG
import com.phongkaster.kanso.ui.tool.TOOL_DONE_TAG

/**
 * The Done button at the top right of every tool screen: a small filled button with the word
 * "Done", calling [onClick] to finish the edit and open the Result screen. While [enabled] is
 * false (the photo not shown yet, an edit running, or Done already working) it is drawn faint and
 * ignores taps.
 *
 * A filled button is Material's strongest button, used here because Done is the screen's main way
 * forward; its theme colours (`primary` behind `onPrimary` text) keep the word readable in light
 * and dark. Material gives every button a touch target of at least 48 dp, even where the drawn
 * button is smaller.
 *
 * While [finishing] (the Edit screen rendering the finished picture), a small spinner stands
 * before the word, so the person sees that Done is working. It is a `CircularProgressIndicator`
 * without a value: it turns for as long as it is shown and stops when the render ends. Its
 * colour is `LocalContentColor`, the colour the button hands to everything inside it, so it
 * always matches the word next to it. It has no description of its own: the word "Done" names the
 * button, and the button is switched off while it turns.
 */
@Composable
internal fun DoneButton(
    enabled: Boolean,
    finishing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.testTag(TOOL_DONE_TAG),
    ) {
        // AnimatedVisibility grows the spinner in when the render starts and out when it ends.
        // Inside the button's row this is the RowScope version, which grows it sideways.
        AnimatedVisibility(visible = finishing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(TOOL_DONE_PROGRESS_TAG),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
        }
        Text(stringResource(R.string.tool_done))
    }
}

/** Done in light and dark: usable, then switched off. The spinner state never stops turning, so no still preview shows it. */
@PreviewLightDark
@Composable
private fun DoneButtonPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Row {
            DoneButton(enabled = true, finishing = false, onClick = {})
            DoneButton(enabled = false, finishing = false, onClick = {})
        }
    }
}
