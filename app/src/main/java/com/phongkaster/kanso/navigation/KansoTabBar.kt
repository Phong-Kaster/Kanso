package com.phongkaster.kanso.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag on the floating tab bar itself: the pill that holds the three tabs. */
const val KANSO_TAB_BAR_TAG = "nav-bar"

/** The test tag on the round Camera button beside the bar (it is not inside the bar). */
const val KANSO_CAMERA_BUTTON_TAG = "nav-camera"

/**
 * Returns the test tag of [tab]'s item on the bar, such as `nav-tab-SETTINGS`. The tag sits on the
 * item's selectable node, the one screen readers announce as a tab.
 */
fun kansoTabTag(tab: KansoTab): String = "nav-tab-${tab.name}"

/** Returns the test tag of the icon inside [tab]'s item, such as `nav-tab-SETTINGS-icon`. */
fun kansoTabIconTag(tab: KansoTab): String = "${kansoTabTag(tab)}-icon"

/**
 * The bar's height: 64 dp, the height Material 3's short navigation bar gives itself (and its
 * smallest height), which fits an icon over its label.
 */
private val TAB_BAR_HEIGHT = 64.dp

/**
 * The size of the round Camera button: 64 dp, as tall as the bar beside it (Material's floating
 * toolbar size), so the two read as one floating group.
 */
private val CAMERA_BUTTON_SIZE = 64.dp

/**
 * The space around the bar and the button: 16 dp from the screen's sides and from the bottom
 * (above the navigation bar or the gesture area), Material's offset for floating bars.
 */
private val TAB_BAR_MARGIN = 16.dp

/** The space between the bar and the Camera button. */
private val CAMERA_BUTTON_GAP = 12.dp

/** How high the bar floats above the screen: the depth of the soft shadow under it. */
private val TAB_BAR_ELEVATION = 6.dp

/**
 * Kanso's floating tab bar: a pill-shaped bar holding the three tabs, Home, My edits and Settings
 * (each an icon over its name), with a round Camera button beside it.
 *
 * What a tab bar is: the app's top-level places, the ones a person moves between all the time,
 * each one tap away from any of the others. The current one, [selected], sits on a small raised
 * pill (the "indicator"); tapping a tab calls [onTabSelected] with it, and tapping the Camera
 * button calls [onCamera]. The bar itself only draws and reports; the caller (the navigation
 * host, through `KansoTabShell`) decides what a tap does to the back stack.
 *
 * The bar is Material 3's `ShortNavigationBar` with one `ShortNavigationBarItem` per tab, icon on
 * top (`NavigationItemIconPosition.Top`). Each item is a selectable node with the Tab role, so a
 * screen reader says "Home, tab, selected"; its label names it, so the icon has no description
 * of its own. The Camera button is a separate `FilledIconButton`, outside the bar, because taking
 * a photo is an action, not a place: it opens the camera on top of the current tab.
 *
 * Why the bar is the inverse of the screen: it floats over the content of the tab screens, so it
 * must stand apart from whatever scrolls under it. Material's "inverse" colour roles are made for
 * exactly this: `inverseSurface` is a dark surface in the light theme and a light one in the dark
 * theme, with `inverseOnSurface` for what is drawn on it. The selected tab's pill is
 * `inversePrimary` (the theme colour, tuned to stand out on the inverse surface), the selected
 * icon on it is `inverseSurface` again, and the selected label is `inversePrimary`, so the
 * current tab reads at a glance; the other icons and labels are `inverseOnSurface`. Every colour
 * comes from the theme, so the bar follows light and dark and the app's colour choice.
 *
 * Why it clips to a pill: `ShortNavigationBar` is a plain full-width surface with square corners
 * and no shape parameter. `shadow` draws the soft shadow of a pill (`CircleShape` on a wide box is
 * a pill: its ends are half circles) and `clip` then cuts the bar's colour to the same pill. The
 * bar's own inset padding is turned off (`WindowInsets(0, 0, 0, 0)`): it would make the bar
 * taller by the height of the navigation bar. Instead the whole row is lifted above the
 * navigation bar and the bottom gesture area, whichever is taller, and 16 dp more, so the bar
 * floats and no tab sits where a swipe up means "go home".
 *
 * The row fills the width it is given: the bar takes what the 64 dp Camera button leaves, and its
 * three tabs share that width equally, so all of it fits on a 320 dp wide phone.
 */
