package com.phongkaster.kanso.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/**
 * The size of a round button: its visible circle and its touch target alike. 48 dp is the
 * smallest touch target Android recommends.
 */
private val ROUND_BUTTON_SIZE = 48.dp

/**
 * A round button showing one icon: a filled, tinted circle 48 dp across that calls [onClick] when
 * tapped. The tool screens, the gallery, Saved and Home use it wherever a single icon acts (Back,
 * Close, Share, the tool shortcuts), so every such button in the app looks the same.
 *
 * It is Material 3's `FilledTonalIconButton`: "tonal" means its circle is filled with the theme's
 * soft `secondaryContainer` colour and its icon drawn in the matching `onSecondaryContainer`, so
 * it stands out on any background without shouting like a primary button. Given
 * `IconButtonDefaults.shapes()` (a set of shapes instead of one), the circle squares its corners a
 * little while it is pressed and rounds them again when let go, animated with the theme's motion
 * scheme: the Material 3 Expressive press feedback.
 *
 * Why `size(48.dp)`: by default the button draws a 40 dp circle inside a 48 dp touch area, so the
 * edge of what can be tapped lies outside the circle. Sizing the button itself to 48 dp makes the
 * circle fill its whole node, which keeps the 48 dp touch target and makes the button read as
 * round right up to its bounds.
 *
 * The icon is a Material Symbols drawable ([iconRes]); `Icon` tints it with the button's content
 * colour (the file itself paints white). [contentDescription] is the button's spoken name, the
 * words a screen reader says, so it is required. Test tags are left to the caller's [modifier].
 */
@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(),
        modifier = modifier.size(ROUND_BUTTON_SIZE),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
        )
    }
}

/**
 * The round Back button at the top left of the screens that are not tabs: a [RoundIconButton]
 * with the back arrow, named "Back" for screen readers, that calls [onClick]. The caller pops the
 * back stack, and gives the button its test tag through [modifier].
 */
@Composable
fun RoundBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundIconButton(
        onClick = onClick,
        iconRes = R.drawable.ic_arrow_back,
        contentDescription = stringResource(R.string.back),
        modifier = modifier,
    )
}

/**
 * A round icon button in light and in dark, on the theme's background, showing the swap icon the
 * tool screen's Replace chip uses, named after it. The tap does nothing: a preview only draws.
 */
@PreviewLightDark
@Composable
private fun RoundIconButtonPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoundIconButton(
                    onClick = {},
                    iconRes = R.drawable.ic_swap_horiz,
                    contentDescription = stringResource(R.string.tool_screen_replace),
                )
            }
        }
    }
}

/** The round Back button in light and in dark, as it sits at the top left of a tool screen. */
@PreviewLightDark
@Composable
private fun RoundBackButtonPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface {
            RoundBackButton(onClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
