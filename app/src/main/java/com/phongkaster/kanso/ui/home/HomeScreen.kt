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
import com.phongkaster.kanso.R
import com.phongkaster.kanso.navigation.Tool

/**
 * The Home screen: a top bar titled "Kanso" and one row per tool, then a Settings row.
 *
 * Tapping a tool row calls [onToolClick] with that tool; tapping Settings calls
 * [onSettingsClick]. The screen itself does not navigate: the caller decides what happens,
 * which keeps this composable easy to preview and test.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
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
            viewModel.tools.forEach { tool ->
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
