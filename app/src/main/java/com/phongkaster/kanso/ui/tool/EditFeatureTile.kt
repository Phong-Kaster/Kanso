package com.phongkaster.kanso.ui.tool

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** How faint a disabled tile is drawn: Material's usual opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

/**
 * One feature of an Edit tool's panel (a crop shape, a turn, a flip, an Adjust slider, a filter):
 * its icon with its title below it, in one tile at least 56 dp wide and tall so it is easy to tap.
 *
 * With [selected] null the tile is a plain action (a turn or a flip) and only calls [onClick].
 * With [selected] true or false it is a choice among several (a crop shape, a filter, an Adjust
 * slider): the chosen tile's background fades to the theme's secondary container colour with
 * `animateColorAsState` (Compose's own animation API), the colour Material uses for "this one is
 * chosen", and a screen reader says which tile is chosen. While [enabled] is false the tile is
 * drawn faint and ignores taps.
 *
 * The title under the icon names the tile, so the icon itself says nothing to a screen reader.
 */
@Composable
fun EditFeatureTile(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    enabled: Boolean = true,
) {
    val colours = MaterialTheme.colorScheme
    val chosen = selected == true
    // The unchosen state is the same colour fully see-through, so the fade never passes through grey.
    val background by animateColorAsState(
        targetValue = if (chosen) colours.secondaryContainer else colours.secondaryContainer.copy(alpha = 0f),
        label = "edit-feature-background",
    )
    val content = if (chosen) colours.onSecondaryContainer else colours.onSurfaceVariant
    val tap = if (selected == null) {
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    } else {
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick)
    }
    Column(
        modifier = modifier
            .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .then(tap)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = content)
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
        )
    }
}

/** Three tiles in light and in dark: a plain action, a chosen choice and an unchosen one. */
@PreviewLightDark
@Composable
private fun EditFeatureTilePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditFeatureTile(R.drawable.ic_rotate_left, R.string.edit_tool_rotate_left, onClick = {})
                EditFeatureTile(R.drawable.ic_brightness, R.string.edit_tool_brightness, onClick = {}, selected = true)
                EditFeatureTile(R.drawable.ic_contrast, R.string.edit_tool_contrast, onClick = {}, selected = false)
            }
        }
    }
}
