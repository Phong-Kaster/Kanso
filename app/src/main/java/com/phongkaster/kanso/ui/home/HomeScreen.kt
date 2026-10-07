package com.phongkaster.kanso.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme

/**
 * The Home screen: a top bar titled "Kanso" and one row per tool, then a Settings row.
 *
 * Why Home is two composables. A screen in this app usually needs things that only exist while
 * the real app runs: here, a [HomeViewModel] that the app's wiring (Koin) creates and keeps
 * alive. Android Studio's preview pane, and a quick test, cannot make one of those. So the screen
 * is cut in two, a technique called "state hoisting" (hoisting means lifting: the state is lifted
 * out of the part that draws, up to its caller):
 *
 * - [HomeScreen], this function, is the *stateful* part. It knows where the state lives: it reads
 *   the list of tools from [viewModel] and hands it on. It draws nothing itself.
 * - [HomeContent] is the *stateless* part. It holds no state of its own and knows nothing about
 *   ViewModels or Koin: it simply draws whatever list of tools it is given, and reports taps
 *   through the callbacks it is given.
 *
 * Because [HomeContent] needs only plain values (a list, two lambdas), a preview can call it
 * with made-up sample data and see exactly what the person sees, with none of the app's
 * machinery running. That is what `HomePreview` at the bottom of this file does. The other
 * screens of the app follow the same pattern.
 *
 * Tapping a tool row calls [onToolClick] with that tool; tapping Settings calls
 * [onSettingsClick]. The screen itself does not navigate: the caller decides what happens.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onToolClick: (Tool) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeContent(
        tools = viewModel.tools,
        onToolClick = onToolClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

/**
 * The stateless part of Home (see [HomeScreen] for why there are two): draws the top bar, one
 * row for each of [tools] in the order given, a divider and the Settings row.
 *
 * Tapping a tool row calls [onToolClick] with that tool; tapping Settings calls
 * [onSettingsClick].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    tools: List<Tool>,
    onToolClick: (Tool) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        // A plain scrolling Column (not a lazy list): seven rows are cheap to draw all at once.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            tools.forEach { tool ->
                HomeRow(
                    label = stringResource(tool.labelRes),
                    onClick = { onToolClick(tool) },
                )
            }
            HorizontalDivider()
            HomeRow(
                label = stringResource(R.string.settings),
                onClick = onSettingsClick,
            )
        }
    }
}

/** One tappable row on Home, showing [label] and calling [onClick] when tapped. */
@Composable
private fun HomeRow(label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        // clickable makes the whole row tappable and tells accessibility services it is a button.
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/**
 * Home as the person sees it, drawn once in light and once in dark.
 *
 * `@PreviewLightDark` asks Android Studio for two pictures of this function, one with the phone
 * in day mode and one in night mode; `ThemeMode.System` makes KansoTheme follow that setting.
 * The sample data is simply every tool the app has (`Tool.entries`, the same list the real Home
 * shows), and the taps do nothing, because a preview cannot be tapped into another screen.
 */
@PreviewLightDark
@Composable
private fun HomePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        HomeContent(
            tools = Tool.entries,
            onToolClick = {},
            onSettingsClick = {},
        )
    }
}
