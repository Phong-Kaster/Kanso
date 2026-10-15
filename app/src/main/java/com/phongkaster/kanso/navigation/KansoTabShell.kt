package com.phongkaster.kanso.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/**
 * The frame of the app's screens: draws [content] (the navigation display) and, floating at the
 * bottom over it, the tab bar ([KansoTabBar]) with [selected] marked, while [showBar] is true and
 * no keyboard is open. Taps on the bar go to [onTabSelected] and [onCamera]; this function only
 * draws and reports.
 *
 * How the tabs live on the Navigation 3 back stack. Navigation 3 has no special idea of a tab:
 * there is one back stack, a list of keys with [Home] at the bottom, and the screen shown is the
 * last key's. A tab is simply a key, [Home], [MyEdits] or [Settings] ([KansoTab.key]), and
 * choosing one rewrites the list to `[Home]` or `[Home, <tab>]` (`selectTab` in
 * BackStackActions.kt), so tabs never pile up and Back from a tab lands on Home. Screens opened
 * from a tab (a gallery, the camera, a tool) are pushed above it as usual and hide the bar. The
 * navigation host decides [showBar] and [selected] from the key on top of the back stack: the bar
 * shows only while that key is a tab (`isTabKey`), and the selected tab is the one with that key.
 *
 * Why the bar is outside the navigation display: the display slides and fades whole screens when
 * the back stack changes. The bar sits beside it in this Box, drawn after (so above) the content,
 * so it stays still while the tabs fade under it, and it is not part of any one screen.
 *
 * The keyboard: while one is open (`WindowInsets.isImeVisible`, true while the on-screen
 * keyboard takes space at the bottom of the window), the bar is hidden, so it never covers the
 * text field being typed into, such as the colour code in Settings.
 *
 * Showing and hiding: `AnimatedVisibility` fades the bar in and out and slides it from and to the
 * bottom edge, on the theme motion scheme's default effects spring. That spring never overshoots,
 * so the bar does not bounce past its place; the Expressive spatial springs would. When hidden,
 * `AnimatedVisibility` removes the bar from the screen once its exit has played, so a hidden bar
 * is neither drawn nor reachable by touch or by a screen reader.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KansoTabShell(
    showBar: Boolean,
    selected: KansoTab,
    onTabSelected: (KansoTab) -> Unit,
    onCamera: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Read while drawing: isImeVisible and the motion scheme are only available in composition.
    val keyboardOpen = WindowInsets.isImeVisible
    val motionScheme = MaterialTheme.motionScheme
    // The same no-overshoot spring for the fade (a Float, the alpha) and the slide (an IntOffset).
    val fade: FiniteAnimationSpec<Float> = motionScheme.defaultEffectsSpec()
    val slide: FiniteAnimationSpec<IntOffset> = motionScheme.defaultEffectsSpec()
    Box(modifier = modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = showBar && !keyboardOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            // In: from its own height below its place (off the bottom edge) up to its place.
            enter = fadeIn(animationSpec = fade) +
                slideInVertically(animationSpec = slide, initialOffsetY = { fullHeight -> fullHeight }),
            // Out: back down past the bottom edge as it fades.
            exit = fadeOut(animationSpec = fade) +
                slideOutVertically(animationSpec = slide, targetOffsetY = { fullHeight -> fullHeight }),
            label = "tabBar",
        ) {
            KansoTabBar(selected = selected, onTabSelected = onTabSelected, onCamera = onCamera)
        }
    }
}

/**
 * The shell as it looks on the Settings tab, drawn once in light and once in dark: a stand-in
 * screen (the word "Settings" on the background) with the floating bar over its bottom edge and
 * the Settings tab selected. The taps do nothing: a preview only draws.
 */
@PreviewLightDark
@Composable
private fun KansoTabShellPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        KansoTabShell(
            showBar = true,
            selected = KansoTab.SETTINGS,
            onTabSelected = {},
            onCamera = {},
        ) {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