@Composable
fun KansoTabBar(
    selected: KansoTab,
    onTabSelected: (KansoTab) -> Unit,
    onCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MaterialTheme.colorScheme
    // The item colours (see the KDoc). `selectedTextColorTopIconPosition` is the selected label's
    // colour when the icon is on top; the older single "selected text colour" parameter is hidden
    // in this version of Material 3 and does not compile.
    val itemColours = ShortNavigationBarItemDefaults.colors(
        selectedIconColor = colours.inverseSurface,
        selectedTextColorTopIconPosition = colours.inversePrimary,
        selectedIndicatorColor = colours.inversePrimary,
        unselectedIconColor = colours.inverseOnSurface,
        unselectedTextColor = colours.inverseOnSurface,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Above the navigation bar and the bottom gesture area, whichever is taller: `union`
            // takes the larger of the two at each edge, and `only(Bottom)` leaves out the side
            // gesture areas, where the back swipe starts.
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
            )
            .padding(TAB_BAR_MARGIN),
        horizontalArrangement = Arrangement.spacedBy(CAMERA_BUTTON_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The bar: a pill-shaped surface (its `shape` draws the shadow and cuts the colour in one
        // step) holding a Row of the three items. The Row, not `ShortNavigationBar`'s own layout,
        // shares the width: each item gets `weight(1f)`, so the three are exactly equal, which
        // `ShortNavigationBar` did not do (it gave every item the full width under Robolectric).
        Surface(
            modifier = Modifier
                // The bar takes all the width the Camera button leaves.
                .weight(1f)
                .testTag(KANSO_TAB_BAR_TAG),
            shape = CircleShape,
            color = colours.inverseSurface,
            contentColor = colours.inverseOnSurface,
            shadowElevation = TAB_BAR_ELEVATION,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TAB_BAR_HEIGHT)
                    // Tells a screen reader the items are one group of which one is selected.
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KansoTab.entries.forEach { tab ->
                    ShortNavigationBarItem(
                        selected = tab == selected,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            // The drawable paints white; Icon tints it with the item's icon colour.
                            Icon(
                                painter = painterResource(tab.iconRes),
                                // The label below names the tab, so the icon stays silent.
                                contentDescription = null,
                                modifier = Modifier.testTag(kansoTabIconTag(tab)),
                            )
                        },
                        label = { Text(text = stringResource(tab.labelRes), maxLines = 1) },
                        // The tag lands on the item's selectable Tab node.
                        modifier = Modifier
                            .weight(1f)
                            .testTag(kansoTabTag(tab)),
                        iconPosition = NavigationItemIconPosition.Top,
                        colors = itemColours,
                    )
                }
            }
        }
        // `shapes =` is the Material 3 Expressive version of the button: round at rest, it squares
        // its outline a little while a finger is on it. "Camera" is its spoken name; it shows no
        // text. Its colours are the theme's primary pair, the strongest action colour.
        FilledIconButton(
            onClick = onCamera,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier
                .size(CAMERA_BUTTON_SIZE)
                .testTag(KANSO_CAMERA_BUTTON_TAG),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colours.primary,
                contentColor = colours.onPrimary,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_photo_camera),
                contentDescription = stringResource(R.string.tool_camera),
            )
        }
    }
}

/**
 * Returns the space at the bottom of the screen that the floating tab bar covers: the bar's 64 dp,
 * the 16 dp below and above it, and the taller of the navigation bar and the bottom gesture area.
 *
 * A tab screen adds this much empty space after its last item (its `contentPadding`), so the
 * person can always scroll the last item up above the bar instead of leaving it under it. Only
 * the navigation host calls it and passes the result down; the screens themselves never do. It
 * reads the window insets, which only exist while Compose is drawing, hence a composable function.
 */
@Composable
fun tabBarClearance(): Dp {
    val density = LocalDensity.current
    // The same insets the bar's row is lifted by, in pixels, turned back into dp below.
    val bottomInset = WindowInsets.navigationBars
        .union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom))
        .getBottom(density)
    return TAB_BAR_HEIGHT + TAB_BAR_MARGIN * 2 + with(density) { bottomInset.toDp() }
}

/**
 * The floating tab bar as the app opens, Home selected, drawn once in light and once in dark on
 * the screen's background.
 *
 * The box around it is exactly [tabBarClearance] tall, so the picture also shows the space a tab
 * screen leaves for the bar: in a preview there is no navigation bar, so the bar and its margins
 * fill the box exactly. The taps do nothing: a preview only draws.
 */
@PreviewLightDark
@Composable
private fun KansoTabBarPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tabBarClearance()),
                contentAlignment = Alignment.BottomCenter,
            ) {
                KansoTabBar(selected = KansoTab.HOME, onTabSelected = {}, onCamera = {})
            }
        }
    }
}
