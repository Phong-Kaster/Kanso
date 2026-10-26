package com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** How faint a disabled entry is drawn: Material's usual opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

/** The diameter of the "this tool holds a change" dot. */
private val DOT_SIZE = 6.dp

/** The size of an entry's icon. */
private val ENTRY_ICON_SIZE = 28.dp

/**
 * One entry of the row: [icon] over [title], at least 48 dp tall so it is easy to tap. While
 * [highlighted] its background is the secondary container colour, faded in and out by
 * `animateColorAsState` (Compose's own animation API) rather than switched at once. While
 * [changed] a small primary-coloured dot (tagged [dotTag]) sits at the icon's top right corner.
 * [tap] builds the touch behaviour (a selectable tab, a button or a switch); the entry is drawn
 * faint while [enabled] is false.
 */
@Composable
internal fun EditToolEntry(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    modifier: Modifier,
    enabled: Boolean,
    tap: () -> Modifier,
    highlighted: Boolean = false,
    changed: Boolean = false,
    dotTag: String? = null,
) {
    val changedText = stringResource(R.string.edit_session_tool_changed)
    val colours = MaterialTheme.colorScheme
    // The closed state is the same colour fully see-through, so the fade never passes through grey.
    val background by animateColorAsState(
        targetValue = if (highlighted) colours.secondaryContainer else colours.secondaryContainer.copy(alpha = 0f),
        label = "edit-tool-background",
    )
    val content = if (highlighted) colours.onSecondaryContainer else colours.onSurfaceVariant
    Column(
        modifier = modifier
            .sizeIn(minHeight = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .then(tap())
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            // Narrow side padding: five entries must fit a 411 dp phone beside Compare.
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        // The name under the icon names the entry, so the icon itself says nothing to a screen reader.
        Box {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(ENTRY_ICON_SIZE),
            )
            if (changed && dotTag != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        // Half outside the icon's corner, so it never covers the symbol itself.
                        .offset(x = DOT_SIZE / 2, y = -DOT_SIZE / 2)
                        .testTag(dotTag)
                        // Its own semantics node (not merged into the entry), so a test finds its
                        // tag and a screen reader says the tool holds a change.
                        .semantics(mergeDescendants = true) { contentDescription = changedText }
                        .size(DOT_SIZE)
                        .clip(CircleShape)
                        .background(colours.primary),
                )
            }
        }
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Two entries in light and dark: one plain, one highlighted with the "changed" dot. */
@PreviewLightDark
@Composable
private fun EditToolEntryPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Row {
            EditToolEntry(
                icon = R.drawable.ic_crop,
                title = R.string.edit_layout_crop,
                modifier = Modifier,
                enabled = true,
                tap = { Modifier },
            )
            EditToolEntry(
                icon = R.drawable.ic_tune,
                title = R.string.edit_layout_adjust,
                modifier = Modifier,
                enabled = true,
                tap = { Modifier },
                highlighted = true,
                changed = true,
            )
        }
    }
}
